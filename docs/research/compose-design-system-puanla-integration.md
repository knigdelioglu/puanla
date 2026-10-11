# Compose Design System → Puanla Entegrasyon Raporu

**Tarih:** 11 Ekim 2026  
**İncelenen sürüm:** Compose Design System `main` / `4c6d96a`  
**Durum:** Gradle bağlantısı ve uygulama uyarlamaları yapıldı. Debug APK Samsung tablete kuruldu ve uygulama açıldı. Doğrulama ayrıntıları aşağıdaki tabloda.

## Karar ve puanlama

Puanla, Compose Design System'i kardeş depodaki `:design-system` Android modülü olarak tüketir. Eski `:arc-compose` modülü derleme grafiğinden çıkarılmıştır; iki kütüphaneyi beraber eklemek aynı Kotlin sınıflarını yineler. Kütüphane yalnızca Compose görünümü ve etkileşimini sağlar. Puanlama kuralları, Room verisi, OCR onayı, dışa aktarma ve yedekleme uygulamada kalır.

**Kütüphane uyumu: 9/10.** Compose + Android hedefi, tablet bileşenleri, erişilebilirlik davranışları ve Puanla'nın mevcut ekranlarıyla API uyumu güçlü. Kalan bir puan, kanıt notu biçimlerinin uygulama dışa aktarma ve raporlama akışında nasıl sunulacağı ve gerçek cihaz kabulünün tamamlanmamış olmasını yansıtır. Puan, ürün uygunluğunu değerlendirir; test sonucunun yerine geçmez.

**Puanla entegrasyon hazırlığı: 8.5/10.** Tek modül bağlantısı, derleme, birim testleri ve APK kurulumu tamamlandı. Kalan doğrulama yatay/dikey tablet, klavye, TalkBack ve hareket azaltma kabul kontrolleridir.

## Uygulanan entegrasyonlar

Bu turda yeni ürün akışlarına `ArcGlassTabBar`, `ArcRichTextEditor` ve `ArcShortcutRecorder` bağlandı; tema paleti ve sistem hareket tercihi Ayarlar'a eklendi. Puanla'nın daha önce kullandığı puanlama, OCR, rapor, sınıf ve grup bileşenleri artık aynı çağrılarla ortak depodaki güncel uygulamadan derleniyor. Yeni API imzalarına göre boş durum ve öğretmen menüsü çağrıları da uyarlandı.

