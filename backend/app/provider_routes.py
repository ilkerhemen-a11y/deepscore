from __future__ import annotations

import os
from typing import Any

from fastapi import APIRouter, HTTPException, Query

from app.providers import ProviderNotConfigured, ProviderRequestError, api_football_get

router = APIRouter(prefix="/v1", tags=["football-data"])


def _get(endpoint: str, params: dict[str, Any]) -> dict[str, Any]:
    try:
        return api_football_get(endpoint, params)
    except ProviderNotConfigured as exc:
        raise HTTPException(status_code=503, detail="Live data is not configured. Add FOOTBALL_API_KEY to the backend environment.") from exc
    except ProviderRequestError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@router.get("/provider/status")
def provider_status() -> dict[str, Any]:
    return {
        "provider": "API-Football",
        "configured": bool(os.getenv("FOOTBALL_API_KEY", "").strip()),
        "note": "The API key stays on the server and is not returned.",
    }


@router.get("/competitions")
def competitions(
    country: str | None = Query(default=None, min_length=2, max_length=60),
    season: int | None = Query(default=None, ge=2000, le=2100),
) -> dict[str, Any]:
    params: dict[str, Any] = {}
    if country:
        params["country"] = country
    if season:
        params["season"] = season
    return _get("leagues", params)


@router.get("/fixtures")
def fixtures(
    league_id: int = Query(..., ge=1, description="League ID returned by the provider competitions endpoint."),
    season: int = Query(..., ge=2000, le=2100, description="Season start year, e.g. 2025 for 2025/26."),
    date_from: str | None = Query(default=None, alias="from", pattern=r"^\d{4}-\d{2}-\d{2}$"),
    date_to: str | None = Query(default=None, alias="to", pattern=r"^\d{4}-\d{2}-\d{2}$"),
    team_id: int | None = Query(default=None, ge=1),
) -> dict[str, Any]:
    params: dict[str, Any] = {"league": league_id, "season": season}
    if date_from:
        params["from"] = date_from
    if date_to:
        params["to"] = date_to
    if team_id:
        params["team"] = team_id
    return _get("fixtures", params)
