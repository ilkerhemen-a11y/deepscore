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

public final class MainActivity extends Activity {
    private LinearLayout matchesContainer;
    private TextView emptyState;
    private TextView sectionTitle;
    private static final int BG = Color.rgb(8, 13, 25);
    private static final int PANEL = Color.rgb(18, 27, 45);
    private static final int PANEL_LIGHT = Color.rgb(24, 36, 57);
    private static final int WHITE = Color.rgb(244, 247, 252);
    private static final int MUTED = Color.rgb(150, 164, 185);
    private static final int GREEN = Color.rgb(53, 211, 154);
    private static final int LINE = Color.rgb(42, 56, 79);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView logo = text("DeepScore", 25, WHITE, true);
        TextView subtitle = text("GEÇMİŞ MAÇLAR • KESİNLEŞMİŞ SKORLAR", 9, MUTED, true);
        subtitle.setLetterSpacing(0.13f);
        brand.addView(logo);
        brand.addView(subtitle);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView profile = text("DS", 14, GREEN, true);
        profile.setGravity(Gravity.CENTER);
        profile.setBackground(round(PANEL_LIGHT, 40, LINE));
        header.addView(profile, new LinearLayout.LayoutParams(dp(44), dp(44)));
        content.addView(header);

        TextView welcome = text("Maç Merkezi", 27, WHITE, true);
        LinearLayout.LayoutParams welcomeLp = new LinearLayout.LayoutParams(-1, -2);
        welcomeLp.topMargin = dp(28);
        content.addView(welcome, welcomeLp);

        TextView intro = text("Daha önce oynanmış maçlar ve doğrulanmış sonuçlar.", 14, MUTED, false);
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(-1, -2);
        introLp.topMargin = dp(5);
        content.addView(intro, introLp);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.setGravity(Gravity.CENTER_VERTICAL);
        String[] filterLabels = {"Tümü", "Süper Lig", "La Liga"};
        TextView[] filterChips = new TextView[filterLabels.length];
        for (int i = 0; i < filterLabels.length; i++) {
            final int selectedIndex = i;
            TextView chip = text(filterLabels[i], 12, i == 0 ? BG : MUTED, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(15), dp(9), dp(15), dp(9));
            chip.setBackground(round(i == 0 ? GREEN : PANEL_LIGHT, 24, i == 0 ? GREEN : LINE));
            chip.setOnClickListener(v -> {
                for (int j = 0; j < filterChips.length; j++) {
                    boolean selected = j == selectedIndex;
                    filterChips[j].setTextColor(selected ? BG : MUTED);
                    filterChips[j].setBackground(round(selected ? GREEN : PANEL_LIGHT, 24, selected ? GREEN : LINE));
                }
                String selectedLeague = selectedIndex == 1 ? "SÜPER LİG" : selectedIndex == 2 ? "LA LIGA" : "";
                int shown = 0;
                for (int k = 0; k < matchesContainer.getChildCount(); k++) {
                    View card = matchesContainer.getChildAt(k);
                    boolean visible = selectedLeague.isEmpty() || selectedLeague.equals((String) card.getTag());
                    card.setVisibility(visible ? View.VISIBLE : View.GONE);
                    if (visible) shown++;
                }
                emptyState.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
            });
            filterChips[i] = chip;
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(-2, -2);
            chipLp.rightMargin = dp(8);
            filters.addView(chip, chipLp);
        }
        LinearLayout.LayoutParams filtersLp = new LinearLayout.LayoutParams(-1, -2);
        filtersLp.topMargin = dp(18);
        content.addView(filters, filtersLp);

