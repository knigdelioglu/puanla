# Puanla × Arc Library: Tablet Öncelikli Kapsamlı İhtiyaç Analizi ve Uyarlama Planı

**Hazırlanma:** 10 Ekim 2026  
**Kaynaklar:** [Puanla mevcut `main`](https://github.com/knigdelioglu/puanla/tree/6ef38c9f0ad557dbf3803fbec1b7f0a9ab41fbf0), [Arc `main@86330cc`](https://github.com/kuratlielia/arc-library/tree/86330cc9270c4acc55b7204c9583fcbaa378192c), kullanıcının sağladığı ürün amaç/kapsam raporunu yansıtan [product-scope.md](../product-scope.md), Arc [registry.json](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry.json) ve [132 girdinin tamamının matrisi](../research/arc-puanla-full-catalog.md).  
**Statü:** Analiz ve uygulama planı. Bu listedeki Arc Compose bileşenleri **henüz Kotlin ile oluşturulmuş veya uygulamada kullanılmakta değildir**.

## 1. Sonuç ve kapsam kararı

Önceki P0/P1 listesi, Arc'ın gerçek UI envanteri ve Puanla'nın tüm ekranları ile karşılaştırıldığında yetersizdi. Registry envanterinde **108 UI, 22 blok, 2 foundation = 132 kayıt** bulunuyor. Dosya ağacında 109 component klasörü görülmesi, registry'deki bileşen sayısının 109 olduğu anlamına gelmez; component klasöründeki bazı girdiler `registry:block` türünde, tek kaynak dosyasından birkaç varyant yayımlanıyor.

Tam envanter incelemesine göre:
- **P0: 46** girdi → çekirdek kullanımda öncelikli uyarlama.
- **P1: 30** girdi → yakın dönem/bağlı ekran için güçlü uyarlama.
- **P2: 23** girdi → gerçek gereksinime ve veri modeline bağlı opsiyonel.
- **X: 33** girdi → şu an Puanla gereksinimini karşılamıyor; bilinçli dışarıda.
  
**Hedef: P0+P1 olan 76 registry girdisinin yeteneklerini kapsamak**, ancak varyant/tekrarları tek yerel API altında birleştirmek. Bu **76 ayrı Kotlin dosyası/özel widget zorunluluğu değildir**; bazıları doğrudan Material3 bileşeni + Arc davranışıyla karşılanır. Üretim performansı ve erişilebilirlik, uyarlanan bileşen sayısından daha değerlidir. P2 girdilerden daha fazlası ancak açık bir ürün yararıyla eklenir.

## 2. Puanla'nın doğrulanmış iş gereksinimi ve mevcut durum

| Konu | Ürün gereksinimi | Depodaki mevcut uygulama | Arc'ın sağlayacağı / sağlayamayacağı |
|---|---|---|---|
| Tablet UX | Hızlı, düşük dikkat maliyetli, yatay/dikey geniş ekran | `PuanlaApp.kt` geçici 840dp iki sütunlu tanıtım | **Sağlar:** panel, eylem, navigasyon, görsel ritim. **Sağlamaz:** iş akışının kendisi |
| Sınıf/öğrenci | 9–12, çok şube, manuel giriş/düzeltme, izolasyon | Henüz ekran/veritabanı yok | Arama, tablo, filtre, düzenleme bileşenleri; **Room veri bütünlüğü Arc dışında** |
| Fotoğraftan liste | Sütun farkı, ad/soyad/numara, belirsiz bilgiyi kullanıcı onayı | OCR uygulaması yok | Fotoğraf seç, adım göstergesi, büyük onay tablosu, hata ve karşılaştırma; **OCR motoru ve sütun çözümleme Arc değildir** |
| Rubrik | Her ölçüte azami puan, sınıfa uygun rubrik | Asıl 11. sınıf rubrik JSON'u yok | Rubrik kartları, açıklama, seçim; **rubrik içeriği üretilemez** |
| Puanlama | `null != 0`, 0–max tamsayı, eksik rubrik kesin not olmaz | Yalnız saf Kotlin `ScoringRules.kt` ve birim testleri | Segmented, slider, number field, progress, undo; **iş kuralları domain'de kalır** |
| Gruplar | 7 hazırlık kontrolü (önceki raporda bir rubrikte), bireysel nota karıştırmama | Yok | Checkbox, avatar-group, chip-group, progress; **grup veri modeli ayrı** |
| Sonuç/CSV | Biten/kısmi/boş, yalnız kesin notların ortalaması, ölçütlü CSV | Yok | Tablo, filtre, metrik, grafik, eylem; **CSV format/kayıt Arc değildir** |
| Offline/yedek | Cihaz içi veri, kalıcı kaydetme, restore | Yok | Güvenli durum geri bildirimi ve dosya seçme; **Room/SAF/yedek bütünlüğü Arc dışında** |

Ürün raporu **ekran sayısı veya stil istemiyor**: aşağıda önerilen ekranlar yeni tasarımdır, eski sürümün kopyası değildir. Rubriklerin tam 48 ölçüt metni/puan ağırlığı kaynak JSON olmadan oluşturulmaz.

## 3. Ekran ve kullanım akışı bazında ayrıntılı Arc haritası

### 3.1 Tablet uygulama kabuğu / navigasyon

**İhtiyaç:** Bir öğretmen birden fazla sınıfı açık tutarken öğrenci ve rubriğe en az gezinmeyle ulaşabilmeli; 11 inç cihazda boş yer, küçük butonlar veya gereksiz kart yığınları olmamalı.

| Arc | Uygulamada konumu / rolü | Yerel yöntem |
|---|---|---|
| `arc-foundation`, `arc-motion-tokens` | Bütün uygulamanın renk/yüzey/radius/font/odak ve tutarlı animasyon sistemi | `ArcTheme`, `ArcMotion`, Compose Material3 token haritalaması |
| `page-header` (block) | Şube, rubrik ve seçili öğrenci bağlamı; içerik kayarken kompakt hale gelme | `ArcWorkspaceHeader`; web SaaS sahnesi kopyalanmaz |
| `resizable-panels` | Öğrenci listesi ↔ ölçütler ↔ puanlama alanı arasında ayarlanabilir bölücüler | `ArcSplitWorkspace` + erişilebilir divider |
| `tabs`, `breadcrumb` | Şube / rubrik sekmeleri ve hiyerarşik konum | `ArcTabs`, yerel breadcrumb |
| `drawer`, `bottom-sheet` | Tablet yan ayrıntı; dar görünümde ekranda yükselen seçenekler | geniş ekranda side sheet, telefonda modal sheet |
| `floating-button-group`, `button-group`, `split-button` | Önceki/sonraki öğrenci, geri al ve ikincil eylemler | Tek ortak toolbar kontrol ailesi |
| `command-palette` (block) | Fiziksel klavye ile öğrenci/rubrik/eylem bulma | Android key event + yerel arama index'i |
| `scroll-area`, `tooltip` | Bağımsız taşan panel ve ikonların açıklaması | Compose lazy list + semantics + mouse/hover desteği |

**İlke:** Android native gezinmeyi Arc'ın web `site-header`/`site-footer` bloklarıyla taklit etmeyiz. Android sistem geri davranışı, IME ve tablet çoklu pencere kuralları geçerlidir.

### 3.2 Sınıflarım / öğrenci yönetimi

**İhtiyaç:** Öğrenci sayısı çok olan sınıflarda arama, eksik puanları süzme, isim/numara düzeltme, çoklu seçim ve hatalı kaydı önleme.

**Arc kombinasyonu:** `sortable-data-table`, `search-field`, `expanding-search`, `filter-toolbar`, `chip-group`, `combobox`, `select`, `multi-select`, `inline-edit`, `avatar`, `badge`, `checkbox`, `swipe-actions`, `context-menu`, `empty-state`, `toast-stack`, `confirm-morph`.

**Önerilen yapı:** Sol öğrenci listesi; satırda ad-soyad, okul numarası, değerlendirme durumu, tek dokunuşla seçim. Durum filtreleri tek satırda; sınıf değişiminde önceki öğrenci verisi veya puan yanlış sınıfta gösterilemez. Satır menüsündeki silme/düzenleme **swipe dışında da görünür**. Karmaşık veri tablosu geniş ekranda daha uygun; telefonda birinci kimlik alanı sabit kalan liste.

### 3.3 Sınıf listesi fotoğrafı / OCR gözden geçirme

**İhtiyaç:** Önceki uygulamadaki başlık/sütun karışmasının kökten önlenmesi. “KIZ”, “KR.”, “BB” benzeri komşu sütunların soyadına eklenmesi kabul edilemez.

**Aday akış:** **Dosya/fotoğraf seçimi** (`file-upload`, `file-dropzone`) → **görüntü işleme** (native cihaz içi) → **algılanan tabloyu kontrol** (`stepper`, `sortable-data-table`, `inline-edit`, `alert`, `image-compare`) → **belirsizlikleri düzelt** (`chip-group`, `filter-toolbar`, `input`, `popover`) → **onayla/kaydet** (`dialog`, `progress`, `toast-stack`).

**Ayrım:** FileUpload/FileDropzone React kaynağı ağ upload demoları içerir; Kotlin sürümünde varsayılan işlev **cihazdan URI seçmek**, görüntüyü yerelde işlemek ve hassas öğrenci verisini internete çıkarmamak olmalı. Görsel düzeltme UI'sı OCR kalitesini kendi başına artırmaz: hücre bbox, başlık güveni, ad/soyad ayrımı, sütun şeması ve insan onayı ayrı mimari gerektirir.

**Kabul:** Bir kaynak satırdaki farklı sütunlar ayrı ayrı gösterilir. Kullanıcı onayı olmadan düşük güvenli ad/soyad/numara kesin kayda girmez. Çift/çok sözcüklü ad desteklenir. Yanlış OCR sonucu silinmeden manuel düzeltme yapılabilir; toplu import transaction olarak tamamlanır.

### 3.4 Rubrik kütüphanesi / rubrik seçimi

**İhtiyaç:** Sınıf düzeyine uyan rubrik seçimi; ölçüt açıklamalarını hızlı görme; uzun ölçüt açıklamasıyla ekranın boğulmaması.

**Arc kombinasyonu:** `radio-cards`, `card`, `tabs`, `select`, `combobox`, `accordion`, `expandable-card`, `progress`, `badge`, `empty-state`, `skeleton`, `tooltip`.

**Kritik:** 11. sınıf rubrik içeriği yeni depoda **henüz bulunmuyor**. Arc kartlarının tasarımı test için sentetik rubriklerle geliştirilebilir ama eksik gerçek JSON gerçekmiş gibi eklenmez. Başka sınıfın rubriği yanlış sınıfa otomatik uygulanmaz.

### 3.5 Rubrik değerlendirme çalışma alanı — en yüksek öncelik

**İhtiyaç:** 3–4 niteliksel seçenek ve istendiğinde 0–max tamsayı girişi; parmakla puanlama; hızlı öğrenci geçişi; yeniden düzeltme/geri alma; eksik notun kesinleştirilmemesi.

**Birlikte kullanılacak Arc öğeleri:**

- `segmented-control`: 3–4 kısa niteliksel tercih, kayan aktif arka plan; gerçek puan eşlemesi yalnız rubrik kurallarından gelir.
- `elastic-slider`: parmak konumunu takip eden, sürükleme sırasında balon gösteren, sınır/detent davranışlı puan seçimi. `onValueChange` anlık görünüm, `onValueCommit` bırakma olayı; pointer cancel'da kaydetmeme.
- `number-field`: aynı puanı ± buton ve sayısal klavyeyle verebilme. Klavyeden doğrudan giriş TalkBack için alternatif.
- `slider`: rubber-band etkisiz veya aralıklı (örn. rapor) durumlarda alternatif; puan ekranında `elastic-slider` ile gereksiz çift kontrol göstermeme.
- `progress`, `badge`, `metric-card`, `animated-counter`: kaç ölçüt kaldı ve *yalnız tamamlanmışsa* kesin toplam; tamamlanmamışta “kesin not” yok.
- `floating-button-group`, `action-button`, `button-group`: önceki/sonraki/geri al; rahat tek elle erişim.
- `toast-stack`, `confirm-morph`: yanlış puanı düzeltme, geri alma, yerinde geri bildirim.
- `expandable-card`, `accordion`, `popover`: ölçüt açıklaması ve yönergeye ihtiyaç halinde erişim.

**Durum modeli:** `NotScored` / `Scored(0)` / `Scored(n)` birbirinden ayrı. Görsel sliderın varsayılan konumu **“0 puan verildi” anlamına gelmez**. Değeri null olan ölçütte kullanıcı fiilen bir puan seçmeden commit oluşamaz. Tekrarlanan pointer event'leri veya öğrenci seçiminin hızlı değişimi başka kayda yazamaz. Puan sınırları domain ve veri katmanında doğrulanır.

**Hareket ergonomisi:** Balon parmak üstünde okunur, kenardan taşmaz; 48dp+ dokunma hedefi. Başparmak balonu bir kalite özelliğidir, yavaş ve gereksiz sürüklenme animasyonu değildir. Sistemin reduced-motion ayarı işi bozmamalı; her pikselde Room'a kayıt yapılmamalı.

### 3.6 Grup hazırlığı ve bireysel puan ayrımı

`checkbox`, `avatar-group`, `multi-select`, `chip-group`, `card`, `progress`, `badge`, `filter-toolbar`. Bir grubun hazırlık maddeleri bağımsız tamamlanır; otomatik olarak bireysel 100 puanlık sonuca katılmaz. Henüz öğrenci-grup modelinin implementasyonu yoktur.

### 3.7 Sonuç, rapor ve istatistik

**Gereksinim:** Tam, kısmi, başlanmadı sınıf sayıları; kesin notların rubrik/öğrenci bazında tablo görünümü ve ölçütlü CSV.

**Arc kombinasyonu:** `stats-band` (block), `metric-card`, `progress`, `badge`, `sortable-data-table`, `filter-toolbar`, `segmented-control`, `bar-chart`, `donut-chart`, `animated-counter`, `gauge`, `sparkline`, `split-button`, `copy-button`. Bu grafikler not dağılımı ile **tamamlanma oranını iki farklı nicelik** olarak ele almalı. Kısmi notlar 0 gibi ortalamaya katılmaz; yeterli tarihsel veri olmadan trend, regresyon ve tahmini grafik gösterilmez.

`line-chart`, `activity-heatmap`, `timeline`, `slope-chart` gibi tarihsel bileşenler ancak gerçek geçmiş kayıtları tutulduğunda anlamlıdır. `streamgraph`, `ridgeline`, `treemap`, `brush-chart` zorunlu değil.

### 3.8 Ayarlar, içe/dışa aktarım ve yedekleme

`switch`, `select`, `theme-switch`, `radio-group`, `split-button`, `file-upload`, `file-dropzone`, `dialog`, `confirm-morph`, `hold-to-confirm` (P2), `alert`, `toast-stack`, `stepper`. CSV ve yedek için Android **Storage Access Framework** ve ContentResolver gerekir. Arc'ın UI onayı, bozulmuş dosya algılama / restore transaction / sürüm uyumluluğu yerine geçmez.

## 4. Adaptif tablet görünümü — önerilen yeni tasarım

### Yatay tablet (geniş)
```text
┌─────────────────────────────────────────────────────────────────────────┐
│ Puanla   11C  ›  Tiyatro Canlandırma     Kısmi: 8 / Tam: 11      [ ⋯ ] │
├───────────────────┬──────────────────────────────┬──────────────────────┤
│ Öğrenciler        │ Değerlendirme ölçütleri      │ Hızlı puan           │
│ [Ara][Filtre]     │ Ölçüt 1  [açıklamayı göster] │ Geliştirilebilir...  │
│ Tam ○ Kısmi ○     │ Ölçüt 2  ...                 │ [o----●----------o] │
│ 1. ...            │ Ölçüt 3  ...                 │       7 / 10        │
│ 2. ... (seçili)   │ Ölçüt 4  ...                 │ [-]  [7]  [+]       │
│ 3. ...            │                              │ Eksik 2 ölçüt       │
├───────────────────┴──────────────────────────────┴──────────────────────┤
│ [Önceki]       [Geri al]     Kaydedildi                 [Sonraki]       │
└─────────────────────────────────────────────────────────────────────────┘
```

Bu bir **başlangıç UX önerisi**, onaylanmış sabit tasarım değil. `resizable-panels` ile bölücüleri ayarlamak, gerektiğinde üçüncü paneli katlamak, uzun öğrenci listesini kaydırmak gerekir. Her öğrencinin puanının okunabilirliği asıl hedef; `card-stack` efekti için değerlendirme akışını kart oyununa dönüştürmek şart değil.

### Dikey tablet / dar ekran
Aynı bilgiyi tek seferde üç dar sütuna sıkıştırma yerine odaklı öğrenci+ölçüt değerlendirme, görünür geri/ileri kontrolü, kalıcı bağlam ve gerektiğinde bottom sheet. Geçişlerde yerel Compose adaptive sınıfları ve **dp genişliği** esas alınır; yalnız 1920×1200 fiziksel piksele göre kod yazılmaz. Katlanabilir ve çoklu pencere ölçüleri de düşünülür.

## 5. Native Compose aktarım mimarisi

Önerilen ilerideki modül/kod organizasyonu:

```text
:app
  feature/classes
  feature/import
  feature/rubrics
  feature/scoring
  feature/groups
  feature/results
  feature/settings
  domain
  data
:arc-compose
  foundation/       ArcTheme, color, typography, motion, focus
  controls/         button, segmented, slider, number, select, checkbox
  navigation/       tabs, adaptive header, pane divider, sheet, palette
  data-display/     table, filter, cards, progress, optional charts
  feedback/         status, alert, snackbar/undo, dialogs
  interaction/      pointer gesture, haptics, semantics
data/               onaylı rubrik JSON — korunur
```

**Not:** `:arc-compose` şu anda **yok**; klasörler/modüller henüz oluşturulmadı. Android projesi olgunlaşmadan her bileşene ayrı Gradle modülü açılmaz; paket düzeyi ayrım yeterlidir. UI bileşenleri ürün kimlik bilgisi veya Room bilmez; kontrollü props + callback kullanır. Compose animation `tween`, `spring`, `Animatable`, `animate*AsState`, `pointerInput`, `NestedScroll` ve semantics ile uyarlama yapılır. Arc'ın Web Motion `stiffness/damping` katsayıları ve CSS easingleri Compose parametrelerine **birebir eşit sanılmaz**, gözle ve ölçümle kalibre edilir.

**Paylaşılan kontrol aileleri:** `button/action-button/split-button/button-group/floating-button-group`; `slider/elastic-slider`; `select/morph-select/combobox`; `toast/toast-stack` — ortak token, ortak test ve API sözleşmesi üzerinden varyantlar halinde yürütülür. Böylece Arc'tan geniş yararlanılırken 108 ayrı karmaşık motor yaratılmaz.

## 6. Uygulama sırası (önerilen bağımlılığa göre)

| Faz | Uyarlanacak ana Arc öğeleri | Tamamlanma kanıtı |
|---|---|---|
| F0: Araç zinciri ve kaynak | Gradle Wrapper, CI, JSON gereksinimi, MIT attribution | Gerçek birim test ve debug derlemesi; mevcut kaynak JSON değiştirilmez |
| F1: Ortak Arc temeli | `arc-foundation`, `arc-motion-tokens`, `button`, `badge`, `card`, `input`, `alert`, `empty-state`, `toast-stack` | Light/dark, odak, 48dp dokunma, animasyon ölçeği testleri |
| F2: Tablet kabuğu | `page-header`, `resizable-panels`, `tabs`, `drawer`, `bottom-sheet`, `command-palette`, `floating-button-group` | Gerçek tablet yatay/dikey ve fiziksel klavye gezinme testleri |
| F3: Sınıf/öğrenci & veri | `sortable-data-table`, `search-field`, `combobox`, `filter-toolbar`, `chip-group`, `inline-edit`, `swipe-actions` | Öğrenci/kayıt izolasyonu ve düzenleme akışları; offline Room |
| F4: Rubrik & puan | `radio-cards`, `segmented-control`, `elastic-slider`, `number-field`, `progress`, `animated-counter`, `accordion` | Null/0 ve max puan testleri, gesture cancel, öğrenci değişimi sırasında yanlış commit olmaması |
| F5: Fotoğraf/OCR onayı | `file-upload`, `file-dropzone`, `stepper`, `image-compare`, `sortable-data-table`, `alert` | Gerçekçi anonim okul listeleri, sütun sızıntısı testleri, onaysız kaydetmeme |
| F6: Sonuç/grup/yedek | `stats-band`, `metric-card`, `bar-chart`, `donut-chart`, `checkbox`, `avatar-group`, `split-button`, `confirm-morph` | Kesin not istatistikleri, CSV round-trip, yedek/restore atomik |
| F7: Derin kalite / bağlı P1 | `notification-center`, `timeline`, `carousel`, `pagination`, `tree-view`, `gauge`, `theme-switch` ve ilgili P1 | Gerçek kullanım ihtiyacı + UI otomatik testleri; lüzumsuz animasyon yok |
| F8: İsteğe bağlı P2 | `image-compare` sonrası ileri OCR deneyleri, `activity-heatmap`, `slope-chart`, `card-stack`, `shortcut-recorder`, `signature-pad` vb. | Özelliğin tanımlı kullanım amacı, maliyet/fayda ve ayrı kabul testi |

`image-compare` F5'te P1 deneyi olarak kullanılabilir; F8, bu bileşenin daha ileri görsel inceleme varyantlarını kapsar. Tasarım bileşeni uyarlamaları ürün özellikleriyle birlikte test edilir; salt galeri portu çalışma tamamlanması sayılmaz.

## 7. Test matrisi ve başarı ölçütü

**Cihaz:** 11 inç 1920×1200, 90Hz Android tablet benzeri cihaz; yatay/dikey; pencere bölme; düşük genişlikte kompakt telefon; kalem/dokunma/fare/harici klavye.

**Erişilebilirlik:** TalkBack; 1.5x/2x sistem yazı ölçeği; 48dp+ etkin dokunma hedefi; renk harici ikon/metin durumları; sistem motion scale=0; yüksek kontrast; klavye Tab/ok/Enter/Escape eşdeğerleri.

**Performans:** Sürükleme sırasında veri tabanına piksel başı işlem yok; yalnız onCommit ve domain doğrulaması kalıcı kayıt. Lazy list ve stabil item key; sahne değişikliği nedeniyle gereksiz yeniden kompozisyon sınırlandırılır. Makrobenchmark/JankStats ile ölçülmeden 90 FPS iddiası kurulmaz.

**Veri güvenliği:** Öğrenci/şube/rubrik kombinasyonu id bazlı izole; null ≠ 0; hiçbir eksik not kesin not olmaz; OCR düşük güvenli alanlar onay bekler; restore ve CSV sonrası veri bütünlüğü doğrulanır.

**Başarı tanımı:** Arc uyarlamasının yüksek sayıda *işe yarayan* bileşeni uygulamanın ilgili üretim akışında çalışır; öğretmen deneme ekranından değil gerçek görevden puan verir. Kaynak lisans/commit ve Kotlin karşılıkları izlenebilir olur.

## 8. Riskler ve kaçınılacak yanlış yaklaşımlar

1. **108 bileşeni tek tek aynen port etmek:** JS/DOM bağımlılıkları, zaman ve bakım maliyetini artırır; ürünün tamamlanmasını geciktirir. Ortak API ve seçilmiş varyantlar daha çok iş görür.
2. **Arc'i WebView ile çalıştırmak:** Native Android kararını bozar; kullanmayacağız.
3. **Çok fazla animasyon:** Ders içi değerlendirme hızını düşürür; hayati buton ve puan görünürlüğü hareketten önce gelir.
4. **Kritik eylemi yalnız swipe/long press yapmak:** TalkBack, erişilebilirlik, yanlış dokunma ve keşfedilebilirlik sorunları yaratır.
5. **UI ile OCR/iş kuralını çözmeye çalışmak:** Sütun ayrımı görselle değil yerel OCR modeli ve kullanıcı doğrulamasıyla; puanlama domain sözleşmesiyle garanti edilir.
6. **Eksik rubrik verisini uydurmak:** 48 ölçütün tam metni/puanı ürün raporunda yok; gerçek kaynak lazım.
7. **Lisans unutmak:** Arc MIT telif/izin bildirimi substantial uyarlama kaynaklarında korunmalı; ücretli Arc Pro parçaları port listesine dahil değildir.

## 9. Net karar

**Önerilen strateji: “Arc tabanlı Puanla tasarım sistemi”** — 46 çekirdek + 30 güçlü tamamlayıcı registry girdisinin işlevlerinden olabildiğince yararlan; bunları Kotlin Compose'da tekrar kullanılabilir kontrollere çevir. P2 öğeleri gerçek Puanla ihtiyacına bağlandığı ölçüde genişlet. Veri güvenliğini ve kullanıcının yaptığı işi, kütüphane kullanım sayısının önünde tut.

Tüm kayıtların birebir karşılaştırması: [Arc 132 girdi × Puanla tam envanteri](../research/arc-puanla-full-catalog.md).