| Akış | Uyum | Compose Design System kullanımı | Puanla davranışı |
|---|---:|---|---|
| Uygulama teması | 9.5/10 | `ArcTheme`, `ArcAccent` | Vurgu paleti tercihi kalıcıdır. Hareket ayarı `Sistem / Azalt / Açık` seçenekleriyle sistem animator ayarını izleyebilir. |
| Ana gezinme | 8.5/10 | `ArcAdvancedItem`, `ArcGlassTabBar` | Seçilen sekme mevcut `AppDestination` değerine eşlenir; iş akışı ve hedefler uygulamada tutulur. |
| Puanlama | 10/10 | `ArcResizablePanels`, `ArcElasticSlider`, `ArcSegmentedControl`, nullable `ArcNumberField`, `ArcAccordion`, `ArcPopover`, `ArcBadge`, `ArcAvatar`, `ArcTreeView`, `ArcFloatingButtonGroup` | Kaydırıcı puanı yalnız commit/release anında yazar; 0..ölçüt azamisi aralığı korunur. `null` puanlanmadı, 0 ise verilmiş sıfırdır; eksik ölçüt kesin toplam üretmez. |
| Kanıt notu | 8/10 | `ArcRichTextEditor` | Kalın/italik/liste/alıntı biçim işaretleri not alanındaki metin olarak saklanır. Room şeması değişmez; editör tam Markdown/HTML ayrıştırıcısı değildir. |
| Sınıf ve öğrenci yönetimi | 9/10 | `ArcSortableDataTable`, `ArcSearchField`, `ArcSelect`, `ArcRadioCards`, `ArcAvatar`, `ArcBadge`, boş durum ve onay bileşenleri | Öğrenci, sınıf ve rubrik kimlikleri uygulama katmanında birbirinden ayrı kalır. |
| OCR inceleme | 10/10 | `ArcFileDropzone`, `ArcFileUpload`, `ArcImageCompare`, `ArcCarousel`, `ArcInlineEdit`, `ArcConfirmMorph`, `ArcCheckbox`, `ArcSlider`, `ArcAlert` | OCR adayları taslak ve öğretmen onaylı durumlarını korur; bileşenler adı, soyadı veya numarayı kendiliğinden doğrulamaz. |
| Raporlar | 9/10 | `ArcStatsBand`, `ArcMetricCard`, `ArcBarChart`, `ArcDonutChart`, `ArcGauge`, `ArcUsageMeter`, `ArcComparisonTable`, `ArcDateRangePicker`, `ArcCalendar` | Grafikler mevcut sınıf/değerlendirme verisini sunar. Ortalama ve tamamlanma kuralları domain katmanında kalır. |
| Grup çalışmaları | 9/10 | `ArcCheckbox`, `ArcMultiSelect`, `ArcProgress` ve kart/badge bileşenleri | Grup hazırlığı bireysel 100 puanlık toplama eklenmez. |
| Komut ve bildirimler | 9/10 | `ArcCommandPalette`, `ArcShortcutRecorder`, `ArcToastStack`, `ArcNotificationCenter`, `ArcTooltip`, `ArcBottomSheet` | Komut paleti kısayolu kaydedilebilir. Kullanıcı özel kısayol belirlemezse Ctrl+K ve Meta+K çalışır; temizlenmiş özel kısayol devre dışı kalır, üst düğme kullanılabilir. |
| Görünüm ayarları | 9.5/10 | `ArcSelect`, `ArcSegmentedControl`, `ArcThemeSwitch` | Palet, hareket ve karanlık tema tercihi `arc_ui` içinde yerelde saklanır. Önceki `reduce_motion` Boolean tercihi okunarak yeni üç seçenekli ayara geçirilir. |

### Kütüphaneden dışarıda bırakılanlar

Puanla'nın ürün akışında karşılığı bulunmadığı için `ArcWalletStack`, `ArcSignaturePad`, `ArcCountdown`, OTP/telefon/parola/ödeme kontrolleri, mention editörü ve takvim planlama bileşenleri uygulama ekranlarına eklenmedi. Bunları yalnız bileşen sayısını artırmak için eklemek yeni veri ve kullanıcı beklentisi oluşturur. Kullanılabilir her bileşeni gerçek bir sınıf, puanlama, OCR, rapor veya ayar işine bağlama hedefi sürdürülecek.

## Yapılandırma

- `settings.gradle.kts`, varsayılan olarak `../compose-design-system/design-system` yolunu `:design-system` projesine bağlar.
- Depolar başka konumdaysa Gradle'a `-PcomposeDesignSystemDir=/mutlak/yol/compose-design-system/design-system` verilebilir.
- Tasarım sistemi derleme çıktıları Puanla deposundaki `build/design-system` dizinine yönlendirilir; kardeş kaynak deposu değiştirilmiyor.
- `app` yalnızca `implementation(project(":design-system"))` bağımlılığı kullanır.
- Doğrulanan hedef sürüm değerleri: AGP `8.13.2`, Kotlin `2.2.21`, Compose BOM `2025.10.01`, `compileSdk 36`, `minSdk 26`.
- Kullanıcının belirttiği üzere iki depo da aynı sahibin; lisans/attribution konusu bu entegrasyon için engel değildir. Kaynak kütüphane deposundan doğrudan derlenir, Puanla'ya kaynak kopyalanmaz.

## Veri ve erişilebilirlik korumaları

