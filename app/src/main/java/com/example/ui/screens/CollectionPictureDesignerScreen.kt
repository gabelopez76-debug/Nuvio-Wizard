package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.ConsoleSectionCard
import com.example.ui.components.StatusPill
import com.example.ui.designer.BackdropSourceMode
import com.example.ui.designer.CollectionArtworkRenderer
import com.example.ui.designer.CollectionArtworkSpec
import com.example.ui.designer.CollectionDesignerPresets
import com.example.ui.designer.CoverAspectRatio
import com.example.ui.designer.CoverLayoutStyle
import com.example.ui.theme.AmberDebrid
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IndigoMeta
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.RoseContainer
import com.example.ui.viewmodel.ArtworkGenUiState
import com.example.ui.viewmodel.SetupUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionPictureDesignerScreen(
    setupState: SetupUiState,
    aiGenState: ArtworkGenUiState,
    initialCollectionTitle: String? = null,
    initialCollectionSubtitle: String? = null,
    initialCollectionBadge: String? = null,
    onGenerateAiBackdrop: (title: String, prompt: String, mood: String) -> Unit,
    onClearAiError: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalogs = setupState.activeCatalogs

    val defaultItem = catalogs.firstOrNull { it.itemKind == "COLLECTION" } ?: catalogs.firstOrNull()
    var title by remember(initialCollectionTitle) {
        mutableStateOf(initialCollectionTitle?.takeIf { it.isNotBlank() } ?: defaultItem?.title ?: "4K Showcase Collection")
    }
    var subtitle by remember(initialCollectionSubtitle) {
        mutableStateOf(
            initialCollectionSubtitle?.takeIf { it.isNotBlank() }
                ?: defaultItem?.description?.ifBlank { "Dolby Vision & Atmos Reference Cinema" }
                ?: "Dolby Vision & Atmos Reference Cinema"
        )
    }
    var badgeText by remember(initialCollectionBadge) {
        mutableStateOf(
            initialCollectionBadge?.takeIf { it.isNotBlank() }
                ?: defaultItem?.let { "${it.itemKind} • ${it.sourceProvider}" }
                ?: "CURATED COLLECTION"
        )
    }

    var aspectRatio by remember { mutableStateOf(CoverAspectRatio.BANNER_16_9) }
    var layoutStyle by remember { mutableStateOf(CoverLayoutStyle.EDITORIAL_BOTTOM) }
    var backdropMode by remember { mutableStateOf(BackdropSourceMode.STUDIO_ART) }
    var selectedStudioIndex by remember { mutableIntStateOf(0) }
    var selectedNeonIndex by remember { mutableIntStateOf(0) }
    var selectedAccentIndex by remember { mutableIntStateOf(0) }
    var scrimStrength by remember { mutableFloatStateOf(0.72f) }
    var showFrameGlow by remember { mutableStateOf(true) }
    var showCinemaGrid by remember { mutableStateOf(false) }

    var importedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aiPrompt by remember { mutableStateOf("Futuristic home cinema observatory with glowing aurora horizon") }
    var aiMood by remember { mutableStateOf("Dark Moody Luxury Cinema") }
    var isExporting by remember { mutableStateOf(false) }

    val aiBitmap = (aiGenState as? ArtworkGenUiState.Success)?.bitmap

    LaunchedEffect(aiBitmap) {
        if (aiBitmap != null) {
            backdropMode = BackdropSourceMode.GEMINI_AI
        }
    }

    // Zero-permission Android Photo Picker (Play policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    CollectionArtworkRenderer.decodeSampledBitmapFromUri(context, uri)
                }
                if (decoded != null) {
                    importedPhotoBitmap = decoded
                    backdropMode = BackdropSourceMode.DEVICE_PHOTO
                    onNotify("Photo loaded into Collection Picture Designer.")
                } else {
                    onNotify("Could not decode the selected image.")
                }
            }
        }
    }

    val currentAccent = CollectionDesignerPresets.accentSwatches[selectedAccentIndex].second
    val currentStudio = CollectionDesignerPresets.studioBackdrops[selectedStudioIndex]
    val currentNeon = CollectionDesignerPresets.neonThemes[selectedNeonIndex]

    val currentSpec = CollectionArtworkSpec(
        title = title,
        subtitle = subtitle,
        badgeText = badgeText,
        aspectRatio = aspectRatio,
        layoutStyle = layoutStyle,
        accentColor = currentAccent,
        backdropMode = backdropMode,
        studioDrawableRes = currentStudio.drawableRes,
        neonTheme = currentNeon,
        scrimStrength = scrimStrength,
        showFrameGlow = showFrameGlow,
        showCinemaGrid = showCinemaGrid,
        importedPhotoBitmap = importedPhotoBitmap,
        aiGeneratedBitmap = aiBitmap,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("collection_designer_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Live WYSIWYG Collection Picture Canvas Preview
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Cover Preview",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${aspectRatio.exportWidth} × ${aspectRatio.exportHeight} PNG • ${layoutStyle.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanSignal,
                            )
                        }
                        StatusPill(backdropMode.label.uppercase(), currentAccent)
                    }

                    Spacer(Modifier.height(14.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CollectionCoverLivePreview(
                            spec = currentSpec,
                            modifier = Modifier
                                .widthIn(
                                    max = when (aspectRatio) {
                                        CoverAspectRatio.BANNER_16_9 -> 540.dp
                                        CoverAspectRatio.POSTER_2_3 -> 240.dp
                                        CoverAspectRatio.SQUARE_1_1 -> 290.dp
                                    }
                                )
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio.ratio)
                                .testTag("designer_live_preview"),
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Aspect Ratio Selector Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CoverAspectRatio.entries.forEach { ratioOption ->
                            FilterChip(
                                selected = aspectRatio == ratioOption,
                                onClick = { aspectRatio = ratioOption },
                                label = { Text(ratioOption.label, maxLines = 1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("designer_ratio_${ratioOption.name.lowercase()}"),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Primary Publish to Gallery & Share Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    val result = withContext(Dispatchers.IO) {
                                        val bmp = CollectionArtworkRenderer.renderHighResBitmap(context, currentSpec)
                                        val res = CollectionArtworkRenderer.saveBitmapToGallery(context, bmp, title)
                                        bmp.recycle()
                                        res
                                    }
                                    isExporting = false
                                    result.onSuccess { msg -> onNotify(msg) }
                                        .onFailure { err -> onNotify(err.localizedMessage ?: "Failed to save image.") }
                                }
                            },
                            enabled = !isExporting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldVerified,
                                contentColor = ObsidianVoid,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("designer_save_gallery_button"),
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = ObsidianVoid,
                                )
                            } else {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Save to Gallery", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    val result = withContext(Dispatchers.IO) {
                                        val bmp = CollectionArtworkRenderer.renderHighResBitmap(context, currentSpec)
                                        val res = CollectionArtworkRenderer.shareBitmap(context, bmp, title)
                                        bmp.recycle()
                                        res
                                    }
                                    isExporting = false
                                    result.onFailure { err -> onNotify(err.localizedMessage ?: "Could not share image.") }
                                }
                            },
                            enabled = !isExporting,
                            border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("designer_share_png_button"),
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = CyanSignal, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share PNG", color = CyanSignal, maxLines = 1)
                        }
                    }
                }
            }
        }

        // 2. Pick Target Collection + Text Overlay
        item {
            ConsoleSectionCard(
                title = "Collection Title & Labels",
                subtitle = "Load any row from your AIOMetadata board or type custom artwork copy",
                icon = Icons.Filled.Brush,
                accentColor = IndigoMeta,
            ) {
                if (catalogs.isNotEmpty()) {
                    Text(
                        text = "Quick-Fill from Your Collections",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        catalogs.forEach { item ->
                            FilterChip(
                                selected = title == item.title,
                                onClick = {
                                    title = item.title
                                    subtitle = item.description.ifBlank { "${item.mediaType} curated via ${item.sourceProvider}" }
                                    badgeText = "${item.itemKind} • ${item.sourceProvider}"
                                },
                                label = { Text(item.title, maxLines = 1) },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Collection Display Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("designer_title_input"),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle / Tagline") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("designer_subtitle_input"),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = badgeText,
                    onValueChange = { badgeText = it },
                    label = { Text("Top Pill Badge (e.g., 4K DOLBY VISION)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("designer_badge_input"),
                )
            }
        }

        // 3. Backdrop Source Studio (Curated Art, Neon Mesh, Device Photo Picker, Gemini AI)
        item {
            ConsoleSectionCard(
                title = "Backdrop Artwork Source",
                subtitle = "Choose bundled studio art, gradient meshes, device photos, or Gemini AI art",
                icon = Icons.Filled.Image,
                accentColor = CyanSignal,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BackdropSourceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = backdropMode == mode,
                            onClick = { backdropMode = mode },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("designer_source_${mode.name.lowercase()}"),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                when (backdropMode) {
                    BackdropSourceMode.STUDIO_ART -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            CollectionDesignerPresets.studioBackdrops.forEachIndexed { idx, preset ->
                                val selected = idx == selectedStudioIndex
                                Surface(
                                    onClick = { selectedStudioIndex = idx },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) currentAccent else ObsidianOutline,
                                    ),
                                    modifier = Modifier
                                        .width(126.dp)
                                        .testTag("designer_studio_${preset.id}"),
                                ) {
                                    Column {
                                        Image(
                                            painter = painterResource(id = preset.drawableRes),
                                            contentDescription = preset.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(68.dp),
                                        )
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) currentAccent else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    BackdropSourceMode.NEON_MESH -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            CollectionDesignerPresets.neonThemes.forEachIndexed { idx, theme ->
                                val selected = idx == selectedNeonIndex
                                Surface(
                                    onClick = { selectedNeonIndex = idx },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) currentAccent else ObsidianOutline,
                                    ),
                                    modifier = Modifier.width(120.dp),
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(58.dp)
                                                .background(Brush.linearGradient(theme.colors))
                                        )
                                        Text(
                                            text = theme.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) currentAccent else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            modifier = Modifier.padding(8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    BackdropSourceMode.DEVICE_PHOTO -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Uses Android's privacy-first Photo Picker (no storage permissions needed).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("designer_pick_photo_button"),
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (importedPhotoBitmap != null) "Choose Different Photo" else "Pick Photo from Device")
                            }
                        }
                    }

                    BackdropSourceMode.GEMINI_AI -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = aiPrompt,
                                onValueChange = {
                                    aiPrompt = it
                                    onClearAiError()
                                },
                                label = { Text("Describe Your Collection Backdrop") },
                                placeholder = { Text("e.g., Golden-age noir city alley under neon rain") },
                                minLines = 2,
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("designer_ai_prompt_input"),
                            )
                            val moodOptions = listOf(
                                "Dark Moody Luxury Cinema",
                                "Cyberpunk Neon Anime",
                                "Minimalist Sci-Fi Vista",
                                "Warm Vintage 35mm Film",
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                moodOptions.forEach { mood ->
                                    FilterChip(
                                        selected = aiMood == mood,
                                        onClick = { aiMood = mood },
                                        label = { Text(mood) },
                                    )
                                }
                            }

                            when (aiGenState) {
                                ArtworkGenUiState.Loading -> {
                                    Surface(
                                        color = ObsidianSurfaceContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(12.dp),
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = IndigoMeta,
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = "Synthesizing custom artwork with Gemini (30–60s)...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                is ArtworkGenUiState.Error -> {
                                    Surface(
                                        color = RoseContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, RoseAlert.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(12.dp),
                                        ) {
                                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = RoseAlert, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = aiGenState.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = RoseAlert,
                                            )
                                        }
                                    }
                                }
                                else -> Unit
                            }

                            Button(
                                onClick = { onGenerateAiBackdrop(title, aiPrompt, aiMood) },
                                enabled = aiGenState !is ArtworkGenUiState.Loading && aiPrompt.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IndigoMeta,
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("designer_generate_ai_button"),
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Generate AI Backdrop")
                            }
                        }
                    }
                }
            }
        }

        // 4. Layout, Accent Color & Overlay Polish
        item {
            ConsoleSectionCard(
                title = "Typography Layout & Polish",
                subtitle = "Fine-tune composition, accent glow, and contrast scrim for Nuvio TV readability",
                icon = Icons.Filled.Tune,
                accentColor = AmberDebrid,
            ) {
                Text("Layout Style", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CoverLayoutStyle.entries.forEach { style ->
                        FilterChip(
                            selected = layoutStyle == style,
                            onClick = { layoutStyle = style },
                            label = { Text(style.label) },
                            modifier = Modifier.testTag("designer_layout_${style.name.lowercase()}"),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Accent Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CollectionDesignerPresets.accentSwatches.forEachIndexed { idx, (name, swatch) ->
                        val selected = idx == selectedAccentIndex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) Color.White else ObsidianOutline,
                                    shape = CircleShape,
                                )
                                .clickable { selectedAccentIndex = idx }
                                .testTag("designer_accent_${name.lowercase()}"),
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "$name selected",
                                    tint = ObsidianVoid,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Contrast Vignette Scrim", style = MaterialTheme.typography.bodyMedium)
                    Text("${(scrimStrength * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = currentAccent)
                }
                Slider(
                    value = scrimStrength,
                    onValueChange = { scrimStrength = it },
                    valueRange = 0.2f..0.95f,
                    colors = SliderDefaults.colors(
                        thumbColor = currentAccent,
                        activeTrackColor = currentAccent,
                    ),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Accent Border Glow Frame", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showFrameGlow, onCheckedChange = { showFrameGlow = it })
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Subtle Cinema Grid Texture", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showCinemaGrid, onCheckedChange = { showCinemaGrid = it })
                }
            }
        }
    }
}

