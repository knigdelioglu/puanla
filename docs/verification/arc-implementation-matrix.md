# Puanla × Arc Library: 99 Kayıtlık Statik Entegrasyon Denetimi

**Tarih:** 10 Ekim 2026  
**Platform:** Android Native (Kotlin 2.0.21 + Jetpack Compose + Material 3 + Room 2.6.1 + Google ML Kit)  
**Modül:** `:arc-compose` (Bağımsız, yeniden kullanılabilir Compose UI kütüphanesi, MIT Lisansı korumalı)  
**Kapsam:** `docs/research/arc-puanla-full-catalog.md` içindeki **P0 (46) + P1 (30) + P2 (23) = 99 kaydın tamamı**.


> **Düzeltme (2026-10-10):** Önceki rapordaki 99 adet "DOĞRULANDI" ibaresi kaynak kodu ve testler tarafından desteklenmiyordu. Aşağıdaki statüler `app/src/main` dosyalarında Arc API çağrısı aranarak belirlenen **statik kullanım durumu**dur. Çağrı bulunması, davranışın doğru çalıştığını kanıtlamaz. Sayım: **47 normal ekran çağrısı, 13 yalnız bileşen galerisi, 39 uygulamada çağrı bulunamadı**. Bu 99 kayıt için otomatik Android UI/etkileşim doğrulaması yok.

---

## 1. P0 Grubu (46 Kayıt — Çekirdek İş Akışları)

