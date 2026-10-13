# Compose Design System → Puanla: sürümlü AAR entegrasyonu

## Etkin bağımlılık

Puanla, Compose Design System'i artık **yayımlanmış ve sabitlenmiş Maven AAR** olarak tüketir:

- Maven koordinatı: `com.knigdelioglu:compose-design-system:0.1.1`
- Kaynak: `https://raw.githubusercontent.com/knigdelioglu/compose-design-system/maven-repository/`
- Sürüm: `gradle.properties` içindeki `composeDesignSystemVersion=0.1.1`
- Mod: `composeUsePublishedByDefault=true`
- Dağıtım: AAR + POM + `.module` dosyaları birlikte yayımlanır. Gradle'ın transitive Compose bağımlılıkları metadata üzerinden çözülür.

Normal geliştirici derlemeleri **komşu `compose-design-system` dizinini gerektirmez**. `settings.gradle.kts`, Maven kullanımında `:design-system` modülünü projeye katmaz. Böylece Compose deposunun `main` dalındaki henüz yayımlanmamış değişiklikler Puanla'yı istem dışı güncellemez.

## CI ve sürüm yükseltmeleri

`.github/workflows/android.yml`:

- `push`, `pull_request`: Puanla'nın seçtiği **sabit AAR sürümünü** çözümler; `:app:dependencyInsight` ile tüketilen gerçek Maven koordinatını denetler.
- Android birim testleri, debug ve release derlemesi, emülatör UI testleri başarılıysa yalnız `main` push'unda debug APK yayımlar.
- `workflow_dispatch` → `compose_published_version`: farklı bir yayımlanmış Maven sürümü için **tek seferlik** uyumluluk testi; `gradle.properties` veya uygulama sürümünü otomatik değiştirmez.
- Yeni upstream kaynak commit'leri veya yeni Maven sürümleri **kendiliğinden ana dala alınmaz**. Yeni sürüme geçiş, `gradle.properties` değişikliği ve başarılı CI üzerinden kontrollü yapılır.

**Neden eski saatlik eşitleme kapatıldı?** Önceki sistem `.github/compose-design-system.sha` commit'ini yeni Compose `main` commit'lerine ilerletiyordu. Sürümlenmiş Maven dağıtımına geçince bu mekanizma hem işlevsiz hem de hangi Compose kodunun dağıtıldığı konusunda yanıltıcı olacaktı. CI artık yalnız sabit ve yayımlanmış AAR sürümünü kullanır.

## Geri alma (yalnız gerektiğinde)

Kaynak tabanlı önceki mekanizma bir acil durum yedeği olarak tutulur:

- Puanla → **Actions → Android CI → Run workflow** → `compose_legacy_sources=true`
- İş akışı yalnız `.github/compose-design-system.sha` dosyasındaki doğrulanmış commit'i checkout eder ve `-PcomposeUsePublishedByDefault=false` ile derler.
- Aynı çağrıda hem `compose_legacy_sources` hem `compose_published_version` belirtilmesi reddedilir.
- Yerelde aynı işlem:
  `./gradlew test :app:assembleDebug -PcomposeUsePublishedByDefault=false -PcomposeDesignSystemDir=/path/to/compose-design-system/design-system`

Geri dönüş işlemi, Maven AAR sürümünü değiştirmez ve otomatik release yayımlamaz. Kalıcı geri dönüş istenirse `gradle.properties` değeri ayrıca değerlendirilmelidir.

## Test sınırları

CI başarıları uygulamanın bütün tablet etkileşimlerini veya gerçek sınıf fotoğraflarıyla OCR doğruluğunu garanti etmez. CI APK'ları **debug imzalıdır** ve imza sertifikaları farklı koşularda değişebilir. Eski kurulum üstüne yerinde güncelleme yapmadan önce veri yedeği alın. Kalıcı imzalama için bkz. [Issue #26](https://github.com/knigdelioglu/puanla/issues/26).
