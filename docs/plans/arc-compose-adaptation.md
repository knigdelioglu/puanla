> **Güncel kapsamlı analiz:** [Arc'ın 132 registry kaydını Puanla ile karşılaştıran tam envanter](../research/arc-puanla-full-catalog.md) ve [genişletilmiş tablet öncelikli ihtiyaç/uyarlama planı](arc-maximal-adoption.md). Bu belge ilk, daha dar kapsamlı planı tarihsel olarak korur; güncel geliştirme önceliklerinde genişletilmiş plan esas alınır.

# Arc → Kotlin / Jetpack Compose uyarlama planı

**Durum:** İncelendi, planlandı — **uyarlama bileşenleri henüz uygulanmadı**.  
**Kaynak sürüm:** [kuratlielia/arc-library@86330cc](https://github.com/kuratlielia/arc-library/commit/86330cc9270c4acc55b7204c9583fcbaa378192c), MIT.  
**Denetim:** [Arc kaynak incelemesi](../research/arc-library-audit.md).

## Hedef

Puanla'da özellikle 11 inç sınıf tabletlerinde **düşük dikkat maliyetiyle, yüksek hızda, hatasız rubrik puanlama** sağlayan, Arc'ın sakin fiziksel hareket dilinden esinlenen **tamamen Android Native** etkileşim seti oluşturmak. Web katmanı/Javascript runtime yok; kaynak JSON ve değerlendirme kuralları bağımsız.

## Mimari sınır

```text
:app                        Android uygulama, ekran/state/iş akışı
:arc-compose                Gelecekte oluşturulacak saf Compose UI + motion bileşenleri
(domain/data katmanları)     Puanlama ve kalıcılık; Arc'a bağımlı değil
data/                       Onaylı rubrik JSON kaynakları — değiştirilemez sınır
```

`:arc-compose` planlanan modüldür, henüz oluşturulmamıştır. Bileşen API'leri ürün verisi bilmez: kontrollü state, olay callback'leri ve Compose semantics sunar. Puan sınırları ve kesin not kararı UI bileşeninde değil domain'de uygulanır.

## Teslim basamakları ve kabul koşulları

### A — Compose tasarım temeli ve motion tokenları
- `:arc-compose` modülünü, dokunma hedefi/boşluk/yüzey/typography ve motion tokenlarını kur.
- Arc sürelerini ve easing karakterini referans al, Compose `tween`/`spring` ile yerel davranışı cihazda kalibre et; reduced motion tercihine uy.
- Çok geniş ekran, tablet yatay/dikey, dark/light ve büyük font için Compose Preview/test örnekleri hazırla.

**Kabul:** 48dp+ kullanılabilir dokunma hedefleri, yüksek kontrast ve 1.5–2x font scale, TalkBack içerik etiketleri; animasyon devre dışı bırakılınca işlev kaybı yok.

### B — Puanlama kontrolleri (ilk native Arc uyarlaması)
- `ArcSegmentedRating`: 3–4 dereceli kısa kategori, kayan seçim göstergesi, seçili/boş durum ve erişilebilir doğrudan seçim.
- `ArcScoreSlider`: dinamik rubrik ölçüt üst sınırı, 0 dahil tamsayı adımları, parmağın üzerinde okunaklı puan balonu, sınır clamp, cancel-safe sürükleme, `onValueChange` / `onValueCommit` ayrımı ve dokunsal detent geri bildirimi (isteğe bağlı).
- `ArcNumberField`: tam sayı girişi, artı/eksi, hata/sınır durumu; slider'ın zorunlu olmayan ama görünür alternatifidir.
- Modelde `Unscored` ile `Scored(0)` daima ayrı kalır; kategori seçimleri bağımsız ölçüt puan kuralına eşlenir.

**Kabul:** 0–max dışında puan kaydı yok; bitmemiş dokunuş/iptal kalıcı puan yazmaz; hızlı sürüklemede takılma yok; TalkBack ve klavye ile puan girişi mümkün; aynı öğrenci dışındaki puanlar değişmez.

### C — Tablet değerlendirme çalışma alanı
- Önce yatay geniş ekran için sınıf/öğrenci listesi + seçili performans ölçütleri + bağlamsal puan alanı önerisini prototiple; ekranı sabit üç panele zorunlu kılma.
- Dar/geniş ekran için adaptif yerleşim; mevcut öğrencinin kimliği, seçili rubrik, eksik/bitmiş statüsü aynı bakışta anlaşılır.
- Öğrenci/ölçüt geçişleri doğrudan, büyük dokunma alanlarıyla ve gereksiz modal olmadan yapılır.
- `ArcProgressStatus`, `ArcRubricChoice` (radio-cards), `ArcFeedbackHost` (snackbar+undo) ekle.

**Kabul:** Kullanıcı yalnız dokunma ile bir öğrencinin tüm ölçütlerini tamamlayabilir, yanlış puanı geri alabilir, sınıf ve öğrenci değiştirebilir. Eksik rubrik **kesin not gibi gösterilmez**. Kaydedilen puanlar kapanıp açınca kalır.

### D — Yardımcı kontroller ve ürün bağlama
- `ArcInlineEdit`, tablet yan paneli/dar ekranda bottom sheet, sekmeler ve açıklayıcı boş durumları gerektiğinde uyarlama.
- OCR kullanıcı onayı, fotoğraftan tespit edilen hücreleri düzeltme, CSV/yedekleme akışlarında görsel tutarlılık sağla.
- Silme ve geri dönüşü zor işlemlerde açık onay; kaydırma jestini tek kontrol yöntemi yapma.

**Kabul:** Öğrenci ad/sütun/numara yanlışları kullanıcı onayı olmadan kesin kayıt haline gelmez; yedekleme, çevrimdışı kayıt ve CSV iş kuralları bozulmaz.

### E — Kalite kapısı ve kaynak takibi
- Tablet cihaz testi: Galaxy Tab A11+ benzeri **1920 × 1200 / 90Hz**, 11 inç; ayrıca kompakt telefon ve diğer ekran yoğunlukları.
- Macrobenchmark/JankStats ile sürükleme akıcılığı; Compose UI testleri ve screenshot/golden kontrolleri; font, erişilebilirlik ve animasyon ölçeği senaryoları.
- MIT atıfları, upstream commit ve alınan davranışların kendi Kotlin karşılıkları matrisi güncel tutulur.

**Kabul:** Testler yeşil; gerçek bir debug APK başarılı derleniyor; animasyon performansı ve puan doğruluğu bağımsız doğrulanıyor. Release dağıtımı bundan sonra ayrı karardır.

## Öncelik

**P0:** motion tokens, segmented rating, elastic score slider, sayısal alternatif, progress/badge.  
**P1:** rubric choice, snackbar+undo, tabs/bottom sheet, inline-edit, empty/skeleton.  
**P2:** swipe actions, confirm morph, metric card ve kalan bileşenler — yalnız kanıtlanmış ürün ihtiyacı varsa.

**Kapsam dışı:** 108 Arc bileşenin tamamını port etmek, React runtime, WebView, Arc Pro varlıkları, mevcut kaynak rubrik JSON'larını yeniden üretmek veya tasarım çalışması için puanlama/veri güvenliği kurallarını gevşetmek.

## Önerilen ilk geliştirme hedefi

`:arc-compose` modülünü kurup `ArcSegmentedRating`, `ArcScoreSlider` ve `ArcNumberField` için *gerçek* Compose örnekleri/testleri tamamla. Ardından puanlama sahnesine bağla. Önce denetimli kontrol akışını bitirmek, erken dönemde 108 bileşenli genel bir tasarım sistemi yazmaktan daha değerlidir.
