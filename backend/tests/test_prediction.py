from __future__ import annotations

import unittest

from app.main import score_probabilities


class PoissonBaselineTests(unittest.TestCase):
    def test_match_outcomes_sum_to_one(self) -> None:
        result = score_probabilities(1.4, 0.9)
        self.assertAlmostEqual(result["home_win"] + result["draw"] + result["away_win"], 1.0, places=3)

    def test_all_probabilities_are_bounded(self) -> None:
        result = score_probabilities(1.8, 1.2)
        for probability in result.values():
            self.assertGreaterEqual(probability, 0.0)
            self.assertLessEqual(probability, 1.0)

    def test_zero_expected_goals_is_supported(self) -> None:
        result = score_probabilities(0.0, 0.0)
        self.assertAlmostEqual(result["draw"], 1.0, places=3)
        self.assertAlmostEqual(result["home_win"], 0.0, places=3)
        self.assertAlmostEqual(result["away_win"], 0.0, places=3)


if __name__ == "__main__":
    unittest.main()
