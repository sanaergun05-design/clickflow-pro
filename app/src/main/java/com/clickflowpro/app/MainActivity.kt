package com.clickflowpro.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clickflowpro.app.data.ProfileStore
import com.clickflowpro.app.model.ClickPoint
import com.clickflowpro.app.model.ClickProfile
import com.clickflowpro.app.model.MarkerShape
import com.clickflowpro.app.model.MarkerStyle
import com.clickflowpro.app.service.AutoClickAccessibilityService
import com.clickflowpro.app.service.ClickerContract
import com.clickflowpro.app.util.LocaleHelper
import com.clickflowpro.app.util.OnboardingPrefs
import java.util.UUID
import kotlin.math.roundToInt

private val Indigo = Color(0xFF5B5FEF)
private val Teal = Color(0xFF19B6A5)

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AutoClickerViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AutoClickerViewModel(applicationContext) as T
        }
    }

    // Dil tercihinin bu Activity'nin Resources'una uygulanması için gerekli.
    // ClickFlowApp (Application) genel context'i sarmalar, ancak her Activity
    // kendi Resources örneğini attachBaseContext'ten oluşturduğu için burada
    // da aynı sarmalamayı yapıyoruz.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            clickCountReceiver,
            IntentFilter(ClickerContract.ACTION_CLICK_COUNT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        setContent {
            ClickFlowTheme(darkTheme = viewModel.darkTheme) {
                RootScreen(viewModel, onRecreate = { recreate() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAccessibilityStatus()
    }

    override fun onDestroy() {
        unregisterReceiver(clickCountReceiver)
        super.onDestroy()
    }

    private val clickCountReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.updateTotalClicks(intent?.getLongExtra(ClickerContract.EXTRA_TOTAL_CLICKS, 0L) ?: 0L)
        }
    }
}

@Composable
fun ClickFlowTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF9EA1FF),
            secondary = Color(0xFF63D7C7),
            background = Color(0xFF0F111A),
            surface = Color(0xFF171A25),
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = Indigo,
            secondary = Teal,
            background = Color(0xFFF8F8FC),
            surface = Color.White,
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/**
 * Uygulamanın ilk ekranı: ilk açılışta (veya izin verilene kadar) tam ekran
 * OnboardingScreen gösterilir; izin verildikten/atlandıktan sonra ana
 * AutoClickerApp ekranına geçilir.
 */
