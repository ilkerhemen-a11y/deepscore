export const API_BASE_URL = (process.env.EXPO_PUBLIC_API_BASE_URL || "https://deepscore-api.onrender.com").trim().replace(/\/+$/, "");

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 60000);
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, { ...options, signal: controller.signal });
    const data = await response.json().catch(() => null);
    if (!response.ok) {
      const detail = typeof data?.detail === "string" ? data.detail : "Sunucu isteği tamamlayamadı.";
      throw new Error(`HTTP ${response.status}: ${detail}`);
    }
    if (data === null) throw new Error("Sunucudan geçerli veri alınamadı.");
    return data as T;
  } catch (error) {
    if (controller.signal.aborted) throw new Error("Sunucu 60 saniye içinde yanıt vermedi. Biraz sonra tekrar dene.");
    if (error instanceof TypeError) throw new Error("Sunucuya bağlanılamadı. İnternet bağlantısını ve API adresini kontrol et.");
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}
