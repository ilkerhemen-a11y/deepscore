from __future__ import annotations

import re
import unicodedata
from collections import defaultdict
from dataclasses import dataclass
from datetime import date
from functools import lru_cache
from math import exp, factorial, log
from typing import Any
from urllib.error import URLError
from urllib.request import Request, urlopen

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field

router = APIRouter(prefix="/v1/historical", tags=["historical-model"])

# OpenFootball's Europe repository dedicates its football.db data to the public domain.
# We consume completed scores only; no live-score claim or live provider is involved.
DATA_URLS = {
    "2018-19": "https://raw.githubusercontent.com/openfootball/europe/master/turkey/2018-19_tr1.txt",
    "2019-20": "https://raw.githubusercontent.com/openfootball/europe/master/turkey/2019-20_tr1.txt",
    "2020-21": "https://raw.githubusercontent.com/openfootball/europe/master/turkey/2020-21_tr1.txt",
    "2023-24": "https://raw.githubusercontent.com/openfootball/europe/master/turkey/2023-24_tr1.txt",
    "2024-25": "https://raw.githubusercontent.com/openfootball/europe/master/turkey/2024-25_tr1.txt",
    "2025-26": "https://raw.githubusercontent.com/openfootball/europe/master/turkey/2025-26_tr1.txt",
}
SOURCE = "OpenFootball Europe / Turkey Süper Lig (public-domain data)"
SEASON_STARTS = {"2018-19": 2018, "2019-20": 2019, "2020-21": 2020, "2023-24": 2023, "2024-25": 2024, "2025-26": 2025}
MONTHS = {name: number for number, name in enumerate(
    ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"], 1
)}
DATE_RE = re.compile(r"^\s{2,}(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)\s+([A-Z][a-z]{2})\s+(\d{1,2})(?:\s+(\d{4}))?\s*$")
MATCH_RE = re.compile(
    r"^\s{4}(?:(\d{2}:\d{2})\s+)?(.+?)\s+v\s+(.+?)\s+(\d{1,2})-(\d{1,2})(?:\s+\(\d{1,2}-\d{1,2}\))?\s*$"
)


@dataclass(frozen=True)
class Match:
    date: date
    home: str
    away: str
    home_goals: int
    away_goals: int
    season: str


def parse_season(text: str, season: str) -> list[Match]:
    """Parse OpenFootball Football.TXT rows, skipping fixtures without a final score."""
    season_start = SEASON_STARTS[season]
    current_date: date | None = None
    matches: list[Match] = []
    for line in text.splitlines():
        date_match = DATE_RE.match(line)
        if date_match:
            month_name, day_text, explicit_year = date_match.groups()
            month = MONTHS[month_name]
            year = int(explicit_year) if explicit_year else (season_start if month >= 7 else season_start + 1)
            try:
                current_date = date(year, month, int(day_text))
            except ValueError:
                current_date = None
            continue
        match = MATCH_RE.match(line)
        if not match or current_date is None:
            continue
        _kickoff, home, away, home_goals, away_goals = match.groups()
        matches.append(Match(
            date=current_date,
            home=home.strip(),
            away=away.strip(),
            home_goals=int(home_goals),
            away_goals=int(away_goals),
            season=season,
        ))
    return matches


@lru_cache(maxsize=1)
def load_matches() -> tuple[Match, ...]:
    """Fetch the three most recent completed-season archives and cache for this process."""
    all_matches: list[Match] = []
    for season, url in DATA_URLS.items():
        request = Request(url, headers={"User-Agent": "DeepScore/1.0 historical-model"})
        try:
            with urlopen(request, timeout=8) as response:
                text = response.read().decode("utf-8")
        except (OSError, URLError, TimeoutError) as exc:
            raise RuntimeError(f"Historical source unavailable for season {season}") from exc
        all_matches.extend(parse_season(text, season))
    # Keep only valid completed matches; date and stable season ordering make walk-forward tests deterministic.
    unique: dict[tuple[str, str, str, int, int], Match] = {}
    for match in all_matches:
        key = (match.date.isoformat(), normalize(match.home), normalize(match.away), match.home_goals, match.away_goals)
        unique[key] = match
    return tuple(sorted(unique.values(), key=lambda m: (m.date, m.season, normalize(m.home), normalize(m.away))))


