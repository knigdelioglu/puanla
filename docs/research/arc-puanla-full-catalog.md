# Arc Library × Puanla: Tam Bileşen Envanteri ve Eşleme Matrisi

**Tarih:** 10 Ekim 2026 — **Puanla** `knigdelioglu/puanla@6ef38c9`, **Arc** `kuratlielia/arc-library@86330cc` (`main`).  
**Kapsam:** Arc'ın resmi `registry.json` manifestindeki **132 girdinin tamamı**: 108 `registry:ui`, 22 `registry:block`, 2 `registry:item`. Bu sayılar dosya/klasör sayısı değildir; örneğin `command-palette` ve `file-upload` kaynakları `registry/components` altında olsa da registry'de `block` türündedir. `theme-switch` ise birden fazla ayrı kayıt adıyla sunulur.

> **Dikkat:** Puanla yalnızca Kotlin + Jetpack Compose kullanır. "Kullanılacak" demek React/TSX dosyalarını eklemek değil, Arc davranış ve görsel sözleşmesini Kotlin Native karşılığında **uyarlamak** demektir. Bu belge mevcut uygulamaya entegre edilmiş özelliklerin listesi değildir.

## Sınıflandırma

- **P0:** Çekirdek iş akışları için tasarlanacak ve erken dönemde kullanılacak.
- **P1:** Ürün işlevlerini tamamlayan güçlü adaylar; ilgili ekran oluştuğunda uygulanacak.
- **P2:** Ancak somut ürün gereksinimi ortaya çıktığında veya opsiyonel deney olarak.
- **X:** Mevcut ürün hedefiyle ilişkisi çok zayıf; Arc kullanım sayısını şişirmek için alınmaz.

**Sayım:** P0 **46**, P1 **30**, P2 **23**, X **33** (toplam 132). P0/P1, planlanmış *uyarlama adayı* sayısıdır; uygulanmış bileşen sayısı **0**.

| Tür | Arc girdisi | Öncelik | Puanla işlevi / değerlendirme |
|---|---|---|---|

## Temel tasarım kayıtları

| Arc öğesi | Karar | Puanla karşılığı | Kaynak |
|---|---|---|---|
| `arc-foundation` | **P0** | Arc'ın renk, yüzey, odak, tipografi ve açık/koyu mod tokenları; Compose Theme temelini kurar | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/foundation.css) |
| `arc-motion-tokens` | **P0** | Arc'ın süre/yay/easing hareket sözlüğünü Compose animasyon tokenlarına uyarlama | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/motion-tokens.ts) |

## UI bileşenleri

