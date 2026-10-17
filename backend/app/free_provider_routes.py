from __future__ import annotations

import json
import time
from threading import Lock
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from typing import Any

from fastapi import APIRouter, HTTPException, Query

router = APIRouter(prefix="/v1/free", tags=["free-football-data"])

BASE_URL = "https://www.thesportsdb.com/api/v1/json/123"
TURKISH_SUPER_LIG_ID = "4339"
CACHE_SECONDS = 300
_cache: dict[str, tuple[float, dict[str, Any]]] = {}
_cache_lock = Lock()


def _fetch_events(view: str) -> dict[str, Any]:
    """Fetch recent or upcoming Turkish Super Lig events from TheSportsDB's free v1 API."""
    now = time.monotonic()
    with _cache_lock:
        cached = _cache.get(view)
        if cached and now - cached[0] < CACHE_SECONDS:
            return cached[1]

    endpoint = "eventsnextleague.php" if view == "upcoming" else "eventspastleague.php"
    url = f"{BASE_URL}/{endpoint}?id={TURKISH_SUPER_LIG_ID}"
    request = Request(url, headers={"Accept": "application/json", "User-Agent": "DeepScore/0.1"}, method="GET")
    try:
        with urlopen(request, timeout=10) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Free football data source returned HTTP {exc.code}.") from exc
    except (URLError, TimeoutError, json.JSONDecodeError) as exc:
        raise HTTPException(status_code=502, detail="Free football data source is temporarily unavailable or returned invalid JSON.") from exc

    if not isinstance(payload, dict):
        raise HTTPException(status_code=502, detail="Free football data source returned an unexpected response.")
    events = payload.get("events")
    if events is not None and not isinstance(events, list):
        raise HTTPException(status_code=502, detail="Free football data source returned an unexpected event list.")

    normalized = {
        "source": "TheSportsDB free v1 API",
        "competition": "Turkish Super Lig",
        "competition_id": TURKISH_SUPER_LIG_ID,
        "view": view,
        "live_scores": False,
        "cache_seconds": CACHE_SECONDS,
        "count": len(events or []),
        "fixtures": [_normalize_event(event) for event in (events or []) if isinstance(event, dict)],
        "notice": "Free schedule/results feed; not a guaranteed live-score service. Verify kickoff times and results with official sources.",
    }
    with _cache_lock:
        _cache[view] = (time.monotonic(), normalized)
    return normalized


def _normalize_event(event: dict[str, Any]) -> dict[str, Any]:
    return {
        "event_id": event.get("idEvent"),
        "name": event.get("strEvent"),
        "date": event.get("dateEvent"),
        "time": event.get("strTime"),
        "round": event.get("intRound"),
        "home_team": event.get("strHomeTeam"),
        "away_team": event.get("strAwayTeam"),
        "home_score": event.get("intHomeScore"),
        "away_score": event.get("intAwayScore"),
        "status": event.get("strStatus"),
        "venue": event.get("strVenue"),
    }


@router.get("/provider/status")
def free_provider_status() -> dict[str, Any]:
    return {
        "provider": "TheSportsDB free v1 API",
        "configured": True,
        "api_key_required": False,
        "competition": "Turkish Super Lig",
        "competition_id": TURKISH_SUPER_LIG_ID,
        "live_scores": False,
        "note": "Uses the provider's public free key; free endpoints have lower limits than premium.",
    }


@router.get("/turkey-super-lig/fixtures")
def turkey_super_lig_fixtures(
    view: str = Query(default="upcoming", pattern="^(upcoming|recent)$"),
) -> dict[str, Any]:
    """Return the provider's next or most recent league events."""
    return _fetch_events(view)
