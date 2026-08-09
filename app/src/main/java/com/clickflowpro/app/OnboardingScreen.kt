package com.clickflowpro.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Uygulama ilk açıldığında gösterilen tam ekran karşılama/izin ekranı.
 * Erişilebilirlik izni verilene kadar (veya kullanıcı "Şimdilik geç"
 * derse) burada kalır; izin tespit edildiği anda MainActivity otomatik
 * olarak ana ekrana geçer (bkz. onResume + refreshAccessibilityStatus).
 */
@Composable
fun OnboardingScreen(
    serviceEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(26.dp)),
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(28.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (serviceEnabled) {
                                    MaterialTheme.colorScheme.secondary.copy(alpha = .16f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (serviceEnabled) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (serviceEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        stringResource(R.string.onboarding_accessibility_title),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.onboarding_accessibility_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (serviceEnabled) {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        ) {
                            Text(stringResource(R.string.onboarding_continue), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onOpenSettings,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(stringResource(R.string.onboarding_open_settings), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
            if (!serviceEnabled) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
        }
    }
}