| # | Arc Kaydı | Öncelik | Compose API (`com.knigdelioglu.arc.compose.*`) | Puanla Ekranı / Kullanım Alanı | Durum |
|---|---|---|---|---|---|
| 1 | `arc-foundation` | P0 | `ArcTheme`, `ArcColors`, `ArcShapes`, `ArcSpacing` | Tüm uygulama kabuğu, açık/koyu mod ve yüzey hiyerarşisi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 2 | `arc-motion-tokens` | P0 | `ArcMotionTokens`, `ArcMotion` (Yay, Snappy, Smooth) | Tüm ekranlar, reduced motion desteği ile animasyonlar | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 3 | `button` | P0 | `ArcButton` (Primary, Secondary, Outline, Ghost, Danger) | Çalışma alanı, sınıf ekleme, kaydetme, onay butonları | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 4 | `action-button` | P0 | `ArcActionButton` (Kompakt ikon ve eylem butonu) | Önceki/sonraki öğrenci gezinmesi, sayı artırma/azaltma | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 5 | `split-button` | P0 | `ArcSplitButton` (Bölünmüş ana eylem ve açılır menü) | Puanları CSV olarak dışa aktar / Farklı formatta aktar | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 6 | `button-group` | P0 | `ArcButtonGroup` (Bitişik buton kümesi) | Önceki/sonraki öğrenci ve hızlı yönlendirme butonları | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 7 | `floating-button-group` | P0 | `ArcFloatingButtonGroup` (Yüzen alt eylem çubuğu) | Puanlama çalışma alanında sabit Önceki / Geri Al / Sonraki çubuğu | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 8 | `dropdown-menu` | P0 | `ArcDropdownMenu`, `ArcDropdownMenuItem` | Üst bar şube ve rubrik seçicileri, satır menüleri | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 9 | `drawer` | P0 | `ArcDrawer` (Yan çekmece paneli) | Tablet yan ayrıntı paneli, sınıf/öğrenci detayları | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 10 | `input` | P0 | `ArcInput` (Tek satırlı metin ve sayı girişi) | Öğrenci numarası, ad/soyad, sınıf adı girişleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 11 | `select` | P0 | `ArcSelect` (Açılır seçim kutusu) | Sınıf şubesi (9, 10, 11, 12) ve rubrik filtreleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 12 | `combobox` | P0 | `ArcCombobox` (Aramalı seçim kutusu) | Hızlı öğrenci arama ve doğrudan seçme alanı | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 13 | `checkbox` | P0 | `ArcCheckbox` (Özelleştirilmiş onay kutusu) | Grup çalışması 7 hazırlık maddesi ve OCR satır seçimi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 14 | `switch` | P0 | `ArcSwitch` (Fiziksel yaylı toggle anahtarı) | Koyu tema, titreşim, otomatik sonraki öğrenciye geçiş ayarları | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 15 | `multi-select` | P0 | `ArcMultiSelect` (Çoklu öğrenci/seçim kutusu) | Grup üyelerini seçme ve toplu öğrenci işlemleri | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 16 | `number-field` | P0 | `ArcNumberField` (Erişilebilir ± butonlu puan kontrolü) | Ölçüt puanını doğrudan tam sayı olarak verme (+/- butonlar) | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 17 | `search-field` | P0 | `ArcSearchField` (Arama alanı) | Öğrenci listesinde ad ve numara ile gerçek zamanlı arama | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 18 | `file-dropzone` | P0 | `ArcFileDropzone` (Dosya sürükle-bırak / tıklama alanı) | Sınıf listesi fotoğrafı seçme ve yerel JSON yedeği yükleme | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 19 | `radio-group` | P0 | `ArcRadioGroup` (Tekli seçim grubu) | Cinsiyet seçimi, rubrik türü ve dışa aktarım seçenekleri | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 20 | `segmented-control` | P0 | `ArcSegmentedControl` (Kayan hap arka planlı kontrol) | Ölçüt niteliksel düzeyleri (Yetersiz, Geliştirilmeli, İyi, Çok İyi) | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 21 | `dialog` | P0 | `ArcDialog` (Modal onay ve işlem penceresi) | Sınıf silme, yedekten geri yükleme ve öğrenci ekleme pencereleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 22 | `popover` | P0 | `ArcPopover` (Noktasal bağlamsal pencere) | Ölçüt yönergesi ve rubrik ipuçlarının açılır gösterimi | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 23 | `tooltip` | P0 | `ArcTooltip` (İpucu baloncuğu) | İkonlu butonların açıklamaları ve erişilebilirlik ipuçları | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 24 | `tabs` | P0 | `ArcTabs` (Sekmeli gezinme çubuğu) | Ana ekranlar (Değerlendirme, Sınıflar, OCR, Gruplar, Raporlar) | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 25 | `expandable-card` | P0 | `ArcExpandableCard` (Genişleyebilir kart) | Uzun ölçüt tanımları ve MEB rubrik yönerge ayrıntıları | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 26 | `alert` | P0 | `ArcAlert` (Info, Success, Warning, Danger uyarıları) | OCR belirsizlik uyarıları, veri koruma bilgilendirmesi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 27 | `progress` | P0 | `ArcProgress` (Çizgisel ilerleme çubuğu) | Rubrik tamamlanma yüzdesi, OCR işleme ilerlemesi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 28 | `skeleton` | P0 | `ArcSkeleton` (Yükleme iskeleti) | Yerel veritabanı okuma sırasında titremesiz yer tutucu | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 29 | `badge` | P0 | `ArcBadge` (Durum rozeti) | Tamamlandı, Kısmi, Başlanmadı ve Doğrulama Gerekiyor rozetleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 30 | `card` | P0 | `ArcCard` (Temel yüzey kartı) | Sınıf kartları, ölçüt kutuları, öğrenci özet kartları | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 31 | `metric-card` | P0 | `ArcMetricCard` (İstatistik ve KPI kartı) | Tamamlanan Öğrenci Sayısı, Sınıf Ortalaması, Kalan Öğrenci | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 32 | `empty-state` | P0 | `ArcEmptyState` (Boş durum yönlendirmesi) | Henüz sınıf eklenmediğinde, öğrenci listesi boşken gösterim | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 33 | `filter-toolbar` | P0 | `ArcFilterToolbar` (Filtre araç çubuğu) | Puanlama durumu (Tümü/Tam/Kısmi/Boş) ve şube filtreleme | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 34 | `sortable-data-table` | P0 | `ArcSortableDataTable` (Sıralanabilir veri tablosu) | Öğrenci listesi, OCR doğrulama tablosu, sonuçlar tablosu | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 35 | `swipe-actions` | P0 | `ArcSwipeActions` (Kaydırarak eylem yapma) | Öğrenci satırını sağa/sola kaydırarak puanla veya düzenle | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 36 | `slider` | P0 | `ArcSlider` (Standart kaydırıcı) | Ayarlar ekranı ve genel eşik aralıkları | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 37 | `elastic-slider` | P0 | `ArcElasticSlider` (Fiziksel elastik yaylı kaydırıcı) | Parmağın üstünde balon gösteren 0..max ölçüt puanlama kontrolü | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 38 | `inline-edit` | P0 | `ArcInlineEdit` (Yerinde doğrudan düzenleme) | Öğrenci adı, soyadı, numarası üzerinde doğrudan düzeltme | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 39 | `chip-group` | P0 | `ArcChipGroup` (Seçilebilir etiketler grubu) | 9, 10, 11, 12. sınıf filtre çipleri ve OCR filtreleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 40 | `bottom-sheet` | P0 | `ArcBottomSheet` (Alt sayfa modalı) | Dikey tablet veya kompakt modda puanlama ve filtre sayfası | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 41 | `resizable-panels` | P0 | `ArcResizablePanels` (Ayarlanabilir bölmeli paneller) | Tablette 3 panelli çalışma alanı (Öğrenciler ↔ Ölçütler ↔ Puan Kontrolü) | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 42 | `toast-stack` | P0 | `ArcToastStack` (Geri al destekli bildirim yığını) | "Puan kaydedildi [Geri Al]", "Yedek oluşturuldu" bildirimleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 43 | `stepper` | P0 | `ArcStepper` (Adım göstergesi) | OCR İçe Aktarma: 1. Fotoğraf Seç → 2. Tara → 3. Doğrula → 4. Kaydet | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 44 | `radio-cards` | P0 | `ArcRadioCards` (Seçilebilir büyük kartlar) | Değerlendirilecek rubrik kartı seçimi | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 45 | `file-upload` | P0 | `ArcFileUpload` (Fotoğraf seçici ve durum göstergesi) | Android Photo Picker ile sınıf listesi görseli yükleme | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 46 | `page-header` | P0 | `ArcPageHeader` (Çalışma alanı sayfa başlığı) | Şube, rubrik, seçili öğrenci ve hızlı aksiyonlar başlığı | **Normal ekran API çağrısı var (işlev teyitsiz)** |

