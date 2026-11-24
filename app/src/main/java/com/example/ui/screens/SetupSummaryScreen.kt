package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.config.ConfigGenerator
import com.example.ui.components.ConsoleSectionCard
import com.example.ui.components.StatusPill
import com.example.ui.components.copyToClipboard
import com.example.ui.components.shareTextContent
import com.example.ui.theme.AmberDebrid
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IndigoMeta
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianVoid
import com.example.ui.viewmodel.SetupMilestoneProgress
import com.example.ui.viewmodel.SetupUiState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SetupSummaryScreen(
    state: SetupUiState,
    onNavigateToRoute: (String) -> Unit,
    onBackToDashboard: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val profile = state.activeProfile ?: return
    val progress = state.progress
    val allComplete = progress.completedSteps == progress.totalSteps && progress.totalSteps > 0

    var replaySeed by remember { mutableIntStateOf(0) }
    val ringProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val burstProgress = remember { Animatable(0f) }

    LaunchedEffect(allComplete, progress.granularFraction, replaySeed) {
        ringProgress.snapTo(0f)
        checkProgress.snapTo(0f)
        burstProgress.snapTo(0f)
        ringProgress.animateTo(
            targetValue = progress.granularFraction,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        )
        if (allComplete) {
            checkProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            )
            burstProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            )
        }
    }

    val animatedPercent = (ringProgress.value * 100f).toInt().coerceIn(0, 100)
    val nextIncompleteMilestone = progress.milestones.firstOrNull { !it.isCompleted }

    val totalStreamModules = profile.enabledAddons.size + profile.customStreamSources.size
    val enabledCatalogCount = state.activeCatalogs.count { it.enabled }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("setup_summary_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Celebratory Hero Emblem + Completion Percentage + Success Animation
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = ObsidianSurface,
                border = BorderStroke(
                    width = if (allComplete) 1.5.dp else 1.dp,
                    color = if (allComplete) EmeraldVerified.copy(alpha = 0.65f) else CyanSignal.copy(alpha = 0.45f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_summary_hero"),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = if (allComplete) {
                                    listOf(
                                        EmeraldVerified.copy(alpha = 0.14f),
                                        ObsidianSurface,
                                        ObsidianVoid.copy(alpha = 0.9f),
                                    )
                                } else {
                                    listOf(
                                        CyanSignal.copy(alpha = 0.10f),
                                        ObsidianSurface,
                                        ObsidianVoid.copy(alpha = 0.9f),
                                    )
                                }
                            )
                        )
                        .padding(22.dp),
                ) {
                    if (allComplete) {
                        IconButton(
                            onClick = { replaySeed++ },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .minimumInteractiveComponentSize()
                                .testTag("summary_replay_animation_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.cd_replay_celebration),
                                tint = EmeraldVerified,
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        StatusPill(
                            text = if (allComplete) {
                                "ALL ${progress.totalSteps} STEPS COMPLETE"
                            } else {
                                "${progress.completedSteps} OF ${progress.totalSteps} STEPS COMPLETE"
                            },
                            color = if (allComplete) EmeraldVerified else CyanSignal,
                        )

                        Spacer(Modifier.height(16.dp))

                        // Animated Radial Progress Ring + Success Checkmark & Particle Burst
                        SetupSummaryAnimationStage(
                            ringFraction = ringProgress.value,
                            checkFraction = checkProgress.value,
                            burstFraction = burstProgress.value,
                            displayPercent = animatedPercent,
                            allComplete = allComplete,
                            completedTasks = progress.completedTasks,
                            totalTasks = progress.totalTasks,
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = if (allComplete) {
                                "Nuvio Configuration Complete!"
                            } else {
                                "Configuration In Progress"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (allComplete) {
                                "Profile '${profile.profileName}' passed all ${progress.totalTasks} setup checks. Your unified Real-Debrid streams and AIOMetadata catalogs are ready for playback."
                            } else {
                                "${progress.nextPendingTask} — finish the remaining checks in the vertical stepper to unlock 100% readiness."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(18.dp))

                        // Quick Stats Strip
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            SummaryMetricPill(
                                label = "Completion",
                                value = "${progress.granularPercent}%",
                                accent = if (allComplete) EmeraldVerified else CyanSignal,
                                modifier = Modifier.weight(1f),
                            )
                            SummaryMetricPill(
                                label = "Stream Modules",
                                value = "$totalStreamModules",
                                accent = CyanSignal,
                                modifier = Modifier.weight(1f),
                            )
                            SummaryMetricPill(
                                label = "Catalog Rows",
                                value = "$enabledCatalogCount",
                                accent = IndigoMeta,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (!allComplete && nextIncompleteMilestone != null) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { onNavigateToRoute(nextIncompleteMilestone.route) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("summary_continue_next_step_button"),
                            ) {
                                Text("Continue Step ${nextIncompleteMilestone.stepNumber}: ${nextIncompleteMilestone.shortLabel}")
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

        // 2. Step-by-Step Verification Scorecard
        item {
            ConsoleSectionCard(
                title = "Configuration Scorecard",
                subtitle = "Tap any milestone below to review or edit its settings",
                icon = Icons.Filled.Verified,
                accentColor = if (allComplete) EmeraldVerified else CyanSignal,
                badgeText = "${progress.completedTasks}/${progress.totalTasks} CHECKS",
                modifier = Modifier.testTag("summary_scorecard_card"),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    progress.milestones.forEach { milestone ->
                        SummaryMilestoneRow(
                            milestone = milestone,
                            onClick = { onNavigateToRoute(milestone.route) },
                        )
                    }
                }
            }
        }

        // 3. Export & Backup Actions
        item {
            ConsoleSectionCard(
                title = "Export & Backup Profile",
                subtitle = "Share a clean configuration report or copy it to your notes (passwords redacted)",
                icon = Icons.Filled.Share,
                accentColor = AmberDebrid,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = {
                            val summaryText = ConfigGenerator.generateFullExportSummary(
                                profile = profile,
                                catalogs = state.activeCatalogs,
                                includeSecrets = false,
                            )
                            shareTextContent(context, "Nuvio Setup Summary — ${profile.profileName}", summaryText)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberDebrid,
                            contentColor = ObsidianVoid,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("summary_share_button"),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share Report", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            val summaryText = ConfigGenerator.generateFullExportSummary(
                                profile = profile,
                                catalogs = state.activeCatalogs,
                                includeSecrets = false,
                            )
                            copyToClipboard(context, "Nuvio Setup Summary", summaryText, onNotify)
                        },
                        border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("summary_copy_button"),
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = CyanSignal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Sheet", color = CyanSignal, maxLines = 1)
                    }
                }
            }
        }

        // 4. Return to Central Dashboard CTA
        item {
            OutlinedButton(
                onClick = onBackToDashboard,
                border = BorderStroke(1.dp, ObsidianOutline),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("summary_back_dashboard_button"),
            ) {
                Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Return to Control Dashboard")
            }
        }
    }
}

@Composable
private fun SetupSummaryAnimationStage(
    ringFraction: Float,
    checkFraction: Float,
    burstFraction: Float,
    displayPercent: Int,
    allComplete: Boolean,
    completedTasks: Int,
    totalTasks: Int,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "summary_halo")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "halo_pulse",
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "halo_orbit",
    )

    val particlePalette = remember {
        listOf(EmeraldVerified, CyanSignal, AmberDebrid, IndigoMeta)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(204.dp)
            .testTag(if (allComplete) "summary_success_animation" else "summary_progress_ring"),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val ringRadius = size.minDimension * 0.36f
            val ringStroke = 12.dp.toPx()

            // Ambient radial glow behind ring
            if (allComplete) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            EmeraldVerified.copy(alpha = 0.28f),
                            CyanSignal.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = ringRadius * 1.48f * haloPulse,
                    ),
                    radius = ringRadius * 1.48f * haloPulse,
                    center = center,
                )

                // Expanding shockwave ring during celebration burst
                if (burstFraction > 0f && burstFraction < 1f) {
                    val waveRadius = ringRadius + (ringRadius * 0.42f * burstFraction)
                    drawCircle(
                        color = EmeraldVerified.copy(alpha = (1f - burstFraction) * 0.55f),
                        radius = waveRadius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx() * (1f - burstFraction * 0.5f)),
                    )
                }

                // Celebratory geometric particle burst (16 radial sparks)
                if (burstFraction > 0.02f) {
                    val fade = (1f - (burstFraction * 0.85f)).coerceIn(0f, 1f)
                    val particleCount = 16
                    for (i in 0 until particleCount) {
                        val angleRad = ((i * (360f / particleCount)) + ringRotation * 0.12f) * (PI / 180.0)
                        val startDist = ringRadius * 1.04f
                        val travel = ringRadius * (0.28f + (i % 3) * 0.11f) * burstFraction
                        val dist = startDist + travel
                        val px = center.x + (cos(angleRad) * dist).toFloat()
                        val py = center.y + (sin(angleRad) * dist).toFloat()
                        val dotRadius = (4.5f - (i % 2) * 1.3f).dp.toPx() * (0.4f + 0.6f * fade)
                        drawCircle(
                            color = particlePalette[i % particlePalette.size].copy(alpha = fade),
                            radius = dotRadius,
                            center = Offset(px, py),
                        )
                    }
                }
            }

            // Track ring
            val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
            val arcSize = Size(ringRadius * 2f, ringRadius * 2f)
            drawArc(
                color = ObsidianOutline,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = ringStroke, cap = StrokeCap.Round),
            )

            // Animated Progress Sweep
            if (ringFraction > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = if (allComplete) {
                            listOf(EmeraldVerified, CyanSignal, EmeraldVerified, IndigoMeta, EmeraldVerified)
                        } else {
                            listOf(CyanSignal, IndigoMeta, AmberDebrid, CyanSignal)
                        },
                        center = center,
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * ringFraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = ringStroke, cap = StrokeCap.Round),
                )
            }

            // Inner glowing medallion + stroke-drawn checkmark when 100% complete
            if (allComplete && checkFraction > 0f) {
                val badgeRadius = ringRadius * 0.36f
                val badgeCenter = Offset(center.x, center.y - ringRadius * 0.25f)
                drawCircle(
                    color = EmeraldVerified.copy(alpha = 0.20f * checkFraction),
                    radius = badgeRadius * (0.85f + 0.15f * checkFraction),
                    center = badgeCenter,
                )

                val p1 = Offset(badgeCenter.x - badgeRadius * 0.42f, badgeCenter.y + badgeRadius * 0.02f)
                val p2 = Offset(badgeCenter.x - badgeRadius * 0.10f, badgeCenter.y + badgeRadius * 0.34f)
                val p3 = Offset(badgeCenter.x + badgeRadius * 0.46f, badgeCenter.y - badgeRadius * 0.30f)

                val path = Path().apply {
                    moveTo(p1.x, p1.y)
                    val firstSeg = (checkFraction / 0.45f).coerceIn(0f, 1f)
                    val curMid = Offset(
                        x = p1.x + (p2.x - p1.x) * firstSeg,
                        y = p1.y + (p2.y - p1.y) * firstSeg,
                    )
                    lineTo(curMid.x, curMid.y)
                    if (checkFraction > 0.45f) {
                        val secondSeg = ((checkFraction - 0.45f) / 0.55f).coerceIn(0f, 1f)
                        val curEnd = Offset(
                            x = p2.x + (p3.x - p2.x) * secondSeg,
                            y = p2.y + (p3.y - p2.y) * secondSeg,
                        )
                        lineTo(curEnd.x, curEnd.y)
                    }
                }
                drawPath(
                    path = path,
                    color = EmeraldVerified,
                    style = Stroke(
                        width = 4.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = if (allComplete) 42.dp else 0.dp),
        ) {
            Text(
                text = "$displayPercent%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (allComplete) EmeraldVerified else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("summary_completion_percentage"),
            )
            Text(
                text = "$completedTasks/$totalTasks CHECKS",
                style = MaterialTheme.typography.labelSmall,
                color = if (allComplete) CyanSignal else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianSurfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SummaryMilestoneRow(
    milestone: SetupMilestoneProgress,
    onClick: () -> Unit,
) {
    val accent = when (milestone.stepNumber) {
        1 -> CyanSignal
        2 -> AmberDebrid
        3 -> CyanSignal
        4 -> IndigoMeta
        else -> EmeraldVerified
    }
    val icon: ImageVector = when (milestone.stepNumber) {
        1 -> Icons.Filled.AccountCircle
        2 -> Icons.Filled.Key
        3 -> Icons.Filled.Hub
        4 -> Icons.Filled.AutoAwesomeMosaic
        else -> Icons.Filled.RocketLaunch
    }
    val animatedFraction by animateFloatAsState(
        targetValue = milestone.fraction,
        animationSpec = spring(stiffness = 240f),
        label = "summary_row_fraction_${milestone.stepNumber}",
    )

    Surface(
        color = ObsidianSurfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (milestone.isCompleted) EmeraldVerified.copy(alpha = 0.45f) else ObsidianOutline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("summary_milestone_row_${milestone.stepNumber}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        (if (milestone.isCompleted) EmeraldVerified else accent).copy(alpha = 0.16f)
                    ),
            ) {
                Icon(
                    imageVector = if (milestone.isCompleted) Icons.Filled.CheckCircle else icon,
                    contentDescription = if (milestone.isCompleted) "Step completed" else null,
                    tint = if (milestone.isCompleted) EmeraldVerified else accent,
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
                        text = "Step ${milestone.stepNumber} • ${milestone.title}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = "${(animatedFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (milestone.isCompleted) EmeraldVerified else accent,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = milestone.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = if (milestone.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (milestone.isCompleted) "Complete" else "Incomplete",
                tint = if (milestone.isCompleted) EmeraldVerified else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
