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

Never commit API keys; configure private provider keys as server-side environment variables.

## Free Turkish Super Lig data

The backend exposes a no-account, free v1 schedule/results source through TheSportsDB:

- `GET /v1/free/provider/status` — provider capabilities and limits note
- `GET /v1/free/turkey-super-lig/fixtures?view=upcoming` — upcoming league events
- `GET /v1/free/turkey-super-lig/fixtures?view=recent` — recent league events

The free source is cached for five minutes to reduce requests. It is **not a guaranteed live-score feed**, and the free tier has lower request limits than premium. Kickoff times and results should be verified with official sources. API-Football endpoints remain available as an optional integration, but the free Turkish league endpoints do not require its API key.

## Initial scope

- Dark-themed match dashboard
- Historical head-to-head and recent-form views
- Transparent baseline probabilities (clearly labeled as estimates)
- API health endpoint and starter prediction endpoint
- Virtual predictions only; no real-money wagering

Historical backtests must use only information available before each match and must be evaluated on held-out time periods to reduce data leakage.
