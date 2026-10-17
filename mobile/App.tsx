import React from "react";
import { SafeAreaView, StatusBar, StyleSheet, Text, View } from "react-native";

export default function App() {
  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar barStyle="light-content" backgroundColor="#080D17" />
      <View style={styles.content}>
        <View style={styles.mark}><Text style={styles.markText}>D</Text></View>
        <Text style={styles.title}>DeepScore</Text>
        <Text style={styles.subtitle}>Temiz başlangıç sürümü</Text>
        <View style={styles.status}><View style={styles.dot} /><Text style={styles.statusText}>Uygulama başarıyla açıldı</Text></View>
        <Text style={styles.note}>Bu sürüm yalnızca temel açılışı doğrulamak için hazırlanmıştır. Maç, tahmin ve diğer özellikler henüz eklenmemiştir.</Text>
        <Text style={styles.version}>SÜRÜM 3.0.0 · TEST TEMELİ</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#080D17" },
  content: { flex: 1, alignItems: "center", justifyContent: "center", paddingHorizontal: 28 },
  mark: { width: 76, height: 76, borderRadius: 24, backgroundColor: "#35D39A", alignItems: "center", justifyContent: "center", marginBottom: 22 },
  markText: { color: "#080D17", fontSize: 48, fontWeight: "900" },
  title: { color: "#F2F5FA", fontSize: 32, fontWeight: "900", letterSpacing: 0.3 },
  subtitle: { color: "#91A0B8", fontSize: 14, marginTop: 8 },
  status: { flexDirection: "row", alignItems: "center", backgroundColor: "#111A2A", borderColor: "#243149", borderWidth: 1, borderRadius: 14, paddingHorizontal: 16, paddingVertical: 13, marginTop: 32 },
  dot: { width: 8, height: 8, borderRadius: 4, backgroundColor: "#35D39A", marginRight: 10 },
  statusText: { color: "#35D39A", fontSize: 13, fontWeight: "800" },
  note: { color: "#91A0B8", fontSize: 12, lineHeight: 19, textAlign: "center", marginTop: 24, maxWidth: 300 },
  version: { color: "#59667B", fontSize: 10, fontWeight: "800", letterSpacing: 1.2, marginTop: 36 },
});
