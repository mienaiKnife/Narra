package com.mienaiknife.narra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mienaiknife.narra.data.models.Article
import com.mienaiknife.narra.ui.theme.NarraTheme

@Composable
fun ArticleCard(
    article: Article,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(140.dp),
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column {
            // Image Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFF2C2C2C)),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Progress Bar at the bottom of the image area
                article.progress?.let { progress ->
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFF1B3134),
                        strokeCap = StrokeCap.Butt,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9E9E9E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ArticleCardPreview() {
    NarraTheme {
        ArticleCard(
            article = Article(
                id = "1",
                title = "Why training AI can't be IP theft",
                source = "blog.giovanh.com",
                progress = 0.65f
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
