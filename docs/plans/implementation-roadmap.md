# Puanla Android Native geliştirme yol haritası

**2026-10-10:** Temel repo iskeleti ve ürün sözleşmesi var. Değerlendirme, OCR, Room, CSV/yedek ve Arc Compose bileşenleri **henüz uygulanmadı**.

| Aşama | Çıktı | Bitiş koşulu |
| --- | --- | --- |
| 0. Temel | Android Gradle/Compose + test/CI + kapsam sınırları | JDK17/SDK36 ile debug derleme ve testlerin geçmesi |
| 1. Arc çekirdeği | Ayrı `:arc-compose` UI modülü; motion tokenları; segmented/slider/number input | Tablet önizleme, jest iptali, a11y ve limit testleri |
| 2. Veri/puanlama | Rubrik kaynağı doğrulama; sınıf/öğrenci Room; değerlendirme kayıtları; tablet ana akışı | Kimlik/puan izolasyonu, null/0, yarım değerlendirme, kaldığı yerden devam |
| 3. Aktarma | Yerel fotoğraf OCR, hücre/sütun analizi ve kullanıcı onayı; manuel giriş | Cinsiyet/pansiyon bilgi sızıntısına karşı gerçekçi anonim fixture testleri |
| 4. Sonuç | CSV, ilerleme, ortalama, grup hazırlık takibi, yedekleme/geri yükleme | Yuvarlama/kimlik ve round-trip doğruluğu; offline |
| 5. Kalite | Erişilebilirlik, 11 inç tablet cihaz ölçümleri, performans ve yayın hazırlığı | CI + instrumented testler, veri kaybı yok, APK doğrulaması |

Bu plan bağımlılıkların mantıksal önceliğidir; Arc UI sadece sunum katmanını etkiler. Arayüz değişikliği veri modelini otomatik dönüştüremez.

## Şimdiki engeller
- Onaylı `data/rubrics.json` yeni depoda bulunmuyor. Ürün raporu 48 ölçütün **tam içeriğini içermez**.
- İlk CI koşusu Android SDK setup aracındaki eski `tools` paketi nedeniyle başarısız oldu; yapılandırma düzeltildi ve yeni koşunun sonucuna göre tekrar doğrulanmalıdır.
- Gradle Wrapper henüz repoda yok. İlk başarılı build sonrası wrapper dosyaları eklenmeli ve CI wrapper üzerinden çalışmalıdır.

Detaylı Arc uyarlaması: [arc-compose-adaptation.md](arc-compose-adaptation.md). Testler: [../testing.md](../testing.md).
