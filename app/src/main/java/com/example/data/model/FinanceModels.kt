package com.example.data.model

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.i18n.AppLanguage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

enum class TransactionType {
    INCOME, EXPENSE;

    companion object {
        fun fromString(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class LoanKind {
    BORROW, LEND;

    companion object {
        fun fromString(raw: String?): LoanKind =
            entries.firstOrNull { it.name == raw } ?: BORROW
    }
}

enum class RecurringFrequency(val labelEn: String, val labelBn: String) {
    WEEKLY("Weekly", "সাপ্তাহিক"),
    MONTHLY("Monthly", "মাসিক"),
    YEARLY("Yearly", "বাৎসরিক");

    fun displayLabel(lang: AppLanguage): String = if (lang == AppLanguage.BN) labelBn else labelEn

    companion object {
        fun fromString(raw: String?): RecurringFrequency =
            entries.firstOrNull { it.name == raw } ?: MONTHLY
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode");

    companion object {
        fun fromString(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class CurrencyOption(val code: String, val symbol: String, val label: String) {
    BDT("BDT", "৳", "Bangladeshi Taka (৳)"),
    USD("USD", "$", "US Dollar ($)"),
    EUR("EUR", "€", "Euro (€)"),
    GBP("GBP", "£", "British Pound (£)"),
    INR("INR", "₹", "Indian Rupee (₹)"),
    CAD("CAD", "C$", "Canadian Dollar (C$)"),
    AUD("AUD", "A$", "Australian Dollar (A$)"),
    JPY("JPY", "¥", "Japanese Yen (¥)");

    companion object {
        fun fromCode(code: String?): CurrencyOption =
            entries.firstOrNull { it.code == code } ?: BDT
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val currencyCode: String = CurrencyOption.BDT.code,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TransactionItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = TransactionType.EXPENSE.name,
    val category: String = "other",
    val note: String? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val monthKey: String = FinanceFormatters.monthKeyFromMillis(System.currentTimeMillis()),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedType: TransactionType
        get() = TransactionType.fromString(type)
}

@Keep
@IgnoreExtraProperties
data class CategoryBudget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "OVERALL",
    val limitAmount: Double = 0.0,
    val monthKey: String = FinanceFormatters.monthKeyFromMillis(System.currentTimeMillis()),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RecurringPayment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = TransactionType.EXPENSE.name,
    val category: String = "wifi",
    val frequency: String = RecurringFrequency.MONTHLY.name,
    val dayOfMonth: Int = 1,
    val nextDueMillis: Long = System.currentTimeMillis(),
    val reminderDaysBefore: Int = 3,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedType: TransactionType
        get() = TransactionType.fromString(type)
    val parsedFrequency: RecurringFrequency
        get() = RecurringFrequency.fromString(frequency)
}

@Keep
@IgnoreExtraProperties
data class LoanItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kind: String = LoanKind.BORROW.name,
    val personName: String = "",
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String? = null,
    val settled: Boolean = false,
    val repayments: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedKind: LoanKind
        get() = LoanKind.fromString(kind)

    val remainingAmount: Double
        get() = (amount - paidAmount).coerceAtLeast(0.0)

    fun parsedRepayments(): List<Pair<Long, Double>> {
        return repayments.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 2) {
                val ts = parts[0].toLongOrNull() ?: 0L
                val amt = parts[1].toDoubleOrNull() ?: 0.0
                if (amt > 0.0) ts to amt else null
            } else null
        }
    }
}

data class CategoryMeta(
    val id: String,
    val emoji: String,
    val labelBn: String,
    val labelEn: String,
    val icon: ImageVector,
    val color: Color,
    val forType: TransactionType?
) {
    fun displayName(lang: AppLanguage): String = if (lang == AppLanguage.BN) labelBn else labelEn
}

object CategoryCatalog {
    val expenseCategories = listOf(
        CategoryMeta("food", "🍚", "খাবার", "Food", Icons.Rounded.Restaurant, Color(0xFFEA580C), TransactionType.EXPENSE),
        CategoryMeta("transport", "🚌", "যাতায়াত", "Transport", Icons.Rounded.DirectionsBus, Color(0xFF2563EB), TransactionType.EXPENSE),
        CategoryMeta("wifi", "📶", "ওয়াইফাই বিল", "WiFi bill", Icons.Rounded.Wifi, Color(0xFF0891B2), TransactionType.EXPENSE),
        CategoryMeta("elec", "💡", "বিদ্যুৎ বিল", "Electricity bill", Icons.Rounded.Bolt, Color(0xFFD97706), TransactionType.EXPENSE),
        CategoryMeta("gas", "🔥", "গ্যাস বিল", "Gas bill", Icons.Rounded.LocalFireDepartment, Color(0xFFF97316), TransactionType.EXPENSE),
        CategoryMeta("snack", "🥪", "নাস্তা", "Snacks", Icons.Rounded.Fastfood, Color(0xFFCA8A04), TransactionType.EXPENSE),
        CategoryMeta("dps", "🏦", "DPS / সঞ্চয়", "DPS / Savings", Icons.Rounded.Savings, Color(0xFF12B886), TransactionType.EXPENSE),
        CategoryMeta("rent", "🏠", "বাসা ভাড়া", "Rent", Icons.Rounded.Home, Color(0xFF7C3AED), TransactionType.EXPENSE),
        CategoryMeta("shop", "🛍️", "কেনাকাটা", "Shopping", Icons.Rounded.ShoppingBag, Color(0xFFDB2777), TransactionType.EXPENSE),
        CategoryMeta("health", "💊", "চিকিৎসা", "Health", Icons.Rounded.LocalHospital, Color(0xFFF0506E), TransactionType.EXPENSE),
        CategoryMeta("edu", "📚", "শিক্ষা", "Education", Icons.Rounded.School, Color(0xFF5B5BF0), TransactionType.EXPENSE),
        CategoryMeta("fun", "🎬", "বিনোদন", "Entertainment", Icons.Rounded.Movie, Color(0xFF8A5CF6), TransactionType.EXPENSE),
        CategoryMeta("other", "📦", "অন্যান্য", "Other", Icons.Rounded.Category, Color(0xFF6B7690), TransactionType.EXPENSE)
    )

    val incomeCategories = listOf(
        CategoryMeta("salary", "💼", "বেতন", "Salary", Icons.Rounded.AccountBalance, Color(0xFF12B886), TransactionType.INCOME),
        CategoryMeta("business", "🏪", "ব্যবসা", "Business", Icons.Rounded.Storefront, Color(0xFF059669), TransactionType.INCOME),
        CategoryMeta("freelance", "💻", "ফ্রিল্যান্স", "Freelance", Icons.Rounded.LaptopMac, Color(0xFF5B5BF0), TransactionType.INCOME),
        CategoryMeta("bal", "📱", "মোবাইল/ইন্টারনেট ব্যালেন্স", "Mobile/Internet balance", Icons.Rounded.PhoneAndroid, Color(0xFF0891B2), TransactionType.INCOME),
        CategoryMeta("gift", "🎁", "উপহার", "Gift", Icons.Rounded.CardGiftcard, Color(0xFFD97706), TransactionType.INCOME),
        CategoryMeta("other_in", "💰", "অন্যান্য", "Other", Icons.Rounded.AttachMoney, Color(0xFF6B7690), TransactionType.INCOME)
    )

    private val legacyAliases = mapOf(
        "Food & Dining" to "food",
        "Transport" to "transport",
        "Housing & Rent" to "rent",
        "Utilities" to "elec",
        "Shopping" to "shop",
        "Health" to "health",
        "Entertainment" to "fun",
        "Education" to "edu",
        "Other" to "other",
        "Salary" to "salary",
        "Freelance" to "freelance",
        "Investments" to "dps",
        "Bonus & Gifts" to "gift"
    )

    private val allMap = (expenseCategories + incomeCategories).associateBy { it.id }

    fun metaFor(nameOrId: String): CategoryMeta {
        val resolvedId = legacyAliases[nameOrId] ?: nameOrId
        return allMap[resolvedId]
            ?: (expenseCategories + incomeCategories).firstOrNull {
                it.labelEn.equals(nameOrId, ignoreCase = true) || it.labelBn == nameOrId
            }
            ?: CategoryMeta(nameOrId, "📦", nameOrId, nameOrId, Icons.Rounded.Category, Color(0xFF6B7690), null)
    }

    fun categoriesFor(type: TransactionType): List<CategoryMeta> {
        return if (type == TransactionType.INCOME) incomeCategories else expenseCategories
    }
}

object FinanceFormatters {
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val bnMonths = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )
    private val enMonths = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    private val enShortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val bnShortMonths = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন", "জুলা", "আগ", "সেপ্ট", "অক্টো", "নভে", "ডিসে")
    private val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun localizeDigits(raw: String, lang: AppLanguage): String {
        if (lang == AppLanguage.EN) return raw
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            if (ch in '0'..'9') sb.append(bnDigits[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    fun formatNumber(n: Number, lang: AppLanguage): String {
        val locale = if (lang == AppLanguage.BN) Locale.forLanguageTag("bn-BD") else Locale.US
        val formatted = NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 0
        }.format(n)
        return localizeDigits(formatted, lang)
    }

    fun monthKeyFromMillis(millis: Long): String = synchronized(monthKeyFormat) {
        monthKeyFormat.format(Date(millis))
    }

    fun formatMonthDisplay(monthKey: String, lang: AppLanguage = AppLanguage.EN): String {
        val parsed = runCatching { synchronized(monthKeyFormat) { monthKeyFormat.parse(monthKey) } }.getOrNull()
            ?: return monthKey
        val cal = Calendar.getInstance().apply { time = parsed }
        val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val y = cal.get(Calendar.YEAR)
        val monthName = if (lang == AppLanguage.BN) bnMonths[m] else enMonths[m]
        val yearStr = localizeDigits(y.toString(), lang)
        return "$monthName $yearStr"
    }

    fun shortMonthLabel(monthKey: String, lang: AppLanguage = AppLanguage.EN): String {
        val parsed = runCatching { synchronized(monthKeyFormat) { monthKeyFormat.parse(monthKey) } }.getOrNull()
            ?: return monthKey
        val cal = Calendar.getInstance().apply { time = parsed }
        val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
        return if (lang == AppLanguage.BN) bnShortMonths[m] else enShortMonths[m]
    }

    fun shiftMonthKey(monthKey: String, deltaMonths: Int): String {
        val parsed = runCatching { synchronized(monthKeyFormat) { monthKeyFormat.parse(monthKey) } }.getOrNull()
            ?: Date()
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.MONTH, deltaMonths)
        }
        return synchronized(monthKeyFormat) { monthKeyFormat.format(cal.time) }
    }

    fun recentMonthKeys(currentMonthKey: String, count: Int = 6): List<String> {
        return ((count - 1) downTo 0).map { offset ->
            shiftMonthKey(currentMonthKey, -offset)
        }
    }

    fun formatDateFull(millis: Long, lang: AppLanguage = AppLanguage.EN): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val y = cal.get(Calendar.YEAR)
        return if (lang == AppLanguage.BN) {
            "${localizeDigits(d.toString(), lang)} ${bnMonths[m]} ${localizeDigits(y.toString(), lang)}"
        } else {
            val fmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
            fmt.format(Date(millis))
        }
    }

    fun formatDateShort(millis: Long, lang: AppLanguage = AppLanguage.EN): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
        return if (lang == AppLanguage.BN) {
            "${localizeDigits(d.toString(), lang)} ${bnMonths[m]}"
        } else {
            "$d ${enMonths[m]}"
        }
    }

    fun formatDayHeader(millis: Long, lang: AppLanguage = AppLanguage.EN): String {
        val calNow = Calendar.getInstance()
        val calTarget = Calendar.getInstance().apply { timeInMillis = millis }
        if (calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
        ) {
            return if (lang == AppLanguage.BN) "আজ" else "Today"
        }
        calNow.add(Calendar.DAY_OF_YEAR, -1)
        if (calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
        ) {
            return if (lang == AppLanguage.BN) "গতকাল" else "Yesterday"
        }
        return formatDateFull(millis, lang)
    }

    fun formatClockTime(millis: Long, lang: AppLanguage): String {
        val locale = if (lang == AppLanguage.BN) Locale.forLanguageTag("bn-BD") else Locale.US
        val fmt = SimpleDateFormat("hh:mm:ss a", locale)
        return localizeDigits(fmt.format(Date(millis)), lang)
    }

    fun formatClockDate(millis: Long, lang: AppLanguage): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
        if (lang == AppLanguage.BN) {
            val bnWeekdays = listOf("রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার")
            val dow = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
            return "${bnWeekdays[dow]}, ${localizeDigits(d.toString(), lang)} ${bnMonths[m]}"
        }
        val fmt = SimpleDateFormat("EEEE, MMMM d", Locale.US)
        return fmt.format(Date(millis))
    }

