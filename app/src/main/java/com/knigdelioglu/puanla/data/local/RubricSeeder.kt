package com.knigdelioglu.puanla.data.local

import java.util.UUID

object RubricSeeder {

    suspend fun seedBuiltInRubricsIfEmpty(rubricDao: RubricDao) {
        val existing = rubricDao.getAllRubrics()
        if (existing.isNotEmpty()) return

        val rubrics = getBuiltInRubrics()
        rubrics.forEach { (rubric, criteriaWithLevels) ->
            val criteria = criteriaWithLevels.map { it.first }
            val levels = criteriaWithLevels.flatMap { it.second }
            rubricDao.insertRubric(rubric)
            rubricDao.insertCriteria(criteria)
            rubricDao.insertLevels(levels)
        }
    }

    private fun getBuiltInRubrics(): List<Pair<RubricEntity, List<Pair<CriterionEntity, List<CriterionLevelEntity>>>>> {
        return listOf(
            createRubric(
                id = "rubric_oral_presentation",
                title = "Sözlü Sunum",
                description = "Hazırlıklı bir sunumda içerik, düzen, dil, anlaşılabilirlik ve dinleyiciyle iletişimi birlikte değerlendirir.",
                grade = 11,
                targetTask = "Sunum ve Canlandırma",
                criteria = listOf(
                    CriterionDef(
                        title = "İçeriğin doğruluğu ve yeterliliği",
                        description = "Sunumun konuya uygun, doğru ve yeterli bilgi içerip içermediğini değerlendirir.",
                        maxPoints = 25,
                        levels = listOf(
                            LevelDef(0, "İçerik yok veya tamamen yanlış."),
                            LevelDef(10, "Konuya ilişkin bilgi sınırlıdır; önemli eksikler var."),
                            LevelDef(18, "Temel bilgiler doğru ve yeterlidir; konu uygun açıklanır."),
                            LevelDef(25, "Bilgiler doğru, yeterli ve seçicidir; ayrıntılar amaca güçlü hizmet eder.")
                        )
                    ),
                    CriterionDef(
                        title = "Düzen ve anlatım bütünlüğü",
                        description = "Sunumun giriş, gelişme ve sonuç ilişkisini; düşünceler arasındaki bağlantıları değerlendirir.",
                        maxPoints = 20,
                        levels = listOf(
                            LevelDef(0, "Düzen ve bağlantı yok."),
                            LevelDef(8, "Temel bir sıra var ancak bölümler kopuktur."),
                            LevelDef(15, "Düşünceler mantıklı bir sırada sunulur; bütünlük korunur."),
                            LevelDef(20, "Düşünceler amaçlı bir düzen içinde ilerler; geçişler kusursuzdur.")
                        )
                    ),
                    CriterionDef(
                        title = "Dil ve ifade",
                        description = "Sözcük seçimi, cümle kuruluşu ve anlatımın amaca ve dinleyiciye uygunluğunu değerlendirir.",
                        maxPoints = 20,
                        levels = listOf(
                            LevelDef(0, "Dil kullanımı anlaşılmayı engelliyor."),
                            LevelDef(8, "Anlatım anlaşılır ancak tekrarlar ve sınırlı söz varlığı var."),
                            LevelDef(15, "Dil açık ve konuya uygundur; sözcük seçimi etkilidir."),
                            LevelDef(20, "Dil zengin, doğal ve amaca tam uygundur; anlamı güçlendirir.")
                        )
                    ),
                    CriterionDef(
                        title = "Ses, akıcılık ve anlaşılabilirlik",
                        description = "Konuşmanın duyulabilirliğini, akışını ve anlamayı destekleyen söyleyiş özelliklerini değerlendirir.",
                        maxPoints = 20,
                        levels = listOf(
                            LevelDef(0, "Ses düzeyi veya söyleyiş anlaşılmayı güçleştirir."),
                            LevelDef(8, "Sık duraksama veya tekdüze tonlama akışı bozar."),
                            LevelDef(15, "Konuşma çoğunlukla akıcı ve anlaşılırdır."),
                            LevelDef(20, "Konuşma akıcı, vurgu ve tonlama etkileyici ve amaca uygundur.")
                        )
                    ),
                    CriterionDef(
                        title = "Dinleyiciyle iletişim ve sunum becerisi",
                        description = "Dinleyiciyle bağ kurma, araçları amaca uygun kullanma ve süreyi yönetme becerisini değerlendirir.",
                        maxPoints = 15,
                        levels = listOf(
                            LevelDef(0, "İletişim ve süre yönetimi yetersizdir."),
                            LevelDef(5, "Dinleyiciyle sınırlı iletişim; süre dengesizdir."),
                            LevelDef(10, "İletişim sürdürülür; araçlar ve süre çoğunlukla uygundur."),
                            LevelDef(15, "Dinleyiciyle güçlü bağ; araçlar ve süre etkili biçimde yönetilir.")
                        )
                    )
                )
            ),
            createRubric(
                id = "rubric_prepared_speaking",
                title = "Hazırlıklı Konuşma",
                description = "Hazırlıklı bireysel konuşmalarda düşünce geliştirme, bütünlük, akıcılık, anlaşılabilirlik ve dil kullanımını değerlendirir.",
                grade = 11,
                targetTask = "Bireysel Konuşma Görevi",
                criteria = listOf(
                    CriterionDef("İçerik ve düşünce geliştirme", "Konuşmanın konuya uygun düşünceler içerme ve bunları açıklama düzeyi.", 20),
                    CriterionDef("Anlatım bütünlüğü", "Düşüncelerin mantıklı bir sıra ve açık bağlantılar içinde sunulması.", 20),
                    CriterionDef("Akıcılık", "Gereksiz duraksama ve tekrarlar olmadan sürdürülebilirlik.", 20),
                    CriterionDef("Söyleyiş ve anlaşılabilirlik", "Vurgu, tonlama ve açık sesletim ile dinleyiciye ulaşabilme.", 20),
                    CriterionDef("Dil ve söz varlığı", "Sözcüklerin ve cümle yapılarının zenginliği ve amaca uygunluğu.", 20)
                )
            ),
            createRubric(
                id = "rubric_interactive_speaking",
                title = "Etkileşimli Konuşma",
                description = "Tartışma, görüşme ve karşılıklı konuşmalarda dinleme, karşılık verme, gerekçelendirme ve iletişim davranışlarını değerlendirir.",
                grade = 11,
                targetTask = "Grup Münazara / Panel",
                criteria = listOf(
                    CriterionDef("Konuya uygun katkı", "Söylenenlerin tartışılan konu ve amaçla ilişkisi.", 20),
                    CriterionDef("Dinleme ve karşılık verme", "Karşı tarafın söylediklerini takip edip uygun karşılık geliştirme.", 20),
                    CriterionDef("Görüşü gerekçelendirme", "Görüşlerin neden, kanıt veya örneklerle tutarlı desteklenmesi.", 20),
                    CriterionDef("İletişim ve söz sırası", "Söz sırasına uyma, iş birliği ve yapıcı konuşma tavrı.", 20),
                    CriterionDef("Dil kullanımı", "Saygılı, amaca uygun ve nitelikli Türkçe kullanımı.", 20)
                )
            ),
            createRubric(
                id = "rubric_group_work",
                title = "Grup Çalışması - Bireysel Katkı",
                description = "Grup projelerinde öğrencinin bireysel sorumluluğunu, iş birliğini ve üretkenliğini değerlendirir.",
                grade = 11,
                targetTask = "Ortak Performans Projesi",
                criteria = listOf(
                    CriterionDef("Görev ve sorumluluk bilinci", "Kendisine verilen rolü zamanında ve eksiksiz yerine getirme.", 25),
                    CriterionDef("Grup içi iş birliği ve iletişim", "Ekip arkadaşlarıyla yapıcı, destekleyici ve uyumlu çalışma.", 25),
                    CriterionDef("Problem çözme ve üretkenlik", "Çalışma sürecinde karşılaşılan güçlüklere çözüm üretme.", 25),
                    CriterionDef("Zaman yönetimi ve teslim", "Ara hedeflere ve nihai teslim takvimine uyum.", 25)
                )
            ),
            createRubric(
                id = "rubric_reading_skill",
                title = "Okuma ve Metin Çözümleme",
                description = "Edebi metinleri anlama, ana ve yardımcı düşünceleri belirleme, dil ve anlatım özelliklerini tahlil etme.",
                grade = 11,
                targetTask = "Metin Tahlili",
                criteria = listOf(
                    CriterionDef("Metni anlama ve ana düşünce", "Metnin temel tezini veya temasını doğru tespit etme.", 20),
                    CriterionDef("Metin içi çıkarım ve yorum", "Açık ve örtük iletileri doğru yorumlama.", 20),
                    CriterionDef("Dil ve anlatım özellikleri", "Anlatım biçimleri, düşünceyi geliştirme yolları ve edebi sanatlar.", 20),
                    CriterionDef("Eleştirel değerlendirme", "Metnin tutarlılığını ve yazarın bakış açısını sorgulama.", 20),
                    CriterionDef("Söz varlığı çözümleme", "Kelimelerin bağlamdaki anlamlarını ve çağrışımlarını kavrama.", 20)
                )
            ),
            createRubric(
                id = "rubric_writing_skill",
                title = "Yazma Becerisi",
                description = "Düşünceleri planlı, kurallı ve etkili bir biçimde yazılı metne dönüştürme becerisi.",
                grade = 11,
                targetTask = "Metin Yazma Çalışması",
                criteria = listOf(
                    CriterionDef("Konuyu sınırlandırma ve ana düşünce", "Yazının odak noktasını belirleme ve tez geliştirme.", 20),
                    CriterionDef("Paragraf düzeni ve bütünlük", "Giriş, gelişme ve sonuç paragrafları arasındaki mantıksal bağ.", 20),
                    CriterionDef("İmla ve noktalama kuralları", "Yazım kurallarına ve noktalama işaretlerine tam uyum.", 20),
                    CriterionDef("Sözcük seçimi ve üslup", "Amaca ve türe uygun kelime dağarcığı ve anlatım çeşitliliği.", 20),
                    CriterionDef("Başlık ve metin uyumu", "Metnin içeriğini yansıtan özgün ve çarpıcı başlık seçimi.", 20)
                )
            ),
            createRubric(
                id = "rubric_argumentative_writing",
                title = "Tartışmacı Metin Yazma",
                description = "Bir görüşü savunmak veya karşı görüşü çürütmek amacıyla yazılan eleştirel deneme ve makaleler.",
                grade = 11,
                targetTask = "Deneme / Eleştiri",
                criteria = listOf(
                    CriterionDef("Tezin netliği", "Savunulan görüşün açık, net ve tartışılabilir ifade edilmesi.", 25),
                    CriterionDef("Kanıt ve gerekçelendirme", "Görüşü destekleyen sağlam gerekçeler, alıntılar ve örnekler.", 25),
                    CriterionDef("Mantıksal tutarlılık", "Düşünceler arasındaki nedensellik ve akılcı kurgu.", 25),
                    CriterionDef("Sonuç ve ikna edicilik", "Okuyucuyu ikna eden güçlü ve toparlayıcı final değerlendirmesi.", 25)
                )
            ),
            createRubric(
                id = "rubric_project_assessment",
                title = "Proje Değerlendirme",
                description = "Uzun soluklu edebi ve kültürel araştırma projelerinin kapsamlı değerlendirilmesi.",
                grade = 11,
                targetTask = "Yıllık / Dönemlik Proje",
                criteria = listOf(
                    CriterionDef("Araştırma ve kaynak kullanımı", "Birincil ve ikincil kaynakların zenginliği ve doğruluğu.", 20),
                    CriterionDef("Bilimsel ve edebi doğruluk", "İçerikteki bilgilerin akademik/müfredat standartlarına uygunluğu.", 20),
                    CriterionDef("Yöntem ve süreç", "Planlama adımlarına uyum ve çalışma disiplini.", 20),
                    CriterionDef("Ürün niteliği ve özgünlük", "Ortaya konan rapor veya eserin özgünlüğü ve tasarımı.", 20),
                    CriterionDef("Sunum ve savunma", "Projenin dinleyicilere aktarılması ve soruların cevaplanması.", 20)
                )
            )
        )
    }

