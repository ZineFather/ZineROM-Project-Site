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
private const val NOTIFICATION_CHANNEL = "zinerom_updates"
private val httpClient = OkHttpClient()

data class AppConfig(
    val websiteUrl: String,
    val releasesUrl: String,
    val telegramUrl: String,
    val changelogUrl: String,
    val workflowUrl: String
)
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
        val work = PeriodicWorkRequestBuilder<UpdateWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "zinerom-update-check", ExistingPeriodicWorkPolicy.KEEP, work
        )
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            FirebaseMessaging.getInstance().subscribeToTopic("zinerom_all")
            val model = Build.MODEL.uppercase()
            val topic = when {
                model.startsWith("SM-A528") -> "zinerom_a52s-5g"
                model.startsWith("SM-A536") -> "zinerom_a53-5g"
                model.startsWith("SM-A346") -> "zinerom_a34-5g"
                model.startsWith("SM-A256") -> "zinerom_a25-5g"
                model == "SM-A155F" -> "zinerom_a15-4g"
                model == "SM-A165F" -> "zinerom_a16-4g"
                model == "SM-A115F" -> "zinerom_a11-4g"
                model == "SM-A245F" -> "zinerom_a24-4g"
                model == "SM-X216B" -> "zinerom_tab-a9-plus-5g-x216b"
                else -> null
            }
            if (topic != null) FirebaseMessaging.getInstance().subscribeToTopic(topic)
        }
        setContent { ZineApp() }
    }
}

private fun createUpdateChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL, "ZineROM Updates", NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

private fun notifyUpdate(context: Context, version: String) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return
    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("ZineROM info update available")
        .setContentText("Version " + version + " is available. Open Telegram for the changelog.")
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    try { NotificationManagerCompat.from(context).notify(325, notification) } catch (_: SecurityException) { }
}

