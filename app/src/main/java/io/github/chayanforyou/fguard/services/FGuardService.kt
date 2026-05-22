package io.github.chayanforyou.fguard.services

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FGuardService : BaseBlockingService() {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()
    private var lastEventTimeStamp = 0L

    // 每日限额（毫秒），默认30分钟
    private val dailyLimitMs = 30 * 60 * 1000L

    // 当前正在追踪的使用（包名 → 开始时间戳）
    private var currentTrackedPackage: String? = null
    private var currentTrackStartMs: Long = 0L

    private val monitoredApps = hashSetOf(
        // === 短视频 (12款) ===
        "com.ss.android.ugc.aweme",       // 抖音
        "com.ss.android.ugc.aweme.lite",  // 抖音极速版
        "com.ss.android.ugc.live",        // 抖音火山版
        "com.smile.gifmaker",             // 快手
        "com.kuaishou.nebula",            // 快手极速版
        "com.tencent.weishi",             // 微视
        "com.xingin.xhs",                 // 小红书
        "tv.danmaku.bili",                // B站
        "com.ss.android.article.video",   // 西瓜视频
        "com.sup.android.superb",         // 皮皮虾
        "com.meitu.meipaimv",             // 美拍
        "com.google.android.youtube",     // YouTube
        // === 网文小说 (13款) ===
        "com.dragon.read",                // 番茄小说
        "com.chaozh.iReader",             // 掌阅
        "com.chaozh.iReaderFree",         // 掌阅免费版
        "com.zhangyue.read.ireadercn",    // 掌阅国际版
        "com.qidian.QDReader",            // 起点读书
        "com.qq.reader",                  // QQ阅读
        "com.tencent.weread",             // 微信读书
        "com.kmxs.reader",                // 七猫免费小说
        "com.shuqi.controller",           // 书旗小说
        "com.faloo.BookReader4Android",   // 飞卢小说
        "com.jjwxc.reader",              // 晋江文学城
        "com.ushaqi.zhuishushenqi",       // 追书神器
        "com.esbook.reader",              // 宜搜小说
    )

    // 微信内部界面拦截（仅限微信的视频号和直播，不误伤朋友圈和聊天）
    private val wechatPackage = "com.tencent.mm"
    private val blockedContentDescriptions = hashSetOf(
        "视频号",
        "直播",
        "直播中",
        "直播和附近",
    )

    private val prefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences("jieshua_usage", Context.MODE_PRIVATE)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.source == null) return

        val root = rootInActiveWindow ?: return
        val packageName = root.packageName?.toString() ?: return

        // 包名变化 → 结束上一个包的计时，开始新包的计时
        if (packageName != currentTrackedPackage) {
            endTrackingCurrent()
            startTrackingIfMonitored(packageName)
        }

        if (monitoredApps.contains(packageName)) {
            // 每个 WINDOW_STATE_CHANGED 检查是否超时
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                checkTimeLimitAndBlock()
            }
        }

        // 仅在微信内且未超时时检测视频号/直播界面
        if (packageName == wechatPackage &&
            !isOverDailyLimit() &&
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val descriptions = collectContentDescriptions(event.source)
            if (descriptions.any { text -> blockedContentDescriptions.any { blocked -> text.contains(blocked) } }) {
                // 开始追踪微信内刷视频号的时间
                startTrackingIfMonitored(wechatPackage + "/finder")
                checkTimeLimitAndBlock()
            }
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getTodayKey(): String = dateFormat.format(Date())

    private fun getTodayUsedMs(): Long {
        return prefs.getLong(getTodayKey(), 0L)
    }

    private fun isOverDailyLimit(): Boolean {
        val todayUsed = getTodayUsedMs()
        // 加上当前正在追踪的未保存时长
        val currentSession = if (currentTrackedPackage != null && currentTrackStartMs > 0L) {
            SystemClock.uptimeMillis() - currentTrackStartMs
        } else 0L
        return (todayUsed + currentSession) >= dailyLimitMs
    }

    private fun startTrackingIfMonitored(packageName: String) {
        if (monitoredApps.contains(packageName) ||
            packageName.startsWith(wechatPackage + "/finder")) {
            currentTrackedPackage = packageName
            currentTrackStartMs = SystemClock.uptimeMillis()
        }
    }

    private fun endTrackingCurrent() {
        val pkg = currentTrackedPackage ?: return
        if (currentTrackStartMs <= 0L) {
            currentTrackedPackage = null
            return
        }
        val elapsed = SystemClock.uptimeMillis() - currentTrackStartMs
        if (elapsed > 0L) {
            val todayKey = getTodayKey()
            val old = prefs.getLong(todayKey, 0L)
            prefs.edit().putLong(todayKey, old + elapsed).apply()
        }
        currentTrackedPackage = null
        currentTrackStartMs = 0L
    }

    private fun checkTimeLimitAndBlock() {
        if (!isOverDailyLimit()) return
        if (!isDelayOver(lastEventTimeStamp, 2000)) return

        coroutineScope.launch {
            mutex.withLock {
                performGlobalAction(GLOBAL_ACTION_HOME)
                lastEventTimeStamp = SystemClock.uptimeMillis()
            }
        }
    }

    private fun collectContentDescriptions(node: android.view.accessibility.AccessibilityNodeInfo?): Set<String> {
        val result = mutableSetOf<String>()
        if (node == null) return result
        node.contentDescription?.toString()?.let { result.add(it) }
        for (i in 0 until node.childCount) {
            result.addAll(collectContentDescriptions(node.getChild(i)))
        }
        return result
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        endTrackingCurrent()
        coroutineScope.cancel()
        super.onDestroy()
    }
}
