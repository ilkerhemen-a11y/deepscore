import React, { useEffect, useMemo, useState } from "react";
import { SafeAreaView, ScrollView, StatusBar, StyleSheet, Text, TouchableOpacity, View } from "react-native";

type Match = {
  league: string;
  time: string;
  home: string;
  away: string;
  homeForm: string;
  awayForm: string;
  homeWin: number;
  draw: number;
  awayWin: number;
  note: string;
};

type Fixture = {
  round: string;
  date: string | null;
  time: string | null;
  home_team: string;
  away_team: string;
  home_score: number | null;
  away_score: number | null;
  status: "finished" | "scheduled" | "result_unavailable";
};

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? "https://deepscore-api.onrender.com";

const MATCHES: Match[] = [
  { league: "SÜPER LİG", time: "20:00", home: "Galatasaray", away: "Fenerbahçe", homeForm: "G G B G M", awayForm: "G G G B M", homeWin: 43, draw: 29, awayWin: 28, note: "Örnek ekran verisi · canlı veri bağlı değil" },
  { league: "PREMIER LEAGUE", time: "18:30", home: "Arsenal", away: "Liverpool", homeForm: "G B G G G", awayForm: "G G M G B", homeWin: 38, draw: 27, awayWin: 35, note: "Örnek ekran verisi · canlı veri bağlı değil" },
  { league: "LA LIGA", time: "21:00", home: "Real Madrid", away: "Barcelona", homeForm: "G G G M G", awayForm: "G B G G M", homeWin: 41, draw: 26, awayWin: 33, note: "Örnek ekran verisi · canlı veri bağlı değil" }
];

const COLORS = { bg: "#080D17", panel: "#111A2A", border: "#243149", text: "#F2F5FA", muted: "#91A0B8", green: "#35D39A", blue: "#71A7FF", amber: "#F4BC60" };

