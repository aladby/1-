package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CartItemEntity
import com.example.data.local.DeliveryDatabase
import com.example.data.local.MerchantEntity
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.repository.DeliveryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class DeliveryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DeliveryRepository

    init {
        val database = DeliveryDatabase.getDatabase(application)
        repository = DeliveryRepository(database.deliveryDao())
        
        // Ensure data is pre-populated
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    val selectedMerchantId = MutableStateFlow<String?>(null)
    val merchantFilter = MutableStateFlow("ALL") // "ALL", "RESTAURANT", "STORE"
    val currentScreen = MutableStateFlow<Screen>(Screen.Home)
    
    // Professional search query state for real-time filtering of merchants and categories
    val searchQuery = MutableStateFlow("")

    // High performance User Authentication Session management
    val isLoggedIn = MutableStateFlow(true) // Pre-logged in for a rich immediate experience, can be customized or logged out!
    val currentUser = MutableStateFlow(UserSession(
        name = "أنس عرام الفاخر",
        email = "aladbyaladby5@gmail.com",
        phone = "01067194650",
        address = "الشارع الرئيسي، المعادي، القاهرة"
    ))

    fun login(name: String, email: String, phone: String, address: String) {
        viewModelScope.launch {
            currentUser.value = UserSession(name, email, phone, address)
            isLoggedIn.value = true
        }
    }

    fun logout() {
        viewModelScope.launch {
            isLoggedIn.value = false
        }
    }

    fun navigateTo(screen: Screen) {
        currentScreen.value = screen
    }

    val merchants: StateFlow<List<MerchantEntity>> = repository.allMerchants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val merchantProducts: StateFlow<List<ProductEntity>> = selectedMerchantId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getProductsForMerchant(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItemEntity>> = repository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val cartSubtotal: StateFlow<Double> = repository.cartItems
        .flatMapLatest { list ->
            flowOf(list.sumOf { it.productPrice * it.quantity })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val activeOrder: StateFlow<OrderEntity?> = repository.activeOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val orderHistory: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToCart(product: ProductEntity, merchant: MerchantEntity) {
        viewModelScope.launch {
            val currentCart = cartItems.value
            // If the item in cart matches another merchant, reset/clear first to avoid multi-merchant delivery errors
            if (currentCart.isNotEmpty() && currentCart.first().merchantId != merchant.id) {
                repository.clearCart()
            }
            
            val updatedCart = repository.cartItems.first()
            val existing = updatedCart.find { it.productId == product.id }
            if (existing != null) {
                repository.updateCartItemQuantity(existing, isAddition = true)
            } else {
                repository.addToCart(
                    CartItemEntity(
                        productId = product.id,
                        productName = product.name,
                        productPrice = product.price,
                        quantity = 1,
                        merchantId = merchant.id,
                        merchantName = merchant.name
                    )
                )
            }
        }
    }

    fun changeQuantity(item: CartItemEntity, isAddition: Boolean) {
        viewModelScope.launch {
            repository.updateCartItemQuantity(item, isAddition)
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            repository.removeFromCart(productId)
        }
    }

    fun selectMerchant(merchantId: String?) {
        selectedMerchantId.value = merchantId
    }

    fun setFilter(filter: String) {
        merchantFilter.value = filter
    }

    val checkoutState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)

    fun placeOrder(address: String, paymentMethod: String) {
        val currentCart = cartItems.value
        if (currentCart.isEmpty()) return

        val merchantId = currentCart.first().merchantId
        val merchantName = currentCart.first().merchantName
        val subtotal = cartSubtotal.value
        val deliveryFee = 15.0
        val total = subtotal + deliveryFee

        val order = OrderEntity(
            id = "ORD-" + UUID.randomUUID().toString().filter { it.isDigit() || it.isLetter() }.take(5).uppercase(),
            merchantId = merchantId,
            merchantName = merchantName,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            totalAmount = total,
            orderTime = System.currentTimeMillis(),
            status = "PLACED",
            paymentMethod = paymentMethod,
            deliveryAddress = address,
            progressPercent = 0.1f,
            driverName = "الكابتن أنس عرام",
            driverPhone = "01067194650"
        )

        viewModelScope.launch {
            checkoutState.value = CheckoutUiState.Loading
            delay(1500) // Simulating bank processing gateway...
            repository.placeOrder(order)
            checkoutState.value = CheckoutUiState.Success(order)
            startOrderSimulation(order.id)
        }
    }

    private fun startOrderSimulation(orderId: String) {
        viewModelScope.launch {
            // STEP 1: PREPARING (Order accepted & food/shop preparation)
            delay(12000)
            repository.updateOrderStatus(orderId, "PREPARING", 0.45f)

            // STEP 2: ON_THE_WAY (Rider is heading to destination)
            delay(1500) // transition delay
            repository.updateOrderStatus(orderId, "ON_THE_WAY", 0.75f)

            // STEP 3: DELIVERED (Arrived at customer)
            delay(20000)
            repository.updateOrderStatus(orderId, "DELIVERED", 1.0f)
        }
    }

    fun resetCheckout() {
        checkoutState.value = CheckoutUiState.Idle
    }
}

sealed class CheckoutUiState {
    object Idle : CheckoutUiState()
    object Loading : CheckoutUiState()
    data class Success(val order: OrderEntity) : CheckoutUiState()
}

data class UserSession(
    val name: String,
    val email: String,
    val phone: String,
    val address: String
)

sealed class Screen {
    object Home : Screen()
    data class MerchantDetail(val merchantId: String) : Screen()
    object Cart : Screen()
    object Checkout : Screen()
    data class Tracking(val orderId: String) : Screen()
    object Profile : Screen()
    object Login : Screen() // Sleek secure authentication screen
}
