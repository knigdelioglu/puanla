# Puanla — Birleşik Puanlama Çalışma Alanına Geçiş Planı

> **Durum:** Uygulanmayı bekleyen tasarım ve mühendislik planı · **Tarih:** 16 Ekim 2026  
> **İncelenen başlangıç noktası:** `main` — `5836ace59fca5a0b80966f17a95a02200998e223`  
> **Kapsam:** Android Native (Kotlin + Jetpack Compose), tablet öncelikli. Bu belge kod değişikliği veya tamamlanmış özellik beyanı değildir.

## 1. Amaç ve başarı ölçütü

Bireysel ve grup değerlendirmesi, öğretmenin **aynı sırayı ve aynı puanlama kontrollerini** kullandığı tek çalışma alanından yürütülecek. Fark yalnızca değerlendirme hedefi (öğrenci/grup), o moda ait oturum, puanların kayıt/dağıtım davranışı ve gerekli hazırlık işlemidir.

Başarı tanımı:
- Ortak yol: **Sınıf → Rubrik → Bireysel/Grup → Değerlendirmeler → Öğrenci/Grup → Ölçüt → Puan → Sonraki → Raporlar**.
- Var olan bir oturumda her ölçütü puanlamak varsayılan hızlı akışta **bir dokunuş**, sıradaki öğrenci/gruba geçiş **bir dokunuş** gerektirir; ayrıca Kaydet, Tamamla veya Üyelere Dağıt eylemi istenmez.
- Mod değişiminde sınıf ve rubrik korunur; her modun kendi son oturumu, seçili hedefi ve kaldığı konum geri yüklenir. Başka modun puanı veya geri alma geçmişi etkilenmez.
- `puanlanmadı (null)` ile `0` farklıdır; zorunlu tüm ölçütler tamamlanmadıkça kesin not gösterilmez.
- Grup puanları oturumun üyelerine tek atomik işlemde yansır, **bireysel puan kayıtlarını değiştirmez**.
- Var olan Room, yedek/geri yükleme ve PDF/CSV verileri geriye dönük olarak korunur; geçiş veri kaybına yol açmaz.

## 2. Bugünkü durum: koddan doğrulanan başlangıç

- `ui/screens/ScoringWorkspaceScreen.kt`: bireysel değerlendirme, üç panel, hızlı puan, hassas giriş, gözlem notu, önceki/sonraki öğrenci.
- `ui/screens/GroupWorkScreen.kt`: ayrı grup ekranı; aynı `QuickScoreControls` bileşenini kullanıyor, fakat grup hazırlığı, oturum seçimi ve grup puanlama bu ekrana gömülü.
- `ui/components/AppTopBar.kt`: ortak sınıf/rubrik ve Bireysel/Grup seçicisi var; **Değerlendirmeler** yalnız bireysel mod ve Raporlar ekranında görünüyor.
- `ui/components/EvaluationRunSheet.kt`: bireysel `AssessmentRunEntity` oturumlarının yönetimi.
- `ui/viewmodel/PuanlaViewModel.kt`: bireysel ve grup için farklı seçim/puan/geri alma durumları; ortaklaştırma bunları tek veri havuzuna dönüştürmemeli.
- `data/repository/PuanlaRepository.kt`: grup puanı ve üye sonuçları Room transaction sınırında güncelleniyor; kesin üye notu ancak tüm ölçütler tamamlanınca oluşuyor. Grup üyeliği değerlendirme anında oturum görüntüsü olarak saklanıyor.
- `ui/screens/ReportsScreen.kt`: bireysel ve grup raporları aynı ana bölümde, ancak farklı oturum/çıktı seçicileriyle sunuluyor.
- `ui/navigation/NavigationDestinations.kt`: Grup zaten ana navigasyonda ayrı sekme değil; Puanlama altında alt mod.

**Sonuç:** Veri modellerini birleştirmek değil, ortak kullanıcı akışını ve sunum sözleşmesini kurmak gerekiyor. `QuickScoreControls` ve mevcut üç panel iyi bir başlangıç; yeniden yazmak yerine kullanılmalı.

## 3. Nihai etkileşim sözleşmesi

### 3.1 Ortak akış

