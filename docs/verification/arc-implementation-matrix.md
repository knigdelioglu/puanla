# Puanla × Arc Library: 99 Kayıtlık Statik Entegrasyon Denetimi

> **Tarihsel anlık görüntü:** Bu matris 10 Ekim 2026'da Puanla'nın eski yerel `:arc-compose` modülü için hazırlanmıştır. 11 Ekim 2026'daki ortak `:design-system` geçişinden sonra dosya yolları ve bileşen durumları güncel kabul edilmemelidir. Güncel eşleşme ve geçiş durumu: [Compose Design System → Puanla entegrasyon raporu](../research/compose-design-system-puanla-integration.md).

**Tarih:** 10 Ekim 2026  
**Platform:** Android Native (Kotlin 2.0.21 + Jetpack Compose + Material 3 + Room 2.6.1 + Google ML Kit)  
**Modül:** `:arc-compose` (Bağımsız, yeniden kullanılabilir Compose UI kütüphanesi, MIT Lisansı korumalı)  
**Kapsam:** `docs/research/arc-puanla-full-catalog.md` içindeki **P0 (46) + P1 (30) + P2 (23) = 99 kaydın tamamı**.


> **Düzeltme (2026-10-10):** Önceki rapordaki 99 adet "DOĞRULANDI" ibaresi kaynak kodu ve testler tarafından desteklenmiyordu. Aşağıdaki statüler `app/src/main` dosyalarında Arc API çağrısı aranarak belirlenen **statik kullanım durumu**dur. Çağrı bulunması, davranışın doğru çalıştığını kanıtlamaz. **İlk statik sayım (10 Ekim 2026): 53 normal ekran bağlantısı, 10 yalnız galeri, 36 bağlantı bulunamadı.** Sonraki P1 değişikliklerinden sonra yeniden kapsamlı sayım yapılmamıştır; bu rakamlar güncel toplam değildir. Bu 99 kayıt için otomatik Android UI/etkileşim doğrulaması yok.

---

## 1. P0 Grubu (46 Kayıt — Çekirdek İş Akışları)

