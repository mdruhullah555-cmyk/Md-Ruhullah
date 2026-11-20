package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinanceFormatters
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MonoMediumStyle
import kotlinx.coroutines.delay

@Composable
fun BrandLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White)
            .padding(2.5.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(0.64f)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(size * 0.16f)
                    .background(ExpenseRedLight)
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "৳",
                    color = HeroGradientStart,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp,
                    lineHeight = (size.value * 0.38f).sp
                )
            }
        }
    }
}

@Composable
fun SplashIllustrationCanvas(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(155.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Sparkles
            val starColor = Color.White.copy(alpha = 0.72f)
            drawCircle(starColor, radius = 3.5.dp.toPx(), center = Offset(cx - 125.dp.toPx(), 24.dp.toPx()))
            drawCircle(starColor, radius = 4.dp.toPx(), center = Offset(cx + 128.dp.toPx(), 26.dp.toPx()))
            drawCircle(starColor, radius = 2.5.dp.toPx(), center = Offset(cx + 142.dp.toPx(), 98.dp.toPx()))
            drawCircle(starColor, radius = 2.5.dp.toPx(), center = Offset(cx - 140.dp.toPx(), 84.dp.toPx()))

            // Rising bars on the right
            val barColor = Color.White.copy(alpha = 0.88f)
            drawRoundRect(barColor, Offset(cx + 76.dp.toPx(), 98.dp.toPx()), Size(13.dp.toPx(), 36.dp.toPx()), CornerRadius(4.dp.toPx()))
            drawRoundRect(barColor, Offset(cx + 94.dp.toPx(), 80.dp.toPx()), Size(13.dp.toPx(), 54.dp.toPx()), CornerRadius(4.dp.toPx()))
            drawRoundRect(barColor, Offset(cx + 112.dp.toPx(), 60.dp.toPx()), Size(13.dp.toPx(), 74.dp.toPx()), CornerRadius(4.dp.toPx()))

            // Mint trend line
            val mintColor = Color(0xFF7CFFCB)
            val linePath = Path().apply {
                moveTo(cx + 72.dp.toPx(), 84.dp.toPx())
                lineTo(cx + 96.dp.toPx(), 66.dp.toPx())
                lineTo(cx + 120.dp.toPx(), 44.dp.toPx())
            }
            drawPath(
                path = linePath,
                color = mintColor,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawCircle(mintColor, radius = 5.dp.toPx(), center = Offset(cx + 120.dp.toPx(), 44.dp.toPx()))

            // Angled banknote peeking from top
            rotate(degrees = -8f, pivot = Offset(cx, cy - 26.dp.toPx())) {
                drawRoundRect(
                    color = mintColor,
                    topLeft = Offset(cx - 42.dp.toPx(), 24.dp.toPx()),
                    size = Size(84.dp.toPx(), 44.dp.toPx()),
                    cornerRadius = CornerRadius(7.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF12B886),
                    radius = 11.dp.toPx(),
                    center = Offset(cx, 46.dp.toPx())
                )
            }

            // Main Wallet card
            val walletLeft = cx - 64.dp.toPx()
            val walletTop = 58.dp.toPx()
            val walletW = 128.dp.toPx()
            val walletH = 78.dp.toPx()
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color.White, Color(0xFFE6E3FF)),
                    start = Offset(walletLeft, walletTop),
                    end = Offset(walletLeft + walletW, walletTop + walletH)
                ),
                topLeft = Offset(walletLeft, walletTop),
                size = Size(walletW, walletH),
                cornerRadius = CornerRadius(14.dp.toPx())
            )
            // Wallet coral top accent
            drawRoundRect(
                color = Color(0xFFF0506E),
                topLeft = Offset(walletLeft, walletTop),
                size = Size(walletW, 18.dp.toPx()),
                cornerRadius = CornerRadius(10.dp.toPx())
            )
            // Wallet indigo clasp
            drawRoundRect(
                color = HeroGradientStart,
                topLeft = Offset(walletLeft + walletW - 42.dp.toPx(), walletTop + 30.dp.toPx()),
                size = Size(48.dp.toPx(), 24.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(walletLeft + walletW - 28.dp.toPx(), walletTop + 42.dp.toPx())
            )

            // Stacked gold coins on left
            val coinCx = cx - 92.dp.toPx()
            val coinW = 54.dp.toPx()
            val coinH = 16.dp.toPx()
            drawOval(Color(0xFFE09F00), Offset(coinCx - coinW / 2, 124.dp.toPx()), Size(coinW, coinH))
            drawRect(Color(0xFFFFB703), Offset(coinCx - coinW / 2, 112.dp.toPx()), Size(coinW, 14.dp.toPx()))
            drawOval(Color(0xFFE09F00), Offset(coinCx - coinW / 2, 106.dp.toPx()), Size(coinW, coinH))
            drawRect(Color(0xFFFFB703), Offset(coinCx - coinW / 2, 96.dp.toPx()), Size(coinW, 14.dp.toPx()))
            drawOval(Color(0xFFFFDF80), Offset(coinCx - coinW / 2, 89.dp.toPx()), Size(coinW, coinH))
        }
    }
}