@Composable
fun RootScreen(viewModel: AutoClickerViewModel, onRecreate: () -> Unit) {
    if (viewModel.showOnboarding) {
        OnboardingScreen(
            serviceEnabled = viewModel.serviceEnabled,
            onOpenSettings = viewModel::openAccessibilitySettings,
            onContinue = viewModel::finishOnboarding,
            onSkip = viewModel::finishOnboarding,
        )
    } else {
        AutoClickerApp(viewModel, onRecreate)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoClickerApp(viewModel: AutoClickerViewModel, onRecreate: () -> Unit) {
    var showProfiles by remember { mutableStateOf(false) }
    var showPointEditor by remember { mutableStateOf(false) }
    var showMarkerSettings by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val profile = viewModel.selectedProfile

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLanguageDialog = true }) {
                        Icon(
                            Icons.Outlined.Language,
                            contentDescription = stringResource(R.string.action_language),
                        )
                    }
                    IconButton(onClick = { viewModel.setTheme(!viewModel.darkTheme) }) {
                        Icon(
                            if (viewModel.darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = stringResource(R.string.action_toggle_theme),
                        )
                    }
                    IconButton(onClick = { showMarkerSettings = true }) {
                        Icon(Icons.Outlined.Palette, contentDescription = stringResource(R.string.action_marker_settings))
                    }
                    IconButton(onClick = { showProfiles = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.action_profiles))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp,
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AnimatedVisibility(visible = !viewModel.serviceEnabled) {
                    AccessibilityCard(viewModel::openAccessibilitySettings)
                }
            }
            item { ProfilePicker(profile.name) { showProfiles = true } }
            item {
                ControlCard(
                    profile = profile,
                    isRunning = viewModel.isRunning,
                    enabled = viewModel.serviceEnabled,
                    onStart = viewModel::start,
                    onStop = viewModel::stop,
                )
            }
            item {
                SpeedCard(
                    cps = profile.cps,
                    intervalMs = profile.intervalMs,
                    onCpsChange = viewModel::setCps,
                    onIntervalChange = viewModel::setInterval,
                )
            }
            item {
                PointsCard(
                    points = profile.points,
                    totalClicks = viewModel.totalClicks,
                    onAdd = viewModel::addPoint,
                    onEdit = { showPointEditor = true },
                )
            }
            item {
                Text(
                    stringResource(R.string.tip_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }

    if (showProfiles) {
        ProfilesDialog(
            profiles = viewModel.profiles,
            selectedId = profile.id,
            onSelect = { viewModel.selectProfile(it); showProfiles = false },
            onCreate = viewModel::createProfile,
            onDismiss = { showProfiles = false },
        )
    }
    if (showPointEditor) {
        PointEditorDialog(
            points = profile.points,
            onPointChange = viewModel::updatePoint,
            onDelete = viewModel::deletePoint,
            onPickOnScreen = { point ->
                // Sürükleme sırasında overlay'in rahat görünmesi için diyaloğu kapatıyoruz,
                // kullanıcı onaylayınca (veya iptal edince) diyalog tekrar açılır.
                showPointEditor = false
                viewModel.pickPointOnScreen(point) {
                    showPointEditor = true
                }
            },
            onDismiss = { showPointEditor = false },
        )
    }
    if (showMarkerSettings) {
        MarkerSettingsDialog(
            style = viewModel.markerStyle,
            onShapeChange = viewModel::setMarkerShape,
            onColorChange = viewModel::setMarkerColor,
            onDismiss = { showMarkerSettings = false },
        )
    }
    if (showLanguageDialog) {
        LanguageDialog(
            onSelect = { language ->
                viewModel.setLanguage(language)
                showLanguageDialog = false
                onRecreate()
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

@Composable
private fun LanguageDialog(onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onSelect(LocaleHelper.LANGUAGE_ENGLISH) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.language_english))
                }
                OutlinedButton(
                    onClick = { onSelect(LocaleHelper.LANGUAGE_TURKISH) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.language_turkish))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done_button)) } },
    )
}

@Composable
private fun AccessibilityCard(onOpenSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                val appName = stringResource(R.string.app_name)
                Text(stringResource(R.string.accessibility_card_title), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.accessibility_card_desc, appName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_enable)) }
        }
    }
}

@Composable
private fun ProfilePicker(name: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.action_profiles), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ControlCard(
    profile: ClickProfile,
    isRunning: Boolean,
    enabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) Teal.copy(alpha = .16f) else Indigo.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = if (isRunning) Teal else Indigo,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isRunning) stringResource(R.string.status_running) else stringResource(R.string.status_ready),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${profile.cps.cleanNumber()} CPS · ${profile.points.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isRunning) LinearProgressIndicator(Modifier.width(44.dp), color = Teal)
            }
            Button(
                onClick = if (isRunning) onStop else onStart,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Teal else Indigo,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Icon(
                    if (isRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isRunning) stringResource(R.string.stop_clicking) else stringResource(R.string.start_clicking),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SpeedCard(
    cps: Float,
    intervalMs: Long,
    onCpsChange: (Float) -> Unit,
    onIntervalChange: (Long) -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.click_speed), fontWeight = FontWeight.Bold)
                Text("${cps.cleanNumber()} CPS", color = Indigo, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = cps,
                onValueChange = { onCpsChange(it.roundToInt().toFloat()) },
                valueRange = 1f..50f,
                steps = 48,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1 CPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("50 CPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.interval_label), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.interval_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = intervalMs.toString(),
                    onValueChange = { value ->
                        value.toLongOrNull()?.coerceIn(20L, 1000L)?.let(onIntervalChange)
                    },
                    suffix = { Text("ms") },
                    singleLine = true,
                    modifier = Modifier.width(128.dp),
                )
            }
        }
    }
}

