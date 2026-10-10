# Android Native mimari çerçevesi

**Kurulu olan:** Android uygulama modülü (`app/`), Compose ile çalışan başlangıç görünümü, saf Kotlin puan kuralı ve birim testleri. Başlangıç görünümü gerçek veriye bağlı değildir; OCR, Room, CSV ve yedekleme henüz uygulanmamıştır.

## Planlanan katmanlar
- **UI:** Compose + adaptif tablet düzeni; ölçütlere doğrudan erişim, büyük etkileşim alanları, kalem/dokunma ergonomisi ve erişilebilirlik. Arc uyarlamaları sadece görünüm/etkileşim bileşenlerinde.
- **Domain:** Sınıf, öğrenci, rubrik, ölçüt ve değerlendirme sözleşmeleri; null/0 ve toplam kuralları. Android bağımlılığından mümkün olduğunca bağımsız.
- **Data:** Room ile cihaz içi ilişkisel kayıtlar; veri geçişleri, transaction ve benzersizlik; ilk onaylı rubrikleri JSON'dan yalnızca okuma. Hassas veriler güvenli saklama ve dışa aktarma kurallarına bağlı.
- **Import:** CameraX / sistem fotoğraf seçici, cihaz içi ML Kit OCR, sütun geometrisi ve kullanıcı doğrulama ekranı. OCR sonucu veri tabanına doğrudan yazılmaz.
- **Export:** CSV için UTF-8 ve Excel uyumluluğu; kullanıcı onaylı SAF dosya seçimi; açık formatlı doğrulanabilir yedek.

## Değişim sınırları
Kaynak rubrik JSON'ları salt kaynak girdi olarak korunur; uygulama güncellemesi bu dosyaları otomatik yeniden yazmaz. Kişisel öğrenciler repo/test fixture'larına kaydedilmez.

## Aşamalı teslim
1. Compose tablet kabuğu ve tasarım ilkeleri.
2. Kaynak rubrik doğrulaması, Room model/DAO, sınıf yönetimi, temel manuel veri akışı.
3. Puanlama akışı, eksik/tamamlandı durumları ve geri alma.
4. OCR + review ve CSV/yedekleme.
5. Erişilebilirlik, performans, cihaz matrisi ve rafine Arc etkileşimleri.

Bu sıralama ilk öneridir; özellikler gerçek test ve geri bildirimle önceliklendirilir.
