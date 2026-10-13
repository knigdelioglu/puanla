# Compose Design System → Puanla otomatik güncelleme

## Sözleşme

Puanla `implementation(project(":design-system"))` ile kardeş Compose Design System kaynak modülünü kullanır. Kopya bileşen yoktur. Ancak artık sürüm seçimi `.github/compose-design-system.sha` içindeki **40 karakterlik commit SHA** ile sabittir. Böylece günlük Puanla derlemesi upstream `main` hareket etti diye değişmez.

- `push`/`pull_request`: sabitlenmiş SHA ile derleme, birim testleri, APK üretimi ve Android emülatör testleri. `main` push APK'sı ancak tüm kontroller geçerse Releases'a çıkar.
- Compose deposunda `main` push'u başarılı **Android** CI'yı tamamlayınca `Notify Puanla of verified Compose updates` iş akışı bildirim gönderebilir.
- Puanla `repository_dispatch`, `workflow_dispatch` ve **her saat 19. dakikada** zamanlanmış kontrolle upstream `main` SHA'sına bakar. Bildirim için token olmasa da zamanlanmış kontrol çalışır (GitHub schedule gecikebilir).
- Yeni SHA, Compose deposunun **Android push CI** çalışması başarıyla sonuçlandıysa aday olur. Puanla birim/derleme/emülatör testleri de geçerse pin dosyası `main` dalına bot commit'iyle güncellenir ve **o doğrulanmış kombinasyonun** APK'sı Releases'a çıkar.
- Hata, eksik CI kanıtı veya derleme/test sorunu olursa pin değişmez, APK yayımlanmaz. Puanla `main` ilerlediyse eski uygulama commit'ini içeren aday APK da yayımlanmaz; sonraki kontrol yeni kodla tekrar dener.

## Bir kereye mahsus hızlı bildirim kurulumu (isteğe bağlı)

GitHub'da **Compose Design System → Settings → Secrets and variables → Actions** altında `PUANLA_DISPATCH_TOKEN` deposu sırrı tanımlayın. İnce kapsamlı GitHub kişisel erişim token'ı yalnız `knigdelioglu/puanla` deposunda **Contents: read/write** iznine sahip olmalıdır (repository dispatch). İki depo public olduğu için yalnız kaynak checkout için token gerekmez. Token olmadan da saatlik otomatik eşitleme aktiftir.

Ayrıca Puanla deposunda **Settings → Actions → General → Workflow permissions** altında `Read and write permissions` seçeneğine ve varsayılan `GITHUB_TOKEN` ile içerik yazmaya izin verilmelidir. Dal koruması bot push'una engelse terfiyi sağlayacak izin veya farklı PR-onay akışı gerekir. Botun PR, ürün/domain şeması veya OCR verisi yazma hakkı yoktur; yalnız `.github/compose-design-system.sha` sabitini ilerletir.

## Güvenlik ve sınırlar

Kaynak derlemenin ref'i **Git commit SHA** olarak checkout edilir, canlı `main` değil. Bu hem tekrarlanabilirlik hem rollback sağlar. Compose'un yeni bileşenleri Puanla ekranına otomatik bağlanmaz; yalnız zaten kullanılan ortak API davranışı otomatik güncellenir. API kırılmalarında adaptasyon gerekir. Başarılı bir CI gerçek tablet üzerinde tüm kabul testlerinin yerine geçmez. Önizleme APK'ları debug imzalıdır; üretim için ayrı imzalı yayın kanalı tercih edilmelidir.

### Elle çalıştırma

Puanla → **Actions → Android CI → Run workflow**: yeni upstream commit'ini hemen kontrol eder. Pin sabitse hiçbir şey yayımlamaz. Hatalar Android CI iş akışının `resolve`, `verify` ve `promote-compose` işlerinde görünür.

### Geri alma

Doğrulanmış eski SHA'yı `.github/compose-design-system.sha` dosyasına yazarak Puanla'nın eski Compose koduyla tekrar derlenmesini sağlayabilirsiniz. Ancak bir sonraki otomatik senkron, yeni upstream SHA'yı tekrar test edip başarı halinde benimseyebilir; kalıcı durdurma için zamanlanmış/dispatch tetikleyicilerini geçici olarak kapatın.


## Henüz yayımlanmamış Maven AAR için korumalı geçiş

`composeDesignSystemVersion=0.1.0`, gelecekte doğrulanacak sürümü belirtir; **yayımlanmış bir AAR değildir**. `composeUsePublishedByDefault=false` olduğu sürece Puanla, doğrulanmış `.github/compose-design-system.sha` kaynak sürümünü ve mevcut CI/promosyon akışını kullanmaya devam eder.

AAR, Compose deposunda erişilebilir bir Maven sürümü olarak yayımlandıktan, Puanla birim ve emülatör testlerinde doğrulandıktan sonra `composeUsePublishedByDefault=true` değişikliği ayrı PR ile yapılmalıdır. Yayınlanmamış sürümü zorunlu tutmak uygulamayı derlenemez duruma getirir. Geçişten önce kaynak tabanlı güvenli geri dönüş korunur.
