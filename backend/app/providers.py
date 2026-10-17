from __future__ import annotations

import json
import os
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from typing import Any


BASE_URL = "https://v3.football.api-sports.io"


class ProviderNotConfigured(RuntimeError):
    pass


class ProviderRequestError(RuntimeError):
    pass


def api_football_get(endpoint: str, params: dict[str, Any] | None = None) -> dict[str, Any]:
    """Call API-Football from the backend; never expose the API key to the mobile app."""
    api_key = os.getenv("FOOTBALL_API_KEY", "").strip()
    if not api_key:
        raise ProviderNotConfigured("FOOTBALL_API_KEY is not configured on the server.")

    query = urlencode({key: value for key, value in (params or {}).items() if value is not None})
    url = f"{BASE_URL}/{endpoint.lstrip('/')}"
    if query:
        url = f"{url}?{query}"

    request = Request(
        url,
        headers={"x-apisports-key": api_key, "Accept": "application/json"},
        method="GET",
    )
    try:
        with urlopen(request, timeout=12) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except HTTPError as exc:
        raise ProviderRequestError(f"Football data provider returned HTTP {exc.code}.") from exc
    except (URLError, TimeoutError, json.JSONDecodeError) as exc:
        raise ProviderRequestError("Football data provider could not be reached or returned invalid JSON.") from exc

    errors = payload.get("errors")
    if errors:
        # Classify provider errors without returning raw provider text or secrets.
        values = " ".join(str(value) for value in errors.values()) if isinstance(errors, dict) else str(errors)
        normalized = values.lower()
        if any(word in normalized for word in ("token", "api key", "apikey", "unauthorized", "unauthorised", "invalid key", "authentication")):
            message = "API-Football rejected the API key. Check FOOTBALL_API_KEY in Render."
        elif any(word in normalized for word in ("subscription", "plan", "season", "access", "endpoint")):
            message = "API-Football denied access to this season or endpoint. Check plan coverage and try a supported season."
        elif any(word in normalized for word in ("limit", "requests", "quota", "rate")):
            message = "API-Football request quota or rate limit may have been reached."
        else:
            message = "API-Football rejected the request. Check provider access and request parameters."
        raise ProviderRequestError(message)
    return payload