def normalize(value: str) -> str:
    decomposed = unicodedata.normalize("NFKD", value.casefold())
    return " ".join("".join(ch for ch in decomposed if not unicodedata.combining(ch)).split())


def poisson_probability(goals: int, rate: float) -> float:
    if rate <= 0:
        return 1.0 if goals == 0 else 0.0
    return exp(-rate) * (rate ** goals) / factorial(goals)


def score_grid(home_rate: float, away_rate: float, max_goals: int = 8) -> dict[str, Any]:
    grid: list[tuple[int, int, float]] = []
    for home_goals in range(max_goals + 1):
        for away_goals in range(max_goals + 1):
            probability = poisson_probability(home_goals, home_rate) * poisson_probability(away_goals, away_rate)
            grid.append((home_goals, away_goals, probability))
    total = sum(item[2] for item in grid) or 1.0
    home_win = sum(p for h, a, p in grid if h > a) / total
    draw = sum(p for h, a, p in grid if h == a) / total
    away_win = sum(p for h, a, p in grid if h < a) / total
    over_25 = sum(p for h, a, p in grid if h + a >= 3) / total
    btts = sum(p for h, a, p in grid if h > 0 and a > 0) / total
    top_scores = sorted(grid, key=lambda item: item[2], reverse=True)[:5]
    return {
        "home_win": round(home_win, 4),
        "draw": round(draw, 4),
        "away_win": round(away_win, 4),
        "over_2_5": round(over_25, 4),
        "under_2_5": round(1 - over_25, 4),
        "both_teams_to_score": round(btts, 4),
        "top_scores": [
            {"score": f"{h}-{a}", "probability": round(p / total, 4)}
            for h, a, p in top_scores
        ],
    }


def expected_goals(history: list[Match], home_team: str, away_team: str) -> tuple[float, float]:
    """Smoothed, season-weighted attack/defence rates computed from prior completed matches."""
    league_home_goals = league_away_goals = total_weight = 0.0
    stats: dict[str, dict[str, float]] = defaultdict(lambda: {
        "home_scored": 0.0, "home_conceded": 0.0, "home_games": 0.0,
        "away_scored": 0.0, "away_conceded": 0.0, "away_games": 0.0,
    })
    for match in history:
        # Give the latest season more influence without discarding older evidence.
        season_year = SEASON_STARTS[match.season]
        weight = 0.72 ** max(0, 2025 - season_year)
        league_home_goals += match.home_goals * weight
        league_away_goals += match.away_goals * weight
        total_weight += weight
        h = stats[normalize(match.home)]
        a = stats[normalize(match.away)]
        h["home_scored"] += match.home_goals * weight
        h["home_conceded"] += match.away_goals * weight
        h["home_games"] += weight
        a["away_scored"] += match.away_goals * weight
        a["away_conceded"] += match.home_goals * weight
        a["away_games"] += weight
    if total_weight <= 0:
        raise ValueError("At least one completed historical match is required")
    avg_home = max(0.2, league_home_goals / total_weight)
    avg_away = max(0.2, league_away_goals / total_weight)
    # Prior strength prevents small samples from producing extreme goal rates.
    prior = 5.0
    h = stats[normalize(home_team)]
    a = stats[normalize(away_team)]
    home_attack = ((h["home_scored"] + prior * avg_home) / (h["home_games"] + prior)) / avg_home
    home_defence = ((h["home_conceded"] + prior * avg_away) / (h["home_games"] + prior)) / avg_away
    away_attack = ((a["away_scored"] + prior * avg_away) / (a["away_games"] + prior)) / avg_away
    away_defence = ((a["away_conceded"] + prior * avg_home) / (a["away_games"] + prior)) / avg_home
    home_rate = avg_home * home_attack * away_defence
    away_rate = avg_away * away_attack * home_defence
    return min(4.5, max(0.15, home_rate)), min(4.5, max(0.15, away_rate))


