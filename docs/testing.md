# Puanla — Test ve doğrulama stratejisi

## Otomatik testlerle şu an denetlenenler
- `ScoringRulesTest`: puanlanmadı ≠ 0, eksik değerlendirme kesinleşmez, puan sınırları.
- `OcrRosterParserTest`, `OcrPositionedParserTest`: metin ve geometrik sütun ayrımı, yanlış sütun sızıntısı ve eksik başlıkların reddi.
- `BackupSnapshotTest`: tüm tabloları taşıyan snapshot şeması, JSON round-trip, bozuk ilişki ve tutarsız toplamların reddi.
- `CsvExportFormatTest`: **gerçek üretim `CsvExportFormatter`** çağrılır; UTF-8 BOM, Türkçe, noktalı virgül, çift tırnak, CRLF, Excel formül koruması, null/0 ve kısmi not.
- `ArcMotionTest` ve `ArcTokensTest`: tema ve animasyon tokenları.

## CI
`./gradlew test :app:assembleDebug --no-daemon` — GitHub Actions workflow `.github/workflows/android.yml` (JDK 17, Android SDK 36).

Başarılı birim test ve APK derlemesi, gerçek cihaz fonksiyonlarının çalıştığı anlamına gelmez. Raporlarda commit bazında Actions durumu teyit edilmelidir.

## Henüz cihaz/emülatör üzerinde doğrulanması gerekenler
- Gerçek basılı sınıf listeleriyle OCR; karmaşık soyadları, yatılı/cinsiyet sütunları, rotasyon, bulanık fotoğraf ve OCR onayı.
- Room işlemlerinde not yazma/geri alma, öğrenci silme, eşzamanlı seçim, restore ve migration davranışları.
- SAF dosya seçiciyle CSV kaydetme/açma, UTF-8 Türkçe karakterler ve Excel/Numbers uyumluluğu.
- Tablet yatay/dikey, 11 inç, yüksek yazı ölçeği, TalkBack, fare/kalem ve fiziksel klavye.
- 99 Arc girdisinin ayrı etkileşim ve ekrana bağlanma doğrulaması.

[Doğrulanmış kapsam ve eksikler](verification/arc-implementation-matrix.md).
