package com.tumba.ai.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tumba.ai.R
import com.tumba.ai.domain.model.Message
import com.tumba.ai.domain.model.MessageRole
import com.tumba.ai.domain.model.MessageStatus
import com.tumba.ai.ui.components.LoadingDots
import com.tumba.ai.ui.components.TumbaMark
import com.tumba.ai.ui.markdown.MarkdownText
import java.io.File

@Composable
fun MessageBubble(
    message: Message,
    overrideContent: String?,
    canRegenerate: Boolean,
    onCopy: (String) -> Unit,
    onRegenerate: () -> Unit,
    onImageClick: (String) -> Unit,
) {
    val content = overrideContent ?: message.content
    if (message.role == MessageRole.USER) {
        UserMessage(
            content = content,
            message = message,
            onImageClick = onImageClick,
        )
    } else {
        AssistantMessage(
            content = content,
            message = message,
            canRegenerate = canRegenerate,
            onCopy = onCopy,
            onRegenerate = onRegenerate,
        )
    }
}

@Composable
private fun UserMessage(
    content: String,
    message: Message,
    onImageClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        if (message.attachments.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                message.attachments.forEach { attachment ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(104.dp)
                            .clickable { onImageClick(attachment.path) },
                    ) {
                        AsyncImage(
                            model = File(attachment.path),
                            contentDescription = stringResource(R.string.chat_fullscreen_image),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        if (content.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 8.dp, bottomStart = 22.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                ),
            ) {
                SelectionContainer {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantMessage(
    content: String,
    message: Message,
    canRegenerate: Boolean,
    onCopy: (String) -> Unit,
    onRegenerate: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        TumbaMark(size = 28.dp, modifier = Modifier.padding(top = 1.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.label_tumba),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            if (content.isBlank()) {
                if (message.status == MessageStatus.STREAMING) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LoadingDots()
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.chat_generating),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.chat_message_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                MarkdownText(markdown = content)
            }

            when (message.status) {
                MessageStatus.CANCELLED -> StatusNote(
                    text = stringResource(R.string.chat_stopped),
                    icon = Icons.Rounded.StopCircle,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                MessageStatus.ERROR -> StatusNote(
                    text = stringResource(R.string.chat_message_failed),
                    icon = Icons.Rounded.ErrorOutline,
                    tint = MaterialTheme.colorScheme.error,
                )

                else -> Unit
            }

            if (content.isNotBlank() || message.status == MessageStatus.ERROR) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (content.isNotBlank()) {
                        MessageAction(
                            icon = Icons.Rounded.ContentCopy,
                            label = stringResource(R.string.action_copy),
                            onClick = { onCopy(content) },
                        )
                    }
                    if (canRegenerate && message.status != MessageStatus.STREAMING) {
                        MessageAction(
                            icon = Icons.Rounded.Refresh,
                            label = stringResource(R.string.chat_regenerate),
                            onClick = onRegenerate,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusNote(
    text: String,
    icon: ImageVector,
    tint: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}

@Composable
fun TypingDots() {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        LoadingDots()
    }
}
