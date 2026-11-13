package com.zinerom.info

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.work.*
import coil.compose.AsyncImage
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

private const val DEFAULT_TELEGRAM = "https://t.me/ZineROMProject/13/4542"
private const val DEFAULT_WEBSITE = "https://zinefather.github.io/ZineROM-Project-Site/"
private const val NOTIFICATION_CHANNEL = "zinerom_alerts_v2"
private const val CURRENT_APP_VERSION = "3.2.5"
private const val DEFAULT_UPDATE_JSON =
    "https://raw.githubusercontent.com/ZineFather/ZineROM-Project-Site/zinerom-info-development/remote/update.json"
private val httpClient = OkHttpClient()

data class AppConfig(
    val websiteUrl: String,
    val releasesUrl: String,
    val updateJsonUrl: String,
    val telegramUrl: String,
    val changelogUrl: String
)
data class AppUpdate(
    val version: String,
    val downloadUrl: String?,
    val changelogUrl: String?,
    val notes: String?
)
data class UpdateCheckResult(val update: AppUpdate?, val hasTargetRules: Boolean)
data class Rom(val title: String, val version: String, val downloadUrl: String)
data class Device(
    val id: String, val name: String, val model: String?, val versions: List<String>,
    val imageUrl: String?, val description: String?, val downloadUrl: String?, val roms: List<Rom>
)
data class Release(val title: String, val version: String, val deviceId: String, val downloadUrl: String?)

private val Blue = Color(0xFF85BDFF)
private val Background = Color(0xFF08111F)
private val SurfaceDark = Color(0xFF14243A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createUpdateChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 325)
        }
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            FirebaseMessaging.getInstance().subscribeToTopic("zinerom_all")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.util.Log.i("ZineFCM", "Subscribed to zinerom_all")
                    } else {
                        android.util.Log.e("ZineFCM", "Failed to subscribe to zinerom_all", task.exception)
                    }
                }
        } else {
            android.util.Log.e("ZineFCM", "FirebaseApp is not initialized")
        }
        setContent { ZineApp() }
    }
}

private fun createUpdateChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL, "ZineROM Alerts", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important ZineROM announcements and update alerts"
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

private fun versionIsNewer(remote: String, current: String): Boolean {
    val a = remote.trim().removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val b = current.trim().removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    for (index in 0 until maxOf(a.size, b.size)) {
        val left = a.getOrElse(index) { 0 }
        val right = b.getOrElse(index) { 0 }
        if (left != right) return left > right
    }
    return false
}

private fun readStringSet(json: JSONObject, vararg keys: String): Set<String> {
    val values = mutableSetOf<String>()
    keys.forEach { key ->
        when (val value = json.opt(key)) {
            is org.json.JSONArray -> for (i in 0 until value.length()) {
                value.optString(i).trim().takeIf { it.isNotEmpty() }?.let { values.add(it.uppercase()) }
            }
            is String -> value.split(",", ";").map { it.trim() }
                .filter { it.isNotEmpty() }.forEach { values.add(it.uppercase()) }
        }
    }
    return values
}

private fun matchesThisDevice(target: JSONObject): Boolean {
    val codenames = readStringSet(target, "device_codenames", "codenames", "device_codename")
    val models = readStringSet(target, "models", "model")
    val localCodenames = setOf(Build.DEVICE, Build.PRODUCT, Build.BOARD)
        .map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()
    val localModels = setOf(Build.MODEL, Build.MANUFACTURER + " " + Build.MODEL)
        .map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()
    return (codenames.isNotEmpty() && codenames.any { it in localCodenames }) ||
        (models.isNotEmpty() && models.any { wanted ->
            localModels.any { actual -> actual == wanted || actual.startsWith(wanted) }
        })
}

private fun parseAppUpdate(json: JSONObject): AppUpdate {
    val version = json.optString("version", json.optString("latest_app_version", ""))
    val download = json.optString("download_url").takeIf { it.startsWith("https://") }
    val changelog = json.optString("changelog_url").takeIf { it.startsWith("https://") }
    return AppUpdate(version, download, changelog, json.optString("notes").takeIf { it.isNotBlank() })
}

