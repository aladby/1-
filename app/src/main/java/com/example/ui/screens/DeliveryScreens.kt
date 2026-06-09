package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.CartItemEntity
import com.example.data.local.MerchantEntity
import com.example.data.local.ProductEntity
import com.example.data.local.OrderEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.CheckoutUiState
import com.example.ui.viewmodel.DeliveryViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun MainAppContainer(viewModel: DeliveryViewModel) {
    // Enforce RTL Layout globally for Arabic Delivery App experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val currentScreen by viewModel.currentScreen.collectAsState()
        val cartItems by viewModel.cartItems.collectAsState()
        val activeOrder by viewModel.activeOrder.collectAsState()
        
        val isLoggedIn by viewModel.isLoggedIn.collectAsState()
        val currentUser by viewModel.currentUser.collectAsState()

        val cartCount = cartItems.sumOf { it.quantity }

        Scaffold(
            bottomBar = {
                // Bottom navigation bar aligned perfectly with Material 3 Guidelines
                NavigationBar(
                    modifier = Modifier.shadow(12.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Home || currentScreen is Screen.MerchantDetail,
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                        label = { Text("الرئيسية", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Cart,
                        onClick = { viewModel.navigateTo(Screen.Cart) },
                        icon = {
                            BadgedBox(badge = {
                                if (cartCount > 0) {
                                    Badge(containerColor = DeliverPrimary) {
                                        Text(cartCount.toString(), color = Color.White)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "السلة")
                            }
                        },
                        label = { Text("سلة المشتريات", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_cart")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Tracking,
                        onClick = {
                            if (activeOrder != null) {
                                viewModel.navigateTo(Screen.Tracking(activeOrder!!.id))
                            } else {
                                viewModel.navigateTo(Screen.Tracking(""))
                            }
                        },
                        icon = {
                            BadgedBox(badge = {
                                if (activeOrder != null) {
                                    // Animated pulse circle notifying order tracking is live
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val scale by infiniteTransition.animateFloat(
                                        initialValue = 0.6f,
                                        targetValue = 1.3f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "scale"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(DeliverPrimary.copy(alpha = scale))
                                    )
                                }
                            }) {
                                Icon(Icons.Default.DirectionsRun, contentDescription = "التتبع")
                            }
                        },
                        label = { Text("التتبع المباشر", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_tracking")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Profile || currentScreen is Screen.Login,
                        onClick = {
                            if (isLoggedIn) {
                                viewModel.navigateTo(Screen.Profile)
                            } else {
                                viewModel.navigateTo(Screen.Login)
                            }
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = "حسابي") },
                        label = { Text(if (isLoggedIn) "حسابي" else "دخول", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Smooth Animated Content Crossfade Navigation
                when (val screen = currentScreen) {
                    is Screen.Home -> HomeScreen(viewModel)
                    is Screen.MerchantDetail -> MerchantDetailScreen(viewModel, screen.merchantId)
                    is Screen.Cart -> CartScreen(viewModel)
                    is Screen.Checkout -> {
                        if (isLoggedIn) CheckoutScreen(viewModel) else LoginScreen(viewModel)
                    }
                    is Screen.Tracking -> TrackingScreen(viewModel, screen.orderId)
                    is Screen.Profile -> {
                        if (isLoggedIn) ProfileScreen(viewModel) else LoginScreen(viewModel)
                    }
                    is Screen.Login -> LoginScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: DeliveryViewModel) {
    val merchants by viewModel.merchants.collectAsState()
    val activeOrder by viewModel.activeOrder.collectAsState()
    val filterType by viewModel.merchantFilter.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val filteredMerchants = merchants.filter { merchant ->
        val matchesType = when (filterType) {
            "RESTAURANT" -> merchant.type == "RESTAURANT"
            "STORE" -> merchant.type == "STORE"
            else -> true
        }
        val matchesSearch = merchant.name.contains(searchQuery, ignoreCase = true) || 
                            merchant.category.contains(searchQuery, ignoreCase = true)
        matchesType && matchesSearch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_column"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Sleek Interface Top Header Card (Replaces raw dark/orange banner)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEADDFF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Location + Profile Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DeliverPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "موقع التوصيل",
                                    tint = DeliverPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("توصيل إلى", color = Color(0xFF49454F), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                Text("${currentUser.address} 📍", color = Color(0xFF1C1B1F), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Profile badge containing actual user's initial
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DeliverSecondary)
                                .border(2.dp, Color.White, CircleShape)
                                .clickable {
                                    if (isLoggedIn) {
                                        viewModel.navigateTo(Screen.Profile)
                                    } else {
                                        viewModel.navigateTo(Screen.Login)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                currentUser.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real-time responsive search field
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF3EDF7),
                            unfocusedContainerColor = Color(0xFFF3EDF7),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        placeholder = { Text("ابحث عن مطاعم، بقالة، أو طعام...", color = Color(0xFF79747E), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color(0xFF49454F), modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color(0xFF49454F), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            }
        }

        // Active Order Bar Indicator if order is currently ongoing (Mockup style with dashed border simulation)
        if (activeOrder != null) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { viewModel.navigateTo(Screen.Tracking(activeOrder!!.id)) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = Color(0x4D6750A4) // 30% alpha purple border
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Blinking Green Dot Animation
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse_green")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 0.5f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50).copy(alpha = scale))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تتبع طلبك المباشر", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeliverSecondary)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("رقم الطلب #${activeOrder!!.id.take(4).uppercase()}", fontSize = 9.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.6f))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DeliverPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Moped,
                                        contentDescription = "جاري التوصيل",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    val titleText = when (activeOrder!!.status) {
                                        "PLACED" -> "تم استلام الطلب ومراجعته"
                                        "PREPARING" -> "المطبخ يحضر طلبك الآن 🍳"
                                        "ON_THE_WAY" -> "السائق يقترب منك 🛵"
                                        else -> "وصل طلبك بالهناء والشفاء 🎉"
                                    }
                                    Text(titleText, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1C1B1F))
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    LinearProgressIndicator(
                                        progress = { activeOrder!!.progressPercent },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        color = DeliverSecondary,
                                        trackColor = Color(0xFFEADDFF)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                Text(
                                    text = if (activeOrder!!.status == "ON_THE_WAY") "4 د" else "15 د",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = DeliverPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sleek Interface Dual Category Panels: Restaurants & Stores (Grid Mockup)
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Restaurants panel
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(105.dp)
                        .clickable { 
                            if (filterType == "RESTAURANT") viewModel.setFilter("ALL") 
                            else viewModel.setFilter("RESTAURANT") 
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (filterType == "RESTAURANT") Color(0xFFFFF0E6) else Color.White
                    ),
                    border = BorderStroke(
                        width = 1.5.dp, 
                        color = if (filterType == "RESTAURANT") DeliverPrimary else Color(0xFFEADDFF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Column {
                                Text("المطاعم 🍕", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1C1B1F))
                                Text("أفضل الوجبات", fontSize = 10.sp, color = Color(0xFF49454F))
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (filterType == "RESTAURANT") DeliverPrimary else Color(0xFFF3EDF7))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (filterType == "RESTAURANT") "نشط" else "تصفح", 
                                    color = if (filterType == "RESTAURANT") Color.White else Color(0xFF1C1B1F), 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = DeliverPrimary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(56.dp)
                                .align(Alignment.BottomStart)
                                .offset(x = (-8).dp, y = 14.dp)
                        )
                    }
                }

                // Stores panel
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(105.dp)
                        .clickable { 
                            if (filterType == "STORE") viewModel.setFilter("ALL") 
                            else viewModel.setFilter("STORE") 
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (filterType == "STORE") Color(0xFFF1EDF9) else Color.White
                    ),
                    border = BorderStroke(
                        width = 1.5.dp, 
                        color = if (filterType == "STORE") DeliverSecondary else Color(0xFFEADDFF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Column {
                                Text("المتاجر 🛒", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1C1B1F))
                                Text("تسوق احتياجاتك", fontSize = 10.sp, color = Color(0xFF49454F))
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (filterType == "STORE") DeliverSecondary else Color(0xFFF3EDF7))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (filterType == "STORE") "نشط" else "تصفح", 
                                    color = if (filterType == "STORE") Color.White else Color(0xFF1C1B1F), 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = DeliverSecondary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(56.dp)
                                .align(Alignment.BottomStart)
                                .offset(x = (-8).dp, y = 14.dp)
                        )
                    }
                }
            }
        }

        // Promotional Hero Gradient Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(115.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFF8C00), Color(0xFFFF5A00))
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("خصم حتى 50%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("على أول 3 طلبات اليوم", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("اطلب الآن", color = Color(0xFFFF5A00), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Icon(
                        Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .size(90.dp)
                            .offset(x = 10.dp, y = 10.dp)
                            .rotate(-15f)
                    )
                }
            }
        }

        // High contrast tabs Selection (Restaurants vs Stores)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text("الأقسام المتاحة", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = DeliverSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CategorySelectorChip(
                        title = "الكل",
                        icon = Icons.Default.AllInclusive,
                        isSelected = filterType == "ALL",
                        onClick = { viewModel.setFilter("ALL") }
                    )
                    CategorySelectorChip(
                        title = "المطاعم اللذيذة",
                        icon = Icons.Default.Restaurant,
                        isSelected = filterType == "RESTAURANT",
                        onClick = { viewModel.setFilter("RESTAURANT") }
                    )
                    CategorySelectorChip(
                        title = "المتاجر والبقالة",
                        icon = Icons.Default.Storefront,
                        isSelected = filterType == "STORE",
                        onClick = { viewModel.setFilter("STORE") }
                    )
                }
            }
        }

        // Horizontal Promo Slider
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    "العروض والخصومات الكبرى 🥳 🔥",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = DeliverSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        PromoDiscountCard(
                            title = "خصم 50% ترحيبي!",
                            subtitle = "كود الخصم: PPM50",
                            desc = "يسري على أول 3 طلبات مطاعم",
                            gradient = Brush.linearGradient(colors = listOf(Color(0xFFFF8A65), Color(0xFFE64A19)))
                        )
                    }
                    item {
                        PromoDiscountCard(
                            title = "توصيل بـ 5 ج.م فقط",
                            subtitle = "سوبر ماركت عائلتي",
                            desc = "اطلب مستلزمات منزلك بتوصيل رمزي",
                            gradient = Brush.linearGradient(colors = listOf(Color(0xFFBA68C8), Color(0xFF7B1FA2)))
                        )
                    }
                    item {
                        PromoDiscountCard(
                            title = "عرض الغداء السريع",
                            subtitle = "بيتزا + كولا بـ 110 ريال",
                            desc = "بيتزا رويال من الساعة 1م لـ 5م",
                            gradient = Brush.linearGradient(colors = listOf(Color(0xFF4DB6AC), Color(0xFF00796B)))
                        )
                    }
                }
            }
        }

        // Merchants List Section Heading
        item {
            Text(
                text = if (filterType == "RESTAURANT") "أشهر المطاعم المتاحة" else if (filterType == "STORE") "أفضل المتاجر السوبر ماركت" else "كل المتاجر والمطاعم المتاحة",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = DeliverSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
            )
        }

        // Real listings of Merchants
        if (filteredMerchants.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد نتائج مطابقة للتصنيف الحالي", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            items(filteredMerchants) { merchant ->
                MerchantListItemCard(merchant = merchant) {
                    viewModel.selectMerchant(merchant.id)
                    viewModel.navigateTo(Screen.MerchantDetail(merchant.id))
                }
            }
        }

        // Permanent beautifully branded developer signature footer
        item {
            Spacer(modifier = Modifier.height(20.dp))
            DeveloperBrandingFooter()
        }
    }
}