1. Öğretmen mevcut sınıfı ve rubriği seçer; uygulama uygun son seçimleri mümkün olduğunca geri getirir.
2. Üst alandaki `Bireysel | Grup` seçicisiyle modu seçer.
3. İki modda da `Değerlendirmeler` üzerinden mevcut oturumu açar veya `+ Yeni değerlendirme` oluşturur. **Görsel akış aynı; oturum türleri ayrı.** Bireysel modun birden fazla sınıfa atanabilen değerlendirmeleri korunur; grup oturumu mevcut sınıf/rubriğe bağlıdır.
4. Sol panelden **öğrenci veya grup**, orta panelden **ölçüt** seçer. Sağ panelde aynı hızlı/hassas puanlama işlemlerini yapar.
5. Puan otomatik kaydedilir. Varsayılan olarak yalnız **sonraki ölçüte** geçilir; başka öğrenci/gruba kendiliğinden geçilmez. Son ölçütte hedefin `Tamamlandı` veya `Kısmi` durumu açıkça görünür.
6. `Sonraki` düğmesinin anlamı modla değişir: **Sonraki Öğrenci** / **Sonraki Grup**. Önceki, Geri Al, Puanı kaldır, kayıt durumu ve rapora gitme tutarlı yerde kalır.
7. `Raporlar`, seçili mod/oturum bağlamıyla açılır; **PDF/CSV** aynı giriş noktasından, mevcut bireysel/grup formatları bozulmadan üretilir.

### 3.2 Grup için tek ek hazırlık

- Grup yoksa anlamlı boş durum: `Grupları Yönet` → `Yeni Grup` veya `Rastgele Grup Oluştur`.
- Grup varsa grup oluşturma formu puanlamayı işgal etmez. Üyelik düzenleme, silme, kontrol listesi ve gerektiğinde `Üyelikleri Eşitle` **Grupları Yönet** yüzeyinde bulunur.
- Daha önce puanlanmış bir oturumun üye görüntüsü sessizce güncellenmez. Eşitleme etkileri açıkça açıklanır ve onay gerektirir.
- Üye notları grup panelinde `Eksik` / `N öğrenciye aktarıldı` gibi açık durumla gösterilir; her üyeye ayrı onay düğmesi yoktur.

### 3.3 Durumlar ve kenar koşulları

- **Başlanmadı / Kısmi / Tamamlandı** hem öğrenci/grup listesinde hem ölçütlerde aynı anlamı taşır. Ölçüt bazında geçerli `0` başarılı puanlama sayılır.
- Hızlı seçim otomatik sonraki ölçüte geçebilir; **not editörü veya hassas puan etkileşimi açıkken istemsiz geçiş yapılmaz**. Düzenleme ve manuel `Sonraki ölçüt` yolu ulaşılabilir kalır.
- `Geri Al` son uygun puan mutasyonunu geçmiş koşuluna göre geri çevirir; `Puanı kaldır` ise seçili ölçütü boşaltır. İkisi eş anlamlı değildir. Yanlış hedef/oturum üzerinde geri alma yasak.
- Kayıt henüz onaylanmadıysa `Kaydediliyor` gösterilir; başarı yalnız kalıcı sonuçtan sonra, hata ise yeniden deneme/düzeltme olanağıyla gösterilir. Hızlı mod/ölçüt/hedef değişiminde kimlikler önceki eyleme doğru bağlanır.
- Önceden arşivlenmiş bireysel oturumlarda mevcut salt-okunur kural korunur. Grup oturumu için arşivleme ayrıca tanımlanmadıkça yeni bir kilit mekanizması eklenmez.
- Yedek geri yüklendikten sonra geçici gezinme/geri alma durumları yeni kayda göre temizlenir; eski UI bağlamı geri yüklenen veriye yanlış yazamaz.
- Rapor almak ve ders sonunda ayrılmak, değerlendirmeyi arşivlemeyi gerektirmez. Tamamlanmayan hedefler raporda `Eksik` kalır.

### 3.4 Adaptif yerleşim

| Genişlik | Beklenen deneyim |
| --- | --- |
| Geniş tablet (başlangıçta ≥900 dp) | Sabit bağlam üstte; üç yeniden boyutlandırılabilir panel: hedef listesi, ölçütler, puanlama. Yaklaşık **25/25/50**, fakat okunabilir minimum genişlik daha önceliklidir. |
| Orta/dar tablet | Aynı bağlam/oturum; `Öğrenciler veya Gruplar / Ölçütler / Puanlama` odaklı görünüm. Hedef veya ölçüt seçimi puanlama alanına götürür. |
| Her ekran | Sistem gezinme çubuğu, IME, yazı büyütme ve TalkBack altında düğme gizlenmez; bir elle erişilebilir hedefler, görünür hata/kayıt durumu. |

