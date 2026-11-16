package com.tumba.ai.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.KeyOff
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tumba.ai.BuildConfig
import com.tumba.ai.R
import com.tumba.ai.domain.model.AppLanguage
import com.tumba.ai.domain.model.AppSettings
import com.tumba.ai.domain.model.ConversationStats
import com.tumba.ai.domain.model.ThemeMode
import com.tumba.ai.ui.components.ApiKeyDialog
import com.tumba.ai.ui.components.SegmentedControl
import com.tumba.ai.ui.components.SectionLabel
import com.tumba.ai.ui.components.TumbaCard
import java.util.Locale
import kotlin.math.roundToInt

private data class KeyTarget(val providerId: String, val displayName: String)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenLocalModels: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showPromptDialog by remember { mutableStateOf(false) }
    var showClearChats by remember { mutableStateOf(false) }
    var showClearKeys by remember { mutableStateOf(false) }
    var keyTarget by remember { mutableStateOf<KeyTarget?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportTo(uri) }

    LaunchedEffect(state.notice) {
        val notice = state.notice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(notice.messageRes))
        viewModel.clearNotice()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SettingsLinkCard(
                        icon = Icons.Rounded.Layers,
                        title = stringResource(R.string.models_title),
                        subtitle = stringResource(R.string.settings_models_desc),
                        onClick = onOpenModels,
                    )
                }
                item {
                    AppearanceCard(
                        settings = settings,
                        onTheme = viewModel::setTheme,
                        onLanguage = viewModel::setLanguage,
                    )
                }
                item {
                    AiBehaviorCard(
                        settings = settings,
                        onEditPrompt = { showPromptDialog = true },
                        onContext = viewModel::setContextMessages,
                        onTemperature = viewModel::setTemperature,
                        onStreaming = viewModel::setStreaming,
                    )
                }
                item {
                    ApiKeysCard(
                        keys = state.keys,
                        onManage = { providerId, displayName ->
                            keyTarget = KeyTarget(providerId, displayName)
                        },
                    )
                }
                item {
                    SettingsLinkCard(
                        icon = Icons.Rounded.Memory,
                        title = stringResource(R.string.settings_local_models),
                        subtitle = stringResource(R.string.settings_local_models_desc),
                        onClick = onOpenLocalModels,
                    )
                }
                item {
                    DataPrivacyCard(
                        stats = state.stats,
                        exporting = state.exporting,
                        onExport = { exportLauncher.launch("tumba-ai-export.json") },
                        onClearChats = { showClearChats = true },
                        onClearKeys = { showClearKeys = true },
                    )
                }
                item {
                    AboutCard(stats = state.stats)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    if (showPromptDialog) {
        SystemPromptDialog(
            initial = settings.systemPrompt,
            onDismiss = { showPromptDialog = false },
            onSave = { prompt ->
                viewModel.setSystemPrompt(prompt)
                showPromptDialog = false
            },
            onReset = {
                viewModel.setSystemPrompt("")
                showPromptDialog = false
            },
        )
    }

    keyTarget?.let { target ->
        ApiKeyDialog(
            title = stringResource(R.string.models_key_title, target.displayName),
            configured = state.keys.contains(target.providerId),
            onDismiss = { keyTarget = null },
            onSave = { key ->
                viewModel.saveApiKey(target.providerId, key)
                keyTarget = null
            },
            onRemove = {
                viewModel.removeApiKey(target.providerId)
                keyTarget = null
            },
        )
    }

    if (showClearChats) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_chats_title),
            message = stringResource(R.string.settings_clear_chats_message),
            confirm = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.deleteAllConversations()
                showClearChats = false
            },
            onDismiss = { showClearChats = false },
        )
    }

    if (showClearKeys) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_keys_title),
            message = stringResource(R.string.settings_clear_keys_message),
            confirm = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.deleteAllApiKeys()
                showClearKeys = false
            },
            onDismiss = { showClearKeys = false },
        )
    }
}

@Composable
private fun SettingsLinkCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    TumbaCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AppearanceCard(
    settings: AppSettings,
    onTheme: (ThemeMode) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
) {
    TumbaCard {
        SectionLabel(text = stringResource(R.string.settings_appearance))
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            options = listOf(
                stringResource(R.string.theme_system),
                stringResource(R.string.theme_light),
                stringResource(R.string.theme_dark),
            ),
            selectedIndex = when (settings.theme) {
                ThemeMode.SYSTEM -> 0
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
            },
            onSelect = { index ->
                onTheme(
                    when (index) {
                        0 -> ThemeMode.SYSTEM
                        1 -> ThemeMode.LIGHT
                        else -> ThemeMode.DARK
                    },
                )
            },
        )

        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            options = listOf(
                stringResource(R.string.language_system),
                stringResource(R.string.language_en),
                stringResource(R.string.language_uk),
            ),
            selectedIndex = when (settings.language) {
                AppLanguage.SYSTEM -> 0
                AppLanguage.ENGLISH -> 1
                AppLanguage.UKRAINIAN -> 2
            },
            onSelect = { index ->
                onLanguage(
                    when (index) {
                        0 -> AppLanguage.SYSTEM
                        1 -> AppLanguage.ENGLISH
                        else -> AppLanguage.UKRAINIAN
                    },
                )
            },
        )
    }
}

