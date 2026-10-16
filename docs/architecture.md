# Android Native mimari çerçevesi

**Kurulu olan:** Android uygulama modülü (`app/`), kardeş `compose-design-system` deposundan alınan `:design-system` Compose modülü, Room veri katmanı, yerel OCR, CSV ve yedekleme akışları. Puanlama kuralları ve değerlendirme durumları uygulamanın saf Kotlin domain katmanında tutulur. Bağımlılık yolunun yapılandırması için [entegrasyon raporuna](research/compose-design-system-puanla-integration.md) bakın.

## Planlanan katmanlar
- **UI:** Compose + adaptif tablet düzeni; ölçütlere doğrudan erişim, büyük etkileşim alanları, kalem/dokunma ergonomisi ve erişilebilirlik. `:design-system` yalnızca görünüm/etkileşim bileşenlerini sağlar; ürün kurallarına ve veri modellerine bağımlı değildir.
- **Domain:** Sınıf, öğrenci, rubrik, ölçüt ve değerlendirme sözleşmeleri; null/0 ve toplam kuralları. Android bağımlılığından mümkün olduğunca bağımsız.
- **Data:** Room ile cihaz içi ilişkisel kayıtlar; veri geçişleri, transaction ve benzersizlik; ilk onaylı rubrikleri JSON'dan yalnızca okuma. Hassas veriler güvenli saklama ve dışa aktarma kurallarına bağlı.
- **Import:** CameraX / sistem fotoğraf seçici, cihaz içi ML Kit OCR, sütun geometrisi ve kullanıcı doğrulama ekranı. OCR sonucu veri tabanına doğrudan yazılmaz.
- **Export:** CSV için UTF-8 ve Excel uyumluluğu; kullanıcı onaylı SAF dosya seçimi; açık formatlı doğrulanabilir yedek.

## Değişim sınırları
Kaynak rubrik JSON'ları salt kaynak girdi olarak korunur; uygulama güncellemesi bu dosyaları otomatik yeniden yazmaz. Kişisel öğrenciler repo/test fixture'larına kaydedilmez.

## Aşamalı teslim
1. Compose tablet kabuğu ve tasarım ilkeleri.
2. Kaynak rubrik doğrulaması, Room model/DAO, sınıf yönetimi, temel manuel veri akışı.
3. Puanlama akışı, eksik/tamamlandı durumları ve geri alma.
4. OCR + review ve CSV/yedekleme.
5. Erişilebilirlik, performans, cihaz matrisi ve rafine Arc etkileşimleri.

Bu sıralama ilk öneridir; özellikler gerçek test ve geri bildirimle önceliklendirilir.

## Birleşik bireysel/grup çalışma alanı — geçiş dalı (16 Ekim 2026)

**Henüz main'e alınmadı.** [Geçiş planı](plans/unified-scoring-workspace-migration.md) sırasıyla #32–#38 PR'larda uygulandı. P5'in [son CI çalışması](https://github.com/knigdelioglu/puanla/actions/runs/38948714951) birim, APK ve Android emülatör testlerini geçti. Fiziksel 11 inç tablet, ekran döndürme, TalkBack/IME ve gerçek kullanım hızı kabulü açıktır.

- **Sunum kabuğu:** `ui/screens/UnifiedScoringLayout.kt` bireysel/grup için geniş ekranda ortak üç paneli, dar ekranda ortak sekmeleri sağlar. Mod veri modelini birleştirmez.
- **Paylaşılan ürün bileşenleri:** `CriteriaExplorerPane.kt`, `QuickScoreControls.kt`, `CriterionGradingEditor.kt`, `ScoringTargetToolbar.kt` iki modda aynı ölçüt seçimi, hızlı/hassas puan, sonraki hedef ve geri alma görünümünü sunar.
- **Mod bağlayıcıları:** `ScoringWorkspaceScreen.kt` öğrenci hedefini; `GroupWorkScreen.kt` grup hedefini, üye görüntüsünü ve grup hazırlığını sağlar.
- **Kalıcı veri:** `AssessmentRunEntity` ve grup oturumları ayrıdır; Room grup üye puanlarını transaction içinde günceller. `0` ile `null` ayrıdır; kanıt notları puan olmadan saklanabilir.
- **Tutarlı raporlar:** bireysel ve grup PDF/CSV işlemleri başlangıçtaki oturum kimliğini sabitler, `MutationCoordinator.snapshot` ile bekleyen yazımların ardından okur.
- **Yedek/geri alma:** not debounce ve puan yazımları restore/backup koordinasyonu kapsamındadır; undo geçmişleri bağlam temellidir ve modlar arasında paylaşılmaz.

**P6 sınırı:** Bu saf Compose refaktöründe veri tabanı, rubrik JSON veya rapor biçimi değiştirilmedi. Yeni bileşenler hâlâ Puanla'nın ürün katmanındadır, genel amaçlı tasarım sistemi modülüne taşınmadı. Eski yolların kaldırıldığına veya kullanıcının cihazında kabul aldığına dair iddiada bulunmayın. [P5 kabul matrisi](plans/unified-scoring-p5-acceptance.md) tamamlanmadan sürüm onayı verilmez.
