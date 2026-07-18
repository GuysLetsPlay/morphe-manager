/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 */

package app.morphe.manager.ui.screen.settings.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.morphe.manager.R
import app.morphe.manager.ui.screen.shared.*
import app.morphe.manager.ui.viewmodel.ImportExportViewModel
import app.morphe.manager.ui.viewmodel.SettingsViewModel
import app.morphe.manager.util.isAndroidTv
import app.morphe.manager.util.rememberFolderPickerWithPermission
import app.morphe.manager.util.toFilePath
import app.morphe.manager.data.platform.Filesystem
import org.koin.compose.koinInject

/**
 * Storage management section.
 */
@Composable
fun FilesAndStorageSection(
    settingsViewModel: SettingsViewModel,
    importExportViewModel: ImportExportViewModel,
    onFilePickerPositioned: ((Rect) -> Unit)? = null
) {
    val context = LocalContext.current
    val isTV = remember { context.isAndroidTv() }
    val useExpertMode by settingsViewModel.prefs.useExpertMode.getAsState()
    val useCustomFilePicker by settingsViewModel.prefs.useCustomFilePicker.getAsState()
    val autoApkFolderDiscovery by settingsViewModel.prefs.autoApkFolderDiscovery.getAsState()
    val autoApkFolderPath by settingsViewModel.prefs.autoApkFolderPath.getAsState()
    val autoApkFolderIncludeSubdirectories by settingsViewModel.prefs.autoApkFolderIncludeSubdirectories.getAsState()
    val enabledState = stringResource(R.string.enabled)
    val disabledState = stringResource(R.string.disabled)
    val filesystem: Filesystem = koinInject()
    val (storagePermissionContract, storagePermissionName) = remember { filesystem.permissionContract() }
    val storagePermissionLauncher = rememberLauncherForActivityResult(storagePermissionContract) { granted ->
        if (granted) settingsViewModel.setAutoApkFolderDiscovery(true)
    }
    val chooseApkFolder = rememberFolderPickerWithPermission { uri ->
        settingsViewModel.setAutoApkFolderPath(uri.toFilePath())
    }

    // Storage counts
    val originalApkCount by settingsViewModel.originalApkCount.collectAsStateWithLifecycle()
    val patchedApkCount by settingsViewModel.patchedApkCount.collectAsStateWithLifecycle()
    val patchedPackagesCount by settingsViewModel.patchedPackagesCount.collectAsStateWithLifecycle()

    val showApkManagementDialog = remember { mutableStateOf<ApkManagementType?>(null) }
    val showPatchSelectionDialog = remember { mutableStateOf(false) }

    // APK management dialog
    showApkManagementDialog.value?.let { type ->
        ApkManagementDialog(
            type = type,
            onDismissRequest = { showApkManagementDialog.value = null }
        )
    }

    // Patch selection management dialog
    if (showPatchSelectionDialog.value) {
        PatchSelectionManagementDialog(
            settingsViewModel = settingsViewModel,
            importExportViewModel = importExportViewModel,
            onDismiss = { showPatchSelectionDialog.value = false }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPadding)) {
        SectionTitle(
            text = stringResource(R.string.settings_system_files),
            icon = Icons.Outlined.Storage
        )

        SectionCard {
            Column {
                // Original APKs management
                RichSettingsItem(
                    onClick = { showApkManagementDialog.value = ApkManagementType.ORIGINAL },
                    title = stringResource(R.string.settings_system_original_apks_title),
                    subtitle = stringResource(R.string.settings_system_original_apks_description),
                    leadingContent = {
                        MorpheIcon(icon = Icons.Outlined.Storage)
                    },
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (originalApkCount > 0) {
                                InfoBadge(
                                    text = originalApkCount.toString(),
                                    style = InfoBadgeStyle.Default,
                                    isCompact = true
                                )
                            }
                            MorpheIcon(icon = Icons.Outlined.ChevronRight)
                        }
                    }
                )

                MorpheSettingsDivider()

                // Patched APKs management
                RichSettingsItem(
                    onClick = { showApkManagementDialog.value = ApkManagementType.PATCHED },
                    title = stringResource(R.string.settings_system_patched_apks_title),
                    subtitle = stringResource(R.string.settings_system_patched_apks_description),
                    leadingContent = {
                        MorpheIcon(icon = Icons.Outlined.Apps)
                    },
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (patchedApkCount > 0) {
                                InfoBadge(
                                    text = patchedApkCount.toString(),
                                    style = InfoBadgeStyle.Default,
                                    isCompact = true
                                )
                            }
                            MorpheIcon(icon = Icons.Outlined.ChevronRight)
                        }
                    }
                )

                // Patch Selections management (Expert mode only)
                if (useExpertMode) {
                    MorpheSettingsDivider()

                    RichSettingsItem(
                        onClick = { showPatchSelectionDialog.value = true },
                        title = stringResource(R.string.settings_system_patch_selections_title),
                        subtitle = stringResource(R.string.settings_system_patch_selections_description),
                        leadingContent = {
                            MorpheIcon(icon = Icons.Outlined.Tune)
                        },
                        trailingContent = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (patchedPackagesCount > 0) {
                                    InfoBadge(
                                        text = patchedPackagesCount.toString(),
                                        style = InfoBadgeStyle.Default,
                                        isCompact = true
                                    )
                                }
                                MorpheIcon(icon = Icons.Outlined.ChevronRight)
                            }
                        }
                    )
                }
            }
        }

        // TV always uses the custom picker regardless of this toggle, so hide it to avoid confusion
        if (!isTV) {
            SectionCard(
                modifier = if (onFilePickerPositioned != null)
                    Modifier.onGloballyPositioned { coords -> onFilePickerPositioned(coords.boundsInWindow()) }
                else Modifier
            ) {
                RichSettingsItem(
                    onClick = { settingsViewModel.setUseCustomFilePicker(!useCustomFilePicker) },
                    leadingContent = { MorpheIcon(icon = Icons.Outlined.FolderOpen) },
                    title = stringResource(R.string.settings_system_custom_file_picker),
                    subtitle = stringResource(R.string.settings_system_custom_file_picker_description),
                    trailingContent = {
                        MorpheSwitch(
                            checked = useCustomFilePicker,
                            onCheckedChange = null,
                            modifier = Modifier.semantics {
                                stateDescription = if (useCustomFilePicker) enabledState else disabledState
                            }
                        )
                    }
                )
            }
        }

        // The discovery scan is intentionally Expert-only because it exposes source APK choices.
        if (useExpertMode) {
            SectionCard {
                Column {
                    RichSettingsItem(
                        onClick = {
                            if (autoApkFolderDiscovery) {
                                settingsViewModel.setAutoApkFolderDiscovery(false)
                            } else if (filesystem.hasStoragePermission()) {
                                settingsViewModel.setAutoApkFolderDiscovery(true)
                            } else {
                                storagePermissionLauncher.launch(storagePermissionName)
                            }
                        },
                        leadingContent = { MorpheIcon(icon = Icons.Outlined.FindInPage) },
                        title = stringResource(R.string.settings_system_apk_folder_discovery),
                        subtitle = stringResource(R.string.settings_system_apk_folder_discovery_description),
                        trailingContent = {
                            MorpheSwitch(
                                checked = autoApkFolderDiscovery,
                                onCheckedChange = null,
                                modifier = Modifier.semantics {
                                    stateDescription = if (autoApkFolderDiscovery) enabledState else disabledState
                                }
                            )
                        }
                    )

                    if (autoApkFolderDiscovery) {
                        MorpheSettingsDivider()
                        RichSettingsItem(
                            onClick = chooseApkFolder,
                            leadingContent = { MorpheIcon(icon = Icons.Outlined.FolderOpen) },
                            title = stringResource(R.string.settings_system_apk_folder),
                            subtitle = autoApkFolderPath,
                            trailingContent = { MorpheIcon(icon = Icons.Outlined.ChevronRight) }
                        )
                        MorpheSettingsDivider()
                        RichSettingsItem(
                            onClick = {
                                settingsViewModel.setAutoApkFolderIncludeSubdirectories(
                                    !autoApkFolderIncludeSubdirectories
                                )
                            },
                            leadingContent = { MorpheIcon(icon = Icons.Outlined.AccountTree) },
                            title = stringResource(R.string.settings_system_apk_folder_subdirectories),
                            subtitle = stringResource(R.string.settings_system_apk_folder_subdirectories_description),
                            trailingContent = {
                                MorpheSwitch(
                                    checked = autoApkFolderIncludeSubdirectories,
                                    onCheckedChange = null,
                                    modifier = Modifier.semantics {
                                        stateDescription = if (autoApkFolderIncludeSubdirectories) enabledState else disabledState
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
