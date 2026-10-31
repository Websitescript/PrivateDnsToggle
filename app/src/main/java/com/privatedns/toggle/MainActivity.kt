package com.privatedns.toggle

import android.app.StatusBarManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Teal = Color(0xFF14B8A6)
private const val ADB_CMD = "adb shell pm grant com.privatedns.toggle android.permission.WRITE_SECURE_SETTINGS"

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { Screen(vm) } }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = Teal)
        else -> lightColorScheme(primary = Color(0xFF0E8A7D))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun Screen(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Private DNS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)

            // Status
            Card(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(if (state.isOn) Teal else Color(0xFF9AA3A9)))
                        Spacer(Modifier.width(10.dp))
                        Text("Private DNS is ${state.statusLabel}", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("Mode: ${state.modeLabel}", style = MaterialTheme.typography.bodyMedium)
                    if (state.mode == DnsMode.HOSTNAME) {
                        Text("Provider: ${state.providerLabel}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Button(
                onClick = { PrivateDns.openSettings(ctx) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open Private DNS settings") }

            // Direct toggle (only when the permission is granted)
            if (state.canWrite) {
                Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Quick toggle", style = MaterialTheme.typography.titleMedium)
                            Text("Changes the real system setting", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = state.isOn, onCheckedChange = { vm.toggle() })
                    }
                }
            } else {
                InfoCard(
                    "Direct toggle is locked",
                    "Android does not let normal apps change Private DNS. Until you grant a one-time permission, " +
                        "the tile and widget will open the settings screen instead of switching. Run this once from a PC " +
                        "with USB debugging on (no root needed):\n\n$ADB_CMD"
                ) {
                    OutlinedButton(onClick = {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("adb", ADB_CMD))
                        Toast.makeText(ctx, "Command copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy command") }
                }
            }

            // Tile
            InfoCard(
                "Quick Settings tile",
                "Swipe down twice, tap the pencil / Edit icon, then drag \"Private DNS\" into your active tiles. " +
                    "Long-press the tile to open this screen."
            ) {
                if (Build.VERSION.SDK_INT >= 33) {
                    OutlinedButton(onClick = { requestTile(ctx) }) { Text("Add tile") }
                }
            }

            // Widget
            InfoCard(
                "Home-screen widget",
                "Long-press an empty area of the home screen, tap Widgets, find \"Private DNS\" and drag it onto the screen. " +
                    "Resize it to switch between compact and full layouts."
            )

            // ColorOS / Realme
            InfoCard(
                "Realme / ColorOS tips",
                "Private DNS is under Settings > Connection & sharing > Private DNS. " +
                    "For reliable widget refresh, allow this app to run in the background (Settings > Apps > Private DNS > " +
                    "Battery usage > Allow background activity) and enable auto-start if your ColorOS version has that option."
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, extra: (@Composable () -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            extra?.invoke()
        }
    }
}

private fun requestTile(ctx: Context) {
    if (Build.VERSION.SDK_INT < 33) return
    try {
        val sbm = ctx.getSystemService(StatusBarManager::class.java)
        sbm.requestAddTileService(
            ComponentName(ctx, PrivateDnsTileService::class.java),
            ctx.getString(R.string.tile_label),
            Icon.createWithResource(ctx, R.drawable.ic_dns),
            ctx.mainExecutor
        ) { }
    } catch (_: Exception) {
        Toast.makeText(ctx, "Add it manually from the Quick Settings edit screen", Toast.LENGTH_LONG).show()
    }
}