| # | Arc Kaydı | Öncelik | Compose API (`com.knigdelioglu.arc.compose.*`) | Puanla Ekranı / Kullanım Alanı | Durum |
|---|---|---|---|---|---|
| 1 | `arc-foundation` | P0 | `ArcTheme`, `ArcColors`, `ArcShapes`, `ArcSpacing` | Tüm uygulama kabuğu, açık/koyu mod ve yüzey hiyerarşisi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 2 | `arc-motion-tokens` | P0 | `ArcMotionTokens`, `ArcMotion` (Yay, Snappy, Smooth) | ArcElasticSlider animasyonları ve ArcTheme üzerinden azaltılmış hareket | **Dolaylı üretim kullanımı var; bütün animasyonlar denetlenmedi** |
| 3 | `button` | P0 | `ArcButton` (Primary, Secondary, Outline, Ghost, Danger) | Çalışma alanı, sınıf ekleme, kaydetme, onay butonları | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 4 | `action-button` | P0 | `ArcActionButton` (Kompakt ikon ve eylem butonu) | Üst bar işlem geçmişi ArcActionButton ve ArcNumberField +/- eylemleri | **Üretim API çağrısı var; cihazda erişilebilirlik doğrulanmalı**|
| 5 | `split-button` | P0 | `ArcSplitButton` (Bölünmüş ana eylem ve açılır menü) | Raporlarda CSV önizlemesi ve ayrı CSV panoya kopyalama eylemi | **ArcSplitButton üretim ekranında gerçek CSV işlemlerine bağlı**|
| 6 | `button-group` | P0 | `ArcButtonGroup` (Bitişik buton kümesi) | Raporlardaki tarih filtresini aç/sıfırla eylemleri | **ArcButtonGroup üretim ekranında filtre durumuna bağlı**|
| 7 | `floating-button-group` | P0 | `ArcFloatingButtonGroup` (Yüzen alt eylem çubuğu) | Puanlama ekranında Önceki / Geri Al / Sonraki | **Normal ekranda gerçek ViewModel eylemlerine bağlı; cihaz testi gerekli** |
| 8 | `dropdown-menu` | P0 | `ArcDropdownMenu`, `ArcDropdownMenuItem` | Raporlar CSV alternatifleri açılır menüsü | **ArcDropdownMenu panoya kopyalama işlemini açıyor**|
| 9 | `drawer` | P0 | `ArcDrawer` (Yan çekmece paneli) | Öğrenci tablosundan açılan ayrıntı/düzenleme paneli | **ArcDrawer gerçek öğrenci kimliği ve alan kaydıyla bağlı**|
| 10 | `input` | P0 | `ArcInput` (Tek satırlı metin ve sayı girişi) | Öğrenci numarası, ad/soyad, sınıf adı girişleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 11 | `select` | P0 | `ArcSelect` (Açılır seçim kutusu) | Sınıf şubesi (9, 10, 11, 12) ve rubrik filtreleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 12 | `combobox` | P0 | `ArcCombobox` (Aramalı seçim kutusu) | Puanlama ekranında numara/isimle doğrudan öğrenci seçimi | **ArcCombobox öğrenci seçimi ViewModel'e bağlı**|
| 13 | `checkbox` | P0 | `ArcCheckbox` (Özelleştirilmiş onay kutusu) | Grup çalışması 7 hazırlık maddesi ve OCR satır seçimi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 14 | `switch` | P0 | `ArcSwitch` (Fiziksel yaylı toggle anahtarı) | Koyu tema, titreşim, otomatik sonraki öğrenciye geçiş ayarları | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 15 | `multi-select` | P0 | `ArcMultiSelect` (Çoklu öğrenci/seçim kutusu) | Grup üyesi seçimindeki öğrenci ID kümesi | **Normal ekranda grup oluşturma callback'ine bağlı; cihaz testi gerekli** |
| 16 | `number-field` | P0 | `ArcNumberField` (Erişilebilir ± butonlu puan kontrolü) | Ölçüt puanını doğrudan tam sayı olarak verme (+/- butonlar) | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 17 | `search-field` | P0 | `ArcSearchField` (Arama alanı) | Öğrenci listesinde ad ve numara ile gerçek zamanlı arama | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 18 | `file-dropzone` | P0 | `ArcFileDropzone` (Dosya sürükle-bırak / tıklama alanı) | OCR fotoğraf seçicisi + Android URI sürükle/bırak (yedek SAF arayüzü ayrıdır) | **Doğrudan bağlı; gerçek çapraz uygulama drop cihaz testi gerekli** |
| 19 | `radio-group` | P0 | `ArcRadioGroup` (Tekli seçim grubu) | Sınıf öğrenci durum filtresi (Tümü / Tamamlanan / Kısmi / Puanlanmadı) | **ArcRadioGroup gerçek liste filtresine bağlı**|
| 20 | `segmented-control` | P0 | `ArcSegmentedControl` (Kayan hap arka planlı kontrol) | Ölçüt niteliksel düzeyleri (Yetersiz, Geliştirilmeli, İyi, Çok İyi) | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 21 | `dialog` | P0 | `ArcDialog` (Modal onay ve işlem penceresi) | Sınıf silme, yedekten geri yükleme ve öğrenci ekleme pencereleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 22 | `popover` | P0 | `ArcPopover` (Noktasal bağlamsal pencere) | Sınıf durum filtresi ve ölçüt yönergesinin bağlamsal açıklaması | **ArcPopover gerçek durum ve ölçüt açıklamalarını gösteriyor**|
| 23 | `tooltip` | P0 | `ArcTooltip` (İpucu baloncuğu) | Üst bardaki yerel işlem geçmişi eylemi | **Material3 gerçek tooltip ile normal ekran bağlantısı var; cihaz testi gerekli** |
| 24 | `tabs` | P0 | `ArcTabs` (Sekmeli gezinme çubuğu) | Ana ekranlar (Değerlendirme, Sınıflar, OCR, Gruplar, Raporlar) | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 25 | `expandable-card` | P0 | `ArcExpandableCard` (Genişleyebilir kart) | Uzun ölçüt tanımları ve MEB rubrik yönerge ayrıntıları | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 26 | `alert` | P0 | `ArcAlert` (Info, Success, Warning, Danger uyarıları) | OCR belirsizlik uyarıları, veri koruma bilgilendirmesi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 27 | `progress` | P0 | `ArcProgress` (Çizgisel ilerleme çubuğu) | Rubrik tamamlanma yüzdesi, OCR işleme ilerlemesi | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 28 | `skeleton` | P0 | `ArcSkeleton` (Yükleme iskeleti) | OCR sırasında yükleme yer tutucu satırları | **ArcSkeleton yalnız çalışan OCR yüklemesinde gösteriliyor**|
| 29 | `badge` | P0 | `ArcBadge` (Durum rozeti) | Tamamlandı, Kısmi, Başlanmadı ve Doğrulama Gerekiyor rozetleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 30 | `card` | P0 | `ArcCard` (Temel yüzey kartı) | Sınıf kartları, ölçüt kutuları, öğrenci özet kartları | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 31 | `metric-card` | P0 | `ArcMetricCard` (İstatistik ve KPI kartı) | Tamamlanan Öğrenci Sayısı, Sınıf Ortalaması, Kalan Öğrenci | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 32 | `empty-state` | P0 | `ArcEmptyState` (Boş durum yönlendirmesi) | Henüz sınıf eklenmediğinde, öğrenci listesi boşken gösterim | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 33 | `filter-toolbar` | P0 | `ArcFilterToolbar` (Filtre araç çubuğu) | Sınıflar ekranında sınıf düzeyi, değerlendirme durumu ve öğrenci araması | **ArcFilterToolbar gerçek filtrelenmiş öğrenci listesine bağlı**|
| 34 | `sortable-data-table` | P0 | `ArcSortableDataTable` (Sıralanabilir veri tablosu) | Öğrenci listesi, OCR doğrulama tablosu, sonuçlar tablosu | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 35 | `swipe-actions` | P0 | `ArcSwipeActions` (Kaydırarak eylem yapma) | Öğrenci numarası alanında kaydırarak düzenle/sil eylemleri | **ArcSwipeActions düzenleme çekmecesine; silme onayına bağlı**|
| 36 | `slider` | P0 | `ArcSlider` (Standart kaydırıcı) | OCR kaynak görsel karşılaştırmasında önizleme büyütme (1–2,5x) | **ArcSlider gerçek fotoğraf ölçeğine bağlı; veri kaydetmiyor**|
| 37 | `elastic-slider` | P0 | `ArcElasticSlider` (Fiziksel elastik yaylı kaydırıcı) | Parmağın üstünde balon gösteren 0..max ölçüt puanlama kontrolü | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 38 | `inline-edit` | P0 | `ArcInlineEdit` (Yerinde doğrudan düzenleme) | Öğrenci adı, soyadı, numarası üzerinde doğrudan düzeltme | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 39 | `chip-group` | P0 | `ArcChipGroup` (Seçilebilir etiketler grubu) | 9, 10, 11, 12. sınıf filtre çipleri ve OCR filtreleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 40 | `bottom-sheet` | P0 | `ArcBottomSheet` (Alt sayfa modalı) | Üst bardan açılan rubrik seçim sayfası | **ArcBottomSheet gerçek rubrik seçim menüsü sunuyor**|
| 41 | `resizable-panels` | P0 | `ArcResizablePanels` (Ayarlanabilir bölmeli paneller) | Tablette 3 panelli çalışma alanı (Öğrenciler ↔ Ölçütler ↔ Puan Kontrolü) | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 42 | `toast-stack` | P0 | `ArcToastStack` (Geri al destekli bildirim yığını) | "Puan kaydedildi [Geri Al]", "Yedek oluşturuldu" bildirimleri | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 43 | `stepper` | P0 | `ArcStepper` (Adım göstergesi) | OCR İçe Aktarma: 1. Fotoğraf Seç → 2. Tara → 3. Doğrula → 4. Kaydet | **Normal ekran API çağrısı var (işlev teyitsiz)** |
| 44 | `radio-cards` | P0 | `ArcRadioCards` (Seçilebilir büyük kartlar) | Sınıf düzeyine uygun rubrikleri seçmek için büyük kartlar | **ArcRadioCards gerçek rubrik seçimini ViewModel'e bağlıyor**|
| 45 | `file-upload` | P0 | `ArcFileUpload` (Fotoğraf seçici ve durum göstergesi) | OCR fotoğraf seçiminden sonra dosya adı/boyutu ve kaldırma eylemi | **ArcFileUpload gerçek ContentResolver dosya bilgisi ve kaldırma ile bağlı**|
| 46 | `page-header` | P0 | `ArcPageHeader` (Çalışma alanı sayfa başlığı) | Şube, rubrik, seçili öğrenci ve hızlı aksiyonlar başlığı | **Normal ekran API çağrısı var (işlev teyitsiz)** |

---

## 2. P1 Grubu (30 Kayıt — Güçlü Tamamlayıcı Bileşenler)

