package com.tumba.ai.ui.models

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tumba.ai.R
import com.tumba.ai.core.ProviderIds
import com.tumba.ai.domain.model.CustomProvider
import com.tumba.ai.domain.model.LocalModel
import com.tumba.ai.domain.model.ModelInfo
import com.tumba.ai.ui.components.SegmentedControl
import com.tumba.ai.ui.components.SectionLabel
import com.tumba.ai.ui.components.TumbaCard
import com.tumba.ai.ui.util.aiErrorMessage
import com.tumba.ai.ui.util.contextWindowLabel
import com.tumba.ai.ui.util.providerDisplayName
import com.tumba.ai.ui.util.providerName

private data class KeyTarget(
    val providerId: String,
    val displayName: String,
    val configured: Boolean,
)

@Composable
fun ModelsScreen(
    onBack: () -> Unit,
    viewModel: ModelsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var tab by remember { mutableIntStateOf(0) }
    var showAddCustom by remember { mutableStateOf(false) }
    var editingCustom by remember { mutableStateOf<CustomProvider?>(null) }
    var deletingCustom by remember { mutableStateOf<CustomProvider?>(null) }
    var deletingLocal by remember { mutableStateOf<LocalModel?>(null) }
    var keyTarget by remember { mutableStateOf<KeyTarget?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importLocalModel(uri) }

    LaunchedEffect(state.notice) {
        val notice = state.notice ?: return@LaunchedEffect
        val message = if (notice.argument != null) {
            context.getString(notice.messageRes, notice.argument)
        } else {
            context.getString(notice.messageRes)
        }
        snackbarHostState.showSnackbar(message)
        viewModel.clearNotice()
    }

    LaunchedEffect(state.errorRes) {
        val res = state.errorRes ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(res))
        viewModel.clearError()
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
                    .padding(start = 4.dp, end = 20.dp, top = 4.dp),
                verticalAlignment = Alignment.Top,
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
                        text = stringResource(R.string.models_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.models_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            SegmentedControl(
                options = listOf(
                    stringResource(R.string.models_tab_cloud),
                    stringResource(R.string.models_tab_local),
                ),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )

            if (tab == 0) {
                CloudModelsTab(
                    state = state,
                    onSelect = viewModel::selectModel,
                    onTest = viewModel::testConnection,
                    onSyncModels = { viewModel.syncModels(ProviderIds.GEMINI) },
                    onManageKey = { section ->
                        keyTarget = KeyTarget(
                            providerId = section.providerId,
                            displayName = section.nameOverride
                                ?: providerName(context, section.providerId),
                            configured = section.keyConfigured,
                        )
                    },
                    onAddCustom = { showAddCustom = true },
                    onEditCustom = { providerId ->
                        editingCustom = state.customProviders.firstOrNull {
                            it.id == providerId.removePrefix("custom:")
                        }
                    },
                    onDeleteCustom = { providerId ->
                        deletingCustom = state.customProviders.firstOrNull {
                            it.id == providerId.removePrefix("custom:")
                        }
                    },
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    LocalModelsContent(
                        state = state,
                        onImport = { importLauncher.launch(arrayOf("*/*")) },
                        onDownload = viewModel::downloadModel,
                        onCancelDownload = viewModel::cancelDownload,
                        onSelect = viewModel::selectLocalModel,
                        onDelete = { deletingLocal = it },
                        estimateRuntimeBytes = viewModel::estimateRuntimeBytes,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Spacer(Modifier.height(24.dp))
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

    if (showAddCustom) {
        CustomProviderDialog(
            initial = null,
            hasSavedKey = false,
            onDismiss = { showAddCustom = false },
            onSave = { draft ->
                viewModel.addCustom(draft)
                showAddCustom = false
            },
        )
    }

    editingCustom?.let { provider ->
        val providerId = "custom:${provider.id}"
        CustomProviderDialog(
            initial = provider,
            hasSavedKey = state.keys.contains(providerId),
            onDismiss = { editingCustom = null },
            onSave = { draft ->
                viewModel.updateCustom(provider.id, draft)
                editingCustom = null
            },
        )
    }

    deletingCustom?.let { provider ->
        AlertDialog(
            onDismissRequest = { deletingCustom = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.models_delete_custom)) },
            text = { Text(stringResource(R.string.models_delete_custom_message, provider.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustom(provider)
                        deletingCustom = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCustom = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    deletingLocal?.let { model ->
        AlertDialog(
            onDismissRequest = { deletingLocal = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.models_local_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.models_local_delete_message,
                        model.displayName,
                        android.text.format.Formatter.formatShortFileSize(context, model.sizeBytes),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLocalModel(model)
                        deletingLocal = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingLocal = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    keyTarget?.let { target ->
        com.tumba.ai.ui.components.ApiKeyDialog(
            title = stringResource(R.string.models_key_title, target.displayName),
            configured = target.configured,
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
}

@Composable
private fun CloudModelsTab(
    state: ModelsUiState,
    onSelect: (ModelInfo) -> Unit,
    onTest: (ModelInfo) -> Unit,
    onSyncModels: () -> Unit,
    onManageKey: (ProviderSectionUi) -> Unit,
    onAddCustom: () -> Unit,
    onEditCustom: (String) -> Unit,
    onDeleteCustom: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ActiveModelCard(state.activeModel) }

        items(state.sections, key = { it.providerId }) { section ->
            ProviderCard(
                section = section,
                state = state,
                onSelect = onSelect,
                onTest = onTest,
                onManageKey = onManageKey,
                onEditCustom = onEditCustom,
                onDeleteCustom = onDeleteCustom,
                syncing = state.syncingProviders.contains(section.providerId),
                onSync = if (section.providerId == ProviderIds.GEMINI &&
                    section.keyConfigured
                ) {
                    onSyncModels
                } else {
                    null
                },
            )
        }

        item {
            AddCustomApiCard(onClick = onAddCustom)
        }

        item {
            Text(
                text = stringResource(R.string.settings_keys_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
            )
        }
    }
}

@Composable
private fun ActiveModelCard(activeModel: ModelInfo?) {
    TumbaCard {
        SectionLabel(text = stringResource(R.string.models_active_label))
        Spacer(Modifier.height(10.dp))
        if (activeModel == null) {
            Text(
                text = stringResource(R.string.models_none_active),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeModel.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = providerDisplayName(activeModel.providerId),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        contextWindowLabel(activeModel.contextWindow)?.let { context ->
                            Text(
                                text = context,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (activeModel.capabilities.supportsImages) {
                            Text(
                                text = stringResource(R.string.models_vision_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.models_selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    section: ProviderSectionUi,
    state: ModelsUiState,
    onSelect: (ModelInfo) -> Unit,
    onTest: (ModelInfo) -> Unit,
    onManageKey: (ProviderSectionUi) -> Unit,
    onEditCustom: (String) -> Unit,
    onDeleteCustom: (String) -> Unit,
    syncing: Boolean,
    onSync: (() -> Unit)?,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val target = section.models.firstOrNull { state.isActive(it) } ?: section.models.firstOrNull()
    val connection = target?.let { state.testStateFor(it) }
    val displayName = section.nameOverride ?: providerDisplayName(section.providerId)

    TumbaCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (section.isCustom) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.models_custom),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when {
                        section.keyConfigured -> stringResource(R.string.models_key_configured)
                        section.isCustom -> stringResource(R.string.models_key_optional)
                        else -> stringResource(R.string.models_key_missing)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (section.requiresKey && !section.keyConfigured) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (onSync != null) {
                IconButton(onClick = onSync, enabled = !syncing) {
                    if (syncing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.models_sync),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (section.isCustom) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.cd_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.models_edit_custom)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onEditCustom(section.providerId)
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.models_delete_custom),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDeleteCustom(section.providerId)
                            },
                        )
                    }
                }
            }
        }

        section.descriptionRes?.let { descriptionRes ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onSync != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.models_sync_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(10.dp))

        section.models.forEach { model ->
            ModelRow(
                model = model,
                selected = state.isActive(model),
                onClick = { onSelect(model) },
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onManageKey(section) }) {
                Text(
                    text = if (section.keyConfigured) {
                        stringResource(R.string.models_manage_key)
                    } else {
                        stringResource(R.string.settings_set_key)
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { target?.let(onTest) },
                enabled = target != null && connection !is ConnectionState.Testing,
            ) {
                if (connection is ConnectionState.Testing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.models_testing),
                        style = MaterialTheme.typography.labelMedium,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.models_test_connection),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        when (connection) {
            is ConnectionState.Success -> Text(
                text = stringResource(R.string.models_connected, connection.latencyMs.toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            is ConnectionState.Failure -> Text(
                text = aiErrorMessage(connection.error, section.providerId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )

            else -> Unit
        }
    }
}

@Composable
private fun ModelRow(model: ModelInfo, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    } else {
                        Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                contextWindowLabel(model.contextWindow)?.let { context ->
                    Text(
                        text = context,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (model.capabilities.supportsImages) {
                        stringResource(R.string.models_vision)
                    } else {
                        stringResource(R.string.models_no_vision)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (selected) {
            Text(
                text = stringResource(R.string.models_selected),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (model.isDefault) {
            Text(
                text = stringResource(R.string.models_default_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AddCustomApiCard(onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.models_add_custom),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}


@Composable
private fun CustomProviderDialog(
    initial: CustomProvider?,
    hasSavedKey: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomProviderDraft) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl.orEmpty()) }
    var modelId by remember { mutableStateOf(initial?.modelId.orEmpty()) }
    var apiKey by remember { mutableStateOf("") }
    var contextWindow by remember {
        mutableStateOf(initial?.contextWindow?.toString().orEmpty())
    }
    var supportsImages by remember { mutableStateOf(initial?.supportsImages ?: false) }
    var showError by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }

    val urlValid = baseUrl.trim().startsWith("http://") || baseUrl.trim().startsWith("https://")
    val contextValid = contextWindow.isBlank() || contextWindow.toIntOrNull() != null
    val valid = name.isNotBlank() && modelId.isNotBlank() && urlValid && contextValid

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        title = {
            Text(
                text = if (initial == null) {
                    stringResource(R.string.models_add_custom)
                } else {
                    stringResource(R.string.models_edit_custom)
                },
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.models_name)) },
                    placeholder = { Text(stringResource(R.string.models_name_hint)) },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.models_base_url)) },
                    placeholder = { Text(stringResource(R.string.models_base_url_hint)) },
                    isError = showError && !urlValid,
                    supportingText = {
                        if (showError && !urlValid) {
                            Text(stringResource(R.string.models_custom_invalid_url))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.models_model_id)) },
                    placeholder = { Text(stringResource(R.string.models_model_id_hint)) },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.models_api_key)) },
                    placeholder = {
                        Text(
                            text = if (hasSavedKey) {
                                stringResource(R.string.models_key_keep_hint)
                            } else {
                                stringResource(R.string.label_optional)
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    visualTransformation = if (showKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) {
                                    Icons.Rounded.VisibilityOff
                                } else {
                                    Icons.Rounded.Visibility
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = contextWindow,
                    onValueChange = { contextWindow = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.models_context_window)) },
                    placeholder = { Text(stringResource(R.string.models_context_window_hint)) },
                    isError = showError && !contextValid,
                    supportingText = {
                        if (showError && !contextValid) {
                            Text(stringResource(R.string.models_context_invalid))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.models_supports_images),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = supportsImages,
                        onCheckedChange = { supportsImages = it },
                    )
                }
                if (showError) {
                    Text(
                        text = if (name.isBlank()) {
                            stringResource(R.string.models_name_required)
                        } else if (modelId.isBlank()) {
                            stringResource(R.string.models_model_required)
                        } else {
                            stringResource(R.string.models_custom_invalid_url)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!valid) {
                        showError = true
                        return@TextButton
                    }
                    onSave(
                        CustomProviderDraft(
                            name = name.trim(),
                            baseUrl = baseUrl.trim(),
                            modelId = modelId.trim(),
                            apiKey = apiKey.trim(),
                            supportsImages = supportsImages,
                            contextWindow = contextWindow.toIntOrNull(),
                        ),
                    )
                },
            ) {
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
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)
