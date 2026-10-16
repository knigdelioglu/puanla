# P5 kabul planı ve kanıt matrisi — birleşik puanlama

> **Tarih:** 16.10.2026 · **Durum:** Otomatik regresyonlar kodlandı; son PR CI ve fiziksel cihaz kabulü bekleniyor.
> **İlgili plan:** [P0–P6 geçiş planı](unified-scoring-workspace-migration.md)  
> **Test dalı:** `test/unified-scoring-p5-acceptance` · **PR:** [#38](https://github.com/knigdelioglu/puanla/pull/38).

## Testin kapsamı ve kanıt ilkesi

- **Otomatik kanıt:** GitHub Actions `test`, `assembleDebug`, `assembleRelease`, `connectedDebugAndroidTest`; başarılı yürütme bağlantıları PR kontrolünden doğrulanır. Derleme başarısı emülatör başarısının veya gerçek tablet kabulünün yerine geçmez.
- **Yeni uçtan uca Android testi:** `UnifiedScoringEndToEndAcceptanceTest.kt`. Gerçek Room kaynaklarına karşı bireysel ve grup puanları, iki grup oturumu, null–0 ayrımı, gözlem notları, PDF/CSV snapshot, tam JSON yedek ve farklı Room veritabanına restore, grup geri alma ve üyelik eşitleme izolasyonu.
- **Mevcut testler:** `GroupModeScoringTest`, `PuanlaViewModelRestoreAndEvidenceTest`, `UnifiedScoringLayoutTest`, `CriterionGradingEditorTest`, `PdfReportWriterAndroidTest`, `EvaluationRunRepositoryTest`, `ScoringStudentNavigationTest`, `ReportModeRulesTest`, `BackupImportTest`.
- **P5 kod güvenliği:** Dışa aktarma hedefi işlem başında **sınıf+rubrik+oturum** olarak sabitlenir. Bireysel ve grup PDF/CSV, `MutationCoordinator.snapshot` üzerinden bekleyen kalıcı puan yazımlarından sonra tutarlı veri okur. Grup çıktısı bireysel oturuma bağlanmaz.

## Kabul matrisi

| Kod | Test / öğretmen davranışı | Otomatik kanıt | Fiziksel cihaz |
| --- | --- | --- | --- |
| A1–A2 | İki modda aynı değerlendirme seçimi ve ölçüt/puanlama yolu | `ScoringSessionSelectionTest`, `UnifiedScoringLayoutTest` | **Açık** — yatay/dikey ekran |
| A3 | Grup hazırlığı: manuel/rastgele; yinelenen üyelik reddi | `GroupModeScoringTest`, `GroupPartitionerTest` | **Açık** — yönetim alanının kullanılabilirliği |
| A4 | **0 puan** kesin puanlanmış, **null** eksik; kısmi kesinleşmez | `UnifiedScoringEndToEndAcceptanceTest`, `PdfReportStatisticsTest` | **Açık** — görsel durum |
| A5 | Hızlı/hassas puan ve gözlem notunda otomatik geçiş kontrolü | `CriterionGradingAdvanceTest`, `CriterionGradingEditorTest` | **Açık** — hızlı seri dokunuş |
| A6 | Puan kaldırma/undo yalnız doğru öğrenci–grup–oturuma yazılır | `UnifiedScoringEndToEndAcceptanceTest`, `PuanlaViewModelRestoreAndEvidenceTest` | **Açık** — gerçek akış |
| A7–A8 | Grup üye puan aktarımı, değişiklik ve yalnız onaylı oturum eşitlemesi | `GroupModeScoringTest`, `UnifiedScoringEndToEndAcceptanceTest` | **Açık** — iki grup oturumu |
| A9 | Mod/sınıf/oturum arasında gezinme ve hedefin korunması | `ScoringSessionSelectionTest`, `UnifiedScoringLayoutTest` | **Açık** — uygulama kapanıp açılınca |
| A10 | Bekleyen not/puan ve geri yüklemede veri kaybı yaşanmaması | `PuanlaViewModelRestoreAndEvidenceTest`, `UnifiedScoringEndToEndAcceptanceTest` | **Açık** — yedek al/geri yükle |
| A11 | Bireysel değerlendirme çoklu sınıf/arşiv kuralları | `EvaluationRunRepositoryTest`, `EvaluationRunMigrationTest` | **Açık** |
| A12 | Bireysel/grup PDF ve CSV oturumu doğru, eksik kesin not değil | `UnifiedScoringEndToEndAcceptanceTest`, `PdfReportWriterAndroidTest`, `ReportModeRulesTest` | **Açık** — dosyayı dışarıda açma |
| A13 | 11” tablet, yazı ölçeği, dokunma alanları, TalkBack, sistem çubukları, IME | Sadece emülatörün kapsadığı kısımlar | **Açık ve zorunlu** |
| A14 | Gerçek sınıf akışında bir puan/sonraki hedef adımı ve gecikmeler | Otomasyon dokunuş hissini veya gerçek öğretim süresini ölçemez | **Açık ve zorunlu** |

**Kanıt durumu:** Testlerin dosyada bulunması tek başına başarılı geçtikleri anlamına gelmez. PR #38 için **son commit'in** GitHub Actions sonucu `success` olmadan P5 otomatik kabulü tamamlandı sayılmaz.

## Gerçek cihaz kabul senaryosu

Galaxy Tab A11+ veya eşdeğer 11” Android tablette **yatay ve dikey** deneyin. Her denemede yalnızca anonim örnek öğrenci verisi kullanın.

1. 11/A sınıfını ve Sunum rubriğini seçip bireysel bir değerlendirme oluşturun; 3 öğrenciden birini 0 puanla, birini kısmi, birini tamamlanmış bırakın. Hızlı puan, hassas puan, gözlem notu ve Geri Al'ı deneyin.
2. Grup moduna geçip Grupları Yönet üzerinden iki grup oluşturun. Rastgele grup, mükerrer üyelik engeli, grup düzenleme, iki ayrı grup oturumu ve üyelik eşitleme onayını sınayın. Sonuçların bireysel notları değiştirmediğini teyit edin.
3. Her iki modda PDF ve CSV kaydedin; dosyaları dışarıda açın. PDF'de öğretmen/ders/oturum bilgileri ve ölçütler; CSV'de Türkçe karakterler, eksik alanlar, 0 puan ve üye satırları doğru olmalı.
4. Puanlama sırasında anında farklı öğrenci/grup/oturuma geçin. Bekleyen yazı ve gözlem notu önceki kimlikte kalmalı; uygulama kapatıp açıldığında tutarlı görünmeli.
5. Yedek alın, ayrı bir test kurulumu veya güvenli senaryoda geri yükleyin; her iki modun verilerini kontrol edin. **Mevcut gerçek öğrenci verilerini bu test için silmeyin.**
6. Yazı boyutunu artırın; koyu mod, ekran döndürme, klavye açıkken giriş, sistem gezinme çubuğu ve TalkBack odağını kontrol edin. Sağdaki puan düğmeleri, geri alma ve sonraki hedef düğmeleri hiçbir durumda görünüm dışına çıkmamalı.

## Yayına çıkış kapısı

- PR #32–#38 zincirinin tüm hedefleri aynı kod birleşiminde doğrulandı; her bir PR'ın tek başına başarılı olması yığılmış PR'ların sonraki birleşim başarısını garanti etmez.
- **Son commit'te** CI: derleme + Android emülatörü başarılı.
- P5 gerçek tablet kabulü kayıtlı. Özellikle A13 ve A14 açıkken “tam doğrulandı” denemez.
- Eski yedek ve oturumları açma, eşitleme ve rapor üretimi doğrulanmış.
- P6 için ölü kod temizliği ancak bu kapıdan sonra; Release/APK yayımlama ayrı karar ve veri yedeği kontrolü gerektirir.
