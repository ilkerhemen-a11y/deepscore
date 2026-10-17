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

The mobile app uses sample data until a licensed football data provider is configured. Never commit API keys; configure them as server-side environment variables.

## Initial scope
- Dark-themed match dashboard
- Historical head-to-head and recent-form views
- Transparent baseline probabilities (clearly labeled as estimates)
- API health endpoint and starter prediction endpoint
- Virtual predictions only; no real-money wagering

Historical backtests must use only information available before each match and must be evaluated on held-out time periods to reduce data leakage.
