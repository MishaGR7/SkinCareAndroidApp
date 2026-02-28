package com.example.skincare
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID


data class GlassThemeColors(
    val background: Color,
    val cardBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val accent: Color,
    val navBar: Color,
    val success: Color,
    val error: Color
)

val DarkGlass = GlassThemeColors(
    background = Color(0xFF121212),
    cardBg = Color(0xFF1E1E1E).copy(alpha = 0.7f),
    textPrimary = Color(0xFFEEEEEE),
    textSecondary = Color(0xFFAAAAAA),
    border = Color(0xFFFFFFFF).copy(alpha = 0.12f),
    accent = Color(0xFF00E5FF),
    navBar = Color(0xFF000000).copy(alpha = 0.85f),
    success = Color(0xFF00E676),
    error = Color(0xFFFF5252)
)

val LightGlass = GlassThemeColors(
    background = Color(0xFFF2F2F7),
    cardBg = Color(0xFFFFFFFF).copy(alpha = 0.75f),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF636366),
    border = Color(0xFF000000).copy(alpha = 0.08f),
    accent = Color(0xFF007AFF),
    navBar = Color(0xFFFFFFFF).copy(alpha = 0.9f),
    success = Color(0xFF34C759),
    error = Color(0xFFFF3B30)
)


enum class ProductType(val label: String, val colorDark: Color, val colorLight: Color) {
    CLEANSER("Очищення (Cleanser)", Color(0xFF5E5CE6), Color(0xFF5E5CE6)),
    RECOVERY("Відновлення (Recovery)", Color(0xFF00D2BE), Color(0xFF30D158)),
    RETINOL("Ретинол (Retinol)", Color(0xFFAF52DE), Color(0xFFAF52DE)),
    ENZYME("Кислоти (Acids)", Color(0xFFFF9F0A), Color(0xFFFF9F0A)),
    PEELING("Пілінг (Peeling)", Color(0xFFFF453A), Color(0xFFFF453A)),
    OTHER("Інше (Other)", Color.Gray, Color.Gray)
}

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ProductType,
    val cooldownDays: Int
)

data class HistoryEntry(
    val date: String,
    val time: String,
    val dayTitle: String,
    val productsUsedIds: List<String>
)

data class RoutineStep(
    val dayNumber: String,
    val description: String,
    val targetTypes: List<ProductType>,
    val color: Color
)

class DataManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("SkinCycleGlassV3", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveProducts(list: List<Product>) = prefs.edit().putString("PRODS", gson.toJson(list)).apply()
    fun getProducts(): List<Product> {
        val json = prefs.getString("PRODS", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<Product>>() {}.type)
    }

    fun saveHistory(list: List<HistoryEntry>) = prefs.edit().putString("HIST", gson.toJson(list)).apply()
    fun getHistory(): List<HistoryEntry> {
        val json = prefs.getString("HIST", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<HistoryEntry>>() {}.type)
    }

    fun clearHistory() = prefs.edit().remove("HIST").apply()
    fun getStartDate(): String = prefs.getString("START", LocalDate.now().toString()) ?: LocalDate.now().toString()

    fun isDarkTheme(): Boolean = prefs.getBoolean("IS_DARK", true)
    fun setDarkTheme(isDark: Boolean) = prefs.edit().putBoolean("IS_DARK", isDark).apply()
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlassApp()
        }
    }
}

