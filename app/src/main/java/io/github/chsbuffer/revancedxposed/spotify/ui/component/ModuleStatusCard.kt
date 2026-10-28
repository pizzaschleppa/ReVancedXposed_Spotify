@file:Suppress("ktlint:standard:function-naming")

package io.github.chsbuffer.revancedxposed.spotify.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.chsbuffer.revancedxposed.spotify.ui.theme.Tokens


@Composable
fun ModuleStatusCard(
    isActive: Boolean,
    versionName: String,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isActive)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.errorContainer,
        animationSpec = tween(durationMillis = 500),
        label = "statusColor",
    )

    val contentColor by animateColorAsState(
        targetValue = if (isActive)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onErrorContainer,
        animationSpec = tween(durationMillis = 500),
        label = "statusContentColor",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Tokens.StatusCardCornerRadius),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Tokens.SpacingXl, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.SpacingLg),
        ) {
            // Status icon
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Rounded.Check else Icons.Rounded.PriorityHigh,
                    contentDescription = if (isActive) "Module active" else "Module not active",
                    modifier = Modifier.size(28.dp),
                )
            }

            // Labels
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (isActive) "Module is active" else "Module is not active",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (isActive) "v$versionName" else "Activate in your Xposed manager",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
