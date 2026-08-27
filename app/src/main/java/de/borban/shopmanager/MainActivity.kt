package de.borban.shopmanager

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.borban.shopmanager.data.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Blue=Color(0xFF0C72A7); private val Ink=Color(0xFF172033); private val Soft=Color(0xFFF5F8FB)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{BorbanTheme{PermissionGate(); ShopManagerUi()}}}
}
@Composable private fun PermissionGate(){ if(Build.VERSION.SDK_INT>=33){ val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}; LaunchedEffect(Unit){launcher.launch(Manifest.permission.POST_NOTIFICATIONS)} } }
@Composable private fun BorbanTheme(content:@Composable()->Unit){MaterialTheme(colorScheme=lightColorScheme(primary=Blue,background=Soft,surface=Color.White,onSurface=Ink),content=content)}

class MainVm(private val repo:Repository):androidx.lifecycle.ViewModel(){
 var shops by mutableStateOf(repo.shops()); var dashboards by mutableStateOf<Map<ShopConnection,Dashboard>>(emptyMap()); var loading by mutableStateOf(false); var error by mutableStateOf<String?>(null)
 fun refresh(){viewModelScope.launch{loading=true;error=null;dashboards=runCatching{repo.dashboards()}.onFailure{error=it.message}.getOrDefault(emptyMap());shops=repo.shops();loading=false}}
 suspend fun pair(url:String,code:String,name:String)=repo.pair(url,code,name).also{shops=repo.shops()}
 fun remove(id:String){repo.remove(id);shops=repo.shops();refresh()}
 companion object{fun factory(ctx:android.content.Context)=object:ViewModelProvider.Factory{override fun <T:androidx.lifecycle.ViewModel> create(modelClass:Class<T>):T=MainVm(Repository(ctx)) as T}}
}

@Composable fun ShopManagerUi(){
 val ctx=androidx.compose.ui.platform.LocalContext.current; val vm:MainVm=viewModel(factory=MainVm.factory(ctx)); var tab by remember{mutableIntStateOf(0)}; var add by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){vm.refresh()}
 Scaffold(topBar={TopAppBar(title={Column{Text("Borban Shop Manager",fontWeight=FontWeight.Bold);Text("Private Multi-Shop Übersicht",style=MaterialTheme.typography.labelSmall,color=Color.Gray)}},actions={IconButton(onClick={vm.refresh()}){Icon(Icons.Outlined.Refresh,"Aktualisieren")};IconButton(onClick={add=true}){Icon(Icons.Outlined.Add,"Shop hinzufügen")}})},bottomBar={NavigationBar{listOf("Übersicht" to Icons.Outlined.Dashboard,"Bestellungen" to Icons.Outlined.ReceiptLong,"Shops" to Icons.Outlined.Store).forEachIndexed{i,(l,ic)->NavigationBarItem(tab==i,{tab=i},{Icon(ic,l)},{Text(l)})}}}){pad->Box(Modifier.padding(pad).fillMaxSize()){when(tab){0->DashboardScreen(vm);1->OrdersScreen(vm);else->ShopsScreen(vm){add=true}};if(vm.loading)LinearProgressIndicator(Modifier.fillMaxWidth())}}
 if(add) PairDialog(vm){add=false}
}

@Composable private fun DashboardScreen(vm:MainVm){
 val all=vm.dashboards.values; val revenue=all.sumOf{it.today.revenue}; val orders=all.sumOf{it.today.orders}; val open=all.sumOf{it.openOrders}; val paid=all.sumOf{it.paidToday}
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Text("Heute",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricCard("Umsatz",money(revenue),Icons.Outlined.Euro,Modifier.weight(1f));MetricCard("Bestellungen",orders.toString(),Icons.Outlined.ShoppingBag,Modifier.weight(1f))}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricCard("Offen",open.toString(),Icons.Outlined.PendingActions,Modifier.weight(1f));MetricCard("Bezahlt",paid.toString(),Icons.Outlined.CheckCircle,Modifier.weight(1f))}}
  item{Spacer(Modifier.height(6.dp));Text("Shops",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
  items(vm.shops){s->val d=vm.dashboards[s];Card{Column(Modifier.padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Surface(color=Blue.copy(.1f),shape=RoundedCornerShape(10.dp)){Icon(Icons.Outlined.Store,s.name,tint=Blue,modifier=Modifier.padding(9.dp))};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(s.name,fontWeight=FontWeight.Bold);Text(s.url,style=MaterialTheme.typography.bodySmall,color=Color.Gray)};Text(if(d!=null) money(d.today.revenue) else "–",fontWeight=FontWeight.Bold)};if(d!=null){Spacer(Modifier.height(12.dp));Text("${d.today.orders} Bestellungen · ${d.openOrders} offen",style=MaterialTheme.typography.bodyMedium)}}}}
 }
}
@Composable private fun MetricCard(label:String,value:String,icon:androidx.compose.ui.graphics.vector.ImageVector,mod:Modifier=Modifier){Card(mod){Column(Modifier.padding(15.dp)){Icon(icon,label,tint=Blue);Spacer(Modifier.height(10.dp));Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(label,color=Color.Gray,style=MaterialTheme.typography.bodySmall)}}}

