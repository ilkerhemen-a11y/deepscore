import React, { useState } from "react";
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

const C = {
  bg: "#080D17",
  panel: "#111A2A",
  border: "#243149",
  text: "#F2F5FA",
  muted: "#91A0B8",
  green: "#35D39A",
  blue: "#71A7FF",
  amber: "#F4BC60",
};

const TABS = ["Maçlar", "Tahmin", "Sanal Kupon", "Profil"];

export default function App() {
  const [tab, setTab] = useState("Maçlar");

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" backgroundColor={C.bg} />
      <View style={styles.header}>
        <View style={styles.brandRow}>
          <View style={styles.logo}><Text style={styles.logoText}>D</Text></View>
          <View>
            <Text style={styles.brand}>DeepScore</Text>
            <Text style={styles.tagline}>VERİYLE ANALİZ ET</Text>
          </View>
        </View>
        <View style={styles.liveBadge}><View style={styles.dot} /><Text style={styles.liveText}>ANALİZ MERKEZİ</Text></View>
      </View>

      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        {tab === "Maçlar" && (
          <>
            <Text style={styles.eyebrow}>MAÇ MERKEZİ</Text>
            <Text style={styles.title}>Merhaba!</Text>
            <Text style={styles.subtitle}>Karşılaşmaları incele, istatistikleri keşfet.</Text>

            <View style={styles.featureCard}>
              <View style={styles.cardTop}>
                <Text style={styles.league}>ÖRNEK KARŞILAŞMA · SÜPER LİG</Text>
                <View style={styles.sampleBadge}><Text style={styles.sampleText}>ÖRNEK</Text></View>
              </View>
              <View style={styles.teams}>
                <View style={styles.team}>
                  <View style={styles.teamBadge}><Text style={styles.teamInitial}>G</Text></View>
                  <Text style={styles.teamName}>Galatasaray</Text>
                </View>
                <Text style={styles.vs}>VS</Text>
                <View style={styles.team}>
                  <View style={[styles.teamBadge, styles.awayBadge]}><Text style={styles.teamInitial}>F</Text></View>
                  <Text style={styles.teamName}>Fenerbahçe</Text>
                </View>
              </View>
              <View style={styles.divider} />
              <Text style={styles.disclaimer}>Bu ekran tasarım örneğidir. Gerçek maç verileri ve tahminler sonraki aşamada bağlanacaktır.</Text>
              <TouchableOpacity style={styles.primaryButton} onPress={() => setTab("Tahmin")} activeOpacity={0.8}>
                <Text style={styles.primaryButtonText}>Analiz ekranına git  ›</Text>
              </TouchableOpacity>
            </View>

            <Text style={styles.sectionTitle}>DeepScore nasıl çalışır?</Text>
            <View style={styles.infoRow}>
              <View style={styles.infoIcon}><Text style={styles.infoIconText}>01</Text></View>
              <View style={styles.infoCopy}><Text style={styles.infoTitle}>Maçı seç</Text><Text style={styles.infoText}>İncelemek istediğin karşılaşmayı belirle.</Text></View>
            </View>
            <View style={styles.infoRow}>
              <View style={styles.infoIcon}><Text style={styles.infoIconText}>02</Text></View>
              <View style={styles.infoCopy}><Text style={styles.infoTitle}>Verileri incele</Text><Text style={styles.infoText}>Takım performansı ve olasılıkları görüntüle.</Text></View>
            </View>
            <View style={styles.infoRow}>
              <View style={styles.infoIcon}><Text style={styles.infoIconText}>03</Text></View>
              <View style={styles.infoCopy}><Text style={styles.infoTitle}>Kendi kararını ver</Text><Text style={styles.infoText}>Tahminleri kesin sonuç olarak değerlendirme.</Text></View>
            </View>
          </>
        )}

        {tab === "Tahmin" && (
          <>
            <Text style={styles.eyebrow}>VERİ ANALİZİ</Text>
            <Text style={styles.title}>Tahmin merkezi</Text>
            <Text style={styles.subtitle}>Analiz altyapısını adım adım kuruyoruz.</Text>
            <View style={styles.panel}>
              <Text style={styles.panelTitle}>Tahmin motoru hazırlanıyor</Text>
              <Text style={styles.body}>Bu ilk kararlı sürümde açılış ve gezinme altyapısını doğruluyoruz. Maç verisi, takım istatistikleri ve olasılık hesaplamaları sonraki güncellemelerde eklenecek.</Text>
              <View style={styles.notice}><Text style={styles.noticeText}>Bilgi: Henüz gerçek zamanlı tahmin sunulmamaktadır.</Text></View>
            </View>
          </>
        )}

        {tab === "Sanal Kupon" && (
          <>
            <Text style={styles.eyebrow}>KİŞİSEL ALAN</Text>
            <Text style={styles.title}>Sanal kupon</Text>
            <Text style={styles.subtitle}>Seçimlerini burada düzenleyebileceğin alan.</Text>
            <View style={styles.panel}><Text style={styles.panelTitle}>Henüz seçim yok</Text><Text style={styles.body}>Maç seçimi ve sanal kupon özelliklerini temel uygulamanın açılışı doğrulandıktan sonra ekleyeceğiz.</Text></View>
          </>
        )}

        {tab === "Profil" && (
          <>
            <Text style={styles.eyebrow}>HESABIM</Text>
            <Text style={styles.title}>Profil</Text>
            <Text style={styles.subtitle}>DeepScore uygulama bilgileri.</Text>
            <View style={styles.panel}>
              <View style={styles.profileRow}><Text style={styles.profileLabel}>Uygulama</Text><Text style={styles.profileValue}>DeepScore</Text></View>
              <View style={styles.divider} />
              <View style={styles.profileRow}><Text style={styles.profileLabel}>Sürüm</Text><Text style={styles.profileValue}>2.0.0</Text></View>
              <View style={styles.divider} />
              <View style={styles.profileRow}><Text style={styles.profileLabel}>Durum</Text><Text style={[styles.profileValue, { color: C.green }]}>Temel sürüm</Text></View>
            </View>
          </>
        )}
        <Text style={styles.footer}>DeepScore · İstatistik odaklı analiz</Text>
      </ScrollView>

      <View style={styles.tabBar}>
        {TABS.map((item, index) => {
          const active = tab === item;
          const symbols = ["◈", "⌁", "▤", "◉"];
          return (
            <TouchableOpacity key={item} style={styles.tabItem} onPress={() => setTab(item)} activeOpacity={0.75}>
              <Text style={[styles.tabSymbol, active && styles.activeTabText]}>{symbols[index]}</Text>
              <Text style={[styles.tabLabel, active && styles.activeTabText]}>{item}</Text>
              {active ? <View style={styles.activeMark} /> : <View style={styles.inactiveMark} />}
            </TouchableOpacity>
          );
        })}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: C.bg },
  header: { paddingHorizontal: 20, paddingTop: 12, paddingBottom: 16, borderBottomWidth: 1, borderBottomColor: C.border, flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  brandRow: { flexDirection: "row", alignItems: "center" },
  logo: { width: 42, height: 42, borderRadius: 14, backgroundColor: C.green, alignItems: "center", justifyContent: "center", marginRight: 11 },
  logoText: { color: C.bg, fontSize: 27, fontWeight: "900" },
  brand: { color: C.text, fontSize: 21, fontWeight: "900", letterSpacing: 0.2 },
  tagline: { color: C.muted, fontSize: 9, fontWeight: "800", letterSpacing: 1.8, marginTop: 3 },
  liveBadge: { flexDirection: "row", alignItems: "center", borderWidth: 1, borderColor: C.border, borderRadius: 20, paddingHorizontal: 9, paddingVertical: 7 },
  dot: { width: 6, height: 6, borderRadius: 3, backgroundColor: C.green, marginRight: 6 },
  liveText: { color: C.muted, fontSize: 9, fontWeight: "800", letterSpacing: 0.5 },
  content: { padding: 20, paddingBottom: 28 },
  eyebrow: { color: C.green, fontSize: 10, fontWeight: "900", letterSpacing: 1.7, marginTop: 5, marginBottom: 9 },
  title: { color: C.text, fontSize: 29, fontWeight: "900", letterSpacing: -0.6 },
  subtitle: { color: C.muted, fontSize: 13, lineHeight: 20, marginTop: 6, marginBottom: 22 },
  featureCard: { backgroundColor: C.panel, borderWidth: 1, borderColor: C.border, borderRadius: 22, padding: 17, marginBottom: 25 },
  cardTop: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  league: { color: C.muted, fontSize: 9, fontWeight: "900", letterSpacing: 0.8 },
  sampleBadge: { backgroundColor: "#263449", borderRadius: 6, paddingHorizontal: 7, paddingVertical: 4 },
  sampleText: { color: C.amber, fontSize: 8, fontWeight: "900" },
  teams: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingTop: 24, paddingBottom: 22 },
  team: { width: "40%", alignItems: "center" },
  teamBadge: { width: 54, height: 54, borderRadius: 18, backgroundColor: "#B91C36", alignItems: "center", justifyContent: "center", marginBottom: 9 },
  awayBadge: { backgroundColor: "#183B7A" },
  teamInitial: { color: "#FFFFFF", fontSize: 25, fontWeight: "900" },
  teamName: { color: C.text, fontSize: 12, fontWeight: "800", textAlign: "center" },
  vs: { color: C.muted, fontSize: 13, fontWeight: "900" },
  divider: { height: 1, backgroundColor: C.border, marginVertical: 14 },
  disclaimer: { color: C.muted, fontSize: 11, lineHeight: 17 },
  primaryButton: { backgroundColor: C.green, borderRadius: 12, paddingVertical: 13, alignItems: "center", marginTop: 16 },
  primaryButtonText: { color: C.bg, fontSize: 13, fontWeight: "900" },
  sectionTitle: { color: C.text, fontSize: 17, fontWeight: "900", marginBottom: 13 },
  infoRow: { flexDirection: "row", alignItems: "center", marginBottom: 17 },
  infoIcon: { width: 39, height: 39, borderRadius: 13, backgroundColor: "#17283A", alignItems: "center", justifyContent: "center", marginRight: 12 },
  infoIconText: { color: C.blue, fontSize: 12, fontWeight: "900" },
  infoCopy: { flex: 1 },
  infoTitle: { color: C.text, fontSize: 13, fontWeight: "800", marginBottom: 3 },
  infoText: { color: C.muted, fontSize: 11, lineHeight: 16 },
  panel: { backgroundColor: C.panel, borderWidth: 1, borderColor: C.border, borderRadius: 18, padding: 18 },
  panelTitle: { color: C.text, fontSize: 16, fontWeight: "900", marginBottom: 10 },
  body: { color: C.muted, fontSize: 13, lineHeight: 21 },
  notice: { backgroundColor: "#2C271B", borderRadius: 10, padding: 12, marginTop: 16 },
  noticeText: { color: C.amber, fontSize: 11, lineHeight: 17 },
  profileRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 8 },
  profileLabel: { color: C.muted, fontSize: 13 },
  profileValue: { color: C.text, fontSize: 13, fontWeight: "800" },
  footer: { color: "#59667B", fontSize: 10, textAlign: "center", marginTop: 28, marginBottom: 5 },
  tabBar: { flexDirection: "row", backgroundColor: "#0D1422", borderTopWidth: 1, borderTopColor: C.border, paddingTop: 8, paddingBottom: 5, paddingHorizontal: 4 },
  tabItem: { flex: 1, alignItems: "center", justifyContent: "center", paddingVertical: 3 },
  tabSymbol: { color: C.muted, fontSize: 19, marginBottom: 3 },
  tabLabel: { color: C.muted, fontSize: 9, fontWeight: "700" },
  activeTabText: { color: C.green },
  activeMark: { width: 18, height: 3, borderRadius: 2, backgroundColor: C.green, marginTop: 5 },
  inactiveMark: { width: 18, height: 3, marginTop: 5, backgroundColor: "transparent" },
});