Oranlar ve eşikler cihaz testleriyle ayarlanacak, katı piksel sözleşmesi olmayacak. Sağ panel, özellikle grup üyeleri veya gözlem içeriği nedeniyle gereksiz kaydırmaya zorlanmamalı.

## 4. Mühendislik yaklaşımı ve sınırlar

**Önerilen sınır:** UI seviyesinde ortak bir çalışma alanı, modun veri ve eylemlerine ulaşan iki ayrı bağlayıcı. Örnek sözleşme: `mode`, `selectedSession`, `targets`, `selectedTarget`, `criteria`, `scores`, `progress`, `saveState`, `score/clear/undo/next`. Bu bir yönelimdir; mevcut ViewModel'in tamamını tek dev sınıfa taşımayı zorunlu kılmaz.

- Ortaklaştırılacak: bağlam çubuğu, mod seçimi, değerlendirme seçici kabuğu, hedef/ölçüt panelleri, hızlı/hassas puan kontrolü, tamamlama göstergesi ve gezinme.
- Ayrı kalacak: bireysel `AssessmentRunEntity` ile grup `GroupAssessmentSessionEntity`; üye görüntüsü ve atomik dağıtım; bireysel/grup undo yığınları; ilgili veri erişimi; rapor üretim kuralları.
- Seçim anahtarı en az `(mod, sınıf, rubrik, oturum, hedef)` ayrımını gözetmeli. Son ölçüt konumu aynı oturuma geri dönüşte korunabilir; farklı hedefe **ilk ölçütten** başlanır. Kullanıcının manuel ölçüt seçimi asla başka hedefe sızmaz.
- İlk tercih **şema değişikliği olmadan** UI/akış refaktörü. Şema gerekiyorsa sebep, geriye dönük Room migration, eski APK verisi ve yedek uyumluluğu bağımsız PR ve testlerle kanıtlanmadan uygulanmaz. Destructive migration yasak.
- `data/` içindeki onaylı rubrik JSON'larına, OCR kararlarına ve kaynak metinlere bu iş kapsamında dokunulmaz. Öğrenci verileri örnek/test deposuna yüklenmez.
- Ortak Compose bileşenleri Puanla'da tutulur; genel amaçlı ve projeler arası gerçekten yeniden kullanılabilir olduğu kanıtlanmadıkça `compose-design-system` deposuna özellik mantığı taşınmaz.

## 5. Aşamalı geçiş ve teslim kapıları

Aşamalar küçük, birbirinden geri alınabilir PR'lara bölünmeli. Her aşama önceki çalışan puanlama yolunu bozmayacak şekilde tamamlanır.

| Aşama | Odak / başlıca iş | Bitirme koşulu |
| --- | --- | --- |
| **P0 — Referans ve güvence** | Mevcut ekran akışını, her iki moddaki dokunuş sayısını, durum/oturum seçimlerini ve bilinen hataları kaydet; temsilî anonim veri ve regresyon testleri kur. | Bireysel + grup örnek oturumları; null/0, puan düzeltme, dağıtım, undo, backup/restore referans senaryoları tekrarlanabilir. |
| **P1 — Ortak bağlam ve oturum seçimi** | `AppTopBar` ve `EvaluationRunSheet` deneyimini moda göre ortak adım sırasına getir; grup `Yeni Oturum` mantığını `Değerlendirmeler` kabuğuna bağla. | Her modda aynı ekran konumundan oturum açma/oluşturma; ayrı türler korunur; mod değişiminde doğru oturum geri gelir. |
| **P2 — Ortak çalışma alanı** | Ayrık `ScoringWorkspaceScreen` / `GroupWorkScreen` puanlama düzenini tek adaptif kabukta birleştir; hedef tipi ve seçimi bağlayıcıyla ayrıştır. | Geniş/dar düzenlerde aynı sol/orta/sağ görevleri; doğru hedef ve ölçüt; eski grup hazırlığı işlevleri kaybolmaz. |
| **P3 — Puanlama etkileşimlerinin birleştirilmesi** | Ortak hızlı/hassas giriş, otomatik ölçüt geçişi, not düzenleme uygunluğu, önceki/sonraki hedef, geri alma ve kalıcı kayıt durumu. | Hızlı ardışık dokunma ve seçim değişimlerinde kayıt başka kimliğe gitmez; modlar aynı davranır; grup dağıtımı bireysel notları değiştirmez. |
| **P4 — Grup hazırlığı ve rapor akışı** | `Grupları Yönet` yardımcı yüzeyi; üyelik/eşitleme uyarıları; moda duyarlı tek `Raporlar` girişi ve PDF/CSV erişimi. | Hiç grup yok / hazır grup / değiştirilmiş üyelik / iki farklı oturum senaryoları kayıpsız; raporlar doğru oturum ve formatta. |
| **P5 — Sertleştirme ve yayın kapısı** | Birim, Room, Compose UI ve emülatör testleri; gerçek tablet kontrolü; erişilebilirlik; performans; geri alma/geri yükleme; eski veriye karşı karşılaştırma. | Tüm aşağıdaki kabul matrisi karşılandı; CI yeşil; cihaz testleri kayda geçti; sürüm yayımlama kararı kanıta bağlı. |
| **P6 — Temizlik** | Artık kullanılmayan ekran dalları ve yinelenen UI durumunu kaldır; eski rotaları güvenle yönlendir; mimari, test ve kullanım belgelerini güncelle. | Tek puanlama girişi; işlev kaybı/ölü kod yok; önceki sürüm verileri okunur; belgeler güncel. |

