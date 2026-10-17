from __future__ import annotations

import unittest
from datetime import date

from app.historical_model import Match, expected_goals, parse_season, score_grid


class HistoricalModelTests(unittest.TestCase):
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
