# Ürün kapsamı — 2026-10-10

Kaynak: Kullanıcının sağladığı **Puanla — Ürün Amacı ve İşlevsel Kapsam Raporu**. Bu belge ekran tasarımını veya eski teknik mimariyi devralmaz.

## Amaç ve kullanım
Öğretmenin sınıf içinde yazılı, sözlü, drama, sunum ve diğer performans görevlerinde öğrencileri önceden tanımlanmış rubriklere göre **hızlı ve güvenilir** değerlendirmesi. Birincil kullanıcı çoklu sınıf yöneten öğretmendir.

## Gereksinimler
1. **Sınıflar:** Birden fazla şube; düzey 9–12 bilgisi; ekleme, düzeltme, mükerrer öğrenci kontrolü ve izolasyon.
2. **Liste aktarımı:** Fotoğraftan satır/sütun temelli OCR; ad (birden çok sözcük olabilir), soyad (kaynakta bağımsız sütun), okul numarası ve komşu cinsiyet/pansiyon/sıra alanları karıştırılmaz. Türkçe karakter veya OCR yazım farklarıyla olası yinelenen adlar uyarı alır; otomatik birleştirilmez ve aktarım için öğretmen kararı gerekir. Belirsiz alanlar işaretlenir; öğretmen doğrulayıp onaylar. Manuel alternatif her zaman kullanılabilir.
3. **Rubrik:** Her ölçütün azami puanı vardır. 0 puan ile puanlanmamış (null) ayrı durumdur. Hızlı niteliksel ve gerektiğinde aralık içi tam sayı puanlama desteklenir.
4. **Tamamlama:** Zorunlu ölçütleri eksik değerlendirme kesin not değildir. Puanlar öğrenci, rubrik ve sınıfa göre izole; değişiklikler düzeltilebilir/geri alınabilir.
5. **İlerleme:** Tamam/kısmi/başlanmamış durumlar; tamamlananlardan toplam ve sınıf ortalaması; ölçüt ayrıntılı CSV.
6. **Gruplar:** Grup hazırlığı, bireysel 100 puanlık değerlendirmeye kendiliğinden katılmaz.
7. **Güven:** Çevrimdışı veri kullanımı, yerel kayıt, yedekleme/geri yükleme; öğrencilerin kişisel bilgileri gereksiz dış hizmetlere çıkmaz.

## Mevcut içerik bilgisi
Kullanıcının sağladığı `35-ogrenci-rubrik-puanlama.xlsx` dosyası 11. sınıf için 8 rubrik ve 48 ölçüt içerir: **İletişim Engelleri, E-posta, Türk Dünyası Konuşma, Müze İzlenim Yazısı, Kemal Tahir Mülakat, Diyalog Dönüştürme, Tiyatro Canlandırma, Afiş**. Her rubrik 100 puandır. Ölçüt metinleri ve puan ağırlıkları `data/rubrics.json` dosyasına aktarılmıştır. Yalnızca kaynakta bulunan İletişim Engelleri ölçüt açıklamaları ve 7 maddelik grup hazırlık listesi eklenmiştir; grup hazırlığı bireysel nota dâhil değildir. Kaynak çalışma kitabındaki boş sınıf listesi aktarılmamıştır.

## Değişmez kabul koşulları
- Aynı sınıfta farklı öğrenciler ve farklı sınıflarda aynı öğrenci için kayıt izolasyonu.
- Geçersiz puan reddi; null/0 farkı; eksik nota kesin sonuç denmemesi.
- OCR: isim/soyad/sayı ve komşu sütunların karışmaması; belirsiz veriye uydurma yapılmaması.
- Araya giren uygulama kapanması veya bağlantı kesintisinden sonra çalışmaya devam.
- CSV, yedekleme ve geri yüklemede anlam/kimlik korunumu.
- Gerçekçi, kişisel verilerden arındırılmış okul listeleriyle OCR değerlendirmesi.

Ürün raporu arayüz, ekran veya animasyon tarif etmez; tablet UX ve Arc uyarlaması bu davranış sözleşmesi bozulmadan özgürce tasarlanır.