@Composable
private fun PointsCard(
    points: List<ClickPoint>,
    totalClicks: Long,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tap_points_title), fontWeight = FontWeight.Bold)
                    Text(
                        pluralStringResource(R.plurals.points_active, points.size, points.size, totalClicks),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_point_desc))
                }
                TextButton(onClick = onEdit) { Text(stringResource(R.string.edit_button)) }
            }
            PointPreview(points)
            points.forEachIndexed { index, point ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(10.dp)
                            .clip(CircleShape)
                            .background(if (index % 2 == 0) Indigo else Teal),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(point.label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text(
                        "${point.x}, ${point.y}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PointPreview(points: List<ClickPoint>) {
    val maxX = (points.maxOfOrNull { it.x } ?: 1080).coerceAtLeast(1080)
    val maxY = (points.maxOfOrNull { it.y } ?: 1920).coerceAtLeast(1920)
    val outlineColor = MaterialTheme.colorScheme.outline
    Canvas(
        Modifier.fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val scaleX = size.width / maxX
        val scaleY = size.height / maxY
        drawRoundRect(
            color = outlineColor.copy(alpha = .18f),
            style = Stroke(width = 2f),
            cornerRadius = CornerRadius(18f),
        )
        points.forEachIndexed { index, point ->
            val center = Offset(point.x * scaleX, point.y * scaleY)
            val color = if (index % 2 == 0) Indigo else Teal
            drawCircle(color.copy(alpha = .16f), 22f, center)
            drawCircle(color, 8f, center)
            drawCircle(Color.White, 3f, center)
        }
    }
}

@Composable
private fun ProfilesDialog(
    profiles: List<ClickProfile>,
    selectedId: String,
    onSelect: (ClickProfile) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profiles_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.forEach { profile ->
                    OutlinedButton(
                        onClick = { onSelect(profile) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            if (profile.id == selectedId) Icons.Outlined.CheckCircle else Icons.Outlined.TouchApp,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(profile.name, Modifier.weight(1f))
                        Text(
                            stringResource(R.string.points_count_short, profile.points.size),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                TextButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.create_profile))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done_button)) } },
    )
}

@Composable
private fun MarkerSettingsDialog(
    style: MarkerStyle,
    onShapeChange: (MarkerShape) -> Unit,
    onColorChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.marker_settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.marker_settings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    Modifier.fillMaxWidth()
                        .height(70.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    MarkerPreview(style)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.marker_shape_label), fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MarkerShape.values().forEach { shape ->
                            val selected = shape == style.shape
                            OutlinedButton(
                                onClick = { onShapeChange(shape) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = if (selected) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                },
                            ) {
                                Text(shape.label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.marker_color_label), fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MarkerStyle.PRESET_COLORS.forEach { colorArgb ->
                            val selected = colorArgb == style.colorArgb
                            Box(
                                Modifier.size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorArgb))
                                    .then(
                                        if (selected) {
                                            Modifier.padding(2.dp)
                                        } else {
                                            Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                IconButton(onClick = { onColorChange(colorArgb) }, modifier = Modifier.fillMaxSize()) {
                                    if (selected) {
                                        Icon(
                                            Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = if (colorArgb == 0xFFFFFFFF.toInt()) Color.Black else Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done_button)) } },
    )
}

@Composable
private fun MarkerPreview(style: MarkerStyle) {
    val color = Color(style.colorArgb)
    Canvas(Modifier.size(48.dp)) {
        val r = size.minDimension / 2f - 6f
        val center = Offset(size.width / 2f, size.height / 2f)
        when (style.shape) {
            MarkerShape.RING -> drawCircle(color, r, center, style = Stroke(width = 6f))
            MarkerShape.DOT -> drawCircle(color, r, center)
            MarkerShape.SQUARE -> drawRoundRect(
                color = color,
                topLeft = Offset(center.x - r, center.y - r),
                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                style = Stroke(width = 6f),
            )
            MarkerShape.DIAMOND -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x, center.y - r)
                    lineTo(center.x + r, center.y)
                    lineTo(center.x, center.y + r)
                    lineTo(center.x - r, center.y)
                    close()
                }
                drawPath(path, color, style = Stroke(width = 6f))
            }
            MarkerShape.CROSS -> {
                drawLine(color, Offset(center.x - r, center.y), Offset(center.x + r, center.y), strokeWidth = 6f)
                drawLine(color, Offset(center.x, center.y - r), Offset(center.x, center.y + r), strokeWidth = 6f)
            }
        }
    }
}

