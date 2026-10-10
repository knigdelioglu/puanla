package com.knigdelioglu.puanla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Ürün iskeleti: şu anda veri kaydetmez ve rubrik değerlendirmesi yapmaz.
 * Tablet düzeni gerçek Compose ölçülerine göre adaptif kurulur.
 */
@Composable
fun PuanlaApp() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            BoxWithConstraints {
                val wide = maxWidth >= 840.dp
                Scaffold { insets ->
                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(insets).padding(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            IntroPanel(Modifier.weight(1.2f))
                            SetupPanel(Modifier.weight(1f))
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(insets).padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            IntroPanel(Modifier.fillMaxWidth())
                            SetupPanel(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroPanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Puanla", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Öğrenciyi izle. Ölçüte göre puanla.", style = MaterialTheme.typography.titleLarge)
        Text(
            "Android Native uygulaması tablet öncelikli olarak yeniden kuruluyor. " +
                "Öğrenci bilgileri ve değerlendirmeler cihazda korunacak.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SetupPanel(modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Başlangıç altyapısı", style = MaterialTheme.typography.headlineSmall)
            Text("• Kotlin + Jetpack Compose")
            Text("• Geniş ekrana göre uyarlanan yerleşim")
            Text("• Rubrik/öğrenci verileri için korumalı alan")
            Text("• Arc etkileşimleri için yerel uyarlama planı")
            Spacer(Modifier.height(6.dp))
            Text(
                "Henüz sınıf, OCR ve puanlama işlevleri etkin değildir.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
