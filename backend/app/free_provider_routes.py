from __future__ import annotations

from datetime import date

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


# OpenFootball publishes the full Turkish Süper Lig season archive as public-domain JSON.
SEASON_ARCHIVE_BASE = "https://raw.githubusercontent.com/openfootball/football.json/master"
_SEASON_CACHE_SECONDS = 21600
_season_cache: dict[str, tuple[float, dict[str, Any]]] = {}


@router.get("/turkey-super-lig/season")
def turkey_super_lig_season(
    season: str = Query(default="2025-26", pattern=r"^(2023-24|2024-25|2025-26)$"),
) -> dict[str, Any]:
    """Return the complete season schedule and final scores from the public-domain archive."""
    now = time.monotonic()
    with _cache_lock:
        cached = _season_cache.get(season)
        if cached and now - cached[0] < _SEASON_CACHE_SECONDS:
            return cached[1]

    url = f"{SEASON_ARCHIVE_BASE}/{season}/tr.1.json"
    request = Request(url, headers={"Accept": "application/json", "User-Agent": "DeepScore/1.0"})
    try:
        with urlopen(request, timeout=15) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Season archive returned HTTP {exc.code}.") from exc
    except (URLError, TimeoutError, json.JSONDecodeError) as exc:
        raise HTTPException(status_code=502, detail="Season archive is temporarily unavailable or returned invalid JSON.") from exc

    raw_matches = payload.get("matches") if isinstance(payload, dict) else None
    if not isinstance(raw_matches, list):
        raise HTTPException(status_code=502, detail="Season archive did not contain a valid match list.")

    fixtures: list[dict[str, Any]] = []
    for match in raw_matches:
        if not isinstance(match, dict):
            continue
        score = match.get("score")
        full_time = score.get("ft") if isinstance(score, dict) else score if isinstance(score, list) else None
        if not isinstance(full_time, list) or len(full_time) < 2:
            home_score = away_score = None
        else:
            home_score, away_score = full_time[0], full_time[1]
        fixtures.append({
            "round": str(match.get("round") or "Tur bilgisi yok"),
            "date": match.get("date"),
            "time": match.get("time"),
            "home_team": match.get("team1"),
            "away_team": match.get("team2"),
            "home_score": home_score,
            "away_score": away_score,
            "status": (
                "finished" if home_score is not None and away_score is not None
                else "scheduled" if (match.get("date") or "9999-99-99") >= date.today().isoformat()
                else "result_unavailable"
            ),
        })

    fixtures.sort(key=lambda item: (item.get("date") or "9999-99-99", item.get("time") or "99:99", item["round"]))
    result = {
        "source": "OpenFootball / football.json (public-domain archive)",
        "competition": "Turkish Süper Lig",
        "season": season,
        "count": len(fixtures),
        "finished_count": sum(1 for item in fixtures if item["status"] == "finished"),
        "fixtures": fixtures,
        "notice": "Historical schedule/results archive; kickoff times are shown as published by the source and may need official verification.",
    }
    with _cache_lock:
        _season_cache[season] = (time.monotonic(), result)
    return result
