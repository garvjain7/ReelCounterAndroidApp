package com.example.brainrot.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brainrot.data.ReelDao
import com.example.brainrot.service.ReelSessionManager
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun DashboardScreen(reelDao: ReelDao) {
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // 1. Observe Database Counts (Saved data)
    val dbTodayCount by reelDao.getTodayCount(todayStart).collectAsState(initial = 0)
    val dbIgCount by reelDao.getCountByApp("Instagram").collectAsState(initial = 0)
    val dbYtCount by reelDao.getCountByApp("YouTube").collectAsState(initial = 0)

    // 2. Observe Live Session Counts (Unsaved memory shards)
    val sessionCounts by ReelSessionManager.sessionCounts.collectAsState()
    val sessionIg = sessionCounts["Instagram"] ?: 0
    val sessionYt = sessionCounts["YouTube"] ?: 0

    // 3. Combined "Live" Truth
    val liveTodayCount = dbTodayCount + sessionIg + sessionYt
    val liveIgCount = dbIgCount + sessionIg
    val liveYtCount = dbYtCount + sessionYt

    val last7DaysCounts = remember { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val counts = mutableListOf<Int>()
            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                val end = start + (24 * 60 * 60 * 1000) - 1
                counts.add(reelDao.getCountInRange(start, end))
            }
            last7DaysCounts.clear()
            last7DaysCounts.addAll(counts)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp)
    ) {
        item {
            Text(
                text = "Your Activity",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            MainCounter(liveTodayCount)
            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            PlatformBreakdown(liveIgCount, liveYtCount)
            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            WeeklyChart(last7DaysCounts)
        }
    }
}

@Composable
fun MainCounter(count: Int) {
    val goal = 100f
    val progress by animateFloatAsState(
        targetValue = (count / goal).coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(220.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(200.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            strokeWidth = 12.dp,
            strokeCap = StrokeCap.Round
        )

        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(200.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 12.dp,
            strokeCap = StrokeCap.Round
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                fontSize = 56.sp
            )
            Text(
                text = "Reels Today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlatformBreakdown(ig: Int, yt: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PlatformCard("Instagram", ig, Color(0xFFE1306C), Modifier.weight(1f))
        PlatformCard("YouTube", yt, Color(0xFFFF0000), Modifier.weight(1f))
    }
}

@Composable
fun PlatformCard(name: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "$count", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = name, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun WeeklyChart(counts: List<Int>) {
    if (counts.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Last 7 Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val maxCount = (counts.maxOrNull() ?: 1).coerceAtLeast(1)
            counts.forEachIndexed { index, count ->
                val barHeight = (count.toFloat() / maxCount.toFloat()) * 100
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(barHeight.dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (index == counts.size - 1) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(getDayLabel(6 - index), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

fun getDayLabel(daysAgo: Int): String {
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "S"; Calendar.MONDAY -> "M"; Calendar.TUESDAY -> "T"
        Calendar.WEDNESDAY -> "W"; Calendar.THURSDAY -> "T"; Calendar.FRIDAY -> "F"
        Calendar.SATURDAY -> "S"; else -> ""
    }
}