**Önerilen PR sınırları:** (1) durum/oturum seçimi ve testleri; (2) ortak ekran/panel; (3) ortak eylemler ve kayıt/undo; (4) grup hazırlığı ve raporlar; (5) cihaz kabulü + temizlik. P5/P6 kapsamı gerekirse ayrı PR'lara bölünebilir; her PR küçük ve incelemeye elverişli olmalı.

## 6. Kabul matrisi — testlerle ispatlanacak sözleşme

| Kimlik | Senaryo | Beklenen sonuç / kanıt |
| --- | --- | --- |
| A1 | İlk kurulum sonrası kayıtlı sınıf + rubrik, var olan bireysel oturum | Değerlendirmeler → öğrenci → ölçüt → tek dokunuşla puan; ayrı Kaydet yok. |
| A2 | Aynı bağlamda grup modu, hazır gruplar | Aynı oturum seçme ve puanlama sırası; grup hedefi ve üyeler görünür. |
| A3 | Grup yok / rastgele grup / manuel grup | Hazırlığa açık yönlendirme; mükerrer öğrenci üyeliği engeli ve mevcut grup düzenleme davranışı korunur. |
| A4 | Null, 0, kısmi ve tüm ölçütler | 0 puanlandı sayılır; eksik kesin not değildir; sadece tam değerlendirme sonuçlanır. |
| A5 | Tek ölçüt, hızlı ardışık puan ve otomatik geçiş | En fazla bir hedefe ait doğru ölçüt güncellenir; farklı hedefe otomatik geçilmez; not editörü açıkken kontrol korunur. |
| A6 | Puan değiştir / Puanı kaldır / Geri Al | Eski duruma doğru dönüş; sonraki yazıma veya başka mod/oturum/öğrenciye müdahale yok. |
| A7 | Grup ölçütü tamamla, sonra düzelt, sonra undo | Üyelerde atomik ve tutarlı sonuç; bireysel puanlar değişmez; iki grup oturumu birbirini etkilemez. |
| A8 | Puanlanmış grupta üyelik düzenle ve eşitleme | Önceki oturum görüntüsü kendiliğinden değişmez; açık onaydan sonra yalnız hedef oturumu etkiler. |
| A9 | Mod, sınıf, rubrik, oturum, hedef değiştir; uygulamayı kapat/aç | Kaldığı yere doğru dönüş; geçici durumların karışmaması; eski oturum puanlarının korunması. |
| A10 | Aktif işlem sırasında yedek al/geri yükle, hızlı hedef değiştirme | Bekleyen mutasyonlar güvenle sonlandırılır; sahte kaydedildi bildirimi ve yanlış hedefe yazım yok. |
| A11 | Bireysel çok-sınıflı oturum, arşiv ve eksik kayıt | Eski kapsam/okuma-kilitleme korunur; rapora gitmek arşivlemez. |
| A12 | Bireysel PDF/CSV ve grup PDF/CSV | Oturum, sınıf, ölçüt ve öğrenci/grup bağlamı doğru; eski dışa aktarım biçimleri korunur. |
| A13 | 11 inç yatay/dikey, geniş yazı, TalkBack, klavye/kalem | Sistem çubuğu altında kayıp düğme yok; anlaşılır etiketler; tek elle kullanılabilir; panel kaydırma odak kaybı yaratmaz. |
| A14 | Gerçekçi bir sınıfı iki modda art arda puanlama | Rutin ölçüt puanında 1 dokunuş ve hedef geçişinde 1 dokunuş hedefi; mevcut sürüme göre gereksiz adım artışı yok. |

