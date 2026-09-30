package pt.ipc_app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import pt.ipc_app.ui.theme.MediumBlue
import pt.ipc_app.ui.theme.White
import androidx.compose.ui.text.font.FontWeight


@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleFirst: Boolean = false,
    compact: Boolean = false,
    icon: ImageVector? = null,
    titleAlign: TextAlign = TextAlign.Start,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (compact) 6.dp else 12.dp),
        elevation = 0.dp,
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
        backgroundColor = MediumBlue
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = if (compact) 4.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                icon?.let { img ->
                    Icon(
                        imageVector = img,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(
                    modifier = when (titleAlign) {
                        TextAlign.Center -> Modifier.fillMaxWidth()
                        else -> Modifier
                    },
                    horizontalAlignment = when (titleAlign) {
                        TextAlign.Center -> Alignment.CenterHorizontally
                        TextAlign.End -> Alignment.End
                        else -> Alignment.Start
                    }
                ) {
                    if (subtitleFirst && subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.body2,
                            color = White.copy(alpha = 0.9f),
                            textAlign = titleAlign
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = title,
                        style = if (compact) MaterialTheme.typography.subtitle1 else MaterialTheme.typography.h5,
                        maxLines = if (compact) 1 else Int.MAX_VALUE,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        textAlign = titleAlign
                    )
                    if (!subtitleFirst && subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.body2,
                            color = White.copy(alpha = 0.9f),
                            textAlign = titleAlign
                        )
                    }
                }
            }
            actions()
        }
    }
}