---

## 2. P1 Grubu (30 Kayıt — Güçlü Tamamlayıcı Bileşenler)

| # | Arc Kaydı | Öncelik | Compose API (`com.knigdelioglu.arc.compose.*`) | Puanla Ekranı / Kullanım Alanı | Durum |
|---|---|---|---|---|---|
| 47 | `expanding-button-group` | P1 | `ArcExpandingButtonGroup` (Genişleyen buton grubu) | Kompakt araç çubuğunda odaklanan eylemin başlık açması | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 48 | `context-menu` | P1 | `ArcContextMenu` (Bağlam menüsü) | Kalem veya uzun basış ile öğrenci hızlı işlem menüsü | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 49 | `copy-button` | P1 | `ArcCopyButton` (Kopyalama butonu) | Öğrenci numarası veya CSV verisini tek dokunuşla kopyalama | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 50 | `theme-switch` | P1 | `ArcThemeSwitch` (Açık/koyu tema geçiş düğmesi) | Üst araç çubuğu ve Ayarlar ekranında tema değişimi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 51 | `avatar` | P1 | `ArcAvatar` (Baş harf avatarları) | Öğrenci ad-soyad baş harflerinden anonim profil görseli | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 52 | `avatar-group` | P1 | `ArcAvatarGroup` (İç içe geçmiş avatar grubu) | Grup çalışmasında grup üyelerinin avatarları | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 53 | `calendar` | P1 | `ArcCalendar` (Aylık takvim görünümü) | Değerlendirme oturumunun yapıldığı tarihi seçme | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 54 | `date-picker` | P1 | `ArcDatePicker` (Tarih seçici) | Rapor filtrelerinde değerlendirme tarihi filtreleme | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 55 | `accordion` | P1 | `ArcAccordion` (Açılır kapanır akordeon paneller) | MEB rubrik yönergeleri ve değerlendirme kriterleri yardım alanı | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 56 | `breadcrumb` | P1 | `ArcBreadcrumb` (Ekmek kırıntısı hiyerarşik yol) | Sınıf › Şube › Rubrik › Öğrenci hiyerarşisi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 57 | `toast` | P1 | `ArcToast` (Tekli bildirim) | Hızlı bilgi mesajları ve durum geri bildirimleri | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 58 | `tree-view` | P1 | `ArcTreeView` (Hiyerarşik ağaç görünümü) | MEB Rubrik Tema › Öğrenme Çıktısı › Ölçütler ağacı | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 59 | `pagination` | P1 | `ArcPagination` (Sayfalama kontrolü) | Kalabalık sınıf ve çok sayıda sonuç listelerinde sayfalama | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 60 | `sparkline` | P1 | `ArcSparkline` (Kompakt mini trend çizgisi) | Sınıfın değerlendirme süreci boyunca başarı eğilimi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 61 | `gauge` | P1 | `ArcGauge` (Dairesel gösterge ibresi) | Sınıfın değerlendirme tamamlanma yüzdesi göstergesi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 62 | `animated-counter` | P1 | `ArcAnimatedCounter` (Animasyonlu sayı sayacı) | Puan toplamı değiştikçe akıcı sayı geçişi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 63 | `expanding-search` | P1 | `ArcExpandingSearch` (Genişleyen arama çubuğu) | Tablet başlığında yer tasarrufu sağlayan arama butonu | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 64 | `usage-meter` | P1 | `ArcUsageMeter` (Kapasite ve oran ölçer) | Değerlendirilen / Kalan öğrenci kota ve oran çubuğu | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 65 | `image-compare` | P1 | `ArcImageCompare` (Görüntü karşılaştırma sürgüsü) | OCR kaynak fotoğrafı ile işlenen metin bölgesi karşılaştırması | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 66 | `carousel` | P1 | `ArcCarousel` (Çoklu sayfa karuseli) | Çok sayfalı sınıf listesi fotoğrafları arasında kaydırma | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 67 | `bar-chart` | P1 | `ArcBarChart` (Sütun grafik) | Sınıfın not dağılımı (0-49, 50-69, 70-84, 85-100) | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 68 | `timeline` | P1 | `ArcTimeline` (Zaman çizelgesi) | Değerlendirme geçmişi, yedek alma ve OCR denetim günlüğü | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 69 | `user-menu` | P1 | `ArcUserMenu` (Öğretmen profil ve işlem menüsü) | Sağ üst köşe öğretmen oturum/ayarlar menüsü | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 70 | `donut-chart` | P1 | `ArcDonutChart` (Halka grafik) | Tamamlanan, Kısmi ve Başlanmayan öğrenci oranları | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 71 | `confirm-morph` | P1 | `ArcConfirmMorph` (Dönüşümlü onay butonu) | Tekil öğrenciyi veya puanı yerinde onaylayarak silme | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 72 | `scroll-area` | P1 | `ArcScrollArea` (Özel kaydırma alanı) | Geniş panellerde pürüzsüz kaydırma ve gösterge | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 73 | `command-palette` | P1 | `ArcCommandPalette` (Komut paleti modalı) | Klavyeden Cmd+K / Ctrl+K ile öğrenci, rubrik ve eylem arama | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 74 | `notification-center` | P1 | `ArcNotificationCenter` (Bildirim merkezi) | OCR sonuçları, yedekleme bildirimleri ve sistem uyarıları | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 75 | `empty-states` | P1 | `ArcEmptyStates` (Durumsal boş ekran şablonları) | Rapor yok, OCR taranmadı veya sınıf seçilmedi durumları | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 76 | `stats-band` | P1 | `ArcStatsBand` (Çoklu istatistik şeridi) | Raporlar ekranı üst özet bandı (Tam, Kısmi, Ortalama vb.) | **Normal ekran API çağrısı var (işlev teyitsiz)** |

