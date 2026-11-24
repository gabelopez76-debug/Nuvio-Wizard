package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.config.ConfigGenerator
import com.example.data.model.CatalogCollectionItem
import com.example.data.model.CatalogPresetSpec
import com.example.data.model.SetupCatalogDefaults
import com.example.ui.components.ConsoleSectionCard
import com.example.ui.components.CopyableCodeBlock
import com.example.ui.components.ExternalPortalButton
import com.example.ui.components.NumberedInstructionRow
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AioMetadataScreen(
    state: SetupUiState,
    onSaveAioMetadata: (
        instanceUrl: String,
        movieProvider: String,
        seriesProvider: String,
        animeProvider: String,
        rpdbPostersEnabled: Boolean,
        animeCatalogsEnabled: Boolean,
        manifestUrl: String,
    ) -> Unit,
    onVerifyManifest: (String) -> Unit,
    onAddCatalog: (
        title: String,
        mediaType: String,
        itemKind: String,
        sourceProvider: String,
        sourceReference: String,
        description: String,
    ) -> Unit,
    onToggleCatalog: (catalogId: String, enabled: Boolean) -> Unit,
    onMoveCatalog: (index: Int, direction: Int) -> Unit,
    onDeleteCatalog: (catalogId: String) -> Unit,
    onApplyStarterPack: (CatalogPresetSpec) -> Unit,
    onOpenDesigner: (title: String?, subtitle: String?, badge: String?) -> Unit,
    onOpenMarketplace: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val profile = state.activeProfile ?: return
    val catalogs = state.activeCatalogs

    var instanceUrl by remember(profile.id) { mutableStateOf(profile.aioMetadataInstanceUrl) }
    var movieProvider by remember(profile.id) { mutableStateOf(profile.movieProvider) }
    var seriesProvider by remember(profile.id) { mutableStateOf(profile.seriesProvider) }
    var animeProvider by remember(profile.id) { mutableStateOf(profile.animeProvider) }
    var rpdbPosters by remember(profile.id) { mutableStateOf(profile.rpdbPostersEnabled) }
    var animeCatalogs by remember(profile.id) { mutableStateOf(profile.animeCatalogsEnabled) }
    var manifestUrl by remember(profile.id) { mutableStateOf(profile.aioMetadataManifestUrl) }

    var activeFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredCatalogs = remember(catalogs, activeFilter) {
        when (activeFilter) {
            "CATALOG" -> catalogs.filter { it.itemKind == "CATALOG" }
            "COLLECTION" -> catalogs.filter { it.itemKind == "COLLECTION" }
            else -> catalogs
        }
    }

    val previewProfile = remember(
        profile, instanceUrl, movieProvider, seriesProvider, animeProvider, rpdbPosters, animeCatalogs, manifestUrl
    ) {
        profile.copy(
            aioMetadataInstanceUrl = instanceUrl,
            movieProvider = movieProvider,
            seriesProvider = seriesProvider,
            animeProvider = animeProvider,
            rpdbPostersEnabled = rpdbPosters,
            animeCatalogsEnabled = animeCatalogs,
            aioMetadataManifestUrl = manifestUrl,
        )
    }
    val generatedMetaJson = remember(previewProfile, catalogs) {
        ConfigGenerator.generateAioMetadataConfigJson(previewProfile, catalogs)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero Banner for AIOMetadata
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_catalog_constellation),
                        contentDescription = stringResource(R.string.cd_catalog_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        ObsidianVoid.copy(alpha = 0.25f),
                                        ObsidianVoid.copy(alpha = 0.88f),
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        StatusPill("STEP 4 • AIOMETADATA & BOARD CURATION", IndigoMeta)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Catalogs & Collections Studio",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Design rich posters, TMDB/TVDB metadata, and ordered Nuvio discovery rows.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Studio Quick Actions: Collection Picture Designer + Marketplace
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { onOpenDesigner(null, null, null) },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoMeta, contentColor = ObsidianVoid),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("aiometa_open_designer_button"),
                ) {
                    Icon(Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cover Designer", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenMarketplace,
                    border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("aiometa_open_marketplace_button"),
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = CyanSignal, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Marketplace", color = CyanSignal, maxLines = 1)
                }
            }
        }

        // Step-by-Step AIOMetadata Setup Guide
        item {
            ConsoleSectionCard(
                title = "AIOMetadata Step-by-Step Guide",
                subtitle = "How catalogs & collections power your Nuvio home screen",
                icon = Icons.Filled.AutoAwesomeMosaic,
                accentColor = IndigoMeta,
            ) {
                NumberedInstructionRow(
                    stepNumber = 1,
                    text = "Pick your primary metadata sources below (TMDB for movies, TMDB/TVDB for series, Kitsu/MAL for anime).",
                    accentColor = IndigoMeta,
                )
                NumberedInstructionRow(
                    stepNumber = 2,
                    text = "Choose a Starter Pack or build custom Catalogs (dynamic feeds like Trending or Streaming Services) & Collections (curated MDBList/Trakt lists).",
                    accentColor = IndigoMeta,
                )
                NumberedInstructionRow(
                    stepNumber = 3,
                    text = "Reorder rows using the Up/Down arrows so your favorite rows appear first in Nuvio.",
                    accentColor = IndigoMeta,
                )
                NumberedInstructionRow(
                    stepNumber = 4,
                    text = "Open AIOMetadata, apply your keys & rows, generate your manifest link, and install it #1 (above AIOStreams) in Nuvio.",
                    accentColor = IndigoMeta,
                )
            }
        }

        // Card 1: Metadata & Artwork Providers
        item {
            ConsoleSectionCard(
                title = "Metadata & Artwork Providers",
                subtitle = "Uses TMDB, MDBList & RPDB keys from your Accounts Vault",
                icon = Icons.Filled.Tune,
                accentColor = IndigoMeta,
            ) {
                OutlinedTextField(
                    value = instanceUrl,
                    onValueChange = { instanceUrl = it },
                    label = { Text("AIOMetadata Instance URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aiometa_instance_input"),
                )

                Spacer(Modifier.height(12.dp))
                ProviderSelectorRow("Movies Provider", SetupCatalogDefaults.movieProviders, movieProvider) { movieProvider = it }
                Spacer(Modifier.height(8.dp))
                ProviderSelectorRow("Series Provider", SetupCatalogDefaults.seriesProviders, seriesProvider) { seriesProvider = it }
                Spacer(Modifier.height(8.dp))
                ProviderSelectorRow("Anime ID Mapping", SetupCatalogDefaults.animeProviders, animeProvider) { animeProvider = it }

                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("RPDB Rating Posters", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (profile.rpdbApiKey.isNotBlank()) "RPDB key linked from Accounts Vault" else "Optional: add RPDB key in Accounts for IMDb/RT badges",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = rpdbPosters, onCheckedChange = { rpdbPosters = it })
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Include Anime Catalogs", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = animeCatalogs, onCheckedChange = { animeCatalogs = it })
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        onSaveAioMetadata(
                            instanceUrl,
                            movieProvider,
                            seriesProvider,
                            animeProvider,
                            rpdbPosters,
                            animeCatalogs,
                            manifestUrl,
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoMeta, contentColor = ObsidianVoid),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("save_aiometa_providers_button"),
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Provider Settings")
                }
            }
        }

        // Card 2: Starter Pack One-Tap Presets
        item {
            ConsoleSectionCard(
                title = "One-Tap Starter Packs",
                subtitle = "Populate a complete Nuvio home board instantly, then customize",
                icon = Icons.Filled.CollectionsBookmark,
                accentColor = CyanSignal,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    SetupCatalogDefaults.starterPacks.forEach { pack ->
                        Surface(
                            color = ObsidianSurfaceContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ObsidianOutline),
                            modifier = Modifier.width(210.dp),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = pack.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = pack.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    minLines = 2,
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { onApplyStarterPack(pack) },
                                    border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("apply_pack_${pack.id}"),
                                ) {
                                    Text("Load ${pack.items.size} Rows", color = CyanSignal)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 3: Interactive Catalogs & Collections Builder
        item {
            ConsoleSectionCard(
                title = "Your Catalogs & Collections",
                subtitle = "Order here matches your Nuvio home screen top-to-bottom",
                icon = Icons.Filled.AutoAwesomeMosaic,
                accentColor = IndigoMeta,
                badgeText = "${catalogs.count { it.enabled }} ACTIVE",
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = activeFilter == "ALL",
                            onClick = { activeFilter = "ALL" },
                            label = { Text("All") },
                        )
                        FilterChip(
                            selected = activeFilter == "CATALOG",
                            onClick = { activeFilter = "CATALOG" },
                            label = { Text("Catalogs") },
                        )
                        FilterChip(
                            selected = activeFilter == "COLLECTION",
                            onClick = { activeFilter = "COLLECTION" },
                            label = { Text("Collections") },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoMeta, contentColor = ObsidianVoid),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("add_catalog_collection_button"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Custom Catalog or Collection")
                }

                Spacer(Modifier.height(12.dp))

                if (filteredCatalogs.isEmpty()) {
                    Surface(
                        color = ObsidianVoid.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp),
                        ) {
                            Text(
                                text = "No rows in this filter yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Tap 'Add Custom Catalog or Collection' or load a Starter Pack above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredCatalogs.forEach { item ->
                            val fullIndex = catalogs.indexOfFirst { it.id == item.id }
                            CatalogRowCard(
                                item = item,
                                positionLabel = "#${fullIndex + 1}",
                                canMoveUp = activeFilter == "ALL" && fullIndex > 0,
                                canMoveDown = activeFilter == "ALL" && fullIndex in 0 until catalogs.lastIndex,
                                onToggle = { onToggleCatalog(item.id, it) },
                                onMoveUp = { onMoveCatalog(fullIndex, -1) },
                                onMoveDown = { onMoveCatalog(fullIndex, 1) },
                                onDesignCover = {
                                    onOpenDesigner(
                                        item.title,
                                        item.description.ifBlank { "${item.mediaType} curated via ${item.sourceProvider}" },
                                        "${item.itemKind} • ${item.sourceProvider}",
                                    )
                                },
                                onDelete = { onDeleteCatalog(item.id) },
                            )
                        }
                    }
                }

                // Live Nuvio Home Board Order Preview
                val activeOrdered = catalogs.filter { it.enabled }.sortedBy { it.sortOrder }
                if (activeOrdered.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = ObsidianVoid,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, IndigoMeta.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "NUVIO HOME BOARD PREVIEW (${activeOrdered.size} ROWS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoMeta,
                            )
                            Spacer(Modifier.height(8.dp))
                            activeOrdered.forEachIndexed { idx, rowItem ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 3.dp),
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        style = MonoCodeStyle,
                                        color = IndigoMeta,
                                        modifier = Modifier.width(24.dp),
                                    )
                                    Text(
                                        text = rowItem.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${rowItem.mediaType} • ${rowItem.sourceProvider}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 4: Export AIOMetadata JSON & Verify Manifest
        item {
            ConsoleSectionCard(
                title = "Generate & Verify AIOMetadata Manifest",
                subtitle = "Copy your catalog board config, open AIOMetadata, and verify your live link",
                icon = Icons.Filled.Link,
                accentColor = EmeraldVerified,
                badgeText = if (profile.aioMetadataVerified) "VERIFIED" else "FINALIZE",
            ) {
                CopyableCodeBlock(
                    title = "AIOMETADATA BOARD CONFIG JSON",
                    code = generatedMetaJson,
                    onCopy = { copyToClipboard(context, "AIOMetadata Config JSON", generatedMetaJson, onNotify) },
                    testTagName = "copy_aiometa_json_button",
                )

                Spacer(Modifier.height(10.dp))
                ExternalPortalButton(
                    label = "Open AIOMetadata Configurator",
                    onClick = {
                        val url = instanceUrl.trim().ifBlank { "https://aiometadata.elfhosted.com" }
                        openExternalUrl(context, url, onNotify)
                    },
                    testTagName = "open_aiometa_web_button",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = manifestUrl,
                    onValueChange = { manifestUrl = it },
                    label = { Text("AIOMetadata Manifest URL") },
                    placeholder = { Text("https://aiometadata.elfhosted.com/.../manifest.json") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aiometa_manifest_input"),
                )

                Spacer(Modifier.height(10.dp))
                VerificationBanner(
                    state = state.aioMetaManifestVerification,
                    persistedVerifiedLabel = if (profile.aioMetadataVerified) "AIOMetadata manifest verified live over HTTPS" else null,
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
                            .testTag("verify_aiometa_manifest_button"),
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = EmeraldVerified, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Verify Live", color = EmeraldVerified)
                    }
                    Button(
                        onClick = {
                            onSaveAioMetadata(
                                instanceUrl,
                                movieProvider,
                                seriesProvider,
                                animeProvider,
                                rpdbPosters,
                                animeCatalogs,
                                manifestUrl,
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("save_aiometa_manifest_button"),
                    ) {
                        Text("Save Manifest")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCatalogCollectionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, mediaType, kind, source, reference, description ->
                onAddCatalog(title, mediaType, kind, source, reference, description)
                showAddDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderSelectorRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                )
            }
        }
    }
}

@Composable
private fun CatalogRowCard(
    item: CatalogCollectionItem,
    positionLabel: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDesignCover: () -> Unit,
    onDelete: () -> Unit,
) {
    val kindAccent = if (item.itemKind == "COLLECTION") AmberDebrid else IndigoMeta
    Surface(
        color = if (item.enabled) ObsidianSurfaceContainer else ObsidianSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (item.enabled) kindAccent.copy(alpha = 0.45f) else ObsidianOutline,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = positionLabel,
                    style = MonoCodeStyle,
                    color = kindAccent,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(item.itemKind, kindAccent)
                        StatusPill(item.mediaType, CyanSignal)
                        StatusPill(item.sourceProvider, EmeraldVerified)
                    }
                }
                Switch(
                    checked = item.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("toggle_catalog_${item.id}"),
                )
            }
            if (item.description.isNotBlank() || item.sourceReference.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.sourceReference.isNotBlank()) {
                    Text(
                        text = "Source: ${item.sourceReference}",
                        style = MonoCodeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = onDesignCover,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("design_cover_${item.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Brush,
                        contentDescription = stringResource(R.string.cd_design_collection_cover),
                        tint = IndigoMeta,
                    )
                }
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.cd_move_up))
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.cd_move_down))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_item),
                        tint = RoseAlert,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCatalogCollectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        mediaType: String,
        itemKind: String,
        sourceProvider: String,
        sourceReference: String,
        description: String,
    ) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var itemKind by remember { mutableStateOf("CATALOG") }
    var mediaType by remember { mutableStateOf("Movies") }
    var sourceProvider by remember { mutableStateOf("MDBList") }
    var sourceReference by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val refPlaceholder = when (sourceProvider) {
        "MDBList" -> "e.g., https://mdblist.com/lists/user/top-4k"
        "Trakt" -> "e.g., trakt/box-office-hits"
        "Streaming" -> "e.g., provider/netflix,apple-tv-plus"
        "TMDB" -> "e.g., discover/movie?with_genres=878"
        else -> "e.g., collection/cyberpunk-classics"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Catalog or Collection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = itemKind == "CATALOG",
                        onClick = { itemKind = "CATALOG" },
                        label = { Text("Catalog Row") },
                    )
                    FilterChip(
                        selected = itemKind == "COLLECTION",
                        onClick = { itemKind = "COLLECTION" },
                        label = { Text("Curated Collection") },
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Display Row Title") },
                    placeholder = { Text("e.g., 4K Dolby Vision Classics") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_catalog_title_input"),
                )
                Text("Media Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SetupCatalogDefaults.mediaTypes.forEach { mt ->
                        FilterChip(
                            selected = mediaType == mt,
                            onClick = { mediaType = mt },
                            label = { Text(mt) },
                        )
                    }
                }
                Text("Source Provider", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SetupCatalogDefaults.sourceProviders.forEach { sp ->
                        FilterChip(
                            selected = sourceProvider == sp,
                            onClick = { sourceProvider = sp },
                            label = { Text(sp) },
                        )
                    }
                }
                OutlinedTextField(
                    value = sourceReference,
                    onValueChange = { sourceReference = it },
                    label = { Text("List URL, Slug, or Filter Reference") },
                    placeholder = { Text(refPlaceholder) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_catalog_ref_input"),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Short description for your board") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, mediaType, itemKind, sourceProvider, sourceReference, description)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("dialog_confirm_add_catalog_button"),
            ) {
                Text("Add Row")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
