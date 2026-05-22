package io.github.chayanforyou.fguard

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.chayanforyou.fguard.services.FGuardService
import io.github.chayanforyou.fguard.ui.theme.AppTheme
import io.github.chayanforyou.fguard.utils.isAccessibilityServiceEnabled
import io.github.chayanforyou.fguard.utils.openAccessibilityServiceScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

private val dailyLimitMs = 30 * 60 * 1000L
private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isServiceRunning by remember { mutableStateOf(false) }
    var todayUsedMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceRunning = isAccessibilityServiceEnabled(context, FGuardService::class.java)
                val prefs = context.getSharedPreferences("jieshua_usage", Context.MODE_PRIVATE)
                todayUsedMs = prefs.getLong(dateFormat.format(Date()), 0L)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val usedMinutes = todayUsedMs / 60000
    val limitMinutes = dailyLimitMs / 60000
    val progress = (todayUsedMs.toFloat() / dailyLimitMs.toFloat()).coerceIn(0f, 1f)
    val isOver = todayUsedMs >= dailyLimitMs

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // App title
        Text(
            text = "戒刷",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "帮你少刷手机",
            fontSize = 14.sp,
            color = Color(0xFF999999)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isServiceRunning) {
            ActiveStateContent(usedMinutes, limitMinutes, progress, isOver)
        } else {
            InactiveStateContent(context)
        }
    }
}

@Composable
private fun ActiveStateContent(usedMinutes: Long, limitMinutes: Long, progress: Float, isOver: Boolean) {
    // Status badge
    StatusBadge(
        icon = Icons.Rounded.CheckCircle,
        text = "拦截中",
        color = Color(0xFF4CAF50)
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Usage circle card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "今日使用",
                fontSize = 13.sp,
                color = Color(0xFF999999)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$usedMinutes / $limitMinutes 分钟",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOver) Color(0xFFE53935) else Color(0xFF1A1A2E)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOver) Color(0xFFE53935) else Color(0xFF4CAF50),
                trackColor = Color(0xFFE0E0E0),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isOver) "今日额度已用完" else "剩余 ${limitMinutes - usedMinutes} 分钟",
                fontSize = 13.sp,
                color = if (isOver) Color(0xFFE53935) else Color(0xFF666666)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // What's blocked
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "拦截清单",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "快手 / 抖音 / B站 / 小红书 / 西瓜视频 / 皮皮虾 / 美拍 / YouTube",
                fontSize = 13.sp,
                color = Color(0xFF666666),
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "番茄小说 / 掌阅 / 起点读书 / QQ阅读 / 微信读书 / 七猫 / 书旗 / 飞卢 / 晋江 / 追书神器 / 宜搜",
                fontSize = 13.sp,
                color = Color(0xFF666666),
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "微信视频号 / 微信直播",
                fontSize = 13.sp,
                color = Color(0xFF666666),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun InactiveStateContent(context: Context) {
    // Status badge
    StatusBadge(
        icon = Icons.Rounded.Warning,
        text = "未开启",
        color = Color(0xFFFF9800)
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Intro text
    Text(
        text = "只需设置一次，之后无需任何操作",
        fontSize = 15.sp,
        color = Color(0xFF666666),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Step-by-step guide card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "设置步骤",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E)
            )
            Spacer(modifier = Modifier.height(16.dp))
            SetupStep(number = "1", text = "点击下方「去开启」按钮")
            Spacer(modifier = Modifier.height(12.dp))
            SetupStep(number = "2", text = "在列表中向下滑，找到「戒刷」")
            Spacer(modifier = Modifier.height(12.dp))
            SetupStep(number = "3", text = "打开「戒刷」右侧的开关")
            Spacer(modifier = Modifier.height(12.dp))
            SetupStep(number = "4", text = "在弹出的提示中点击「允许」")
            Spacer(modifier = Modifier.height(12.dp))
            SetupStep(number = "5", text = "返回本页面，看到「拦截中」即成功")
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Action button
    Button(
        onClick = { openAccessibilityServiceScreen(context, FGuardService::class.java) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4A6CF7)
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.Shield,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "去开启", fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "为什么需要手动开启？\nAndroid 系统限制，无障碍服务必须由用户主动授权，App 无法替您自动打开。开启一次后永久生效。",
        fontSize = 12.sp,
        color = Color(0xFFAAAAAA),
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
    )
}

@Composable
private fun StatusBadge(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun SetupStep(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF4A6CF7))
        ) {
            Text(
                text = number,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF333333)
        )
    }
}
