# Puanla

**Puanla**, öğretmenlerin öğrenci performanslarını rubriklerle hızlı, güvenilir ve çevrimdışı değerlendirmesine odaklanan **Android Native** uygulamasıdır. Yeni sürüm **Kotlin + Jetpack Compose** ile, öncelikle **Android tabletler** için sıfırdan geliştirilmektedir.

## Ürün hedefi

Öğretmen; sınıf listesini güvenilir biçimde oluşturabilmeli, sınıf düzeyine uygun rubriği seçebilmeli, öğrencileri tek tek hızla değerlendirebilmeli, yarım kaldığı yerden devam edebilmeli ve sonuçları kriter ayrıntılarıyla CSV olarak alabilmelidir. Uygulama, ders sırasında öğretmenin dikkatini ekrana değil öğrenciye vermesini hedefler.

## Temel kapsam

- Sınıf ve öğrenci kayıtları; gerektiğinde manuel düzeltme ve mükerrer kontrolü
- Fotoğraftan liste aktarımı (özellikle ad, soyad, numara ve komşu sütunların ayrılması; insan onaylı OCR)
- Rubrik başına azami puanları gözeten değerlendirme; **puanlanmadı** ile **0 puan** ayrımı
- Kısmi ve tamamlanmış değerlendirmelerin ayrı izlenmesi, geri alma ve düzeltme
- Grup hazırlığı ile bireysel puanlamanın ayrılması
- Yerel/çevrimdışı veri saklama, yedekleme/geri yükleme ve CSV dışa aktarma

## Teknoloji kararı

- **Kotlin, Jetpack Compose ve Android SDK** — React Native, Expo, Flutter veya PWA kullanılmaz.
- **Tablet öncelikli adaptif arayüz** — geniş ekranlar, yatay/dikey yön, kalem ve dokunma ergonomisi dikkate alınır.
- **Arc Library** (`kuratlielia/arc-library`) doğrudan Android bağımlılığı değildir; seçilmiş etkileşim ve görsel davranışlar Kotlin/Compose üzerinde **yerel olarak yeniden tasarlanacaktır**. Arc kaynaklı 132 kayıt tek tek incelendi: [tam envanter](docs/research/arc-puanla-full-catalog.md) ve [tablet öncelikli genişletilmiş uyarlama planı](docs/plans/arc-maximal-adoption.md). Geliştirme durumu için [kod denetimi matrisi](docs/verification/arc-implementation-matrix.md) esas alınmalıdır; 99/99 üretim entegrasyonu doğrulanmamıştır.
- Öğrenci verileri varsayılan olarak cihazdan çıkmaz; temel puanlama akışı internet olmadan çalışır.

## Depo kapsamı

`app/` Android uygulaması; `core/` paylaşılan Kotlin mantığı için ileride kullanılabilecek alan; `data/` onaylı rubrik ve şema verileri; `docs/` ürün, mimari ve uyarlama planları içindir. Ayrıntılar için [AGENTS.md](AGENTS.md) ve [ürün kapsamı](docs/product-scope.md) belgelerine bakın.

**Veri notu:** Sıfırdan açılan depoda önceki `data/rubrics.json` şu anda mevcut değildir. Önceki ürün raporu 11. sınıf Türk Dili ve Edebiyatı için 8 rubrik/48 ölçütten bahseder; ölçüt metinlerini ve ağırlıklarını içermez. **Onaylı kaynak aktarılmadan rubrik içeriği uydurulmaz.**

## Durum

Android Native `:app` ve ayrı `:arc-compose` modülleri geliştirme aşamasındadır. Room, hızlı puanlama, tablet ekranları ve yerel OCR kodları vardır. 2026-10-10 P0 güvenlik düzeltmeleriyle sahte rubrik/öğrenci tohumlama durdurulmuş, yıkıcı Room migration kaldırılmış, öğrencinin kimliğine bağlı puanlama ve işlem bazlı geri alma düzenlenmiş, tam JSON yedekleme/geri yükleme ve gerçek görsel seçimi eklenmiştir. P1 kapsamında CSV alan kaçışı/formül güvenliği, üretim formatlayıcısını çalıştıran birim testler, gerçek dosya kaydetme, gecikmeli gözlem notu kaydı, gerçek tarih aralığıyla son güncelleme filtresi, rubrik yönergesi akordeonu ve sınıf arama/boş durum bileşenleri eklenmiştir.

**Sınırlar:** 99 Arc kaydının tümü üretim ekranlarında doğrulanmamıştır. Onaylı rubrik verisi yokken gerçek notlandırma yapılmaz. Gerçek cihaz üzerinde uçtan uca OCR, geri yükleme ve puanlama testleri hala gereklidir. [Dürüst Arc doğrulama matrisi](docs/verification/arc-implementation-matrix.md).