| # | Arc Kaydı | Öncelik | Compose API (`com.knigdelioglu.arc.compose.*`) | Puanla Ekranı / Kullanım Alanı | Durum |
|---|---|---|---|---|---|
| 47 | `expanding-button-group` | P1 | `ArcExpandingButtonGroup` (Genişleyen buton grubu) | Dar puanlama ekranında Öğrenciler / Ölçütler / Puanlama gezintisi | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek sekme durumu; tablet odak ve dokunma kanıtı yok |
| 48 | `context-menu` | P1 | `ArcContextMenu` (Bağlam menüsü) | Öğrenci numarasına uzun basarak puanlama veya okul numarasını kopyalama | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek eylem callback'leri; uzun basma/klavye testi yok |
| 49 | `copy-button` | P1 | `ArcCopyButton` (Kopyalama butonu) | Rapor CSV önizlemesinden tek dokunuşla kopyalama | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — panoya gerçek CSV içeriği kopyalanıyor |
| 50 | `theme-switch` | P1 | `ArcThemeSwitch` (Açık/koyu tema geçiş düğmesi) | Üst bar ve Ayarlar içinden kalıcı açık/koyu tema değişimi | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — yerel tercih kaydı bağlı |
| 51 | `avatar` | P1 | `ArcAvatar` (Baş harf avatarları) | Öğrenci baş harflerinden cihaz içi avatar | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek isimlerden türetiliyor; anonimlik korunuyor |
| 52 | `avatar-group` | P1 | `ArcAvatarGroup` (İç içe geçmiş avatar grubu) | Grup üyelerinin baş harfleri | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek grup üyeleri; UI testi yok |
| 53 | `calendar` | P1 | `ArcCalendar` (Aylık takvim görünümü) | Raporları son değişiklik tarihine göre aylık takvimden süzme | **DÜZELTİLDİ** — seçim filtreye yazılıyor, mevcut gün yeniden açmada seçili; `AssessmentDateFilterTest` |
| 54 | `date-picker` | P1 | `ArcDatePicker` (Tarih seçici) | Raporlardaki tarih aralığı seçimi | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — `ArcDateRangePicker` içinden çalışıyor; tarih filtre birim testi var |
| 55 | `accordion` | P1 | `ArcAccordion` (Açılır kapanır akordeon paneller) | Puanlama ölçüt yönergeleri ve puan düzeyleri | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — seçili rubriğin gerçek kriter metni |
| 56 | `breadcrumb` | P1 | `ArcBreadcrumb` (Ekmek kırıntısı hiyerarşik yol) | Seçili sınıf ve rubriğin üst bar hiyerarşisi | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — ViewModel seçiminden besleniyor |
| 57 | `toast` | P1 | `ArcToast` (Tekli bildirim) | Puan kaydetme, hata ve geri al mesajları | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — `ArcToastStack` alt bileşimi; `UndoHistoryTest` |
| 58 | `tree-view` | P1 | `ArcTreeView` (Hiyerarşik ağaç görünümü) | Gerçek rubrik ölçütlerinin açılır hiyerarşik seçimi | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — seçim gerçek ölçüt ID'siyle yapılıyor |
| 59 | `pagination` | P1 | `ArcPagination` (Sayfalama kontrolü) | Sınıf öğrenci listesinde 25 kayıtlık sayfalama | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — `ArcSortableDataTable` bileşimi; `ArcTablePagingTest` |
| 60 | `sparkline` | P1 | `ArcSparkline` (Kompakt mini trend çizgisi) | İlk sürümde öğrenci liste sırası notlarını eğilim gibi gösteriyordu | **BİLİNÇLİ DIŞARIDA** — zamana bağlı karşılaştırılabilir kayıt yok; sahte trend rapordan çıkarıldı |
| 61 | `gauge` | P1 | `ArcGauge` (Dairesel gösterge ibresi) | Değerlendirmesi tamamlanan öğrenci oranı | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek tamamlanma oranı |
| 62 | `animated-counter` | P1 | `ArcAnimatedCounter` (Animasyonlu sayı sayacı) | Seçili öğrencinin kesinleşmiş not sayacı | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — mevcut puan toplamı; hareket ölçeği cihazda doğrulanmadı |
| 63 | `expanding-search` | P1 | `ArcExpandingSearch` (Genişleyen arama çubuğu) | Sınıf ve öğrenci yönetiminde öğrenci arama | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek filtrelenmiş liste |
| 64 | `usage-meter` | P1 | `ArcUsageMeter` (Kapasite ve oran ölçer) | Tamamlanan ve kısmi puanlanan öğrenci oranı | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — mevcut sınıf değerlendirme sayıları |
| 65 | `image-compare` | P1 | `ArcImageCompare` (Görüntü karşılaştırma sürgüsü) | Kaynak OCR fotoğrafı ile tanınan metin taslağını kıyaslama | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek URI/aday; hücre kutusu OCR iddiası yok |
| 66 | `carousel` | P1 | `ArcCarousel` (Çoklu sayfa karuseli) | OCR doğrulamasında adayları tek tek inceleme/onaylama | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — onay tekil aday kimliğine bağlı |
| 67 | `bar-chart` | P1 | `ArcBarChart` (Sütun grafik) | Tamamlanmış notların dağılımı | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — kesin notlardan hesaplanıyor |
| 68 | `timeline` | P1 | `ArcTimeline` (Zaman çizelgesi) | Room denetim günlüğündeki gerçek işlemler | **DÜZELTİLDİ** — boş günlükte sahte “Sistem Başlatıldı” kaldırıldı; `AuditTimelineRecordsTest` |
| 69 | `user-menu` | P1 | `ArcUserMenu` (Öğretmen profil ve işlem menüsü) | Yerel Ayarlar'a geçiş ve akademik yıl | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — hesap/oturum açma iddiası yok |
| 70 | `donut-chart` | P1 | `ArcDonutChart` (Halka grafik) | Tamamlanan, kısmi ve başlanmayan öğrenci grupları | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek durum sayıları |
| 71 | `confirm-morph` | P1 | `ArcConfirmMorph` (Dönüşümlü onay butonu) | OCR aktarımı ve yedek geri yükleme | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek mutasyondan önce ikinci onay; yedek testleri mevcut |
| 72 | `scroll-area` | P1 | `ArcScrollArea` (Özel kaydırma alanı) | Ayarlar ekranının tek eksenli kontrollü kaydırılması | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek `ScrollState` ve sınır göstergesi |
| 73 | `command-palette` | P1 | `ArcCommandPalette` (Komut paleti modalı) | Klavyeden öğrenci, sınıf ve eylemlere geçiş | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek komut listesi; fiziksel klavye denemesi yok |
| 74 | `notification-center` | P1 | `ArcNotificationCenter` (Bildirim merkezi) | Yerel Room işlem geçmişini inceleme | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek `AuditLogEntity` kayıtları |
| 75 | `empty-states` | P1 | `ArcEmptyStates` (Durumsal boş ekran şablonları) | Sınıf bulunmadığında sınıf oluşturma çağrısı | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — gerçek oluşturma diyaloğu callback'i |
| 76 | `stats-band` | P1 | `ArcStatsBand` (Çoklu istatistik şeridi) | Sınıf metriklerinin üst özet şeridi | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — güncel gerçek değerlendirme sonuçları |

---

## 3. P2 Grubu (23 Kayıt — Gelişmiş Etkileşim ve Özelleştirme)

