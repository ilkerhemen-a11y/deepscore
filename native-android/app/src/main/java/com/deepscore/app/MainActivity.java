package com.deepscore.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private LinearLayout root;
    private LinearLayout pageContent;
    private String activeTab = "Maçlar";
    private String selectedLeague = "Tümü";
    private Match selectedMatch;

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
        final String league, home, away, homeScore, awayScore, date, venue, note;
        Match(String league, String home, String away, String homeScore, String awayScore,
              String date, String venue, String note) {
            this.league = league;
            this.home = home;
            this.away = away;
            this.homeScore = homeScore;
            this.awayScore = awayScore;
            this.date = date;
            this.venue = venue;
            this.note = note;
        }
        String score() { return homeScore + " - " + awayScore; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        loadHistoricalMatches();
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageContent, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottomNav(), new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        renderPage();
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
        pageContent.addView(content, new LinearLayout.LayoutParams(-1, -1));
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
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        addHeader(content);
        addText(content, "Tahmin Merkezi", 27, WHITE, true, 26);
        addText(content, "Veriye dayalı analiz için hazırlanan bölüm.", 14, MUTED, false, 5);
        LinearLayout notice = infoPanel("VERİ DURUMU",
                "Şu anda canlı fikstür ve güncel takım istatistikleri bağlı değil. Bu nedenle gerçek maçlar için tahmin veya kazanma yüzdesi üretilmiyor.");
        LinearLayout.LayoutParams noticeLp = new LinearLayout.LayoutParams(-1, -2);
        noticeLp.topMargin = dp(20);
        content.addView(notice, noticeLp);
        addText(content, "Tahmin altyapısı", 18, WHITE, true, 24);
        addFeatureRow(content, "01", "Maç verisi", "Fikstür, takım formu ve geçmiş karşılaşmaların alınması.");
        addFeatureRow(content, "02", "İstatistik analizi", "Ev sahibi/deplasman performansı ve gol verilerinin karşılaştırılması.");
        addFeatureRow(content, "03", "Şeffaf tahmin", "Veri kaynağı, hesaplama tarihi ve belirsizlikle birlikte sonuç gösterimi.");
        TextView label = text("Bu sürümde tahminler bilerek kapalıdır; örnek değerler gerçek tahmin gibi sunulmaz.", 12, MUTED, false);
        label.setPadding(dp(14), dp(14), dp(14), dp(14));
        label.setBackground(round(PANEL, 14, LINE));
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-1, -2);
        labelLp.topMargin = dp(14);
        content.addView(label, labelLp);
        pageContent.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    }

    private void renderProfile() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(24));
        addHeader(content);
        addText(content, "Uygulama", 27, WHITE, true, 26);
        addText(content, "DeepScore sürüm bilgileri", 14, MUTED, false, 5);
        LinearLayout panel = infoPanel("DEEPSCORE ANDROID", "Geçmiş maç arşivi, lig filtreleri ve maç detayları içeren yerel Android sürümü.");
        LinearLayout.LayoutParams panelLp = new LinearLayout.LayoutParams(-1, -2);
        panelLp.topMargin = dp(20);
        content.addView(panel, panelLp);
        addInfoRow(content, "Sürüm", "0.5.0");
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
        String[] labels = {"Maçlar", "Arşiv", "Tahminler", "Profil"};
        String[] icons = {"⌂", "▤", "◇", "◉"};
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