private fun versionIsNewer(remote: String, current: String): Boolean {
    val a = remote.split(".").map { it.toIntOrNull() ?: 0 }
    val b = current.split(".").map { it.toIntOrNull() ?: 0 }
    for (index in 0 until maxOf(a.size, b.size)) {
        val left = a.getOrElse(index) { 0 }
        val right = b.getOrElse(index) { 0 }
        if (left != right) return left > right
    }
    return false
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
        telegramUrl = json.optString("telegram_url", DEFAULT_TELEGRAM),
        changelogUrl = json.optString("changelog_url", DEFAULT_TELEGRAM),
        workflowUrl = json.optString("workflow_url")
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

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val config = readConfig(applicationContext)
        val request = Request.Builder().url(config.releasesUrl).build()
        val latest = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP " + response.code)
            JSONObject(response.body?.string().orEmpty()).optString("latest_app_version", BuildConfig.VERSION_NAME)
        }
        if (versionIsNewer(latest, BuildConfig.VERSION_NAME)) notifyUpdate(applicationContext, latest)
        Result.success()
    } catch (_: Exception) { Result.retry() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZineApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf<AppConfig?>(null) }
    var devices by remember { mutableStateOf(emptyList<Device>()) }
    var selectedDevice by remember { mutableStateOf<Device?>(null) }
    var tab by remember { mutableStateOf("Home") }
    var arabic by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready") }
    var latestVersion by remember { mutableStateOf("") }
    var releases by remember { mutableStateOf(emptyList<Release>()) }

    LaunchedEffect(Unit) {
        config = withContext(Dispatchers.IO) { readConfig(context) }
        devices = withContext(Dispatchers.IO) { readDevices(context) }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Blue, background = Background, surface = SurfaceDark)) {
        CompositionLocalProvider(LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Scaffold(
                containerColor = Background,
                topBar = {
                    TopAppBar(
                        title = { Text("ZineROM info") },
                        actions = { TextButton(onClick = { arabic = !arabic }) { Text(if (arabic) "EN" else "عربي", color = Color.White) } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background, titleContentColor = Color.White)
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = SurfaceDark) {
                        listOf("Home", "Devices", "Updates", "About").forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { selectedDevice = null; tab = item },
                                icon = {
                                    Icon(when (item) {
                                        "Devices" -> Icons.Default.PhoneAndroid
                                        "Updates" -> Icons.Default.SystemUpdate
                                        "About" -> Icons.Default.Info
                                        else -> Icons.Default.Home
                                    }, contentDescription = item)
                                },
                                label = { Text(if (arabic) when (item) {
                                    "Home" -> "الرئيسية"; "Devices" -> "الأجهزة"; "Updates" -> "التحديثات"; else -> "حول"
                                } else item) }
                            )
                        }
                    }
                }
            ) { padding ->
                when {
                    selectedDevice != null -> DeviceDetails(
                        device = selectedDevice!!, arabic = arabic,
                        onBack = { selectedDevice = null },
                        onDownload = { openLink(context, it) }
                    )
                    tab == "Devices" -> LazyColumn(
                        Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Text(if (arabic) "الأجهزة المدعومة" else "Supported devices", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                        items(devices, key = { it.id }) { device ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth().clickable { selectedDevice = device },
                                colors = CardDefaults.elevatedCardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(device.imageUrl, device.name, Modifier.size(88.dp), contentScale = ContentScale.Fit)
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(device.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                        Text(device.model ?: "Model not specified", color = Color.LightGray)
                                        Text(if (device.versions.isEmpty()) "Check website for availability" else "One UI " + device.versions.joinToString(" · "), color = Blue)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Blue)
                                }
                            }
                        }
                    }
                    tab == "Updates" -> Column(
                        Modifier.fillMaxSize().padding(padding).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(if (arabic) "تحديثات التطبيق" else "App updates", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text("Installed version: " + BuildConfig.VERSION_NAME, color = Color.LightGray)
                        Button(enabled = !checking, onClick = {
                            checking = true
                            status = if (arabic) "جارٍ التحقق..." else "Checking for updates..."
                            scope.launch {
                                try {
                                    val feed = withContext(Dispatchers.IO) {
                                        val source = config ?: readConfig(context)
                                        val request = Request.Builder().url(source.releasesUrl).build()
                                        httpClient.newCall(request).execute().use { response ->
                                            if (!response.isSuccessful) throw IllegalStateException("HTTP " + response.code)
                                            JSONObject(response.body?.string().orEmpty())
                                        }
                                    }
                                    latestVersion = feed.optString("latest_app_version", BuildConfig.VERSION_NAME)
                                    val hasUpdate = versionIsNewer(latestVersion, BuildConfig.VERSION_NAME)
                                    val entries = feed.optJSONArray("releases") ?: org.json.JSONArray()
                                    releases = (0 until entries.length()).map { i ->
                                        val entry = entries.getJSONObject(i)
                                        Release(
                                            entry.optString("title", "ZineROM"),
                                            entry.optString("version"),
                                            entry.optString("device_id"),
                                            entry.optString("download_url").takeIf { it.startsWith("https://") }
                                        )
                                    }
                                    status = if (hasUpdate) {
                                        if (arabic) "يتوفر تحديث جديد: " + latestVersion else "Update available: " + latestVersion
                                    } else {
                                        if (arabic) "أنت على أحدث إصدار: " + BuildConfig.VERSION_NAME else "You're up to date: " + BuildConfig.VERSION_NAME
                                    }
                                    if (hasUpdate) notifyUpdate(context, latestVersion)
                                } catch (error: Exception) {
                                    status = (if (arabic) "فشل التحقق: " else "Check failed: ") + (error.localizedMessage ?: "Network error")
                                } finally { checking = false }
                            }
                        }) { Text(if (checking) "Checking..." else if (arabic) "تحقق من التحديثات" else "Check for updates") }
                        Text(status, color = Blue)
                        if (latestVersion.isNotBlank()) {
                            OutlinedButton(onClick = { openLink(context, config?.changelogUrl ?: DEFAULT_TELEGRAM) }) {
                                Text(if (arabic) "عرض سجل التغييرات على تيليجرام" else "View changelog on Telegram")
                            }
                        }
                        OutlinedButton(onClick = { openLink(context, config?.workflowUrl) }) {
                            Text(if (arabic) "فتح GitHub Actions" else "Run / View GitHub Workflow")
                        }
                        releases.forEach { release ->
                            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceDark)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(release.title, color = Color.White)
                                    Text("Version " + release.version + " · " + release.deviceId, color = Color.LightGray)
                                    if (release.downloadUrl != null) TextButton(onClick = { openLink(context, release.downloadUrl) }) { Text("Download") }
                                }
                            }
                        }
                    }
                    tab == "About" -> Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ZineROM info", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text("Version " + BuildConfig.VERSION_NAME, color = Blue)
                        Text("Official ZineROM project companion.", color = Color.LightGray)
                        TextButton(onClick = { openLink(context, config?.websiteUrl) }) { Text("Official website") }
                    }
                    else -> Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("ZineROM info", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                        Text(if (arabic) "رفيقك لمشروع ZineROM" else "Your ZineROM project companion", color = Blue, style = MaterialTheme.typography.titleMedium)
                        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceDark)) {
                            Column(Modifier.padding(18.dp)) {
                                Text(devices.size.toString() + " supported devices", color = Color.White)
                                Text(if (arabic) "تصفح إصدارات الرومات ومعلومات الأجهزة." else "Browse ROM versions and device information.", color = Color.LightGray)
                                TextButton(onClick = { tab = "Devices" }) { Text(if (arabic) "تصفح الأجهزة" else "Browse devices") }
                            }
                        }
                        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceDark)) {
                            Column(Modifier.padding(18.dp)) {
                                Text(if (arabic) "فحص التحديثات" else "App update checker", color = Color.White)
                                Text("Installed version " + BuildConfig.VERSION_NAME, color = Color.LightGray)
                                TextButton(onClick = { tab = "Updates" }) { Text(if (arabic) "فحص الإصدارات" else "Check releases") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceDetails(
    device: Device, arabic: Boolean, onBack: () -> Unit, onDownload: (String?) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (arabic) "رجوع" else "Back")
            }
            AsyncImage(
                model = device.imageUrl, contentDescription = device.name,
                modifier = Modifier.fillMaxWidth().height(230.dp), contentScale = ContentScale.Fit
            )
            Text(device.name, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(device.model ?: "Model not specified", color = Blue)
            Text(device.description ?: "", color = Color.LightGray)
            Text(if (arabic) "إصدارات One UI المدعومة" else "Supported One UI versions", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(
                if (device.versions.isEmpty()) {
                    if (arabic) "لا توجد إصدارات موثقة بعد." else "No verified versions listed yet."
                } else device.versions.joinToString(" · ") { "One UI " + it },
                color = Color.LightGray
            )
            if (device.roms.isEmpty()) {
                Button(onClick = { onDownload(device.downloadUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (arabic) "فتح صفحة التحميل الرسمية" else "Open official download page")
                }
            } else {
                Text(if (arabic) "إصدارات ZineROM" else "ZineROM builds", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
        items(device.roms) { rom ->
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceDark)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(rom.title, color = Color.White)
                    Text("One UI " + rom.version, color = Blue)
                    Button(onClick = { onDownload(rom.downloadUrl) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (arabic) "تحميل Download" else "Download")
                    }
                }
            }
        }
        if (device.roms.isNotEmpty()) {
            item {
                OutlinedButton(onClick = { onDownload(device.downloadUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (arabic) "صفحة الجهاز على الموقع" else "Device page on website")
                }
            }
        }
    }
}