export default function App() {
  const [tab, setTab] = useState("Maçlar");
  const [selected, setSelected] = useState<Match | null>(null);
  const [saved, setSaved] = useState<string[]>([]);
  const [fixtures, setFixtures] = useState<Fixture[]>([]);
  const [fixtureLoading, setFixtureLoading] = useState(false);
  const [fixtureError, setFixtureError] = useState("");
  const [selectedRound, setSelectedRound] = useState("1. Round");
  const today = useMemo(() => new Intl.DateTimeFormat("tr-TR", { weekday: "long", day: "numeric", month: "long" }).format(new Date()), []);

  const toggleSave = (match: Match) => setSaved((current) => current.includes(match.home) ? current.filter((x) => x !== match.home) : [...current, match.home]);

  useEffect(() => {
    if (tab !== "Fikstür") return;
    let active = true;
    setFixtureLoading(true);
    setFixtureError("");
    fetch(`${API_BASE_URL}/v1/free/turkey-super-lig/season?season=2025-26`)
      .then((response) => {
        if (!response.ok) throw new Error(`Fikstür verisi alınamadı (HTTP ${response.status}).`);
        return response.json();
      })
      .then((data) => {
        if (!active) return;
        const items: Fixture[] = Array.isArray(data.fixtures) ? data.fixtures : [];
        setFixtures(items);
        if (items.length > 0 && !items.some((item) => item.round === selectedRound)) setSelectedRound(items[0].round);
        if (items.length === 0) setFixtureError("Kaynakta bu sezon için maç bulunamadı.");
      })
      .catch((error: unknown) => {
        if (active) setFixtureError(error instanceof Error ? error.message : "Fikstür yüklenirken bir hata oluştu.");
      })
      .finally(() => { if (active) setFixtureLoading(false); });
    return () => { active = false; };
  }, [tab]);

  const fixtureRounds = Array.from(new Set(fixtures.map((fixture) => fixture.round))).sort(
    (a, b) => Number(a.match(/\d+/)?.[0] ?? 0) - Number(b.match(/\d+/)?.[0] ?? 0)
  );
  const roundFixtures = fixtures.filter((fixture) => fixture.round === selectedRound);

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" backgroundColor={COLORS.bg} />
      <View style={styles.header}>
        <View>
          <Text style={styles.brand}>Deep<Text style={styles.brandAccent}>Score</Text></Text>
          <Text style={styles.subtitle}>FUTBOL VERİSİ • DAHA DERİN ANALİZ</Text>
        </View>
        <View style={styles.statusPill}><View style={styles.statusDot} /><Text style={styles.statusText}>BETA</Text></View>
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.date}>{today.toLocaleUpperCase("tr-TR")}</Text>
        <Text style={styles.title}>{selected ? "Maç analizi" : tab === "Maçlar" ? "Maç merkezi" : tab}</Text>
        <Text style={styles.description}>{selected ? selected.home + " – " + selected.away : "İstatistikleri incele, modelleri karşılaştır, tahminini test et."}</Text>

        {selected ? (
          <View style={styles.card}>
            <TouchableOpacity onPress={() => setSelected(null)}><Text style={styles.back}>‹  TÜM MAÇLAR</Text></TouchableOpacity>
            <Text style={styles.league}>{selected.league} · {selected.time}</Text>
            <Text style={styles.matchTitle}>{selected.home}</Text>
            <Text style={styles.vs}>VS</Text>
            <Text style={styles.matchTitle}>{selected.away}</Text>
            <Text style={styles.sectionTitle}>1X2 OLASILIK TAHMİNİ</Text>
            <View style={styles.probRow}>
              <Probability label="1" value={selected.homeWin} />
              <Probability label="X" value={selected.draw} />
              <Probability label="2" value={selected.awayWin} />
            </View>
            <Text style={styles.disclaimer}>Bu değerler yalnızca arayüz demosu için örnektir; gerçek analiz veya canlı maç verisi değildir.</Text>
            <Text style={styles.sectionTitle}>SON FORM (ÖRNEK)</Text>
            <View style={styles.formRow}><Text style={styles.teamLabel}>{selected.home}</Text><Text style={styles.form}>{selected.homeForm}</Text></View>
            <View style={styles.formRow}><Text style={styles.teamLabel}>{selected.away}</Text><Text style={styles.form}>{selected.awayForm}</Text></View>
            <TouchableOpacity style={styles.primaryButton} onPress={() => toggleSave(selected)}>
              <Text style={styles.primaryButtonText}>{saved.includes(selected.home) ? "✓ Sanal tahmin listesinde" : "＋ Sanal tahmin listeme ekle"}</Text>
            </TouchableOpacity>
          </View>
        ) : tab === "Maçlar" ? (
          <>
            <View style={styles.hero}>
              <Text style={styles.heroEyebrow}>ANALİZ MASASI</Text>
              <Text style={styles.heroTitle}>Sonuçlara değil,{ "\n" }verilere odaklan.</Text>
              <Text style={styles.heroText}>Geçmiş performans, gol istatistikleri ve model karşılaştırmaları tek yerde.</Text>
              <View style={styles.heroStats}><Stat value="3" label="Tahmin modeli hedefi" /><Stat value="6" label="Başlangıç ligi hedefi" /><Stat value="₺0" label="Gerçek para" /></View>
            </View>
            <View style={styles.sectionHeader}><Text style={styles.sectionTitle}>ÖNE ÇIKAN MAÇLAR</Text><Text style={styles.smallMuted}>DEMO</Text></View>
            {MATCHES.map((match) => <TouchableOpacity key={match.home} style={styles.matchCard} onPress={() => setSelected(match)}>
              <View style={styles.matchMeta}><Text style={styles.league}>{match.league}</Text><Text style={styles.time}>{match.time}</Text></View>
              <View style={styles.teams}><View style={styles.teamCircle}><Text style={styles.teamInitial}>{match.home.slice(0, 1)}</Text></View><Text style={styles.teamName}>{match.home}</Text><Text style={styles.vsSmall}>VS</Text><Text style={styles.teamName}>{match.away}</Text><View style={styles.teamCircle}><Text style={styles.teamInitial}>{match.away.slice(0, 1)}</Text></View></View>
              <View style={styles.cardFooter}><Text style={styles.cardHint}>Maç istatistiklerini görüntüle</Text><Text style={styles.arrow}>›</Text></View>
            </TouchableOpacity>)}
            <View style={styles.notice}><Text style={styles.noticeTitle}>ℹ  Veri durumu</Text><Text style={styles.noticeText}>Şu an ekranda örnek veriler var. Gerçek fikstür ve tarihsel sonuçlar, veri sağlayıcısı bağlandıktan sonra gösterilecek.</Text></View>
          </>
        ) : tab === "Fikstür" ? (
          <View>
            <View style={styles.seasonBanner}>
              <View>
                <Text style={styles.seasonLabel}>TRENDYOL SÜPER LİG</Text>
                <Text style={styles.seasonTitle}>2025–2026 Sezonu</Text>
              </View>
              <View style={styles.countBadge}><Text style={styles.countBadgeText}>{fixtures.length || "—"} MAÇ</Text></View>
            </View>
            <Text style={styles.fixtureSource}>34 hafta · Maç tarihleri ve sonuçlar arşiv verisinden yüklenir.</Text>
            {fixtureLoading ? <View style={styles.card}><Text style={styles.body}>2025–2026 fikstürü yükleniyor…</Text></View> : null}
            {fixtureError ? <View style={styles.notice}><Text style={styles.noticeTitle}>Fikstür yüklenemedi</Text><Text style={styles.noticeText}>{fixtureError} İnternet bağlantısını veya API adresini kontrol et.</Text></View> : null}
            {!fixtureLoading && !fixtureError && fixtures.length > 0 ? (
              <>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.roundScroller}>
                  {fixtureRounds.map((round) => (
                    <TouchableOpacity key={round} onPress={() => setSelectedRound(round)} style={[styles.roundChip, selectedRound === round && styles.roundChipActive]}>
                      <Text style={[styles.roundChipText, selectedRound === round && styles.roundChipTextActive]}>{round.replace(" Round", " Hafta").replace(". Round", ". Hafta")}</Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
                <View style={styles.sectionHeader}>
                  <Text style={styles.sectionTitle}>{selectedRound.replace(" Round", " Hafta").replace(". Round", ". Hafta").toLocaleUpperCase("tr-TR")}</Text>
                  <Text style={styles.smallMuted}>{roundFixtures.length} MAÇ</Text>
                </View>
                {roundFixtures.map((fixture, index) => (
                  <View key={`${fixture.date}-${fixture.home_team}-${fixture.away_team}-${index}`} style={styles.fixtureCard}>
                    <View style={styles.fixtureMeta}>
                      <Text style={styles.fixtureDate}>{fixture.date ? new Date(fixture.date + "T12:00:00").toLocaleDateString("tr-TR", { day: "numeric", month: "short" }) : "Tarih yok"}</Text>
                      <Text style={styles.fixtureStatus}>{fixture.status === "finished" ? "TAMAMLANDI" : fixture.status === "scheduled" ? "PROGRAMDA" : "SONUÇ KAYNAKTA YOK"}</Text>
                    </View>
                    <View style={styles.fixtureTeams}>
                      <Text numberOfLines={2} style={styles.fixtureTeam}>{fixture.home_team}</Text>
                      <View style={styles.fixtureScoreBox}>
                        {fixture.status === "finished" ? <Text style={styles.fixtureScore}>{fixture.home_score} : {fixture.away_score}</Text> : <Text style={styles.fixtureTime}>{fixture.status === "scheduled" ? (fixture.time || "—") : "—"}</Text>}
                      </View>
                      <Text numberOfLines={2} style={[styles.fixtureTeam, styles.fixtureAway]}>{fixture.away_team}</Text>
                    </View>
                    <Text style={styles.fixtureTimeLine}>{fixture.time ? `Başlama: ${fixture.time}` : "Başlama saati belirtilmemiş"}</Text>
                  </View>
                ))}
                <Text style={styles.fixtureFootnote}>Veri kaynağı: OpenFootball. Saatleri resmî TFF kayıtlarıyla karşılaştırmak faydalı olabilir.</Text>
              </>
            ) : null}
          </View>
        ) : tab === "Tahmin" ? (
          <View style={styles.card}><Text style={styles.sectionTitle}>MODEL LABORATUVARI</Text><Text style={styles.body}>İlk aşamada 1X2 olasılık modeli, gol dağılımı modeli ve makine öğrenmesi modeli karşılaştırılacak.</Text><Text style={styles.body}>Her model zaman sıralı geçmiş testlerde değerlendirilecek. Yeterli veri yoksa sonuçlar gösterilmeyecek.</Text><View style={styles.pill}><Text style={styles.pillText}>GELİŞTİRME AŞAMASINDA</Text></View></View>
        ) : tab === "Sanal kupon" ? (
          <View style={styles.card}><Text style={styles.sectionTitle}>SANAL TAHMİNLER</Text><Text style={styles.bigNumber}>{saved.length}</Text><Text style={styles.body}>Listene eklediğin maçlar</Text>{saved.length === 0 ? <Text style={styles.mutedBody}>Maç detayından “Sanal tahmin listeme ekle” seçeneğini kullan.</Text> : saved.map((name) => <Text key={name} style={styles.listItem}>•  {name}</Text>)}<Text style={styles.disclaimer}>Bu sürümde gerçek para, para yatırma veya para çekme özelliği yoktur.</Text></View>
        ) : (
          <View style={styles.card}><Text style={styles.sectionTitle}>PERFORMANSIN</Text><Text style={styles.body}>Tahmin geçmişi ve model başarı ölçümleri, gerçek veri bağlantısı ve kayıt sistemi tamamlandığında burada yer alacak.</Text><View style={styles.pill}><Text style={styles.pillText}>HENÜZ KAYIT YOK</Text></View></View>
        )}
      </ScrollView>
      <View style={styles.tabBar}>{["Maçlar", "Fikstür", "Tahmin", "Sanal kupon", "Profil"].map((item) => <TouchableOpacity key={item} onPress={() => { setTab(item); setSelected(null); }} style={styles.tabItem}><Text style={[styles.tabText, tab === item && styles.tabActive]}>{item}</Text><View style={[styles.tabIndicator, tab === item && styles.tabIndicatorActive]} /></TouchableOpacity>)}</View>
    </SafeAreaView>
  );
}