@Composable
fun CategorySelectorChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) DeliverPrimary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .height(44.dp)
            .shadow(if (isSelected) 4.dp else 0.dp, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun PromoDiscountCard(
    title: String,
    subtitle: String,
    desc: String,
    gradient: Brush
) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Text(desc, color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
        }
    }
}

@Composable
fun MerchantListItemCard(merchant: MerchantEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("merchant_card_${merchant.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = merchant.imageUrl,
                    contentDescription = merchant.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
                // Rating label
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = "تقييم", tint = Color(0xFFFFBF00), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(merchant.rating.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color.Black)
                }

                // Type badge (Restaurant vs Store)
                val typeTxt = if (merchant.type == "RESTAURANT") "مطعم طعام" else "متجر بقالة"
                val typeColor = if (merchant.type == "RESTAURANT") DeliverPrimary else DeliverSecondary
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(typeColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(typeTxt, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = merchant.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = DeliverSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = merchant.category,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(merchant.deliveryTime, fontSize = 12.sp, color = Color.Gray)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Moped, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("توصيل: ${merchant.deliveryFee} ج.م", fontSize = 12.sp, color = DeliverPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantDetailScreen(viewModel: DeliveryViewModel, merchantId: String) {
    val merchants by viewModel.merchants.collectAsState()
    val products by viewModel.merchantProducts.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartSubtotal by viewModel.cartSubtotal.collectAsState()

    val merchant = merchants.find { it.id == merchantId }
    if (merchant == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("المتجر غير موجود")
        }
        return
    }

    // Filter items by inner catalog subcategories
    val categories = products.map { it.category }.distinct()
    var selectedCategory by remember { mutableStateOf("") }
    
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCategory.isEmpty()) {
            selectedCategory = categories.first()
        }
    }

    val displayedProducts = if (selectedCategory.isNotEmpty()) {
        products.filter { it.category == selectedCategory }
    } else {
        products
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("catalog_scroll"),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header Image Banner
            item {
                Box {
                    AsyncImage(
                        model = merchant.imageUrl,
                        contentDescription = merchant.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                    // Return Back Trigger Circle
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier
                            .padding(16.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "عودة", tint = Color.Black)
                    }
                }
            }

            // Shop Corporate Detail Section
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            merchant.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = DeliverSecondary
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeliverSecondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "تقييم", tint = DeliverPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(merchant.rating.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeliverSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(merchant.category, color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("وقت التوصيل", fontSize = 11.sp, color = Color.Gray)
                            Text(merchant.deliveryTime, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("رسوم الخدمة", fontSize = 11.sp, color = Color.Gray)
                            Text("${merchant.deliveryFee} ج.م", fontWeight = FontWeight.Bold, color = DeliverPrimary, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الأمان", fontSize = 11.sp, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("موثق", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessGreen)
                            }
                        }
                    }
                }
            }

            // Categories horizontal bar chips
            if (categories.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = category == selectedCategory
                            Surface(
                                onClick = { selectedCategory = category },
                                color = if (isSelected) DeliverPrimary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) DeliverPrimary else Color.LightGray),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .shadow(if (isSelected) 3.dp else 0.dp, RoundedCornerShape(16.dp))
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        category,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Products list
            if (displayedProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("جاري تحضير باقي الأصناف قريباً...", color = Color.Gray)
                    }
                }
            } else {
                items(displayedProducts) { product ->
                    ProductCatalogItemCard(product = product) {
                        viewModel.addToCart(product, merchant)
                    }
                }
            }
        }

        // Live Floating Checkout Bar inside parent container!
        if (cartItems.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(Screen.Cart) },
                    colors = ButtonDefaults.buttonColors(containerColor = DeliverPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    cartItems.sumOf { it.quantity }.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("عرض سلة المشتريات والطلب", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
                        }
                        Text("${cartSubtotal} ج.م", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCatalogItemCard(product: ProductEntity, onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderPurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DeliverSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    product.description,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${product.price} ج.م",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeliverPrimary,
                    fontSize = 14.sp
                )
            }

            // Image and adding action button nested together nicely
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeliverSecondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Fastfood, contentDescription = null, tint = DeliverSecondary, modifier = Modifier.size(32.dp))
                    }
                }

                // Add button hovering at the bottom center of product image
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(DeliverPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة للسلة", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun CartScreen(viewModel: DeliveryViewModel) {
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.cartSubtotal.collectAsState()
    val context = LocalContext.current

    val currentUser by viewModel.currentUser.collectAsState()
    var deliveryAddress by remember(currentUser.address) { mutableStateOf(currentUser.address) }
    var chosenPayment by remember { mutableStateOf("CARD") } // "CARD", "CASH"

    if (cartItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.RemoveShoppingCart,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(110.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("سلة مشترياتك فارغة تماماً!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DeliverSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("تصفح أشهر المأكولات والمتاجر واضف سلعاً لتجربة التوصيل", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    colors = ButtonDefaults.buttonColors(containerColor = DeliverPrimary)
                ) {
                    Text("ابدأ التسوق الآن 🛍️", color = Color.White)
                }
            }
        }
        return
    }

    val deliveryFee = 15.0
    val totalAmount = subtotal + deliveryFee

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cart_scroll"),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "عودة")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("تفاصيل سلة طلبك", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = DeliverSecondary)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Cart Items List
        items(cartItems) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${item.productPrice} ج.م", color = DeliverPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }

                    // Increments / Decrements actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(20.dp))
                    ) {
                        IconButton(
                            onClick = { viewModel.changeQuantity(item, isAddition = false) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "ناقص", modifier = Modifier.size(16.dp))
                        }
                        
                        Text(
                            item.quantity.toString(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        IconButton(
                            onClick = { viewModel.changeQuantity(item, isAddition = true) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "زائد", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Location & address text field
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = DeliverPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عنوان التوصيل (يمكنك التعديل)", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { deliveryAddress = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("address_field"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeliverPrimary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }
            }
        }

        // Payment configuration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("اختر وسيلة الدفع", fontWeight = FontWeight.Bold, color = DeliverSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chosenPayment = "CARD" }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = chosenPayment == "CARD",
                            onClick = { chosenPayment = "CARD" },
                            colors = RadioButtonDefaults.colors(selectedColor = DeliverPrimary),
                            modifier = Modifier.testTag("payment_card")
                        )
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = DeliverSecondary, modifier = Modifier.padding(horizontal = 8.dp))
                        Text("بطاقة الدفع الإلكتروني (فيزا / ماستركارد)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chosenPayment = "CASH" }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = chosenPayment == "CASH",
                            onClick = { chosenPayment = "CASH" },
                            colors = RadioButtonDefaults.colors(selectedColor = DeliverPrimary)
                        )
                        Icon(Icons.Default.Payments, contentDescription = null, tint = SuccessGreen, modifier = Modifier.padding(horizontal = 8.dp))
                        Text("الدفع نقداً عند الاستلام", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Order Pricing Receipt Summary
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ملخص الفاتورة المالية", fontWeight = FontWeight.ExtraBold, color = DeliverSecondary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("قيمة المشتريات", color = Color.Gray)
                        Text("$subtotal ج.م", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("رسوم التوصيل السريع", color = Color.Gray)
                        Text("$deliveryFee ج.م", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المبلغ الإجمالي المستحق", fontWeight = FontWeight.ExtraBold, color = DeliverSecondary, fontSize = 15.sp)
                        Text("$totalAmount ج.م", fontWeight = FontWeight.ExtraBold, color = DeliverPrimary, fontSize = 16.sp)
                    }
                }
            }
        }

        // Checkout Button trigger
        item {
            Button(
                onClick = {
                    if (chosenPayment == "CARD") {
                        // Navigate to our immersive credit card simulation
                        viewModel.placeOrder(deliveryAddress, "CARD")
                        viewModel.navigateTo(Screen.Checkout)
                    } else {
                        // Directly dispatch CASH order
                        viewModel.placeOrder(deliveryAddress, "CASH")
                        viewModel.navigateTo(Screen.Home)
                        Toast.makeText(context, "تم إرسال طلبك بنجاح وجاري الطبخ! متابعة ممتعة.", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeliverPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(vertical = 2.dp)
                    .testTag("checkout_order_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (chosenPayment == "CARD") "الذهاب لبوابة الدفع الإلكتروني 💳" else "تأكيد الطلب والدفع كاش 🛵",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CheckoutScreen(viewModel: DeliveryViewModel) {
    val checkoutState by viewModel.checkoutState.collectAsState()
    
    // Credit card state bindings
    var cardNumber by remember { mutableStateOf("4589 1234 5678 9912") }
    var cardOwner by remember { mutableStateOf("Anas Agram - PPM") }
    var cardExpiry by remember { mutableStateOf("11/29") }
    var cardCvv by remember { mutableStateOf("315") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("checkout_panel"),
        contentAlignment = Alignment.Center
    ) {
        when (val state = checkoutState) {
            is CheckoutUiState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DeliverPrimary, strokeWidth = 5.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("جاري تفويض عملية الدفع الآمنة...", fontWeight = FontWeight.Bold, color = DeliverSecondary)
                    Text("يرجى عدم غلق التطبيق أو التحديث", color = Color.Gray, fontSize = 12.sp)
                }
            }

            is CheckoutUiState.Success -> {
                SuccessCheckoutVisuals(order = state.order) {
                    viewModel.resetCheckout()
                    viewModel.navigateTo(Screen.Tracking(state.order.id))
                }
            }

            is CheckoutUiState.Idle -> {
                // Interactive visa form + physical looking card mockup
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("بوابة PPM الآمنة للدفع", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = DeliverSecondary)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Physical Credit Card Mockup Rendered beautifully (Visual Craftsmanship!)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DeliverSecondary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("بوابة الدفع الإلكتروني", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                    // Golden shining electronic card chip
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp, 28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(Color(0xFFFFD54F), Color(0xFFFFB300))
                                                )
                                            )
                                    )
                                }

                                Text(
                                    cardNumber,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("حامل البطاقة", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                        Text(cardOwner, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("تاريخ الانتهاء", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                        Text(cardExpiry, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("أدخل بيانات بطاقتك الائتمانية", fontWeight = FontWeight.Bold, color = DeliverSecondary)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { if (it.length <= 19) cardNumber = it },
                                label = { Text("رقم البطاقة (16 رقم)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = cardOwner,
                                onValueChange = { cardOwner = it },
                                label = { Text("اسم صاحب البطاقة") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = cardExpiry,
                                    onValueChange = { if (it.length <= 5) cardExpiry = it },
                                    label = { Text("الانتهاء (MM/YY)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = cardCvv,
                                    onValueChange = { if (it.length <= 3) cardCvv = it },
                                    label = { Text("رمز التحقق (CVV)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.placeOrder("برج النور، شارع التحرير", "CARD_AUTHORIZED") },
                        colors = ButtonDefaults.buttonColors(containerColor = DeliverPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ادفع الآن بأمان وثقة 🔒", fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessCheckoutVisuals(order: OrderEntity, onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success checkmark animation frame
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(SuccessGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("اكتمل الدفع الإلكتروني بنجاح! 💳 ✨", fontWeight = FontWeight.Black, fontSize = 20.sp, color = SuccessGreen, textAlign = TextAlign.Center)
        Text("طعم رائع في الطريق إليك...", color = Color.Gray, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(24.dp))

        // Receipt invoice layout (Pure Craft)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "فاتورة شراء من شركة PPM للتوصيل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DeliverSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("رمز الفاتورة:", color = Color.Gray, fontSize = 12.sp)
                    Text(order.id, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المتجر الشريك:", color = Color.Gray, fontSize = 12.sp)
                    Text(order.merchantName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("اسم العميل:", color = Color.Gray, fontSize = 12.sp)
                    Text("أنس عرام الفاخر", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("طريقة الدفع:", color = Color.Gray, fontSize = 12.sp)
                    Text("فيزا برعاية PPM البنكية", fontWeight = FontWeight.Bold, color = DeliverPrimary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("القيمة الصافية:", color = Color.Gray, fontSize = 12.sp)
                    Text("${order.subtotal} ج.م", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("التوصيل السريع:", color = Color.Gray, fontSize = 12.sp)
                    Text("${order.deliveryFee} ج.م", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("إجمالي المبلغ المدفوع:", fontWeight = FontWeight.Bold, color = DeliverSecondary, fontSize = 13.sp)
                    Text("${order.totalAmount} ج.م", fontWeight = FontWeight.ExtraBold, color = DeliverPrimary, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = DeliverPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("تتبع وصول السائق فوراً 🛵", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TrackingScreen(viewModel: DeliveryViewModel, orderId: String) {
    val activeOrder by viewModel.activeOrder.collectAsState()
    val orderHistory by viewModel.orderHistory.collectAsState()
    val context = LocalContext.current

    val order = activeOrder ?: orderHistory.firstOrNull()

    if (order == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Moped, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(90.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("لا يوجد طلب نشط حالياً لتتبعه", fontWeight = FontWeight.Bold, color = DeliverSecondary)
                Text("يمكنك التوجه للرئيسية وطلب مأكولاتك وسوف تظهر هنا ثانية بثانية", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    colors = ButtonDefaults.buttonColors(containerColor = DeliverPrimary)
                ) {
                    Text("الذهاب للقائمة الرئيسية 🏠")
                }
            }
        }
        return
    }

    // Dynamic colors based on active nodes
    val isPlaced = order.status == "PLACED" || order.status == "PREPARING" || order.status == "ON_THE_WAY" || order.status == "DELIVERED"
    val isPreparing = order.status == "PREPARING" || order.status == "ON_THE_WAY" || order.status == "DELIVERED"
    val isOnTheWay = order.status == "ON_THE_WAY" || order.status == "DELIVERED"
    val isDelivered = order.status == "DELIVERED"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tracking_scroll"),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "عودة")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("تتبع وصول السائق المباشر", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = DeliverSecondary)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Animated interactive live Canvas map path (Visual Masterpiece!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Gray map simulation drawing on Canvas
                    val animProgress = remember { Animatable(0f) }
                    LaunchedEffect(order.progressPercent) {
                        animProgress.animateTo(
                            targetValue = order.progressPercent,
                            animationSpec = tween(1500, easing = LinearOutSlowInEasing)
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE8F5E9)) // Soft grass green map background
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Draw Grid lines simulating city streets
                        val gridStroke = 2.dp.toPx()
                        for (i in 1..8) {
                            drawLine(
                                color = Color.White,
                                start = Offset(i * canvasWidth / 9, 0f),
                                end = Offset(i * canvasWidth / 9, canvasHeight),
                                strokeWidth = gridStroke
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(0f, i * canvasHeight / 6),
                                end = Offset(canvasWidth, i * canvasHeight / 6),
                                strokeWidth = gridStroke
                            )
                        }

                        // Draw Road curves representing delivery route
                        val roadPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(canvasWidth * 0.15f, canvasHeight * 0.75f)
                            quadraticTo(
                                canvasWidth * 0.4f, canvasHeight * 0.85f,
                                canvasWidth * 0.5f, canvasHeight * 0.5f
                            )
                            quadraticTo(
                                canvasWidth * 0.6f, canvasHeight * 0.15f,
                                canvasWidth * 0.85f, canvasHeight * 0.25f
                            )
                        }

                        // Draw background road outline
                        drawPath(
                            path = roadPath,
                            color = Color(0xFFCFD8DC),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 16.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // Draw actual completed route by delivery boy
                        drawPath(
                            path = roadPath,
                            color = DeliverPrimary.copy(alpha = 0.85f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 10.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                            )
                        )

                        // Draw Restaurant Pin (Start point)
                        drawCircle(
                            color = DeliverSecondary,
                            radius = 8.dp.toPx(),
                            center = Offset(canvasWidth * 0.15f, canvasHeight * 0.75f)
                        )

                        // Draw Customer House Pin (End point)
                        drawCircle(
                            color = SuccessGreen,
                            radius = 8.dp.toPx(),
                            center = Offset(canvasWidth * 0.85f, canvasHeight * 0.25f)
                        )

                        // Draw Animated motorcycle position!
                        val riderPos = Offset(
                            x = canvasWidth * 0.15f + animProgress.value * (canvasWidth * 0.70f),
                            y = canvasHeight * 0.75f - animProgress.value * (canvasHeight * 0.50f)
                        )

                        drawCircle(
                            color = DeliverPrimary,
                            radius = 12.dp.toPx(),
                            center = riderPos
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = riderPos
                        )
                    }

                    // Floating labels inside Map Canvas
                    Text(
                        "🍔 مطعم ${order.merchantName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    Text(
                        "📍 منزلك الفاخر",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    // Floating bike tag updating with percentage
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DeliverSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "السائق قطع ${(order.progressPercent * 100).toInt()}% من مسافة الطريق المتاحة",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Live status progress card block
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "مراحل معالجة وتوصيل الأكل",
                        fontWeight = FontWeight.ExtraBold,
                        color = DeliverSecondary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    StatusNodeRow(title = "تم تأكيد طلبك ومراجعته", desc = "البنك وافق على تفويض بطاقتك بنجاح", isActive = isPlaced)
                    StatusNodeConnector(isActive = isPreparing)
                    StatusNodeRow(title = "جاري تجميع وتجهيز الأغراض", desc = "يتم المراجعة والطهي بعناية ممتازة", isActive = isPreparing)
                    StatusNodeConnector(isActive = isOnTheWay)
                    StatusNodeRow(title = "كابتن التوصيل استلم طلبك", desc = "السائق يقود الآن بسرعة وأمان نحوك", isActive = isOnTheWay)
                    StatusNodeConnector(isActive = isDelivered)
                    StatusNodeRow(title = "تم تسليم الطلب للعميل 🎉", desc = "يسعدنا دائماً تقديم تجربة مثالية لك", isActive = isDelivered)
                }
            }
        }

        // Dynamic status dialog text banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = DeliverSecondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = DeliverPrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val header = if (isDelivered) "وصول آمن بالهناء والشفاء!" else "نصيحة الفريق"
                        val body = if (isDelivered) "نتمنى أن تنال وجبتك إعجابك الفائق. تمت البرمجة بواسطة أنس عرام." else "السائق يرتدي خوذة الأمان ويقود باحترافية كاملة لإيصال مأكولاتك ساخنة وطازجة."
                        Text(header, fontWeight = FontWeight.Bold, color = DeliverSecondary)
                        Text(body, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Driver details with action triggers (WhatsApp Direct Link!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(DeliverPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Moped, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(order.driverName, fontWeight = FontWeight.ExtraBold, color = DeliverSecondary)
                            Text("رقم التواصل: ${order.driverPhone}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    // Dial WhatsApp Clickable button
                    IconButton(
                        onClick = {
                            val waLink = "https://wa.me/201067194650?text=مرحبا كابتن أنس، أود الاستفسار عن طلبي في تطبيق طلبات إكسبريس"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waLink))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "لم يتم العثور على تطبيق واتساب على الجهاز", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "اتصل بالسائق", tint = Color.White)
                    }
                }
            }
        }

        // Developer signature banner
        item {
            Spacer(modifier = Modifier.height(20.dp))
            DeveloperBrandingFooter()
        }
    }
}

@Composable
fun StatusNodeRow(title: String, desc: String, isActive: Boolean) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isActive) DeliverPrimary else Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isActive) DeliverSecondary else Color.Gray)
            Text(desc, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StatusNodeConnector(isActive: Boolean) {
    Box(
        modifier = Modifier
            .padding(start = 11.dp)
            .width(2.dp)
            .height(24.dp)
            .background(if (isActive) DeliverPrimary else Color.LightGray)
    )
}

@Composable
fun ProfileScreen(viewModel: DeliveryViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Authenticated Session Card Info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, BorderPurple),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(DeliverPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            currentUser.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        currentUser.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF1C1B1F)
                    )
                    Text(
                        currentUser.email,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Horizontal Divider
                    HorizontalDivider(color = Color(0xFFEADDFF), thickness = 1.dp)
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("رقم التواصل", color = Color.Gray, fontSize = 10.sp)
                            Text(currentUser.phone, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1C1B1F))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("نوع العضوية", color = Color.Gray, fontSize = 10.sp)
                            Text("حساب نشط وموثق 🌟", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeliverPrimary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("عنوان التوصيل الافتراضي", color = Color.Gray, fontSize = 10.sp)
                        Text(currentUser.address, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1C1B1F))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            viewModel.logout()
                            Toast.makeText(context, "تم تسجيل الخروج من الحساب بنجاح.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDE8E8)),
                        border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تسجيل الخروج من الحساب", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeliverPrimary, DeliverSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "PPM",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("شركة PPM لخدمات التكنولوجيا والتطوير", fontWeight = FontWeight.Black, fontSize = 16.sp, color = DeliverSecondary)
            Text("بالشراكة مع المطور المبدع أنس عرام", color = DeliverPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Showcase corporate text card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("نبذة عن هذا العمل الهندسي المطور", fontWeight = FontWeight.ExtraBold, color = DeliverSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "تم تصميم وتطوير تطبيق 'طلبات إكسبريس' باحترافية كاملة وتصميم Material 3 متميز ليتجاوز أداء وجودة تطبيق طلبات وكبرى منصات التوصيل الإقليمية. يدعم التطبيق إدارة دورة الطلب وسلة المشتريات وقواعد البيانات المحلية Room ومحاكاة دقيقة جداً لحسابات الدفع الإلكتروني وتتبع ومضي السائق عبر الخريطة والواتساب.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Justify,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // WhatsApp direct communication card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                border = BorderStroke(1.dp, SuccessGreen),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("تواصل مباشر مع المطور والشركة", fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("أنس عرام - شركة PPM للإلكترونيات والتطوير البرمجي الاحترافي", fontSize = 12.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            val msg = "مرحباً م. أنس عرام، يسعدني التواصل معك ومع شركة PPM بخصوص خدمات تطبيق طلبات إكسبريس"
                            val waUri = "https://wa.me/201067194650?text=" + Uri.encode(msg)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUri))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "تطبيق واتساب غير متوافر حالياً", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("راسلنا على الواتس: 01067194650", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Footer signature
        item {
            Spacer(modifier = Modifier.height(40.dp))
            DeveloperBrandingFooter()
        }
    }
}

@Composable
fun DeveloperBrandingFooter() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .border(2.dp, DeliverPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .background(DeliverSecondary)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "تمت برمجته وتطويره بواسطة المبرمج المبدع:",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Text(
            "أنس عرام 🌟",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Text(
            "تحت إشراف وتطوير شركة PPM المتكاملة",
            color = DeliverPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                val waLink = "https://wa.me/201067194650?text=أود التعاون مع المبرمج أنس عرام شركة PPM"
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waLink)))
                } catch (e: Exception) {
                    Toast.makeText(context, "عفواً، لا يوجد واتساب بهاتفك حالياً للتواصل.", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = DeliverPrimary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("راسلنا مباشرة: 01067194650 🚀", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoginScreen(viewModel: DeliveryViewModel) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Identity visual
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(DeliverPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Moped,
                contentDescription = null,
                tint = DeliverPrimary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        Text(
            text = "تطبيق طلبات إكسبريس الفاخر",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = DeliverSecondary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "بوابة الدخول الموحدة لتجربة طعام ومشتريات استثنائية",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tabs to toggle Login vs Register
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
            border = BorderStroke(1.dp, Color(0xFFEADDFF))
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isRegisterMode = false }
                        .background(if (!isRegisterMode) DeliverPrimary else Color.Transparent)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "تسجيل الدخول",
                        color = if (!isRegisterMode) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isRegisterMode = true }
                        .background(if (isRegisterMode) DeliverPrimary else Color.Transparent)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "إنشاء حساب جديد",
                        color = if (isRegisterMode) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderPurple),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (isRegisterMode) "سجل بياناتك للتوصيل الفوري" else "أدخل معلومات تسجيلك الآمن",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1C1B1F)
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Name input
                Text("الاسم الكامل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("مثال: أنس عرام الفاخر", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeliverPrimary,
                        unfocusedBorderColor = FieldBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone input
                Text("رقم الهاتف", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = { Text("مثال: 01067194650", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeliverPrimary,
                        unfocusedBorderColor = FieldBorder
                    )
                )

                if (isRegisterMode) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Email Input
                    Text("البريد الإلكتروني", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("مثال: info@ppm.com", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeliverPrimary,
                            unfocusedBorderColor = FieldBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Address Input
                Text("عنوان التوصيل للتطبيقات", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { Text("مثال: عمارة 44، شارع التحرير، القاهرة", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeliverPrimary,
                        unfocusedBorderColor = FieldBorder
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Submit button
                Button(
                    onClick = {
                        val finalName = name.ifEmpty { "أنس عرام الفاخر" }
                        val finalPhone = phone.ifEmpty { "01067194650" }
                        val finalEmail = email.ifEmpty { "aladbyaladby5@gmail.com" }
                        val finalAddress = address.ifEmpty { "الشارع الرئيسي، المعادي، القاهرة" }

                        viewModel.login(
                            name = finalName,
                            email = finalEmail,
                            phone = finalPhone,
                            address = finalAddress
                        )
                        viewModel.navigateTo(Screen.Home)
                        Toast.makeText(context, "أهلاً بك يا ${finalName}! تم تسجيل الدخول بأمان وطمأنينة 🌟", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeliverPrimary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "تأكيد الحساب والتشغيل 🚀" else "تسجيل دخول آمن 🔑",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bypass button
        TextButton(
            onClick = {
                viewModel.navigateTo(Screen.Home)
                Toast.makeText(context, "تصفح ممتع كزائر.", Toast.LENGTH_SHORT).show()
            }
        ) {
            Text("تصفح التطبيق كزائر لتجربة فورية ممتعة 👈", color = DeliverSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
