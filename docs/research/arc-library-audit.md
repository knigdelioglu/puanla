# Arc Library kaynak incelemesi — 2026-10-10

**Upstream:** https://github.com/kuratlielia/arc-library  
**İncelenen dal/commit:** `main` / [86330cc](https://github.com/kuratlielia/arc-library/commit/86330cc9270c4acc55b7204c9583fcbaa378192c)  
**Yayın:** GitHub üzerindeki son sürüm `v1.0.0` (2026-09-29). Plan mevcut `main` kodundan çıkarılmıştır; yayın etiketi ile birebir eşit olduğu varsayılmaz.  
**Lisans:** [MIT](https://github.com/kuratlielia/arc-library/blob/main/LICENSE), Copyright (c) 2026 Elia Kuratli. Yeniden kullanılan/uyarlanan substantial source parçalarında izin ve copyright bildirimi korunmalı; ücretli/Pro içerik dahil değildir.

## Kaynak mimarisi

README'ye göre **108 React bileşeni ve 22 blok**. Kod; React 19, TypeScript, `motion/react`, CSS Modules ve bazı bileşenlerde Radix UI üzerine kurulu. `lib/arc-provider.tsx` React bağlantı/görsel adaptörü, `registry/motion-tokens.ts` zamanlama ve hareket sözlüğü sunuyor. Bir Android AAR, Kotlin veya Jetpack Compose paketi yok. `npm`, `shadcn`, `@radix-ui`, DOM pointer events ve CSS doğrudan Android'de kullanılamaz.

Bu proje Arc'ı Gradle'a indirilecek hazır Android bağımlılığı olarak **eklemeyecek**; yararlı **davranış ve görsel ilkelerini Compose'un state/semantics/animation/gesture API'leriyle** yeniden oluşturacaktır. Tüm 108 bileşeni birden taşımak verimsiz; ürün akışında kullanılanlar artımlı uyarlanacaktır.

## Ürüne göre öncelik matrisi

| Upstream dosyası / bileşen | Puanla ihtiyacı | Öneri | Compose karşılığı / özel dikkat |
| --- | --- | --- | --- |
| [elastic-slider](https://github.com/kuratlielia/arc-library/tree/main/registry/components/elastic-slider) | Parmakla hızlı, ölçüt üst sınırına uyumlu puan seçimi | **P0 — uyarlama adayı** | `pointerInput`, controlled `Int?` value, clamp/step/detent, sürüklerken başparmak üzeri sayı balonu, bıraktığında onCommit; null ≠ 0 |
| [segmented-control](https://github.com/kuratlielia/arc-library/tree/main/registry/components/segmented-control) | Geliştirilebilir/İyi/Çok iyi gibi kısa niteliksel seçimler | **P0** | `Row`, `selectableGroup`, kayan `animate*AsState` gösterge, büyük hedefler, semantics |
| [number-field](https://github.com/kuratlielia/arc-library/tree/main/registry/components/number-field) | Kesin tam sayı puan girme/düzeltme | **P0** | sayı klavyesi, ± kontrol, aralık denetimi; fiziksel klavye; değer kontrolü domain'de |
| [radio-cards](https://github.com/kuratlielia/arc-library/tree/main/registry/components/radio-cards) | Rubrik ve değerlendirme türü seçimi | P1 | Compose seçilebilir kart grubu ve okunabilir seçim halkası |
| [progress](https://github.com/kuratlielia/arc-library/tree/main/registry/components/progress), [badge](https://github.com/kuratlielia/arc-library/tree/main/registry/components/badge) | Tam/kısmi/başlanmadı işaretleri | **P0** | `LinearProgressIndicator`, açık metin ve renk harici durum işareti; kesin puan ayrımı |
| [toast-stack](https://github.com/kuratlielia/arc-library/tree/main/registry/components/toast-stack) | Kayıt ve geri alma bildirimi | P1 | `SnackbarHost` ve uygulanabilir Undo; sadece görünür animasyona güvenilmez |
| [tabs](https://github.com/kuratlielia/arc-library/tree/main/registry/components/tabs) | Sınıflar/rubrikler arasında hızlı geçiş | P1 | tablet taşmayan sekmeler, focus/keyboard, seçili çizgi |
| [bottom-sheet](https://github.com/kuratlielia/arc-library/tree/main/registry/components/bottom-sheet) | Yardımcı seçim ve düzenleme | P1 | dar ekran `ModalBottomSheet`; geniş ekranda kalıcı yan panel öncelikli |
| [inline-edit](https://github.com/kuratlielia/arc-library/tree/main/registry/components/inline-edit) | Öğrenci bilgisi düzeltme | P1 | açık kaydet/iptal, hata/geri alma, erişilebilir `TextField` |
| [swipe-actions](https://github.com/kuratlielia/arc-library/tree/main/registry/components/swipe-actions) | Öğrenci listesinde hızlı yardımcı eylem | P2/koşullu | yalnız açık alternatif buton/menüyle; asla puan verme veya silme için tek yöntem değil |
| [confirm-morph](https://github.com/kuratlielia/arc-library/tree/main/registry/components/confirm-morph) | Geri dönüşü zor işlemlerde kontrollü onay | P2 | görünür onay, veri kaybına karşı güvenli geri dönüş |
| [empty-state](https://github.com/kuratlielia/arc-library/tree/main/registry/components/empty-state), [skeleton](https://github.com/kuratlielia/arc-library/tree/main/registry/components/skeleton) | İlk sınıf/boş veri/işleniyor durumu | P1 | durumun metinsel anlatımı, shimmer zorunlu değil |
| [metric-card](https://github.com/kuratlielia/arc-library/tree/main/registry/components/metric-card) | Sınıf sayıları ve ortalama | P2 | sayısal güncelleme, eksik notların sınıf ortalamasını etkilememesi |

**Öncelikli olmayanlar:** pazarlama/auth blokları, chart galerilerinin büyük kısmı, fare hover'ına dayanan kontroller, Pro bileşenleri. Bunların aktarılması Puanla'nın çekirdek amacına hizmet etmez.

## Somut kod bulguları

- `elastic-slider.tsx`: `value/defaultValue`, `onValueChange` ve **`onValueCommit`** ayrımı; `min/max/step`, mark/detent, `snap()`, pointer capture, klavye ve `role=slider`. Puanla'da kontrolün `Int?` seçilmemiş durumunu ayrı modellemek gerekir.
- `number-field.tsx`: aralıklı numerik düzenleme, artır/azalt, opsiyonel yatay label scrub; bilgisayar klavyesi alternatifi. Compose'da scrub'u ilk sürüm için isteğe bağlı tut.
- `segmented-control.tsx`: seçili göstergenin taşınması, ok tuşları, `aria-pressed` ve taşmada kaydırma. Tek başına renk değişimi yeterli değil.
- `radio-cards.tsx`: seçili ring, yalnız bir seçenek, klavye odağı ve radio semantiği.
- `swipe-actions.tsx`: tek satır açık tutma, hareket eşiği ve görünür alternatif menü. Kritik puanlamayı keşfedilemeyen kaydırma eylemine bağlamama.
- `bottom-sheet.tsx`: detent seviyeleri, içerik-sürükleme çatışması ve odağın geri verilmesi.
- `toast-stack.tsx`: Undo, durumun yerinde güncellenmesi ve erişilebilir bilgi geri bildirimi.
- `registry/motion-tokens.ts`: süreler `instant .12s`, `fast .16s`, `standard .24s`, `considered .48s`; yay davranışları `responsive/gentle/snappy/smooth/morph`. Web Motion yay sabitleri Compose `spring` değerlerine **matematiksel olarak birebir kopyalanmamalı**; cihaz üzerinde kalibre edilmeli.
- Çoğu hareketli kaynakta `useReducedMotion` var. Android'de `MotionDurationScale` ve sistem animasyon tercihleri göz önüne alınmalı.

## Riskler ve lisans

1. **Port, kopyala-yapıştır değildir.** DOM, Motion ve Radix yaklaşımı Compose'a otomatik dönüşmez; React/CSS kodu Android proje ağacına eklenmez.
2. **Performans:** puan sürükleme `pointerInput` ve frame başına yalnız UI state güncellemesiyle akmalı; her piksel hareketinde veritabanına yazılmamalı.
3. **Semantics:** hızlı derecelendirme erişilebilir doğrudan seçenekler ve sayısal alternatif sunmalı; yalnız jest ile kullanılmamalı.
4. **Attribution:** port edilen substantial Arc kaynakları için MIT metni ve telif notu korunur; `docs/plans/arc-compose-adaptation.md` kapsam takip tablosudur.
5. **Kaynak değişikliği:** ileride Arc upstream yükseltmeleri otomatik olarak Android ürününe yansıtılmaz. Kaynak commit, davranış farkı ve regresyon testleriyle bilinçli yükseltme yapılır.