@Composable
fun GlassApp() {
    val context = LocalContext.current
    val dataManager = remember { DataManager(context) }

    var isDarkTheme by remember { mutableStateOf(dataManager.isDarkTheme()) }
    var selectedTab by remember { mutableStateOf(0) }
    val products = remember { mutableStateListOf<Product>().apply { addAll(dataManager.getProducts()) } }
    val history = remember { mutableStateListOf<HistoryEntry>().apply { addAll(dataManager.getHistory()) } }

    val targetTheme = if (isDarkTheme) DarkGlass else LightGlass
    val bgColor by animateColorAsState(targetTheme.background, label = "bg", animationSpec = tween(500))

    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        Scaffold(
            containerColor = bgColor,
            bottomBar = {
                GlassBottomBar(selectedTab, targetTheme) { selectedTab = it }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Theme Toggle
                ThemeToggle(
                    isDark = isDarkTheme,
                    theme = targetTheme,
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).zIndex(10f),
                    onToggle = {
                        isDarkTheme = !isDarkTheme
                        dataManager.setDarkTheme(isDarkTheme)
                    }
                )

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        0 -> DashboardScreen(dataManager, products, history, targetTheme, isDarkTheme) { entry ->
                            history.add(0, entry)
                            dataManager.saveHistory(history)
                        }
                        1 -> InventoryScreen(products, history, targetTheme, isDarkTheme) { dataManager.saveProducts(it) }
                        2 -> HistoryScreen(history, products, targetTheme, isDarkTheme) {
                            history.clear()
                            dataManager.clearHistory()
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 1: DASHBOARD ---

@Composable
fun DashboardScreen(
    dm: DataManager,
    allProducts: List<Product>,
    history: List<HistoryEntry>,
    theme: GlassThemeColors,
    isDark: Boolean,
    onLog: (HistoryEntry) -> Unit
) {
    val routine = getRoutine()
    val start = LocalDate.parse(dm.getStartDate())
    val today = LocalDate.now()
    val daysPassed = ChronoUnit.DAYS.between(start, today).toInt()
    val currentStep = routine[if (daysPassed >= 0) (daysPassed % routine.size) else 0]

    val availableProducts = allProducts.filter { product ->
        val usageDates = history.filter { it.productsUsedIds.contains(product.id) }
            .mapNotNull { try { LocalDate.parse(it.date) } catch(e: Exception) { null } }

        if (usageDates.isEmpty()) true
        else {
            val lastUsageDate = usageDates.maxOrNull() ?: LocalDate.MIN
            val daysSince = ChronoUnit.DAYS.between(lastUsageDate, today)
            if (product.cooldownDays == 0) true
            else daysSince > product.cooldownDays
        }
    }.sortedByDescending {
        currentStep.targetTypes.contains(it.type) || it.type == ProductType.CLEANSER || it.type == ProductType.RECOVERY
    }

    val selectedIds = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            color = theme.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Minimalist Header (Without Phase Pill as requested)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Сьогодні (Today)",
                color = theme.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        if (availableProducts.isEmpty()) {
            GlassEmptyState("Всі активи на відпочинку або полиця порожня.", theme)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(availableProducts) { product ->
                val isSelected = selectedIds.contains(product.id)
                val isRecommended = currentStep.targetTypes.contains(product.type) ||
                        product.type == ProductType.CLEANSER ||
                        product.type == ProductType.RECOVERY

                GlassProductCard(product, isSelected, isRecommended, theme, isDark) {
                    if (isSelected) selectedIds.remove(product.id) else selectedIds.add(product.id)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                GlassButton("Зберегти догляд", currentStep.color, selectedIds.isNotEmpty()) {
                    val currentTime = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    onLog(HistoryEntry(today.toString(), currentTime, "", selectedIds.toList())) // Empty string for phase desc
                    selectedIds.clear()
                }
            }
        }
    }
}

// --- SCREEN 2: INVENTORY ---

@Composable
fun InventoryScreen(
    products: MutableList<Product>,
    history: List<HistoryEntry>,
    theme: GlassThemeColors,
    isDark: Boolean,
    onUpdate: (List<Product>) -> Unit
) {
    var showAddSheet by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Spacer(modifier = Modifier.height(30.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Полиця (Shelf)", color = theme.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { showAddSheet = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(theme.accent.copy(alpha = 0.2f))
                        .size(44.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = theme.accent)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (products.isEmpty()) GlassEmptyState("Полиця порожня.", theme)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(products) { product ->
                    val usageDates = history.filter { it.productsUsedIds.contains(product.id) }
                        .mapNotNull { try { LocalDate.parse(it.date) } catch(e: Exception) { null } }

                    var status = "Доступно (Ready)"
                    var statusColor = theme.success

                    if (usageDates.isNotEmpty() && product.cooldownDays > 0) {
                        val lastUsage = usageDates.maxOrNull() ?: LocalDate.MIN
                        val daysSince = ChronoUnit.DAYS.between(lastUsage, today)

                        if (daysSince <= product.cooldownDays) {
                            val daysLeft = (product.cooldownDays - daysSince) + 1
                            status = "Пауза: ще $daysLeft дн."
                            statusColor = theme.error
                        }
                    }

                    GlassSwipeCard(product, status, statusColor, theme, isDark) {
                        products.remove(product)
                        onUpdate(products)
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddProductGlassSheet(theme, isDark, onDismiss = { showAddSheet = false }) { p ->
            products.add(p)
            onUpdate(products)
            showAddSheet = false
        }
    }
}

// --- SCREEN 3: HISTORY (REDESIGNED) ---

@Composable
fun HistoryScreen(
    history: List<HistoryEntry>,
    allProducts: List<Product>,
    theme: GlassThemeColors,
    isDark: Boolean,
    onClear: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
        Spacer(modifier = Modifier.height(30.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Історія (Log)", color = theme.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            if (history.isNotEmpty()) {
                IconButton(onClick = { showConfirm = true }) {
                    Icon(Icons.Default.Delete, null, tint = theme.textSecondary)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        if (history.isEmpty()) GlassEmptyState("Історія чиста.", theme)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
            items(history) { entry ->
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    // Left Timeline
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(50.dp).fillMaxHeight()
                    ) {
                        // Date parsing simplified
                        val date = try { LocalDate.parse(entry.date) } catch(e:Exception) { LocalDate.now() }
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("d")),
                            color = theme.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("MMM")),
                            color = theme.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.width(2.dp).weight(1f).background(theme.border))
                    }

                    // Right Content Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(theme.cardBg)
                            .border(1.dp, theme.border, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            // Time badge
                            Text(
                                text = entry.time,
                                color = theme.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(theme.background.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // VISUAL PRODUCT CHIPS
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            entry.productsUsedIds.forEach { id ->
                                val p = allProducts.find { it.id == id }
                                p?.let {
                                    val pColor = if(isDark) it.type.colorDark else it.type.colorLight
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(pColor.copy(alpha = 0.2f))
                                            .border(1.dp, pColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(8.dp).clip(CircleShape).background(pColor)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(it.name, color = theme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.weight(1f))
                                        // Optional: Small type label
                                        Text(it.type.label.split("(")[0], color = pColor, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            containerColor = theme.background,
            onDismissRequest = { showConfirm = false },
            title = { Text("Очистити?", color = theme.textPrimary) },
            text = { Text("Видалити всю історію безповоротно?", color = theme.textSecondary) },
            confirmButton = {
                Button(
                    onClick = { onClear(); showConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.error)
                ) { Text("Видалити", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Ні", color = theme.textPrimary) } }
        )
    }
}

// --- UI COMPONENTS ---

@Composable
fun ThemeToggle(isDark: Boolean, theme: GlassThemeColors, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val bgColor by animateColorAsState(if (isDark) Color(0xFF1E1E1E) else Color.White, label = "toggleBg")
    val iconColor by animateColorAsState(if (isDark) Color.Yellow else Color(0xFFFF9800), label = "toggleIcon")

    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(bgColor.copy(alpha=0.8f))
                .border(1.dp, theme.border, CircleShape)
                .clickable(onClick = onToggle)
                .padding(10.dp)
        ) {
            Icon(
                if (isDark) Icons.Default.Settings else Icons.Default.Star,
                contentDescription = "Theme",
                tint = theme.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun GlassProductCard(
    product: Product,
    isSelected: Boolean,
    isRecommended: Boolean,
    theme: GlassThemeColors,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val prodColor = if (isDark) product.type.colorDark else product.type.colorLight
    val borderColor by animateColorAsState(if (isSelected) prodColor else theme.border, label = "border")
    val bgAlpha = if (isSelected) 0.15f else 0.0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(prodColor.copy(alpha = bgAlpha))
            .background(theme.cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) prodColor else Color.Transparent)
                .border(2.dp, if (isSelected) prodColor else theme.textSecondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, color = theme.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(product.type.label, color = theme.textSecondary, fontSize = 12.sp)
        }

        if (isRecommended) Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700))
        else if (product.cooldownDays > 0) Text("CD: ${product.cooldownDays}", color = theme.textSecondary, fontSize = 10.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassSwipeCard(
    product: Product,
    status: String,
    statusColor: Color,
    theme: GlassThemeColors,
    isDark: Boolean,
    onDelete: () -> Unit
) {
    val prodColor = if (isDark) product.type.colorDark else product.type.colorLight
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            containerColor = theme.background,
            onDismissRequest = { showDialog = false },
            title = { Text("Видалити?", color = theme.textPrimary) },
            confirmButton = { Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = theme.error)) { Text("Так", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Ні", color = theme.textPrimary) } }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(theme.cardBg)
            .border(1.dp, theme.border, RoundedCornerShape(24.dp))
            .combinedClickable(onClick = {}, onLongClick = { showDialog = true })
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(prodColor.copy(alpha=0.2f)), contentAlignment = Alignment.Center) {
            Text(product.type.label.take(1), color = prodColor, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text("Пауза: ${product.cooldownDays} дн.", color = theme.textSecondary, fontSize = 12.sp)
        }
        Text(status, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GlassBottomBar(selected: Int, theme: GlassThemeColors, onSelect: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(35.dp))
            .background(theme.navBar)
            .border(1.dp, theme.border, RoundedCornerShape(35.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(Icons.Rounded.Home, selected == 0, theme) { onSelect(0) }
            NavItem(Icons.Rounded.List, selected == 1, theme) { onSelect(1) }
            NavItem(Icons.Rounded.DateRange, selected == 2, theme) { onSelect(2) }
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, isSelected: Boolean, theme: GlassThemeColors, onClick: () -> Unit) {
    val color by animateColorAsState(if (isSelected) theme.accent else theme.textSecondary, label = "icon")
    IconButton(onClick = onClick) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductGlassSheet(theme: GlassThemeColors, isDark: Boolean, onDismiss: () -> Unit, onAdd: (Product) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ProductType.RECOVERY) }
    var cooldown by remember { mutableStateOf("0") }
    var showTypePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = theme.background) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Додати засіб (Add Product)", color = theme.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            GlassInput(name, "Назва (Name)", theme) { name = it }
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.cardBg)
                    .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                    .clickable { showTypePicker = true }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val pColor = if(isDark) type.colorDark else type.colorLight
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(pColor))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(type.label, color = theme.textPrimary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            GlassInput(cooldown, "Днів паузи (Cooldown Days)", theme, true) { if(it.all { c -> c.isDigit() }) cooldown = it }

            Spacer(modifier = Modifier.height(30.dp))
            GlassButton("Додати", if(isDark) type.colorDark else type.colorLight, name.isNotEmpty()) {
                onAdd(Product(name = name, type = type, cooldownDays = cooldown.toIntOrNull()?:0))
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showTypePicker) {
        ModalBottomSheet(onDismissRequest = { showTypePicker = false }, containerColor = theme.background) {
            LazyColumn(modifier = Modifier.padding(bottom = 30.dp)) {
                items(ProductType.values()) { t ->
                    val pColor = if(isDark) t.colorDark else t.colorLight
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { type = t; showTypePicker = false }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(pColor))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(t.label, color = theme.textPrimary)
                        if(type == t) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Check, null, tint = theme.textPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassInput(value: String, label: String, theme: GlassThemeColors, number: Boolean = false, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = theme.textSecondary) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = theme.textPrimary,
            unfocusedTextColor = theme.textPrimary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = theme.accent,
            unfocusedBorderColor = theme.border
        ),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = if(number) androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) else androidx.compose.foundation.text.KeyboardOptions.Default
    )
}

@Composable
fun GlassButton(text: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = Color.Gray.copy(alpha=0.3f))
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun GlassEmptyState(text: String, theme: GlassThemeColors) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = theme.textSecondary, textAlign = TextAlign.Center)
    }
}

// --- ROUTINE RULES ---
fun getRoutine() = listOf(
    RoutineStep("1", "Ексфоліація (Exfoliation)", listOf(ProductType.ENZYME, ProductType.CLEANSER), Color(0xFFFF9500)),
    RoutineStep("2", "Відновлення (Recovery)", listOf(ProductType.RECOVERY), Color(0xFF00D2BE)),
    RoutineStep("3", "Відновлення (Recovery)", listOf(ProductType.RECOVERY), Color(0xFF00D2BE)),
    RoutineStep("4", "Ексфоліація (Exfoliation)", listOf(ProductType.ENZYME), Color(0xFFFF9500)),
    RoutineStep("5", "Відновлення (Recovery)", listOf(ProductType.RECOVERY), Color(0xFF00D2BE)),
    RoutineStep("6", "Ретинол (Retinol)", listOf(ProductType.RETINOL), Color(0xFFAF52DE)),
    RoutineStep("7", "Відновлення (Recovery)", listOf(ProductType.RECOVERY), Color(0xFF00D2BE)),
    RoutineStep("8", "Відновлення (Recovery)", listOf(ProductType.RECOVERY), Color(0xFF00D2BE)),
    RoutineStep("9", "Ексфоліація (Exfoliation)", listOf(ProductType.ENZYME), Color(0xFFFF9500)),
    RoutineStep("10", "Відновлення (Recovery)", listOf(ProductType.RECOVERY), Color(0xFF00D2BE)),
    RoutineStep("11", "Пілінг (Peeling)", listOf(ProductType.PEELING), Color(0xFFFF3B30))
)