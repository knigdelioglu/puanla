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
- **Compose Design System** (ortak `compose-design-system` deposu) Kotlin + Jetpack Compose ile yazılmış yerel Android kütüphanesidir. Puanla `:design-system` modülünü doğrudan kullanır; uygulama iş kuralları ve Room modelleri kütüphaneye taşınmaz. Depolar varsayılan olarak aynı üst klasörde yan yana olmalıdır. Farklı bir konum için `-PcomposeDesignSystemDir=/mutlak/yol/compose-design-system/design-system` verin. Önceki Arc envanteri ve mevcut eşleştirme için [entegrasyon raporuna](docs/research/compose-design-system-puanla-integration.md) bakın.
- Öğrenci verileri varsayılan olarak cihazdan çıkmaz; temel puanlama akışı internet olmadan çalışır.

## Depo kapsamı

`app/` Android uygulaması; `core/` paylaşılan Kotlin mantığı için ileride kullanılabilecek alan; `data/` onaylı rubrik ve şema verileri; `docs/` ürün, mimari ve uyarlama planları içindir. Ayrıntılar için [AGENTS.md](AGENTS.md) ve [ürün kapsamı](docs/product-scope.md) belgelerine bakın.

**Veri notu:** Sıfırdan açılan depoda önceki `data/rubrics.json` şu anda mevcut değildir. Önceki ürün raporu 11. sınıf Türk Dili ve Edebiyatı için 8 rubrik/48 ölçütten bahseder; ölçüt metinlerini ve ağırlıklarını içermez. **Onaylı kaynak aktarılmadan rubrik içeriği uydurulmaz.**

## Durum

Android Native `:app` ve ortak `:design-system` modülü geliştirilmektedir. `:design-system` kaynakları varsayılan olarak kardeş `compose-design-system` deposundan okunur; üretilen build çıktıları Puanla deposunda tutulur. Room, hızlı puanlama, tablet ekranları, yerel OCR, CSV ve JSON yedekleme/geri yükleme kodları vardır. Ortak bileşen entegrasyonu ve doğrulama durumu [entegrasyon raporunda](docs/research/compose-design-system-puanla-integration.md) izlenir.

**Sınırlar:** Eski Arc kayıt matrisi, ortak kütüphaneye geçiş öncesi bir envanter anlık görüntüsüdür; güncel kaynak ve ekran eşleşmesi yeni entegrasyon raporundadır. Gerçek cihaz üzerinde uçtan uca OCR, geri yükleme, puanlama, TalkBack ve klavye testleri gereklidir.
