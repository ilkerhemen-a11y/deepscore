# DeepScore API notes

The API currently includes a backend-only API-Football client in `app/providers.py` and routes in `app/provider_routes.py`:

- `GET /v1/provider/status` reports whether `FOOTBALL_API_KEY` is configured without exposing it.
- `GET /v1/competitions?country=Turkey&season=2026` proxies the provider league catalogue.
- `GET /v1/fixtures?league_id=203&season=2026&from=2026-08-01&to=2026-08-31` proxies fixtures/results for a competition season and optional date range.

The provider key belongs only in the backend runtime environment. Set `FOOTBALL_API_KEY` in Render's environment settings after creating an API-Football account. Do not add a real key to GitHub, mobile config, screenshots or chat.

The provider routes are not live until a valid key is configured. Provider season/competition coverage must be verified from actual responses before building a historical dataset. The free tier is quota-limited, so responses should be cached before broad syncing. Current demo prediction output remains demo-only and is not a real forecast.
