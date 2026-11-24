package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.config.ConfigGenerator
import com.example.data.model.SetupCatalogDefaults
import com.example.ui.components.ConsoleSectionCard
import com.example.ui.components.CopyableCodeBlock
import com.example.ui.components.ExternalPortalButton
import com.example.ui.components.NumberedInstructionRow
import com.example.ui.components.SecretInputField
import com.example.ui.components.StatusPill
import com.example.ui.components.VerificationBanner
import com.example.ui.components.copyToClipboard
import com.example.ui.components.openExternalUrl
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberDebrid
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseAlert
import com.example.ui.viewmodel.SetupUiState
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AioStreamsScreen(
    state: SetupUiState,
    onGoToAccounts: () -> Unit,
    onSaveAioStreams: (
        instanceUrl: String,
        password: String,
        enabledAddons: List<String>,
        customStreamSources: List<String>,
        preferredResolutions: List<String>,
        cachedOnly: Boolean,
        excludeCam: Boolean,
        preferDolbyVision: Boolean,
        maxFileSizeGb: Long,
        formatterPreset: String,
        manifestUrl: String,
    ) -> Unit,
    onVerifyManifest: (String) -> Unit,
    onOpenMarketplace: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val profile = state.activeProfile ?: return

    var instanceUrl by remember(profile.id) { mutableStateOf(profile.aioStreamsInstanceUrl) }
    var configPassword by remember(profile.id) { mutableStateOf(profile.aioStreamsPassword) }
    var enabledAddons by remember(profile.id, profile.enabledAddons) { mutableStateOf(profile.enabledAddons) }
    var customSources by remember(profile.id, profile.customStreamSources) { mutableStateOf(profile.customStreamSources) }
    var newCustomSource by remember { mutableStateOf("") }
    var preferredResolutions by remember(profile.id) { mutableStateOf(profile.preferredResolutions) }
    var cachedOnly by remember(profile.id) { mutableStateOf(profile.cachedOnly) }
    var excludeCam by remember(profile.id) { mutableStateOf(profile.excludeCam) }
    var preferDv by remember(profile.id) { mutableStateOf(profile.preferDolbyVision) }
    var maxSizeGb by remember(profile.id) { mutableFloatStateOf(profile.maxFileSizeGb.toFloat()) }
    var formatterPreset by remember(profile.id) { mutableStateOf(profile.formatterPreset) }
    var manifestUrl by remember(profile.id) { mutableStateOf(profile.aioStreamsManifestUrl) }

    val hasRdKey = profile.realDebridApiKey.isNotBlank()
    val previewProfile = remember(
        profile, instanceUrl, configPassword, enabledAddons, customSources,
        preferredResolutions, cachedOnly, excludeCam, preferDv, maxSizeGb, formatterPreset, manifestUrl
    ) {
        profile.copy(
            aioStreamsInstanceUrl = instanceUrl,
            aioStreamsPassword = configPassword,
            enabledAddons = enabledAddons,
            customStreamSources = customSources,
            preferredResolutions = preferredResolutions,
            cachedOnly = cachedOnly,
            excludeCam = excludeCam,
            preferDolbyVision = preferDv,
            maxFileSizeGb = maxSizeGb.roundToLong(),
            formatterPreset = formatterPreset,
            aioStreamsManifestUrl = manifestUrl,
        )
    }
    val generatedJson = remember(previewProfile) {
        ConfigGenerator.generateAioStreamsConfigJson(previewProfile)
    }
    val selectedFormatterSpec = remember(formatterPreset) {
        SetupCatalogDefaults.formatterPresets.firstOrNull { it.id == formatterPreset }
            ?: SetupCatalogDefaults.formatterPresets.first()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Master Single-Config Propagation Status Banner
        item {
            Surface(
                color = if (hasRdKey) EmeraldContainer else AmberContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (hasRdKey) EmeraldVerified.copy(alpha = 0.5f) else AmberDebrid.copy(alpha = 0.5f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp),
                ) {
                    Icon(
                        imageVector = if (hasRdKey) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = if (hasRdKey) EmeraldVerified else AmberDebrid,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasRdKey) {
                                "Master Real-Debrid Config Bound Once"
                            } else {
                                "Real-Debrid Key Not Set Yet"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (hasRdKey) EmeraldVerified else AmberDebrid,
                        )
                        Text(
                            text = if (hasRdKey) {
                                "All ${enabledAddons.size + customSources.size} stream add-ons below automatically inherit your Real-Debrid key, resolution filters, and cached-only rules."
                            } else {
                                "Add your Real-Debrid API token in Accounts first so every stream add-on inherits it automatically."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (!hasRdKey) {
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onGoToAccounts,
                            border = BorderStroke(1.dp, AmberDebrid),
                            modifier = Modifier.testTag("go_set_rd_key_button"),
                        ) {
                            Text("Set Key", color = AmberDebrid)
                        }
                    }
                }
            }
        }

        // Card 1: AIOStreams Instance & Encryption Password
        item {
            ConsoleSectionCard(
                title = "AIOStreams Instance & Password",
                subtitle = "Single hub managing every stream scraper in Nuvio",
                icon = Icons.Filled.Hub,
                accentColor = CyanSignal,
                badgeText = "STEP 3",
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilterChip(
                        selected = instanceUrl.contains("elfhosted", ignoreCase = true),
                        onClick = { instanceUrl = "https://aiostreams.elfhosted.com" },
                        label = { Text("ElfHosted (Official)") },
                    )
                    FilterChip(
                        selected = !instanceUrl.contains("elfhosted", ignoreCase = true),
                        onClick = { if (instanceUrl.contains("elfhosted", ignoreCase = true)) instanceUrl = "https://" },
                        label = { Text("Custom Host") },
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = instanceUrl,
                    onValueChange = { instanceUrl = it },
                    label = { Text("AIOStreams Base URL") },
                    placeholder = { Text("https://aiostreams.elfhosted.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aiostreams_instance_input"),
                )
                Spacer(Modifier.height(10.dp))
                SecretInputField(
                    value = configPassword,
                    onValueChange = { configPassword = it },
                    label = "AIOStreams Config Encryption Password",
                    placeholder = "Required when saving your AIOStreams session",
                    testTagName = "aiostreams_password_input",
                    supportingText = "AIOStreams encrypts your settings with this password and has no reset option — saved safely in your profile.",
                )
            }
        }

        // Card 2: Unified Add-ons & Custom Stream Sources (All Auto-Configured Once)
        item {
            ConsoleSectionCard(
                title = "Unified Stream Add-ons & Sources",
                subtitle = "Every enabled or newly added source auto-inherits your master API keys & filters",
                icon = Icons.Filled.Add,
                accentColor = CyanSignal,
                badgeText = "${enabledAddons.size + customSources.size} ACTIVE",
            ) {
                OutlinedButton(
                    onClick = onOpenMarketplace,
                    border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("aiostreams_open_marketplace_button"),
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = CyanSignal, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Browse Compatible Nuvio / Stremio Marketplace", color = CyanSignal)
                }
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SetupCatalogDefaults.builtInStreamAddons.forEach { addon ->
                        val isSelected = addon.id in enabledAddons
                        Surface(
                            color = if (isSelected) ObsidianSurfaceContainer else ObsidianVoid.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyanSignal.copy(alpha = 0.55f) else ObsidianOutline,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = addon.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (addon.recommended) {
                                            Spacer(Modifier.width(8.dp))
                                            StatusPill("REC", CyanSignal)
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = addon.tagline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (isSelected) {
                                        Spacer(Modifier.height(6.dp))
                                        StatusPill(
                                            text = if (hasRdKey) "AUTO-CONFIGURED • INHERITS MASTER RD" else "PENDING MASTER RD KEY",
                                            color = if (hasRdKey) EmeraldVerified else AmberDebrid,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        enabledAddons = if (checked) {
                                            (enabledAddons + addon.id).distinct()
                                        } else {
                                            enabledAddons - addon.id
                                        }
                                    },
                                    modifier = Modifier.testTag("addon_toggle_${addon.id}"),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Add New Stream Source via AIOStreams",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Paste any Stremio/Nuvio stream addon URL or scraper name. AIOStreams will wrap it with your single Real-Debrid key & filters automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = newCustomSource,
                        onValueChange = { newCustomSource = it },
                        label = { Text("Custom Addon Name or Manifest URL") },
                        placeholder = { Text("https://.../manifest.json") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_stream_source_input"),
                    )
                    Button(
                        onClick = {
                            val trimmed = newCustomSource.trim()
                            if (trimmed.isNotBlank() && customSources.size < 20) {
                                customSources = (customSources + trimmed).distinct()
                                newCustomSource = ""
                                onNotify("Added stream source — auto-configured with master Real-Debrid & filters!")
                            }
                        },
                        enabled = newCustomSource.isNotBlank(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_custom_stream_source_button"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add stream source")
                    }
                }

                if (customSources.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        customSources.forEach { src ->
                            Surface(
                                color = ObsidianSurfaceContainer,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = src,
                                            style = MonoCodeStyle,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "Auto-configured with Master RD + ${preferredResolutions.joinToString("/")}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldVerified,
                                        )
                                    }
                                    IconButton(
                                        onClick = { customSources = customSources - src },
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Remove custom source",
                                            tint = RoseAlert,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 3: Global Quality, Cache & Formatter Studio
        item {
            ConsoleSectionCard(
                title = "Global Filters & Stream Formatter",
                subtitle = "Applied once across all AIOStreams sources",
                icon = Icons.Filled.Tune,
                accentColor = CyanSignal,
            ) {
                Text(
                    text = "Preferred Resolutions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SetupCatalogDefaults.resolutionOptions.forEach { res ->
                        val active = res in preferredResolutions
                        FilterChip(
                            selected = active,
                            onClick = {
                                preferredResolutions = if (active) {
                                    (preferredResolutions - res).ifEmpty { listOf(res) }
                                } else {
                                    (preferredResolutions + res).distinct()
                                }
                            },
                            label = { Text(res) },
                            modifier = Modifier.testTag("resolution_chip_$res"),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                ToggleOptionRow("Cached Real-Debrid Only (Instant Playback)", cachedOnly) { cachedOnly = it }
                ToggleOptionRow("Exclude CAM / Telesync / Screener", excludeCam) { excludeCam = it }
                ToggleOptionRow("Prioritize Dolby Vision & HDR10+", preferDv) { preferDv = it }

                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Maximum File Size Limit", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${maxSizeGb.roundToLong()} GB",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyanSignal,
                    )
                }
                Slider(
                    value = maxSizeGb,
                    onValueChange = { maxSizeGb = it },
                    valueRange = 5f..100f,
                    steps = 18,
                    modifier = Modifier.testTag("max_file_size_slider"),
                )

                Spacer(Modifier.height(10.dp))
                Text("Nuvio Display Formatter", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SetupCatalogDefaults.formatterPresets.forEach { preset ->
                        FilterChip(
                            selected = formatterPreset == preset.id,
                            onClick = { formatterPreset = preset.id },
                            label = { Text(preset.label) },
                            modifier = Modifier.testTag("formatter_chip_${preset.id}"),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                // Live Nuvio Stream Row Preview
                Surface(
                    color = ObsidianVoid,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "LIVE NUVIO STREAM PREVIEW (${selectedFormatterSpec.label})",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanSignal,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = selectedFormatterSpec.previewTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = selectedFormatterSpec.previewSubtitle,
                            style = MonoCodeStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        onSaveAioStreams(
                            instanceUrl,
                            configPassword,
                            enabledAddons,
                            customSources,
                            preferredResolutions,
                            cachedOnly,
                            excludeCam,
                            preferDv,
                            maxSizeGb.roundToLong(),
                            formatterPreset,
                            manifestUrl,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("save_aiostreams_button"),
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Unified AIOStreams Config")
                }
            }
        }

        // Card 4: Export JSON, Open Configurator & Verify Manifest URL
        item {
            ConsoleSectionCard(
                title = "Generate & Verify AIOStreams Manifest",
                subtitle = "Export your unified config, open AIOStreams, and test the live manifest link",
                icon = Icons.Filled.Link,
                accentColor = EmeraldVerified,
                badgeText = if (profile.aioStreamsVerified) "VERIFIED" else "FINALIZE",
            ) {
                NumberedInstructionRow(
                    stepNumber = 1,
                    text = "Copy your unified AIOStreams JSON config below or open the AIOStreams web configurator.",
                    accentColor = CyanSignal,
                )
                NumberedInstructionRow(
                    stepNumber = 2,
                    text = "Enter your Real-Debrid key once in Services, verify your enabled add-ons, set your password, and click Save & Install.",
                    accentColor = CyanSignal,
                )
                NumberedInstructionRow(
                    stepNumber = 3,
                    text = "Copy the generated https://.../manifest.json link, paste it below, and tap Verify Live.",
                    accentColor = CyanSignal,
                )

                Spacer(Modifier.height(12.dp))
                CopyableCodeBlock(
                    title = "UNIFIED AIOSTREAMS CONFIG JSON",
                    code = generatedJson,
                    onCopy = { copyToClipboard(context, "AIOStreams Config JSON", generatedJson, onNotify) },
                    testTagName = "copy_aiostreams_json_button",
                )

                Spacer(Modifier.height(10.dp))
                ExternalPortalButton(
                    label = "Open AIOStreams Configurator",
                    onClick = {
                        val target = instanceUrl.trim().ifBlank { "https://aiostreams.elfhosted.com" }
                        openExternalUrl(context, target, onNotify)
                    },
                    testTagName = "open_aiostreams_web_button",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = manifestUrl,
                    onValueChange = { manifestUrl = it },
                    label = { Text("AIOStreams Manifest URL") },
                    placeholder = { Text("https://aiostreams.elfhosted.com/.../manifest.json") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aiostreams_manifest_input"),
                )

                Spacer(Modifier.height(10.dp))
                VerificationBanner(
                    state = state.aioStreamsManifestVerification,
                    persistedVerifiedLabel = if (profile.aioStreamsVerified) "AIOStreams manifest verified live over HTTPS" else null,
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = { onVerifyManifest(manifestUrl) },
                        border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("verify_aiostreams_manifest_button"),
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = EmeraldVerified, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Verify Live", color = EmeraldVerified)
                    }
                    Button(
                        onClick = {
                            onSaveAioStreams(
                                instanceUrl,
                                configPassword,
                                enabledAddons,
                                customSources,
                                preferredResolutions,
                                cachedOnly,
                                excludeCam,
                                preferDv,
                                maxSizeGb.roundToLong(),
                                formatterPreset,
                                manifestUrl,
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("save_aiostreams_manifest_button"),
                    ) {
                        Text("Save Manifest")
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