private fun fetchAppUpdate(updateJsonUrl: String): UpdateCheckResult {
    val request = Request.Builder().url(updateJsonUrl).header("Cache-Control", "no-cache").build()
    val root = httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IllegalStateException("Update server HTTP " + response.code)
        JSONObject(response.body?.string().orEmpty())
    }
    val targets = root.optJSONArray("targets")
    if (targets != null && targets.length() > 0) {
        for (index in 0 until targets.length()) {
            val target = targets.optJSONObject(index) ?: continue
            if (matchesThisDevice(target)) {
                val merged = JSONObject(root.toString())
                listOf("version", "latest_app_version", "download_url", "changelog_url", "notes").forEach { key ->
                    if (!target.has(key) && root.has(key)) merged.put(key, root.opt(key))
                }
                target.keys().forEach { key -> merged.put(key, target.opt(key)) }
                return UpdateCheckResult(parseAppUpdate(merged), true)
            }
        }
        return UpdateCheckResult(null, true)
    }
    return UpdateCheckResult(parseAppUpdate(root), false)
}

private fun openLink(context: Context, url: String?) {
    if (!url.isNullOrBlank() && url.startsWith("https://")) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun readConfig(context: Context): AppConfig {
    val json = JSONObject(context.assets.open("app-config.json").bufferedReader().use { it.readText() })
    return AppConfig(
        websiteUrl = json.optString("website_url"),
        releasesUrl = json.optString("releases_url"),
        updateJsonUrl = json.optString("update_json_url", DEFAULT_UPDATE_JSON).ifBlank { DEFAULT_UPDATE_JSON },
        telegramUrl = json.optString("telegram_url", DEFAULT_TELEGRAM),
        changelogUrl = json.optString("changelog_url", DEFAULT_TELEGRAM)
    )
}

private fun readDevices(context: Context): List<Device> {
    val root = JSONObject(context.assets.open("devices.json").bufferedReader().use { it.readText() })
    val array = root.getJSONArray("devices")
    return (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        val versions = item.optJSONArray("versions")
        val romArray = item.optJSONArray("roms")
        val roms = if (romArray == null) emptyList() else (0 until romArray.length()).map { n ->
            val entry = romArray.getJSONObject(n)
            Rom(entry.optString("title"), entry.optString("version"), entry.optString("download_url"))
        }
        Device(
            id = item.getString("id"),
            name = item.getString("name"),
            model = item.optString("model").takeIf { it.isNotBlank() && it != "null" },
            versions = if (versions == null) emptyList() else (0 until versions.length()).map { versions.getString(it) },
            imageUrl = item.optString("image_url").takeIf { it.startsWith("https://") },
            description = item.optString("description"),
            downloadUrl = item.optString("download_url").takeIf { it.startsWith("https://") },
            roms = roms
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private data class MainTab(val route: String, val label: String, val arabicLabel: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZineApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    var config by remember { mutableStateOf<AppConfig?>(null) }
    var devices by remember { mutableStateOf(emptyList<Device>()) }
    var selectedDevice by remember { mutableStateOf<Device?>(null) }
    var arabic by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready") }
    var latestVersion by remember { mutableStateOf("") }
    var releases by remember { mutableStateOf(emptyList<Release>()) }

    val tabs = listOf(
        MainTab("home", "Home", "الرئيسية", Icons.Default.Home),
        MainTab("devices", "Devices", "الأجهزة", Icons.Default.PhoneAndroid),
        MainTab("updates", "Updates", "التحديثات", Icons.Default.SystemUpdate),
        MainTab("settings", "Settings", "الإعدادات", Icons.Default.Settings)
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            config = readConfig(context)
            devices = readDevices(context)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Blue, secondary = Color(0xFF8CE6F0),
        background = Color(0xFF070B12), surface = Color(0xFF101927)
    )) {
        CompositionLocalProvider(LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Scaffold(
                containerColor = Color(0xFF070B12),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(listOf(Blue.copy(alpha = .28f), Color(0xFF8CE6F0).copy(alpha = .10f))))
                                        .border(1.dp, Blue.copy(alpha = .35f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) { Text("Z", color = Blue, fontWeight = FontWeight.Black, fontSize = 23.sp) }
                                Column {
                                    Text("ZineROM", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text("PROJECT COMPANION", color = Color(0xFF9AAABD), fontSize = 8.sp, letterSpacing = 1.5.sp)
                                }
                            }
                        },
                        actions = {
                            TextButton(onClick = { arabic = !arabic }) {
                                Text(if (arabic) "EN" else "عربي", color = Blue, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF070B12))
                    )
                },
                bottomBar = {
                    ZineBottomBar(
                        tabs = tabs,
                        selectedRoute = currentRoute,
                        arabic = arabic,
                        onSelect = { route ->
                            selectedDevice = null
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    composable("home") {
                        HomeScreen(
                            devices, arabic, Modifier.fillMaxSize(),
                            onBrowse = { navController.navigate("devices") },
                            onUpdates = { navController.navigate("updates") },
                            onDevice = { selectedDevice = it; navController.navigate("devices") }
                        )
                    }
                    composable("devices") {
                        if (selectedDevice != null) {
                            DeviceDetails(
                                device = selectedDevice!!,
                                arabic = arabic,
                                modifier = Modifier.fillMaxSize(),
                                onBack = { selectedDevice = null },
                                onDownload = { openLink(context, it) }
                            )
                        } else {
                            DevicesScreen(
                                devices, arabic, Modifier.fillMaxSize(),
                                onDevice = { selectedDevice = it }
                            )
                        }
                    }
                    composable("updates") {
                        UpdatesScreen(
                            Modifier.fillMaxSize(), arabic, checking, status,
                            latestVersion, releases,
                            onCheck = {
                                checking = true
                                status = if (arabic) "جارٍ التحقق..." else "Checking for updates..."
                                scope.launch {
                                    try {
                                        val source = config ?: readConfig(context)
                                        val result = withContext(Dispatchers.IO) {
                                            fetchAppUpdate(source.updateJsonUrl)
                                        }
                                        val update = result.update
                                        latestVersion = update?.version?.takeIf { it.isNotBlank() } ?: CURRENT_APP_VERSION
                                        val hasUpdate = update != null && versionIsNewer(update.version, CURRENT_APP_VERSION)

                                        releases = try {
                                            withContext(Dispatchers.IO) {
                                                val request = Request.Builder().url(source.releasesUrl).build()
                                                httpClient.newCall(request).execute().use { response ->
                                                    if (!response.isSuccessful) throw IllegalStateException("HTTP " + response.code)
                                                    val feed = JSONObject(response.body?.string().orEmpty())
                                                    val entries = feed.optJSONArray("releases") ?: org.json.JSONArray()
                                                    (0 until entries.length()).map { i ->
                                                        val entry = entries.getJSONObject(i)
                                                        Release(
                                                            entry.optString("title", "ZineROM"),
                                                            entry.optString("version"),
                                                            entry.optString("device_id"),
                                                            entry.optString("download_url").takeIf { it.startsWith("https://") }
                                                        )
                                                    }
                                                }
                                            }
                                        } catch (_: Exception) { emptyList() }

                                        status = when {
                                            result.hasTargetRules && update == null ->
                                                if (arabic) "لا يوجد تحديث مخصص لهذا الجهاز" else "No update targeted to this device"
                                            hasUpdate ->
                                                if (arabic) "يتوفر تحديث جديد: $latestVersion — افتح الموقع للتفاصيل"
                                                else "Update available: $latestVersion — open the website for details"
                                            else ->
                                                if (arabic) "أنت على أحدث إصدار: $CURRENT_APP_VERSION"
                                                else "You're up to date: $CURRENT_APP_VERSION"
                                        }
                                    } catch (error: Exception) {
                                        status = (if (arabic) "فشل التحقق: " else "Check failed: ") +
                                            (error.localizedMessage ?: "Network error")
                                    } finally { checking = false }
                                }
                            },
                            onChangelog = { openLink(context, config?.websiteUrl ?: DEFAULT_WEBSITE) },
                            onDownload = { openLink(context, it) }
                        )
                    }
                    composable("settings") {
                        SettingsAboutScreen(
                            arabic = arabic,
                            onLanguage = { arabic = !arabic },
                            onNotificationSettings = {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, NOTIFICATION_CHANNEL)
                                    }
                                } else {
                                    Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                                        android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName
                                    )
                                }
                                context.startActivity(intent)
                            },
                            onWebsite = { openLink(context, config?.websiteUrl ?: DEFAULT_WEBSITE) },
                            onTelegram = { openLink(context, config?.telegramUrl ?: DEFAULT_TELEGRAM) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZineBottomBar(
    tabs: List<MainTab>,
    selectedRoute: String,
    arabic: Boolean,
    onSelect: (String) -> Unit
) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0xF0203045), Color(0xF0101928))))
                .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(26.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val active = selectedRoute == tab.route
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                        .background(if (active) Blue.copy(alpha = .16f) else Color.Transparent)
                        .clickable { onSelect(tab.route) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (active) Box(
                            Modifier.size(width = 24.dp, height = 3.dp).align(Alignment.TopCenter)
                                .offset(y = (-7).dp).clip(RoundedCornerShape(3.dp)).background(Blue)
                        )
                        Icon(
                            tab.icon,
                            contentDescription = if (arabic) tab.arabicLabel else tab.label,
                            tint = if (active) Blue else Color(0xFF91A2B8),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Text(
                        if (arabic) tab.arabicLabel else tab.label,
                        color = if (active) Color.White else Color(0xFF91A2B8),
                        fontSize = 9.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsAboutScreen(
    arabic: Boolean,
    onLanguage: () -> Unit,
    onNotificationSettings: () -> Unit,
    onWebsite: () -> Unit,
    onTelegram: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(if (arabic) "الإعدادات" else "Settings", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(
                if (arabic) "تحكم في تجربة التطبيق والإشعارات." else "Manage your app experience and notification behavior.",
                color = Color(0xFF9AAABD), fontSize = 12.sp
            )
        }
        item {
            GlassPanel {
                Text(if (arabic) "التفضيلات" else "PREFERENCES", color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onLanguage),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = Blue)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (arabic) "لغة التطبيق" else "App language", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(if (arabic) "العربية / English" else "English / Arabic", color = Color(0xFF9AAABD), fontSize = 11.sp)
                    }
                    Text(if (arabic) "تغيير" else "Change", color = Blue, fontSize = 12.sp)
                }
                HorizontalDivider(color = Color.White.copy(alpha = .08f))
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onNotificationSettings),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Blue)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (arabic) "إعدادات ظهور الإشعارات" else "Notification pop-up settings", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(if (arabic) "الصوت والنوافذ المنبثقة وقناة التنبيهات" else "Sound, pop-up banners and alert behavior", color = Color(0xFF9AAABD), fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Blue)
                }
            }
        }
        item {
            GlassPanel {
                Text(if (arabic) "عن التطبيق" else "ABOUT ZINEROM", color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("ZineROM Info", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Version $CURRENT_APP_VERSION", color = Color(0xFF9AAABD), fontSize = 12.sp)
                OutlinedButton(onClick = onWebsite, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Language, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (arabic) "الموقع الرسمي" else "Official website")
                }
                OutlinedButton(onClick = onTelegram, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (arabic) "قناة تيليجرام" else "Telegram channel")
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    devices: List<Device>, arabic: Boolean, modifier: Modifier,
    onBrowse: () -> Unit, onUpdates: () -> Unit, onDevice: (Device) -> Unit
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (arabic) "مشروعك. أجهزتك. روماتك." else "YOUR DEVICE. YOUR ROM.", color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(if (arabic) "اكتشف تجربة ZineROM" else "Explore the ZineROM experience", color = Color.White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                Text(if (arabic) "إصدارات الرومات ومعلومات الأجهزة والتحديثات في مكان واحد."
                    else "ROM releases, device information and project updates — all in one place.",
                    color = Color(0xFF9AAABD), fontSize = 13.sp, lineHeight = 21.sp)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(27.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A3552), Color(0xFF101C2B), Color(0xFF172A3B))))
                    .border(1.dp, Blue.copy(alpha = .25f), RoundedCornerShape(27.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ZINEROM INFO", color = Color(0xFF8CE6F0), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(if (arabic) "مركز المشروع" else "Project hub", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Blue.copy(alpha = .14f)).border(1.dp, Blue.copy(alpha = .3f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                        Text("Z", color = Blue, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    StatPill(devices.size.toString(), if (arabic) "جهاز" else "DEVICES")
                    StatPill(CURRENT_APP_VERSION, if (arabic) "الإصدار" else "VERSION")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onBrowse, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color(0xFF07111D))) {
                        Text(if (arabic) "تصفح الأجهزة" else "Explore devices", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onUpdates, shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .22f))) {
                        Text(if (arabic) "التحديثات" else "Updates", color = Color.White)
                    }
                }
            }
        }
        item { SectionHeading(if (arabic) "أجهزة مميزة" else "Featured devices", if (arabic) "افتح الجهاز لعرض إصدارات الروم" else "Open a device to view its ROM builds") }
        items(devices.take(4), key = { it.id }, contentType = { "device" }) { device ->
            DeviceCard(device, arabic) { onDevice(device) }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .07f))
            .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = Color(0xFF9AAABD), fontSize = 8.sp, letterSpacing = .7.sp)
    }
}

