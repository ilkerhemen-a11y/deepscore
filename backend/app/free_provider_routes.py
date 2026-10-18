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



# TFF is the authoritative source for the completed 2025-26 season.
TFF_SEASON_URL = "https://www.tff.org/Default.aspx?hafta={round}&pageID=1768"


def _fetch_tff_round(round_number: int) -> list[dict[str, Any]]:
    """Parse one official TFF round page into normalized fixtures."""
    from html.parser import HTMLParser
    import re
    from html import unescape

    class _Rows(HTMLParser):
        def __init__(self) -> None:
            super().__init__(convert_charrefs=True)
            self.rows: list[list[str]] = []
            self.row: list[str] | None = None
            self.cell: list[str] | None = None

        def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
            if tag == "tr":
                self.row = []
            elif tag in ("td", "th") and self.row is not None:
                self.cell = []

        def handle_data(self, data: str) -> None:
            if self.cell is not None:
                self.cell.append(data)

        def handle_endtag(self, tag: str) -> None:
            if tag in ("td", "th") and self.row is not None and self.cell is not None:
                self.row.append(" ".join(" ".join(self.cell).split()))
                self.cell = None
            elif tag == "tr" and self.row is not None:
                self.rows.append(self.row)
                self.row = None
                self.cell = None

    request = Request(
        TFF_SEASON_URL.format(round=round_number),
        headers={"Accept": "text/html", "User-Agent": "DeepScore/1.0 (fixture archive)"},
    )
    try:
        with urlopen(request, timeout=12) as response:
            raw_page = response.read()
            # TFF pages may use a Turkish legacy encoding even when the
            # response header is missing or incorrect. Prefer the declared
            # charset, then fall back to Windows-1254 if UTF-8 decoding fails.
            charset = response.headers.get_content_charset() or "utf-8"
            try:
                page = raw_page.decode(charset)
            except (LookupError, UnicodeDecodeError):
                page = raw_page.decode("cp1254", errors="replace")
            if "\ufffd" in page:
                legacy_page = raw_page.decode("cp1254", errors="replace")
                if legacy_page.count("\ufffd") < page.count("\ufffd"):
                    page = legacy_page
    except (HTTPError, URLError, TimeoutError):
        return []

    parser = _Rows()
    parser.feed(page)
    fixtures: list[dict[str, Any]] = []
    score_pattern = re.compile(r"^\s*(\d+)\s*[-–]\s*(\d+)\s*$")
    date_pattern = re.compile(r"(\d{2})\.(\d{2})\.(\d{4})(?:\s+(\d{2}:\d{2}))?")
    for row in parser.rows:
        cells = [unescape(cell).strip() for cell in row if cell and cell.strip()]
        score_index = next((i for i, cell in enumerate(cells) if score_pattern.match(cell)), None)
        if score_index is None or score_index < 2 or score_index + 1 >= len(cells):
            continue
        date_match = date_pattern.search(cells[0])
        score_match = score_pattern.match(cells[score_index])
        if not date_match or not score_match:
            continue
        home_team = cells[score_index - 1]
        away_team = cells[score_index + 1]
        if not home_team or not away_team or len(home_team) > 100 or len(away_team) > 100:
            continue
        day, month, year, kickoff = date_match.groups()
        fixtures.append({
            "round": f"{round_number}. Round",
            "date": f"{year}-{month}-{day}",
            "time": kickoff,
            "home_team": home_team,
            "away_team": away_team,
            "home_score": int(score_match.group(1)),
            "away_score": int(score_match.group(2)),
            "status": "finished",
        })
    return fixtures


def _fetch_tff_2025_26() -> list[dict[str, Any]]:
    """Fetch official TFF round pages concurrently; return only a complete season."""
    from concurrent.futures import ThreadPoolExecutor

    with ThreadPoolExecutor(max_workers=8) as pool:
        batches = list(pool.map(_fetch_tff_round, range(1, 35)))
    fixtures = [fixture for batch in batches for fixture in batch]
    # Fail closed: don't silently replace the official feed with a partial scrape.
    return fixtures if len(fixtures) == 306 else []


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

    if season == "2025-26":
        official_fixtures = _fetch_tff_2025_26()
        if official_fixtures:
            official_fixtures.sort(key=lambda item: (item.get("date") or "9999-99-99", item.get("time") or "99:99", item["round"]))
            result = {
                "source": "Türkiye Futbol Federasyonu (TFF) official season fixture pages",
                "competition": "Turkish Süper Lig",
                "season": season,
                "count": len(official_fixtures),
                "finished_count": len(official_fixtures),
                "fixtures": official_fixtures,
                "notice": "Results and fixtures parsed from official TFF round pages.",
            }
            with _cache_lock:
                _season_cache[season] = (time.monotonic(), result)
            return result

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