@Composable
private fun AiBehaviorCard(
    settings: AppSettings,
    onEditPrompt: () -> Unit,
    onContext: (Int) -> Unit,
    onTemperature: (Float) -> Unit,
    onStreaming: (Boolean) -> Unit,
) {
    var contextValue by remember(settings.contextMessages) {
        mutableFloatStateOf(settings.contextMessages.toFloat())
    }
    var temperatureValue by remember(settings.temperature) {
        mutableFloatStateOf(settings.temperature)
    }

    TumbaCard {
        SectionLabel(text = stringResource(R.string.settings_ai))
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_system_prompt),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = settings.systemPrompt.ifBlank {
                        stringResource(R.string.settings_default_prompt)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            TextButton(onClick = onEditPrompt) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.action_edit), style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(
            text = stringResource(R.string.settings_system_prompt_desc),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 14.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_context),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.settings_context_value, contextValue.roundToInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.settings_context_desc),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Slider(
            value = contextValue,
            onValueChange = { contextValue = it },
            valueRange = AppSettings.MIN_CONTEXT_MESSAGES.toFloat()..AppSettings.MAX_CONTEXT_MESSAGES.toFloat(),
            steps = 45,
            onValueChangeFinished = { onContext(contextValue.roundToInt()) },
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_temperature),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = String.format(Locale.getDefault(), "%.2f", temperatureValue),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.settings_temperature_desc),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Slider(
            value = temperatureValue,
            onValueChange = { temperatureValue = it },
            valueRange = AppSettings.MIN_TEMPERATURE..AppSettings.MAX_TEMPERATURE,
            onValueChangeFinished = { onTemperature(temperatureValue) },
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_streaming),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_streaming_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = settings.streaming, onCheckedChange = onStreaming)
        }
    }
}

@Composable
private fun ApiKeysCard(keys: Set<String>, onManage: (String, String) -> Unit) {
    val gemini = stringResource(R.string.models_provider_gemini)
    val deepSeek = stringResource(R.string.models_provider_deepseek)

    TumbaCard {
        SectionLabel(text = stringResource(R.string.settings_keys))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_keys_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        KeyRow(
            name = gemini,
            configured = keys.contains("gemini"),
            onManage = { onManage("gemini", gemini) },
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        KeyRow(
            name = deepSeek,
            configured = keys.contains("deepseek"),
            onManage = { onManage("deepseek", deepSeek) },
        )
    }
}

@Composable
private fun KeyRow(name: String, configured: Boolean, onManage: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (configured) {
                    stringResource(R.string.settings_key_saved)
                } else {
                    stringResource(R.string.settings_key_missing)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        TextButton(onClick = onManage) {
            Text(
                text = if (configured) {
                    stringResource(R.string.settings_update_key)
                } else {
                    stringResource(R.string.settings_set_key)
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun DataPrivacyCard(
    stats: ConversationStats,
    exporting: Boolean,
    onExport: () -> Unit,
    onClearChats: () -> Unit,
    onClearKeys: () -> Unit,
) {
    TumbaCard {
        SectionLabel(text = stringResource(R.string.settings_data))
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.settings_stats,
                stats.conversations,
                stats.messages,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        ActionRow(
            icon = Icons.Rounded.FileDownload,
            title = stringResource(R.string.settings_export),
            subtitle = stringResource(R.string.settings_export_desc),
            onClick = onExport,
            trailing = {
                if (exporting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            },
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        ActionRow(
            icon = Icons.Rounded.DeleteSweep,
            title = stringResource(R.string.settings_clear_chats),
            subtitle = stringResource(R.string.settings_clear_chats_desc),
            onClick = onClearChats,
            tint = MaterialTheme.colorScheme.error,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        ActionRow(
            icon = Icons.Rounded.KeyOff,
            title = stringResource(R.string.settings_clear_keys),
            subtitle = stringResource(R.string.settings_clear_keys_desc),
            onClick = onClearKeys,
            tint = MaterialTheme.colorScheme.error,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = stringResource(R.string.settings_privacy_desc),
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        trailing()
    }
}

@Composable
private fun AboutCard(stats: ConversationStats) {
    TumbaCard {
        SectionLabel(text = stringResource(R.string.settings_about))
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(12.dp))
        InfoRow(
            label = stringResource(R.string.settings_version),
            value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        )
        InfoRow(
            label = stringResource(R.string.settings_package),
            value = BuildConfig.APPLICATION_ID,
        )
        InfoRow(
            label = stringResource(R.string.settings_build),
            value = BuildConfig.BUILD_TYPE,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(
                R.string.settings_stats,
                stats.conversations,
                stats.messages,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SystemPromptDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.settings_system_prompt)) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    minLines = 5,
                    maxLines = 8,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.settings_system_prompt_hint),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onReset, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        text = stringResource(R.string.settings_default_prompt),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirm, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