@Composable
private fun PointEditorDialog(
    points: List<ClickPoint>,
    onPointChange: (ClickPoint) -> Unit,
    onDelete: (ClickPoint) -> Unit,
    onPickOnScreen: (ClickPoint) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_tap_points_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.edit_tap_points_desc),
                    style = MaterialTheme.typography.bodySmall,
                )
                points.forEach { point ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(point.label, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = point.x.toString(),
                                    onValueChange = { value ->
                                        value.toIntOrNull()?.let { x ->
                                            onPointChange(point.copy(x = x.coerceAtLeast(0)))
                                        }
                                    },
                                    label = { Text(stringResource(R.string.x_label)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = point.y.toString(),
                                    onValueChange = { value ->
                                        value.toIntOrNull()?.let { y ->
                                            onPointChange(point.copy(y = y.coerceAtLeast(0)))
                                        }
                                    },
                                    label = { Text(stringResource(R.string.y_label)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        IconButton(onClick = { onPickOnScreen(point) }) {
                            Icon(Icons.Outlined.TouchApp, contentDescription = stringResource(R.string.pick_on_screen_desc))
                        }
                        IconButton(onClick = { onDelete(point) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.delete_point_desc))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done_button)) } },
    )
}

private fun Float.cleanNumber(): String =
    if (this % 1f == 0f) roundToInt().toString() else "%.1f".format(this)

class AutoClickerViewModel(context: Context) : ViewModel() {
    private val store = ProfileStore(context)
    private val appContext = context.applicationContext
    var profiles by mutableStateOf(store.loadProfiles())
        private set
    var selectedProfile by mutableStateOf(profiles.first())
        private set
    var darkTheme by mutableStateOf(store.isDarkTheme())
        private set
    var serviceEnabled by mutableStateOf(false)
        private set
    var isRunning by mutableStateOf(false)
        private set
    var totalClicks by mutableStateOf(0L)
        private set
    var markerStyle by mutableStateOf(store.loadMarkerStyle())
        private set
    var showOnboarding by mutableStateOf(!OnboardingPrefs.isOnboardingDone(appContext))
        private set

