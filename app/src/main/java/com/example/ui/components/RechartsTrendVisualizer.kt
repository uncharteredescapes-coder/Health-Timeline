package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LabResult
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Data structure representing a point along the Recharts-like timeline.
 */
data class TrendMetricPoint(
    val timestamp: Long,
    val formattedDate: String,
    val value: Double,
    val unit: String,
    val referenceLow: Double? = null,
    val referenceHigh: Double? = null,
    val status: String = "normal",
    val note: String? = null
)

/**
 * A declarative, interactive Recharts-like data visualization component in Jetpack Compose.
 * Features:
 * - Smooth Monotone Cubic Spline curve
 * - Area Gradient fill under curve (<AreaChart />)
 * - Cartesian Grid with subtle dashed lines (<CartesianGrid strokeDasharray="3 3" />)
 * - X-Axis date ticks and Y-Axis numeric ticks (<XAxis />, <YAxis />)
 * - Reference normal range band and threshold boundary (<ReferenceArea />, <ReferenceLine />)
 * - Interactive touch scrubber & floating tooltip card on drag/tap (<Tooltip />)
 * - Dynamic Trend summary header with delta and status indicators
 */
@Composable
fun RechartsTrendVisualizer(
    metricName: String,
    points: List<TrendMetricPoint>,
    modifier: Modifier = Modifier,
    language: String = "en",
    accentColor: Color = Primary,
    availableMetrics: List<String> = emptyList(),
    selectedMetric: String = metricName,
    onMetricSelect: (String) -> Unit = {}
) {
    val t: (String, String) -> String = { en, bn -> if (language == "en") en else bn }
    val sortedPoints = remember(points) { points.sortedBy { it.timestamp } }

    var selectedIndex by remember(points) {
        mutableStateOf<Int?>(null)
    }

    val latestPoint = sortedPoints.lastOrNull()
    val previousPoint = if (sortedPoints.size >= 2) sortedPoints[sortedPoints.size - 2] else null
    val delta = if (latestPoint != null && previousPoint != null) latestPoint.value - previousPoint.value else 0.0

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp)),
        color = Surface,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Optional Metric Selection Pills (Tabs like Recharts multi-series)
            if (availableMetrics.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    items(availableMetrics) { metric ->
                        val isSelected = metric.equals(selectedMetric, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onMetricSelect(metric) },
                            label = {
                                Text(
                                    text = metric,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryTint,
                                selectedLabelColor = Primary,
                                containerColor = Surface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Primary else Line
                            )
                        )
                    }
                }
            }

            // Summary Header: Title, Latest Value, Trend Delta Badge, Reference Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = metricName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    if (latestPoint != null) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f", latestPoint.value),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = latestPoint.unit,
                                style = MaterialTheme.typography.titleSmall,
                                color = InkSoft,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                // Trend badge
                if (sortedPoints.size >= 2) {
                    val isDecreasing = delta < -0.01
                    val isIncreasing = delta > 0.01
                    val badgeBg = if (isDecreasing) PrimaryTint else if (isIncreasing) Alert.copy(alpha = 0.12f) else Line.copy(alpha = 0.5f)
                    val badgeColor = if (isDecreasing) Primary else if (isIncreasing) Alert else InkSoft

                    Surface(
                        shape = CircleShape,
                        color = badgeBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isDecreasing -> Icons.Default.TrendingDown
                                    isIncreasing -> Icons.Default.TrendingUp
                                    else -> Icons.Default.TrendingFlat
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = badgeColor
                            )
                            Text(
                                text = String.format(Locale.US, "%s%.1f %s", if (delta > 0) "+" else "", delta, latestPoint?.unit ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }
            }

            // Reference range caption
            val activePoint = if (selectedIndex != null && selectedIndex!! in sortedPoints.indices) {
                sortedPoints[selectedIndex!!]
            } else {
                latestPoint
            }

            if (activePoint?.referenceLow != null || activePoint?.referenceHigh != null) {
                val refStr = when {
                    activePoint.referenceLow != null && activePoint.referenceHigh != null ->
                        "${activePoint.referenceLow} – ${activePoint.referenceHigh} ${activePoint.unit}"
                    activePoint.referenceHigh != null ->
                        "< ${activePoint.referenceHigh} ${activePoint.unit}"
                    else -> "> ${activePoint.referenceLow} ${activePoint.unit}"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = InkSoft
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = t("Normal Reference Range: $refStr", "স্বাভাবিক সীমা: $refStr"),
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Interactive Tooltip Callout (Recharts <Tooltip />)
            AnimatedVisibility(
                visible = selectedIndex != null && selectedIndex!! in sortedPoints.indices,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val pt = sortedPoints[selectedIndex!!]
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Canvas,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = pt.formattedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft
                            )
                            Text(
                                text = "${pt.value} ${pt.unit}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (pt.status.equals("normal", ignoreCase = true)) PrimaryTint else Alert.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = pt.status.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (pt.status.equals("normal", ignoreCase = true)) Primary else Alert,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (sortedPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t("No recorded readings for $metricName", "$metricName এর জন্য কোনো তথ্য নেই"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft
                    )
                }
            } else if (sortedPoints.size == 1) {
                // Single point info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = Primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = t("1 reading recorded (${sortedPoints[0].formattedDate})", "১টি রেকর্ড নথিভুক্ত (${sortedPoints[0].formattedDate})"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink
                        )
                        Text(
                            text = t("Add or scan more reports to see longitudinal trend curves.", "সময়ের সাথে পরিবর্তন দেখতে আরও রিপোর্ট স্ক্যান বা যোগ করুন।"),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // Interactive Recharts Area & Spline Canvas
                val textMeasurer = rememberTextMeasurer()
                RechartsChartCanvas(
                    points = sortedPoints,
                    accentColor = accentColor,
                    selectedIndex = selectedIndex,
                    textMeasurer = textMeasurer,
                    onSelectIndex = { selectedIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = t("Drag or tap chart to inspect values", "মান দেখতে চার্টে ড্র্যাগ বা স্পর্শ করুন"),
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${sortedPoints.size} ${t("readings", "টি মান")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Canvas drawing the Recharts-inspired Area, Spline Line, Cartesian Grid, Axes, and interactive crosshair.
 */
@Composable
private fun RechartsChartCanvas(
    points: List<TrendMetricPoint>,
    accentColor: Color,
    selectedIndex: Int?,
    textMeasurer: TextMeasurer,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasWidth by remember { mutableFloatStateOf(1f) }

    Canvas(
        modifier = modifier
            .pointerInput(points) {
                detectTapGestures(
                    onPress = { offset ->
                        val index = findClosestPointIndex(offset.x, canvasWidth, points.size)
                        onSelectIndex(index)
                    }
                )
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = findClosestPointIndex(offset.x, canvasWidth, points.size)
                        onSelectIndex(index)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val index = findClosestPointIndex(change.position.x, canvasWidth, points.size)
                        onSelectIndex(index)
                    },
                    onDragEnd = {
                        // Keep selected point for inspection
                    },
                    onDragCancel = {
                        onSelectIndex(null)
                    }
                )
            }
    ) {
        canvasWidth = size.width
        val width = size.width
        val height = size.height

        val leftPadding = 42.dp.toPx()
        val rightPadding = 16.dp.toPx()
        val topPadding = 18.dp.toPx()
        val bottomPadding = 26.dp.toPx()

        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - topPadding - bottomPadding

        if (points.size < 2 || chartWidth <= 0 || chartHeight <= 0) return@Canvas

        val values = points.map { it.value }
        val rawMin = values.minOrNull() ?: 0.0
        val rawMax = values.maxOrNull() ?: 10.0

        // Include reference range in Y-axis bounds if present
        val refLow = points.mapNotNull { it.referenceLow }.firstOrNull()
        val refHigh = points.mapNotNull { it.referenceHigh }.firstOrNull()

        var minY = min(rawMin, refLow ?: rawMin)
        var maxY = max(rawMax, refHigh ?: rawMax)

        // Add 15% padding so curves don't clip at top/bottom
        val range = if (maxY - minY == 0.0) 1.0 else maxY - minY
        minY = max(0.0, minY - range * 0.15)
        maxY += range * 0.15
        val ySpan = maxY - minY

        fun xForIndex(index: Int): Float {
            return leftPadding + (index.toFloat() / (points.size - 1)) * chartWidth
        }

        fun yForValue(value: Double): Float {
            val norm = ((value - minY) / ySpan).toFloat().coerceIn(0f, 1f)
            return topPadding + chartHeight * (1f - norm)
        }

        // 1. Draw Cartesian Grid Lines (<CartesianGrid strokeDasharray="3 3" />)
        val gridLineCount = 4
        val gridColor = Line.copy(alpha = 0.7f)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        for (i in 0..gridLineCount) {
            val yVal = minY + (ySpan * i / gridLineCount)
            val yPos = yForValue(yVal)

            // Horizontal grid line
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, yPos),
                end = Offset(width - rightPadding, yPos),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )

            // Y-Axis Numeric Label (<YAxis />)
            val textLayoutResult = textMeasurer.measure(
                text = String.format(Locale.US, "%.1f", yVal),
                style = TextStyle(fontSize = 10.sp, color = InkSoft)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = max(0f, leftPadding - textLayoutResult.size.width - 6.dp.toPx()),
                    y = yPos - textLayoutResult.size.height / 2f
                )
            )
        }

        // 2. Reference Normal Range Band (<ReferenceArea />)
        if (refLow != null && refHigh != null) {
            val yRefLow = yForValue(refLow)
            val yRefHigh = yForValue(refHigh)
            val topBand = min(yRefLow, yRefHigh)
            val bottomBand = max(yRefLow, yRefHigh)

            drawRect(
                color = PrimaryTint.copy(alpha = 0.35f),
                topLeft = Offset(leftPadding, topBand),
                size = androidx.compose.ui.geometry.Size(chartWidth, bottomBand - topBand)
            )

            // Normal threshold boundary line (<ReferenceLine />)
            drawLine(
                color = Primary.copy(alpha = 0.4f),
                start = Offset(leftPadding, topBand),
                end = Offset(width - rightPadding, topBand),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        // 3. Compute Coordinates for Spline
        val coords = points.indices.map { i ->
            Offset(xForIndex(i), yForValue(points[i].value))
        }

        // 4. Smooth Cubic Bézier Spline Curve (Area & Line)
        val linePath = Path().apply {
            moveTo(coords[0].x, coords[0].y)
            for (i in 0 until coords.size - 1) {
                val p0 = coords[max(0, i - 1)]
                val p1 = coords[i]
                val p2 = coords[i + 1]
                val p3 = coords[min(coords.size - 1, i + 2)]

                // Catmull-Rom to Cubic Bézier conversion
                val cp1x = p1.x + (p2.x - p0.x) / 6f
                val cp1y = p1.y + (p2.y - p0.y) / 6f
                val cp2x = p2.x - (p3.x - p1.x) / 6f
                val cp2y = p2.y - (p3.y - p1.y) / 6f

                cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
            }
        }

        // Area Gradient Fill (<Area fill="url(#gradient)" />)
        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(coords.last().x, topPadding + chartHeight)
            lineTo(coords.first().x, topPadding + chartHeight)
            close()
        }

        val areaGradient = Brush.verticalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.32f),
                accentColor.copy(alpha = 0.08f),
                Color.Transparent
            ),
            startY = topPadding,
            endY = topPadding + chartHeight
        )
        drawPath(path = areaPath, brush = areaGradient)

        // Spline Stroke (<Line stroke="#00796B" strokeWidth="2.5" />)
        drawPath(
            path = linePath,
            color = accentColor,
            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 5. Data Points (<Dot />)
        coords.forEachIndexed { i, offset ->
            val isSelected = selectedIndex == i
            val ptColor = if (points[i].status.equals("normal", ignoreCase = true)) accentColor else Alert

            // White center with colored ring
            drawCircle(
                color = Color.White,
                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                center = offset
            )
            drawCircle(
                color = ptColor,
                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                center = offset,
                style = Stroke(width = if (isSelected) 3.dp.toPx() else 2.dp.toPx())
            )
        }

        // 6. X-Axis Date Labels (<XAxis />)
        val step = if (points.size > 5) 2 else 1
        for (i in points.indices step step) {
            val dateText = points[i].formattedDate
            val textLayoutResult = textMeasurer.measure(
                text = dateText,
                style = TextStyle(fontSize = 10.sp, color = InkSoft)
            )
            val x = (coords[i].x - textLayoutResult.size.width / 2f).coerceIn(
                leftPadding - 8f,
                width - rightPadding - textLayoutResult.size.width + 8f
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(x, topPadding + chartHeight + 6.dp.toPx())
            )
        }

        // 7. Interactive Scrubber Crosshair (<Tooltip cursor />)
        if (selectedIndex != null && selectedIndex in coords.indices) {
            val activeOffset = coords[selectedIndex]

            // Vertical crosshair indicator
            drawLine(
                color = accentColor.copy(alpha = 0.6f),
                start = Offset(activeOffset.x, topPadding),
                end = Offset(activeOffset.x, topPadding + chartHeight),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashEffect
            )

            // Outer Pulse Halo
            drawCircle(
                color = accentColor.copy(alpha = 0.22f),
                radius = 12.dp.toPx(),
                center = activeOffset
            )
            drawCircle(
                color = accentColor,
                radius = 6.dp.toPx(),
                center = activeOffset
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = activeOffset
            )
        }
    }
}

private fun findClosestPointIndex(touchX: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0) return 0
    val left = 42f
    val right = width - 16f
    val chartWidth = (right - left).coerceAtLeast(1f)
    val norm = ((touchX - left) / chartWidth).coerceIn(0f, 1f)
    val estimatedIndex = (norm * (count - 1)).toInt()
    return estimatedIndex.coerceIn(0, count - 1)
}

/**
 * Helper to convert a list of LabResult into Recharts trend points.
 */
fun List<LabResult>.toTrendPoints(dateFormat: String = "MMM dd"): List<TrendMetricPoint> {
    val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
    return this.sortedBy { it.collectedAt }.map { lab ->
        TrendMetricPoint(
            timestamp = lab.collectedAt,
            formattedDate = lab.reportDate ?: sdf.format(Date(lab.collectedAt)),
            value = lab.value,
            unit = lab.unit,
            referenceLow = lab.referenceLow,
            referenceHigh = lab.referenceHigh,
            status = lab.status,
            note = lab.laboratoryName
        )
    }
}