---

## 3. P2 Grubu (23 Kayıt — Gelişmiş Etkileşim ve Özelleştirme)

| # | Arc Kaydı | Öncelik | Compose API (`com.knigdelioglu.arc.compose.*`) | Puanla Ekranı / Kullanım Alanı | Durum |
|---|---|---|---|---|---|
| 77 | `textarea` | P2 | `ArcTextarea` (Çok satırlı metin alanı) | Öğretmenin öğrenciye özel niteliksel gözlem/kanıt notu | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 78 | `tag-input` | P2 | `ArcTagInput` (Etiket ekleme ve kaldırma alanı) | Öğrencilere veya gruplara özel etiketler atama | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 79 | `time-picker` | P2 | `ArcTimePicker` (Saat seçici) | Değerlendirme başlangıç/bitiş saati belirleme | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 80 | `text-reveal` | P2 | `ArcTextReveal` (Yazı açığa çıkma efekti) | İlk karşılama ekranı ve çalışma alanı yönerge sunumu | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 81 | `text-morph` | P2 | `ArcTextMorph` (Metin dönüşüm animasyonu) | "Puanlandı" ↔ "Kaydedildi" durum geçişi metni | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 82 | `text-shimmer` | P2 | `ArcTextShimmer` (Işıltılı metin animasyonu) | "Sınıf listesi taranıyor..." OCR işleme durumu | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 83 | `hold-to-confirm` | P2 | `ArcHoldToConfirm` (Basılı tutarak onaylama butonu) | Tüm sınıf puanlarını sıfırlama veya yedeği geri yükleme | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 84 | `card-stack` | P2 | `ArcCardStack` (Kart destesi etkileşimi) | Öğrencileri kart destesi şeklinde tek tek gözden geçirme | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 85 | `activity-heatmap` | P2 | `ArcActivityHeatmap` (Etkinlik ısı haritası) | Öğretmenin haftalık/aylık puanlama yoğunluğu haritası | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 86 | `signature-pad` | P2 | `ArcSignaturePad` (Kalem/Stylus imza ve çizim alanı) | Android Stylus kalem ile öğretmen onay imzası / el notu | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 87 | `date-range-picker` | P2 | `ArcDateRangePicker` (Tarih aralığı seçici) | Rapor filtrelerinde dönemlik/tarih aralıklı filtreleme | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 88 | `color-picker` | P2 | `ArcColorPicker` (Renk seçici palet) | Şube veya grup renklerini kişiselleştirme | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 89 | `morph-select` | P2 | `ArcMorphSelect` (Dönüşümlü seçim menüsü) | Rubrik ve şube seçiminde akıcı morph animasyonu | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 90 | `line-chart` | P2 | `ArcLineChart` (Çizgi grafik) | Öğrencinin dönem içi değerlendirme gelişim çizgisi | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 91 | `waffle-chart` | P2 | `ArcWaffleChart` (100 karelik durum ızgarası) | 100 üzerinden sınıf başarı ve tamamlanma yüzdesi | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 92 | `slope-chart` | P2 | `ArcSlopeChart` (Eğim grafiği) | 1. Değerlendirme ile 2. Değerlendirme arasındaki eğim | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 93 | `announcement-bar` | P2 | `ArcAnnouncementBar` (Duyuru çubuğu) | Çevrimdışı mod uyarısı veya yedekleme hatırlatması | **Yalnız galeri çağrısı (işlev teyitsiz)** |
| 94 | `json-viewer` | P2 | `ArcJsonViewer` (JSON görüntüleyici) | Rubrik kaynak JSON yapısı ve yedek dosyası inceleme | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 95 | `shortcut-recorder` | P2 | `ArcShortcutRecorder` (Klavye kısayolu kaydedici) | Tablet fiziksel klavye kısayollarını özelleştirme | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 96 | `slot-text` | P2 | `ArcSlotText` (Mekanik sayaç / slot yazı animasyonu) | Toplam puan hesaplanırken makara tarzı dönen rakamlar | **Normal ekran çağrısı yok (entegrasyon eksik)** |
| 97 | `changelog-feed` | P2 | `ArcChangelogFeed` (Sürüm geçmişi akışı) | Puanla güncellemeleri ve yenilikler paneli | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 98 | `faq-section` | P2 | `ArcFaqSection` (Sıkça sorulan sorular paneli) | Değerlendirme güvenliği, puanlama kuralları SSS paneli | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 99 | `comparison-table` | P2 | `ArcComparisonTable` (Karşılaştırma tablosu) | İki öğrencinin veya iki şubenin ölçüt bazında karşılaştırması | **Normal ekran çağrısı yok (entegrasyon eksik)** |

---

## 4. Geçerli doğrulama durumu

- Kodda `:arc-compose` ve `:app` modülleri mevcut.
- Bağımsız kaynak denetiminde 99 Arc kaydının **yalnızca 47'si** için üretim/uygulama ekranında API çağrısı görüldü. 13 kayıt katalogda, 39 kayıt normal ekranlarda çağrılmıyor.
- 99 ayrı Arc etkileşim testi veya gerçek tablet kullanım testi çalıştırılmış değil.
- [083deff için GitHub Actions](https://github.com/knigdelioglu/puanla/actions/runs/38083918813) birim test ve debug derlemesinin geçtiğini doğruladı; sonraki P0 commitlerinin sonucu ayrıca kontrol edilmelidir.
- Room veritabanı kurtarma, gerçek fotoğraf OCR ve öğrenci bazlı geri alma için cihaz veya enstrümantasyon testleri halen gerekli.
- Onaylı 11. sınıf rubrik verisi depoda henüz yok; uydurma yerleşik rubriklerin eklenmesi durdurulmuştur.