| # | Arc Kaydı | Öncelik | Compose API (`com.knigdelioglu.arc.compose.*`) | Puanla Ekranı / Kullanım Alanı | Durum |
|---|---|---|---|---|---|
| 77 | `textarea` | P2 | `ArcTextarea` (Çok satırlı metin alanı) | Öğretmenin öğrenciye özel niteliksel gözlem/kanıt notu | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — `CriterionScoreEntity.evidenceNote`; Room yedeği bu alanı taşır |
| 78 | `tag-input` | P2 | `ArcTagInput` (Etiket ekleme ve kaldırma alanı) | Öğrencilere veya gruplara özel etiket atama | **BİLİNÇLİ DIŞARIDA** — ürün kapsamındaki sınıf/puan/kanıt akışına etiket ihtiyacı yok; yeni kişisel alan gerekçesiz |
| 79 | `time-picker` | P2 | `ArcTimePicker` (Saat seçici) | Değerlendirme başlangıç/bitiş saati belirleme | **VERİ MODELİNE BAĞLI** — saklanan değerlendirme oturumu/başlangıç saati yok |
| 80 | `text-reveal` | P2 | `ArcTextReveal` (Yazı açığa çıkma efekti) | Karşılama ve yönerge | **BİLİNÇLİ DIŞARIDA** — bilgi sunumunu geciktirir; öğretim veya puanlama yararı yok |
| 81 | `text-morph` | P2 | `ArcTextMorph` (Metin dönüşüm animasyonu) | Puanlandı/kaydedildi durum metni | **BİLİNÇLİ DIŞARIDA** — kalıcı durum için okunabilir düz metin daha güvenilir; hareket gerekmez |
| 82 | `text-shimmer` | P2 | `ArcTextShimmer` (Işıltılı metin animasyonu) | OCR tarama durumu | **BİLİNÇLİ DIŞARIDA** — gerçek OCR beklemesi mevcut skeleton/progress ile açıklanıyor; shimmer dikkat dağıtır |
| 83 | `hold-to-confirm` | P2 | `ArcHoldToConfirm` (Basılı tutarak onaylama butonu) | Zor geri alınan sınıf silme işlemi | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — sınıf silme akışı; yedek restore mevcut iki aşamalı onayı koruyor |
| 84 | `card-stack` | P2 | `ArcCardStack` (Kart destesi etkileşimi) | Öğrencileri tek tek gözden geçirme | **BİLİNÇLİ DIŞARIDA** — OCR karuseliyle yinelenir ve puanlama hızını düşürür |
| 85 | `activity-heatmap` | P2 | `ArcActivityHeatmap` (Etkinlik ısı haritası) | Son 28 gündeki gerçek ölçüt puanı değişiklikleri | **DÜZELTİLDİ** — audit log zamanlarından üretilir; boş günler sıfır kalır; `AssessmentActivityHeatmapTest` |
| 86 | `signature-pad` | P2 | `ArcSignaturePad` (Kalem/Stylus imza ve çizim alanı) | Öğretmen imzası/el notu | **BİLİNÇLİ DIŞARIDA** — istenmedi; hiçbir ürün belgesi imza gerektirmiyor |
| 87 | `date-range-picker` | P2 | `ArcDateRangePicker` (Tarih aralığı seçici) | Raporlarda son değişiklik tarihi aralığı filtresi | **ENTEGRE / CİHAZ TESTİ BEKLİYOR** — `AssessmentDateFilterTest` tarih sırası/sınırlarını kapsıyor |
| 88 | `color-picker` | P2 | `ArcColorPicker` (Renk seçici palet) | Şube/grup renkleri | **BİLİNÇLİ DIŞARIDA** — kalıcı kullanıcı rengi tercihi ihtiyaca dönüşmedi; renk bilgi kodu değil |
| 89 | `morph-select` | P2 | `ArcMorphSelect` (Dönüşümlü seçim menüsü) | Rubrik ve şube seçimi | **BİLİNÇLİ DIŞARIDA** — arama/erişilebilirliği olan mevcut `ArcSelect`e işlev avantajı yok |
| 90 | `line-chart` | P2 | `ArcLineChart` (Çizgi grafik) | Öğrencinin değerlendirme gelişimi | **VERİ MODELİNE BAĞLI** — öğrenci/rubrik başına tek güncel değerlendirme var; karşılaştırılabilir dönem geçmişi yok |
| 91 | `waffle-chart` | P2 | `ArcWaffleChart` (100 karelik durum ızgarası) | Sınıf tamamlanma yüzdesi | **BİLİNÇLİ DIŞARIDA** — aynı tamamlanma oranı `ArcDonutChart`te mevcut; yeni karşılaştırma sağlamıyor |
| 92 | `slope-chart` | P2 | `ArcSlopeChart` (Eğim grafiği) | İki değerlendirme dönemi karşılaştırması | **VERİ MODELİNE BAĞLI** — iki ayrı değerlendirme dönemi ve tarihli sonuç saklanmıyor |
| 93 | `announcement-bar` | P2 | `ArcAnnouncementBar` (Duyuru çubuğu) | Koşullu yedekleme hatası/işlem engeli | **BİLİNÇLİ DIŞARIDA** — geçici hata mevcut toast ile görünür; kalıcı durum/bekleyen işlem modeli yok |
| 94 | `json-viewer` | P2 | `ArcJsonViewer` (JSON görüntüleyici) | Kaynak rubrik/yedek JSON inceleme | **BİLİNÇLİ DIŞARIDA** — son kullanıcıya JSON paneli istenmiyor; Ayarlar'dan kaldırıldı |
| 95 | `shortcut-recorder` | P2 | `ArcShortcutRecorder` (Klavye kısayolu kaydedici) | Kısayol özelleştirme | **BİLİNÇLİ DIŞARIDA** — çalışan özelleştirilebilir kısayol modeli yok; temsili kaydedici kaldırıldı |
| 96 | `slot-text` | P2 | `ArcSlotText` (Mekanik sayaç / slot yazı animasyonu) | Puan toplamını sunma | **BİLİNÇLİ DIŞARIDA** — `ArcAnimatedCounter` zaten sayıyı sunuyor; ekstra hareket işlem hızını düşürür |
| 97 | `changelog-feed` | P2 | `ArcChangelogFeed` (Sürüm geçmişi akışı) | Uygulama sürüm notları | **BİLİNÇLİ DIŞARIDA** — onaylı sürüm geçmişi kaynağı yok; statik/uydurma liste kaldırıldı |
| 98 | `faq-section` | P2 | `ArcFaqSection` (Sıkça sorulan sorular paneli) | SSS | **BİLİNÇLİ DIŞARIDA** — kullanıcı istemedi; gerçek ürün içeriği yok |
| 99 | `comparison-table` | P2 | `ArcComparisonTable` (Karşılaştırma tablosu) | Aynı sınıf ve rubrikte iki öğrencinin gerçek ölçüt sonuçları | **DÜZELTİLDİ** — rapor karşılaştırma penceresi; `StudentComparisonTest` null/0 ve ölçüt sırasını sınar |

