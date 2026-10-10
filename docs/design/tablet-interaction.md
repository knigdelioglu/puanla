# Puanla tablet deneyimi — tasarım ilkeleri

## Amaç
Sınıftaki öğretmenin bir eli tablet üzerinde, dikkati öğrencideyken hızlı ve kontrollü değerlendirme yapabilmesi. Önceki uygulamanın ekranları/UX'i kopyalanmaz; yeni tasarım ürün kurallarından başlar.

## Etkileşim sözleşmesi
- **Bir bakışta bağlam:** hangi sınıf, öğrenci, rubrik ve ölçüt; durum eksik/kısmi/tamam. Değişken sayısal değerler okunur.
- **Tek eylem/tek anlam:** puan değiştirme anında görünür; 0 seçimi ile henüz puan yok açık biçimde ayrıdır.
- **Parmağın örttüğü sayı okunur:** slider balonu başparmak üzerinde veya erişilebilir alanda görünür; kenarlarda ekrandan taşmaz.
- **İşlem hızlı ama güvenli:** sürükleme sırasında yalnız geçici değer, bırakmada kalıcı işlem; iptal/geri alma yolu net. Otomatik sonraki öğrenciye geçiş kullanıcı tercihine bağlı olmalı.
- **Erişilebilir alternatif:** dereceler tek dokunuşla, tam sayı klavye/stepper ile girilebilir; swipe veya uzun basma tek yöntem olmaz.
- **Tablet düzeni:** 840dp ve üstü büyük çalışma alanında öğrenci seçimi ve rubriği olabildiğince birlikte göster; 600–839dp geçiş çözümü, daha dar alanda odaklı tek görev akışı. Bunlar ilk tasarım eşikleridir, gerçek cihazlarla kalibre edilecek.
- **Görsel dil:** Arc'tan esinlenen sakin, tutarlı ve fiziksel hareket; aşırı parıltı, gereksiz kart katmanları ve sürekli animasyon yok.
- **Boş/eksik durum:** ilk sınıf yoksa gerçek yönlendirme; rubrik JSON yüklenmemişse başka sınıfın rubriğiyle doldurma yok.
- **Offline:** sürekli kayıt ve güvenilir kaldığı yerden devam; ağ yok diye ana akış durmaz.

## Ölçme ve test
- Basit hedef: öğretmen ekran aramadan peş peşe ölçüt puanlayabilsin, yanlış dokunmayı düzeltebilsin.
- Hız yanında doğruluk ve veri koruma zorunlu kabul ölçütleridir.
- Gerçek öğretmen tablet kullanımı sırasında belirlenen yanlış dokunma/sıkıntı noktaları sonraki iterasyonu belirler.

Bu belge görsel mockup veya bitmiş ekran sözleşmesi değil; uyarlamanın ürün odaklı UX ilkeleridir.