    private data class CriterionDef(
        val title: String,
        val description: String,
        val maxPoints: Int,
        val levels: List<LevelDef> = listOf(
            LevelDef(0, "Yetersiz"),
            LevelDef(maxPoints / 3, "Geliştirilmeli"),
            LevelDef((maxPoints * 2) / 3, "İyi"),
            LevelDef(maxPoints, "Çok İyi")
        )
    )

    private data class LevelDef(val points: Int, val description: String)

    private fun createRubric(
        id: String,
        title: String,
        description: String,
        grade: Int,
        targetTask: String,
        criteria: List<CriterionDef>
    ): Pair<RubricEntity, List<Pair<CriterionEntity, List<CriterionLevelEntity>>>> {
        val rubric = RubricEntity(
            id = id,
            title = title,
            description = description,
            grade = grade,
            targetTask = targetTask,
            isBuiltIn = true
        )

        val criteriaList = criteria.mapIndexed { index, def ->
            val criterionId = "${id}_crit_$index"
            val crit = CriterionEntity(
                id = criterionId,
                rubricId = id,
                title = def.title,
                description = def.description,
                maxPoints = def.maxPoints,
                orderIndex = index
            )
            val levels = def.levels.mapIndexed { lIndex, lDef ->
                CriterionLevelEntity(
                    id = "${criterionId}_lvl_$lIndex",
                    criterionId = criterionId,
                    points = lDef.points,
                    description = lDef.description,
                    orderIndex = lIndex
                )
            }
            crit to levels
        }

        return rubric to criteriaList
    }
}
