# Doğrulama stratejisi

## Otomatik
- Saf Kotlin: puan sınırı, puanlanmadı ≠ 0, yarım rubrik kesin toplam içermez, boş rubrik tamamlanmış sayılmaz.
- İleride Room: sınıf/öğrenci/rubrik izolasyonu, migration ve transaction testleri.
- İleride OCR: satır/sütun hizalama, çok sözcüklü ad, tek sütun soyadı, cinsiyet/pansiyon sızıntısı, düşük güven ve manuel onay.
- İleride Compose: tablet 11 inç 1920x1200/90Hz benzeri konfigürasyon, yatay-dikey, font scale, TalkBack, dış klavye ve geri tuşu.
- İleride CSV/yedek: export/import round trip, kısmi not statüsü, sayısal kesinlik, UTF-8 Türkçe karakterler.

## Mevcut test komutu
`gradle :app:testDebugUnitTest :app:assembleDebug` (Gradle 8.13, JDK 17, Android SDK 36 kurulumu gerektirir). Gradle Wrapper henüz üretilmedi; yerel geliştirmede `gradle wrapper --gradle-version 8.13` ile wrapper eklenecek. CI sistem Gradle'ını kurar.

Bir GitHub commit'i tek başına build veya APK başarı kanıtı değildir.
