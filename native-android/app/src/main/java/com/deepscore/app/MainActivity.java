package com.deepscore.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private LinearLayout root;
    private LinearLayout pageContent;
    private String activeTab = "Maçlar";
    private String selectedLeague = "Tümü";
    private Match selectedMatch;
    private static final String API_BASE = "https://deepscore-api.onrender.com";
    private boolean archiveLoading = false;
    private boolean archiveLoaded = false;
    private String archiveMessage = "";
    private int archiveTotal = 0;
    private int archiveApiCount = 0;
    private boolean archiveLoadingMore = false;
    private boolean teamsLoading = false;
    private boolean teamsLoaded = false;
    private String teamsMessage = "";
    private boolean predictionLoading = false;
    private String predictionMessage = "";
    private JSONObject predictionResult;
    private final List<String> teamNames = new ArrayList<>();
    private String selectedHomeTeam = "";
    private String selectedAwayTeam = "";
    private String selectedVirtualMarket = "Ev sahibi kazanır";
    private int virtualBalance = 1000;
    private int pendingVirtualStake = 0;
    private double pendingVirtualProbability = 0.0;
    private double pendingVirtualOdds = 0.0;
    private String pendingVirtualMarket = "";
    private String pendingVirtualMatch = "";
    private String virtualBetMessage = "";

    private static final int BG = Color.rgb(8, 13, 25);
    private static final int PANEL = Color.rgb(18, 27, 45);
    private static final int PANEL_LIGHT = Color.rgb(24, 36, 57);
    private static final int WHITE = Color.rgb(244, 247, 252);
    private static final int MUTED = Color.rgb(150, 164, 185);
    private static final int GREEN = Color.rgb(53, 211, 154);
    private static final int LINE = Color.rgb(42, 56, 79);
    private static final int BLUE = Color.rgb(107, 166, 255);

    private final List<Match> matches = new ArrayList<>();

    private static final class Match {
        final String league, home, away, homeScore, awayScore, date, venue, note, season;
        Match(String league, String home, String away, String homeScore, String awayScore,
              String date, String venue, String note) {
            this(league, home, away, homeScore, awayScore, date, venue, note, "");
        }
        Match(String league, String home, String away, String homeScore, String awayScore,
              String date, String venue, String note, String season) {
            this.league = league;
            this.home = home;
            this.away = away;
            this.homeScore = homeScore;
            this.awayScore = awayScore;
            this.date = date;
            this.venue = venue;
            this.note = note;
            this.season = season;
        }
        String score() { return homeScore + " - " + awayScore; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        loadHistoricalMatches();
        virtualBalance = getSharedPreferences("deepscore_virtual_bets", MODE_PRIVATE).getInt("balance", 1000);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageContent, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottomNav(), new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        renderPage();
        loadHistoricalArchive();
    }

    private void loadHistoricalMatches() {
        matches.add(new Match("SÜPER LİG", "Galatasaray", "Fenerbahçe", "0", "1",
                "19 Mayıs 2024", "RAMS Park", "2023–2024 sezonu lig karşılaşması."));
        matches.add(new Match("LA LIGA", "Real Madrid", "Barcelona", "3", "2",
                "21 Nisan 2024", "Santiago Bernabéu", "2023–2024 sezonu lig karşılaşması."));
        matches.add(new Match("SÜPER LİG", "Fenerbahçe", "Galatasaray", "1", "3",
                "21 Eylül 2024", "Ülker Stadyumu", "2024–2025 sezonu lig karşılaşması."));
        matches.add(new Match("LA LIGA", "Real Madrid", "Barcelona", "0", "4",
                "26 Ekim 2024", "Santiago Bernabéu", "2024–2025 sezonu lig karşılaşması."));
        matches.add(new Match("SÜPER LİG", "Galatasaray", "Fenerbahçe", "0", "0",
                "24 Şubat 2025", "RAMS Park", "2024–2025 sezonu lig karşılaşması."));
        matches.add(new Match("LA LIGA", "Barcelona", "Real Madrid", "1", "2",
                "28 Ekim 2023", "Estadi Olímpic Lluís Companys", "2023–2024 sezonu lig karşılaşması."));
    }

    private void renderPage() {
        pageContent.removeAllViews();
        if (selectedMatch != null) {
            renderMatchDetail(selectedMatch);
            return;
        }
        switch (activeTab) {
            case "Arşiv": renderMatches(true); break;
            case "Tahminler": renderPredictions(); break;
            case "Sanal Kupon": renderVirtualBets(); break;
            case "Profil": renderProfile(); break;
            default: renderMatches(false); break;
        }
    }

    private void renderMatches(boolean archive) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        addHeader(content);
        addText(content, archive ? "Maç Arşivi" : "Maç Merkezi", 27, WHITE, true, 26);
        addText(content, archive
                ? "Geçmiş sezonlardan tamamlanmış karşılaşmalar."
                : "Geçmiş maçlar ve kesinleşmiş skorlar.", 14, MUTED, false, 5);
        if (archiveLoading) addText(content, "Tarihsel sonuçlar sunucudan yükleniyor…", 12, MUTED, false, 10);
        if (!archiveMessage.isEmpty()) addText(content, archiveMessage, 12, Color.rgb(255, 171, 105), false, 10);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.setGravity(Gravity.CENTER_VERTICAL);
        String[] leagues = {"Tümü", "Süper Lig", "La Liga"};
        for (String league : leagues) {
            boolean selected = league.equals(selectedLeague);
            TextView chip = text(league, 12, selected ? BG : MUTED, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), dp(9), dp(14), dp(9));
            chip.setBackground(round(selected ? GREEN : PANEL_LIGHT, 24, selected ? GREEN : LINE));
            chip.setOnClickListener(v -> { selectedLeague = league; renderPage(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            filters.addView(chip, lp);
        }
        LinearLayout.LayoutParams filterLp = new LinearLayout.LayoutParams(-1, -2);
        filterLp.topMargin = dp(18);
        content.addView(filters, filterLp);

        LinearLayout section = new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(archive ? "Tüm Karşılaşmalar" : "Sonuçlanmış Maçlar", 18, WHITE, true);
        section.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView count = text(countMatches() + " MAÇ", 10, GREEN, true);
        count.setPadding(dp(9), dp(6), dp(9), dp(6));
        count.setBackground(round(Color.rgb(15, 57, 50), 30, Color.rgb(25, 99, 77)));
        section.addView(count);
        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(-1, -2);
        sectionLp.topMargin = dp(24);
        sectionLp.bottomMargin = dp(12);
        content.addView(section, sectionLp);

        int shown = 0;
        for (int i = 0; i < matches.size(); i++) {
            Match match = matches.get(i);
            if (!matchesFilter(match)) continue;
            if (!archive && shown >= 3) continue;
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
            if (shown > 0) cardLp.topMargin = dp(12);
            content.addView(matchCard(match), cardLp);
            shown++;
        }
        if (shown == 0) {
            TextView empty = text("Bu filtrede gösterilecek geçmiş maç bulunamadı.", 13, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(24), dp(18), dp(24));
            empty.setBackground(round(PANEL, 16, LINE));
            content.addView(empty);
        }
        if (!archive) {
            TextView archiveButton = text("Tüm maç arşivini gör  →", 13, GREEN, true);
            archiveButton.setGravity(Gravity.CENTER);
            archiveButton.setPadding(dp(12), dp(16), dp(12), dp(12));
            archiveButton.setOnClickListener(v -> { activeTab = "Arşiv"; renderPage(); updateNav(); });
            content.addView(archiveButton);
        } else if (archiveApiCount < archiveTotal) {
            TextView more = text(archiveLoadingMore ? "Daha fazla maç yükleniyor…" : "Daha fazla maç yükle  ↓", 13, GREEN, true);
            more.setGravity(Gravity.CENTER);
            more.setPadding(dp(12), dp(18), dp(12), dp(18));
            more.setOnClickListener(v -> { if (!archiveLoadingMore) loadMoreHistoricalArchive(); });
            content.addView(more);
            addText(content, archiveApiCount + " / " + archiveTotal + " tarihsel Süper Lig maçı yüklendi", 11, MUTED, false, 2);
        }
        pageContent.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private int countMatches() {
        int count = 0;
        for (Match match : matches) if (matchesFilter(match)) count++;
        return count;
    }

    private boolean matchesFilter(Match match) {
        return "Tümü".equals(selectedLeague)
                || ("Süper Lig".equals(selectedLeague) && "SÜPER LİG".equals(match.league))
                || ("La Liga".equals(selectedLeague) && "LA LIGA".equals(match.league));
    }

    private void addHeader(LinearLayout content) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.addView(text("DeepScore", 25, WHITE, true));
        TextView subtitle = text("GEÇMİŞ MAÇLAR • KESİNLEŞMİŞ SKORLAR", 9, MUTED, true);
        subtitle.setLetterSpacing(0.1f);
        brand.addView(subtitle);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = text("DS", 14, GREEN, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(PANEL_LIGHT, 40, LINE));
        header.addView(badge, new LinearLayout.LayoutParams(dp(44), dp(44)));
        content.addView(header);
    }

    private View matchCard(Match match) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.setBackground(round(PANEL, 18, LINE));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> { selectedMatch = match; renderPage(); });
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView league = text(match.league, 10, GREEN, true);
        league.setLetterSpacing(0.08f);
        top.addView(league, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(text(match.date, 11, MUTED, true));
        card.addView(top);
        addDivider(card, 12, 12);
        LinearLayout teams = new LinearLayout(this);
        teams.setGravity(Gravity.CENTER_VERTICAL);
        teams.addView(teamColumn(match.home), new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout scoreBox = new LinearLayout(this);
        scoreBox.setOrientation(LinearLayout.HORIZONTAL);
        scoreBox.setGravity(Gravity.CENTER);
        scoreBox.addView(text(match.homeScore, 21, WHITE, true));
        scoreBox.addView(text(" - ", 15, MUTED, true));
        scoreBox.addView(text(match.awayScore, 21, WHITE, true));
        scoreBox.setPadding(dp(6), dp(5), dp(6), dp(5));
        scoreBox.setBackground(round(PANEL_LIGHT, 12, LINE));
        teams.addView(scoreBox, new LinearLayout.LayoutParams(-2, dp(42)));
        LinearLayout.LayoutParams awayLp = new LinearLayout.LayoutParams(0, -2, 1);
        awayLp.leftMargin = dp(8);
        teams.addView(teamColumn(match.away), awayLp);
        card.addView(teams);
        TextView finished = text("✓  MAÇ TAMAMLANDI  ·  DETAYLAR →", 10, GREEN, true);
        finished.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams finishLp = new LinearLayout.LayoutParams(-1, -2);
        finishLp.topMargin = dp(12);
        card.addView(finished, finishLp);
        return card;
    }

    private void renderMatchDetail(Match match) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(24));
        TextView back = text("←  Maç listesine dön", 14, GREEN, true);
        back.setPadding(0, dp(8), 0, dp(12));
        back.setOnClickListener(v -> { selectedMatch = null; renderPage(); });
        content.addView(back);
        addHeader(content);
        addText(content, "Maç Detayı", 27, WHITE, true, 28);
        addText(content, match.league + "  •  " + match.date, 13, MUTED, false, 6);
        LinearLayout scorePanel = new LinearLayout(this);
        scorePanel.setOrientation(LinearLayout.VERTICAL);
        scorePanel.setGravity(Gravity.CENTER);
        scorePanel.setPadding(dp(18), dp(24), dp(18), dp(24));
        scorePanel.setBackground(round(PANEL, 20, LINE));
        LinearLayout teams = new LinearLayout(this);
        teams.setGravity(Gravity.CENTER);
        teams.addView(teamColumn(match.home), new LinearLayout.LayoutParams(0, -2, 1));
        TextView score = text(match.score(), 27, WHITE, true);
        score.setGravity(Gravity.CENTER);
        teams.addView(score, new LinearLayout.LayoutParams(0, -2, 1));
        teams.addView(teamColumn(match.away), new LinearLayout.LayoutParams(0, -2, 1));
        scorePanel.addView(teams);
        TextView status = text("✓  MAÇ TAMAMLANDI", 11, GREEN, true);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.topMargin = dp(18);
        scorePanel.addView(status, statusLp);
        LinearLayout.LayoutParams panelLp = new LinearLayout.LayoutParams(-1, -2);
        panelLp.topMargin = dp(20);
        content.addView(scorePanel, panelLp);
        addInfoRow(content, "Lig", match.league);
        addInfoRow(content, "Tarih", match.date);
        addInfoRow(content, "Stadyum", match.venue);
        addInfoRow(content, "Karşılaşma", match.home + " – " + match.away);
        addInfoRow(content, "Durum", "Tamamlandı · kesinleşmiş tarihsel skor");
        TextView note = text(match.note, 12, MUTED, false);
        note.setPadding(dp(14), dp(14), dp(14), dp(14));
        note.setBackground(round(PANEL, 14, LINE));
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
        noteLp.topMargin = dp(16);
        content.addView(note, noteLp);
        ScrollView detailScroll = new ScrollView(this);
        detailScroll.setFillViewport(false);
        detailScroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        pageContent.addView(detailScroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void addInfoRow(LinearLayout content, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(13), dp(2), dp(13));
        row.addView(text(label, 12, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        TextView valueView = text(value, 12, WHITE, true);
        valueView.setGravity(Gravity.RIGHT);
        row.addView(valueView, new LinearLayout.LayoutParams(0, -2, 2));
        content.addView(row);
        addDivider(content, 0, 0);
    }

    private void renderPredictions() {
        if (!teamsLoaded && !teamsLoading) loadTeamsFromApi();
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        addHeader(content);
        addText(content, "Yapay Zekâ Tahmin Merkezi", 25, WHITE, true, 26);
        addText(content, "Geçmiş Süper Lig sonuçlarından hesaplanan ayrıntılı istatistiksel tahmin.", 14, MUTED, false, 5);

        LinearLayout notice = infoPanel("MODEL VE VERİ",
                "Geçmiş tamamlanmış maçlar, ev/deplasman gol performansı ve sezon ağırlıkları kullanılır. Canlı oran veya canlı maç verisi değildir; tahminler garanti vermez.");
        LinearLayout.LayoutParams noticeLp = new LinearLayout.LayoutParams(-1, -2);
        noticeLp.topMargin = dp(18);
        content.addView(notice, noticeLp);

        if (teamsLoading) addText(content, "Takım listesi yükleniyor…", 13, MUTED, false, 18);
        if (!teamsMessage.isEmpty()) addText(content, teamsMessage, 12, Color.rgb(255, 171, 105), false, 12);

        if (teamsLoaded && teamNames.size() >= 2) {
            addText(content, "EV SAHİBİ TAKIM", 12, GREEN, true, 20);
            Spinner homeSpinner = teamSpinner();
            homeSpinner.setBackground(round(PANEL_LIGHT, 14, GREEN));
            content.addView(homeSpinner, new LinearLayout.LayoutParams(-1, dp(54)));
            addText(content, "DEPLASMAN TAKIMI", 12, BLUE, true, 14);
            Spinner awaySpinner = teamSpinner();
            awaySpinner.setBackground(round(PANEL_LIGHT, 14, BLUE));
            content.addView(awaySpinner, new LinearLayout.LayoutParams(-1, dp(54)));
            setSpinnerSelection(homeSpinner, selectedHomeTeam);
            setSpinnerSelection(awaySpinner, selectedAwayTeam);
            homeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    selectedHomeTeam = teamNames.get(position);
                    pulseSelection(homeSpinner);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            });
            awaySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    selectedAwayTeam = teamNames.get(position);
                    pulseSelection(awaySpinner);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            });
            addText(content, "✓  Ev sahibi: " + (selectedHomeTeam.isEmpty() ? "Takım seçin" : selectedHomeTeam), 12, GREEN, true, 10);
            addText(content, "✦  Deplasman: " + (selectedAwayTeam.isEmpty() ? "Takım seçin" : selectedAwayTeam), 12, BLUE, true, 4);
            TextView predict = text(predictionLoading ? "TAHMİN HESAPLANIYOR…" : "✦  YAPAY ZEKÂ İLE TAHMİN ET", 14, BG, true);
            predict.setGravity(Gravity.CENTER);
            predict.setPadding(dp(16), dp(16), dp(16), dp(16));
            predict.setBackground(round(GREEN, 14, GREEN));
            LinearLayout.LayoutParams predictLp = new LinearLayout.LayoutParams(-1, -2);
            predictLp.topMargin = dp(18);
            content.addView(predict, predictLp);
            predict.setOnClickListener(v -> {
                if (!predictionLoading) requestPrediction(selectedHomeTeam, selectedAwayTeam);
            });
        } else if (!teamsLoading && teamsMessage.isEmpty()) {
            addText(content, "Tahmin için takım listesi alınamadı.", 13, MUTED, false, 18);
        }

        if (!predictionMessage.isEmpty()) {
            TextView error = text(predictionMessage, 12, Color.rgb(255, 171, 105), false);
            error.setPadding(dp(12), dp(12), dp(12), dp(12));
            error.setBackground(round(PANEL, 12, LINE));
            LinearLayout.LayoutParams errorLp = new LinearLayout.LayoutParams(-1, -2);
            errorLp.topMargin = dp(14);
            content.addView(error, errorLp);
        }
        if (predictionResult != null) renderPredictionResult(content, predictionResult);
        TextView virtualButton = text("🎟  SANAL KUPON OLUŞTUR  →", 13, GREEN, true);
        virtualButton.setGravity(Gravity.CENTER);
        virtualButton.setPadding(dp(12), dp(16), dp(12), dp(16));
        virtualButton.setBackground(round(PANEL_LIGHT, 14, GREEN));
        LinearLayout.LayoutParams virtualLp = new LinearLayout.LayoutParams(-1, -2);
        virtualLp.topMargin = dp(16);
        content.addView(virtualButton, virtualLp);
        virtualButton.setOnClickListener(v -> {
            activeTab = "Sanal Kupon";
            renderPage();
            updateNav();
        });
        addText(content, "Kaynak: OpenFootball tarihsel Süper Lig arşivi. Gerçek zamanlı maç veya bahis oranı kullanılmaz.", 11, MUTED, false, 18);
        pageContent.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void pulseSelection(View view) {
        AlphaAnimation pulse = new AlphaAnimation(0.55f, 1.0f);
        pulse.setDuration(420);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(5);
        view.startAnimation(pulse);
    }

    private Spinner teamSpinner() {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, teamNames) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView label = (TextView) super.getView(position, convertView, parent);
                label.setTextColor(WHITE);
                label.setTextSize(15);
                label.setPadding(dp(12), dp(8), dp(8), dp(8));
                label.setSingleLine(true);
                return label;
            }
            @Override public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView label = (TextView) super.getDropDownView(position, convertView, parent);
                label.setTextColor(WHITE);
                label.setTextSize(14);
                label.setPadding(dp(14), dp(12), dp(14), dp(12));
                label.setBackgroundColor(PANEL);
                return label;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private void setSpinnerSelection(Spinner spinner, String team) {
        int index = teamNames.indexOf(team);
        if (index >= 0) spinner.setSelection(index);
    }

    private void renderPredictionResult(LinearLayout content, JSONObject result) {
        JSONObject match = result.optJSONObject("match");
        JSONObject goals = result.optJSONObject("expected_goals");
        JSONObject probabilities = result.optJSONObject("probabilities");
        if (match == null || goals == null || probabilities == null) return;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(round(PANEL, 18, GREEN));
        LinearLayout.LayoutParams panelLp = new LinearLayout.LayoutParams(-1, -2);
        panelLp.topMargin = dp(20);
        content.addView(panel, panelLp);
        panel.addView(text("TAHMİN RAPORU", 11, GREEN, true));
        addText(panel, match.optString("home") + "  -  " + match.optString("away"), 18, WHITE, true, 10);
        addText(panel, "Beklenen gol: " + goals.optString("home") + " - " + goals.optString("away"), 14, WHITE, true, 8);
        addText(panel, "Toplam beklenen gol: " + String.format(java.util.Locale.US, "%.2f", goals.optDouble("home") + goals.optDouble("away")), 12, MUTED, false, 4);
        addDivider(panel, 14, 10);
        addText(panel, "MAÇ SONUCU OLASILIKLARI", 11, BLUE, true, 2);
        addProbabilityRow(panel, "Ev sahibi kazanır (1)", probabilities.optDouble("home_win") * 100);
        addProbabilityRow(panel, "Beraberlik (X)", probabilities.optDouble("draw") * 100);
        addProbabilityRow(panel, "Deplasman kazanır (2)", probabilities.optDouble("away_win") * 100);
        addDivider(panel, 10, 8);
        double homeWin = probabilities.optDouble("home_win") * 100;
        double draw = probabilities.optDouble("draw") * 100;
        double awayWin = probabilities.optDouble("away_win") * 100;
        addText(panel, "ÇİFTE ŞANS", 11, BLUE, true, 2);
        addProbabilityRow(panel, "1X · Ev sahibi veya beraberlik", homeWin + draw);
        addProbabilityRow(panel, "X2 · Beraberlik veya deplasman", draw + awayWin);
        addProbabilityRow(panel, "12 · Beraberlik hariç", homeWin + awayWin);
        addDivider(panel, 10, 8);
        addText(panel, "GOL PAZARLARI", 11, BLUE, true, 2);
        addProbabilityRow(panel, "2,5 gol üst", probabilities.optDouble("over_2_5") * 100);
        addProbabilityRow(panel, "2,5 gol alt", probabilities.optDouble("under_2_5") * 100);
        addProbabilityRow(panel, "Karşılıklı gol var", probabilities.optDouble("both_teams_to_score") * 100);
        addProbabilityRow(panel, "Karşılıklı gol yok", (1 - probabilities.optDouble("both_teams_to_score")) * 100);
        JSONArray scores = probabilities.optJSONArray("top_scores");
        if (scores != null && scores.length() > 0) {
            addDivider(panel, 12, 8);
            addText(panel, "EN OLASI SKORLAR", 11, BLUE, true, 2);
            for (int i = 0; i < Math.min(5, scores.length()); i++) {
                JSONObject item = scores.optJSONObject(i);
                if (item != null) addProbabilityRow(panel, item.optString("score"), item.optDouble("probability") * 100);
            }
        }
        addDivider(panel, 12, 8);
        addText(panel, "Model bilgisi", 12, WHITE, true, 2);
        addText(panel, "Kullanılan tamamlanmış maç: " + result.optInt("training_matches"), 11, MUTED, false, 5);
        JSONArray seasons = result.optJSONArray("training_seasons");
        if (seasons != null) {
            StringBuilder seasonText = new StringBuilder();
            for (int i = 0; i < seasons.length(); i++) {
                if (i > 0) seasonText.append(" · ");
                seasonText.append(seasons.optString(i));
            }
            addText(panel, "Sezonlar: " + seasonText, 11, MUTED, false, 5);
        }
        addText(panel, "Bu yüzdeler tarihsel veriden hesaplanan model olasılıklarıdır. Kesin sonuç veya kazanç garantisi yoktur.", 11, MUTED, false, 12);
    }

    private void addProbabilityRow(LinearLayout parent, String label, double percent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        row.addView(text(label, 12, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        TextView value = text(Math.round(percent) + "%", 13, WHITE, true);
        value.setGravity(Gravity.RIGHT);
        row.addView(value);
        parent.addView(row);
        View track = new View(this);
        track.setBackground(round(PANEL_LIGHT, 6, LINE));
        LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(-1, dp(5));
        trackLp.bottomMargin = dp(3);
        parent.addView(track, trackLp);
        track.post(() -> {
            int width = track.getWidth();
            if (width <= 0) return;
            View fill = new View(this);
            fill.setBackground(round(GREEN, 6, GREEN));
            LinearLayout.LayoutParams fillLp = new LinearLayout.LayoutParams((int) (width * Math.max(0, Math.min(100, percent)) / 100.0), dp(5));
            fillLp.topMargin = -dp(5);
            ((LinearLayout) track.getParent()).addView(fill, ((LinearLayout) track.getParent()).indexOfChild(track) + 1, fillLp);
        });
    }

    private void renderVirtualBets() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        addHeader(content);
        addText(content, "Sanal Bahis Laboratuvarı", 25, WHITE, true, 24);
        addText(content, "Gerçek para kullanmadan tahminlerini test et.", 14, MUTED, false, 5);

        LinearLayout wallet = infoPanel("SANAL BAKİYE", String.format(java.util.Locale.forLanguageTag("tr-TR"), "%,d puan", virtualBalance));
        wallet.setBackground(round(PANEL, 16, GREEN));
        LinearLayout.LayoutParams walletLp = new LinearLayout.LayoutParams(-1, -2);
        walletLp.topMargin = dp(18);
        content.addView(wallet, walletLp);
        LinearLayout notice = infoPanel("ÖNEMLİ",
                "Bu bölüm yalnızca sanal puanlarla çalışan bir simülasyondur. Gerçek para, ödeme, para yatırma veya çekme yoktur. Simüle edilen sonuç gerçek maç sonucu değildir.");
        LinearLayout.LayoutParams noticeLp = new LinearLayout.LayoutParams(-1, -2);
        noticeLp.topMargin = dp(10);
        content.addView(notice, noticeLp);

        if (predictionResult == null) {
            addText(content, "Önce Tahminler sekmesinden iki takım seçip tahmin oluştur. Sonra burada sanal kupon hazırlayabilirsin.", 13, WHITE, false, 18);
            TextView go = text("TAHMİNLERE GİT  →", 13, GREEN, true);
            go.setGravity(Gravity.CENTER);
            go.setPadding(dp(14), dp(15), dp(14), dp(15));
            go.setBackground(round(PANEL_LIGHT, 12, GREEN));
            LinearLayout.LayoutParams goLp = new LinearLayout.LayoutParams(-1, -2);
            goLp.topMargin = dp(12);
            content.addView(go, goLp);
            go.setOnClickListener(v -> { activeTab = "Tahminler"; renderPage(); updateNav(); });
        } else {
            JSONObject match = predictionResult.optJSONObject("match");
            JSONObject probabilities = predictionResult.optJSONObject("probabilities");
            if (match != null && probabilities != null) {
                addText(content, "Maç: " + match.optString("home") + " – " + match.optString("away"), 15, WHITE, true, 18);
                addText(content, "Tahmin pazarı", 12, GREEN, true, 14);
                String[] markets = {"Ev sahibi kazanır", "Beraberlik", "Deplasman kazanır", "2,5 gol üst", "2,5 gol alt", "Karşılıklı gol var", "Çifte şans 1X", "Çifte şans X2", "Çifte şans 12"};
                Spinner marketSpinner = new Spinner(this);
                ArrayAdapter<String> marketAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, markets) {
                    @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                        TextView label = (TextView) super.getView(position, convertView, parent);
                        label.setTextColor(WHITE); label.setTextSize(14); label.setPadding(dp(12), dp(8), dp(8), dp(8));
                        return label;
                    }
                    @Override public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
                        label.setTextColor(WHITE); label.setPadding(dp(14), dp(12), dp(14), dp(12)); label.setBackgroundColor(PANEL);
                        return label;
                    }
                };
                marketAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                marketSpinner.setAdapter(marketAdapter);
                marketSpinner.setBackground(round(PANEL_LIGHT, 12, LINE));
                int marketIndex = java.util.Arrays.asList(markets).indexOf(selectedVirtualMarket);
                if (marketIndex >= 0) marketSpinner.setSelection(marketIndex);
                marketSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { selectedVirtualMarket = markets[position]; }
                    @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
                });
                content.addView(marketSpinner, new LinearLayout.LayoutParams(-1, dp(52)));

                addText(content, "Sanal puan miktarı", 12, GREEN, true, 14);
                EditText stake = new EditText(this);
                stake.setSingleLine(true);
                stake.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                stake.setTextColor(WHITE);
                stake.setHintTextColor(MUTED);
                stake.setHint("Örn. 50");
                stake.setPadding(dp(14), dp(8), dp(14), dp(8));
                stake.setBackground(round(PANEL_LIGHT, 12, LINE));
                content.addView(stake, new LinearLayout.LayoutParams(-1, dp(52)));

                TextView place = text("SANAL KUPONU OLUŞTUR", 14, BG, true);
                place.setGravity(Gravity.CENTER);
                place.setPadding(dp(14), dp(16), dp(14), dp(16));
                place.setBackground(round(GREEN, 14, GREEN));
                LinearLayout.LayoutParams placeLp = new LinearLayout.LayoutParams(-1, -2);
                placeLp.topMargin = dp(14);
                content.addView(place, placeLp);
                place.setOnClickListener(v -> {
                    if (pendingVirtualStake > 0) {
                        virtualBetMessage = "Önce açık sanal kuponun simülasyonunu tamamla.";
                        renderPage(); return;
                    }
                    int amount;
                    try { amount = Integer.parseInt(stake.getText().toString().trim()); }
                    catch (Exception ex) { amount = 0; }
                    if (amount < 1 || amount > virtualBalance) {
                        virtualBetMessage = "1 ile mevcut sanal bakiyen arasında bir puan miktarı gir.";
                        renderPage(); return;
                    }
                    double p = virtualMarketProbability(selectedVirtualMarket, probabilities);
                    if (p <= 0 || p > 1) {
                        virtualBetMessage = "Bu pazar için olasılık hesaplanamadı.";
                        renderPage(); return;
                    }
                    pendingVirtualStake = amount;
                    pendingVirtualProbability = p;
                    pendingVirtualOdds = 1.0 / p;
                    pendingVirtualMarket = selectedVirtualMarket;
                    pendingVirtualMatch = match.optString("home") + " – " + match.optString("away");
                    virtualBalance -= amount;
                    getSharedPreferences("deepscore_virtual_bets", MODE_PRIVATE).edit().putInt("balance", virtualBalance).apply();
                    virtualBetMessage = "Kupon hazırlandı. Tahmini oran: " + String.format(java.util.Locale.US, "%.2f", pendingVirtualOdds) + " · Olası geri dönüş: " + Math.round(amount * pendingVirtualOdds) + " puan.";
                    renderPage();
                });

                if (pendingVirtualStake > 0) {
                    LinearLayout pending = infoPanel("AÇIK SANAL KUPON",
                            pendingVirtualMatch + "\nPazar: " + pendingVirtualMarket + "\nMiktar: " + pendingVirtualStake + " puan\nModel olasılığı: " + Math.round(pendingVirtualProbability * 100) + "%\nSanal oran: " + String.format(java.util.Locale.US, "%.2f", pendingVirtualOdds));
                    LinearLayout.LayoutParams pendingLp = new LinearLayout.LayoutParams(-1, -2);
                    pendingLp.topMargin = dp(16);
                    content.addView(pending, pendingLp);
                    addText(content, "Maçın gerçek sonucunu kontrol et; kuponun doğruysa TUTTU, değilse BAŞARISIZ seç. Sonuç otomatik kontrol edilmez.", 12, MUTED, false, 10);
                    LinearLayout resultButtons = new LinearLayout(this);
                    resultButtons.setOrientation(LinearLayout.HORIZONTAL);
                    resultButtons.setGravity(Gravity.CENTER);
                    TextView wonButton = text("✓ TUTTU", 14, BG, true);
                    wonButton.setGravity(Gravity.CENTER);
                    wonButton.setPadding(dp(8), dp(16), dp(8), dp(16));
                    wonButton.setBackground(round(GREEN, 12, GREEN));
                    TextView lostButton = text("✕ BAŞARISIZ", 14, WHITE, true);
                    lostButton.setGravity(Gravity.CENTER);
                    lostButton.setPadding(dp(8), dp(16), dp(8), dp(16));
                    lostButton.setBackground(round(Color.rgb(220, 75, 75), 12, Color.rgb(220, 75, 75)));
                    LinearLayout.LayoutParams resultLp = new LinearLayout.LayoutParams(0, -2, 1);
                    resultLp.topMargin = dp(12);
                    resultLp.rightMargin = dp(6);
                    resultButtons.addView(wonButton, resultLp);
                    LinearLayout.LayoutParams lostLp = new LinearLayout.LayoutParams(0, -2, 1);
                    lostLp.topMargin = dp(12);
                    lostLp.leftMargin = dp(6);
                    resultButtons.addView(lostButton, lostLp);
                    content.addView(resultButtons, new LinearLayout.LayoutParams(-1, -2));
                    wonButton.setOnClickListener(v -> settleVirtualBet(true));
                    lostButton.setOnClickListener(v -> settleVirtualBet(false));
                }
            }
        }
        if (!virtualBetMessage.isEmpty()) {
            TextView message = text(virtualBetMessage, 12, GREEN, true);
            message.setPadding(dp(12), dp(12), dp(12), dp(12));
            message.setBackground(round(PANEL, 12, LINE));
            LinearLayout.LayoutParams messageLp = new LinearLayout.LayoutParams(-1, -2);
            messageLp.topMargin = dp(14);
            content.addView(message, messageLp);
        }
        pageContent.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }


    private void settleVirtualBet(boolean won) {
        if (pendingVirtualStake <= 0) return;
        int stakeAmount = pendingVirtualStake;
        int payout = won ? (int) Math.round(stakeAmount * pendingVirtualOdds) : 0;
        virtualBalance += payout;
        getSharedPreferences("deepscore_virtual_bets", MODE_PRIVATE).edit().putInt("balance", virtualBalance).apply();
        virtualBetMessage = won
                ? "✓ TUTTU! İşaretlediğin doğru sonuç için " + payout + " sanal puan bakiyene eklendi."
                : "✕ BAŞARISIZ. " + stakeAmount + " sanal puanlık kupon kaybedildi.";
        pendingVirtualStake = 0;
        pendingVirtualProbability = 0;
        pendingVirtualOdds = 0;
        pendingVirtualMarket = "";
        pendingVirtualMatch = "";
        renderPage();
    }

    private double virtualMarketProbability(String market, JSONObject probabilities) {
        double home = probabilities.optDouble("home_win");
        double draw = probabilities.optDouble("draw");
        double away = probabilities.optDouble("away_win");
        if ("Ev sahibi kazanır".equals(market)) return home;
        if ("Beraberlik".equals(market)) return draw;
        if ("Deplasman kazanır".equals(market)) return away;
        if ("2,5 gol üst".equals(market)) return probabilities.optDouble("over_2_5");
        if ("2,5 gol alt".equals(market)) return probabilities.optDouble("under_2_5");
        if ("Karşılıklı gol var".equals(market)) return probabilities.optDouble("both_teams_to_score");
        if ("Çifte şans 1X".equals(market)) return home + draw;
        if ("Çifte şans X2".equals(market)) return draw + away;
        if ("Çifte şans 12".equals(market)) return home + away;
        return 0;
    }

    private void loadTeamsFromApi() {
        teamsLoading = true;
        new Thread(() -> {
            List<String> loaded = new ArrayList<>();
            String error = "";
            try {
                JSONObject json = apiGet("/v1/historical/teams");
                JSONArray array = json.optJSONArray("teams");
                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        String team = array.optString(i, "");
                        if (!team.isEmpty()) loaded.add(team);
                    }
                }
                if (loaded.size() < 2) error = "Arşivde yeterli takım bulunamadı.";
            } catch (Exception ex) {
                error = "Takım verileri alınamadı. İnternet bağlantısını kontrol edip tekrar deneyin.";
            }
            final String finalError = error;
            final List<String> finalTeams = loaded;
            runOnUiThread(() -> {
                teamsLoading = false;
                teamsLoaded = finalTeams.size() >= 2;
                teamsMessage = finalError;
                teamNames.clear();
                teamNames.addAll(finalTeams);
                if (teamsLoaded) {
                    selectedHomeTeam = teamNames.contains("Galatasaray") ? "Galatasaray" : teamNames.get(0);
                    selectedAwayTeam = teamNames.contains("Fenerbahçe") ? "Fenerbahçe" : teamNames.get(1);
                }
                if ("Tahminler".equals(activeTab) && selectedMatch == null) renderPage();
            });
        }).start();
    }

    private void requestPrediction(String home, String away) {
        if (home == null || away == null || home.trim().isEmpty() || away.trim().isEmpty()) {
            predictionMessage = "Lütfen iki takım seçin.";
            renderPage();
            return;
        }
        if (home.equalsIgnoreCase(away)) {
            predictionMessage = "Ev sahibi ve deplasman takımı farklı olmalı.";
            renderPage();
            return;
        }
        predictionLoading = true;
        predictionMessage = "";
        predictionResult = null;
        renderPage();
        new Thread(() -> {
            JSONObject result = null;
            String error = "";
            try {
                JSONObject body = new JSONObject();
                body.put("home_team", home);
                body.put("away_team", away);
                result = apiPost("/v1/historical/predictions", body);
            } catch (Exception ex) {
                error = "Tahmin alınamadı. Servis veya internet bağlantısı geçici olarak kullanılamıyor.";
            }
            final JSONObject finalResult = result;
            final String finalError = error;
            runOnUiThread(() -> {
                predictionLoading = false;
                predictionResult = finalResult;
                predictionMessage = finalError;
                if ("Tahminler".equals(activeTab) && selectedMatch == null) renderPage();
            });
        }).start();
    }

    private void loadHistoricalArchive() {
        if (archiveLoading || archiveLoaded) return;
        archiveLoading = true;
        new Thread(() -> {
            List<Match> downloaded = new ArrayList<>();
            String error = "";
            try {
                JSONObject json = apiGet("/v1/historical/matches?limit=500&offset=0");
                archiveTotal = json.optInt("total", 0);
                JSONArray array = json.optJSONArray("matches");
                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.optJSONObject(i);
                        if (item == null) continue;
                        String date = item.optString("date", "");
                        String[] parts = date.split("-");
                        if (parts.length == 3) date = parts[2] + "." + parts[1] + "." + parts[0];
                        String season = item.optString("season", "");
                        downloaded.add(new Match("SÜPER LİG", item.optString("home"), item.optString("away"),
                                String.valueOf(item.optInt("home_goals")), String.valueOf(item.optInt("away_goals")),
                                date, "Kaynakta belirtilmemiş", "OpenFootball tarihsel arşivi · " + season, season));
                    }
                }
                if (downloaded.isEmpty()) error = "Tarihsel arşiv boş döndü; yerel kayıtlar gösteriliyor.";
            } catch (Exception ex) {
                error = "Tarihsel arşive ulaşılamadı; şimdilik kayıtlı maçlar gösteriliyor.";
            }
            final String finalError = error;
            final List<Match> finalMatches = downloaded;
            runOnUiThread(() -> {
                archiveLoading = false;
                archiveLoaded = true;
                archiveMessage = finalError;
                archiveApiCount = finalMatches.size();
                if (!finalMatches.isEmpty()) {
                    List<Match> nonSuperLig = new ArrayList<>();
                    for (Match old : matches) if (!"SÜPER LİG".equals(old.league)) nonSuperLig.add(old);
                    matches.clear();
                    matches.addAll(finalMatches);
                    matches.addAll(nonSuperLig);
                }
                if (selectedMatch == null) renderPage();
            });
        }).start();
    }

    private void loadMoreHistoricalArchive() {
        if (archiveLoadingMore || archiveApiCount >= archiveTotal) return;
        archiveLoadingMore = true;
        new Thread(() -> {
            List<Match> downloaded = new ArrayList<>();
            String error = "";
            int total = archiveTotal;
            try {
                JSONObject json = apiGet("/v1/historical/matches?limit=500&offset=" + archiveApiCount);
                JSONArray array = json.optJSONArray("matches");
                total = json.optInt("total", archiveTotal);
                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.optJSONObject(i);
                        if (item == null) continue;
                        String date = item.optString("date", "");
                        String[] parts = date.split("-");
                        if (parts.length == 3) date = parts[2] + "." + parts[1] + "." + parts[0];
                        String season = item.optString("season", "");
                        downloaded.add(new Match("SÜPER LİG", item.optString("home"), item.optString("away"),
                                String.valueOf(item.optInt("home_goals")), String.valueOf(item.optInt("away_goals")),
                                date, "Kaynakta belirtilmemiş", "OpenFootball tarihsel arşivi · " + season, season));
                    }
                }
                if (downloaded.isEmpty()) error = "Yeni maç alınamadı. Biraz sonra tekrar deneyin.";
            } catch (Exception ex) {
                error = "Daha fazla maç yüklenemedi. İnternet bağlantısını kontrol edin.";
            }
            final String finalError = error;
            final int finalTotal = total;
            final List<Match> finalMatches = downloaded;
            runOnUiThread(() -> {
                archiveLoadingMore = false;
                archiveMessage = finalError;
                archiveTotal = finalTotal;
                if (!finalMatches.isEmpty()) {
                    List<Match> superLig = new ArrayList<>();
                    List<Match> otherLeagues = new ArrayList<>();
                    for (Match old : matches) {
                        if ("SÜPER LİG".equals(old.league)) superLig.add(old);
                        else otherLeagues.add(old);
                    }
                    superLig.addAll(finalMatches);
                    matches.clear();
                    matches.addAll(superLig);
                    matches.addAll(otherLeagues);
                    archiveApiCount += finalMatches.size();
                }
                if (selectedMatch == null) renderPage();
            });
        }).start();
    }

    private JSONObject apiGet(String path) throws Exception {
        return apiRequest(path, null);
    }

    private JSONObject apiPost(String path, JSONObject body) throws Exception {
        return apiRequest(path, body);
    }

    private JSONObject apiRequest(String path, JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API_BASE + path).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(45000);
        connection.setRequestProperty("Accept", "application/json");
        if (body != null) {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }
        } else {
            connection.setRequestMethod("GET");
        }
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder response = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }
        }
        connection.disconnect();
        if (code < 200 || code >= 300) {
            String message = response.toString();
            try {
                JSONObject error = new JSONObject(message);
                message = error.optString("detail", message);
            } catch (Exception ignored) { }
            throw new IllegalStateException("HTTP " + code + ": " + message);
        }
        return new JSONObject(response.toString());
    }

    private void renderProfile() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(24));
        addHeader(content);
        addText(content, "Uygulama", 27, WHITE, true, 26);
        addText(content, "DeepScore sürüm bilgileri", 14, MUTED, false, 5);
        LinearLayout panel = infoPanel("DEEPSCORE ANDROID", "Genişletilmiş tarihsel maç arşivi ve geçmiş verilere dayalı tahmin merkezi.");
        LinearLayout.LayoutParams panelLp = new LinearLayout.LayoutParams(-1, -2);
        panelLp.topMargin = dp(20);
        content.addView(panel, panelLp);
        addInfoRow(content, "Sürüm", "0.6.0");
        addInfoRow(content, "Arşivdeki maç", String.valueOf(matches.size()));
        addInfoRow(content, "Canlı veri", "Bağlı değil");
        addInfoRow(content, "Tahminler", "Veri kaynağı bekleniyor");
        pageContent.addView(content, new LinearLayout.LayoutParams(-1, -1));
    }

    private LinearLayout infoPanel(String heading, String body) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(round(PANEL, 16, LINE));
        panel.addView(text(heading, 10, GREEN, true));
        addText(panel, body, 13, WHITE, false, 10);
        return panel;
    }

    private void addFeatureRow(LinearLayout content, String number, String heading, String body) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setBackground(round(PANEL, 14, LINE));
        TextView badge = text(number, 13, BLUE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(PANEL_LIGHT, 10, LINE));
        row.addView(badge, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(12), 0, 0, 0);
        words.addView(text(heading, 14, WHITE, true));
        addText(words, body, 12, MUTED, false, 4);
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10);
        content.addView(row, lp);
    }

    private LinearLayout teamColumn(String name) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        TextView badge = text(initials(name), 14, WHITE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(Color.rgb(34, 49, 73), 50, LINE));
        column.addView(badge, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView label = text(name, 12, WHITE, true);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-1, -2);
        labelLp.topMargin = dp(8);
        column.addView(label, labelLp);
        return column;
    }

    private LinearLayout bottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(dp(6), dp(7), dp(6), dp(7));
        nav.setBackground(round(PANEL, 0, LINE));
        String[] labels = {"Maçlar", "Arşiv", "Tahminler", "Sanal Kupon", "Profil"};
        String[] icons = {"⌂", "▤", "◇", "🎟", "◉"};
        for (int i = 0; i < labels.length; i++) {
            final String tab = labels[i];
            boolean selected = tab.equals(activeTab) && selectedMatch == null;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setClickable(true);
            TextView icon = text(icons[i], 19, selected ? GREEN : MUTED, true);
            TextView label = text(labels[i], 10, selected ? GREEN : MUTED, selected);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-2, -2);
            labelLp.topMargin = dp(3);
            item.addView(icon);
            item.addView(label, labelLp);
            item.setOnClickListener(v -> {
                selectedMatch = null;
                activeTab = tab;
                renderPage();
                updateNav();
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(54), 1));
        }
        return nav;
    }

    private void updateNav() {
        if (root != null) {
            View old = root.getChildAt(root.getChildCount() - 1);
            root.removeView(old);
            root.addView(bottomNav(), new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private void addText(LinearLayout parent, String value, float size, int color, boolean bold, int topMargin) {
        TextView view = text(value, size, color, bold);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(topMargin);
        parent.addView(view, lp);
    }

    private void addDivider(LinearLayout parent, int top, int bottom) {
        View divider = new View(this);
        divider.setBackgroundColor(LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.topMargin = dp(top);
        lp.bottomMargin = dp(bottom);
        parent.addView(divider, lp);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable round(int color, int radius, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        if (strokeColor != 0) shape.setStroke(dp(1), strokeColor);
        return shape;
    }

    private String initials(String name) {
        String[] parts = name.split(" ");
        if (parts.length > 1) return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
