# Puanla Android Native geliştirme yol haritası

**11 Ekim 2026 kod durumu:** Puanla `:app`, Room tabloları, puanlama ekranları, fotoğraf OCR akışı, CSV dışa aktarma ve JSON yedekleme kodu içerir. Compose Design System'in kardeş depodaki `:design-system` modülüne geçişi tamamlandı; derleme, birim testleri ve debug APK kurulumu başarılı. API 36 Samsung tablete APK kuruldu ve açılışı doğrulandı. Yatay/dikey tablet, TalkBack, fiziksel klavye, OCR onayı ve yedekten geri yükleme cihaz kabulü sıradadır. Eski `:arc-compose` envanteri geçiş öncesi tarihsel durumdur; güncel bileşen eşleşmesi [entegrasyon raporundadır](../research/compose-design-system-puanla-integration.md).

| Aşama | Çıktı | Bitiş koşulu |
| --- | --- | --- |
| 0. Temel | Android Gradle/Compose + test/CI + kapsam sınırları | JDK17/SDK36 ile debug derleme ve testlerin geçmesi |
| 1. Arc çekirdeği | Ayrı `:arc-compose` UI modülü; motion tokenları; segmented/slider/number input | Tablet önizleme, jest iptali, a11y ve limit testleri |
| 2. Veri/puanlama | Rubrik kaynağı doğrulama; sınıf/öğrenci Room; değerlendirme kayıtları; tablet ana akışı | Kimlik/puan izolasyonu, null/0, yarım değerlendirme, kaldığı yerden devam |
| 3. Aktarma | Yerel fotoğraf OCR, hücre/sütun analizi ve kullanıcı onayı; manuel giriş | Cinsiyet/pansiyon bilgi sızıntısına karşı gerçekçi anonim fixture testleri |
| 4. Sonuç | CSV, ilerleme, ortalama, grup hazırlık takibi, yedekleme/geri yükleme | Yuvarlama/kimlik ve round-trip doğruluğu; offline |
| 5. Kalite | Erişilebilirlik, 11 inç tablet cihaz ölçümleri, performans ve yayın hazırlığı | CI + instrumented testler, veri kaybı yok, APK doğrulaması |

Bu plan bağımlılıkların mantıksal önceliğidir; Arc UI sadece sunum katmanını etkiler. Arayüz değişikliği veri modelini otomatik dönüştüremez.

## Açık doğrulama ihtiyaçları
- Onaylı `data/rubrics.json` bulunmuyor. Gerçek rubrik ve ölçüt metinleri eksikken canlı öğretmen notu üretimi başlatılmamalı.
- P0 değişikliklerinin GitHub Actions test/derlemesi yeşil; cihaz içi OCR, öğrenci/puan izolasyonu ve tam yedekleme–geri yükleme için gerçek tablet veya enstrümantasyon testleri açık.
- P1 CSV testleri artık gerçek üretim formatlayıcısını çağırıyor; Excel açılışı ve SAF dışa aktarma uçtan uca cihazda doğrulanmalı.
- Arc P0/P1/P2 kayıtlarının 99/99 entegrasyon iddiası geçersiz; statik kullanım dağılımı ve eksikler [Arc doğrulama matrisi](../verification/arc-implementation-matrix.md) içinde.

Detaylı Arc uyarlaması: [arc-compose-adaptation.md](arc-compose-adaptation.md). Testler: [../testing.md](../testing.md).
