package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SetupCatalogDefaults
import com.example.data.model.VerificationState
import com.example.ui.components.ConsoleSectionCard
import com.example.ui.components.StatusPill
import com.example.ui.components.VerificationBanner
import com.example.ui.components.VerticalConfigurationStepper
import com.example.ui.theme.AmberDebrid
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IndigoMeta
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseAlert
import com.example.ui.viewmodel.SetupUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuideDashboardScreen(
    state: SetupUiState,
    onOpenProfiles: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToAioStreams: () -> Unit,
    onNavigateToAioMetadata: () -> Unit,
    onNavigateToDeploy: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToSummary: () -> Unit,
    onNavigateToMarketplace: () -> Unit,
    onNavigateToArtDesigner: () -> Unit,
    onVerifyRealDebrid: (String) -> Unit,
    onVerifyTmdb: (String) -> Unit,
    onVerifyAioStreamsManifest: (String) -> Unit,
    onVerifyAioMetadataManifest: (String) -> Unit,
) {
    val profile = state.activeProfile
    val progress = state.progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress.granularFraction,
        animationSpec = spring(stiffness = 220f),
        label = "setup_progress",
    )

    val nextStepAction: Pair<String, () -> Unit> = when {
        !progress.nuvioDone -> "Start Step 1: Nuvio Account" to onNavigateToAccounts
        !progress.debridDone -> "Step 2: Real-Debrid & APIs" to onNavigateToAccounts
        !progress.aioStreamsDone -> "Step 3: AIOStreams Hub" to onNavigateToAioStreams
        !progress.aioMetaDone -> "Step 4: AIOMetadata Board" to onNavigateToAioMetadata
        !progress.deployDone -> "Step 5: Deploy to Nuvio" to onNavigateToDeploy
        else -> "View Setup Summary (100%)" to onNavigateToSummary
    }

    val nuvioStatusLabel = when {
        profile?.nuvioAccountCreated == true && profile.nuvioUsername.isNotBlank() -> "ACTIVE"
        profile?.nuvioUsername?.isNotBlank() == true -> "DRAFTED"
        else -> "NOT SET"
    }
    val nuvioStatusColor = when (nuvioStatusLabel) {
        "ACTIVE" -> EmeraldVerified
        "DRAFTED" -> AmberDebrid
        else -> RoseAlert
    }

    val rdStatusLabel = when {
        state.rdVerification is VerificationState.Checking -> "CHECKING"
        profile?.realDebridVerified == true -> "CONNECTED"
        profile?.realDebridApiKey?.isNotBlank() == true -> "UNVERIFIED"
        else -> "DISCONNECTED"
    }
    val rdStatusColor = when (rdStatusLabel) {
        "CONNECTED" -> EmeraldVerified
        "CHECKING" -> CyanSignal
        "UNVERIFIED" -> AmberDebrid
        else -> RoseAlert
    }

    val totalStreamModules = (profile?.enabledAddons?.size ?: 0) + (profile?.customStreamSources?.size ?: 0)
    val aioStreamsAvailable = (profile?.realDebridApiKey?.isNotBlank() == true) && totalStreamModules > 0
    val aioStreamsStatusLabel = when {
        state.aioStreamsManifestVerification is VerificationState.Checking -> "TESTING"
        profile?.aioStreamsVerified == true -> "LIVE"
        aioStreamsAvailable && profile?.aioStreamsManifestUrl?.isNotBlank() == true -> "MANIFEST SET"
        aioStreamsAvailable -> "READY TO EXPORT"
        else -> "NEEDS SETUP"
    }
    val aioStreamsStatusColor = when (aioStreamsStatusLabel) {
        "LIVE" -> EmeraldVerified
        "TESTING", "MANIFEST SET" -> CyanSignal
        "READY TO EXPORT" -> AmberDebrid
        else -> RoseAlert
    }

    val enabledCatalogCount = state.activeCatalogs.count { it.enabled }
    val aioMetaStatusLabel = when {
        profile?.aioMetadataVerified == true -> "LIVE"
        profile?.aioMetadataManifestUrl?.isNotBlank() == true -> "MANIFEST SET"
        enabledCatalogCount > 0 -> "BOARD READY"
        else -> "EMPTY"
    }
    val aioMetaStatusColor = when (aioMetaStatusLabel) {
        "LIVE" -> EmeraldVerified
        "MANIFEST SET", "BOARD READY" -> IndigoMeta
        else -> RoseAlert
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("central_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Hero Command Progress Banner
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_setup),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ObsidianVoid.copy(alpha = 0.95f),
                                        ObsidianVoid.copy(alpha = 0.78f),
                                        ObsidianVoid.copy(alpha = 0.38f),
                                    )
                                )
                            )
                    )
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                StatusPill(
                                    text = "PROFILE: ${profile?.profileName?.uppercase() ?: "LOADING"}",
                                    color = CyanSignal,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Nuvio Control Dashboard",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${progress.completedSteps}/${progress.totalSteps} milestones • ${progress.completedTasks}/${progress.totalTasks} checks complete",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .size(82.dp)
                                    .testTag("dashboard_progress_ring"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = 8.dp.toPx()
                                    drawArc(
                                        color = ObsidianOutline,
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                                    )
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            listOf(CyanSignal, IndigoMeta, EmeraldVerified, CyanSignal)
                                        ),
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedProgress,
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${progress.granularPercent}%",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        text = "${progress.completedTasks}/${progress.totalTasks}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanSignal,
                                    )
                                }
                            }
                        }

                        // Segmented 5-Milestone Progress Bar + Primary Next Action
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                val flags = listOf(
                                    progress.nuvioDone to CyanSignal,
                                    progress.debridDone to AmberDebrid,
                                    progress.aioStreamsDone to CyanSignal,
                                    progress.aioMetaDone to IndigoMeta,
                                    progress.deployDone to EmeraldVerified,
                                )
                                flags.forEach { (done, color) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (done) color else ObsidianOutline)
                                    )
                                }
                            }

                            Button(
                                onClick = nextStepAction.second,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("hero_next_step_button"),
                            ) {
                                Text(nextStepAction.first, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Live System Telemetry Overview (Account, Real-Debrid, AIOStreams, AIOMetadata)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "System Status & Availability",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                StatusPill(
                    text = "${progress.completedSteps}/${progress.totalSteps} READY",
                    color = if (progress.completedSteps == progress.totalSteps) EmeraldVerified else CyanSignal,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Nuvio Account Status + AIOMetadata Board Status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TelemetryTileCard(
                        title = "Nuvio Account",
                        statusLabel = nuvioStatusLabel,
                        statusColor = nuvioStatusColor,
                        primaryValue = profile?.nuvioUsername?.ifBlank { "No username yet" } ?: "Not configured",
                        secondaryText = when {
                            profile?.nuvioAccountCreated == true -> profile.nuvioEmail.ifBlank { "Account ready for sign-in" }
                            profile?.nuvioPasswordHint?.isNotBlank() == true -> "Credentials saved • Finish sign-up"
                            else -> "Tap to configure username & password"
                        },
                        icon = Icons.Filled.AccountCircle,
                        testTagName = "dashboard_account_status_card",
                        onClick = onNavigateToAccounts,
                        modifier = Modifier.weight(1f),
                    )
                    TelemetryTileCard(
                        title = "AIOMetadata",
                        statusLabel = aioMetaStatusLabel,
                        statusColor = aioMetaStatusColor,
                        primaryValue = "$enabledCatalogCount Active Rows",
                        secondaryText = "${profile?.movieProvider ?: "TMDB"} • ${profile?.seriesProvider ?: "TMDB"} • ${if (profile?.rpdbPostersEnabled == true) "RPDB On" else "Standard Art"}",
                        icon = Icons.Filled.AutoAwesomeMosaic,
                        testTagName = "dashboard_aiometa_status_card",
                        onClick = onNavigateToAioMetadata,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Card: Real-Debrid Live Connection State
                ConsoleSectionCard(
                    title = "Real-Debrid Connection State",
                    subtitle = "Master debrid tunnel shared once across all AIOStreams sources",
                    icon = if (profile?.realDebridVerified == true) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                    accentColor = rdStatusColor,
                    badgeText = rdStatusLabel,
                    modifier = Modifier.testTag("dashboard_realdebrid_status_card"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    profile?.realDebridVerified == true -> profile.realDebridUserLabel
                                    profile?.realDebridApiKey?.isNotBlank() == true -> "API Token stored (pending live ping)"
                                    else -> "No Real-Debrid API key configured"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = when {
                                    profile?.realDebridVerified == true && profile.realDebridPremiumDays > 0L ->
                                        "Premium active • ${profile.realDebridPremiumDays} days remaining • Cached 4K streams enabled"
                                    profile?.realDebridVerified == true ->
                                        "Connected (Free tier) • Activate Premium for instant cached streams"
                                    profile?.realDebridApiKey?.isNotBlank() == true ->
                                        "Tap Test Connection to confirm your Real-Debrid Premium status."
                                    else ->
                                        "Open Accounts to register Real-Debrid and paste your token once."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (state.rdVerification !is VerificationState.Idle) {
                        Spacer(Modifier.height(10.dp))
                        VerificationBanner(state = state.rdVerification)
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val key = profile?.realDebridApiKey.orEmpty()
                                if (key.isNotBlank()) {
                                    onVerifyRealDebrid(key)
                                } else {
                                    onNavigateToAccounts()
                                }
                            },
                            enabled = state.rdVerification !is VerificationState.Checking,
                            border = BorderStroke(1.dp, rdStatusColor.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_test_rd_button"),
                        ) {
                            if (state.rdVerification is VerificationState.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CyanSignal,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = rdStatusColor,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (profile?.realDebridApiKey?.isNotBlank() == true) "Test Connection" else "Add RD Token",
                                color = rdStatusColor,
                                maxLines = 1,
                            )
                        }
                        OutlinedButton(
                            onClick = onNavigateToAccounts,
                            border = BorderStroke(1.dp, ObsidianOutline),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_manage_keys_button"),
                        ) {
                            Text("Keys Vault")
                        }
                    }
                }

                // Card: AIOStreams Module Availability Matrix
                ConsoleSectionCard(
                    title = "AIOStreams Module Availability",
                    subtitle = "$totalStreamModules active source(s) inheriting master Real-Debrid & ${profile?.formatterPreset ?: "PRISM"} formatter",
                    icon = Icons.Filled.Hub,
                    accentColor = aioStreamsStatusColor,
                    badgeText = aioStreamsStatusLabel,
                    modifier = Modifier.testTag("dashboard_aiostreams_availability_card"),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val enabledIds = profile?.enabledAddons.orEmpty()
                        SetupCatalogDefaults.builtInStreamAddons.forEach { spec ->
                            val enabled = spec.id in enabledIds
                            val dotColor = when {
                                enabled && profile?.realDebridApiKey?.isNotBlank() == true -> EmeraldVerified
                                enabled -> AmberDebrid
                                else -> ObsidianOutline
                            }
                            ModuleAvailabilityChip(
                                name = spec.name,
                                active = enabled,
                                indicatorColor = dotColor,
                            )
                        }
                        profile?.customStreamSources.orEmpty().forEachIndexed { idx, _ ->
                            ModuleAvailabilityChip(
                                name = "Custom #${idx + 1}",
                                active = true,
                                indicatorColor = if (profile?.realDebridApiKey?.isNotBlank() == true) CyanSignal else AmberDebrid,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = buildString {
                            append("Resolutions: ")
                            append(profile?.preferredResolutions?.joinToString(", ")?.ifBlank { "Any" } ?: "2160p, 1080p")
                            append(" • ")
                            append(if (profile?.cachedOnly == true) "Cached RD Only" else "Cached + Uncached")
                            append(" • Max ")
                            append("${profile?.maxFileSizeGb ?: 35L} GB")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (state.aioStreamsManifestVerification !is VerificationState.Idle) {
                        Spacer(Modifier.height(10.dp))
                        VerificationBanner(state = state.aioStreamsManifestVerification)
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToAioStreams,
                            border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_configure_streams_button"),
                        ) {
                            Text("Configure Modules", color = CyanSignal)
                        }
                        if (profile?.aioStreamsManifestUrl?.isNotBlank() == true) {
                            OutlinedButton(
                                onClick = { onVerifyAioStreamsManifest(profile.aioStreamsManifestUrl) },
                                enabled = state.aioStreamsManifestVerification !is VerificationState.Checking,
                                border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_ping_manifest_button"),
                            ) {
                                Text("Ping Manifest", color = EmeraldVerified)
                            }
                        }
                    }
                }
            }
        }

        // 3. Profile Switcher & AI Support Quick Bar
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    onClick = onOpenProfiles,
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_profiles_card"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            tint = CyanSignal,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profiles (${state.profiles.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Switch or add",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }

                Surface(
                    onClick = onNavigateToSupport,
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, IndigoMeta.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_ask_ai_card"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = IndigoMeta,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Troubleshooter",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Ask Gemini",
                                style = MaterialTheme.typography.bodySmall,
                                color = IndigoMeta,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Quick Bar Row 2: Compatible Add-on Marketplace + Collection Picture Designer
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    onClick = onNavigateToMarketplace,
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_marketplace_card"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = CyanSignal,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Marketplace",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Nuvio & Stremio",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanSignal,
                                maxLines = 1,
                            )
                        }
                    }
                }

                Surface(
                    onClick = onNavigateToArtDesigner,
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AmberDebrid.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_designer_card"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Brush,
                            contentDescription = null,
                            tint = AmberDebrid,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cover Studio",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Design & Gallery",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberDebrid,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        // 4. Vertical Configuration Stepper (Account, Real-Debrid & API Verification, AIOStreams, AIOMetadata, Deploy)
        item {
            VerticalConfigurationStepper(
                state = state,
                onNavigateToRoute = { route ->
                    when (route) {
                        "accounts" -> onNavigateToAccounts()
                        "aiostreams" -> onNavigateToAioStreams()
                        "aiometadata" -> onNavigateToAioMetadata()
                        "deploy" -> onNavigateToDeploy()
                        else -> onNavigateToAccounts()
                    }
                },
                onVerifyRealDebrid = onVerifyRealDebrid,
                onVerifyTmdb = onVerifyTmdb,
                onVerifyAioStreamsManifest = onVerifyAioStreamsManifest,
                onVerifyAioMetadataManifest = onVerifyAioMetadataManifest,
                onOpenSetupSummary = onNavigateToSummary,
            )
        }
    }
}

@Composable
private fun TelemetryTileCard(
    title: String,
    statusLabel: String,
    statusColor: Color,
    primaryValue: String,
    secondaryText: String,
    icon: ImageVector,
    testTagName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        modifier = modifier.testTag(testTagName),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(statusColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                StatusPill(text = statusLabel, color = statusColor)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ModuleAvailabilityChip(
    name: String,
    active: Boolean,
    indicatorColor: Color,
) {
    Surface(
        color = if (active) ObsidianSurfaceContainer else ObsidianVoid.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (active) indicatorColor.copy(alpha = 0.45f) else ObsidianOutline,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

