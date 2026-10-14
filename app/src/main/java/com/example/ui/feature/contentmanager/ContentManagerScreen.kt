package com.example.ui.feature.contentmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.data.repository.ConflictResolution
import com.example.data.validation.ParsedContentType
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentManagerScreen(
    viewModel: ContentManagerViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var topicToDelete by remember { mutableStateOf<String?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissUserMessage()
        }
    }

    // Storage Access Framework JSON file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                    val content = reader.readText()
                    viewModel.onJsonInputChanged(content)
                    Toast.makeText(context, "Loaded JSON file successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Export Dialog
    if (state.exportedJson != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportedJson() },
            title = { Text(state.exportTitle ?: "Exported JSON") },
            text = {
                Column {
                    Text(
                        text = "UTF-8 JSON output. You can copy to clipboard or share directly to local files / apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = state.exportedJson!!.take(600) + if (state.exportedJson!!.length > 600) "\n..." else "",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Exported JSON", state.exportedJson))
                        Toast.makeText(context, "Copied JSON to clipboard", Toast.LENGTH_SHORT).show()
                        viewModel.clearExportedJson()
                    }
                ) {
                    Text("Copy")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, state.exportedJson)
                            type = "application/json"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share JSON"))
                        viewModel.clearExportedJson()
                    }
                ) {
                    Text("Share")
                }
            }
        )
    }

    // Delete single topic dialog
    if (topicToDelete != null) {
        AlertDialog(
            onDismissRequest = { topicToDelete = null },
            title = { Text("Delete Topic?") },
            text = { Text("Are you sure you want to delete topic '$topicToDelete'? All its sections and history will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTopic(topicToDelete!!)
                        topicToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { topicToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear all data dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Delete All Topics & Data?") },
            text = { Text("This will remove all installed topic packs and reset all local progress. Make sure you have exported a full backup first if you want to keep your data.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearAllConfirm = false
                    }
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Content Manager",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Import") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Backup & Export") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Library (${state.existingTopics.size})") }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> ImportTabContent(
                        state = state,
                        onJsonChange = { viewModel.onJsonInputChanged(it) },
                        onPickFile = { filePickerLauncher.launch("application/json") },
                        onImportDemo = { viewModel.importDemoPack() },
                        onConflictChange = { viewModel.setConflictResolution(it) },
                        onImportNow = { viewModel.importParsedContent() },
                        onClearInput = { viewModel.clearInput() }
                    )
                    1 -> ExportTabContent(
                        onExportLibrary = { viewModel.exportCompleteLibrary() },
                        onExportBackup = { viewModel.exportFullBackup() }
                    )
                    2 -> ManageLibraryTabContent(
                        existingTopics = state.existingTopics,
                        onDeleteTopic = { topicToDelete = it },
                        onClearAll = { showClearAllConfirm = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportTabContent(
    state: ContentManagerUiState,
    onJsonChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onImportDemo: () -> Unit,
    onConflictChange: (ConflictResolution) -> Unit,
    onImportNow: () -> Unit,
    onClearInput: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Quick Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onPickFile,
                modifier = Modifier
                    .weight(1f)
                    .testTag("import_file_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select .json")
            }
            OutlinedButton(
                onClick = onImportDemo,
                modifier = Modifier
                    .weight(1f)
                    .testTag("import_demo_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Load Demo")
            }
        }

        // Paste JSON Input Field
        OutlinedTextField(
            value = state.rawJsonInput,
            onValueChange = onJsonChange,
            label = { Text("Paste Topic Pack or Full Backup JSON") },
            placeholder = { Text("{\n  \"schemaVersion\": 1,\n  \"topicId\": \"...\"\n}") },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag("paste_json_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (state.rawJsonInput.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClearInput) {
                    Text("Clear Text", fontSize = 12.sp)
                }
            }
        }

        // Real-Time Pre-Import Validation Box
        val vResult = state.validationResult
        if (vResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (vResult.isValid) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                ),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (vResult.isValid) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            contentDescription = null,
                            tint = if (vResult.isValid) Color(0xFF166534) else Color(0xFF991B1B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (vResult.isValid) {
                                when (state.parsedContentType) {
                                    ParsedContentType.FULL_BACKUP -> "Valid Full Backup: ${vResult.topicCount} topic(s)"
                                    ParsedContentType.TOPIC_PACK_ARRAY -> "Valid Batch: ${vResult.topicCount} topics, ${vResult.sectionCount} sections"
                                    else -> "Valid Topic Pack: ${vResult.topicCount} topic, ${vResult.sectionCount} sections"
                                }
                            } else "Validation Failed",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (vResult.isValid) Color(0xFF166534) else Color(0xFF991B1B)
                        )
                    }

                    // Fatal Errors
                    if (vResult.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        vResult.errors.forEach { err ->
                            Text(
                                text = "• $err",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }

                    // Non-Fatal Warnings
                    if (vResult.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Warnings (${vResult.warnings.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            )
                        }
                        vResult.warnings.forEach { warn ->
                            Text(
                                text = "• $warn",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }

            // Conflict Resolution Selector (if single or array)
            if (vResult.isValid && state.parsedContentType != ParsedContentType.FULL_BACKUP) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "If topic ID already exists:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        ConflictResolution.entries.forEach { res ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onConflictChange(res) }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = state.conflictResolution == res,
                                    onClick = { onConflictChange(res) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (res) {
                                        ConflictResolution.UPDATE -> "Update content (preserve existing practice history)"
                                        ConflictResolution.SKIP -> "Skip existing topics"
                                        ConflictResolution.COPY -> "Create as separate copy with new ID"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Preview Parsed Topics
            if (state.parsedTopics.isNotEmpty()) {
                Text(
                    text = "Topics to Import (${state.parsedTopics.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                state.parsedTopics.take(3).forEach { t ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = t.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (!t.titleBn.isNullOrBlank()) {
                                Text(
                                    text = t.titleBn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Category: ${t.category} • ${t.sections.size} sections • Est. ${t.estimatedMinutes} min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Confirm Import Button
            if (vResult.isValid) {
                Button(
                    onClick = onImportNow,
                    enabled = !state.isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_import_button")
                ) {
                    if (state.isImporting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (state.parsedContentType == ParsedContentType.FULL_BACKUP) "Restore Full Backup"
                            else "Confirm & Import Content"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportTabContent(
    onExportLibrary: () -> Unit,
    onExportBackup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Export Topic Packs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Topic Packs Library (Content Only)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Exports all installed topics and sections as a clean JSON array. This file contains educational content only and does not contain personal practice history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onExportLibrary,
                    modifier = Modifier.testTag("export_library_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Topic Library")
                }
            }
        }

        // Full Backup Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Complete Backup (Content & History)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Exports everything: topics, sections, bookmarks, completed sessions, self-review answers, and elapsed time records. Use this to transfer data between devices or create a safety backup before uninstalling.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onExportBackup,
                    modifier = Modifier.testTag("export_full_backup_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Full Backup")
                }
            }
        }

        // Share App APK with Friends
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
        val context = androidx.compose.ui.platform.LocalContext.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("share_apk_manage_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Share App APK (Offline Installer)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Share the MicroWrite Android APK directly with friends and study partners via Bluetooth, Nearby Share / Quick Share, WhatsApp, or Telegram. They can install and use it 100% offline without needing Google Play.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            com.example.util.ApkShareHelper.shareAppApk(context)
                        }
                    },
                    modifier = Modifier.testTag("export_share_apk_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share APK with Friends")
                }
            }
        }
    }
}

@Composable
private fun ManageLibraryTabContent(
    existingTopics: List<com.example.data.local.entity.TopicEntity>,
    onDeleteTopic: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Installed Topics (${existingTopics.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (existingTopics.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_all_data_button")
                ) {
                    Text("Clear All", color = Color(0xFFDC2626))
                }
            }
        }

        if (existingTopics.isEmpty()) {
            Text(
                text = "No topics are currently installed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            existingTopics.forEach { topic ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topic.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ID: ${topic.topicId} • Rev ${topic.revision} • ${topic.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onDeleteTopic(topic.topicId) },
                            modifier = Modifier.testTag("delete_topic_${topic.topicId}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete topic",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
