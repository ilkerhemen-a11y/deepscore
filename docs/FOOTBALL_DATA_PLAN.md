# DeepScore football data plan

## Initial provider choice

Use API-Football (API-SPORTS) as the first live-data provider, behind the DeepScore backend. Its published free tier currently advertises 100 requests per day and endpoints for leagues, fixtures, head-to-head, standings, team statistics, and predictions. The free plan has limits on historical seasons, so season availability must be checked per league before we promise deep history.

Official references:
- Pricing and quota: https://www.api-football.com/pricing
- API-Football getting started and quota headers: https://www.api-football.com/news/post/how-to-get-started-with-api-football
- Football-data.org API reference: https://www.football-data.org/documentation/api
- Football-data.co.uk historical CSV data and terms: https://football-data.co.uk/data

## Why API-Football first?

DeepScore needs more than final scores: current fixtures, league/season discovery, head-to-head, team statistics and match details are useful for the Turkish Süper Lig and major European leagues. API-Football lists these endpoints and makes a free plan available. We must not assume every older season or every statistic is included in that plan; verify the provider's coverage for each competition and season.

Football-data.org is a useful alternative for fixtures and standings, but access to competitions and historical seasons varies by plan. Football-data.co.uk offers extensive downloadable historical CSVs, but its published terms say the data is intended for private individuals and not commercial or automated AI/data-training products. Do not use those CSVs as DeepScore's training source unless the licensing terms are clarified and permission is obtained.

## Backend integration rules

1. Keep the provider API key in the server environment only; never bundle it in the mobile app or commit it to Git.
2. Read quota/rate-limit headers and cache responses to stay within the free daily quota.
3. Treat missing provider coverage as unavailable data, not as zero values.
4. Store normalized match results with source, competition, season and retrieval timestamp so that models can be audited.
5. Build a historical dataset and evaluate predictions chronologically (train on past matches, test on later matches); do not report accuracy until a real backtest has been run.
6. Label any output based on sample values as DEMO. Do not describe demo probabilities as real forecasts.
7. DeepScore is for analysis and virtual predictions only; no deposits, withdrawals or real-money betting features.

## First implementation milestone

- Configure an API-Football account and store its key as FOOTBALL_API_KEY in the backend deployment environment.
- Verify coverage for Türkiye Süper Lig, Premier League, La Liga, Bundesliga, Serie A and Ligue 1 for current and previous seasons.
- Fetch fixtures/results into a normalized store and add caching before the mobile app starts making repeated requests.
- Then implement team form, home/away splits, head-to-head, Poisson and a second independent model, followed by historical backtesting and calibration checks.