        LinearLayout section = new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        sectionTitle = text("Sonuçlanmış Maçlar", 18, WHITE, true);
        section.addView(sectionTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView sample = text("TAMAMLANDI", 9, GREEN, true);
        sample.setPadding(dp(9), dp(6), dp(9), dp(6));
        sample.setBackground(round(Color.rgb(15, 57, 50), 30, Color.rgb(25, 99, 77)));
        section.addView(sample);
        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(-1, -2);
        sectionLp.topMargin = dp(24);
        sectionLp.bottomMargin = dp(12);
        content.addView(section, sectionLp);

        matchesContainer = new LinearLayout(this);
        matchesContainer.setOrientation(LinearLayout.VERTICAL);
        matchesContainer.addView(matchCard("SÜPER LİG", "Galatasaray", "Fenerbahçe", "0", "1", "19 Mayıs 2024"));
        LinearLayout.LayoutParams card2 = new LinearLayout.LayoutParams(-1, -2);
        card2.topMargin = dp(12);
        matchesContainer.addView(matchCard("LA LIGA", "Real Madrid", "Barcelona", "3", "2", "21 Nisan 2024"), card2);
        content.addView(matchesContainer);

        emptyState = text("Bu lig filtresinde gösterilecek geçmiş maç bulunamadı.", 13, MUTED, false);
        emptyState.setGravity(Gravity.CENTER);
        emptyState.setPadding(dp(18), dp(24), dp(18), dp(24));
        emptyState.setBackground(round(PANEL, 16, LINE));
        emptyState.setVisibility(View.GONE);
        LinearLayout.LayoutParams emptyLp = new LinearLayout.LayoutParams(-1, -2);
        emptyLp.topMargin = dp(12);
        content.addView(emptyState, emptyLp);

        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottomNav());
        setContentView(root);
    }

    private View matchCard(String league, String home, String away, String homeScore, String awayScore, String date) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.setBackground(round(PANEL, 18, LINE));
        card.setTag(league);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView leagueLabel = text(league, 10, GREEN, true);
        leagueLabel.setLetterSpacing(0.08f);
        top.addView(leagueLabel, new LinearLayout.LayoutParams(0, -2, 1));
        TextView timeLabel = text(date, 11, MUTED, true);
        top.addView(timeLabel);
        card.addView(top);

        View divider = new View(this);
        divider.setBackgroundColor(LINE);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(-1, dp(1));
        dividerLp.topMargin = dp(12);
        dividerLp.bottomMargin = dp(12);
        card.addView(divider, dividerLp);

        LinearLayout teams = new LinearLayout(this);
        teams.setGravity(Gravity.CENTER_VERTICAL);
        teams.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout homeColumn = teamColumn(home);
        LinearLayout awayColumn = teamColumn(away);
        teams.addView(homeColumn, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout scoreBox = new LinearLayout(this);
        scoreBox.setOrientation(LinearLayout.HORIZONTAL);
        scoreBox.setGravity(Gravity.CENTER);
        TextView homeGoals = text(homeScore, 21, WHITE, true);
        TextView dash = text(" - ", 15, MUTED, true);
        TextView awayGoals = text(awayScore, 21, WHITE, true);
        scoreBox.addView(homeGoals);
        scoreBox.addView(dash);
        scoreBox.addView(awayGoals);
        scoreBox.setPadding(dp(6), dp(5), dp(6), dp(5));
        scoreBox.setBackground(round(PANEL_LIGHT, 12, LINE));
        teams.addView(scoreBox, new LinearLayout.LayoutParams(-2, dp(42)));
        LinearLayout.LayoutParams awayLp = new LinearLayout.LayoutParams(0, -2, 1);
        awayLp.leftMargin = dp(8);
        teams.addView(awayColumn, awayLp);
        card.addView(teams);
        TextView finished = text("✓  MAÇ TAMAMLANDI", 10, GREEN, true);
        finished.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams finishedLp = new LinearLayout.LayoutParams(-1, -2);
        finishedLp.topMargin = dp(12);
        card.addView(finished, finishedLp);
        return card;
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
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setBackground(round(PANEL, 0, LINE));
        String[] labels = {"Ana Sayfa", "Canlı", "Tahminler", "Profil"};
        for (int i = 0; i < labels.length; i++) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            TextView icon = text(new String[]{"⌂", "●", "▤", "◉"}[i], 19, i == 0 ? GREEN : MUTED, true);
            TextView label = text(labels[i], 10, i == 0 ? GREEN : MUTED, i == 0);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-2, -2);
            labelLp.topMargin = dp(3);
            item.addView(icon);
            item.addView(label, labelLp);
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(54), 1));
        }
        return nav;
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