@Composable private fun OrdersScreen(vm:MainVm){
 val scope=rememberCoroutineScope(); var selected by remember(vm.shops){mutableStateOf(vm.shops.firstOrNull())}; var list by remember{mutableStateOf<List<OrderSummary>>(emptyList())}; var search by remember{mutableStateOf("")}; var detail by remember{mutableStateOf<OrderDetail?>(null)}; var busy by remember{mutableStateOf(false)}
 val ctx=androidx.compose.ui.platform.LocalContext.current; fun reload(){val s=selected?:return;scope.launch{busy=true;list=runCatching{Repository(ctx).orders(s,search)}.getOrDefault(emptyList());busy=false}}
 LaunchedEffect(selected){reload()}
 Column(Modifier.fillMaxSize().padding(16.dp)){if(vm.shops.isEmpty()){EmptyState("Noch kein Shop gekoppelt");return@Column};ShopPicker(vm.shops,selected){selected=it};Spacer(Modifier.height(10.dp));OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),placeholder={Text("Kunde oder Bestellnummer suchen")},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={IconButton(onClick={reload()}){Icon(Icons.Outlined.ArrowForward,null)}});Spacer(Modifier.height(10.dp));if(busy)LinearProgressIndicator(Modifier.fillMaxWidth());LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(list){o->OrderCard(o){val s=selected?:return@OrderCard;scope.launch{detail=Repository(ctx).order(s,o.id)}}}}}
 if(detail!=null) OrderDetailDialog(detail!!,selected!!,{detail=null}){group,action->scope.launch{Repository(ctx).transition(selected!!,detail!!.id,group,action);detail=Repository(ctx).order(selected!!,detail!!.id)}}
}

@Composable private fun ShopPicker(shops:List<ShopConnection>,selected:ShopConnection?,on:(ShopConnection)->Unit){var exp by remember{mutableStateOf(false)};Box{OutlinedButton({exp=true}){Icon(Icons.Outlined.Store,null);Spacer(Modifier.width(8.dp));Text(selected?.name ?: "Shop wählen")};DropdownMenu(exp,{exp=false}){shops.forEach{s->DropdownMenuItem({Text(s.name)},{on(s);exp=false})}}}}
@Composable private fun OrderCard(o:OrderSummary,onClick:()->Unit){Card(onClick=onClick){Column(Modifier.padding(14.dp)){Row{Column(Modifier.weight(1f)){Text("#${o.number}",fontWeight=FontWeight.Bold);Text(o.customer,color=Color.Gray)};Text(money(o.amount),fontWeight=FontWeight.Bold)};Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){StatusChip(o.orderState.label.ifBlank{o.orderState.technical});StatusChip(o.paymentState.label.ifBlank{o.paymentState.technical});StatusChip(o.deliveryState.label.ifBlank{o.deliveryState.technical})};Spacer(Modifier.height(8.dp));Text("${o.positions} Positionen · ${formatDate(o.date)}",style=MaterialTheme.typography.bodySmall,color=Color.Gray)}}}
@Composable private fun StatusChip(t:String){Surface(shape=RoundedCornerShape(999.dp),color=Blue.copy(.08f)){Text(t.ifBlank{"–"},Modifier.padding(horizontal=9.dp,vertical=4.dp),style=MaterialTheme.typography.labelSmall,color=Blue)}}