@Composable
fun LanguageAndThemePills(
    language: AppLanguage,
    isDark: Boolean,
    copy: LocalizedCopy,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onToggleLanguage,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.testTag("language_toggle_button")
        ) {
            Text(
                text = copy.langToggleLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
        Surface(
            onClick = onToggleTheme,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.testTag("theme_quick_toggle_button")
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = if (isDark) copy.themeLight else copy.themeDark,
                    tint = if (isDark) Color(0xFFFFD43B) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AppTopBar(
    copy: LocalizedCopy,
    language: AppLanguage,
    isDark: Boolean,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            BrandLogoMark(size = 36.dp)
            Column {
                Text(
                    text = copy.appTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = LocalFinanceColors.current.income
                    )
                    Text(
                        text = copy.syncedBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        LanguageAndThemePills(
            language = language,
            isDark = isDark,
            copy = copy,
            onToggleLanguage = onToggleLanguage,
            onToggleTheme = onToggleTheme
        )
    }
}

@Composable
fun LiveClockCard(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = FinanceFormatters.formatClockTime(nowMillis, language),
                style = MonoMediumStyle.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = FinanceFormatters.formatClockDate(nowMillis, language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MonthSwitcherBar(
    monthKey: String,
    language: AppLanguage,
    onShiftMonth: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            IconButton(
                onClick = { onShiftMonth(-1) },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("prev_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Previous month"
                )
            }
        }
        Text(
            text = FinanceFormatters.formatMonthDisplay(monthKey, language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            IconButton(
                onClick = { onShiftMonth(1) },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Next month"
                )
            }
        }
    }
}

@Composable
fun LowBalanceAlertBanner(
    balance: Double,
    limit: Double,
    currencyCode: String,
    language: AppLanguage,
    copy: LocalizedCopy,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    val balText = FinanceFormatters.formatCurrency(balance, currencyCode, lang = language)
    val limText = FinanceFormatters.formatCurrency(limit, currencyCode, lang = language)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = financeColors.expense.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, financeColors.expense.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = copy.lowBalanceTitle,
                tint = financeColors.expense,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = copy.lowBalanceTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = financeColors.expense
                )
                Text(
                    text = copy.lowBalanceBody(balText, limText),
                    style = MaterialTheme.typography.bodySmall,
                    color = financeColors.expense
                )
            }
        }
    }
}

@Composable
fun ShareAppCard(
    copy: LocalizedCopy,
    onCopied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = copy.shareWithFriends,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { shareAppText(context, copy) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_app_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(copy.shareLinkLabel.replace("📣 ", ""))
                }
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText(copy.appTitle, copy.shareMessage))
                        onCopied()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_share_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (copy.langToggleLabel == "EN") "কপি করুন" else "Copy Text")
                }
            }
        }
    }
}

fun shareAppText(context: Context, copy: LocalizedCopy) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, copy.appTitle)
        putExtra(Intent.EXTRA_TEXT, copy.shareMessage)
    }
    context.startActivity(Intent.createChooser(sendIntent, copy.shareWithFriends))
}

@Composable
fun PrivacyPolicyDialog(
    copy: LocalizedCopy,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(copy.privacyPolicyTitle, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = copy.privacyUpdated,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                copy.privacySections.forEach { (heading, body) ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(copy.ok)
            }
        }
    )
}
