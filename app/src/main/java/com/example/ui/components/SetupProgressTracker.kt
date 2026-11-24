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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
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
import com.example.ui.viewmodel.SetupProgressSummary

private fun milestoneAccent(stepNumber: Int): Color = when (stepNumber) {
    1 -> CyanSignal
    2 -> AmberDebrid
    3 -> CyanSignal
    4 -> IndigoMeta
    else -> EmeraldVerified
}

private fun milestoneIcon(stepNumber: Int): ImageVector = when (stepNumber) {
    1 -> Icons.Filled.AccountCircle
    2 -> Icons.Filled.Key
    3 -> Icons.Filled.Hub
    4 -> Icons.Filled.AutoAwesomeMosaic
    else -> Icons.Filled.RocketLaunch
}

/**
 * Persistent top progress rail rendered below the TopAppBar across all setup screens.
 * Displays an interactive 5-step stepper timeline, smooth granular progress bar, and an
 * expandable live checklist preview so users always know their setup state.
 */
@Composable
fun PersistentSetupProgressRail(
    progress: SetupProgressSummary,
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (progress.milestones.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val animatedFraction by animateFloatAsState(
        targetValue = progress.granularFraction,
        animationSpec = spring(stiffness = 260f),
        label = "rail_progress_fraction",
    )

    val activeMilestone = progress.milestones.firstOrNull { !it.isCompleted }
        ?: progress.milestones.last()

    Surface(
        color = ObsidianSurface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("persistent_progress_tracker"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top compact telemetry header + interactive 5-node stepper
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Interactive 5-node stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        progress.milestones.forEachIndexed { idx, milestone ->
                            val isCurrentTarget = milestone.stepNumber == progress.currentActiveStepNumber && !milestone.isCompleted
                            val isOnScreen = currentRoute == milestone.route && (
                                milestone.stepNumber == progress.currentActiveStepNumber ||
                                    milestone.stepNumber >= 3 ||
                                    (milestone.stepNumber == 1 && !milestone.isCompleted) ||
                                    (milestone.stepNumber == 2 && progress.nuvioDone)
                                )
                            val accent = milestoneAccent(milestone.stepNumber)
                            val nodeBg by animateColorAsState(
                                targetValue = when {
                                    milestone.isCompleted -> EmeraldVerified
                                    isCurrentTarget || isOnScreen -> accent.copy(alpha = 0.22f)
                                    milestone.inProgress -> accent.copy(alpha = 0.14f)
                                    else -> ObsidianSurfaceContainer
                                },
                                label = "node_bg_${milestone.stepNumber}",
                            )
                            val nodeBorder by animateColorAsState(
                                targetValue = when {
                                    milestone.isCompleted -> EmeraldVerified
                                    isCurrentTarget || isOnScreen -> accent
                                    milestone.inProgress -> accent.copy(alpha = 0.65f)
                                    else -> ObsidianOutline
                                },
                                label = "node_border_${milestone.stepNumber}",
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onNavigateToRoute(milestone.route) }
                                    .testTag("tracker_step_node_${milestone.stepNumber}"),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(nodeBg)
                                            .border(
                                                width = if (isCurrentTarget || isOnScreen) 1.5.dp else 1.dp,
                                                color = nodeBorder,
                                                shape = CircleShape,
                                            ),
                                    ) {
                                        if (milestone.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "${milestone.shortLabel} complete",
                                                tint = ObsidianVoid,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        } else {
                                            Text(
                                                text = milestone.stepNumber.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentTarget || isOnScreen || milestone.inProgress) accent
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = milestone.shortLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            milestone.isCompleted -> EmeraldVerified
                                            isCurrentTarget || isOnScreen -> accent
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                    )
                                }
                            }

                            if (idx < progress.milestones.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .padding(bottom = 12.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (milestone.isCompleted) EmeraldVerified.copy(alpha = 0.75f)
                                            else ObsidianOutline
                                        )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Compact percentage + expand toggle
                    Surface(
                        onClick = { expanded = !expanded },
                        color = ObsidianSurfaceContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (progress.granularPercent == 100) EmeraldVerified.copy(alpha = 0.5f)
                            else CyanSignal.copy(alpha = 0.35f),
                        ),
                        modifier = Modifier.testTag("progress_tracker_expand_button"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "${progress.granularPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (progress.granularPercent == 100) EmeraldVerified else CyanSignal,
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = stringResource(R.string.cd_toggle_progress_drawer),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Smooth animated granular progress track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(ObsidianOutline.copy(alpha = 0.7f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFraction.coerceAtLeast(0.02f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(CyanSignal, IndigoMeta, EmeraldVerified)
                                )
                            )
                    )
                }
            }

            // Expandable Live Checklist Drawer
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Surface(
                    color = ObsidianVoid.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_tracker_drawer"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (progress.completedSteps == progress.totalSteps) {
                                        "Setup Complete"
                                    } else {
                                        "Next: Step ${activeMilestone.stepNumber} • ${activeMilestone.title}"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${progress.completedTasks} of ${progress.totalTasks} required checks complete • ${progress.completedSteps}/${progress.totalSteps} milestones",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (currentRoute != activeMilestone.route) {
                                OutlinedButton(
                                    onClick = {
                                        expanded = false
                                        onNavigateToRoute(activeMilestone.route)
                                    },
                                    border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("tracker_drawer_jump_button"),
                                ) {
                                    Text("Open", color = CyanSignal)
                                }
                            }
                        }

                        activeMilestone.subTasks.forEach { task ->
                            SubTaskChecklistRow(task = task)
                        }
                    }
                }
            }

            HorizontalDivider(color = ObsidianOutline.copy(alpha = 0.65f))
        }
    }
}