@Composable
private fun DevicesScreen(devices: List<Device>, arabic: Boolean, modifier: Modifier, onDevice: (Device) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(devices, query) {
        devices.filter { it.name.contains(query, true) || (it.model?.contains(query, true) == true) }
    }
    LazyColumn(
        modifier, contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(if (arabic) "كتالوج الأجهزة" else "Device catalog", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(if (arabic) "ابحث عن جهازك وافتح تفاصيل الروم." else "Find your device and explore its ROM details.", color = Color(0xFF9AAABD), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF101927))
                    .border(1.dp, Color.White.copy(alpha = .09f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF9AAABD))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text(if (arabic) "ابحث بالاسم أو رقم الموديل" else "Search device or model", color = Color(0xFF9AAABD), fontSize = 13.sp)
                        inner()
                    }
                )
            }
            Text(filtered.size.toString() + (if (arabic) " أجهزة" else " devices"), color = Blue, fontSize = 11.sp, modifier = Modifier.padding(top = 9.dp))
        }
        items(filtered, key = { it.id }, contentType = { "device" }) { device -> DeviceCard(device, arabic) { onDevice(device) } }
        if (filtered.isEmpty()) item { Text(if (arabic) "لا توجد أجهزة مطابقة." else "No matching devices.", color = Color(0xFF9AAABD), modifier = Modifier.padding(vertical = 24.dp)) }
    }
}