class HistoricalPredictionRequest(BaseModel):
    home_team: str = Field(min_length=1, max_length=100)
    away_team: str = Field(min_length=1, max_length=100)


def get_data() -> tuple[Match, ...]:
    try:
        return load_matches()
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc


@router.get("/model/status")
def model_status() -> dict[str, Any]:
    matches = get_data()
    teams = sorted({team for m in matches for team in (m.home, m.away)}, key=normalize)
    return {
        "mode": "historical_data_model",
        "source": SOURCE,
        "source_license": "Public domain (OpenFootball repository declaration)",
        "seasons": list(DATA_URLS),
        "completed_matches_loaded": len(matches),
        "teams_in_archives": len(teams),
        "model": "season_weighted_smoothed_poisson",
        "live_scores": False,
        "warning": "Predictions are statistical estimates, not guarantees. Historical data may be incomplete or corrected upstream.",
    }


@router.get("/teams")
def historical_teams() -> dict[str, Any]:
    """Return normalized team choices available in the historical archive."""
    matches = get_data()
    teams = sorted({team for match in matches for team in (match.home, match.away)}, key=normalize)
    return {
        "source": SOURCE,
        "teams": teams,
        "count": len(teams),
        "seasons": list(DATA_URLS),
        "live_scores": False,
    }


@router.get("/matches")
def historical_matches(
    team: str | None = Query(default=None, min_length=1, max_length=100),
    season: str | None = Query(default=None, pattern=r"^\d{4}-\d{2}$"),
    limit: int = Query(default=100, ge=1, le=500),
    offset: int = Query(default=0, ge=0, le=10000),
) -> dict[str, Any]:
    """Paginated completed historical results, newest first."""
    matches = list(get_data())
    if team:
        team_key = normalize(team)
        matches = [match for match in matches if team_key in (normalize(match.home), normalize(match.away))]
    if season:
        matches = [match for match in matches if match.season == season]
    matches.sort(key=lambda match: (match.date, match.season, normalize(match.home), normalize(match.away)), reverse=True)
    total = len(matches)
    page = matches[offset:offset + limit]
    return {
        "source": SOURCE,
        "league": "Süper Lig",
        "count": len(page),
        "total": total,
        "limit": limit,
        "offset": offset,
        "has_more": offset + len(page) < total,
        "seasons": list(DATA_URLS),
        "live_scores": False,
        "matches": [
            {
                "date": match.date.isoformat(),
                "home": match.home,
                "away": match.away,
                "home_goals": match.home_goals,
                "away_goals": match.away_goals,
                "score": f"{match.home_goals}-{match.away_goals}",
                "season": match.season,
                "status": "completed",
            }
            for match in page
        ],
    }


@router.post("/predictions")
def historical_prediction(request: HistoricalPredictionRequest) -> dict[str, Any]:
    matches = [m for m in get_data() if m.date < date.today()]
    if normalize(request.home_team) == normalize(request.away_team):
        raise HTTPException(status_code=422, detail="Home and away teams must be different")
    if len(matches) < 30:
        raise HTTPException(status_code=503, detail="Not enough completed historical matches to build a prediction")
    known_teams = {normalize(team) for m in matches for team in (m.home, m.away)}
    unknown = [team for team in (request.home_team, request.away_team) if normalize(team) not in known_teams]
    if unknown:
        raise HTTPException(
            status_code=422,
            detail="No historical archive data found for team(s): " + ", ".join(unknown),
        )
    try:
        home_rate, away_rate = expected_goals(matches, request.home_team, request.away_team)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return {
        "mode": "historical_data_model",
        "source": SOURCE,
        "match": {"home": request.home_team, "away": request.away_team},
        "training_matches": len(matches),
        "training_seasons": list(DATA_URLS),
        "model": "season_weighted_smoothed_poisson",
        "expected_goals": {"home": round(home_rate, 2), "away": round(away_rate, 2)},
        "probabilities": score_grid(home_rate, away_rate),
        "live_scores": False,
        "warning": "Historical-data estimate only; not a guarantee. Team names are matched against source data after accent/case normalization.",
    }