    fun formatCurrency(
        amount: Double,
        currencyCode: String,
        showSign: Boolean = false,
        isPositive: Boolean = true,
        lang: AppLanguage = AppLanguage.EN
    ): String {
        val option = CurrencyOption.fromCode(currencyCode)
        val useDecimals = option != CurrencyOption.JPY && option != CurrencyOption.BDT &&
            kotlin.math.abs(amount - amount.roundToLong()) > 0.005
        val locale = if (lang == AppLanguage.BN) Locale.forLanguageTag("bn-BD") else Locale.US
        val numberFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = if (useDecimals) 2 else 0
            maximumFractionDigits = if (useDecimals) 2 else 0
        }
        val absVal = if (useDecimals) kotlin.math.abs(amount) else kotlin.math.abs(amount).roundToLong().toDouble()
        val formattedNumber = localizeDigits(numberFormat.format(absVal), lang)
        val prefix = when {
            showSign && isPositive -> "+"
            showSign && !isPositive -> "−"
            amount < 0 -> "−"
            else -> ""
        }
        return "$prefix${option.symbol}$formattedNumber"
    }

    fun advanceRecurringMillis(currentDueMillis: Long, frequency: RecurringFrequency): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentDueMillis }
        when (frequency) {
            RecurringFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurringFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }
}