    fun refreshAccessibilityStatus() {
        val manager = appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        serviceEnabled = manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == appContext.packageName }
        // Kullanıcı ayarlardan izni verip uygulamaya geri döndüğünde ilk açılış
        // ekranını otomatik olarak kapatıp ana ekrana geçiyoruz.
        if (serviceEnabled && showOnboarding) {
            finishOnboarding()
        }
    }

    fun finishOnboarding() {
        OnboardingPrefs.setOnboardingDone(appContext)
        showOnboarding = false
    }

    fun setLanguage(language: String) {
        LocaleHelper.setLanguage(appContext, language)
    }

    fun openAccessibilitySettings() {
        appContext.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun setTheme(dark: Boolean) {
        darkTheme = dark
        store.setDarkTheme(dark)
    }

    fun setMarkerShape(shape: com.clickflowpro.app.model.MarkerShape) {
        markerStyle = markerStyle.copy(shape = shape)
        store.saveMarkerStyle(markerStyle)
        // Servis o an calisiyorsa yeni sekli aninda uygula - yeniden baslatmaya gerek yok.
        AutoClickAccessibilityService.instance?.updateMarkerStyle(markerStyle)
    }

    fun setMarkerColor(colorArgb: Int) {
        markerStyle = markerStyle.copy(colorArgb = colorArgb)
        store.saveMarkerStyle(markerStyle)
        AutoClickAccessibilityService.instance?.updateMarkerStyle(markerStyle)
    }

    fun updateProfile(profile: ClickProfile) {
        selectedProfile = profile
        profiles = profiles.map { if (it.id == profile.id) profile else it }
        store.saveProfiles(profiles)
    }

    fun setCps(cps: Float) {
        val safeCps = cps.coerceIn(1f, 50f)
        updateProfile(selectedProfile.copy(cps = safeCps, intervalMs = (1000f / safeCps).roundToInt().toLong()))
    }

    fun setInterval(interval: Long) {
        val safeInterval = interval.coerceIn(20L, 1000L)
        updateProfile(selectedProfile.copy(intervalMs = safeInterval, cps = (1000f / safeInterval).coerceIn(1f, 50f)))
    }

    fun addPoint() {
        // ONEMLI: ClickPoint()'un varsayilan x/y degeri her zaman ekran merkezi
        // (540, 960). Onceden yeni eklenen her nokta tam olarak ayni pikselin
        // ustune biniyordu - "2 nokta" yazsa da onizlemede tek nokta gorunuyor
        // ve bot aslinda ayni yere iki kere dokunuyordu. Her yeni noktayi
        // merkezden koseglemesine kaydiriyoruz ki hem gorunur hem de fonksiyonel
        // olarak farkli bir yer olsun (kullanici yine de Edit > surukle ile
        // tam istedigi yere tasiyabilir).
        val index = selectedProfile.points.size
        val step = 120
        val offsetX = (index * step) % 480 - 240
        val offsetY = (index * step / 480) * step
        val newPoint = ClickPoint(
            x = (540 + offsetX).coerceIn(40, 1040),
            y = (960 + offsetY).coerceIn(40, 1880),
            label = "Point ${index + 1}",
        )
        updateProfile(selectedProfile.copy(points = selectedProfile.points + newPoint))
    }

    fun updatePoint(point: ClickPoint) {
        updateProfile(selectedProfile.copy(points = selectedProfile.points.map {
            if (it.id == point.id) point else it
        }))
    }

    fun deletePoint(point: ClickPoint) {
        if (selectedProfile.points.size > 1) {
            updateProfile(selectedProfile.copy(points = selectedProfile.points.filterNot { it.id == point.id }))
        }
    }

    fun createProfile() {
        val profile = ClickProfile(UUID.randomUUID().toString(), "Profile ${profiles.size + 1}")
        profiles = profiles + profile
        selectedProfile = profile
        store.saveProfiles(profiles)
    }

    fun selectProfile(profile: ClickProfile) {
        selectedProfile = profile
    }

    fun start() {
        if (!serviceEnabled) return
        // NOT: Servise startService()/Intent ile komut GÖNDERMİYORUZ artık.
        // Servis BIND_ACCESSIBILITY_SERVICE izni istiyor ve bu izin hiçbir
        // uygulamaya verilmez; Intent ile çağrı SecurityException'a sebep olurdu.
        // Bunun yerine servisin çalışan örneğine (companion object) doğrudan erişiyoruz.
        val service = AutoClickAccessibilityService.instance ?: return
        totalClicks = 0L
        service.applyAndStart(selectedProfile.points, selectedProfile.intervalMs)
        isRunning = true
    }

    fun updateTotalClicks(count: Long) {
        totalClicks = count
    }

    fun stop() {
        AutoClickAccessibilityService.instance?.requestStop()
        isRunning = false
    }

    /**
     * Ekranda sürüklenebilir bir nişangah gösterip kullanıcının parmağıyla
     * tıklama noktasını seçmesini sağlar. Servis çalışmıyorsa (erişilebilirlik
     * kapalıysa) hiçbir şey yapmaz.
     */
    fun pickPointOnScreen(point: ClickPoint, onFinished: () -> Unit) {
        val service = AutoClickAccessibilityService.instance
        if (service == null) {
            onFinished()
            return
        }
        service.startPointPicker(
            initialX = point.x,
            initialY = point.y,
            onPicked = { x, y ->
                updatePoint(point.copy(x = x, y = y))
                onFinished()
            },
            onCancelled = { onFinished() },
        )
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