| Arc öğesi | Karar | Puanla karşılığı | Kaynak |
|---|---|---|---|
| `button` | **P0** | Uygulamanın tüm ana eylemleri ve yükleme/disabled durumları | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/button/button.tsx) |
| `action-button` | **P0** | Öğrenci önceki/sonraki ve hızlı işlem araçları | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/action-button/action-button.tsx) |
| `split-button` | **P0** | Puanları dışa aktar / diğer dışa aktarma seçenekleri | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/split-button/split-button.tsx) |
| `button-group` | **P0** | Öğrenci gezinmesi ve yan yana ilgili eylemler | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/button-group/button-group.tsx) |
| `floating-button-group` | **P0** | Değerlendirmenin sabit ama görünür ileri/geri/geri al araçları | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/floating-button-group/floating-button-group.tsx) |
| `expanding-button-group` | **P1** | Dar araç çubuğunda odaklanan düğmenin etiket açması | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/expanding-button-group/expanding-button-group.tsx) |
| `dropdown-menu` | **P0** | Öğrenci, sınıf ve rubrik bağlamsal seçenekleri | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/dropdown-menu/dropdown-menu.tsx) |
| `context-menu` | **P1** | Fare/kalem sağ tık ile öğrenci ve ölçüt menüsü; görünür menü de şart | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/context-menu/context-menu.tsx) |
| `copy-button` | **P1** | Öğrenci numarası veya rapor tanımlayıcı değerini kopyalama | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/copy-button/copy-button.tsx) |
| `drawer` | **P0** | Geniş ekranda ayrıntı/ayar ve düzenleme yan paneli | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/drawer/drawer.tsx) |
| `theme-switch` | **P1** | Açık/koyu görünüm seçimi; web'e özgü wipe animasyonu taşınmaz | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/theme-switch/theme-switch.tsx) |
| `theme-switch-eclipse` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/theme-switch/theme-switch.tsx) |
| `theme-switch-split` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/theme-switch/theme-switch.tsx) |
| `theme-switch-rise` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/theme-switch/theme-switch.tsx) |
| `avatar` | **P1** | Öğrencinin baş harflerinden kişisel veri içermeyen ayırt edici işaret | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/avatar/avatar.tsx) |
| `avatar-group` | **P1** | Grup çalışmasındaki öğrenciler | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/avatar-group/avatar-group.tsx) |
| `input` | **P0** | Öğrenci numarası, ad/soyad, sınıf adı gibi tek satır alanlar | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/input/input.tsx) |
| `textarea` | **P2** | Öğretmen gözlem notu ileride eklenirse çok satırlı metin girişi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/textarea/textarea.tsx) |
| `select` | **P0** | Sınıf, şube, rubrik ve rapor tipi seçimi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/select/select.tsx) |
| `combobox` | **P0** | Çok öğrenci/sınıf/rubrik içinde arayarak seçim | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/combobox/combobox.tsx) |
| `checkbox` | **P0** | Grup hazırlık kontrolü ve OCR satır onayı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/checkbox/checkbox.tsx) |
| `switch` | **P0** | Otomatik geçiş, titreşim, animasyon vb. ayarlar | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/switch/switch.tsx) |
| `multi-select` | **P0** | Grup üyeleri, OCR satır onayı, çoklu öğrenci eylemleri | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/multi-select/multi-select.tsx) |
| `number-field` | **P0** | Ölçüt için zorunlu erişilebilir tam sayı puanı alternatifi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/number-field/number-field.tsx) |
| `password-field` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/password-field/password-field.tsx) |
| `search-field` | **P0** | Öğrenci ad/numara araması | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/search-field/search-field.tsx) |
| `tag-input` | **P2** | Kullanıcı etiketleri/performans görev grupları eklenirse | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/tag-input/tag-input.tsx) |
| `file-dropzone` | **P0** | Tablet fare/dosya sürükle-bırak ile sınıf listesi görseli içe alma; yükleme değil yerel seçme | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/file-dropzone/file-dropzone.tsx) |
| `radio-group` | **P0** | Tek seçim gerektiren diğer ayarlar ve dereceler | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/radio-group/radio-group.tsx) |
| `segmented-control` | **P0** | 3–4 niteliksel derece, durum filtreleri ve görünüm kipleri | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/segmented-control/segmented-control.tsx) |
| `calendar` | **P1** | Değerlendirme tarihi seçimi; yalnız oturum tarihi eklenirse | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/calendar/calendar.tsx) |
| `date-picker` | **P1** | Değerlendirme tarihli oturum seçimi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/date-picker/date-picker.tsx) |
| `time-picker` | **P2** | Değerlendirme oturumu saati tutulacaksa | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/time-picker/time-picker.tsx) |
| `accordion` | **P1** | Ölçüt yönergeleri, erişilebilir yardım ve açıklamalar | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/accordion/accordion.tsx) |
| `dialog` | **P0** | Kayıt silme, yedek geri yükleme gibi kritik kararlar | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/dialog/dialog.tsx) |
| `popover` | **P0** | Ölçüt açıklaması ve noktasal bağlam yardımı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/popover/popover.tsx) |
| `tooltip` | **P0** | Özellikle ikon ağırlıklı tablet kısayolları | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/tooltip/tooltip.tsx) |
| `tabs` | **P0** | Şubeler, rubrikler ve kısmi/tamam değerlendirme görünümleri | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/tabs/tabs.tsx) |
| `expandable-card` | **P0** | Uzun rubrik açıklaması ve kanıt notunu isteğe bağlı açma | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/expandable-card/expandable-card.tsx) |
| `breadcrumb` | **P1** | Sınıf → rubrik → öğrenci bağlamı (özellikle geniş görünüm) | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/breadcrumb/breadcrumb.tsx) |
| `alert` | **P0** | OCR belirsizliği, yükleme ve veri güvenliği uyarıları | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/alert/alert.tsx) |
| `toast` | **P1** | Tek mesajlı basit kayıt/başarı geri bildirimi; toast-stack ile tek hizmet seçilmeli | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/toast/toast.tsx) |
| `progress` | **P0** | Rubriğin tamamlanan ölçüt yüzdesi ve OCR onay akışı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/progress/progress.tsx) |
| `skeleton` | **P0** | Yerel veri yüklenirken titremeyen yer tutucu | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/skeleton/skeleton.tsx) |
| `badge` | **P0** | Tamamlandı / kısmi / başlanmadı / doğrulanacak etiketleri | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/badge/badge.tsx) |
| `card` | **P0** | Sınıf, rubrik ve ölçüt özetlerinin ortak yüzeyi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/card/card.tsx) |
| `metric-card` | **P0** | Tam/kısmi/başlanmadı sayıları ve yalnız kesin notların ortalaması | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/metric-card/metric-card.tsx) |
| `empty-state` | **P0** | Henüz sınıf, rubrik veya sonuç yoksa yönlendirme | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/empty-state/empty-state.tsx) |
| `tree-view` | **P1** | İleride karmaşık rubrik hiyerarşisi oluşursa ölçüt ağacı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/tree-view/tree-view.tsx) |
| `pagination` | **P1** | Çok büyük sınıf/sonuç kümelerinde sayfa geçişi, LazyColumn alternatifi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/pagination/pagination.tsx) |
| `filter-toolbar` | **P0** | Öğrenci listesinde tamam/eksik, şube ve rubrik filtresi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/filter-toolbar/filter-toolbar.tsx) |
| `sortable-data-table` | **P0** | OCR onay tablosu, öğrenci listesi ve sonuç/CSV önizlemesi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/sortable-data-table/sortable-data-table.tsx) |
| `sparkline` | **P1** | Sınıfın zaman içindeki tamamlanmış not eğilimi; veri yeterliyse | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/sparkline/sparkline.tsx) |
| `gauge` | **P1** | Bir rubriğin tamamlanma durumu; 100 puan ile karıştırılmamalı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/gauge/gauge.tsx) |
| `animated-counter` | **P1** | Toplam ve ilerleme değişiminde sakin sayı güncellemesi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/animated-counter/animated-counter.tsx) |
| `code-block` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/code-block/code-block.tsx) |
| `text-reveal` | **P2** | İlk yardım/onboarding metninde isteğe bağlı tek seferlik hareket | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/text-reveal/text-reveal.tsx) |
| `in-view-title` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/in-view-title/in-view-title.tsx) |
| `text-morph` | **P2** | Kaydedildi/düzenleniyor küçük etiket dönüşümü | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/text-morph/text-morph.tsx) |
| `text-shimmer` | **P2** | OCR işleniyor metninde sınırlı hareket; reduced motion şart | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/text-shimmer/text-shimmer.tsx) |
| `hold-to-confirm` | **P2** | Toplu kalıcı silme; her eylemde kullanılmamalı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/hold-to-confirm/hold-to-confirm.tsx) |
| `swipe-actions` | **P0** | Öğrenci satırında düzenle/atla gibi yardımcı işlemler; görünür alternatifle | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/swipe-actions/swipe-actions.tsx) |
| `slider` | **P0** | Puan aralığı ve rapor filtresi; elastic yerine kontrollü düz kaydırıcı seçeneği | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/slider/slider.tsx) |
| `elastic-slider` | **P0** | Ölçüt puanını 0..max aralığında dokunarak/sürükleyerek seçme | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/elastic-slider/elastic-slider.tsx) |
| `inline-edit` | **P0** | Öğrenci adı/soyadı/numarası ve sınıf etiketi düzeltme | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/inline-edit/inline-edit.tsx) |
| `expanding-search` | **P1** | Yer tasarruflu genişleyebilen öğrenci arama alanı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/expanding-search/expanding-search.tsx) |
| `chip-group` | **P0** | Rubrik, sınıf, puan durumu ve OCR güven filtresi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/chip-group/chip-group.tsx) |
| `password-strength` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/password-strength/password-strength.tsx) |
| `bottom-sheet` | **P0** | Dar ekranda rubrik, puan düzeltme ve filtre ayrıntısı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/bottom-sheet/bottom-sheet.tsx) |
| `hover-card` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/hover-card/hover-card.tsx) |
| `resizable-panels` | **P0** | Tablet üzerinde öğrenci listesi / ölçütler / puan alanını kullanıcıya ayarlanabilir bölme | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/resizable-panels/resizable-panels.tsx) |
| `toast-stack` | **P0** | Kaydedildi, düzeltildi, geri al ve hata bildirimi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/toast-stack/toast-stack.tsx) |
| `usage-meter` | **P1** | Tamamlanan / eksik değerlendirme kapasitesi; yalnız doğru anlamda | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/usage-meter/usage-meter.tsx) |
| `image-compare` | **P1** | OCR kaynak fotoğraf ile düzeltilmiş/ön işlenmiş görüntüyü fark denetimi için karşılaştırma | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/image-compare/image-compare.tsx) |
| `carousel` | **P1** | Birden fazla fotoğraflı OCR incelemesinde sayfalar arasında gezinti | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/carousel/carousel.tsx) |
| `card-stack` | **P2** | Öğrenci triage benzeri tek tek gözden geçirme; puanı sağ/sol swipe'a bağlamak riskli | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/card-stack/card-stack.tsx) |
| `bar-chart` | **P1** | Sınıf/rubrik bazlı tamamlanmış puan dağılımı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/bar-chart/bar-chart.tsx) |
| `activity-heatmap` | **P2** | Değerlendirme günleri yoğunluğu; geçmiş kayıt varsa | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/activity-heatmap/activity-heatmap.tsx) |
| `timeline` | **P1** | Düzeltme, yedek alma ve değerlendirme işlem geçmişi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/timeline/timeline.tsx) |
| `user-menu` | **P1** | Hesap yoksa yerel profil/ayar menüsüne adapte edilir | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/user-menu/user-menu.tsx) |
| `stepper` | **P0** | Fotoğraf seç → OCR → gözden geçir → onayla | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/stepper/stepper.tsx) |
| `signature-pad` | **P2** | Kalemle performans notu işaretleme; ürün kapsamı genişlerse | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/signature-pad/signature-pad.tsx) |
| `date-range-picker` | **P2** | Uzun vadeli değerlendirme geçmişi rapor filtresi | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/date-range-picker/date-range-picker.tsx) |
| `color-picker` | **P2** | Kişiselleştirme, ölçüt renkleri (core ihtiyacı değil) | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/color-picker/color-picker.tsx) |
| `morph-select` | **P2** | Rubrik seçiminin daha akıcı varyantı; standart select varken ikinci tasarım şart değil | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/morph-select/morph-select.tsx) |
| `line-chart` | **P2** | Tarihler arasında kesin not eğilimi; oturum geçmişi oluşunca | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/line-chart/line-chart.tsx) |
| `donut-chart` | **P1** | Sınıfın tamamlanan/kısmi/başlanmadı dağılımı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/donut-chart/donut-chart.tsx) |
| `streamgraph` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/streamgraph/streamgraph.tsx) |
| `brush-chart` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/brush-chart/brush-chart.tsx) |
| `ridgeline` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/ridgeline/ridgeline.tsx) |
| `treemap` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/treemap/treemap.tsx) |
| `waffle-chart` | **P2** | 100 öğrencilik/ yüzde bazlı durum dağılımı, sınıf kalabalığına göre anlamlıysa | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/waffle-chart/waffle-chart.tsx) |
| `slope-chart` | **P2** | Önceki/sonraki performans puanı karşılaştırması, aynı rubrik veri modeli varsa | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/slope-chart/slope-chart.tsx) |
| `countdown` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/countdown/countdown.tsx) |
| `announcement-bar` | **P2** | Geri yükleme veya yerel toplu işlem hakkında kritik kalıcı uyarı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/announcement-bar/announcement-bar.tsx) |
| `json-viewer` | **P2** | Rubrik JSON geliştirici/teşhis görünümü; öğretmen ana arayüzüne gerek yok | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/json-viewer/json-viewer.tsx) |
| `phone-input` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/phone-input/phone-input.tsx) |
| `money-input` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/money-input/money-input.tsx) |
| `shortcut-recorder` | **P2** | Fiziksel klavyeli tablet için kullanıcı tanımlı kısayollar | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/shortcut-recorder/shortcut-recorder.tsx) |
| `confirm-morph` | **P1** | Tek kaydı silme/yerinde geri alma gibi küçük önemli eylemler | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/confirm-morph/confirm-morph.tsx) |
| `mention-input` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/mention-input/mention-input.tsx) |
| `chat-thread` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/chat-thread/chat-thread.tsx) |
| `rich-text-editor` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/rich-text-editor/rich-text-editor.tsx) |
| `billing-toggle` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/billing-toggle/billing-toggle.tsx) |
| `scroll-area` | **P1** | Geniş tablet panellerinde kenar taşma farkındalığı; Android yerel kaydırma | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/scroll-area/scroll-area.tsx) |
| `radio-cards` | **P0** | Rubrik/değerlendirme türü ve niteliksel seçim kartları | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/radio-cards/radio-cards.tsx) |
| `comment-thread` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/comment-thread/comment-thread.tsx) |
| `slot-text` | **P2** | Puan animasyonu için deneysel; dikkat dağıtma ve yanlış değeri okuma riski | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/slot-text/slot-text.tsx) |