---

## 4. Geçerli doğrulama durumu

- Kodda `:arc-compose` ve `:app` modülleri mevcut.
- Bağımsız kaynak denetiminde 99 Arc kaydı için ilk kapsam sayımı 53/10/36 idi (üretim ekranı / yalnız galeri / uygulamada çağrı yok); son eklenen ekran bağlantıları nedeniyle **güncel toplam değildir**.
- 99 ayrı Arc etkileşim testi veya gerçek tablet kullanım testi çalıştırılmış değil.
- [083deff için GitHub Actions](https://github.com/knigdelioglu/puanla/actions/runs/38083918813) birim test ve debug derlemesinin geçtiğini doğruladı; sonraki P0 commitlerinin sonucu ayrıca kontrol edilmelidir.
- Room veritabanı kurtarma, gerçek fotoğraf OCR ve öğrenci bazlı geri alma için cihaz veya enstrümantasyon testleri halen gerekli.
- Onaylı 11. sınıf rubrik verisi depoda henüz yok; uydurma yerleşik rubriklerin eklenmesi durdurulmuştur.

## P1 entegrasyon güncellemesi (11 Ekim 2026)

- Öğrenci listesi `pageSize=25` ile `ArcPagination` kullanır. Tüm kayıtlar sıralandıktan sonra sayfa dilimi alınır; `ArcTablePagingTest` üretim algoritmasını test eder.
- OCR doğrulaması artık gerçek seçilmiş görseli `ArcImageCompare` ile okuyabildiği OCR satırlarıyla karşılaştırır. Aktarma ikinci kez `ArcConfirmMorph` üzerinden onaylanır.
- Raporlar ekranı `ArcUsageMeter` ile en az bir ölçütü puanlanmış öğrencileri gösterir.
- Başlıktaki işlem geçmişi eylemi `ArcNotificationCenter` üzerinde gerçek Room audit loglarını gösterir; sahte bildirim veya sunucu verisi kullanılmaz.
- Bunlar kod bağlantılarıdır. Gerçek tablet üzerinde OCR görseli, kaydırma, sayfalama, erişilebilirlik ve pencere kapanışı uçtan uca doğrulanmamıştır; **99/99 başarı iddiası hâlâ geçerli değildir**.

## 11 Ekim 2026 — P1 tarih ve yönerge aşaması

- `ArcDatePicker` artık sabit Ekim 2026 metni yerine Material3'ün gerçek tarih penceresini kullanır; seçilen ISO tarih Android UTC gün semantiğiyle döndürülür.
- `ArcDateRangePicker` başlangıç/bitiş tarihlerini rapor ekranına taşır. Filtre yalnız mevcut değerlendirmenin `lastModifiedAt` zamanına bakar; **geçmiş not anlık görüntüsü değildir**.
- `AssessmentDateFilterTest` dahilî gün sınırlarını, açık uçlu aralıkları ve geçersiz tarihleri üretim filtresiyle sınar.
- `ArcCalendar` ayları ileri/geri alır; ay uzunluğu ve hafta başlangıcını hesaba katar. `ReportsScreen` seçilen ISO tarihi filtreye aktarır ve mevcut tarihi tekrar açıldığında vurgular.
- `ArcAccordion` seçili ölçütün **mevcut** yönergesini ve gerçek puan düzeyi açıklamalarını açar; olmayan içerik uydurulmaz.
- Puanlama gezintisi, arama genişlemesi, accordion ve yerinde onay boyut/geçiş animasyonları Arc reduced-motion ayarı açıkken snap/transition-none davranışına geçer.
- `ArcExpandingSearch` öğrenci listesinde; `ArcEmptyStates.NoClassrooms` sınıf oluşturma durumunda kullanılır.
- Bu aşama gerçek cihaz / TalkBack / dokunma ve veri tutarlılığı için tam entegrasyon testi yerine geçmez.

## 11 Ekim 2026 — Öğrenci hızlı işlemleri

- Öğrenci tablosunda numara hücresine uzun basma `ArcContextMenu` açar. `ArcContextAction` eylemleri menüyü kapatıp seçili öğrencinin gerçek puanlama ekranına gider veya okul numarasını sistem panosuna kopyalar.
- Eski katalog `menuContent` API'si korunmuştur; gerçek tablet uzun basma, fare sağ tıklama ve erişilebilirlik henüz uçtan uca test edilmemiştir.

## 11 Ekim 2026 — Veri güvenliği ve kayıt sıralaması (P1)

- **Geri yükleme için iki aşama:** SAF dosyası en fazla 16 MiB olarak okunur; JSON şeması ve tüm tablo ilişkileri kayıt değiştirilmeden önce doğrulanır. Görünen yedek sayıları sonrası ayrı bir `ArcConfirmMorph` eylemiyle üzerine yazma onaylanır. Dosya seçiminin kendisi asla yedeği uygulamaz.
- **Geri alma bütünlüğü:** Aynı öğrenci-rubrik-ölçüt yazımları `Mutex` ile sıralanır. Puan değiştirilirken gözlem notu korunur. Bir sonraki işlem puanı değiştirmişse önceki puan geri alma eylemi yeni değeri ezmez.
- Başarılı restore eski oturumun geri alma kuyruğunu siler ve eski veriyle ilişkili bekleyen not/puan yazımları iptal edilir.
- **Ayar temizliği:** Yalnızca hafızada değişip gerçekte klavye davranışını etkilemeyen kısayol kaydedici, hiçbir yere kaydedilmeyen imza alanı, temsili JSON/FAQ ve statik sürüm listesi Ayarlar'dan çıkarıldı. Arc katalog öğeleri olarak kalabilirler; ürün özelliği sayılmazlar.
- `BackupImportTest` geçerli özet, bozuk ilişki ve büyük dosya reddini sınar. Gerçek cihaz SAF seçimi, kapanan diyaloglar, eşzamanlı puan-geri alma ve büyük yedek/restore senaryoları için enstrümantasyon testleri hâlâ gereklidir.

## 11 Ekim 2026 — P1 ad/soyad ve geri alma tutarlılığı

- Hızlı puanlama tostu artık rastgele son Undo eylemini değil, oluşturduğu puan işleminin benzersiz ID'sini hedefler. Eski tosta basıldığında daha yeni öğrenci puanı geri alınmaz.
- `UndoHistory` bunun sıralamasını izler; başarısız veya iptal edilen eylemlere yeniden deneme olanağı tanır. `UndoHistoryTest` üretim sınıfını kullanır.
- Öğrenci ad ve soyad düzenlemeleri tek satırın tüm eski kopyasını kaydetmek yerine yalnız seçili alanı Room transaction'ı içinde günceller. Boş ad/soyad ve bozuk metinler yedeği geçersiz hâle getiremeden reddedilir (`StudentNameRulesTest`).
- Tekil öğrenci eklemede numara doğrulama ve benzersizlik kontrolü tek transaction'dadır; doğrulama hatalarında coroutine düşmek yerine uygulama hata sonucu döndürür.
- Tam yedek almadan önce tamamlanmamış puan ve gözlem notları beklenir. Restore öncesi iptal edilen puan / not / geri alma yazımları sonlanana dek beklenir. Bunlar henüz enstrümantasyon testi ile uçtan uca kanıtlanmış değildir.

## 11 Ekim 2026 — Ortak yazma koordinatörü ve doğrudan puan girişi

- Yedek almak yalnız puan ve not yazımlarını değil ViewModel'in bütün öğrenci, sınıf, onaylı OCR ve grup mutasyonlarını da tamamlanana kadar bekler. Restore bu yazımları iptal edip sonlanmalarını bekledikten sonra atomik yedeği uygular. Yeni yazım, veri transferi süresince reddedilir.
- `MutationCoordinatorTest` bekleme, iptal, yeni işleme izin vermeme ve hata sonrası serbest bırakma davranışını test eder. Gerçek veri tabanı ve tablet arayüzü için enstrümantasyon testi hâlâ açık.
- Grup kontrol listesi durumu artık UI'deki eski grup kopyası üzerinden yazılmaz; ilgili kayıt Room transaction'ında yeniden okunup `GroupChecklistRules` ile güncellenir.
- `ArcNumberField` mobil ve fiziksel klavyede tam sayı girişini sağlar. Giriş, tuşa her basışta değil IME onayı veya odak kaybında kaydedilir. Puanlanmamış alan 0 yerine boş çizgiyle gösterilir; 0 puan bilinçli şekilde girilebilir.

## 11 Ekim 2026 — P0 Arc işlevsel entegrasyon düzeltmeleri

- Puanlama ekranındaki önceki/geri al/sonraki sabit eylemleri gerçek `ArcFloatingButtonGroup` içine taşındı. Geri Al, ViewModel'in kimlik denetimli gerçek `undoLastAction` akışını çağırır; kaydırılan ölçüt içeriğinin dışında durur.
- Grup oluştururken üyeler artık `ArcMultiSelect` ile seçiliyor. Önceki satır + iç içe checkbox tıklamaları kaldırıldı; seçilen öğrenci kimlikleri `onConfirm` işlemine aktarılır.
- `ArcElasticSlider` içindeki başparmak büyüklüğü ve gölge animasyonları `ArcMotion.current.fastTween()` ile sistemdeki azaltılmış hareket tercihini gözetir. Bütün Arc animasyonları için kapsamlı reduced-motion denetimi hâlâ gereklidir.
- Tema ve azaltılmış hareket ayarları artık `arc_ui` SharedPreferences üzerinden uygulama yeniden açıldığında korunur. Eski kullanıcılarda tercih yoksa tema sistem tercihine döner.
- `ArcFileDropzone` seçicinin yanında Android Compose `dragAndDropTarget` ile URI kabul edebilir. OCR ekranı bunu gerçek bitmap decode akışına bağlar, `Activity.requestDragAndDropPermissions` iznini decode tamamlanana kadar tutar, bırakılan dosyanın görsel olduğunu doğrular; belge internet ortamına yüklenmez. `ArcImageDropPolicyTest` MIME kabul/red kurallarını sınar. Çoklu görselden ilk URI işlenir.
- Önceden boş `Box` sarmalayıcısı olan `ArcTooltip`, Material3 `TooltipBox` ile gerçek hover / uzun basma / odak ipucuna dönüştürüldü ve üst barda işlem geçmişi eylemine bağlandı.
- Uygulama kaynağındaki “99 bileşen içerir” yorumu kaldırıldı; katalogdaki demosu olan bileşen gerçek ürün entegrasyonu sayılamaz.
- **Sınır:** Bu değişiklikler yerel Android `:arc-compose` uyarlamasındadır; harici `kuratlielia/arc-library` deposuna hiçbir kod yazılmadı. Başka uygulamadan URI sürükleme, TalkBack ve uzun basma için gerçek tablet/enstrümantasyon testi hâlâ gereklidir.

## 11 Ekim 2026 — P0 matrisindeki kalan bileşenlerin üretim bağlantıları

- **Sınıf yönetimi:** `ArcFilterToolbar` sınıf ve değerlendirme durumunu filtreler. Durum seçenekleri `ArcPopover` içinde `ArcRadioGroup` ile seçilir. `ArcSwipeActions` öğrenci numarası hücresinde düzenleme veya onaylı silmeye yönlendirir. `ArcDrawer` gerçek seçili öğrencinin ad/soyad güncellemesini ve puanlama geçişini yapar.
- **Puanlama:** `ArcCombobox` öğrenci adına/numarasına göre hızlı seçim yapar. Ölçütün kendi yönergesine erişen ek `ArcPopover` bulunur.
- **Rubrik:** Üst bardaki `ArcBottomSheet` içinde sınıf düzeyiyle filtrelenmiş `ArcRadioCards` seçimi gerçek `selectRubric` eylemine bağlıdır. Onaylı rubrik kaynağı olmadan hayalî veri gösterilmez.
- **Rapor:** `ArcSplitButton` ana CSV önizleme eylemi ile `ArcDropdownMenu` içindeki gerçek CSV panoya kopyalama eylemini ayırır; `ArcButtonGroup` tarih filtreleme eylemlerini gruplar. `ArcActionButton` zaten üst bardaki işlem geçmişine bağlıydı.
- **OCR:** `ArcFileUpload` önceki seçili görselin gerçek dosya bilgisini gösterir ve kaldırır. `ArcSkeleton` OCR taraması sırasında gösterilir. `ArcSlider`, fotoğraf/OCR karşılaştırmasında görüntü yakınlaştırmasını gerçekten değiştirir.
- **Sonuç:** P0 matrisindeki **46 kaydın tamamı en az bir normal üretim akışında doğrudan veya Arc alt bileşeni üzerinden kullanılmaktadır**; bu beyan kaynak kodu bağlantısını belirtir, *gerçek tablette uçtan uca doğrulama* değildir. Kalan P1/P2 katalog girdileri ürün kapsamından bağımsız değerlendirilir.
- **Gerçek cihazda test edilmeyenler:** kaydırma jestleri, drawer dışına dokunarak kapatma, modal sheet geri düğmesi, küçük ekranlarda menü taşması, dosya sürükle-bırak URI izinleri, uzun rubrik başlıkları, fiziksel klavye/TalkBack ve OCR/CSV SAF dosya izinleri.

## 11 Ekim 2026 — P1 üretim bağlantıları ve doğrulama kapsamı

- 30 P1 kaydının 27'si normal Android ekranından doğrudan çağrılıyor; 3'ü bileşen bileşimiyle gerçek işlev görüyor: `ArcDatePicker` (`ArcDateRangePicker` içinden), `ArcToast` (`ArcToastStack` içinden), `ArcPagination` (`ArcSortableDataTable` içinden). Bunları sırf istatistiği artırmak için ayrı birer kopya olarak eklemek gereksizdir.
- **Bu aşamada eklenenler:** `ArcExpandingButtonGroup` kompakt puanlama gezinmesine, `ArcTreeView` gerçek ölçüt IDsine, `ArcCalendar` rapor tarih filtresine, `ArcCarousel` OCR aday incelemesine, `ArcUserMenu` yerel Ayarlar geçişine bağlandı. `ArcScrollArea` önceki boş sarmalayıcı yerine gerçek kaydırma ve alt sınır göstergesi kullanıyor; Ayarlar sayfası bununla çalışıyor.
- `ArcCarousel` içindeki geçersiz güncel indeks, veri listesi küçülürken güvenli sınıra çekiliyor. OCR aday sayfasının açık onayı tekil aday kimliğine bağlanmıştır.
- **Yanlış algı oluşmaması için:** `ArcTreeView` MEB kaynaklı Tema/Öğrenme Çıktısı hiyerarşisi oluşturmaz; mevcut rubriğin gerçek ölçütlerinden iki düzeyli ağaç yapar. `ArcCalendar` değerlendirme oturumu geçmişini yazmaz; var olan değerlendirmelerin *son değişiklik* tarihini filtreler. `ArcCarousel` çoklu fotoğrafı tek PDF/OCR işine birleştirme özelliği değildir. `ArcUserMenu` bir kullanıcı hesabı veya bulut oturumu açmaz.
- **CI ve test sınırı:** Derleme/birim testleri yeni composable bağlantılarını doğrular; tablet üzerindeki odak, IME, dokunma, geniş/dar ekran, TalkBack, dışarı sürükleme ve modal kapatma deneyimini kapsamaz. Bu senaryolar yerel Android cihaz/enstrümantasyon testlerini gerektirir.

## 11 Ekim 2026 — P1/P2 tamamlaması ve satır bazlı kanıt

Güncel `origin/main` (`ec89105`) üzerinden çalışıldı. Kod değişiklikleri **[eb369db](https://github.com/knigdelioglu/puanla/commit/eb369db6d814e6e91335bd2ae6deec21552bfb44)** commit'indedir. Durumların özeti: P1'de #47–59 ve #61–67, #69–76 entegre fakat cihaz testi bekliyor; #53 ve #68 düzeltildi; #60 bilinçli dışarıda. P2'de #77, #83, #87 entegre/cihaz testi bekliyor; #79, #90, #92 veri modeline bağlı; #85 ve #99 düzeltildi; diğer P2 satırları bilinçli dışarıda.

| # | Arc kaynak dosyası → normal ekran/kullanım | İlgili test kanıtı |
|---:|---|---|
| 47 | `arc-compose/.../controls/ArcButtons.kt` → `ui/screens/ScoringWorkspaceScreen.kt` | Doğrudan UI testi yok; tablet testi bekliyor |
| 48 | `arc-compose/.../controls/ArcDropdowns.kt` → `ui/screens/ClassroomsScreen.kt` | Doğrudan UI testi yok; uzun basma/klavye testi bekliyor |
| 49 | `arc-compose/.../display/ArcDisplayBasics.kt` → `ui/screens/ReportsScreen.kt` | Doğrudan UI testi yok; panoya kopyalama cihaz testi bekliyor |
| 50 | `arc-compose/.../display/ArcDisplayBasics.kt` → `ui/components/AppTopBar.kt`, `ui/screens/SettingsScreen.kt` | Doğrudan UI/tercih yaşam döngüsü testi yok |
| 51 | `arc-compose/.../display/ArcDisplayBasics.kt` → `ui/screens/ClassroomsScreen.kt` | Derleme; erişilebilirlik cihaz testi bekliyor |
| 52 | `arc-compose/.../display/ArcDisplayBasics.kt` → `ui/screens/GroupWorkScreen.kt` | `GroupChecklistRulesTest` (grup iş kuralı; avatar UI testi değil) |
| 53 | `arc-compose/.../controls/ArcPickers.kt` → `ui/screens/ReportsScreen.kt` | `AssessmentDateFilterTest`; `ArcP1InteractionsTest.calendarSelectionReturnsTheSelectedIsoDate` (CI emülatöründe çalıştırılacak) |
| 54 | `arc-compose/.../controls/ArcPickers.kt` → `ui/screens/ReportsScreen.kt` | `AssessmentDateFilterTest`; Material tarih penceresi cihaz testi bekliyor |
| 55 | `arc-compose/.../display/ArcDisplayBasics.kt` → `ui/screens/ScoringWorkspaceScreen.kt` | Doğrudan aç/kapa UI testi yok |
| 56 | `arc-compose/.../navigation/ArcNavComponents.kt` → `ui/components/AppTopBar.kt` | Doğrudan seçim UI testi yok |
| 57 | `arc-compose/.../display/ArcFeedback.kt` → `PuanlaApp.kt` içindeki `ArcToastStack` | `UndoHistoryTest`; toast/geri alma cihaz testi bekliyor |
| 58 | `arc-compose/.../display/ArcDisplayBasics.kt` → `ui/screens/ScoringWorkspaceScreen.kt` | Doğrudan ağaç seçimi UI testi yok |
| 59 | `arc-compose/.../navigation/ArcNavComponents.kt` → `ui/screens/ClassroomsScreen.kt` tablosu | `ArcTablePagingTest`; ekran etkileşimi cihaz testi bekliyor |
| 60 | `arc-compose/.../datavis/ArcCharts.kt` → raporlardan çıkarıldı | Test yok; tarihsel seri modeli bulunmadığı için bilinçli dışarıda |
| 61 | `arc-compose/.../datavis/ArcCharts.kt` → `ui/screens/ReportsScreen.kt` | `ScoringRulesTest` iş durumlarını kapsar; gauge UI testi yok |
| 62 | `arc-compose/.../datavis/ArcDataDisplay.kt` → `ui/screens/ScoringWorkspaceScreen.kt` | `ScoringRulesTest`; animasyon/motion cihaz testi bekliyor |
| 63 | `arc-compose/.../controls/ArcTextInput.kt` → `ui/screens/ClassroomsScreen.kt` | Doğrudan arama UI testi yok |
| 64 | `arc-compose/.../datavis/ArcCharts.kt` → `ui/screens/ReportsScreen.kt` | Doğrudan gösterge UI testi yok |
| 65 | `arc-compose/.../display/ArcMedia.kt` → `ui/screens/OcrImportScreen.kt` | `OcrPositionedParserTest`, `ArcImageDropPolicyTest`; gerçek OCR cihaz testi bekliyor |
| 66 | `arc-compose/.../navigation/ArcNavComponents.kt` → `ui/screens/OcrImportScreen.kt` | `ArcP1InteractionsTest.carouselAdvancesOneRealItemAtATime` (CI emülatöründe çalıştırılacak); OCR kimlik onayı tablet kabul testi bekliyor |
| 67 | `arc-compose/.../datavis/ArcCharts.kt` → `ui/screens/ReportsScreen.kt` | `ScoringRulesTest`; grafik UI testi yok |
| 68 | `arc-compose/.../display/ArcFeedback.kt` → `ui/screens/SettingsScreen.kt` | `AuditTimelineRecordsTest` gerçek kayıt ve boş durum davranışını sınar |
| 69 | `arc-compose/.../display/ArcFeedback.kt` → `ui/components/AppTopBar.kt` | Doğrudan menü UI testi yok |
| 70 | `arc-compose/.../datavis/ArcCharts.kt` → `ui/screens/ReportsScreen.kt` | `ScoringRulesTest`; grafik UI testi yok |
| 71 | `arc-compose/.../controls/ArcConfirmation.kt` → OCR/restore ekranları | `BackupImportTest`, `MutationCoordinatorTest`; `ArcP1InteractionsTest.confirmationRequiresTheExplicitSecondAction` (CI emülatöründe çalıştırılacak) |
| 72 | `arc-compose/.../navigation/ArcNavComponents.kt` → `ui/screens/SettingsScreen.kt` | Doğrudan kaydırma UI testi yok |
| 73 | `arc-compose/.../navigation/ArcNavComponents.kt` → `ui/components/AppCommandPaletteModal.kt` | Doğrudan klavye/odak testi yok; fiziksel klavye bekliyor |
| 74 | `arc-compose/.../display/ArcFeedback.kt` → `PuanlaApp.kt` | Doğrudan aç/kapa UI testi yok |
| 75 | `arc-compose/.../display/ArcDisplayBasics.kt` → `ui/screens/ClassroomsScreen.kt` | Doğrudan empty-state callback testi yok |
| 76 | `arc-compose/.../datavis/ArcDataDisplay.kt` → `ui/screens/ReportsScreen.kt` | Doğrudan istatistik UI testi yok |
| 77 | `arc-compose/.../controls/ArcTextInput.kt` → `ui/screens/ScoringWorkspaceScreen.kt` | `BackupSnapshotTest` not alanının yedek round-trip'ini kapsar; IME/yeniden açma cihaz testi bekliyor |
| 78 | `arc-compose/.../controls/ArcTextInput.kt` → yalnız `ui/screens/CatalogGalleryScreen.kt` | Test yok; üretim bağlantısı yok |
| 79 | `arc-compose/.../controls/ArcPickers.kt` → yalnız katalog | Test yok; oturum saati alanı yok |
| 80 | `arc-compose/.../datavis/ArcDataDisplay.kt` → yalnız katalog | Test yok; bilinçli kullanılmıyor |
| 81 | `arc-compose/.../datavis/ArcDataDisplay.kt` → yalnız katalog | Test yok; bilinçli kullanılmıyor |
| 82 | `arc-compose/.../datavis/ArcDataDisplay.kt` → yalnız katalog | Test yok; bilinçli kullanılmıyor |
| 83 | `arc-compose/.../controls/ArcConfirmation.kt` → `ui/screens/ClassroomsScreen.kt` sınıf silme | Doğrudan hold gesture UI testi yok; cihaz testi bekliyor |
| 84 | `arc-compose/.../navigation/ArcNavComponents.kt` → yalnız katalog | Test yok; karuselle yineleniyor |
| 85 | `arc-compose/.../datavis/ArcCharts.kt` → `ui/screens/SettingsScreen.kt` | `AssessmentActivityHeatmapTest` gerçek tarihler, eylem türü ve boş günleri sınar; `ArcP1InteractionsTest.activityHeatmapExposesRealDayLabelsToAccessibility` (CI emülatöründe çalıştırılacak) |
| 86 | `arc-compose/.../controls/ArcHardware.kt` → yalnız katalog | Test yok; üretim ihtiyacı yok |
| 87 | `arc-compose/.../controls/ArcPickers.kt` → `ui/screens/ReportsScreen.kt` | `AssessmentDateFilterTest` sıra/sınır/boş uçları sınar; cihaz testi bekliyor |
| 88 | `arc-compose/.../controls/ArcPickers.kt` → yalnız katalog | Test yok; kalıcı renk tercihi ihtiyacı yok |
| 89 | `arc-compose/.../controls/ArcDropdowns.kt` → yalnız katalog | Test yok; mevcut aramalı seçim tercih edildi |
| 90 | `arc-compose/.../datavis/ArcCharts.kt` → yalnız katalog | Test yok; tarihli tekrar değerlendirme modeli yok |
| 91 | `arc-compose/.../datavis/ArcCharts.kt` → yalnız katalog | Test yok; donut ile aynı oranı tekrar eder |
| 92 | `arc-compose/.../datavis/ArcCharts.kt` → yalnız katalog | Test yok; iki dönemlik karşılaştırma modeli yok |
| 93 | `arc-compose/.../display/ArcDisplayBasics.kt` → yalnız katalog | Test yok; geçici işlem bildirimi toast ile karşılanıyor |
| 94 | `arc-compose/.../display/ArcFeedback.kt` → yalnız katalog | Test yok; kullanıcı JSON paneli istemiyor |
| 95 | `arc-compose/.../controls/ArcHardware.kt` → yalnız katalog | Test yok; çalışan özelleştirilebilir kısayol modeli yok |
| 96 | `arc-compose/.../datavis/ArcDataDisplay.kt` → yalnız katalog | Test yok; `ArcAnimatedCounter` yeterli |
| 97 | `arc-compose/.../display/ArcFeedback.kt` → yalnız katalog | Test yok; onaylı changelog kaynağı yok |
| 98 | `arc-compose/.../display/ArcFeedback.kt` → yalnız katalog | Test yok; SSS ihtiyacı/içeriği yok |
| 99 | `arc-compose/.../display/ArcFeedback.kt` → `ui/screens/ReportsScreen.kt` | `StudentComparisonTest` ölçüt sırasını ve null/0 ayrımını sınar; tablet diyaloğu cihaz testi bekliyor |

Yukarıdaki `arc-compose/...` yollarının kökü `arc-compose/src/main/java/com/knigdelioglu/arc/compose/` dizinidir. Compose etkileşim testleri `app/src/androidTest/` altında, Puanla veritabanından bağımsız `ArcTestHostActivity` ile kuruldu. Bu testler yerel derlemede başarıyla derlendi; CI'daki API 36 Android emülatörü çalıştırması henüz tamamlanmadığından UI testleri başarılı olarak raporlanmıyor. Gerçek Puanla akışlarının (özellikle OCR kimlik onayı, yedekten geri yükleme, tablet yatay/dikey düzeni ve TalkBack/fiziksel klavye) cihaz kabulü bekliyor. Room şeması değişmedi, migration ve yedek şeması etkisi yok.
