package com.example.ui.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SectionType
import com.example.ui.feature.practice.sectionrenderers.RenderSectionContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel,
    onExitPractice: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showSectionJumpSheet by remember { mutableStateOf(false) }
    var markTopicAsCompletedChoice by remember { mutableStateOf(true) }

    LaunchedEffect(state.sessionFinished) {
        if (state.sessionFinished) {
            onExitPractice()
        }
    }

    BackHandler {
        if (state.isTimerRunning) {
            showExitConfirmDialog = true
        } else {
            onExitPractice()
        }
    }

    // Exit Confirmation Dialog
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("Pause Practice Session?") },
            text = { Text("Your section review states and progress are automatically saved. You can resume anytime from the dashboard.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.pauseTimer()
                        showExitConfirmDialog = false
                        onExitPractice()
                    }
                ) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitConfirmDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }

    // Finish Session Dialog
    if (state.showFinishDialog) {
        val checkedCriteriaCount = state.checklistState.count { it.value }

        AlertDialog(
            onDismissRequest = { viewModel.dismissFinishDialog() },
            title = { Text("Finish Practice Session") },
            text = {
                Column {
                    Text(
                        text = "Great writing session! Check off your evaluation below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Duration:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${state.elapsedSessionSeconds / 60}m ${state.elapsedSessionSeconds % 60}s",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sections Reviewed:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${state.reviewedSectionIds.size} of ${state.sections.size}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Checklist Criteria:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "$checkedCriteriaCount of 5 criteria met",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { markTopicAsCompletedChoice = !markTopicAsCompletedChoice }
                    ) {
                        Checkbox(
                            checked = markTopicAsCompletedChoice,
                            onCheckedChange = { markTopicAsCompletedChoice = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mark topic as Completed in library",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.completeSession(markTopicAsCompletedChoice) },
                    modifier = Modifier.testTag("confirm_finish_session_button")
                ) {
                    Text("Save & Complete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissFinishDialog() }) {
                    Text("Keep Practicing")
                }
            }
        )
    }

    // Free Navigation Bottom Sheet
    if (showSectionJumpSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSectionJumpSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Jump to Section",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                state.sections.forEachIndexed { index, s ->
                    val isCurrent = s.sectionId == state.currentSectionId
                    val isReviewed = state.reviewedSectionIds.contains(s.sectionId)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.onSectionSelected(index)
                                showSectionJumpSheet = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (isReviewed) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Reviewed",
                                    tint = Color(0xFF166534),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    val currentSection = state.sections.getOrNull(state.currentSectionIndex)
    val parsedSection = currentSection?.let { state.parsedSections[it.sectionId] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.topic?.title ?: "Guided Practice",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (state.sections.isNotEmpty())
                                "Section ${state.currentSectionIndex + 1} of ${state.sections.size}: ${currentSection?.title ?: ""}"
                            else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.isTimerRunning) showExitConfirmDialog = true
                            else onExitPractice()
                        },
                        modifier = Modifier.testTag("practice_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit practice"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleSectionBookmark() },
                        modifier = Modifier.testTag("practice_bookmark_button")
                    ) {
                        val isBookmarked = state.bookmarkedSectionIds.contains(state.currentSectionId)
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark section",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showSectionJumpSheet = true },
                        modifier = Modifier.testTag("practice_section_list_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "All Sections",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { viewModel.openFinishDialog() },
                        modifier = Modifier.testTag("practice_finish_button")
                    ) {
                        Text("Finish", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            PracticeBottomBar(
                hasPrevious = state.currentSectionIndex > 0,
                hasNext = state.currentSectionIndex < state.sections.size - 1,
                isReviewed = state.reviewedSectionIds.contains(state.currentSectionId),
                onPrevious = { viewModel.onPreviousSection() },
                onToggleReviewed = { viewModel.toggleSectionReviewed() },
                onNext = { viewModel.onNextSection() },
                onFinish = { viewModel.openFinishDialog() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Overall Section Progress Indicator
            val progressFraction = if (state.sections.isEmpty()) 0f
            else ((state.currentSectionIndex + 1).toFloat() / state.sections.size).coerceIn(0f, 1f)

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Timer Module Card
            TimerModuleCard(
                timerMode = state.timerMode,
                remainingSeconds = state.remainingSeconds,
                isRunning = state.isTimerRunning,
                isOvertime = state.isOvertime,
                onModeSelect = { viewModel.setTimerMode(it) },
                onStartPause = {
                    if (state.isTimerRunning) viewModel.pauseTimer()
                    else viewModel.startOrResumeTimer()
                },
                onReset = { viewModel.resetTimer() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Section Type Badge & Title Header
            if (currentSection != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = SectionType.fromKey(currentSection.type).label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    if (state.reviewedSectionIds.contains(currentSection.sectionId)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFF166534),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Reviewed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF166534)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentSection.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Main Section Content Renderer
            if (parsedSection != null) {
                RenderSectionContent(
                    section = parsedSection,
                    checklistState = state.checklistState,
                    onChecklistToggle = { viewModel.toggleChecklistItem(it) }
                )
            } else {
                Text(
                    text = "Section content is loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TimerModuleCard(
    timerMode: TimerMode,
    remainingSeconds: Long,
    isRunning: Boolean,
    isOvertime: Boolean,
    onModeSelect: (TimerMode) -> Unit,
    onStartPause: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timer_module_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (isOvertime) Color(0xFFFEF2F2)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Mode Select Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerMode.entries.forEach { mode ->
                    val isSelected = mode == timerMode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelect(mode) },
                        label = { Text(mode.label, fontSize = 11.sp) },
                        modifier = Modifier.height(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = if (isOvertime) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    val formatted = "%02d:%02d".format(mins, secs)

                    Text(
                        text = if (isOvertime) "+$formatted" else formatted,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 24.sp
                        ),
                        color = if (isOvertime) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                    )

                    if (isOvertime) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEE2E2), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OVERTIME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("timer_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset timer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onStartPause,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("timer_start_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRunning) "Pause" else "Start", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeBottomBar(
    hasPrevious: Boolean,
    hasNext: Boolean,
    isReviewed: Boolean,
    onPrevious: () -> Unit,
    onToggleReviewed: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            OutlinedButton(
                onClick = onPrevious,
                enabled = hasPrevious,
                modifier = Modifier.testTag("practice_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Prev")
            }

            // Reviewed Toggle
            OutlinedButton(
                onClick = onToggleReviewed,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isReviewed) Color(0xFFDCFCE7) else Color.Transparent
                ),
                modifier = Modifier.testTag("practice_mark_reviewed_button")
            ) {
                Icon(
                    imageVector = if (isReviewed) Icons.Filled.CheckCircle else Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (isReviewed) Color(0xFF166534) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isReviewed) "Reviewed" else "Mark Done",
                    color = if (isReviewed) Color(0xFF166534) else MaterialTheme.colorScheme.primary
                )
            }

            // Next or Finish
            if (hasNext) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.testTag("practice_next_button")
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Button(
                    onClick = onFinish,
                    modifier = Modifier.testTag("practice_last_finish_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Finish")
                }
            }
        }
    }
}
