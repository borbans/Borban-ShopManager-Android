@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.borban.shopmanager

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingFlat
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.borban.shopmanager.data.Dashboard
import de.borban.shopmanager.data.OrderDetail
import de.borban.shopmanager.data.OrderSummary
import de.borban.shopmanager.data.Repository
import de.borban.shopmanager.data.ShopConnection
import de.borban.shopmanager.data.StatBucket
import de.borban.shopmanager.data.StatisticsRange
import de.borban.shopmanager.data.connectionKey
import de.borban.shopmanager.push.PushCoordinator
import de.borban.shopmanager.push.PushNavigation
import de.borban.shopmanager.push.PushTarget
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val Brand = Color(0xFF0A78AE)
private val BrandDeep = Color(0xFF075B8B)
private val BrandDark = Color(0xFF073D63)
private val Aqua = Color(0xFF20B6C9)
private val Ink = Color(0xFF13233A)
private val Muted = Color(0xFF6E7B8D)
private val Canvas = Color(0xFFF3F7FA)
private val CardSurface = Color(0xFFFFFFFF)
private val Line = Color(0xFFE1E9EF)
private val Positive = Color(0xFF12835C)
private val Negative = Color(0xFFC34242)
private val Warning = Color(0xFFC27A16)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PushNavigation.openFromIntent(intent)
        setContent {
            BorbanTheme {
                PermissionGate()
                ShopManagerUi()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PushNavigation.openFromIntent(intent)
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
            primary = Brand,
            secondary = Aqua,
            background = Canvas,
            surface = CardSurface,
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
            dashboards = runCatching { repo.dashboards() }
                .onFailure { error = it.message }
                .getOrDefault(emptyMap())
            shops = repo.shops()
            loading = false
        }
    }

    suspend fun pair(url: String, code: String, name: String) = repo.pair(url, code, name).also { shops = repo.shops() }

    fun remove(connectionKey: String) {
        repo.remove(connectionKey)
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
    var statisticsRange by remember { mutableStateOf("day") }
    var statisticsShopKey by remember { mutableStateOf<String?>(null) }
    val pushTarget = PushNavigation.target

    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(pushTarget) { if (pushTarget != null) tab = 2 }

    Scaffold(
        containerColor = Canvas,
        topBar = { PremiumHeader(onRefresh = { vm.refresh() }, onAdd = { add = true }) },
        bottomBar = { PremiumBottomBar(tab = tab, onTab = { tab = it }) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> DashboardScreen(vm) { shopKey ->
                    statisticsShopKey = shopKey
                    statisticsRange = "day"
                    tab = 1
                }
                1 -> StatisticsScreen(
                    vm = vm,
                    range = statisticsRange,
                    onRangeChange = { statisticsRange = it },
                    selectedShopKey = statisticsShopKey,
                    onShopChange = { statisticsShopKey = it },
                )
                2 -> OrdersScreen(vm, pushTarget) { PushNavigation.consume() }
                else -> ShopsScreen(vm) { add = true }
            }
            if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Aqua)
        }
    }

    if (add) PairDialog(vm) { add = false }
}

@Composable
private fun PremiumHeader(onRefresh: () -> Unit, onAdd: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(BrandDeep, Brand, Aqua)))
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Borban ShopManager",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF8EF1C8)))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "LIVE · Read-only · sichere Direktverbindung",
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HeaderAction(Icons.Outlined.Refresh, "Aktualisieren", onRefresh)
            Spacer(Modifier.width(5.dp))
            HeaderAction(Icons.Outlined.Add, "Shop hinzufügen", onAdd)
        }
    }
}

@Composable
private fun HeaderAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(icon, description, tint = Color.White)
        }
    }
}

@Composable
private fun PremiumBottomBar(tab: Int, onTab: (Int) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        listOf(
            "Übersicht" to Icons.Outlined.Dashboard,
            "Statistik" to Icons.Outlined.QueryStats,
            "Bestellungen" to Icons.Outlined.ReceiptLong,
            "Shops" to Icons.Outlined.Store,
        ).forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = tab == index,
                onClick = { onTab(index) },
                icon = { Icon(icon, label) },
                label = { Text(label, maxLines = 1, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandDark,
                    selectedTextColor = BrandDark,
                    indicatorColor = Color(0xFFDDF2FA),
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                ),
            )
        }
    }
}

