package com.example.data.repository

import com.example.data.local.CartItemEntity
import com.example.data.local.DeliveryDao
import com.example.data.local.MerchantEntity
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DeliveryRepository(private val deliveryDao: DeliveryDao) {

    val allMerchants: Flow<List<MerchantEntity>> = deliveryDao.getAllMerchants()
    val cartItems: Flow<List<CartItemEntity>> = deliveryDao.getCartItems()
    val allOrders: Flow<List<OrderEntity>> = deliveryDao.getOrders()
    val activeOrder: Flow<OrderEntity?> = deliveryDao.getActiveOrder()

    fun getMerchantsByType(type: String): Flow<List<MerchantEntity>> {
        return deliveryDao.getMerchantsByType(type)
    }

    fun getProductsForMerchant(merchantId: String): Flow<List<ProductEntity>> {
        return deliveryDao.getProductsForMerchant(merchantId)
    }

    suspend fun addToCart(item: CartItemEntity) {
        deliveryDao.insertCartItem(item)
    }

    suspend fun removeFromCart(productId: String) {
        deliveryDao.deleteCartItem(productId)
    }

    suspend fun updateCartItemQuantity(item: CartItemEntity, isAddition: Boolean) {
        val newQuantity = if (isAddition) item.quantity + 1 else item.quantity - 1
        if (newQuantity <= 0) {
            deliveryDao.deleteCartItem(item.productId)
        } else {
            deliveryDao.insertCartItem(item.copy(quantity = newQuantity))
        }
    }

    suspend fun clearCart() {
        deliveryDao.clearCart()
    }

    suspend fun placeOrder(order: OrderEntity) {
        deliveryDao.insertOrder(order)
        deliveryDao.clearCart()
    }

    suspend fun updateOrderStatus(id: String, status: String, progress: Float) {
        deliveryDao.updateOrderStatus(id, status, progress)
    }

    suspend fun prepopulateIfEmpty() {
        val currentMerchants = deliveryDao.getAllMerchants().first()
        if (currentMerchants.size < 9) {
            val mockMerchants = listOf(
                MerchantEntity(
                    id = "rest_shawarma",
                    name = "شاورما الشام والأصيل",
                    type = "RESTAURANT",
                    category = "مأكولات شامية • شاورما",
                    rating = 4.8,
                    deliveryTime = "25-15 دقيقة",
                    deliveryFee = 12.0,
                    imageUrl = "https://images.unsplash.com/photo-1561651823-34feb02250e4?w=500&auto=format&fit=crop&q=60",
                    isPopular = true
                ),
                MerchantEntity(
                    id = "rest_burger",
                    name = "برجر هاوس - Burger House",
                    type = "RESTAURANT",
                    category = "أمريكي • برجر ولحوم",
                    rating = 4.6,
                    deliveryTime = "30-20 دقيقة",
                    deliveryFee = 15.0,
                    imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&auto=format&fit=crop&q=60",
                    isPopular = true
                ),
                MerchantEntity(
                    id = "rest_pizza",
                    name = "بيتزا رويال الملكي",
                    type = "RESTAURANT",
                    category = "إيطالي • بيتزا ومعجنات",
                    rating = 4.7,
                    deliveryTime = "35-25 دقيقة",
                    deliveryFee = 10.0,
                    imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500&auto=format&fit=crop&q=60",
                    isPopular = false
                ),
                MerchantEntity(
                    id = "rest_koshary",
                    name = "كشري التحرير الأصلي",
                    type = "RESTAURANT",
                    category = "مأكولات شعبية • كشري وحواوشي",
                    rating = 4.9,
                    deliveryTime = "20-15 دقيقة",
                    deliveryFee = 8.0,
                    imageUrl = "https://images.unsplash.com/photo-1541532713592-79a0317b6b77?w=500&auto=format&fit=crop&q=60",
                    isPopular = true
                ),
                MerchantEntity(
                    id = "rest_sushi",
                    name = "سوشي كلوب - Sushi Club",
                    type = "RESTAURANT",
                    category = "آسيوي • سوشي ونودلز فاخرة",
                    rating = 4.8,
                    deliveryTime = "45-35 دقيقة",
                    deliveryFee = 20.0,
                    imageUrl = "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=500&auto=format&fit=crop&q=60",
                    isPopular = true
                ),
                MerchantEntity(
                    id = "store_khair",
                    name = "سوبرماركت الخير والبركة",
                    type = "STORE",
                    category = "بقالة ومواد غذائية مستوردة",
                    rating = 4.5,
                    deliveryTime = "40-30 دقيقة",
                    deliveryFee = 8.0,
                    imageUrl = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=500&auto=format&fit=crop&q=60",
                    isPopular = true
                ),
                MerchantEntity(
                    id = "store_green",
                    name = "طازج وسريع للخضار والفواكه",
                    type = "STORE",
                    category = "خضار طازجة وفواكه موسمية",
                    rating = 4.9,
                    deliveryTime = "20-15 دقيقة",
                    deliveryFee = 7.0,
                    imageUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=500&auto=format&fit=crop&q=60",
                    isPopular = false
                ),
                MerchantEntity(
                    id = "store_pastry",
                    name = "حلواني الصعيدي الفاخر",
                    type = "STORE",
                    category = "حلويات شرقية • كعك وبسبوسة",
                    rating = 4.7,
                    deliveryTime = "30-20 دقيقة",
                    deliveryFee = 12.0,
                    imageUrl = "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500&auto=format&fit=crop&q=60",
                    isPopular = false
                ),
                MerchantEntity(
                    id = "store_pharmacy",
                    name = "صيدلية الشفاء والعافية",
                    type = "STORE",
                    category = "مستحضرات طبية وعناية شخصية",
                    rating = 4.9,
                    deliveryTime = "15-10 دقيقة",
                    deliveryFee = 10.0,
                    imageUrl = "https://images.unsplash.com/photo-1586015555751-63bb77f4322a?w=500&auto=format&fit=crop&q=60",
                    isPopular = false
                )
            )

            val mockProducts = listOf(
                ProductEntity(
                    id = "prod_shaw_1",
                    merchantId = "rest_shawarma",
                    name = "شاورما سوبر عربي دبل",
                    description = "شاورما دجاج متبل بخلطة شامية، بطاطس مقرمشة، مخلل، صوص الثومية المميز في خبز صاج بلدي عريض وسمنة خفيفة.",
                    price = 78.0,
                    imageUrl = "https://images.unsplash.com/photo-1561651823-34feb02250e4?w=500&auto=format&fit=crop&q=60",
                    category = "الوجبات الأكثر مبيعاً"
                ),
                ProductEntity(
                    id = "prod_shaw_2",
                    merchantId = "rest_shawarma",
                    name = "وجبة شاورما عربي فرط",
                    description = "شاورما فرط مقطعة بصحن مع بطاطس أصابع، صوص ثومية، صلصة حارة ومخللات مشكلة.",
                    price = 95.0,
                    imageUrl = "https://images.unsplash.com/photo-1529042410759-befb1204b468?w=500&auto=format&fit=crop&q=60",
                    category = "الوجبات الأكثر مبيعاً"
                ),
                ProductEntity(
                    id = "prod_shaw_3",
                    merchantId = "rest_shawarma",
                    name = "ساندويتش شاورما دجاج جامبو",
                    description = "ساندويتش شاورما دجاج بحجم جامبو مع الثومية والمخلل والبطاطس الملفوفة.",
                    price = 45.0,
                    imageUrl = "https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=500&auto=format&fit=crop&q=60",
                    category = "الساندوتشات الفردية"
                ),
                ProductEntity(
                    id = "prod_shaw_4",
                    merchantId = "rest_shawarma",
                    name = "علبة ثومية إضافية كلاسيك",
                    description = "كريم الثوم المحضر طازجاً كل صباح بدون مواد حافظة.",
                    price = 15.0,
                    imageUrl = "",
                    category = "المقبلات والشوربات"
                ),

                ProductEntity(
                    id = "prod_burg_1",
                    merchantId = "rest_burger",
                    name = "سموك هاوس باربكيو برجر دبل",
                    description = "قطعتين من اللحم البقري الصافي المشوي على اللهب، شريحة جبن شيدر، لحم بقري مقدد مقرمش، بصل مكرمل وصوص باربكيو مدخن.",
                    price = 120.0,
                    imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&auto=format&fit=crop&q=60",
                    category = "برجر لحم فاخر"
                ),
                ProductEntity(
                    id = "prod_burg_2",
                    merchantId = "rest_burger",
                    name = "تشيز برجر كلاسيك الرهيب",
                    description = "قطعة لحم بقري بلدي مشوية، صوص سري خاص، مخلل، بصل، طماطم وخس الكابوتشا الطازج ذو طعم غني وجارح.",
                    price = 85.0,
                    imageUrl = "https://images.unsplash.com/photo-1550547660-d9450f859349?w=500&auto=format&fit=crop&q=60",
                    category = "برجر لحم فاخر"
                ),
                ProductEntity(
                    id = "prod_burg_3",
                    merchantId = "rest_burger",
                    name = "تشيكن مقرمش حار هالبينو",
                    description = "صدر دجاج مقرمش ذهبي حار، صوص الديناميت الخاص، شرائح هالبينو، جبنة شيدر سائحة وخس طازج.",
                    price = 98.0,
                    imageUrl = "https://images.unsplash.com/photo-1625813506062-0aeb1d7a094b?w=500&auto=format&fit=crop&q=60",
                    category = "برجر دجاج مقرمش"
                ),

                ProductEntity(
                    id = "prod_piz_1",
                    merchantId = "rest_pizza",
                    name = "بيتزا بيبروني فاخرة بالجبن",
                    description = "عجينة إيطالية تقليدية هشة مخمرة لـ 24 ساعة، غنية بصلصة الطماطم العضوية، جبنة الموزاريلا الفاخرة وشرائح الببروني البقري.",
                    price = 135.0,
                    imageUrl = "https://images.unsplash.com/photo-1534308983496-4fabb1a015ee?w=500&auto=format&fit=crop&q=60",
                    category = "البيتزا الإيطالية"
                ),
                ProductEntity(
                    id = "prod_piz_2",
                    merchantId = "rest_pizza",
                    name = "بيتزا مارغريتا كلاسيكية",
                    description = "العجينة النابولية الكلاسيكية، جبنة الموزاريلا غنية ونضرة، صلصة طماطم مارزانو، وريحان طازج مع زيت زيتون بكر ممتاز.",
                    price = 110.0,
                    imageUrl = "https://images.unsplash.com/photo-1574071318508-1cdbab80d00a?w=500&auto=format&fit=crop&q=60",
                    category = "البيتزا الإيطالية"
                ),
                ProductEntity(
                    id = "prod_piz_3",
                    merchantId = "rest_pizza",
                    name = "وجبة باستا ألفريدو دجاج وفطر",
                    description = "باستا باستيتشو مشوية مع شرائح دجاج مشوية بمكعبات الزبدة، فطر بري، مغطاة بصلصة الكريمة الذهبية اللذيذة.",
                    price = 115.0,
                    imageUrl = "https://images.unsplash.com/photo-1645112411341-6c4fd023714a?w=500&auto=format&fit=crop&q=60",
                    category = "باستا ومعجنات"
                ),

                ProductEntity(
                    id = "prod_kosh_1",
                    merchantId = "rest_koshary",
                    name = "علبة كشري التحرير جامبو عائلي",
                    description = "علبة كشري عملاقة لجميع العائلة مع التقلية المقرمشة، حصتين من الصلصة المميزة، الدقة والصلصة الحارة المنعشة.",
                    price = 45.0,
                    imageUrl = "https://images.unsplash.com/photo-1541532713592-79a0317b6b77?w=500&auto=format&fit=crop&q=60",
                    category = "الأكثر مبيعاً"
                ),
                ProductEntity(
                    id = "prod_kosh_2",
                    merchantId = "rest_koshary",
                    name = "طاجن مكرونة باللحمة المفرومة بالفرن",
                    description = "مكرونة فرن غنية بصلصة الطماطم الكثيفة، مغطاة باللحم البقري المفروم المتبل ومسواة في طواجن فخار أصيلة.",
                    price = 55.0,
                    imageUrl = "",
                    category = "طواجن التحرير"
                ),
                ProductEntity(
                    id = "prod_kosh_3",
                    merchantId = "rest_koshary",
                    name = "طبق حلو أرز باللبن بالمكسرات",
                    description = "أرز باللبن كريمي غني ومبرد ومزين ببرش جوز الهند المبشور، الفستق والزبيب البلدي.",
                    price = 25.0,
                    imageUrl = "",
                    category = "الحلويات والمشروبات"
                ),

                ProductEntity(
                    id = "prod_sush_1",
                    merchantId = "rest_sushi",
                    name = "بوكس الساموراي سوشي مشكل VIP - 16 قطعة",
                    description = "مجموعة مختارة مذهلة تشمل 4 مكي سلمون، 4 كاليفورنيا رول، 4 فيلادلفيا كلاسيك، و4 رول كرانشي كابوريا حارة.",
                    price = 240.0,
                    imageUrl = "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=500&auto=format&fit=crop&q=60",
                    category = "العروض والبوكسات"
                ),
                ProductEntity(
                    id = "prod_sush_2",
                    merchantId = "rest_sushi",
                    name = "كرانشي سلمون رول سبايسي",
                    description = "رول سوشي مقرمش محشو بالسلمون الطازج، صوص السريراتشا الحار، مغطى بفتات التمبورا المقرمشة والسمسم الأسود.",
                    price = 110.0,
                    imageUrl = "",
                    category = "أطباق السوشي الفردية"
                ),
                ProductEntity(
                    id = "prod_sush_3",
                    merchantId = "rest_sushi",
                    name = "شوربة توم يوم البحرية التايلاندية",
                    description = "شوربة تايلاندية كلاسيكية حامضة وحارة، غنية بجمبري البحر، فطر عيش الغراب، عشبة الليمون وأوراق الكافير العطرية.",
                    price = 85.0,
                    imageUrl = "",
                    category = "الشوربات والمقبلات"
                ),

                ProductEntity(
                    id = "prod_str_1",
                    merchantId = "store_khair",
                    name = "حليب المراعي طازج كامل الدسم - 2 لتر",
                    description = "حليب أبقار نقي 100% طازج من مزارع المراعي الطبيعية.",
                    price = 32.0,
                    imageUrl = "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=60",
                    category = "الألبان والأجبان"
                ),
                ProductEntity(
                    id = "prod_str_2",
                    merchantId = "store_khair",
                    name = "أرز بسمتي هندي هلالي طويل الحبة (5 كجم)",
                    description = "أرز أبيض بنكهة نادرة فواحة، مثالي للكبسة والمندي والولائم وعزائم العائلات.",
                    price = 185.0,
                    imageUrl = "",
                    category = "الأرز والحبوب والمكرونات"
                ),
                ProductEntity(
                    id = "prod_str_3",
                    merchantId = "store_khair",
                    name = "شوكولاتة نوتيلا قابلة للدهن 750 جم",
                    description = "بزبدة البندق الفاخرة والكاكاو الغني اللذيذ لتفتتح بها صباحاتك اللذيذة.",
                    price = 140.0,
                    imageUrl = "",
                    category = "الحلويات والمقرمشات"
                ),

                ProductEntity(
                    id = "prod_gr_1",
                    merchantId = "store_green",
                    name = "كيلو تفاح أحمر سكري فاخر للغاية",
                    description = "تفاح سكري طازج جداً، منتقى بعناية، ذو قشرة ناضجة ممتلئة بالفوائد.",
                    price = 48.0,
                    imageUrl = "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=500&auto=format&fit=crop&q=60",
                    category = "الفواكه الطازجة"
                ),
                ProductEntity(
                    id = "prod_gr_2",
                    merchantId = "store_green",
                    name = "كيلو طماطم عنقودية بلدي من المزرعة",
                    description = "طماطم طازجة جداً مقطوفة صباحاً من البيوت المحمية العضوية.",
                    price = 18.0,
                    imageUrl = "https://images.unsplash.com/photo-1595855759920-86582396756a?w=500&auto=format&fit=crop&q=60",
                    category = "الخضراوات الأساسية"
                ),
                ProductEntity(
                    id = "prod_gr_3",
                    merchantId = "store_green",
                    name = "كيلو بوكس بطاطس تحمير فرنسي",
                    description = "بطاطس غنية بالنشويات الطبيعية ممتازة للتحمير السريع، تمنح قرمشة غامرة باللون الذهبي.",
                    price = 22.0,
                    imageUrl = "",
                    category = "الخضراوات الأساسية"
                ),

                ProductEntity(
                    id = "prod_past_1",
                    merchantId = "store_pastry",
                    name = "صينية بسبوسة ملوكي بالسمن البلدي والبندق",
                    description = "بسبوسة السعيدي الشهيرة مغطاة بطبقة غنية من حبات البندق المحمصة ومسقية بالشربات الساخن المعطر بماء الورد والسمن البلدي.",
                    price = 145.0,
                    imageUrl = "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500&auto=format&fit=crop&q=60",
                    category = "حلويات شرقية فاخرة"
                ),
                ProductEntity(
                    id = "prod_past_2",
                    merchantId = "store_pastry",
                    name = "طبق كنافة نابلسية بالجبن العكاوي السائح",
                    description = "كنافة نابلسية بلون ذهبي فاقع، محشوة بجبن العكاوي الغني السائح ومزينة ببرش الفستق الحلبي، تقدم ساخنة وذائبة.",
                    price = 95.0,
                    imageUrl = "",
                    category = "حلويات شرقية فاخرة"
                ),
                ProductEntity(
                    id = "prod_past_3",
                    merchantId = "store_pastry",
                    name = "قالب تورتة شيكولاتة فودج الكلاسيكية",
                    description = "تورتة عائلية دائرية غنية بـ 3 طبقات من كيك الشيكولاتة الاسفنجي وصوص جناش الكاكاو البلجيكي الداكن.",
                    price = 195.0,
                    imageUrl = "",
                    category = "تورته وكيكات للمناسبات"
                ),

                ProductEntity(
                    id = "prod_phar_1",
                    merchantId = "store_pharmacy",
                    name = "علبة كمامات طبية أندلسية معقمة (50 قطعة)",
                    description = "كمامات حماية ثلاثية الطبقات مع شريط مرن مريح حول الأذن مع سلك تثبيت الأنف لسلامة فائقة.",
                    price = 35.0,
                    imageUrl = "https://images.unsplash.com/photo-1586015555751-63bb77f4322a?w=500&auto=format&fit=crop&q=60",
                    category = "الإسعافات الأولية والسلامة"
                ),
                ProductEntity(
                    id = "prod_phar_2",
                    merchantId = "store_pharmacy",
                    name = "شامبو لوريال إلفيف هير المعالج الكامل بالكرياتين",
                    description = "عبوة 400 مل لعلاج الشعر التالف والمقصف يمنحه لمعاناً حريرياً ومقاومة عالية للتساقط.",
                    price = 125.0,
                    imageUrl = "",
                    category = "العناية بالشعر والبشرة"
                ),
                ProductEntity(
                    id = "prod_phar_3",
                    merchantId = "store_pharmacy",
                    name = "جهاز قياس ضغط الدم أومرون ديجيتال الذكي",
                    description = "جهاز قياس الضغط الإلكتروني من ماركة Omron العالمية لقياس دقيق للغاية وسريع مع شاشة ديجيتال واضحة وذاكرة للقراءات السابقة.",
                    price = 650.0,
                    imageUrl = "",
                    category = "الأجهزة الطبية المنزلية"
                )
            )

            // Overwrite database lists with the newly expanded records
            deliveryDao.insertMerchants(mockMerchants)
            deliveryDao.insertProducts(mockProducts)
        }
    }
}
