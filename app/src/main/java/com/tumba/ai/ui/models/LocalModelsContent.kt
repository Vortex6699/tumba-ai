package com.tumba.ai.ui.models

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tumba.ai.R
import com.tumba.ai.core.formatBytes
import com.tumba.ai.data.ai.local.LocalEngineStatus
import com.tumba.ai.domain.model.DownloadableModel
import com.tumba.ai.domain.model.LocalModel
import com.tumba.ai.domain.model.LocalModelCatalog
import com.tumba.ai.ui.components.SectionLabel
import com.tumba.ai.ui.components.TumbaCard

@Composable
fun LocalModelsContent(
    state: ModelsUiState,
    onImport: () -> Unit,
    onDownload: (DownloadableModel) -> Unit,
    onCancelDownload: (DownloadableModel) -> Unit,
    onSelect: (LocalModel) -> Unit,
    onDelete: (LocalModel) -> Unit,
    estimateRuntimeBytes: (Long) -> Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val engine = state.engineStatus
    val available = state.memory.availableBytes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EngineCard(engine = engine)

        TumbaCard {
            SectionLabel(text = stringResource(R.string.models_memory))
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.models_memory_available, formatBytes(context, available)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.localModels.isNotEmpty()) {
                val largest = state.localModels.maxByOrNull { it.sizeBytes }
                val required = largest?.let { estimateRuntimeBytes(it.sizeBytes) } ?: 0L
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.models_memory_line,
                        formatBytes(context, available),
                        formatBytes(context, required),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (required > available) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.models_memory_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        ImportCard(
            importing = state.importing,
            onImport = onImport,
        )

        DownloadsCard(
            state = state,
            onDownload = onDownload,
            onCancelDownload = onCancelDownload,
        )

        if (state.localModels.isEmpty()) {
            TumbaCard {
                Text(
                    text = stringResource(R.string.models_local_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.models_local_empty_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            state.localModels.forEach { model ->
                LocalModelCard(
                    model = model,
                    selected = state.activeModel?.providerId == "local" &&
                        state.activeModel.id == model.id,
                    runtimeBytes = estimateRuntimeBytes(model.sizeBytes),
                    availableBytes = available,
                    onSelect = { onSelect(model) },
                    onDelete = { onDelete(model) },
                )
            }
            Text(
                text = stringResource(
                    R.string.models_storage,
                    formatBytes(context, state.storageUsed),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun EngineCard(engine: LocalEngineStatus) {
    val ready = engine is LocalEngineStatus.Ready
    TumbaCard {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Rounded.Memory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SectionLabel(text = stringResource(R.string.models_engine))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (ready) {
                        stringResource(R.string.models_engine_ready)
                    } else {
                        stringResource(R.string.models_engine_missing)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!ready) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.models_engine_missing_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val reason = (engine as LocalEngineStatus.Unavailable).reason
                    if (reason.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportCard(importing: Boolean, onImport: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !importing, onClick = onImport),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.UploadFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (importing) {
                    stringResource(R.string.models_importing)
                } else {
                    stringResource(R.string.models_import)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (importing) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DownloadsCard(
    state: ModelsUiState,
    onDownload: (DownloadableModel) -> Unit,
    onCancelDownload: (DownloadableModel) -> Unit,
) {
    val context = LocalContext.current
    TumbaCard {
        SectionLabel(text = stringResource(R.string.models_downloads_title))
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.models_downloads_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        LocalModelCatalog.downloads.forEachIndexed { index, spec ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            val download = state.downloadFor(spec.id)
            val downloaded = state.isDownloaded(spec.id)
            val fits = state.memory.totalBytes <= 0L ||
                spec.recommendedRamBytes <= state.memory.totalBytes
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = spec.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(
                                R.string.models_download_meta,
                                spec.parameters,
                                spec.quantization,
                                formatBytes(context, spec.sizeBytes),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!fits) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.models_memory_warning),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    when {
                        download != null -> {
                            Text(
                                text = stringResource(
                                    R.string.models_downloading,
                                    download.percent,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            IconButton(onClick = { onCancelDownload(spec) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(
                                        R.string.models_download_cancel,
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        downloaded -> {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        else -> {
                            TextButton(onClick = { onDownload(spec) }) {
                                Text(
                                    text = stringResource(R.string.models_download),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
                if (download != null) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { download.fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.onSurface,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.models_download_progress,
                            formatBytes(context, download.downloadedBytes),
                            formatBytes(context, download.totalBytes),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalModelCard(
    model: LocalModel,
    selected: Boolean,
    runtimeBytes: Long,
    availableBytes: Long,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val tight = runtimeBytes > availableBytes
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (selected) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.models_local_selected),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.models_size, formatBytes(context, model.sizeBytes)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    model.quantization?.let { quantization ->
                        Text(
                            text = quantization,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (tight) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.models_memory_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