@Composable
private fun DashboardScreen(vm: MainVm, onOpenStatistics: (String) -> Unit) {
    val dashboards = vm.dashboards.values
    val revenue = dashboards.sumOf { it.today.revenue }
    val orders = dashboards.sumOf { it.today.orders }
    val open = dashboards.sumOf { it.openOrders }
    val paid = dashboards.sumOf { it.paidToday }
    val yesterdayRevenue = dashboards.sumOf { it.yesterday.revenue }
    val updatedAt = dashboards.map { it.generatedAt }.maxOrNull()
    var sortMode by remember { mutableStateOf("orders") }

    val sortItems = vm.shops.map { shop ->
        val dashboard = vm.dashboards[shop]
        ShopSortItem(
            id = shop.connectionKey(),
            name = shop.name,
            orders = dashboard?.today?.orders ?: 0,
            revenue = dashboard?.today?.revenue ?: 0.0,
            open = dashboard?.openOrders ?: 0,
        )
    }
    val sortedIds = sortShopItems(sortItems, sortMode).map { it.id }
    val sortedShops = sortedIds.mapNotNull { id -> vm.shops.firstOrNull { it.connectionKey() == id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            DashboardHero(revenue, orders, open, paid, yesterdayRevenue)
        }
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    SectionTitle("Shops", "Antippen öffnet direkt die Shop-Statistik")
                    updatedAt?.let { Text("Aktualisiert ${formatUpdatedAt(it)}", color = Muted, fontSize = 10.sp) }
                }
            }
        }
        item { DashboardSortSelector(sortMode, onSelect = { sortMode = it }) }
        if (vm.shops.isEmpty()) {
            item { EmptyState("Noch kein Shop gekoppelt") }
        }
        items(sortedShops, key = { it.connectionKey() }) { shop ->
            ShopDashboardCard(shop, vm.dashboards[shop]) { onOpenStatistics(shop.connectionKey()) }
        }
    }
}