@Composable
private fun CollectionCoverLivePreview(
    spec: CollectionArtworkSpec,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianVoid)
            .border(1.dp, ObsidianOutline, RoundedCornerShape(16.dp)),
    ) {
        // 1. Backdrop Layer
        when (spec.backdropMode) {
            BackdropSourceMode.STUDIO_ART -> {
                Image(
                    painter = painterResource(id = spec.studioDrawableRes),
                    contentDescription = "Collection studio artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            BackdropSourceMode.DEVICE_PHOTO -> {
                val bmp = spec.importedPhotoBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Imported collection backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(spec.neonTheme.colors))
                    )
                }
            }
            BackdropSourceMode.GEMINI_AI -> {
                val bmp = spec.aiGeneratedBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "AI generated collection backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(spec.neonTheme.colors))
                    )
                }
            }
            BackdropSourceMode.NEON_MESH -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(spec.neonTheme.colors))
                )
            }
        }

        // 2. Cinema Grid + Scrim Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (spec.showCinemaGrid) {
                val step = (size.minDimension / 10f).coerceAtLeast(18f)
                var x = step
                while (x < size.width) {
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070A10).copy(alpha = (spec.scrimStrength * 0.35f).coerceIn(0f, 0.85f)),
                        Color(0xFF070A10).copy(alpha = (spec.scrimStrength * 0.52f).coerceIn(0f, 0.90f)),
                        Color(0xFF070A10).copy(alpha = (spec.scrimStrength * 0.94f).coerceIn(0.2f, 0.98f)),
                    )
                )
            )
        }

        // 3. Optional Accent Glow Frame
        if (spec.showFrameGlow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .border(
                        width = 1.5.dp,
                        color = spec.accentColor.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(12.dp),
                    )
            )
        }

        // 4. Typography Layout Overlay
        val displayTitle = spec.title.trim().ifBlank { "UNTITLED COLLECTION" }
        val displaySubtitle = spec.subtitle.trim()
        val displayBadge = spec.badgeText.trim().uppercase()

        when (spec.layoutStyle) {
            CoverLayoutStyle.CENTERED_CREST -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                ) {
                    if (displayBadge.isNotBlank()) {
                        StatusPill(text = displayBadge, color = spec.accentColor)
                        Spacer(Modifier.height(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(spec.accentColor)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (displaySubtitle.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = displaySubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            CoverLayoutStyle.MINIMAL_CORNER -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                ) {
                    if (displayBadge.isNotBlank()) {
                        StatusPill(
                            text = displayBadge,
                            color = spec.accentColor,
                            modifier = Modifier.align(Alignment.TopStart),
                        )
                    }
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(spec.accentColor)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (displaySubtitle.isNotBlank()) {
                            Text(
                                text = displaySubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            CoverLayoutStyle.CINEMA_STRIP -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                ) {
                    Surface(
                        color = Color(0xCC0A0F18),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, spec.accentColor.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(spec.accentColor)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (displayBadge.isNotBlank()) {
                                    Text(
                                        text = displayBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = spec.accentColor,
                                        maxLines = 1,
                                    )
                                }
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (displaySubtitle.isNotBlank()) {
                                    Text(
                                        text = displaySubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CoverLayoutStyle.EDITORIAL_BOTTOM -> {
                Column(
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                ) {
                    if (displayBadge.isNotBlank()) {
                        StatusPill(text = displayBadge, color = spec.accentColor)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (displaySubtitle.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = displaySubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
