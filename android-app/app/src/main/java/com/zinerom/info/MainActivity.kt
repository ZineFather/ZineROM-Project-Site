package com.zinerom.info

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Device(val id:String,val name:String,val model:String?,val versions:List<String>)
class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val req=PeriodicWorkRequestBuilder<UpdateWorker>(24,TimeUnit.HOURS).build()
  WorkManager.getInstance(this).enqueueUniquePeriodicWork("zinerom-update-check",ExistingPeriodicWorkPolicy.KEEP,req)
  if (FirebaseApp.getApps(this).isNotEmpty()) {
   val topic = when (android.os.Build.MODEL.uppercase()) {
    "SM-A528B", "SM-A528N", "SM-A528U", "SM-A528W" -> "zinerom_a52s-5g"
    "SM-A536B", "SM-A536E" -> "zinerom_a53-5g"
    "SM-A346B", "SM-A346E" -> "zinerom_a34-5g"
    "SM-A256B", "SM-A256E" -> "zinerom_a25-5g"
    "SM-A155F" -> "zinerom_a15-4g"
    "SM-A165F" -> "zinerom_a16-4g"
    "SM-A115F" -> "zinerom_a11-4g"
    "SM-A245F" -> "zinerom_a24-4g"
    "SM-X216B" -> "zinerom_tab-a9-plus-5g-x216b"
    else -> null
   }
   FirebaseMessaging.getInstance().subscribeToTopic("zinerom_all")
   if (topic != null) FirebaseMessaging.getInstance().subscribeToTopic(topic)
  }
  setContent { ZineApp() }
 }
}
class UpdateWorker(ctx:android.content.Context,p:WorkerParameters):CoroutineWorker(ctx,p){
 override suspend fun doWork():Result=try{
  val req=Request.Builder().url("https://raw.githubusercontent.com/ZineFather/ZineROM-Project-Site/main/remote/releases.json").build()
  OkHttpClient().newCall(req).execute().use { if(it.isSuccessful) Result.success() else Result.retry() }
 }catch(_:Exception){Result.retry()}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ZineApp(){
 val ctx=androidx.compose.ui.platform.LocalContext.current
 val scope=rememberCoroutineScope()
 var tab by remember{mutableStateOf("Home")}
 var devices by remember{mutableStateOf(emptyList<Device>())}
 var status by remember{mutableStateOf("Ready")}
 var releaseEntries by remember{mutableStateOf(emptyList<Pair<String,String?>>())}
 LaunchedEffect(Unit){
  devices=withContext(Dispatchers.IO){
   val root=JSONObject(ctx.assets.open("devices.json").bufferedReader().use{it.readText()})
   val a=root.getJSONArray("devices")
   (0 until a.length()).map{ i-> val d=a.getJSONObject(i);val v=d.getJSONArray("versions");Device(d.getString("id"),d.getString("name"),if(d.isNull("model"))null else d.getString("model"),(0 until v.length()).map{v.getString(it)})}
  }
 }
 MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF85BDFF),background=Color(0xFF08111F),surface=Color(0xFF14243A))){
  Scaffold(containerColor=Color(0xFF08111F),topBar={TopAppBar(title={Text("ZineROM INFO")},colors=TopAppBarDefaults.topAppBarColors(containerColor=Color(0xFF08111F),titleContentColor=Color.White))},bottomBar={
   NavigationBar(containerColor=Color(0xFF14243A)){listOf("Home","Devices","Updates","About").forEach{t->NavigationBarItem(selected=tab==t,onClick={tab=t},icon={Icon(when(t){"Devices"->Icons.Default.PhoneAndroid;"Updates"->Icons.Default.SystemUpdate;"About"->Icons.Default.Info;else->Icons.Default.Home},t)},label={Text(t)})}}
  }){pad->
   when(tab){
    "Devices"->LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
     item{Text("Supported devices",style=MaterialTheme.typography.headlineSmall,color=Color.White)}
     items(devices){d->ElevatedCard(Modifier.fillMaxWidth(),colors=CardDefaults.elevatedCardColors(containerColor=Color(0xFF14243A))){Column(Modifier.padding(16.dp)){Text(d.name,color=Color.White,style=MaterialTheme.typography.titleMedium);Text(d.model?:"Model not specified",color=Color.LightGray);Text(if(d.versions.isEmpty())"No ROM release listed" else "One UI "+d.versions.joinToString(" · "),color=Color(0xFF85BDFF))}}}
    }
    "Updates"->Column(Modifier.padding(pad).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
     Text("Software updates",style=MaterialTheme.typography.headlineSmall,color=Color.White)
     Button(onClick={status="Checking…";scope.launch{try{val result=withContext(Dispatchers.IO){val request=Request.Builder().url("https://raw.githubusercontent.com/ZineFather/ZineROM-Project-Site/main/remote/releases.json").build();OkHttpClient().newCall(request).execute().use{response->if(!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}");val a=JSONObject(response.body?.string().orEmpty()).optJSONArray("releases")?:org.json.JSONArray();(0 until a.length()).map{i->val x=a.getJSONObject(i);Pair("${x.optString("title","ZineROM")} · ${x.optString("version")} · ${x.optString("device_id")}",x.optString("download_url").takeIf{u->u.startsWith("https://")})}}};releaseEntries=result;status="Check complete · ${result.size} release(s)"}catch(e:Exception){status="Check failed: ${e.localizedMessage}"}}}){Text("Check now")}
     Text(status,color=Color(0xFF85BDFF))
     releaseEntries.forEach{entry->ElevatedCard(colors=CardDefaults.elevatedCardColors(containerColor=Color(0xFF14243A))){Column(Modifier.padding(12.dp)){Text(entry.first,color=Color.White);if(entry.second!=null)TextButton(onClick={ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(entry.second)))}){Text("Download release")}}}}
    }
    "About"->Column(Modifier.padding(pad).padding(20.dp)){Text("About ZineROM Info",color=Color.White,style=MaterialTheme.typography.headlineSmall);Text("Official companion for the ZineROM Project.",color=Color.LightGray);Text("https://zinefather.github.io/ZineROM-Project-Site/",color=Color(0xFF85BDFF))}
    else->Column(Modifier.padding(pad).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Text("ZineROM Info",color=Color.White,style=MaterialTheme.typography.headlineLarge);Text("Your ZineROM project companion",color=Color(0xFF85BDFF),style=MaterialTheme.typography.titleMedium);ElevatedCard(colors=CardDefaults.elevatedCardColors(containerColor=Color(0xFF14243A))){Column(Modifier.padding(18.dp)){Text("${devices.size} supported catalog devices",color=Color.White);Text("Browse ROM versions and device information.",color=Color.LightGray);TextButton(onClick={tab="Devices"}){Text("Browse devices")}}};ElevatedCard(colors=CardDefaults.elevatedCardColors(containerColor=Color(0xFF14243A))){Column(Modifier.padding(18.dp)){Text("Automatic update checks",color=Color.White);Text("Background checks are scheduled approximately every 24 hours.",color=Color.LightGray);TextButton(onClick={tab="Updates"}){Text("Check releases")}}}}
   }
  }
 }
}
