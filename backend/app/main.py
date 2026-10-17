from __future__ import annotations

import os
from math import exp, factorial
from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel, Field

from app.provider_routes import router as provider_router

app = FastAPI(
    title="DeepScore API",
    version="0.1.0",
    description="Football analytics API. Initial endpoints use clearly labeled demo inputs.",
)


app.include_router(provider_router)


class PredictionRequest(BaseModel):
    home_team: str = Field(min_length=1, max_length=100)
    away_team: str = Field(min_length=1, max_length=100)
    home_expected_goals: float = Field(default=1.35, ge=0, le=8)
    away_expected_goals: float = Field(default=1.05, ge=0, le=8)


def poisson_probability(goals: int, rate: float) -> float:
    if rate == 0:
        return 1.0 if goals == 0 else 0.0
    return exp(-rate) * (rate**goals) / factorial(goals)


def score_probabilities(home_rate: float, away_rate: float, max_goals: int = 10) -> dict[str, float]:
    home_win = draw = away_win = over_25 = btts = 0.0
    for home_goals in range(max_goals + 1):
        for away_goals in range(max_goals + 1):
            probability = poisson_probability(home_goals, home_rate) * poisson_probability(away_goals, away_rate)
            if home_goals > away_goals:
                home_win += probability
            elif home_goals == away_goals:
                draw += probability
            else:
                away_win += probability
            if home_goals + away_goals >= 3:
                over_25 += probability
            if home_goals > 0 and away_goals > 0:
                btts += probability
    total = home_win + draw + away_win
    if total:
        home_win, draw, away_win = home_win / total, draw / total, away_win / total
    return {
        "home_win": round(home_win, 4),
        "draw": round(draw, 4),
        "away_win": round(away_win, 4),
        "over_2_5": round(over_25, 4),
        "both_teams_to_score": round(btts, 4),
    }


@app.get("/health")
def health() -> dict[str, str]:
    configured = bool(os.getenv("FOOTBALL_API_KEY", "").strip())
    return {
        "status": "ok",
        "service": "deepscore-api",
        "data_mode": "provider_configured" if configured else "demo",
    }


@app.get("/")
def root() -> dict[str, str]:
    return {"name": "DeepScore API", "docs": "/docs", "health": "/health"}


@app.post("/v1/demo/predictions")
def demo_prediction(request: PredictionRequest) -> dict[str, Any]:
    """Return a Poisson baseline from caller-supplied expected-goal estimates.

    This endpoint is a development demo, not a real match forecast. Expected goals
    are not yet derived from historical/provider data.
    """
    return {
        "mode": "demo",
        "warning": "Demo only: inputs are illustrative and not sourced from real match data.",
        "match": {"home": request.home_team, "away": request.away_team},
        "expected_goals": {
            "home": request.home_expected_goals,
            "away": request.away_expected_goals,
        },
        "model": "independent_poisson_baseline",
        "probabilities": score_probabilities(request.home_expected_goals, request.away_expected_goals),
    }
