package com.schengen.geoexplorer

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class Country(val name:String,val capital:String,val divisions:String,val currency:String,val regions:List<String>)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val countries = assets.open("countries.txt").bufferedReader().use { reader ->
   reader.readLines().filter { it.isNotBlank() }.mapNotNull { line ->
    val p=line.split("|")
    if(p.size<5) null else Country(p[0],p[1],p[2],p[3],p[4].split(";").filter { it.isNotBlank() })
   }
  }
  setContent { MaterialTheme { Explorer(countries) } }
 }
}
@Composable fun Explorer(data:List<Country>) {
 val context=LocalContext.current
 val prefs=remember { context.getSharedPreferences("favorites",Activity.MODE_PRIVATE) }
 var country by remember { mutableStateOf<Country?>(null) }
 var region by remember { mutableStateOf<String?>(null) }
 var query by remember { mutableStateOf("") }
 var favoritesOnly by remember { mutableStateOf(false) }
 var favorites by remember { mutableStateOf(prefs.getStringSet("countries",emptySet())!!.toSet()) }
 fun back() { if(region!=null) region=null else country=null;query="" }
 BackHandler(country!=null) { back() }
 Column(Modifier.fillMaxSize().padding(16.dp)) {
  Text("Schengen GeoExplorer",style=MaterialTheme.typography.headlineMedium)
  Text("Offline country directory • 29 Schengen countries",style=MaterialTheme.typography.bodySmall)
  Spacer(Modifier.height(12.dp))
  if(country!=null) TextButton(onClick={back()}) { Text("← Back") }
  OutlinedTextField(value=query,onValueChange={query=it},label={Text("Search")},singleLine=true,modifier=Modifier.fillMaxWidth())
  Spacer(Modifier.height(8.dp))
  if(country==null) {
   Row {
    FilterChip(selected=!favoritesOnly,onClick={favoritesOnly=false},label={Text("All countries")})
    Spacer(Modifier.width(8.dp))
    FilterChip(selected=favoritesOnly,onClick={favoritesOnly=true},label={Text("Favorites")})
   }
   LazyColumn {
    items(data.filter { it.name.contains(query,true) && (!favoritesOnly || it.name in favorites) }) { c ->
     ListItem(headlineContent={Text(c.name)},supportingContent={Text("Capital: ${c.capital}")},trailingContent={Text(if(c.name in favorites) "★" else "›")},modifier=Modifier.clickable {country=c;query=""})
     HorizontalDivider()
    }
   }
  } else {
   val c=country!!
   if(region==null) {
    Text(c.name,style=MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text("Capital: ${c.capital}")
    Text("Administrative structure: ${c.divisions}")
    Text("Currency: ${c.currency}")
    Text("Regions listed: ${c.regions.size}")
    TextButton(onClick={
     val next=favorites.toMutableSet()
     if(!next.add(c.name))next.remove(c.name)
     favorites=next
     prefs.edit().putStringSet("countries",next).apply()
    }) { Text(if(c.name in favorites) "★ Remove favorite" else "☆ Add favorite") }
    Spacer(Modifier.height(8.dp))
    Text("Administrative divisions",style=MaterialTheme.typography.titleMedium)
    if(c.regions.isEmpty()) Text("Detailed regional directory not yet included.")
    LazyColumn {
     items(c.regions.filter { it.contains(query,true) }) { r ->
      ListItem(headlineContent={Text(r)},modifier=Modifier.clickable {region=r;query=""})
      HorizontalDivider()
     }
    }
   } else {
    Text(region!!,style=MaterialTheme.typography.headlineSmall)
    Text("Country: ${c.name}")
    Spacer(Modifier.height(8.dp))
    Text("Province and municipality dataset is not yet included in this prototype.")
   }
  }
 }
}