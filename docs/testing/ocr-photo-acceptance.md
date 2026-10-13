# OCR fotoğraf kabul testi — satır bütünlüğü ve perspektif

## Kapsam
Bu belge, sınıf listesinin OCR ile aktarılmasında başka öğrenciden ad/soyad alınmasını önleyen üç aşamanın gerçek fotoğraflarla kabulünü tanımlar. Kod tabanındaki sentetik kelime/koordinat testleri **gerçek fotoğraf testi değildir**.

## Kullanılacak veri
- Yazılı izinli veya sentetik 15–40 satırlık Türkçe sınıf listeleri.
- Sıra numarası, okul numarası, adı, soyadı, cinsiyet ve yatılı sütunları.
- Birleşik «Adı Soyadı» başlıklı varyant, çok sözcüklü ad ve soyad, eksik okul numarası.
- Aynı sayfanın düz, düşük ışıklı, hafif eğik, belirgin perspektifli ve 90° döndürülmüş fotoğrafları.
- Gerçek öğrenci fotoğrafları ve kişisel bilgiler CI artefaktlarına, GitHub deposuna veya hata raporlarına eklenmez; anonimleştirilir.

## Test sırası
1. Her örnek için fotoğraftaki fiziksel satırları ve beklenen hücre değerlerini referans tabloya kaydet.
2. Önce «Basılı Sınıf Listesi Fotoğrafı Seç» yolunu dene.
3. Aynı fotoğrafı «Belgeyi Tara / Perspektifi Düzelt» üzerinden galeriden yükle, kırp/perspektif düzelt ve tekrar OCR çalıştır.
4. Her adayın okul numarası, adı ve soyadını **kaynak fiziksel satır kimliğiyle** karşılaştır.
5. Sıra veya okul numarası eksik olduğunda aşağıdaki bütün öğrencilerde kayma bulunmadığını doğrula.
6. Belirsiz hücrelerin yeniden okuma önerisinin ilk okumayı otomatik değiştirmediğini doğrula.
7. Google Play Hizmetleri tarayıcısı desteklenmiyorsa normal fotoğraf seçme yolunun çalıştığını doğrula.
8. Tablet cihazında önceki/sonraki, satıra odaklanma ve tüm fotoğraf geçişlerini dene.

## Kabul kuralları
- **Bloklayıcı:** Herhangi bir öğrencinin başka fiziksel satırından alınan ad veya soyadı, belirsizlik göstermeden kalıcı kayda dönüşüyor.
- **Bloklayıcı:** Okunamayan numara veya isim, sonraki öğrencilerin verilerini kaydırıyor ya da o fiziksel satır sessizce kayboluyor.
- **Bloklayıcı:** İkinci OCR önerisi öğretmen onayı olmadan öğrenci kimliğini değiştiriyor.
- **Bloklayıcı:** Google belge tarayıcısı kullanılamadığında galeri üzerinden aktarım tamamen devre dışı kalıyor.
- **Performans:** Uzun sınıf listelerinde ek OCR tekrarları kaynakları tüketmemeli; güncel kod en fazla 12 belirsiz hücreyi tekrar okur.
- **Gözlem:** Kesin eşleşme oranı, belirsiz satır oranı, kullanıcı düzeltme sayısı ve aktarım süresi fotoğraf türüne göre kaydedilir.

## Şu anki durum
- GitHub CI ve sentetik/cihaz testleri PR üzerinde çalışır; buradan gerçek belge doğruluğu çıkarılamaz.
- Gerçek fotoğraf kabul ölçümleri henüz sağlanmadı. Çapraz satır kaymasının sahada sıfırlandığı iddia edilmemelidir.
