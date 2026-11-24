package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.VerificationState
import com.example.ui.theme.AmberDebrid
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IndigoMeta
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianVoid
import com.example.ui.viewmodel.ProgressSubTask
import com.example.ui.viewmodel.SetupMilestoneProgress
import com.example.ui.viewmodel.SetupUiState

private fun stepperAccent(stepNumber: Int): Color = when (stepNumber) {
    1 -> CyanSignal
    2 -> AmberDebrid
    3 -> CyanSignal
    4 -> IndigoMeta
    else -> EmeraldVerified
}

private fun stepperIcon(stepNumber: Int): ImageVector = when (stepNumber) {
    1 -> Icons.Filled.AccountCircle
    2 -> Icons.Filled.Key
    3 -> Icons.Filled.Hub
    4 -> Icons.Filled.AutoAwesomeMosaic
    else -> Icons.Filled.RocketLaunch
}

private fun stepperGuidanceCopy(stepNumber: Int): String = when (stepNumber) {
    1 -> "Create your Nuvio credentials and save them to this profile so every add-on and catalog syncs to the right setup."
    2 -> "Link your Real-Debrid Premium API token for instant cached 4K streams, then run live HTTPS verification for Real-Debrid and TMDB."
    3 -> "Enable your stream scrapers once inside AIOStreams so every module inherits your Real-Debrid key, filters, and formatter."
    4 -> "Select TMDB, TVDB, or Kitsu metadata providers, arrange your home catalogs & collections, and verify your AIOMetadata manifest."
    else -> "Install AIOMetadata #1 on top and AIOStreams #2 below inside Nuvio, then confirm playback order and export a backup."
}

/**
 * Interactive Vertical Configuration Stepper for the Central Dashboard.
 * Guides users step-by-step down a connected vertical timeline through Nuvio Account,
 * Real-Debrid & API verification, AIOStreams, AIOMetadata, and Deployment with inline
 * live diagnostics and Previous/Next step controls.
 */
