@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.borban.shopmanager

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.borban.shopmanager.data.OrderDetail
import de.borban.shopmanager.data.OrderSummary
import de.borban.shopmanager.data.Repository
import de.borban.shopmanager.data.ShopConnection
import de.borban.shopmanager.data.StatBucket
import de.borban.shopmanager.data.StatisticsRange
import de.borban.shopmanager.data.Dashboard
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Blue = Color(0xFF0C72A7)
private val Ink = Color(0xFF172033)
private val Soft = Color(0xFFF5F8FB)
private val Positive = Color(0xFF127A47)
private val Negative = Color(0xFFB43131)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BorbanTheme {
                PermissionGate()
                ShopManagerUi()
            }
        }
    }
}

@Composable
private fun PermissionGate() {
    if (Build.VERSION.SDK_INT >= 33) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
}

@Composable
private fun BorbanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Blue,
            background = Soft,
            surface = Color.White,
            onSurface = Ink,
        ),
        content = content,
    )
}

class MainVm(private val repo: Repository) : androidx.lifecycle.ViewModel() {
    var shops by mutableStateOf(repo.shops())
    var dashboards by mutableStateOf<Map<ShopConnection, Dashboard>>(emptyMap())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun refresh() {
        viewModelScope.launch {
            loading = true
            error = null
            dashboards = runCatching { repo.dashboards() }.onFailure { error = it.message }.getOrDefault(emptyMap())
            shops = repo.shops()
            loading = false
        }
    }

    suspend fun pair(url: String, code: String, name: String) = repo.pair(url, code, name).also { shops = repo.shops() }

    fun remove(id: String) {
        repo.remove(id)
        shops = repo.shops()
        refresh()
    }

    companion object {
        fun factory(ctx: android.content.Context) = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = MainVm(Repository(ctx)) as T
        }
    }
}

@Composable
fun ShopManagerUi() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val vm: MainVm = viewModel(factory = MainVm.factory(ctx))
    var tab by remember { mutableIntStateOf(0) }
    var add by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Column {
                        Text("Borban ShopManager", fontWeight = FontWeight.Bold)
                        Text("Read-only · sichere Direktverbindung", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.82f))
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Blue, titleContentColor = Color.White),
                actions = {
                    IconButton(onClick = { vm.refresh() }) { Icon(Icons.Outlined.Refresh, "Aktualisieren", tint = Color.White) }
                    IconButton(onClick = { add = true }) { Icon(Icons.Outlined.Add, "Shop hinzufügen", tint = Color.White) }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(
                    "Übersicht" to Icons.Outlined.Dashboard,
                    "Statistik" to Icons.Outlined.QueryStats,
                    "Bestellungen" to Icons.Outlined.ReceiptLong,
                    "Shops" to Icons.Outlined.Store,
                ).forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icon, label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> DashboardScreen(vm)
                1 -> StatisticsScreen(vm)
                2 -> OrdersScreen(vm)
                else -> ShopsScreen(vm) { add = true }
            }
            if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }

    if (add) PairDialog(vm) { add = false }
}

