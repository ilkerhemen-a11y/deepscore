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

    if payload.get("errors"):
        # Do not return provider internals or credentials to clients.
        raise ProviderRequestError("Football data provider reported an API error.")
    return payload
