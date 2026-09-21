package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = InkSoft
) {
    Text(
        text = text.uppercase(),
        style = EyebrowStyle,
        color = color,
        modifier = modifier
    )
}

@Composable
fun MonthHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = MonthLabelStyle,
        color = InkSoft,
        modifier = modifier
    )
}

enum class BadgeType {
    OK, WATCH, ATTN, NEUTRAL
}

@Composable
fun StatusBadge(
    text: String,
    type: BadgeType = BadgeType.OK,
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor) = when (type) {
        BadgeType.OK -> PrimaryTint to Primary
        BadgeType.WATCH -> AccentTint to Accent
        BadgeType.ATTN -> AlertTint to Alert
        BadgeType.NEUTRAL -> Line to InkSoft
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CheckCircle(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animatePop by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (animatePop) 1.28f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { animatePop = false },
        label = "pop"
    )
    val bgColor by animateColorAsState(
        targetValue = if (checked) Primary else Color.Transparent,
        label = "bgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) Primary else Line,
        label = "borderColor"
    )

    Box(
        modifier = modifier
            .size(26.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable {
                animatePop = true
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
fun DesignPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color.White,
            disabledContainerColor = Primary.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.8f)
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            fontFamily = BodyFontFamily
        )
    }
}

@Composable
fun DesignOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, Primary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Primary
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            fontFamily = BodyFontFamily,
            color = Primary
        )
    }
}

@Composable
fun DesignTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = InkSoft
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            fontFamily = BodyFontFamily,
            color = color
        )
    }
}

@Composable
fun LinkRow(
    title: String,
    meta: String? = null,
    actionText: String? = null,
    trailingChevron: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                fontFamily = BodyFontFamily
            )
            if (!meta.isNullOrEmpty()) {
                Text(
                    text = meta,
                    fontSize = 12.sp,
                    color = InkSoft,
                    fontFamily = BodyFontFamily,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (!actionText.isNullOrEmpty()) {
            Text(
                text = actionText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
                fontFamily = BodyFontFamily
            )
        } else if (trailingChevron) {
            Text(
                text = "›",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = InkSoft,
                fontFamily = BodyFontFamily
            )
        }
    }
}

@Composable
fun SummaryBlock(
    eyebrowTitle: String,
    body: String,
    cite: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line),
        color = Surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = eyebrowTitle.uppercase(),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                color = InkSoft,
                fontFamily = BodyFontFamily,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = body,
                    fontSize = 13.5.sp,
                    lineHeight = 21.sp,
                    color = Ink,
                    fontFamily = BodyFontFamily,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!cite.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = PrimaryTint,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = cite,
                            color = Primary,
                            style = CiteStyle,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroundingNote(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "✓",
            color = Primary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Text(
            text = text,
            color = InkSoft,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            fontFamily = BodyFontFamily
        )
    }
}
