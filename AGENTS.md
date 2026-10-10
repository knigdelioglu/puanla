# Puanla — Çalışma kapsamı ve değişmez kurallar

## Platform
Bu depo **yalnızca Kotlin + Jetpack Compose ile Android Native** geliştirme içindir. Tablet önceliklidir. React Native, Expo, Flutter, PWA ve web uygulaması eklenmez.

## Veri sınırı
- `data/` içinde bulunan onaylı JSON rubriklerini, şemaları ve kaynak verileri **izinsiz silme, yeniden adlandırma, yeniden biçimlendirme veya otomatik dönüştürme**.
- Uygulama kodu/veri katmanı üzerinde çalışırken kaynak JSON içeriğine dokunma; veri değişikliği gerekiyorsa ayrı, açık kapsamlı iş olarak ele al.
- Geçmiş 11. sınıf rubriklerinin 48 ölçütü ve puanları bu yeni depoda bulunmuyorsa **tahmin ederek oluşturma**. Kaynak dosya gelene kadar veri alanı sadece belgelenir.
- Öğrenci kişisel verilerini örnek veri, günlük, ekran görüntüsü veya uzak servislere aktarım yoluyla ifşa etme. Sentetik/anonim test verisi kullan.

## İş kuralları
- **Puanlanmadı != 0**. Bir rubrik tüm zorunlu ölçütleri puanlanmadan tamamlanmış not sayılamaz.
- Ölçüt puanı 0 ile izin verilen üst sınır arasında tam sayıdır; öğrenciler, sınıflar, rubrikler birbirine karışmaz.
- OCR çıktıları kesin bilgi değildir: adı/soyadı/numarayı tahmin etme, sütunları birleştirme, öğretmen onayı olmadan kaydetme.
- Grup hazırlık işaretleri bireysel 100 puanlık toplama kendiliğinden eklenmez.
- Veri kaybı olmadan çevrimdışı kullanım, yedekleme ve dışa aktarım ana gereksinimlerdir.

## UI ve Arc uyarlaması
- Arc Library'den kodu doğrudan JS/TS olarak taşıma; Compose üzerinde native etkileşimler ve erişilebilir durum yönetimi kur.
- Arc görünümü iş mantığından ve veri katmanından ayrılır. Görsel denemeler veri şemasını değiştiremez.
- Tablet ergonomisi, yatay/dikey ekran, büyük dokunma hedefleri, erişilebilirlik, `reduce motion`, performans ve tutarlı animasyon esas alınır.
- Uyarlanan kaynak kod/parçalar için upstream lisans ve attribution koşullarını kontrol et; plan/karar dokümantasyonu güncel kalsın.

## Teslim disiplini
- Çalışmayan veya test edilmeyen bir ekranı tamamlanmış özellik olarak sunma.
- Davranış değişikliklerini birim testleriyle; geniş ekran işlerini Compose UI testleriyle koru.
- Ürün amaçları: `docs/product-scope.md`, mimari sınırlar: `docs/architecture.md`.