@Composable
private fun DashboardHero(revenue: Double, orders: Int, open: Int, paid: Int, yesterdayRevenue: Double) {
    val change = percentChange(revenue, yesterdayRevenue)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(BrandDark, BrandDeep, Brand)))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("HEUTE", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(money(revenue), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    Text(
                        if (yesterdayRevenue > 0) "Gestern ${money(yesterdayRevenue)}" else "Live aus allen verbundenen Shops",
                        color = Color.White.copy(alpha = 0.76f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TrendBadge(change)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroMetric("Bestellungen", orders.toString(), Icons.Outlined.ShoppingBag, Modifier.weight(1f))
                HeroMetric("Offen", open.toString(), Icons.Outlined.PendingActions, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroMetric("Bezahlt", paid.toString(), Icons.Outlined.CheckCircle, Modifier.weight(1f))
                HeroMetric("Ø Warenkorb", if (orders > 0) money(revenue / orders) else money(0.0), Icons.Outlined.ShowChart, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color.White.copy(alpha = 0.11f), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color.White.copy(alpha = 0.13f), shape = RoundedCornerShape(12.dp)) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(label, color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ShopDashboardCard(shop: ShopConnection, dashboard: Dashboard?, onClick: () -> Unit) {
    PremiumCard(onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFE0F3FA), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Outlined.Store, shop.name, tint = Brand, modifier = Modifier.padding(11.dp).size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(shop.name, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(shop.url.removePrefix("https://").removePrefix("http://"), color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (dashboard != null) money(dashboard.today.revenue) else "–", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1)
                    Text("heute", color = Muted, fontSize = 11.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ShopStat("Bestellungen", (dashboard?.today?.orders ?: 0).toString(), Modifier.weight(1f))
                ShopStat("Offen", (dashboard?.openOrders ?: 0).toString(), Modifier.weight(1f))
                ShopStat("Bezahlt", (dashboard?.paidToday ?: 0).toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ShopStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color(0xFFF3F8FB), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(label, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DashboardSortSelector(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "orders" to "Bestellungen",
            "revenue" to "Umsatz",
            "open" to "Offen",
            "az" to "A–Z",
        ).forEach { (key, label) ->
            PremiumFilterChip(label, selected == key) { onSelect(key) }
        }
    }
}

@Composable
private fun StatisticsScreen(
    vm: MainVm,
    range: String,
    onRangeChange: (String) -> Unit,
    selectedShopKey: String?,
    onShopChange: (String?) -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember(ctx) { Repository(ctx) }
    var stats by remember { mutableStateOf<StatisticsRange?>(null) }
    var busy by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vm.shops, selectedShopKey) {
        if (selectedShopKey != null && vm.shops.none { it.connectionKey() == selectedShopKey }) {
            onShopChange(null)
        }
    }

    LaunchedEffect(range, selectedShopKey, vm.shops) {
        if (vm.shops.isEmpty()) {
            stats = null
            return@LaunchedEffect
        }
        busy = true
        loadError = null
        val result = runCatching {
            if (selectedShopKey == null) repo.statisticsAll(range)
            else vm.shops.firstOrNull { it.connectionKey() == selectedShopKey }?.let { repo.statistics(it, range) }
        }
        stats = result.getOrNull()
        loadError = result.exceptionOrNull()?.message
        busy = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionTitle("Statistik", "Umsatz, Bestellungen und Entwicklung") }
        item { PeriodSelector(range, onSelect = onRangeChange) }
        item { ShopSelector(vm.shops, selectedShopKey, onSelect = onShopChange) }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Aqua) }
        loadError?.let { error -> item { ErrorBanner(error) } }
        if (vm.shops.isEmpty()) {
            item { EmptyState("Noch kein Shop gekoppelt") }
        } else if (stats != null) {
            item { StatisticsHero(stats!!) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile("Bestellungen", stats!!.summary.orders.toString(), Icons.Outlined.ShoppingBag, Modifier.weight(1f))
                    MetricTile("Ø Warenkorb", money(stats!!.summary.averageOrderValue), Icons.Outlined.ShowChart, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile("Offene Orders", stats!!.summary.openOrders.toString(), Icons.Outlined.PendingActions, Modifier.weight(1f))
                    MetricTile("Vorperiode", money(stats!!.previous.revenue), Icons.Outlined.QueryStats, Modifier.weight(1f))
                }
            }
            item { ComparisonCard(stats!!) }
            item { RevenueChartCard(stats!!) }
        }
    }
}

@Composable
private fun PeriodSelector(selected: String, onSelect: (String) -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) {
        Row(Modifier.fillMaxWidth().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("day" to "Tag", "week" to "Woche", "month" to "Monat", "year" to "Jahr").forEach { (key, label) ->
                val active = selected == key
                Surface(
                    onClick = { onSelect(key) },
                    modifier = Modifier.weight(1f),
                    color = if (active) BrandDark else Color.Transparent,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        color = if (active) Color.White else Ink,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopSelector(shops: List<ShopConnection>, selectedShopKey: String?, onSelect: (String?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PremiumFilterChip("Alle Shops", selectedShopKey == null) { onSelect(null) }
        shops.forEach { shop -> PremiumFilterChip(shop.name, selectedShopKey == shop.connectionKey()) { onSelect(shop.connectionKey()) } }
    }
}

@Composable
private fun PremiumFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = if (selected) ({ Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(17.dp)) }) else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFDDF2FA),
            selectedLabelColor = BrandDark,
            selectedLeadingIconColor = Brand,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Line,
            selectedBorderColor = Color(0xFFB8DCEB),
        ),
    )
}

@Composable
private fun StatisticsHero(stats: StatisticsRange) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BrandDark, BrandDeep)))
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stats.label.uppercase(Locale.GERMANY), color = Color.White.copy(alpha = 0.67f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(money(stats.summary.revenue), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    Text("Umsatz", color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp)
                }
                TrendBadge(stats.comparison.revenuePercent)
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    PremiumCard(modifier.heightIn(min = 118.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(color = Color(0xFFE1F3FA), shape = RoundedCornerShape(12.dp)) {
                Icon(icon, null, tint = Brand, modifier = Modifier.padding(8.dp).size(21.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(value, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ComparisonCard(stats: StatisticsRange) {
    PremiumCard {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            SectionMiniTitle("Vergleich zur Vorperiode")
            ComparisonRow("Umsatz", stats.comparison.revenuePercent, money(stats.previous.revenue))
            HorizontalDivider(color = Line)
            ComparisonRow("Bestellungen", stats.comparison.ordersPercent, "${stats.previous.orders}")
            HorizontalDivider(color = Line)
            ComparisonRow("Ø Warenkorb", stats.comparison.averageOrderValuePercent, money(stats.previous.averageOrderValue))
        }
    }
}

@Composable
private fun ComparisonRow(label: String, value: Double?, previous: String) {
    val trendColor = trendColor(value)
    val icon = when {
        value == null || value == 0.0 -> Icons.Outlined.TrendingFlat
        value > 0 -> Icons.Outlined.TrendingUp
        else -> Icons.Outlined.TrendingDown
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = trendColor.copy(alpha = 0.10f), shape = RoundedCornerShape(12.dp)) {
            Icon(icon, null, tint = trendColor, modifier = Modifier.padding(8.dp).size(20.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("Vorher $previous", color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(percentText(value), color = trendColor, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun RevenueChartCard(stats: StatisticsRange) {
    var mode by remember { mutableStateOf("revenue") }
    val maxValue = remember(stats, mode) {
        if (mode == "revenue") stats.buckets.maxOfOrNull { it.revenue } ?: 0.0
        else stats.buckets.maxOfOrNull { it.orders.toDouble() } ?: 0.0
    }.coerceAtLeast(1.0)

    PremiumCard {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionMiniTitle(if (mode == "revenue") "Umsatzverlauf" else "Bestellverlauf")
                    Text("${stats.buckets.size} Zeitpunkte", color = Muted, fontSize = 11.sp)
                }
                MiniChartToggle(mode, onChange = { mode = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(if (stats.buckets.size <= 12) 10.dp else 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                stats.buckets.forEachIndexed { index, bucket ->
                    PremiumBar(bucket, maxValue, mode, showLabel = stats.buckets.size <= 12 || index % 5 == 0 || index == stats.buckets.lastIndex)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Aqua))
                Spacer(Modifier.width(6.dp))
                Text(if (mode == "revenue") "Balkenhöhe = Umsatz" else "Balkenhöhe = Bestellungen", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun MiniChartToggle(selected: String, onChange: (String) -> Unit) {
    Surface(color = Color(0xFFF1F6F9), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(3.dp)) {
            listOf("revenue" to "€", "orders" to "#").forEach { (key, label) ->
                Surface(
                    onClick = { onChange(key) },
                    color = if (selected == key) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(9.dp),
                ) {
                    Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (selected == key) BrandDark else Muted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PremiumBar(bucket: StatBucket, maxValue: Double, mode: String, showLabel: Boolean) {
    val value = if (mode == "revenue") bucket.revenue else bucket.orders.toDouble()
    val fraction = (value / maxValue).coerceIn(if (value > 0) 0.05 else 0.0, 1.0)
    val width = if (mode == "revenue") 30.dp else 28.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 34.dp)) {
        Text(
            when {
                value <= 0 -> ""
                mode == "revenue" -> moneyCompact(value)
                else -> value.toInt().toString()
            },
            color = Muted,
            fontSize = 9.sp,
            maxLines = 1,
        )
        Spacer(Modifier.height(5.dp))
        Box(Modifier.height(146.dp).width(width).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF0F5F8)), contentAlignment = Alignment.BottomCenter) {
            if (fraction > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((132 * fraction).dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(Aqua, Brand))),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(if (showLabel) bucket.label else "", color = Muted, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun OrdersScreen(vm: MainVm, pushTarget: PushTarget?, onPushConsumed: () -> Unit) {
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
    LaunchedEffect(pushTarget, vm.shops) {
        val target = pushTarget ?: return@LaunchedEffect
        val targetShop = vm.shops.firstOrNull { it.deviceId == target.deviceId } ?: return@LaunchedEffect
        selected = targetShop
        busy = true
        detail = runCatching { repo.order(targetShop, target.orderId) }.getOrNull()
        list = runCatching { repo.orders(targetShop, search) }.getOrDefault(emptyList())
        busy = false
        onPushConsumed()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        if (vm.shops.isEmpty()) {
            EmptyState("Noch kein Shop gekoppelt")
            return@Column
        }
        SectionTitle("Bestellungen", "Schneller Überblick ohne PC")
        Spacer(Modifier.height(12.dp))
        ShopPicker(vm.shops, selected) { selected = it }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            placeholder = { Text("Kunde oder Bestellnummer") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = { IconButton(onClick = { reload() }) { Icon(Icons.Outlined.ArrowForward, null) } },
        )
        Spacer(Modifier.height(12.dp))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Aqua)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 22.dp)) {
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
        OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Outlined.Store, null)
            Spacer(Modifier.width(8.dp))
            Text(selected?.name ?: "Shop wählen", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            shops.forEach { shop ->
                DropdownMenuItem(text = { Text(shop.name, maxLines = 1) }, onClick = {
                    onSelected(shop)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun OrderCard(order: OrderSummary, onClick: () -> Unit) {
    PremiumCard(onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("#${order.number}", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1)
                    Text(order.customer.ifBlank { "Gastbestellung" }, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(money(order.amount), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusChip(operationalStatus(order.orderState.technical, order.deliveryState.technical), emphasized = true)
                StatusChip("Zahlung: ${order.paymentState.label.ifBlank { order.paymentState.technical }}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${order.positions} ${if (order.positions == 1) "Position" else "Positionen"}", color = Muted, fontSize = 11.sp)
                Text("  ·  ", color = Line)
                Text(formatDate(order.date), color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, emphasized: Boolean = false) {
    val color = statusColor(text)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (emphasized) color.copy(alpha = 0.16f) else color.copy(alpha = 0.09f),
        border = if (emphasized) BorderStroke(1.dp, color.copy(alpha = 0.24f)) else null,
    ) {
        Text(
            text.ifBlank { "–" },
            Modifier.padding(horizontal = if (emphasized) 12.dp else 10.dp, vertical = 5.dp),
            fontSize = if (emphasized) 11.sp else 10.sp,
            fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
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
                Text(shop.name, style = MaterialTheme.typography.labelMedium, color = Brand)
            }
        },
        text = {
            LazyColumn(Modifier.heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row {
                        Text(money(order.amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(formatDate(order.date), color = Muted)
                    }
                }
                item {
                    Text("Status", fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusChip(operationalStatus(order.orderState.technical, order.deliveryState.technical), emphasized = true)
                        StatusChip("Zahlung: ${order.paymentState.label.ifBlank { order.paymentState.technical }}")
                        StatusChip("Lieferung: ${order.deliveryState.label.ifBlank { order.deliveryState.technical }}")
                    }
                }
                items(order.items) { item ->
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(item.label, fontWeight = FontWeight.SemiBold)
                            Text("${item.quantity} × ${money(item.unitPrice)} · ${item.productNumber}", style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                        Text(money(item.totalPrice))
                    }
                }
                order.customer?.let { customer ->
                    item {
                        HorizontalDivider()
                        Text("Kunde", fontWeight = FontWeight.Bold)
                        Text("${customer.firstName} ${customer.lastName}")
                        Text(customer.email, color = Brand)
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
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { SectionTitle("Shops", "Gekoppelte Geräteverbindungen") }
                Button(onClick = onAdd, shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Neu")
                }
            }
        }
        if (vm.shops.isEmpty()) item { EmptyState("Noch kein Shop gekoppelt") }
        items(vm.shops, key = { it.connectionKey() }) { shop ->
            var pushEnabled by remember(shop.deviceId) { mutableStateOf(PushCoordinator.isPushEnabled(ctx, shop)) }
            PremiumCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFE0F3FA), shape = RoundedCornerShape(15.dp)) {
                            Icon(Icons.Outlined.VerifiedUser, null, tint = Brand, modifier = Modifier.padding(10.dp).size(23.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(shop.name, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(shop.url.removePrefix("https://").removePrefix("http://"), color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(Positive))
                                Spacer(Modifier.width(5.dp))
                                Text("Direkt verbunden · eigener Geräteschlüssel", color = Positive, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = { vm.remove(shop.connectionKey()) }) { Icon(Icons.Outlined.Delete, "Entfernen", tint = Muted) }
                    }
                    HorizontalDivider(color = Line)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Notifications, null, tint = Brand, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Bestell-Push", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(if (pushEnabled) "Aktiv · Shopname, Betrag und Positionen" else "Für diesen Shop deaktiviert", color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(checked = pushEnabled, onCheckedChange = { enabled ->
                            pushEnabled = enabled
                            PushCoordinator.setPushEnabled(ctx, shop, enabled)
                        })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { PushCoordinator.sendTestNotification(ctx, shop) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Testton")
                        }
                        OutlinedButton(
                            onClick = { PushCoordinator.openChannelSettings(ctx, shop) },
                            modifier = Modifier.weight(1.65f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Outlined.Notifications, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ton & Vibration")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairDialog(vm: MainVm, onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(android.os.Build.MODEL.ifBlank { "Android-Gerät" }) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shop sicher koppeln") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Direkte HTTPS-Verbindung. Keine Shopware-Admin-Zugangsdaten auf dem Gerät.", color = Muted)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Shop-URL") }, placeholder = { Text("https://www.vapetrade.de") }, singleLine = true)
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Einmaliger Kopplungscode") }, singleLine = true)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Gerätename") }, singleLine = true)
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
                        result.onSuccess { shop ->
                            PushCoordinator.onShopPaired(ctx, shop)
                            vm.refresh()
                            onDismiss()
                        }.onFailure { error = it.message }
                    }
                },
            ) { Text(if (busy) "Verbinden…" else "Koppeln") }
        },
    )
}

@Composable
private fun PremiumCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = Color.White)
    val elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(22.dp), colors = colors, elevation = elevation, border = BorderStroke(1.dp, Line), content = { content() })
    } else {
        Card(modifier = modifier, shape = RoundedCornerShape(22.dp), colors = colors, elevation = elevation, border = BorderStroke(1.dp, Line), content = { content() })
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(subtitle, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SectionMiniTitle(title: String) {
    Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
}

@Composable
private fun TrendBadge(value: Double?) {
    val color = trendColor(value)
    val icon = when {
        value == null || value == 0.0 -> Icons.Outlined.TrendingFlat
        value > 0 -> Icons.Outlined.TrendingUp
        else -> Icons.Outlined.TrendingDown
    }
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (color == Muted) Color.White.copy(alpha = 0.8f) else color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text(percentText(value), color = if (color == Muted) Color.White.copy(alpha = 0.84f) else color, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(color = Negative.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Negative.copy(alpha = 0.18f))) {
        Text(message, Modifier.padding(14.dp), color = Negative, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyState(text: String) {
    PremiumCard {
        Box(Modifier.fillMaxWidth().padding(vertical = 38.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = Color(0xFFE5F3F9), shape = CircleShape) {
                    Icon(Icons.Outlined.Store, null, tint = Brand, modifier = Modifier.padding(13.dp).size(28.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(text, color = Muted, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun statusColor(text: String): Color {
    val lower = text.lowercase(Locale.GERMANY)
    return when {
        "bezahlt" in lower || "abgeschlossen" in lower || "erledigt" in lower || "versendet" in lower || "complete" in lower || "shipped" in lower || "paid" in lower -> Positive
        "bearbeitung" in lower || "progress" in lower -> Warning
        "storni" in lower || "cancel" in lower || "fehl" in lower -> Negative
        else -> Brand
    }
}

private fun trendColor(value: Double?): Color = when {
    value == null || value == 0.0 -> Muted
    value > 0 -> Positive
    else -> Negative
}

private fun percentText(value: Double?): String = when {
    value == null -> "neu"
    abs(value) < 0.05 -> "0,0 %"
    value > 0 -> "+${String.format(Locale.GERMANY, "%.1f", value)} %"
    else -> "${String.format(Locale.GERMANY, "%.1f", value)} %"
}

private fun percentChange(current: Double, previous: Double): Double? {
    if (previous == 0.0) return if (current == 0.0) 0.0 else null
    return ((current - previous) / previous) * 100.0
}

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(value)
private fun moneyCompact(value: Double): String = when {
    value >= 1_000_000 -> String.format(Locale.GERMANY, "%.1fM", value / 1_000_000.0)
    value >= 1_000 -> String.format(Locale.GERMANY, "%.1fk", value / 1_000.0)
    else -> String.format(Locale.GERMANY, "%.0f", value)
}
private fun formatUpdatedAt(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("–")

private fun formatDate(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"))
}.getOrDefault(value)
