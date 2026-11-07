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
data class Place(val code:String,val region:String,val province:String,val city:String)
val codes = mapOf("Austria" to "AT","Belgium" to "BE","Bulgaria" to "BG","Croatia" to "HR","Czechia" to "CZ","Denmark" to "DK","Estonia" to "EE","Finland" to "FI","France" to "FR","Germany" to "DE","Greece" to "GR","Hungary" to "HU","Iceland" to "IS","Italy" to "IT","Latvia" to "LV","Liechtenstein" to "LI","Lithuania" to "LT","Luxembourg" to "LU","Malta" to "MT","Netherlands" to "NL","Norway" to "NO","Poland" to "PL","Portugal" to "PT","Romania" to "RO","Slovakia" to "SK","Slovenia" to "SI","Spain" to "ES","Sweden" to "SE","Switzerland" to "CH")

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val countries = assets.open("countries.txt").bufferedReader().useLines { lines ->
   lines.filter { it.isNotBlank() }.mapNotNull { line ->
    val p=line.split("|")
    if(p.size<5) null else Country(p[0],p[1],p[2],p[3],p[4].split(";").filter { it.isNotBlank() })
   }.toList()
  }
  val places = try {
   assets.open("places.tsv").bufferedReader().useLines { lines ->
    lines.mapNotNull { line ->
     val p=line.split("\t")
     if(p.size!=4)null else Place(p[0],p[1],p[2],p[3])
    }.toList()
   }
  } catch (_:Exception) { emptyList() }
  setContent { MaterialTheme { Explorer(countries,places) } }
 }
}
@Composable fun Explorer(data:List<Country>,places:List<Place>) {
 val context=LocalContext.current
 val prefs=remember { context.getSharedPreferences("favorites",Activity.MODE_PRIVATE) }
 var country by remember { mutableStateOf<Country?>(null) }
 var region by remember { mutableStateOf<String?>(null) }
 var province by remember { mutableStateOf<String?>(null) }
 var query by remember { mutableStateOf("") }
 var favoritesOnly by remember { mutableStateOf(false) }
 var favorites by remember { mutableStateOf(prefs.getStringSet("countries",emptySet())!!.toSet()) }
 fun back() {
  if(province!=null) province=null else if(region!=null)region=null else country=null
  query=""
 }
 BackHandler(country!=null) { back() }
 val current=country
 val subset=remember(current,places) { places.filter { it.code==codes[current?.name] } }
 Column(Modifier.fillMaxSize().padding(16.dp)) {
  Text("Schengen GeoExplorer",style=MaterialTheme.typography.headlineMedium)
  Text("Offline • 29 countries • GeoNames cities500 coverage",style=MaterialTheme.typography.bodySmall)
  Spacer(Modifier.height(12.dp))
  if(current!=null) TextButton(onClick={back()}) { Text("← Back") }
  OutlinedTextField(value=query,onValueChange={query=it},label={Text("Search current level")},singleLine=true,modifier=Modifier.fillMaxWidth())
  Spacer(Modifier.height(8.dp))
  if(current==null) {
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
  } else if(region==null) {
   Text(current.name,style=MaterialTheme.typography.headlineSmall)
   Text("Capital: ${current.capital}")
   Text("Administrative structure: ${current.divisions}")
   Text("Currency: ${current.currency}")
   Text("Places included: ${subset.size}")
   TextButton(onClick={
    val next=favorites.toMutableSet()
    if(!next.add(current.name))next.remove(current.name)
    favorites=next
    prefs.edit().putStringSet("countries",next).apply()
   }) { Text(if(current.name in favorites) "★ Remove favorite" else "☆ Add favorite") }
   Text("Regions / first-level areas",style=MaterialTheme.typography.titleMedium)
   val regions=remember(subset) { subset.map { it.region }.distinct().sorted() }
   LazyColumn {
    items(regions.filter { it.contains(query,true) }) { r ->
     ListItem(headlineContent={Text(r)},modifier=Modifier.clickable {region=r;query=""})
     HorizontalDivider()
    }
   }
  } else if(province==null) {
   Text(region!!,style=MaterialTheme.typography.headlineSmall)
   Text(current.name,style=MaterialTheme.typography.bodySmall)
   Text("Provinces / second-level areas",style=MaterialTheme.typography.titleMedium)
   val provinces=remember(subset,region) { subset.filter { it.region==region }.map { it.province }.distinct().sorted() }
   LazyColumn {
    items(provinces.filter { it.contains(query,true) }) { p ->
     ListItem(headlineContent={Text(p)},modifier=Modifier.clickable {province=p;query=""})
     HorizontalDivider()
    }
   }
  } else {
   Text(province!!,style=MaterialTheme.typography.headlineSmall)
   Text("${current.name} / ${region}",style=MaterialTheme.typography.bodySmall)
   Text("Cities and towns (population ≥500, where recorded)",style=MaterialTheme.typography.titleMedium)
   val cities=remember(subset,region,province) { subset.filter { it.region==region && it.province==province }.map { it.city }.distinct().sorted() }
   LazyColumn {
    items(cities.filter { it.contains(query,true) }) { city ->
     ListItem(headlineContent={Text(city)})
     HorizontalDivider()
    }
   }
  }
 }
}