@router.get("/backtest")
def historical_backtest(
    min_training_matches: int = Query(default=120, ge=60, le=800),
    max_test_matches: int = Query(default=250, ge=20, le=500),
) -> dict[str, Any]:
    """Chronological walk-forward test; each test match is predicted only from earlier dates."""
    matches = list(get_data())
    # Same-day matches are kept out of one another's training data to avoid date leakage.
    test_dates = sorted({m.date for m in matches})[-max_test_matches:]
    evaluated = 0
    correct = 0
    brier_total = 0.0
    log_loss_total = 0.0
    home_pick_correct = 0
    majority_pick_correct = 0
    prior_brier_total = 0.0
    prior_log_loss_total = 0.0
    outcome_keys = ("home_win", "draw", "away_win")
    for match_date in test_dates:
        prior = [m for m in matches if m.date < match_date]
        todays = [m for m in matches if m.date == match_date]
        if len(prior) < min_training_matches:
            continue
        prior_counts = {key: 1 for key in outcome_keys}  # Laplace smoothing avoids zero probabilities.
        for old_match in prior:
            old_result = "home_win" if old_match.home_goals > old_match.away_goals else "away_win" if old_match.home_goals < old_match.away_goals else "draw"
            prior_counts[old_result] += 1
        prior_total = sum(prior_counts.values())
        prior_probabilities = {key: prior_counts[key] / prior_total for key in outcome_keys}
        majority_pick = max(outcome_keys, key=lambda key: prior_counts[key])
        for match in todays:
            try:
                home_rate, away_rate = expected_goals(prior, match.home, match.away)
            except ValueError:
                continue
            probabilities = score_grid(home_rate, away_rate)
            p = probabilities
            actual = "home_win" if match.home_goals > match.away_goals else "away_win" if match.home_goals < match.away_goals else "draw"
            predicted = max(outcome_keys, key=lambda key: p[key])
            correct += int(predicted == actual)
            home_pick_correct += int(actual == "home_win")
            majority_pick_correct += int(majority_pick == actual)
            target = {key: float(key == actual) for key in outcome_keys}
            brier_total += sum((p[key] - target[key]) ** 2 for key in outcome_keys)
            log_loss_total -= log(max(1e-6, p[actual]))
            prior_brier_total += sum((prior_probabilities[key] - target[key]) ** 2 for key in outcome_keys)
            prior_log_loss_total -= log(max(1e-6, prior_probabilities[actual]))
            evaluated += 1
    model_accuracy = correct / evaluated if evaluated else None
    prior_accuracy = majority_pick_correct / evaluated if evaluated else None
    return {
        "mode": "chronological_walk_forward_backtest",
        "source": SOURCE,
        "evaluated_matches": evaluated,
        "minimum_training_matches": min_training_matches,
        "accuracy": round(model_accuracy, 4) if model_accuracy is not None else None,
        "mean_multiclass_brier_score": round(brier_total / evaluated, 4) if evaluated else None,
        "mean_log_loss": round(log_loss_total / evaluated, 4) if evaluated else None,
        "baselines": {
            "always_pick_home_win_accuracy": round(home_pick_correct / evaluated, 4) if evaluated else None,
            "historical_majority_outcome_accuracy": round(prior_accuracy, 4) if prior_accuracy is not None else None,
            "historical_outcome_frequency_brier_score": round(prior_brier_total / evaluated, 4) if evaluated else None,
            "historical_outcome_frequency_log_loss": round(prior_log_loss_total / evaluated, 4) if evaluated else None,
        },
        "improvement_vs_historical_majority_accuracy_percentage_points": round((model_accuracy - prior_accuracy) * 100, 2) if model_accuracy is not None and prior_accuracy is not None else None,
        "improvement_vs_historical_frequency_brier": round((prior_brier_total - brier_total) / evaluated, 4) if evaluated else None,
        "improvement_vs_historical_frequency_log_loss": round((prior_log_loss_total - log_loss_total) / evaluated, 4) if evaluated else None,
        "live_scores": False,
        "warning": "Baselines use only results dated before each test date. This is a historical walk-forward evaluation, not a guarantee of future performance. Metrics are unavailable if the source archive does not contain enough dated results.",
    }
