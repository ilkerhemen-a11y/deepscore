# DeepScore

DeepScore is a football analytics app focused on historical match data, transparent probability estimates, model comparison, and **free-to-play virtual predictions only**. It does not accept deposits or offer cash-out.

## Structure

- `mobile/`: React Native + Expo + TypeScript app
- `backend/`: FastAPI service and starter analysis engine

## Local development

### Mobile app
```bash
cd mobile
npm install
npx expo start
```

### Backend
```bash
cd backend
python -m venv .venv
# Activate the environment, then:
pip install -r requirements.txt
uvicorn app.main:app --reload
```

The mobile prediction detail uses the historical model API for the Turkish Süper Lig. The API fetches public-domain historical archives server-side; never commit API keys, and keep any provider credentials in server-side environment variables.

## Initial scope
- Dark-themed match dashboard
- Historical head-to-head and recent-form views
- Transparent baseline probabilities (clearly labeled as estimates)
- API health endpoint and starter prediction endpoint
- Virtual predictions only; no real-money wagering

Historical backtests must use only information available before each match and must be evaluated on held-out time periods to reduce data leakage.


## Historical Süper Lig model

The first real-data prediction path uses the OpenFootball Europe Turkey Süper Lig archives for seasons 2023/24, 2024/25 and 2025/26. The repository declares its football data to be public domain. The backend fetches only these historical archive files, caches them in-process, and ignores fixtures without a final score. It does not depend on live scores or a paid football API.

Endpoints:
- `GET /v1/historical/model/status` — source, model and loaded-data status.
- `POST /v1/historical/predictions` — input JSON: `{"home_team":"Galatasaray","away_team":"Fenerbahçe"}`.
- `GET /v1/historical/backtest` — chronological walk-forward evaluation using only matches dated before each test match date.

The model is a season-weighted, smoothed Poisson baseline using home/away scoring and conceding rates. It is an initial statistical baseline, not a claim of proven predictive advantage. Backtest metrics must be reviewed before calling it reliable. Live scores are not provided. The archive fetch requires outbound network access from the backend runtime.

## Free Turkish Super Lig data

The backend exposes a no-account, free v1 schedule/results source through TheSportsDB:

- `GET /v1/free/provider/status` — provider capabilities and limits note
- `GET /v1/free/turkey-super-lig/fixtures?view=upcoming` — upcoming league events
- `GET /v1/free/turkey-super-lig/fixtures?view=recent` — recent league events
- `GET /v1/free/turkey-super-lig/season?season=2025-26` — full season fixture list grouped by round, including available final scores (also supports 2023-24 and 2024-25)

The free source is cached for five minutes to reduce requests. It is **not a guaranteed live-score feed**, and the free tier has lower request limits than premium. Kickoff times and results should be verified with official sources. API-Football endpoints remain available as an optional integration, but the free Turkish league endpoints do not require its API key.