@Composable private fun OrderDetailDialog(o:OrderDetail,shop:ShopConnection,onDismiss:()->Unit,onTransition:(String,String)->Unit){AlertDialog(onDismissRequest=onDismiss,confirmButton={TextButton(onClick=onDismiss){Text("Schließen")}},title={Column{Text("Bestellung #${o.number}");Text(shop.name,style=MaterialTheme.typography.labelMedium,color=Blue)}},text={LazyColumn(Modifier.heightIn(max=620.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Row{Text(money(o.amount),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(formatDate(o.date),color=Color.Gray)}};item{Text("Status",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){StatusChip(o.orderState.label);StatusChip(o.paymentState.label);StatusChip(o.deliveryState.label)}};items(o.items){i->Row{Column(Modifier.weight(1f)){Text(i.label,fontWeight=FontWeight.SemiBold);Text("${i.quantity} × ${money(i.unitPrice)} · ${i.productNumber}",style=MaterialTheme.typography.bodySmall,color=Color.Gray)};Text(money(i.totalPrice))}};o.customer?.let{c->item{HorizontalDivider();Text("Kunde",fontWeight=FontWeight.Bold);Text("${c.firstName} ${c.lastName}");Text(c.email,color=Blue);c.shipping?.let{a->Text("${a.street}, ${a.zipcode} ${a.city}\n${a.country}",style=MaterialTheme.typography.bodySmall)}}};item{HorizontalDivider();Text("Schnellaktionen",fontWeight=FontWeight.Bold);Column(verticalArrangement=Arrangement.spacedBy(6.dp)){AssistChip({onTransition("payment","paid")},{Text("Als bezahlt")});AssistChip({onTransition("delivery","ship")},{Text("Versendet")});AssistChip({onTransition("order","complete")},{Text("Abschließen")})}}}})}

@Composable private fun ShopsScreen(vm:MainVm,onAdd:()->Unit){LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Row(verticalAlignment=Alignment.CenterVertically){Text("Gekoppelte Shops",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Button(onAdd){Icon(Icons.Outlined.Add,null);Text("Hinzufügen")}}};items(vm.shops){s->Card{Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.VerifiedUser,null,tint=Blue);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(s.name,fontWeight=FontWeight.Bold);Text(s.url,style=MaterialTheme.typography.bodySmall,color=Color.Gray);Text("Direktverbindung · eigener Geräteschlüssel",style=MaterialTheme.typography.labelSmall,color=Blue)};IconButton({vm.remove(s.shopId)}){Icon(Icons.Outlined.Delete,"Entfernen")}}}};if(vm.shops.isEmpty())item{EmptyState("Noch kein Shop gekoppelt")}}}

@Composable private fun PairDialog(vm:MainVm,onDismiss:()->Unit){val scope=rememberCoroutineScope();var url by remember{mutableStateOf("")};var code by remember{mutableStateOf("")};var name by remember{mutableStateOf(android.os.Build.MODEL.ifBlank{"Samsung"})};var error by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)};AlertDialog(onDismissRequest=onDismiss,title={Text("Shop koppeln")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Die App verbindet sich anschließend direkt per HTTPS mit deinem Shop.",color=Color.Gray);OutlinedTextField(url,{url=it},label={Text("Shop-URL")},placeholder={Text("https://www.vapetrade.de")});OutlinedTextField(code,{code=it},label={Text("Einmaliger Kopplungscode")});OutlinedTextField(name,{name=it},label={Text("Gerätename")});error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},dismissButton={TextButton(onDismiss){Text("Abbrechen")}},confirmButton={Button(enabled=!busy&&url.isNotBlank()&&code.isNotBlank(),onClick={scope.launch{busy=true;val r=vm.pair(url,code,name);busy=false;r.onSuccess{vm.refresh();onDismiss()}.onFailure{error=it.message}}}){Text(if(busy)"Verbinden…" else "Sicher koppeln")}})}
@Composable private fun EmptyState(t:String){Box(Modifier.fillMaxWidth().padding(40.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Outlined.Store,null,tint=Color.Gray,modifier=Modifier.size(42.dp));Spacer(Modifier.height(8.dp));Text(t,color=Color.Gray)}}}
private fun money(v:Double)=NumberFormat.getCurrencyInstance(Locale.GERMANY).format(v)
private fun formatDate(v:String)=runCatching{OffsetDateTime.parse(v).format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"))}.getOrDefault(v)
