import React, { useEffect, useRef, useState } from "react";
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { apiRequest } from "./api";

type Prediction = {
  match: { home: string; away: string };
  source: string;
  training_matches: number;
  expected_goals: { home: number; away: number };
  probabilities: { home_win: number; draw: number; away_win: number; over_2_5: number; under_2_5: number; both_teams_to_score: number; top_scores: { score: string; probability: number }[] };
};
type Backtest = { evaluated_matches: number; accuracy: number | null; mean_multiclass_brier_score: number | null; mean_log_loss: number | null; baselines: { historical_majority_outcome_accuracy: number | null } };
const percent = (value: number) => `${(value * 100).toFixed(1)}%`;

export default function PredictionLab() {
  const [teams, setTeams] = useState<string[]>([]);
  const [home, setHome] = useState("");
  const [away, setAway] = useState("");
  const [prediction, setPrediction] = useState<Prediction | null>(null);
  const [backtest, setBacktest] = useState<Backtest | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [testing, setTesting] = useState(false);
  const mounted = useRef(true);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  useEffect(() => {
    let active = true;
    setLoading(true); setError("");
    apiRequest<{ teams: string[] }>("/v1/historical/teams").then(data => {
      if (!active) return;
      if (!Array.isArray(data.teams) || data.teams.length < 2) throw new Error("Tahmin için yeterli takım verisi yok.");
      setTeams(data.teams); setHome(data.teams[0]); setAway(data.teams[1]);
    }).catch(e => { if (active) setError(e.message); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [attempt]);

  async function predict() {
    setLoading(true); setError(""); setPrediction(null);
    try {
      const data = await apiRequest<Prediction>("/v1/historical/predictions", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ home_team: home, away_team: away }) });
      if (mounted.current) setPrediction(data);
    } catch (e) { if (mounted.current) setError(e instanceof Error ? e.message : "Tahmin alınamadı."); }
    finally { if (mounted.current) setLoading(false); }
  }
  async function evaluate() {
    setTesting(true); setError("");
    try {
      const data = await apiRequest<Backtest>("/v1/historical/backtest?max_test_matches=100");
      if (mounted.current) setBacktest(data);
    } catch (e) { if (mounted.current) setError(e instanceof Error ? e.message : "Test sonuçları alınamadı."); }
    finally { if (mounted.current) setTesting(false); }
  }
  const select = (team: string, side: "home" | "away") => { setPrediction(null); side === "home" ? setHome(team) : setAway(team); };
  return <View style={s.card}>
    <Text style={s.title}>TAHMİN LABORATUVARI</Text>
    <Text style={s.body}>Geçmiş Süper Lig sonuçlarından hesaplanan istatistiksel tahmin. Model: sezon ağırlıklı Poisson.</Text>
    {teams.length > 0 && <>
      {(["home", "away"] as const).map(side => <View key={side}>
        <Text style={s.label}>{side === "home" ? "EV SAHİBİ" : "DEPLASMAN"}</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          {teams.map(team => <TouchableOpacity accessibilityRole="button" accessibilityState={{ selected: (side === "home" ? home : away) === team, disabled: loading }} disabled={loading} key={team} onPress={() => select(team, side)} style={[s.chip, (side === "home" ? home : away) === team && s.active]}><Text style={s.text}>{team}</Text></TouchableOpacity>)}
        </ScrollView>
      </View>)}
      {home === away && <Text style={s.warning}>İki farklı takım seç.</Text>}
      <TouchableOpacity accessibilityRole="button" disabled={loading || home === away} onPress={predict} style={[s.button, (loading || home === away) && s.disabled]}><Text style={s.buttonText}>{loading ? "Veriler inceleniyor…" : "Tahmini hesapla"}</Text></TouchableOpacity>
    </>}
    {loading && !teams.length && <Text style={s.body}>Takımlar yükleniyor… Sunucunun açılması biraz sürebilir.</Text>}
    {!!error && <Text accessibilityRole="alert" style={s.warning}>{error}</Text>}
    {!teams.length && !loading && <TouchableOpacity onPress={() => setAttempt(x => x + 1)} style={s.button}><Text style={s.buttonText}>Yeniden dene</Text></TouchableOpacity>}
    {prediction && <View>
      <Text style={s.title}>{prediction.match.home} – {prediction.match.away}</Text>
      <Text style={s.body}>{prediction.training_matches} geçmiş maç kullanıldı.</Text>
      {([
        ["Ev sahibi kazanır", prediction.probabilities.home_win], ["Beraberlik", prediction.probabilities.draw], ["Deplasman kazanır", prediction.probabilities.away_win],
        ["2,5 gol üstü", prediction.probabilities.over_2_5], ["2,5 gol altı", prediction.probabilities.under_2_5], ["Karşılıklı gol", prediction.probabilities.both_teams_to_score],
      ] as [string, number][]).map(([label, value]) => <View key={label} style={s.row}><Text style={s.text}>{label}</Text><Text style={s.value}>{percent(value)}</Text></View>)}
      <Text style={s.body}>Beklenen gol: {prediction.expected_goals.home} – {prediction.expected_goals.away}</Text>
      <Text style={s.label}>OLASI SKORLAR</Text>
      {prediction.probabilities.top_scores.map(item => <Text key={item.score} style={s.body}>{item.score} · {percent(item.probability)}</Text>)}
      <Text style={s.body}>Kaynak: {prediction.source}</Text>
      <Text style={s.warning}>Olasılıklar garanti değildir. Sakatlık, kadro ve canlı maç verileri bu modele dahil değildir.</Text>
    </View>}
    <TouchableOpacity accessibilityRole="button" disabled={testing} onPress={evaluate} style={[s.chip, testing && s.disabled]}><Text style={s.text}>{testing ? "Geçmiş test çalışıyor…" : "Modelin geçmiş performansını ölç"}</Text></TouchableOpacity>
    {backtest && <View>
      <Text style={s.label}>ZAMAN SIRALI GEÇMİŞ TEST</Text>
      <Text style={s.body}>{backtest.evaluated_matches} maç değerlendirildi. Her maç yalnızca kendisinden önceki tarihlerle tahmin edilir.</Text>
      <Text style={s.body}>1X2 doğruluğu: {backtest.accuracy === null ? "Yeterli veri yok" : percent(backtest.accuracy)}</Text>
      <Text style={s.body}>En sık sonucu seçme karşılaştırması: {backtest.baselines.historical_majority_outcome_accuracy === null ? "—" : percent(backtest.baselines.historical_majority_outcome_accuracy)}</Text>
      <Text style={s.body}>Brier: {backtest.mean_multiclass_brier_score ?? "—"} · Log loss: {backtest.mean_log_loss ?? "—"} (düşük değer daha iyi)</Text>
    </View>}
  </View>;
}
const s = StyleSheet.create({
  card: { backgroundColor: "#111A2A", borderRadius: 18, padding: 18, gap: 12 }, title: { color: "#F2F5FA", fontSize: 16, fontWeight: "800" },
  body: { color: "#B7C2D3", fontSize: 12, lineHeight: 19, marginVertical: 5 }, text: { color: "#F2F5FA", fontSize: 12 }, label: { color: "#71A7FF", fontSize: 11, marginVertical: 12, fontWeight: "800" },
  chip: { padding: 12, borderRadius: 12, backgroundColor: "#243149", marginRight: 8, marginVertical: 4 }, active: { backgroundColor: "#25664F" },
  button: { backgroundColor: "#35D39A", padding: 15, borderRadius: 12, alignItems: "center" }, buttonText: { color: "#062017", fontWeight: "800" }, disabled: { opacity: 0.5 },
  row: { flexDirection: "row", justifyContent: "space-between", paddingVertical: 9 }, value: { color: "#35D39A", fontWeight: "800" }, warning: { color: "#F4BC60", fontSize: 12, lineHeight: 19 },
});
