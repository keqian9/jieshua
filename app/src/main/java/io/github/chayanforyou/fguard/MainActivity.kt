package io.github.chayanforyou.fguard

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
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
                    AccessibilityApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

private val dailyLimitMs = 30 * 60 * 1000L
private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

@Composable
fun AccessibilityApp(modifier: Modifier = Modifier) {
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

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isServiceRunning) "拦截服务运行中" else "拦截服务未开启",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 今日使用进度
            Text(
                text = "今日已用 $usedMinutes / $limitMinutes 分钟",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOver) Color(0xFFE53935) else Color.Unspecified
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .height(8.dp),
                color = if (isOver) Color(0xFFE53935) else Color(0xFF4CAF50),
                trackColor = Color(0xFFE0E0E0),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isOver) "今日额度已用完" else "剩余 ${limitMinutes - usedMinutes} 分钟",
                fontSize = 13.sp,
                color = if (isOver) Color(0xFFE53935) else Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "已拦截：抖音/快手/B站/小红书/番茄小说/起点等25款App",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { openAccessibilityServiceScreen(context, FGuardService::class.java) },
                shape = CircleShape,
                modifier = Modifier.size(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceRunning) Color(0xFF4CAF50) else Color(0xFF2196F3)
                )
            ) {
                Text(
                    text = if (isServiceRunning) "已开启" else "去开启",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "点击按钮 → 找到「戒刷」→ 打开开关",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}