**Otomasyon ve cihaz doğrulaması:** Mevcut CI `./gradlew test :app:assembleDebug :app:assembleRelease` ile birim/derlemeyi ve emülatörde `:app:connectedDebugAndroidTest` ile Android testlerini çalıştırıyor. Mevcut `GroupModeScoringTest`, `EvaluationRunRepositoryTest`, `ScoringStudentNavigationTest` ve `CriterionSelectionPaneTest` üzerine yeni ortak akış/oturum bağlamı testleri eklenmeli. CI başarılı olsa bile fiziksel tablet incelemesi ayrıca kaydedilmeli.

## 7. Riskler ve geri dönüş

| Risk | Kontrol / geri dönüş |
| --- | --- |
| Mod değişiminde yanlış oturum/öğrenciye yazma | Bağlam kimliğini olay oluşturulurken sabitle; anahtarlı UI state ve yarış testleri; sorunlu yeni kabuğu geri alırken eski veri katmanına dokunma. |
| Geç kaydedilen puanın kaybolması veya sahte başarı | Tek mutasyon koordinasyonu + veri tabanından onaylanan kayıt durumu; yaşam döngüsü ve yedeklemeyle regresyon. |
| Grup üye sonuçlarının bireysel değerlendirmeyle karışması | Ayrı oturum ve sonuç depoları; eski üyelik görüntüsü ve transaction invariant'ları; grup testleri. |
| Refaktörde eski değerlendirmelerin açılamaması | Eski veriyle açılış, iki oturum/çoklu sınıf, CSV/PDF ve JSON round-trip testleri; şema değişimini varsayılan olarak reddet. |
| 3 panelli görünümün küçük tablette sıkışması | Adaptif tasarım; gerçek cihaz, IME ve erişilebilirlik doğrulaması; sabit oran zorlamama. |
| Büyük refaktörde geri dönüşün zorlaşması | Artımlı PR, eski yolu geçici referans olarak tutma, her etapta çalışan APK ve doğrulama; P6'ya kadar kalıcı işlev silmeme. |

**Yayımlama kuralı:** Birleşik akışın testleri ve cihaz kabulü tamamlanmadan eski puanlama yollarını kaldırma. Mevcut `.github/workflows/android.yml`, `main` dalına yapılan her push sonrasında doğrulama başarılı olursa debug APK ön sürümü yayımlar; dokümantasyon PR'ını doğrulanmadan sırf yayın almak amacıyla birleştirme. APK'nın farklı debug imzası mevcut kurulum üstüne sorunsuz güncelleme garantisi vermez; veri yedeğini ve imzalama politikasını kontrol et.

## 8. İşin kapsamı dışı

Bu geçişte OCR akışının yeniden yazılması, rubrik içeriklerinin değiştirilmesi, puanların yeni not hesaplama formülüne dönüştürülmesi, grup üyelerine bireysel nottan farklı özel dağıtım algoritması, yeni bir bulut servisi veya tasarım sistemi deposunun geniş refaktörü **yapılmayacak**. Yalnızca ortak kullanıcı akışının çalışması için kanıtla gerekli görülen değişiklikler kapsama alınabilir.

## 9. Başlangıç sırası ve karar

**İlk uygulama PR'ı P0 + P1'in sınırlı kapsamı olmalı:** oturum seçimini iki modda eşleştirip modlar arası bağlam izolasyonunu testle kanıtlamak. Sonraki PR'lar ortak ekran ve puanlama etkileşimlerine geçer.

**Tek cümlelik hedef:** Öğretmen bireysel puanlamayı öğrendiğinde, grup puanlamasını da yeniden öğrenmeden yapabilmeli; bu sırada hiçbir eski puan, oturum veya grup üyesi yanlışlıkla değişmemeli.

### Kaynak belgeler

- [Ürün kapsamı](../product-scope.md) · [Mimari çerçeve](../architecture.md) · [Tablet etkileşimi](../design/tablet-interaction.md)
- [Mevcut yol haritası](implementation-roadmap.md) · [Test stratejisi](../testing.md) · [Depo kuralları](../../AGENTS.md)
