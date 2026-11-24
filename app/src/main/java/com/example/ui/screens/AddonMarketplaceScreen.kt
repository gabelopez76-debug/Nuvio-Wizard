package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AddonIntegrationMode
import com.example.data.model.MarketplaceAddonSpec
import com.example.data.model.MarketplaceCatalog
import com.example.data.model.MarketplaceCategory
import com.example.data.model.VerificationState
import com.example.ui.components.ExternalPortalButton
import com.example.ui.components.StatusPill
import com.example.ui.components.VerificationBanner
import com.example.ui.components.copyToClipboard
import com.example.ui.components.openExternalUrl
import com.example.ui.theme.AmberDebrid
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IndigoMeta
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseAlert
import com.example.ui.viewmodel.SetupUiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddonMarketplaceScreen(
    state: SetupUiState,
    manifestVerification: VerificationState,
    onToggleStreamAddon: (MarketplaceAddonSpec) -> Unit,
    onInstallCatalogAddon: (MarketplaceAddonSpec) -> Unit,
    onVerifyManifest: (String) -> Unit,
    onClearVerification: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val profile = state.activeProfile
    val activeCatalogs = state.activeCatalogs

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MarketplaceCategory.ALL) }
    var selectedIntegrationFilter by remember { mutableStateOf<String>("ALL") }
    var inspectedAddon by remember { mutableStateOf<MarketplaceAddonSpec?>(null) }

    fun isInstalled(addon: MarketplaceAddonSpec): Boolean {
        if (profile == null) return false
        return when (addon.integrationMode) {
            AddonIntegrationMode.AIOSTREAMS_BUILTIN -> addon.builtinAddonId in profile.enabledAddons
            AddonIntegrationMode.AIOSTREAMS_CUSTOM -> addon.manifestUrl in profile.customStreamSources
            AddonIntegrationMode.AIOMETADATA_ROW -> {
                val tpl = addon.catalogTemplate ?: return false
                activeCatalogs.any {
                    it.sourceReference.equals(tpl.sourceReference, ignoreCase = true) ||
                        it.title.equals(tpl.title, ignoreCase = true)
                }
            }
            AddonIntegrationMode.DIRECT_NUVIO -> false
        }
    }

    val filteredAddons = remember(searchQuery, selectedCategory, selectedIntegrationFilter) {
        val q = searchQuery.trim().lowercase()
        MarketplaceCatalog.allAddons.filter { addon ->
            val matchesCategory = selectedCategory == MarketplaceCategory.ALL || addon.category == selectedCategory
            val matchesMode = when (selectedIntegrationFilter) {
                "AIOSTREAMS" -> addon.integrationMode == AddonIntegrationMode.AIOSTREAMS_BUILTIN ||
                    addon.integrationMode == AddonIntegrationMode.AIOSTREAMS_CUSTOM
                "AIOMETADATA" -> addon.integrationMode == AddonIntegrationMode.AIOMETADATA_ROW
                "DIRECT" -> addon.integrationMode == AddonIntegrationMode.DIRECT_NUVIO
                else -> true
            }
            val matchesQuery = q.isBlank() ||
                addon.name.lowercase().contains(q) ||
                addon.shortDescription.lowercase().contains(q) ||
                addon.developer.lowercase().contains(q) ||
                addon.tags.any { it.lowercase().contains(q) }
            matchesCategory && matchesMode && matchesQuery
        }
    }

    val installedCount = remember(profile, activeCatalogs) {
        MarketplaceCatalog.allAddons.count { isInstalled(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("addon_marketplace_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Marketplace Hero Header
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = ObsidianSurface,
                border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    CyanSignal.copy(alpha = 0.14f),
                                    IndigoMeta.copy(alpha = 0.12f),
                                    ObsidianSurface,
                                )
                            )
                        )
                        .padding(18.dp),
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            StatusPill("NUVIO & STREMIO V3 COMPATIBLE", CyanSignal)
                            StatusPill("$installedCount INSTALLED", EmeraldVerified)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Add-on Marketplace",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Every stream scraper added here routes straight into AIOStreams so your Real-Debrid key & filters propagate once automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 2. Search Bar + Category & Integration Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Torrentio, Cyberflix, OpenSubtitles, Anime...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("marketplace_search_input"),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    MarketplaceCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) },
                            modifier = Modifier.testTag("marketplace_cat_${cat.name.lowercase()}"),
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val modeFilters = listOf(
                        "ALL" to "All Modes",
                        "AIOSTREAMS" to "AIOStreams Native",
                        "AIOMETADATA" to "AIOMetadata Board",
                        "DIRECT" to "Direct Nuvio",
                    )
                    modeFilters.forEach { (id, label) ->
                        FilterChip(
                            selected = selectedIntegrationFilter == id,
                            onClick = { selectedIntegrationFilter = id },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        // 3. Results Count
        item {
            Text(
                text = "Showing ${filteredAddons.size} compatible add-on(s)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (filteredAddons.isEmpty()) {
            item {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("No matching add-ons found", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Try clearing your search or selecting 'All' categories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(filteredAddons, key = { it.id }) { addon ->
                val installed = isInstalled(addon)
                MarketplaceAddonCard(
                    addon = addon,
                    installed = installed,
                    isSaving = state.isSaving,
                    onInspect = {
                        onClearVerification()
                        inspectedAddon = addon
                    },
                    onQuickAction = {
                        when (addon.integrationMode) {
                            AddonIntegrationMode.AIOSTREAMS_BUILTIN,
                            AddonIntegrationMode.AIOSTREAMS_CUSTOM -> onToggleStreamAddon(addon)
                            AddonIntegrationMode.AIOMETADATA_ROW -> onInstallCatalogAddon(addon)
                            AddonIntegrationMode.DIRECT_NUVIO -> {
                                copyToClipboard(context, "${addon.name} Manifest URL", addon.manifestUrl, onNotify)
                            }
                        }
                    },
                )
            }
        }
    }

    // Add-on Compatibility & Live Verification Detail Bottom Sheet
    inspectedAddon?.let { addon ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val installed = isInstalled(addon)
        ModalBottomSheet(
            onDismissRequest = {
                onClearVerification()
                inspectedAddon = null
            },
            sheetState = sheetState,
            containerColor = ObsidianSurface,
        ) {
            MarketplaceAddonDetailSheet(
                addon = addon,
                installed = installed,
                manifestVerification = manifestVerification,
                onPrimaryAction = {
                    when (addon.integrationMode) {
                        AddonIntegrationMode.AIOSTREAMS_BUILTIN,
                        AddonIntegrationMode.AIOSTREAMS_CUSTOM -> onToggleStreamAddon(addon)
                        AddonIntegrationMode.AIOMETADATA_ROW -> onInstallCatalogAddon(addon)
                        AddonIntegrationMode.DIRECT_NUVIO -> {
                            copyToClipboard(context, "${addon.name} Manifest URL", addon.manifestUrl, onNotify)
                        }
                    }
                },
                onVerifyManifest = { onVerifyManifest(addon.manifestUrl) },
                onCopyManifest = {
                    copyToClipboard(context, "${addon.name} Manifest URL", addon.manifestUrl, onNotify)
                },
                onOpenPortal = {
                    openExternalUrl(context, addon.configPortalUrl, onNotify)
                },
            )
        }
    }
}

private fun categoryAccent(category: MarketplaceCategory): Color = when (category) {
    MarketplaceCategory.STREAMS -> CyanSignal
    MarketplaceCategory.DEBRID_CLOUD -> AmberDebrid
    MarketplaceCategory.CATALOGS -> IndigoMeta
    MarketplaceCategory.METADATA -> IndigoMeta
    MarketplaceCategory.SUBTITLES -> EmeraldVerified
    MarketplaceCategory.ALL -> CyanSignal
}

private fun categoryIcon(category: MarketplaceCategory): ImageVector = when (category) {
    MarketplaceCategory.STREAMS -> Icons.Filled.Hub
    MarketplaceCategory.DEBRID_CLOUD -> Icons.Filled.CloudQueue
    MarketplaceCategory.CATALOGS, MarketplaceCategory.METADATA -> Icons.Filled.AutoAwesomeMosaic
    MarketplaceCategory.SUBTITLES -> Icons.Filled.Subtitles
    MarketplaceCategory.ALL -> Icons.Filled.Storefront
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarketplaceAddonCard(
    addon: MarketplaceAddonSpec,
    installed: Boolean,
    isSaving: Boolean,
    onInspect: () -> Unit,
    onQuickAction: () -> Unit,
) {
    val accent = categoryAccent(addon.category)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(
            width = if (installed) 1.5.dp else 1.dp,
            color = if (installed) EmeraldVerified.copy(alpha = 0.55f) else ObsidianOutline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onInspect)
            .testTag("marketplace_card_${addon.id}"),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background((if (installed) EmeraldVerified else accent).copy(alpha = 0.15f)),
                ) {
                    Icon(
                        imageVector = categoryIcon(addon.category),
                        contentDescription = null,
                        tint = if (installed) EmeraldVerified else accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = addon.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (installed) {
                            StatusPill("INSTALLED", EmeraldVerified)
                        } else if (addon.featured) {
                            StatusPill("POPULAR", accent)
                        }
                    }
                    Text(
                        text = "by ${addon.developer} • ${addon.integrationMode.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = addon.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (addon.inheritsRealDebrid) {
                    StatusPill("Auto RD Sync", AmberDebrid)
                }
                addon.tags.forEach { tag ->
                    StatusPill(tag, accent)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = onInspect,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("marketplace_details_${addon.id}"),
                ) {
                    Text("Details")
                }

                val actionLabel = when (addon.integrationMode) {
                    AddonIntegrationMode.AIOSTREAMS_BUILTIN,
                    AddonIntegrationMode.AIOSTREAMS_CUSTOM -> if (installed) "Remove from Hub" else "Add to AIOStreams"
                    AddonIntegrationMode.AIOMETADATA_ROW -> if (installed) "On Meta Board" else "Add to AIOMetadata"
                    AddonIntegrationMode.DIRECT_NUVIO -> "Copy Manifest URL"
                }

                Button(
                    onClick = onQuickAction,
                    enabled = !isSaving && !(installed && addon.integrationMode == AddonIntegrationMode.AIOMETADATA_ROW),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            installed && addon.integrationMode == AddonIntegrationMode.AIOMETADATA_ROW -> EmeraldVerified
                            installed -> RoseAlert.copy(alpha = 0.85f)
                            else -> accent
                        },
                        contentColor = ObsidianVoid,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("marketplace_install_${addon.id}"),
                ) {
                    Icon(
                        imageVector = when {
                            installed && addon.integrationMode == AddonIntegrationMode.AIOMETADATA_ROW -> Icons.Filled.Check
                            addon.integrationMode == AddonIntegrationMode.DIRECT_NUVIO -> Icons.Filled.ContentCopy
                            installed -> Icons.Filled.Close
                            else -> Icons.Filled.Add
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(actionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarketplaceAddonDetailSheet(
    addon: MarketplaceAddonSpec,
    installed: Boolean,
    manifestVerification: VerificationState,
    onPrimaryAction: () -> Unit,
    onVerifyManifest: () -> Unit,
    onCopyManifest: () -> Unit,
    onOpenPortal: () -> Unit,
) {
    val accent = categoryAccent(addon.category)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
            ) {
                Icon(categoryIcon(addon.category), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(addon.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Developer: ${addon.developer} • ${addon.category.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Compatibility Matrix Badges
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusPill("Nuvio Compatible", EmeraldVerified)
            StatusPill("Stremio Protocol v3", CyanSignal)
            if (addon.inheritsRealDebrid) {
                StatusPill("AIOStreams RD Auto-Sync", AmberDebrid)
            }
            StatusPill(addon.integrationMode.label, IndigoMeta)
        }

        Text(
            text = addon.detailedNotes,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Official Manifest Preview Box
        Surface(
            color = ObsidianSurfaceContainer,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ObsidianOutline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ENDPOINT / MANIFEST URL", style = MaterialTheme.typography.labelSmall, color = accent)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = addon.manifestUrl,
                        style = MonoCodeStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onCopyManifest,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy manifest URL", tint = accent)
                }
            }
        }

        if (manifestVerification !is VerificationState.Idle) {
            VerificationBanner(state = manifestVerification)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (addon.manifestUrl.endsWith("manifest.json")) {
                OutlinedButton(
                    onClick = onVerifyManifest,
                    enabled = manifestVerification !is VerificationState.Checking,
                    border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("marketplace_verify_manifest_button"),
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = EmeraldVerified, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ping Live", color = EmeraldVerified, maxLines = 1)
                }
            }
            ExternalPortalButton(
                label = "Open Portal",
                onClick = onOpenPortal,
                testTagName = "marketplace_open_portal_button",
                modifier = Modifier.weight(1f),
            )
        }

        Button(
            onClick = onPrimaryAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (installed) EmeraldVerified else accent,
                contentColor = ObsidianVoid,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("marketplace_sheet_primary_action"),
        ) {
            Text(
                text = when (addon.integrationMode) {
                    AddonIntegrationMode.AIOSTREAMS_BUILTIN,
                    AddonIntegrationMode.AIOSTREAMS_CUSTOM ->
                        if (installed) "Remove ${addon.name} from AIOStreams" else "Add ${addon.name} to AIOStreams"
                    AddonIntegrationMode.AIOMETADATA_ROW ->
                        if (installed) "Already on AIOMetadata Board" else "Add ${addon.name} Row to AIOMetadata"
                    AddonIntegrationMode.DIRECT_NUVIO -> "Copy Direct Nuvio Manifest URL"
                },
            )
        }
    }
}