/**
 * Full interactive Milestone & Sub-Task Progress Tracker for the Central Dashboard.
 * Connects all 5 setup milestones along a vertical timeline rail with expandable sub-checklists.
 */
@Composable
fun MilestoneProgressTrackerSection(
    progress: SetupProgressSummary,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedStep by remember(progress.currentActiveStepNumber) {
        mutableStateOf(progress.currentActiveStepNumber)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("milestone_progress_tracker_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section header + live granular summary pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Setup Progress Tracker",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
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
            StatusPill(
                text = "${progress.completedTasks}/${progress.totalTasks} CHECKS",
                color = if (progress.completedTasks == progress.totalTasks) EmeraldVerified else CyanSignal,
            )
        }

        // Milestone cards with vertical timeline indicators
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            progress.milestones.forEach { milestone ->
                val isExpanded = expandedStep == milestone.stepNumber
                val isCurrentActive = milestone.stepNumber == progress.currentActiveStepNumber && !milestone.isCompleted
                MilestoneTrackerCard(
                    milestone = milestone,
                    isExpanded = isExpanded,
                    isCurrentActive = isCurrentActive,
                    onToggleExpand = {
                        expandedStep = if (isExpanded) -1 else milestone.stepNumber
                    },
                    onOpenStep = { onNavigateToRoute(milestone.route) },
                )
            }
        }
    }
}

@Composable
private fun MilestoneTrackerCard(
    milestone: SetupMilestoneProgress,
    isExpanded: Boolean,
    isCurrentActive: Boolean,
    onToggleExpand: () -> Unit,
    onOpenStep: () -> Unit,
) {
    val accent = milestoneAccent(milestone.stepNumber)
    val statusColor = when {
        milestone.isCompleted -> EmeraldVerified
        isCurrentActive || milestone.inProgress -> accent
        else -> ObsidianOutline
    }
    val statusLabel = when {
        milestone.isCompleted -> "DONE"
        isCurrentActive -> "NEXT"
        milestone.inProgress -> "IN PROGRESS"
        else -> "PENDING"
    }
    val animatedStepFraction by animateFloatAsState(
        targetValue = milestone.fraction,
        animationSpec = spring(stiffness = 240f),
        label = "milestone_fraction_${milestone.stepNumber}",
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(
            width = if (isCurrentActive) 1.5.dp else 1.dp,
            color = when {
                milestone.isCompleted -> EmeraldVerified.copy(alpha = 0.45f)
                isCurrentActive -> accent.copy(alpha = 0.7f)
                else -> ObsidianOutline
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("roadmap_step_${milestone.stepNumber}"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Header (tappable to expand/collapse checklist)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (milestone.isCompleted) EmeraldVerified.copy(alpha = 0.16f)
                            else accent.copy(alpha = 0.14f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (milestone.isCompleted) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Step ${milestone.stepNumber} completed",
                            tint = EmeraldVerified,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Icon(
                            imageVector = milestoneIcon(milestone.stepNumber),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "STEP ${milestone.stepNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (milestone.isCompleted) EmeraldVerified else accent,
                        )
                        StatusPill(
                            text = statusLabel,
                            color = if (statusColor == ObsidianOutline) MaterialTheme.colorScheme.onSurfaceVariant else statusColor,
                        )
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
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = milestone.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(8.dp))
                    // Per-step mini progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(ObsidianOutline.copy(alpha = 0.6f)),
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
                }

                Spacer(Modifier.width(6.dp))

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("roadmap_step_${milestone.stepNumber}_toggle"),
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.cd_expand_milestone),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Expandable Granular Sub-Task Checklist + Action CTA
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianVoid.copy(alpha = 0.55f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    milestone.subTasks.forEach { task ->
                        SubTaskChecklistRow(task = task)
                    }

                    Spacer(Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = onOpenStep,
                        border = BorderStroke(
                            1.dp,
                            (if (milestone.isCompleted) EmeraldVerified else accent).copy(alpha = 0.6f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("roadmap_step_${milestone.stepNumber}_open_button"),
                    ) {
                        Text(
                            text = if (milestone.isCompleted) "Review Step ${milestone.stepNumber}" else "Configure Step ${milestone.stepNumber}",
                            color = if (milestone.isCompleted) EmeraldVerified else accent,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (milestone.isCompleted) EmeraldVerified else accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubTaskChecklistRow(
    task: ProgressSubTask,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("progress_subtask_${task.id}"),
    ) {
        Icon(
            imageVector = if (task.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (task.completed) "Completed" else "Pending",
            tint = when {
                task.completed -> EmeraldVerified
                !task.required -> IndigoMeta.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
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
