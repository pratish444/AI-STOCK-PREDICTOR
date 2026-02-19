package com.example.stocktracker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stocktracker.domain.model.SentimentResult
import com.example.stocktracker.ui.theme.Green
import com.example.stocktracker.ui.theme.Red

@Composable
fun SentimentBadge(
    sentiment: SentimentResult?,
    modifier: Modifier = Modifier
) {
    if (sentiment == null) return

    val (backgroundColor, contentColor, icon) = when (sentiment.overallLabel.lowercase()) {
        "positive", "bullish" -> Triple(Green.copy(alpha = 0.1f), Green, Icons.Default.ThumbUp)
        "negative", "bearish" -> Triple(Red.copy(alpha = 0.1f), Red, Icons.Default.ThumbDown)
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Remove
        )
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = sentiment.recommendation.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${(sentiment.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SentimentDetailCard(
    sentiment: SentimentResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "News Sentiment Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                SentimentBadge(sentiment = sentiment)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sentiment Distribution
            Text(
                text = "Sentiment Distribution",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SentimentBar(
                bullish = sentiment.bullishCount,
                bearish = sentiment.bearishCount,
                neutral = sentiment.neutralCount
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Overall Score
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall Score: ",
                    style = MaterialTheme.typography.bodyMedium
                )
                val scoreColor = when {
                    sentiment.overallScore > 0.3 -> Green
                    sentiment.overallScore < -0.3 -> Red
                    else -> MaterialTheme.colorScheme.primary
                }
                Text(
                    text = String.format("%.2f", sentiment.overallScore),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Recent sentiments
            if (sentiment.sentiments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Recent Analysis",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                sentiment.sentiments.take(3).forEach { item ->
                    SentimentListItem(item = item)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SentimentBar(bullish: Int, bearish: Int, neutral: Int) {
    val total = (bullish + bearish + neutral).coerceAtLeast(1)
    val bullishPct = bullish.toFloat() / total
    val bearishPct = bearish.toFloat() / total
    val neutralPct = neutral.toFloat() / total

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        if (bullishPct > 0) {
            Box(
                modifier = Modifier
                    .weight(bullishPct)
                    .fillMaxHeight()
                    .background(Green)
            )
        }
        if (neutralPct > 0) {
            Box(
                modifier = Modifier
                    .weight(neutralPct)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
        if (bearishPct > 0) {
            Box(
                modifier = Modifier
                    .weight(bearishPct)
                    .fillMaxHeight()
                    .background(Red)
            )
        }
    }

    // Legend
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(color = Green, label = "Bullish", count = bullish)
        LegendItem(color = MaterialTheme.colorScheme.outline, label = "Neutral", count = neutral)
        LegendItem(color = Red, label = "Bearish", count = bearish)
    }
}

@Composable
private fun LegendItem(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun SentimentListItem(item: com.example.stocktracker.domain.model.SentimentItem) {
    val color = when (item.label.lowercase()) {
        "positive" -> Green
        "negative" -> Red
        else -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(item.score * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}