@Composable
private fun DeviceCard(device: Device, arabic: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF17263A), Color(0xFF101927))))
            .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(94.dp, 104.dp).clip(RoundedCornerShape(17.dp)).background(Color(0xFF1C3047)), contentAlignment = Alignment.Center) {
            AsyncImage(device.imageUrl, device.name, Modifier.fillMaxSize().padding(5.dp), contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(device.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(device.model ?: "Model not specified", color = Color(0xFF9AAABD), fontSize = 10.sp)
            Text(if (device.versions.isEmpty()) {
                if (arabic) "تفاصيل الدعم على الموقع" else "Check support details"
            } else "One UI " + device.versions.joinToString(" · "), color = Blue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(if (arabic) "عرض التفاصيل" else "View details", color = Color(0xFF9AAABD), fontSize = 10.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Blue)
    }
}

@Composable
private fun UpdatesScreen(
    modifier: Modifier, arabic: Boolean, checking: Boolean, status: String,
    latestVersion: String, releases: List<Release>, onCheck: () -> Unit,
    onChangelog: () -> Unit, onDownload: (String?) -> Unit
) {
    LazyColumn(modifier, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(if (arabic) "مركز التحديثات" else "Update center", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(if (arabic) "تحديثات التطبيق وإصدارات الرومات." else "App updates and ROM releases.", color = Color(0xFF9AAABD), fontSize = 12.sp)
        }
        item {
            GlassPanel {
                Text(if (arabic) "إصدار التطبيق الحالي" else "CURRENT APP VERSION", color = Color(0xFF9AAABD), fontSize = 9.sp, letterSpacing = 1.5.sp)
                Text(CURRENT_APP_VERSION, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onCheck, enabled = !checking, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color(0xFF07111D))) {
                    Icon(if (checking) Icons.Default.Sync else Icons.Default.SystemUpdate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (checking) (if (arabic) "جارٍ الفحص..." else "Checking...") else (if (arabic) "فحص التحديثات" else "Check for updates"), fontWeight = FontWeight.Bold)
                }
                Text(status, color = if (status.contains("failed", true) || status.contains("فشل")) Color(0xFFFFA6A6) else Color(0xFF8CE6F0), fontSize = 12.sp)
                if (latestVersion.isNotBlank()) Text(if (arabic) "الإصدار المستهدف: " + latestVersion else "Server version: " + latestVersion, color = Color(0xFF9AAABD), fontSize = 11.sp)

            }
        }
        item {
            GlassPanel {
                Text(if (arabic) "روابط المشروع" else "PROJECT LINKS", color = Color(0xFF9AAABD), fontSize = 9.sp, letterSpacing = 1.5.sp)
                OutlinedButton(onClick = onChangelog, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Article, contentDescription = null); Spacer(Modifier.width(8.dp))
                    Text(if (arabic) "فتح الموقع الرسمي" else "Open official website")
                }

            }
        }
        if (releases.isNotEmpty()) {
            item { SectionHeading(if (arabic) "إصدارات الرومات" else "ROM releases", "") }
            items(releases, key = { it.deviceId + it.version + it.title }) { release ->
                GlassPanel {
                    Text(release.title, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("One UI " + release.version + " • " + release.deviceId, color = Color(0xFF9AAABD), fontSize = 11.sp)
                    if (release.downloadUrl != null) TextButton(onClick = { onDownload(release.downloadUrl) }) {
                        Icon(Icons.Default.Download, contentDescription = null); Text(if (arabic) "تحميل" else "Download", color = Blue)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(modifier: Modifier, arabic: Boolean, onWebsite: () -> Unit, onTelegram: () -> Unit) {
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (arabic) "عن ZineROM" else "About ZineROM", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        GlassPanel {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Blue.copy(alpha = .16f)).border(1.dp, Blue.copy(alpha = .3f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Text("Z", color = Blue, fontSize = 44.sp, fontWeight = FontWeight.Black)
            }
            Text("ZineROM info", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(if (arabic) "التطبيق المرافق لمشروع ZineROM." else "The companion app for the ZineROM project.", color = Color(0xFF9AAABD), fontSize = 13.sp)
            Text("Version " + CURRENT_APP_VERSION, color = Blue, fontSize = 12.sp)
        }
        OutlinedButton(onClick = onWebsite, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Language, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(if (arabic) "الموقع الرسمي" else "Official website")
        }
        OutlinedButton(onClick = onTelegram, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Send, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(if (arabic) "قناة تيليجرام" else "Telegram channel")
        }
    }
}

@Composable
private fun DeviceDetails(device: Device, arabic: Boolean, modifier: Modifier, onBack: () -> Unit, onDownload: (String?) -> Unit) {
    LazyColumn(modifier, contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = null); Spacer(Modifier.width(5.dp))
                Text(if (arabic) "رجوع للأجهزة" else "Back to devices")
            }
            Box(Modifier.fillMaxWidth().height(235.dp).clip(RoundedCornerShape(27.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1A3048), Color(0xFF0F1927))))
                .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(27.dp)), contentAlignment = Alignment.Center) {
                AsyncImage(device.imageUrl, device.name, Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.height(10.dp))
            Text(device.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(device.model ?: "Model not specified", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(device.description.orEmpty(), color = Color(0xFF9AAABD), fontSize = 12.sp, lineHeight = 19.sp)
        }
        item {
            GlassPanel {
                Text(if (arabic) "إصدارات One UI" else "SUPPORTED ONE UI VERSIONS", color = Color(0xFF9AAABD), fontSize = 9.sp, letterSpacing = 1.4.sp)
                if (device.versions.isEmpty()) Text(if (arabic) "لا توجد إصدارات موثقة بعد." else "No verified versions listed yet.", color = Color(0xFF9AAABD), fontSize = 12.sp)
                else Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    device.versions.forEach { version ->
                        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Blue.copy(alpha = .13f)).border(1.dp, Blue.copy(alpha = .22f), RoundedCornerShape(10.dp)).padding(horizontal = 11.dp, vertical = 7.dp)) {
                            Text(version, color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (device.roms.isEmpty()) {
            item {
                GlassPanel {
                    Text(if (arabic) "صفحة الروم" else "ROM information", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(if (arabic) "افتح صفحة الجهاز على موقع ZineROM لعرض التفاصيل والتحميلات." else "Open the ZineROM device page for complete details and downloads.", color = Color(0xFF9AAABD), fontSize = 12.sp)
                    Button(onClick = { onDownload(device.downloadUrl) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null); Spacer(Modifier.width(8.dp))
                        Text(if (arabic) "فتح صفحة الجهاز" else "Open device page")
                    }
                }
            }
        } else {
            item { SectionHeading(if (arabic) "إصدارات ZineROM" else "ZineROM builds", "") }
            items(device.roms, key = { it.title + it.version }) { rom ->
                GlassPanel {
                    Text(rom.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("One UI " + rom.version, color = Blue, fontSize = 11.sp)
                    Button(onClick = { onDownload(rom.downloadUrl) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color(0xFF07111D))) {
                        Icon(Icons.Default.Download, contentDescription = null); Spacer(Modifier.width(8.dp))
                        Text(if (arabic) "تحميل الروم" else "Download ROM", fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { TextButton(onClick = { onDownload(device.downloadUrl) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (arabic) "كل تفاصيل الجهاز على الموقع" else "Full device page on website", color = Blue)
            } }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) Text(subtitle, color = Color(0xFF9AAABD), fontSize = 11.sp)
    }
}

@Composable
private fun GlassPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xE51B2A3D), Color(0xE5101927))))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .16f), Blue.copy(alpha = .13f), Color.White.copy(alpha = .05f))), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content
    )
}

// CI build trigger: verify the current ZineROM Info APK after notification workflow repair.