@Composable
fun VerticalConfigurationStepper(
    state: SetupUiState,
    onNavigateToRoute: (String) -> Unit,
    onVerifyRealDebrid: (String) -> Unit,
    onVerifyTmdb: (String) -> Unit,
    onVerifyAioStreamsManifest: (String) -> Unit,
    onVerifyAioMetadataManifest: (String) -> Unit,
    onOpenSetupSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = state.progress
    val milestones = progress.milestones
    if (milestones.isEmpty()) return
    val allComplete = progress.completedSteps == progress.totalSteps && progress.totalSteps > 0

    var activeExpandedStep by rememberSaveable(progress.currentActiveStepNumber) {
        mutableIntStateOf(progress.currentActiveStepNumber)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vertical_stepper"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section Header + Focus Active Step Chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Guided Configuration Stepper",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = progress.nextPendingTask,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (progress.completedSteps == progress.totalSteps) EmeraldVerified
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (activeExpandedStep != progress.currentActiveStepNumber) {
                Surface(
                    onClick = { activeExpandedStep = progress.currentActiveStepNumber },
                    color = CyanSignal.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("vertical_stepper_focus_next"),
                ) {
                    Text(
                        text = "Focus Step ${progress.currentActiveStepNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanSignal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            } else {
                StatusPill(
                    text = "${progress.completedTasks}/${progress.totalTasks} CHECKS",
                    color = if (progress.completedTasks == progress.totalTasks) EmeraldVerified else CyanSignal,
                )
            }
        }

        // Vertical Connected Steps
        Column(modifier = Modifier.fillMaxWidth()) {
            milestones.forEachIndexed { index, milestone ->
                val isLast = index == milestones.lastIndex
                val isExpanded = activeExpandedStep == milestone.stepNumber
                val isCurrentTarget = milestone.stepNumber == progress.currentActiveStepNumber && !milestone.isCompleted
                val nextMilestoneCompleted = milestones.getOrNull(index + 1)?.isCompleted == true

                VerticalStepperRow(
                    state = state,
                    milestone = milestone,
                    isExpanded = isExpanded,
                    isCurrentTarget = isCurrentTarget,
                    isLast = isLast,
                    nextMilestoneCompleted = nextMilestoneCompleted,
                    canGoPrev = index > 0,
                    canGoNext = !isLast,
                    onToggleExpand = {
                        activeExpandedStep = if (isExpanded) -1 else milestone.stepNumber
                    },
                    onGoPrev = {
                        if (index > 0) activeExpandedStep = milestones[index - 1].stepNumber
                    },
                    onGoNext = {
                        if (!isLast) activeExpandedStep = milestones[index + 1].stepNumber
                    },
                    onNavigateToRoute = onNavigateToRoute,
                    onVerifyRealDebrid = onVerifyRealDebrid,
                    onVerifyTmdb = onVerifyTmdb,
                    onVerifyAioStreamsManifest = onVerifyAioStreamsManifest,
                    onVerifyAioMetadataManifest = onVerifyAioMetadataManifest,
                )
            }
        }

        // Stepper Completion Footer -> Launch Final Setup Summary Screen
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (allComplete) EmeraldVerified.copy(alpha = 0.12f) else ObsidianSurface,
            border = BorderStroke(
                width = if (allComplete) 1.5.dp else 1.dp,
                color = if (allComplete) EmeraldVerified.copy(alpha = 0.65f) else ObsidianOutline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vertical_stepper_summary_banner"),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (allComplete) "All 5 Steps Complete!" else "Final Setup Summary",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (allComplete) EmeraldVerified else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (allComplete) {
                            "100% configured — open your final Setup Summary & celebration."
                        } else {
                            "${progress.granularPercent}% complete (${progress.completedTasks}/${progress.totalTasks} checks). View scorecard anytime."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onOpenSetupSummary,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allComplete) EmeraldVerified else CyanSignal,
                        contentColor = ObsidianVoid,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("vertical_stepper_open_summary_button"),
                ) {
                    Text(if (allComplete) "View Summary" else "Summary", maxLines = 1)
                    Spacer(Modifier.width(4.dp))
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

@Composable
private fun VerticalStepperRow(
    state: SetupUiState,
    milestone: SetupMilestoneProgress,
    isExpanded: Boolean,
    isCurrentTarget: Boolean,
    isLast: Boolean,
    nextMilestoneCompleted: Boolean,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onToggleExpand: () -> Unit,
    onGoPrev: () -> Unit,
    onGoNext: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onVerifyRealDebrid: (String) -> Unit,
    onVerifyTmdb: (String) -> Unit,
    onVerifyAioStreamsManifest: (String) -> Unit,
    onVerifyAioMetadataManifest: (String) -> Unit,
) {
    val accent = stepperAccent(milestone.stepNumber)
    val nodeBackground by animateColorAsState(
        targetValue = when {
            milestone.isCompleted -> EmeraldVerified
            isCurrentTarget || isExpanded -> accent.copy(alpha = 0.22f)
            milestone.inProgress -> accent.copy(alpha = 0.14f)
            else -> ObsidianSurfaceContainer
        },
        label = "vstepper_node_bg_${milestone.stepNumber}",
    )
    val nodeBorder by animateColorAsState(
        targetValue = when {
            milestone.isCompleted -> EmeraldVerified
            isCurrentTarget || isExpanded -> accent
            milestone.inProgress -> accent.copy(alpha = 0.65f)
            else -> ObsidianOutline
        },
        label = "vstepper_node_border_${milestone.stepNumber}",
    )
    val animatedStepFraction by animateFloatAsState(
        targetValue = milestone.fraction,
        animationSpec = spring(stiffness = 240f),
        label = "vstepper_fraction_${milestone.stepNumber}",
    )

    val statusLabel = when {
        milestone.isCompleted -> "VERIFIED"
        isCurrentTarget -> "ACTIVE STEP"
        milestone.inProgress -> "IN PROGRESS"
        else -> "UPCOMING"
    }
    val statusColor = when {
        milestone.isCompleted -> EmeraldVerified
        isCurrentTarget || milestone.inProgress -> accent
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .testTag("vertical_stepper_step_${milestone.stepNumber}"),
    ) {
        // Left Vertical Timeline Spine (Node + Connector Line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(onClick = onToggleExpand)
                    .testTag("vertical_stepper_node_${milestone.stepNumber}"),
            ) {
                // Outer pulse ring for active step
                if (isCurrentTarget || isExpanded) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background((if (milestone.isCompleted) EmeraldVerified else accent).copy(alpha = 0.12f))
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(nodeBackground)
                        .border(
                            width = if (isCurrentTarget || isExpanded) 2.dp else 1.dp,
                            color = nodeBorder,
                            shape = CircleShape,
                        ),
                ) {
                    if (milestone.isCompleted) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Step ${milestone.stepNumber} completed",
                            tint = ObsidianVoid,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            text = milestone.stepNumber.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentTarget || isExpanded || milestone.inProgress) accent
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .weight(1f)
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            when {
                                milestone.isCompleted && nextMilestoneCompleted ->
                                    Brush.verticalGradient(listOf(EmeraldVerified, EmeraldVerified))
                                milestone.isCompleted ->
                                    Brush.verticalGradient(listOf(EmeraldVerified, accent.copy(alpha = 0.45f)))
                                isCurrentTarget || isExpanded ->
                                    Brush.verticalGradient(listOf(accent.copy(alpha = 0.7f), ObsidianOutline))
                                else ->
                                    Brush.verticalGradient(listOf(ObsidianOutline, ObsidianOutline))
                            }
                        )
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Right Interactive Step Content Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isExpanded) ObsidianSurface else ObsidianSurface.copy(alpha = 0.88f),
            border = BorderStroke(
                width = if (isCurrentTarget || isExpanded) 1.5.dp else 1.dp,
                color = when {
                    milestone.isCompleted -> EmeraldVerified.copy(alpha = if (isExpanded) 0.65f else 0.38f)
                    isCurrentTarget || isExpanded -> accent.copy(alpha = 0.72f)
                    else -> ObsidianOutline
                },
            ),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Step Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleExpand)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("vertical_stepper_header_${milestone.stepNumber}"),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background((if (milestone.isCompleted) EmeraldVerified else accent).copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = stepperIcon(milestone.stepNumber),
                            contentDescription = null,
                            tint = if (milestone.isCompleted) EmeraldVerified else accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            StatusPill(text = statusLabel, color = statusColor)
                            Text(
                                text = "${milestone.completedRequiredCount}/${milestone.totalRequiredCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = milestone.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = milestone.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isExpanded) 3 else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.cd_expand_stepper_step),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Thin Per-Step Completion Progress Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = if (isExpanded) 10.dp else 12.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(ObsidianOutline.copy(alpha = 0.55f)),
                ) {
                    if (animatedStepFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedStepFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (milestone.isCompleted) EmeraldVerified else accent)
                        )
                    }
                }

                // Expanded Guided Task Body
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianVoid.copy(alpha = 0.52f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Concise Task Guidance Note
                        Text(
                            text = stepperGuidanceCopy(milestone.stepNumber),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Sub-Task Checklist
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            milestone.subTasks.forEach { task ->
                                StepperSubTaskRow(task = task)
                            }
                        }

                        // Step-Specific Inline Verification & Action Panel
                        StepperInlineActionPanel(
                            state = state,
                            milestone = milestone,
                            accent = accent,
                            onNavigateToRoute = onNavigateToRoute,
                            onVerifyRealDebrid = onVerifyRealDebrid,
                            onVerifyTmdb = onVerifyTmdb,
                            onVerifyAioStreamsManifest = onVerifyAioStreamsManifest,
                            onVerifyAioMetadataManifest = onVerifyAioMetadataManifest,
                        )

                        // Stepper Back / Next Navigation Bar
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (canGoPrev) {
                                TextButton(
                                    onClick = onGoPrev,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("stepper_prev_${milestone.stepNumber}"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Previous")
                                }
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }

                            if (canGoNext) {
                                TextButton(
                                    onClick = onGoNext,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("stepper_next_${milestone.stepNumber}"),
                                ) {
                                    Text("Next Step", color = CyanSignal)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = CyanSignal,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperInlineActionPanel(
    state: SetupUiState,
    milestone: SetupMilestoneProgress,
    accent: Color,
    onNavigateToRoute: (String) -> Unit,
    onVerifyRealDebrid: (String) -> Unit,
    onVerifyTmdb: (String) -> Unit,
    onVerifyAioStreamsManifest: (String) -> Unit,
    onVerifyAioMetadataManifest: (String) -> Unit,
) {
    val profile = state.activeProfile

    when (milestone.stepNumber) {
        2 -> {
            // Real-Debrid Setup & Live API Verification inline controls
            val rdKey = profile?.realDebridApiKey.orEmpty()
            val tmdbKey = profile?.tmdbApiKey.orEmpty()

            if (state.rdVerification !is VerificationState.Idle || profile?.realDebridVerified == true) {
                VerificationBanner(
                    state = state.rdVerification,
                    persistedVerifiedLabel = if (profile?.realDebridVerified == true) {
                        "Real-Debrid verified • ${profile.realDebridUserLabel} (${profile.realDebridPremiumDays}d premium)"
                    } else null,
                )
            }
            if (state.tmdbVerification !is VerificationState.Idle || profile?.tmdbVerified == true) {
                VerificationBanner(
                    state = state.tmdbVerification,
                    persistedVerifiedLabel = if (profile?.tmdbVerified == true) {
                        "TMDB API key verified"
                    } else null,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = {
                        if (rdKey.isNotBlank()) onVerifyRealDebrid(rdKey)
                        else onNavigateToRoute(milestone.route)
                    },
                    enabled = state.rdVerification !is VerificationState.Checking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberDebrid,
                        contentColor = ObsidianVoid,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("stepper_verify_rd_button"),
                ) {
                    if (state.rdVerification is VerificationState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = ObsidianVoid,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (rdKey.isNotBlank()) "Verify Real-Debrid" else "Set Up Real-Debrid",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (tmdbKey.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onVerifyTmdb(tmdbKey) },
                        enabled = state.tmdbVerification !is VerificationState.Checking,
                        border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("stepper_verify_tmdb_button"),
                    ) {
                        Text("Verify TMDB", color = CyanSignal, maxLines = 1)
                    }
                }
            }

            if (rdKey.isNotBlank()) {
                OutlinedButton(
                    onClick = { onNavigateToRoute(milestone.route) },
                    border = BorderStroke(1.dp, ObsidianOutline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("roadmap_step_2_open_button"),
                ) {
                    Text("Open API Keys Vault")
                }
            }
        }

        3 -> {
            // AIOStreams Hub + Inline Manifest Ping
            val manifestUrl = profile?.aioStreamsManifestUrl.orEmpty()
            if (state.aioStreamsManifestVerification !is VerificationState.Idle || profile?.aioStreamsVerified == true) {
                VerificationBanner(
                    state = state.aioStreamsManifestVerification,
                    persistedVerifiedLabel = if (profile?.aioStreamsVerified == true) {
                        "AIOStreams manifest reachable over HTTPS"
                    } else null,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { onNavigateToRoute(milestone.route) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("roadmap_step_3_open_button"),
                ) {
                    Text(
                        text = if (milestone.isCompleted) "Review AIOStreams" else "Configure AIOStreams",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (manifestUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onVerifyAioStreamsManifest(manifestUrl) },
                        enabled = state.aioStreamsManifestVerification !is VerificationState.Checking,
                        border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("stepper_ping_streams_button"),
                    ) {
                        Text("Verify URL", color = EmeraldVerified, maxLines = 1)
                    }
                }
            }
        }

        4 -> {
            // AIOMetadata + Inline Manifest Ping
            val manifestUrl = profile?.aioMetadataManifestUrl.orEmpty()
            if (state.aioMetaManifestVerification !is VerificationState.Idle || profile?.aioMetadataVerified == true) {
                VerificationBanner(
                    state = state.aioMetaManifestVerification,
                    persistedVerifiedLabel = if (profile?.aioMetadataVerified == true) {
                        "AIOMetadata manifest reachable over HTTPS"
                    } else null,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { onNavigateToRoute(milestone.route) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoMeta,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("roadmap_step_4_open_button"),
                ) {
                    Text(
                        text = if (milestone.isCompleted) "Review Catalogs" else "Build Catalogs",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (manifestUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onVerifyAioMetadataManifest(manifestUrl) },
                        enabled = state.aioMetaManifestVerification !is VerificationState.Checking,
                        border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("stepper_ping_meta_button"),
                    ) {
                        Text("Verify URL", color = EmeraldVerified, maxLines = 1)
                    }
                }
            }
        }

        else -> {
            // Step 1 (Account) & Step 5 (Deploy)
            Button(
                onClick = { onNavigateToRoute(milestone.route) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (milestone.isCompleted) EmeraldVerified else accent,
                    contentColor = ObsidianVoid,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("roadmap_step_${milestone.stepNumber}_open_button"),
            ) {
                Text(
                    text = when {
                        milestone.stepNumber == 1 && milestone.isCompleted -> "Review Nuvio Account"
                        milestone.stepNumber == 1 -> "Configure Nuvio Account"
                        milestone.isCompleted -> "Review Deployment & Export"
                        else -> "Open Deploy Checklist"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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

@Composable
private fun StepperSubTaskRow(
    task: ProgressSubTask,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stepper_subtask_${task.id}"),
    ) {
        Icon(
            imageVector = if (task.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (task.completed) "Completed" else "Pending",
            tint = when {
                task.completed -> EmeraldVerified
                !task.required -> IndigoMeta.copy(alpha = 0.75f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = task.label,
            style = MaterialTheme.typography.bodySmall,
            color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
        )
        if (!task.required) {
            Spacer(Modifier.width(6.dp))
            StatusPill(
                text = "OPTIONAL",
                color = if (task.completed) EmeraldVerified else IndigoMeta,
            )
        }
    }
}
