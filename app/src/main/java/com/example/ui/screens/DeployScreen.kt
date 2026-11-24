package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.config.ConfigGenerator
import com.example.ui.components.ConsoleSectionCard
import com.example.ui.components.NumberedInstructionRow
import com.example.ui.components.StatusPill
import com.example.ui.components.copyToClipboard
import com.example.ui.components.shareTextContent
import com.example.ui.theme.AmberDebrid
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IndigoMeta
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianVoid
import com.example.ui.viewmodel.SetupUiState

@Composable
fun DeployScreen(
    state: SetupUiState,
    onUpdateChecklist: (metaInstalled: Boolean, streamsInstalled: Boolean, orderVerified: Boolean) -> Unit,
    onOpenSetupSummary: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val profile = state.activeProfile ?: return
    var includeSecretsInExport by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Card 1: Addon Priority Order in Nuvio
        item {
            ConsoleSectionCard(
                title = "Nuvio Addon Priority Stack",
                subtitle = "Install in this exact top-to-bottom order inside Nuvio -> Settings -> Addons",
                icon = Icons.Filled.Layers,
                accentColor = EmeraldVerified,
                badgeText = "STEP 5",
            ) {
                PriorityStackItem(
                    rank = "#1 TOP",
                    name = "AIOMetadata",
                    detail = "Loads posters, ratings, catalogs & collections first before streams resolve.",
                    url = profile.aioMetadataManifestUrl,
                    accent = IndigoMeta,
                    onCopy = {
                        if (profile.aioMetadataManifestUrl.isNotBlank()) {
                            copyToClipboard(context, "AIOMetadata Manifest URL", profile.aioMetadataManifestUrl, onNotify)
                        } else {
                            onNotify("Save your AIOMetadata Manifest URL in the AIOMetadata tab first.")
                        }
                    },
                    testTagName = "copy_deploy_aiometa_button",
                )
                Spacer(Modifier.height(10.dp))
                PriorityStackItem(
                    rank = "#2 BELOW",
                    name = "AIOStreams",
                    detail = "Unified Real-Debrid stream hub feeding ${profile.enabledAddons.size + profile.customStreamSources.size} configured add-ons.",
                    url = profile.aioStreamsManifestUrl,
                    accent = CyanSignal,
                    onCopy = {
                        if (profile.aioStreamsManifestUrl.isNotBlank()) {
                            copyToClipboard(context, "AIOStreams Manifest URL", profile.aioStreamsManifestUrl, onNotify)
                        } else {
                            onNotify("Save your AIOStreams Manifest URL in the AIOStreams tab first.")
                        }
                    },
                    testTagName = "copy_deploy_aiostreams_button",
                )
            }
        }

        // Card 2: Interactive Nuvio Installation Checklist
        item {
            ConsoleSectionCard(
                title = "Final Nuvio Install Checklist",
                subtitle = "Walk through on your TV, tablet, or desktop Nuvio app",
                icon = Icons.Filled.RocketLaunch,
                accentColor = CyanSignal,
            ) {
                NumberedInstructionRow(1, "Open Nuvio and sign in with username '${profile.nuvioUsername.ifBlank { "(not set)" }}'.", CyanSignal)
                NumberedInstructionRow(2, "Go to Settings -> Addons and remove default metadata if you want AIOMetadata to lead cleanly.", CyanSignal)
                NumberedInstructionRow(3, "Tap Add Addon, paste your AIOMetadata manifest link (#1), and install.", IndigoMeta)
                NumberedInstructionRow(4, "Tap Add Addon again, paste your AIOStreams manifest link (#2), and install.", CyanSignal)
                NumberedInstructionRow(5, "Confirm AIOMetadata sits at the top of your addon list above AIOStreams.", EmeraldVerified)

                Spacer(Modifier.height(12.dp))
                ChecklistRow(
                    label = "Installed AIOMetadata manifest in Nuvio",
                    checked = profile.nuvioMetaInstalled,
                    testTagName = "deploy_check_meta",
                    onCheckedChange = {
                        onUpdateChecklist(it, profile.nuvioStreamsInstalled, profile.nuvioOrderVerified)
                    },
                )
                ChecklistRow(
                    label = "Installed AIOStreams manifest in Nuvio",
                    checked = profile.nuvioStreamsInstalled,
                    testTagName = "deploy_check_streams",
                    onCheckedChange = {
                        onUpdateChecklist(profile.nuvioMetaInstalled, it, profile.nuvioOrderVerified)
                    },
                )
                ChecklistRow(
                    label = "Verified AIOMetadata is ordered #1 above AIOStreams",
                    checked = profile.nuvioOrderVerified,
                    testTagName = "deploy_check_order",
                    onCheckedChange = {
                        onUpdateChecklist(profile.nuvioMetaInstalled, profile.nuvioStreamsInstalled, it)
                    },
                )

                Spacer(Modifier.height(12.dp))
                val allStepsDone = state.progress.completedSteps == state.progress.totalSteps
                Button(
                    onClick = onOpenSetupSummary,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allStepsDone) EmeraldVerified else CyanSignal,
                        contentColor = ObsidianVoid,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("deploy_open_summary_button"),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (allStepsDone) {
                            "View Final Setup Summary (100%)"
                        } else {
                            "View Setup Summary (${state.progress.granularPercent}%)"
                        },
                    )
                }
            }
        }

        // Card 3: Export & Share Setup Sheet
        item {
            ConsoleSectionCard(
                title = "Export & Share Setup Summary",
                subtitle = "Send your complete Nuvio configuration sheet to another device or notes",
                icon = Icons.Filled.Share,
                accentColor = AmberDebrid,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include Nuvio Password in Export", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Keep OFF when sharing with others; turn ON for private personal backups.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = includeSecretsInExport,
                        onCheckedChange = { includeSecretsInExport = it },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = {
                            val summary = ConfigGenerator.generateFullExportSummary(
                                profile = profile,
                                catalogs = state.activeCatalogs,
                                includeSecrets = includeSecretsInExport,
                            )
                            shareTextContent(context, "Nuvio Setup — ${profile.profileName}", summary)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberDebrid, contentColor = ObsidianVoid),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("share_setup_bundle_button"),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share Summary")
                    }
                    Button(
                        onClick = {
                            val summary = ConfigGenerator.generateFullExportSummary(
                                profile = profile,
                                catalogs = state.activeCatalogs,
                                includeSecrets = includeSecretsInExport,
                            )
                            copyToClipboard(context, "Full Nuvio Setup Summary", summary, onNotify)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("copy_setup_bundle_button"),
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy All")
                    }
                }
            }
        }

        // Card 4: Quick Troubleshooting Reference
        item {
            ConsoleSectionCard(
                title = "Quick Troubleshooting Tips",
                subtitle = "Need deeper help? Open the AI Support Chat tab anytime!",
                icon = Icons.Filled.Build,
                accentColor = IndigoMeta,
            ) {
                TroubleshootTip(
                    title = "No [RD+] cached streams appearing?",
                    fix = "Re-verify your Real-Debrid key in Accounts (ensure no leading/trailing spaces) and confirm your Real-Debrid Premium days are active.",
                )
                Spacer(Modifier.height(8.dp))
                TroubleshootTip(
                    title = "Nuvio home screen or streams loading slowly?",
                    fix = "Keep 2–3 core scrapers enabled inside AIOStreams (Torrentio + MediaFusion + Comet) and switch the formatter preset to Minimalistic.",
                )
                Spacer(Modifier.height(8.dp))
                TroubleshootTip(
                    title = "Posters missing ratings or custom rows not on top?",
                    fix = "Confirm AIOMetadata is positioned above AIOStreams in Nuvio Addons and that your RPDB key is saved in AIOMetadata.",
                )
            }
        }
    }
}

@Composable
private fun PriorityStackItem(
    rank: String,
    name: String,
    detail: String,
    url: String,
    accent: androidx.compose.ui.graphics.Color,
    onCopy: () -> Unit,
    testTagName: String,
) {
    Surface(
        color = ObsidianSurfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(rank, accent)
                    Spacer(Modifier.width(8.dp))
                    Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(4.dp))
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = url.ifBlank { "Manifest URL not saved yet" },
                    style = MonoCodeStyle,
                    color = if (url.isNotBlank()) EmeraldVerified else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(testTagName),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy $name manifest", tint = accent)
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    checked: Boolean,
    testTagName: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        color = ObsidianSurfaceContainer,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (checked) EmeraldVerified.copy(alpha = 0.4f) else ObsidianOutline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTagName),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Done",
                    tint = EmeraldVerified,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun TroubleshootTip(title: String, fix: String) {
    Surface(
        color = ObsidianVoid.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(3.dp))
            Text(fix, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