function Probability({ label, value }: { label: string; value: number }) {
  return <View style={styles.probBox}><Text style={styles.probLabel}>{label}</Text><Text style={styles.probValue}>{value}%</Text><View style={styles.track}><View style={[styles.fill, { width: (value + "%") as `${number}%` }]} /></View></View>;
}

function Stat({ value, label }: { value: string; label: string }) {
  return <View style={styles.stat}><Text style={styles.statValue}>{value}</Text><Text style={styles.statLabel}>{label}</Text></View>;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: COLORS.bg },
  header: { paddingHorizontal: 20, paddingTop: 12, paddingBottom: 16, flexDirection: "row", justifyContent: "space-between", alignItems: "center", borderBottomWidth: 1, borderBottomColor: "#182338" },
  brand: { color: COLORS.text, fontSize: 27, fontWeight: "900", letterSpacing: -1 },
  brandAccent: { color: COLORS.green },
  subtitle: { color: COLORS.muted, fontSize: 9, letterSpacing: 1.4, marginTop: 2 },
  statusPill: { borderColor: "#2B795F", borderWidth: 1, borderRadius: 20, paddingHorizontal: 10, paddingVertical: 6, flexDirection: "row", alignItems: "center", gap: 6 },
  statusDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: COLORS.green },
  statusText: { color: COLORS.green, fontSize: 10, fontWeight: "800" },
  content: { padding: 18, paddingBottom: 28 },
  date: { color: COLORS.green, fontSize: 10, fontWeight: "800", letterSpacing: 1.5, marginTop: 4 },
  title: { color: COLORS.text, fontSize: 28, fontWeight: "800", marginTop: 7, letterSpacing: -0.7 },
  description: { color: COLORS.muted, fontSize: 13, lineHeight: 19, marginTop: 7, marginBottom: 20 },
  hero: { backgroundColor: "#122238", borderRadius: 20, padding: 20, borderWidth: 1, borderColor: "#28425E", marginBottom: 25 },
  heroEyebrow: { color: COLORS.green, fontSize: 10, fontWeight: "900", letterSpacing: 1.5 },
  heroTitle: { color: COLORS.text, fontSize: 27, fontWeight: "900", lineHeight: 32, marginTop: 10 },
  heroText: { color: "#B4C3D9", fontSize: 13, lineHeight: 19, marginTop: 10 },
  heroStats: { flexDirection: "row", marginTop: 20, paddingTop: 16, borderTopWidth: 1, borderTopColor: "#2A3D57" },
  stat: { flex: 1 },
  statValue: { color: COLORS.green, fontSize: 18, fontWeight: "900" },
  statLabel: { color: COLORS.muted, fontSize: 9, lineHeight: 13, marginTop: 4, paddingRight: 6 },
  sectionHeader: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 12 },
  sectionTitle: { color: COLORS.text, fontSize: 11, fontWeight: "900", letterSpacing: 1.1, marginTop: 6, marginBottom: 13 },
  smallMuted: { color: COLORS.muted, fontSize: 9, fontWeight: "800" },
  matchCard: { backgroundColor: COLORS.panel, borderRadius: 16, borderWidth: 1, borderColor: COLORS.border, padding: 15, marginBottom: 12 },
  matchMeta: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  league: { color: COLORS.green, fontSize: 9, fontWeight: "900", letterSpacing: 1 },
  time: { color: COLORS.amber, fontSize: 12, fontWeight: "800" },
  teams: { flexDirection: "row", alignItems: "center", marginTop: 16, marginBottom: 16, gap: 8 },
  teamCircle: { width: 28, height: 28, borderRadius: 14, backgroundColor: "#202E43", alignItems: "center", justifyContent: "center" },
  teamInitial: { color: COLORS.blue, fontWeight: "900", fontSize: 12 },
  teamName: { color: COLORS.text, fontSize: 12, fontWeight: "700", flexShrink: 1 },
  vsSmall: { color: COLORS.muted, fontSize: 9, fontWeight: "800" },
  cardFooter: { borderTopWidth: 1, borderTopColor: "#263349", paddingTop: 11, flexDirection: "row", justifyContent: "space-between" },
  cardHint: { color: COLORS.muted, fontSize: 11 },
  arrow: { color: COLORS.green, fontSize: 20, lineHeight: 18 },
  notice: { backgroundColor: "#1B2432", borderRadius: 13, padding: 14, marginTop: 7, borderWidth: 1, borderColor: "#3B4658" },
  noticeTitle: { color: COLORS.amber, fontSize: 12, fontWeight: "800", marginBottom: 5 },
  noticeText: { color: "#B7C2D3", fontSize: 11, lineHeight: 17 },
  seasonBanner: { backgroundColor: "#122238", borderRadius: 16, borderWidth: 1, borderColor: "#28425E", padding: 16, flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 8 },
  seasonLabel: { color: COLORS.green, fontSize: 9, fontWeight: "900", letterSpacing: 1.2 },
  seasonTitle: { color: COLORS.text, fontSize: 20, fontWeight: "900", marginTop: 5 },
  countBadge: { backgroundColor: "#20364A", borderRadius: 10, paddingHorizontal: 10, paddingVertical: 8 },
  countBadgeText: { color: COLORS.green, fontSize: 10, fontWeight: "900" },
  fixtureSource: { color: COLORS.muted, fontSize: 11, lineHeight: 16, marginBottom: 14 },
  roundScroller: { paddingVertical: 4, paddingBottom: 16, gap: 8 },
  roundChip: { backgroundColor: COLORS.panel, borderColor: COLORS.border, borderWidth: 1, borderRadius: 20, paddingHorizontal: 13, paddingVertical: 9 },
  roundChipActive: { backgroundColor: COLORS.green, borderColor: COLORS.green },
  roundChipText: { color: COLORS.muted, fontSize: 10, fontWeight: "800" },
  roundChipTextActive: { color: "#062017" },
  fixtureCard: { backgroundColor: COLORS.panel, borderColor: COLORS.border, borderWidth: 1, borderRadius: 14, padding: 13, marginBottom: 10 },
  fixtureMeta: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 12 },
  fixtureDate: { color: COLORS.amber, fontSize: 11, fontWeight: "800" },
  fixtureStatus: { color: COLORS.muted, fontSize: 8, fontWeight: "900", letterSpacing: 0.8 },
  fixtureTeams: { flexDirection: "row", alignItems: "center", gap: 8 },
  fixtureTeam: { color: COLORS.text, flex: 1, fontSize: 12, fontWeight: "800" },
  fixtureAway: { textAlign: "right" },
  fixtureScoreBox: { minWidth: 62, alignItems: "center", justifyContent: "center", backgroundColor: "#0B1220", borderColor: COLORS.border, borderWidth: 1, borderRadius: 9, paddingHorizontal: 8, paddingVertical: 8 },
  fixtureScore: { color: COLORS.green, fontSize: 15, fontWeight: "900" },
  fixtureTime: { color: COLORS.amber, fontSize: 11, fontWeight: "800" },
  fixtureTimeLine: { color: COLORS.muted, fontSize: 9, marginTop: 10 },
  fixtureFootnote: { color: COLORS.muted, fontSize: 10, lineHeight: 15, marginTop: 5, marginBottom: 12 },
  card: { backgroundColor: COLORS.panel, borderRadius: 18, borderWidth: 1, borderColor: COLORS.border, padding: 18, marginBottom: 16 },
  back: { color: COLORS.green, fontSize: 11, fontWeight: "900", marginBottom: 20 },
  matchTitle: { color: COLORS.text, fontSize: 23, fontWeight: "900", textAlign: "center", marginTop: 10 },
  vs: { color: COLORS.muted, fontSize: 10, textAlign: "center", fontWeight: "900", marginTop: 5 },
  probRow: { flexDirection: "row", gap: 9, marginTop: 5 },
  probBox: { flex: 1, backgroundColor: "#0B1220", borderRadius: 12, padding: 12, borderWidth: 1, borderColor: COLORS.border },
  probLabel: { color: COLORS.muted, fontSize: 10, fontWeight: "800" },
  probValue: { color: COLORS.green, fontSize: 20, fontWeight: "900", marginTop: 5 },
  track: { height: 4, backgroundColor: "#27354A", borderRadius: 3, marginTop: 9, overflow: "hidden" },
  fill: { height: 4, backgroundColor: COLORS.green, borderRadius: 3 },
  disclaimer: { color: COLORS.amber, fontSize: 10, lineHeight: 15, marginTop: 13 },
  formRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: COLORS.border },
  teamLabel: { color: COLORS.text, fontSize: 12, fontWeight: "700" },
  form: { color: COLORS.green, fontSize: 11, fontWeight: "900", letterSpacing: 1 },
  primaryButton: { backgroundColor: COLORS.green, borderRadius: 12, padding: 14, alignItems: "center", marginTop: 20 },
  primaryButtonText: { color: "#062017", fontSize: 12, fontWeight: "900" },
  body: { color: "#B7C2D3", fontSize: 13, lineHeight: 20, marginBottom: 12 },
  mutedBody: { color: COLORS.muted, fontSize: 12, lineHeight: 18, marginTop: 10 },
  bigNumber: { color: COLORS.green, fontSize: 42, fontWeight: "900", marginTop: 10 },
  listItem: { color: COLORS.text, fontSize: 13, paddingVertical: 7 },
  pill: { alignSelf: "flex-start", borderRadius: 20, backgroundColor: "#26364B", paddingHorizontal: 11, paddingVertical: 7, marginTop: 10 },
  pillText: { color: COLORS.blue, fontSize: 9, fontWeight: "900", letterSpacing: 1 },
  tabBar: { flexDirection: "row", backgroundColor: "#0D1421", borderTopWidth: 1, borderTopColor: "#202B3E", paddingTop: 11, paddingBottom: 8 },
  tabItem: { flex: 1, alignItems: "center", gap: 8 },
  tabText: { color: COLORS.muted, fontSize: 10, fontWeight: "700" },
  tabActive: { color: COLORS.green },
  tabIndicator: { height: 3, width: 24, borderRadius: 2, backgroundColor: "transparent" },
  tabIndicatorActive: { backgroundColor: COLORS.green }
});
