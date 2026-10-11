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

## Eklenen P1 rapor tarihi testleri (11 Ekim 2026)

`AssessmentDateFilterTest` üretimde kullanılan son-değişiklik tarihi filtresini Europe/Istanbul gün sınırlarıyla ve açık başlangıç/bitiş aralıklarıyla doğrular. Material3 tarih penceresi, tablet üzerindeki gerçek dokunma akışı ve takvim ay değiştirme davranışı yalnız birim testle doğrulanamaz; cihaz testi açıktır.

## 11 Ekim 2026 — P1 veri bütünlüğü sertleştirmesi

- `UndoHistoryTest`: Bir bildirimden gelen eski geri alma eylemi, daha yeni başka bir puan kaydını geri alamaz. Başarısız geri alma ve yedek sonrası geçmiş temizliği de birim testindedir.
- `StudentNameRulesTest`: Boş / rakamlı / aşırı uzun / kontrol karakterli ad-soyad reddedilir; Türkçe soyadı büyük harfe çevirme ve çok sözcüklü ad desteklenir. Sınıf listesi ayrı ad ve soyad alanlarını işlem bazında günceller.
- Tekil öğrenci kaydında okul numarası tekrarı kontrolü ve ekleme aynı Room transaction'ındadır; doğrulama hataları `Result.failure` üzerinden kullanıcıya döner.
- Puan ve gözlem notu yazımları, tam JSON yedeği üretilmeden önce tamamlanır. Geri yüklemede bekleyen puan, not ve geri alma işlemleri iptal edilip `joinAll` ile sonlanmaları beklenir.
- Farklı yedek dosyası seçildiğinde önceki ikinci onay otomatik sıfırlanır.
- **Cihazda hâlâ doğrulanması gerekenler:** hızlı art arda ölçüt puanlama + eski toast geri alma, ad/soyad eşzamanlı düzenleme, en son gözlem notunu girip hemen yedek alma, Android SAF üzerinden gerçek dosya seçme ve geri yükleme, sınıf ve grup işlemleri sırasında restore.
