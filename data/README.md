# Onaylı kaynak veriler

Bu klasör rubrik JSON dosyaları ve şema gibi **kaynak veriler** içindir. Uygulamanın çalışma zamanı öğrenci kayıtları burada tutulmaz.

`rubrics.json`, kullanıcının sağladığı `35-ogrenci-rubrik-puanlama.xlsx` çalışma kitabından aktarılmış onaylı rubrik kataloğudur. Android uygulaması bu dosyayı paket varlığı olarak okur ve eksik rubrikleri Room'a ilk açılışta, tek transaction içinde ekler. Katalog yalnızca **11. sınıf** için geçerlidir.

Katalogda 8 rubrik ve 48 ölçüt vardır; her rubriğin azami puanı 100'dür. Çalışma kitabında yalnızca “İletişim Engelleri” rubriğinin ölçüt açıklamaları ve 7 maddelik grup hazırlık listesi bulunur. Diğer rubriklerde bulunmayan açıklama/düzeyler üretilmemiştir. Grup hazırlık listesi bireysel nota eklenmez. Boş “Sınıf Listesi” sekmesinden öğrenci veya sınıf aktarılmamıştır.

Kaynak dosyadaki rubrik başlıkları, ölçüt metinleri, puan ağırlıkları ve sıralama değiştirilmeden aktarılmalıdır. Yeni bir kaynak geldiğinde içerik otomatik dönüştürülmeden önce açık talep ve test gerekir.