## Hazır bloklar

| Arc öğesi | Karar | Puanla karşılığı | Kaynak |
|---|---|---|---|
| `signup-form` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/signup-form/signup-form.tsx) |
| `logo-marquee` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/logo-marquee/logo-marquee.tsx) |
| `plan-comparison` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/plan-comparison/plan-comparison.tsx) |
| `command-palette` | **P1** | Fiziksel klavye kullanan tablette hızlı öğrenci/rubrik/eylem arama | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/command-palette/command-palette.tsx) |
| `notification-center` | **P1** | İçe aktarma sorunları ve yerel işlem geçmişi; uzaktan bildirim değil | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/notification-center/notification-center.tsx) |
| `file-upload` | **P0** | OCR fotoğrafı ve yerel JSON yedeği için seçme/önizleme/hata akışı; buluta yükleme kullanılmaz | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/file-upload/file-upload.tsx) |
| `otp-input` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/components/otp-input/otp-input.tsx) |
| `changelog-feed` | **P2** | Uygulama sürüm notları bölümü eklenirse | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/changelog-feed/changelog-feed.tsx) |
| `sign-in` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/sign-in/sign-in.tsx) |
| `page-header` | **P0** | Sınıf-rubrik-öğrenci bağlamını taşıyan uyarlanabilir çalışma başlığı | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/page-header/page-header.tsx) |
| `empty-states` | **P1** | İlk kullanım, hiç rubrik yok, OCR boş, rapor boş için tutarlı görünüm seti | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/empty-states/empty-states.tsx) |
| `login-centered` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/login-centered/login-centered.tsx) |
| `site-header` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/site-header/site-header.tsx) |
| `site-footer` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/site-footer/site-footer.tsx) |
| `hero-section` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/hero-section/hero-section.tsx) |
| `faq-section` | **P2** | Uygulama içi yardım/SSS alanında uyarlanabilecek blok | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/faq-section/faq-section.tsx) |
| `contact-section` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/contact-section/contact-section.tsx) |
| `blog-grid` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/blog-grid/blog-grid.tsx) |
| `comparison-table` | **P2** | Birden fazla rubriği/performansı karşılaştırma görünümü eklenirse | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/comparison-table/comparison-table.tsx) |
| `stats-band` | **P1** | Sınıf genelinde tam/kısmi/boş sayaçlarının toplu sunumu | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/stats-band/stats-band.tsx) |
| `cta-section` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/cta-section/cta-section.tsx) |
| `newsletter-signup` | **X** | Mevcut Puanla ürün kapsamında karşılığı yok; başka bir ürün işlevi için tasarlanmış. | [kaynak](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/registry/blocks/newsletter-signup/newsletter-signup.tsx) |