@Composable
private fun DashboardScreen(vm: MainVm) {
    val all = vm.dashboards.values
    val revenue = all.sumOf { it.today.revenue }
    val orders = all.sumOf { it.today.orders }
    val open = all.sumOf { it.openOrders }
    val paid = all.sumOf { it.paidToday }
    val yesterdayRevenue = all.sumOf { it.yesterday.revenue }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeroOverviewCard(revenue = revenue, orders = orders, open = open, paid = paid, yesterdayRevenue = yesterdayRevenue)
        }
        item {
            Text("Shops", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (vm.shops.isEmpty()) {
            item { EmptyState("Noch kein Shop gekoppelt") }
        }
        items(vm.shops) { shop ->
            val dashboard = vm.dashboards[shop]
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Blue.copy(0.10f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Outlined.Store, shop.name, tint = Blue, modifier = Modifier.padding(10.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(shop.name, fontWeight = FontWeight.Bold)
                            Text(shop.url, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(if (dashboard != null) money(dashboard.today.revenue) else "–", fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallInfoPill("Heute", "${dashboard?.today?.orders ?: 0} Bestellungen", Modifier.weight(1f))
                        SmallInfoPill("Offen", "${dashboard?.openOrders ?: 0}", Modifier.weight(1f))
                        SmallInfoPill("Bezahlt", "${dashboard?.paidToday ?: 0}", Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroOverviewCard(revenue: Double, orders: Int, open: Int, paid: Int, yesterdayRevenue: Double) {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Heute", style = MaterialTheme.typography.labelLarge, color = Blue)
            Text(money(revenue), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (yesterdayRevenue > 0) "Gestern ${money(yesterdayRevenue)}" else "Neuer Tag · Live aus deinen Shops",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Bestellungen", orders.toString(), Icons.Outlined.ShoppingBag, Modifier.weight(1f))
                MetricCard("Offen", open.toString(), Icons.Outlined.PendingActions, Modifier.weight(1f))
                MetricCard("Bezahlt", paid.toString(), Icons.Outlined.CheckCircle, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, label, tint = Blue)
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SmallInfoPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Blue.copy(0.06f), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Blue)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatisticsScreen(vm: MainVm) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember(ctx) { Repository(ctx) }
    var range by remember { mutableStateOf("week") }
    var selectedShopId by remember(vm.shops) { mutableStateOf<String?>(null) }
    var stats by remember { mutableStateOf<StatisticsRange?>(null) }
    var busy by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(range, selectedShopId, vm.shops) {
        if (vm.shops.isEmpty()) {
            stats = null
            return@LaunchedEffect
        }
        busy = true
        loadError = null
        val result = runCatching {
            if (selectedShopId == null) {
                repo.statisticsAll(range)
            } else {
                vm.shops.firstOrNull { it.shopId == selectedShopId }?.let { repo.statistics(it, range) }
            }
        }
        stats = result.getOrNull()
        loadError = result.exceptionOrNull()?.message
        busy = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Statistik", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("week" to "Woche", "month" to "Monat", "year" to "Jahr").forEach { (key, label) ->
                    FilterChip(selected = range == key, onClick = { range = key }, label = { Text(label) })
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedShopId == null, onClick = { selectedShopId = null }, label = { Text("Alle Shops") })
                vm.shops.forEach { shop ->
                    FilterChip(selected = selectedShopId == shop.shopId, onClick = { selectedShopId = shop.shopId }, label = { Text(shop.name) })
                }
            }
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        loadError?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        if (vm.shops.isEmpty()) {
            item { EmptyState("Noch kein Shop gekoppelt") }
        } else if (stats != null) {
            item {
                Text(stats?.label ?: "", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Umsatz", money(stats!!.summary.revenue), Icons.Outlined.Euro, Modifier.weight(1f))
                    MetricCard("Bestellungen", stats!!.summary.orders.toString(), Icons.Outlined.ShoppingBag, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Ø Warenkorb", money(stats!!.summary.averageOrderValue), Icons.Outlined.ShowChart, Modifier.weight(1f))
                    MetricCard("Offen", stats!!.summary.openOrders.toString(), Icons.Outlined.PendingActions, Modifier.weight(1f))
                }
            }
            item {
                ComparisonCard(stats!!)
            }
            item {
                RevenueChartCard(stats!!)
            }
        }
    }
}

@Composable
private fun ComparisonCard(stats: StatisticsRange) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Vergleich zum vorherigen Zeitraum", fontWeight = FontWeight.Bold)
            ComparisonRow("Umsatz", stats.comparison.revenuePercent)
            ComparisonRow("Bestellungen", stats.comparison.ordersPercent)
            ComparisonRow("Ø Warenkorb", stats.comparison.averageOrderValuePercent)
        }
    }
}

@Composable
private fun ComparisonRow(label: String, value: Double?) {
    val text = when (value) {
        null -> "neu"
        else -> if (value > 0) "+${String.format(Locale.GERMANY, "%.1f", value)} %" else "${String.format(Locale.GERMANY, "%.1f", value)} %"
    }
    val color = when {
        value == null -> Blue
        value > 0 -> Positive
        value < 0 -> Negative
        else -> Color.Gray
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Text(text, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RevenueChartCard(stats: StatisticsRange) {
    val maxRevenue = remember(stats) { (stats.buckets.maxOfOrNull { it.revenue } ?: 0.0).coerceAtLeast(1.0) }
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.QueryStats, null, tint = Blue)
                Spacer(Modifier.width(8.dp))
                Text("Umsatzverlauf", fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                stats.buckets.forEach { bucket ->
                    RevenueBar(bucket, maxRevenue)
                }
            }
            Text("Balkenhöhe = Umsatz pro Zeitraum", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
private fun RevenueBar(bucket: StatBucket, maxRevenue: Double) {
    val fraction = (bucket.revenue / maxRevenue).coerceIn(0.04, 1.0)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 28.dp)) {
        Text(if (bucket.revenue > 0.0) moneyShort(bucket.revenue) else "0", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.height(150.dp).width(26.dp), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((130 * fraction).dp)
                    .background(Blue, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp)),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(bucket.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text("${bucket.orders}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
private fun OrdersScreen(vm: MainVm) {
    val scope = rememberCoroutineScope()
    var selected by remember(vm.shops) { mutableStateOf(vm.shops.firstOrNull()) }
    var list by remember { mutableStateOf<List<OrderSummary>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<OrderDetail?>(null) }
    var busy by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember(ctx) { Repository(ctx) }

    fun reload() {
        val shop = selected ?: return
        scope.launch {
            busy = true
            list = runCatching { repo.orders(shop, search) }.getOrDefault(emptyList())
            busy = false
        }
    }

    LaunchedEffect(selected) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (vm.shops.isEmpty()) {
            EmptyState("Noch kein Shop gekoppelt")
            return@Column
        }
        ShopPicker(vm.shops, selected) { selected = it }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Kunde oder Bestellnummer suchen") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = { IconButton(onClick = { reload() }) { Icon(Icons.Outlined.ArrowForward, null) } },
        )
        Spacer(Modifier.height(10.dp))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { order ->
                OrderCard(order) {
                    val shop = selected ?: return@OrderCard
                    scope.launch { detail = repo.order(shop, order.id) }
                }
            }
        }
    }

    if (detail != null && selected != null) {
        OrderDetailDialog(detail!!, selected!!, { detail = null }) { group, action ->
            scope.launch {
                repo.transition(selected!!, detail!!.id, group, action)
                detail = repo.order(selected!!, detail!!.id)
            }
        }
    }
}

@Composable
private fun ShopPicker(shops: List<ShopConnection>, selected: ShopConnection?, onSelected: (ShopConnection) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.Store, null)
            Spacer(Modifier.width(8.dp))
            Text(selected?.name ?: "Shop wählen")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            shops.forEach { shop ->
                DropdownMenuItem(text = { Text(shop.name) }, onClick = {
                    onSelected(shop)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun OrderCard(order: OrderSummary, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("#${order.number}", fontWeight = FontWeight.Bold)
                    Text(order.customer, color = Color.Gray)
                }
                Text(money(order.amount), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusChip(order.orderState.label.ifBlank { order.orderState.technical })
                StatusChip(order.paymentState.label.ifBlank { order.paymentState.technical })
                StatusChip(order.deliveryState.label.ifBlank { order.deliveryState.technical })
            }
            Spacer(Modifier.height(8.dp))
            Text("${order.positions} Positionen · ${formatDate(order.date)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
private fun StatusChip(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = Blue.copy(0.08f)) {
        Text(text.ifBlank { "–" }, Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Blue)
    }
}

@Composable
private fun OrderDetailDialog(order: OrderDetail, shop: ShopConnection, onDismiss: () -> Unit, onTransition: (String, String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
        title = {
            Column {
                Text("Bestellung #${order.number}")
                Text(shop.name, style = MaterialTheme.typography.labelMedium, color = Blue)
            }
        },
        text = {
            LazyColumn(Modifier.heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row {
                        Text(money(order.amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(formatDate(order.date), color = Color.Gray)
                    }
                }
                item {
                    Text("Status", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusChip(order.orderState.label)
                        StatusChip(order.paymentState.label)
                        StatusChip(order.deliveryState.label)
                    }
                }
                items(order.items) { item ->
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(item.label, fontWeight = FontWeight.SemiBold)
                            Text("${item.quantity} × ${money(item.unitPrice)} · ${item.productNumber}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Text(money(item.totalPrice))
                    }
                }
                order.customer?.let { customer ->
                    item {
                        HorizontalDivider()
                        Text("Kunde", fontWeight = FontWeight.Bold)
                        Text("${customer.firstName} ${customer.lastName}")
                        Text(customer.email, color = Blue)
                        customer.shipping?.let { address ->
                            Text("${address.street}, ${address.zipcode} ${address.city}\n${address.country}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    HorizontalDivider()
                    Text("Schnellaktionen", fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = { onTransition("payment", "paid") }, label = { Text("Als bezahlt") })
                        AssistChip(onClick = { onTransition("delivery", "ship") }, label = { Text("Versendet") })
                        AssistChip(onClick = { onTransition("order", "complete") }, label = { Text("Abschließen") })
                    }
                }
            }
        },
    )
}

@Composable
private fun ShopsScreen(vm: MainVm, onAdd: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gekoppelte Shops", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Button(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Hinzufügen")
                }
            }
        }
        if (vm.shops.isEmpty()) {
            item { EmptyState("Noch kein Shop gekoppelt") }
        }
        items(vm.shops) { shop ->
            Card {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VerifiedUser, null, tint = Blue)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(shop.name, fontWeight = FontWeight.Bold)
                        Text(shop.url, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Direktverbindung · eigener Geräteschlüssel", style = MaterialTheme.typography.labelSmall, color = Blue)
                    }
                    IconButton(onClick = { vm.remove(shop.shopId) }) {
                        Icon(Icons.Outlined.Delete, "Entfernen")
                    }
                }
            }
        }
    }
}

@Composable
private fun PairDialog(vm: MainVm, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(android.os.Build.MODEL.ifBlank { "Android-Gerät" }) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shop koppeln") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Die App verbindet sich anschließend direkt per HTTPS mit deinem Shop.", color = Color.Gray)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Shop-URL") }, placeholder = { Text("https://www.vapetrade.de") })
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Einmaliger Kopplungscode") })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Gerätename") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        confirmButton = {
            Button(
                enabled = !busy && url.isNotBlank() && code.isNotBlank(),
                onClick = {
                    scope.launch {
                        busy = true
                        val result = vm.pair(url, code, name)
                        busy = false
                        result.onSuccess {
                            vm.refresh()
                            onDismiss()
                        }.onFailure { error = it.message }
                    }
                },
            ) {
                Text(if (busy) "Verbinden…" else "Sicher koppeln")
            }
        },
    )
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Store, null, tint = Color.Gray, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(8.dp))
            Text(text, color = Color.Gray)
        }
    }
}

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(value)
private fun moneyShort(value: Double): String = if (value >= 1000) String.format(Locale.GERMANY, "%.1fk", value / 1000.0) else String.format(Locale.GERMANY, "%.0f", value)
private fun formatDate(value: String): String = runCatching { OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")) }.getOrDefault(value)