- `null = puanlanmadı`, `0 = açıkça verilmiş sıfır puan`; bu ayrım slider başlığında, sayı alanında ve ölçüt badge'inde görünür. Slider boş değeri görsel olarak sıfır konumunda gösterebilir ama `onValueCommit` olmadan kayıt yazmaz.
- Her puan alanı `0..criterion.maxPoints` aralığını kullanır. Tamsayı hassasiyeti korunur.
- Hareket tercihi `system` olduğunda `ArcTheme` Android animasyon ölçeği tercihine uyar. Eski kaydedilmiş Boolean değerler davranışı koruyacak şekilde `Azalt` veya `Açık` olur; ayar yoksa `Sistem` kullanılır.
- OCR görüntüsü ve verileri cihazda işlenir. OCR önerileri öğretmen onayı olmadan kesin öğrenci kaydına yazılmaz.
- Öğrenci ve rubrik kaynak JSON'ları, Room şeması, yedekleme biçimi ve iş kuralları bu UI geçişinde değiştirilmedi.

## Doğrulama durumu

| Kontrol | Durum |
|---|---|
| `git diff --check` | Geçti |
| `:design-system` ve `:app` Kotlin derlemesi | Geçti |
| Puanla ve tasarım sistemi birim testleri | Geçti |
| Puanla debug APK paketleme | Geçti |
| `:app:compileDebugAndroidTestKotlin` | Geçti |
| Yeni ana gezinme ve zengin not editörü UI testleri | API 36 emülatöründe 2/2 geçti; kullanıcı isteği üzerine emülatör daha sonra kapatıldı |
| Gradle bağlı cihaz UI test görevi | Durduruldu. Gradle, cihaz seri numarası parametresini uygulamayarak hem emülatörü hem SM-X230 tableti hedefledi; tablette Compose test Activity hiyerarşisi bulunamadı. Bu sonuç cihaz testi başarısı sayılmaz. |
| SM-X230 gerçek cihaz kurulum kontrolü | `adb install -r` başarılı; uygulama açıldı ve süreç çalışır durumda doğrulandı |
| Gerçek cihaz kabulü: tablet yatay/dikey, büyük yazı, TalkBack, fiziksel klavye, animator ölçeği, OCR onayı ve yedekten geri yükleme | Bekliyor |

Compose UI bağlantı testleri `app/src/androidTest/java/com/knigdelioglu/puanla/ArcDesignSystemIntegrationTest.kt` içindedir. Birim testler yeni görünüm tercihlerini eski ayardan okuma davranışını ve komut paleti kısayol eşleşmesini kapsar. Compose Design System'in kendi testleri, Puanla'nın bu iş kurallarına bağlandığını tek başına kanıtlamaz. Kullanıcı emülatör kullanımını durdurup uygulamanın tablete kurulmasını istedi; mevcut tablet kurulumu ve açılış kontrolü tamamlandı, daha ileri cihaz kabulü yapılmadı.

## İlgili kaynaklar

- Modül bağlantısı: `settings.gradle.kts`, `app/build.gradle.kts`, `build.gradle.kts`.
- Puanlama ve not editörü: `app/src/main/java/com/knigdelioglu/puanla/ui/screens/ScoringWorkspaceScreen.kt`.
- Tema/kısayol tercihleri: `app/src/main/java/com/knigdelioglu/puanla/ui/screens/SettingsScreen.kt`, `app/src/main/java/com/knigdelioglu/puanla/ui/preferences/AppearancePreferences.kt`, `app/src/main/java/com/knigdelioglu/puanla/ui/components/KeyboardShortcutMatcher.kt`.
- OCR ve rapor bileşenleri: `OcrImportScreen.kt`, `ReportsScreen.kt`.
- İş kuralları: `app/src/main/java/com/knigdelioglu/puanla/domain/ScoringRules.kt`.
- API geçişi ve davranış: kardeş depodaki `docs/api-migration.md` ve `docs/behavior-equivalence.md`.