## Ürün kapsamı ile tutarlılık

- **Çekirdek olmayan özellikler**, gerçek üretim ihtiyacı ve onaylı veri modeli olmadan geliştirilmemelidir: oturum zaman çizelgesi, geçmiş trendleri, kullanıcı hesabı, para/ödeme, sosyal özellikler.
- **OCR kalitesi Arc ile çözülemez.** Arc burada sadece doğru veri denetim ve düzenleme etkileşimine yardımcı olur. Hücre/kolon OCR modeli, yerel işleme ve kullanıcı onayı ayrı sorumluluktur.
- **Güvenilir not hesaplamasını Arc yapmaz.** `null != 0`, üst sınır, tamamlanma ve öğrenci/rubrik izolasyonu domain'de doğrulanır.
- **Eşdeğer varyantların hepsi yüklenmez.** Örn. `slider` / `elastic-slider`, `toast` / `toast-stack`, `select` / `morph-select`: ortak API ve seçilmiş davranış varyantları olabilir; ayrı 3 kütüphane zorunlu değildir.
- **Tema varyantları** ve hareket örnekleri çok sayıda sayılabilir ancak gerçek sınırlı UI değerinde tekrar oluşturulmaz.

Lisans: Arc [MIT](https://github.com/kuratlielia/arc-library/blob/86330cc9270c4acc55b7204c9583fcbaa378192c/LICENSE); substantial kaynak uyarlaması olursa telif/izin metni korunur. Arc Pro ücretli unsurları bu repoda olmadığı için kapsama alınmaz.
