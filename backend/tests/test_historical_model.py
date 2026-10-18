from __future__ import annotations

import unittest
from datetime import date, timedelta
from unittest.mock import patch

from app.historical_model import Match, expected_goals, historical_backtest, historical_matches, historical_teams, parse_season, score_grid


class HistoricalModelTests(unittest.TestCase):
    def test_backtest_caps_matches_and_excludes_future_results(self) -> None:
        start = date(2024, 1, 1)
        matches = [Match(start + timedelta(days=i // 3), "Alpha", "Beta", 1, 0, "2024-25") for i in range(90)]
        matches.append(Match(date.today() + timedelta(days=1), "Alpha", "Beta", 0, 9, "2024-25"))
        with patch("app.historical_model.get_data", return_value=tuple(matches)):
            result = historical_backtest(min_training_matches=60, max_test_matches=20)
        self.assertEqual(result["evaluated_matches"], 20)
        self.assertEqual(result["accuracy"], 1.0)

    def test_parser_reads_completed_matches_and_skips_unplayed_fixtures(self) -> None:
        text = """= Turkish Süper Lig 2024/25

  Fri Aug 9 2024
    20:30  Galatasaray              v Konyaspor                2-1 (1-0)
  Sat Aug 10
    19:00  Fenerbahçe                v Beşiktaş
"""
        matches = parse_season(text, "2024-25")
        self.assertEqual(len(matches), 1)
        self.assertEqual(matches[0].home, "Galatasaray")
        self.assertEqual(matches[0].away, "Konyaspor")
        self.assertEqual((matches[0].home_goals, matches[0].away_goals), (2, 1))
        self.assertEqual(matches[0].date, date(2024, 8, 9))

    def test_probability_outputs_are_bounded_and_outcomes_sum_to_one(self) -> None:
        result = score_grid(1.55, 1.05)
        self.assertAlmostEqual(result["home_win"] + result["draw"] + result["away_win"], 1.0, places=3)
        for key in ("home_win", "draw", "away_win", "over_2_5", "under_2_5", "both_teams_to_score"):
            self.assertGreaterEqual(result[key], 0.0)
            self.assertLessEqual(result[key], 1.0)
        self.assertTrue(result["top_scores"])

    def test_backtest_reports_walk_forward_baselines(self) -> None:
        matches = []
        start = date(2024, 8, 1)
        teams = ("Alpha", "Beta", "Gamma", "Delta")
        for index in range(60):
            matches.append(Match(
                date=start + timedelta(days=index),
                home=teams[index % len(teams)],
                away=teams[(index + 1) % len(teams)],
                home_goals=2 if index % 3 else 0,
                away_goals=0 if index % 3 else 1,
                season="2024-25",
            ))
        matches.extend([
            Match(start + timedelta(days=61), "Alpha", "Beta", 1, 0, "2024-25"),
            Match(start + timedelta(days=62), "Gamma", "Delta", 1, 1, "2024-25"),
        ])
        with patch("app.historical_model.get_data", return_value=tuple(matches)):
            result = historical_backtest(min_training_matches=60, max_test_matches=20)
        self.assertEqual(result["evaluated_matches"], 2)
        self.assertIn("always_pick_home_win_accuracy", result["baselines"])
        self.assertIn("historical_majority_outcome_accuracy", result["baselines"])
        self.assertIn("historical_outcome_frequency_brier_score", result["baselines"])
        self.assertIn("historical_outcome_frequency_log_loss", result["baselines"])
        self.assertIsNotNone(result["improvement_vs_historical_majority_accuracy_percentage_points"])

    def test_historical_matches_are_paginated_and_sorted_newest_first(self) -> None:
        matches = (
            Match(date(2024, 8, 1), "Alpha", "Beta", 2, 0, "2024-25"),
            Match(date(2025, 2, 1), "Gamma", "Alpha", 1, 1, "2024-25"),
            Match(date(2024, 9, 1), "Beta", "Gamma", 0, 3, "2024-25"),
        )
        with patch("app.historical_model.get_data", return_value=matches):
            result = historical_matches(team=None, season=None, limit=2, offset=0)
            filtered = historical_matches(team="alpha", season=None, limit=10, offset=0)
        self.assertEqual(result["total"], 3)
        self.assertEqual(result["count"], 2)
        self.assertTrue(result["has_more"])
        self.assertEqual(result["matches"][0]["date"], "2025-02-01")
        self.assertEqual(filtered["total"], 2)
        self.assertTrue(all("Alpha" in (m["home"], m["away"]) for m in filtered["matches"]))

    def test_historical_team_list_is_deduplicated(self) -> None:
        matches = (
            Match(date(2024, 8, 1), "Alpha", "Beta", 2, 0, "2024-25"),
            Match(date(2024, 8, 8), "Beta", "Gamma", 1, 1, "2024-25"),
        )
        with patch("app.historical_model.get_data", return_value=matches):
            result = historical_teams()
        self.assertEqual(result["count"], 3)
        self.assertEqual(result["teams"], ["Alpha", "Beta", "Gamma"])

    def test_team_goal_rates_use_historical_match_results(self) -> None:
        history = [
            Match(date(2024, 8, 1), "Alpha", "Beta", 3, 0, "2024-25"),
            Match(date(2024, 8, 8), "Beta", "Alpha", 0, 2, "2024-25"),
            Match(date(2024, 8, 15), "Alpha", "Gamma", 2, 0, "2024-25"),
            Match(date(2024, 8, 22), "Gamma", "Beta", 1, 1, "2024-25"),
        ]
        home_rate, away_rate = expected_goals(history, "Alpha", "Beta")
        self.assertGreater(home_rate, 0)
        self.assertGreater(away_rate, 0)
        self.assertLessEqual(home_rate, 4.5)
        self.assertLessEqual(away_rate, 4.5)


if __name__ == "__main__":
    unittest.main()
