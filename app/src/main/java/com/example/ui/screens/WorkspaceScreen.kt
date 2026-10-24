package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AskAiResult
import com.example.ui.StudyMateViewModel
import com.example.ui.theme.BrandAmberHighlight
import com.example.ui.theme.BrandEmeraldCorrect
import com.example.ui.theme.BrandIndigoPrimary
import com.example.ui.theme.BrandRoseIncorrect

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(
    viewModel: StudyMateViewModel,
    onNavigateToQuiz: () -> Unit,
    onNavigateToFlashcards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val workspaceState by viewModel.workspaceState.collectAsState()
    val folders by viewModel.foldersFlow.collectAsState()
    val authState by viewModel.authUserState.collectAsState()

    var showSampleMenu by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }

    val hasNotes = workspaceState.notesText.isNotBlank()
    val wordCount = if (hasNotes) workspaceState.notesText.trim().split(Regex("\\s+")).size else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "StudyMate AI",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "StudyMate AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (authState.isLoggedIn) "Student: ${authState.username}" else "College Study Assistant (Guest)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Sample Notes Dropdown Button
                    Box {
                        OutlinedButton(
                            onClick = { showSampleMenu = true },
                            modifier = Modifier.testTag("sample_notes_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sample Notes", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        DropdownMenu(
                            expanded = showSampleMenu,
                            onDismissRequest = { showSampleMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🧬 Biology: Cellular Respiration & ATP") },
                                onClick = {
                                    showSampleMenu = false
                                    viewModel.loadSampleNotes(0)
                                    Toast.makeText(context, "Loaded Biology Sample Notes", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📈 Economics: Inflation & Monetary Policy") },
                                onClick = {
                                    showSampleMenu = false
                                    viewModel.loadSampleNotes(1)
                                    Toast.makeText(context, "Loaded Economics Sample Notes", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("💻 CS: OS Concurrency & Deadlocks") },
                                onClick = {
                                    showSampleMenu = false
                                    viewModel.loadSampleNotes(2)
                                    Toast.makeText(context, "Loaded Computer Science Sample Notes", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Notification Banner (Save status, etc)
        workspaceState.saveNoteMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandEmeraldCorrect)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(msg, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Note Metadata: Title, Folder, Tags
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Title field
                OutlinedTextField(
                    value = workspaceState.title,
                    onValueChange = { viewModel.onNoteTitleChanged(it) },
                    placeholder = { Text("Note Title (e.g., Biology 101: Respiration)") },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Folder selector & Tags row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Folder selector button
                    Box(modifier = Modifier.weight(1f)) {
                        val selectedFolder = folders.find { it.id == workspaceState.selectedFolderId }
                        OutlinedButton(
                            onClick = { showFolderMenu = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_folder_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedFolder?.name ?: "Select Folder",
                                maxLines = 1
                            )
                        }

                        DropdownMenu(
                            expanded = showFolderMenu,
                            onDismissRequest = { showFolderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No Folder") },
                                onClick = {
                                    viewModel.onFolderSelected(null)
                                    showFolderMenu = false
                                }
                            )
                            folders.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text("📁 ${f.name}") },
                                    onClick = {
                                        viewModel.onFolderSelected(f.id)
                                        showFolderMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Tags field
                    OutlinedTextField(
                        value = workspaceState.tagsText,
                        onValueChange = { viewModel.onTagsChanged(it) },
                        placeholder = { Text("Tags (comma separated)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("note_tags_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }

        // Notes Input Text Box
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Study Notes Workspace",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$wordCount words",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hasNotes) {
                            IconButton(
                                onClick = { viewModel.clearWorkspace() },
                                modifier = Modifier.size(28.dp).testTag("clear_notes_button")
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = BrandRoseIncorrect)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = workspaceState.notesText,
                    onValueChange = { viewModel.onNotesTextChanged(it) },
                    placeholder = {
                        Text(
                            "Paste your study notes, lecture transcript, or chapter text here...\n\n" +
                            "Tip: Tap 'Sample Notes' above to quickly explore Summaries, Q&A, Quizzes, and Flashcards!"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .testTag("study_notes_textbox"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (!hasNotes) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ℹ️ Paste notes above to enable AI Summarizer, Q&A, Flashcards & Quiz buttons.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Action Buttons Row (Summarize Notes, Ask AI, Generate Quiz, Generate Flashcards, Save Note)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AI Study Actions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Summarize Notes Button
                    Button(
                        onClick = { viewModel.summarizeNotes() },
                        enabled = hasNotes && !workspaceState.isSummarizing,
                        modifier = Modifier.testTag("summarize_notes_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (workspaceState.isSummarizing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Summarizing...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Summarize Notes", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Generate Quiz Button
                    Button(
                        onClick = {
                            viewModel.generateQuizFromNotes()
                            onNavigateToQuiz()
                        },
                        enabled = hasNotes,
                        modifier = Modifier.testTag("generate_quiz_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandIndigoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Quiz (5 MCQs)", fontWeight = FontWeight.SemiBold)
                    }

                    // Generate Flashcards Button
                    Button(
                        onClick = {
                            viewModel.generateFlashcardsFromNotes()
                            onNavigateToFlashcards()
                        },
                        enabled = hasNotes,
                        modifier = Modifier.testTag("generate_flashcards_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Flashcards", fontWeight = FontWeight.SemiBold)
                    }

                    // Save Note Button
                    OutlinedButton(
                        onClick = { viewModel.saveCurrentNote() },
                        enabled = hasNotes,
                        modifier = Modifier.testTag("save_note_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Note")
                    }
                }
            }
        }

        // Summary Error Banner
        workspaceState.summaryError?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Summary Result Card
        workspaceState.summaryResult?.let { summary ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_result_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = BrandEmeraldCorrect.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "AI SUMMARY",
                                    color = BrandEmeraldCorrect,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = summary.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = {
                                val fullSummary = "${summary.title}\n\n${summary.summary}\n\nKey Takeaways:\n" +
                                    summary.keyTakeaways.joinToString("\n") { "• $it" }
                                clipboardManager.setText(AnnotatedString(fullSummary))
                                Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_summary_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Summary")
                        }
                    }

                    Text(
                        text = summary.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (summary.keyTakeaways.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Key Takeaways",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        summary.keyTakeaways.forEach { takeaway ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = takeaway.removePrefix("•").trim(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (summary.bulletPoints.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Detailed Breakdown",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        summary.bulletPoints.forEach { pt ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("✓", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = pt.removePrefix("•").removePrefix("✓").trim(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Ask AI Question Box Section
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ask AI About Your Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Strict contextual answering: admits when facts are missing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = workspaceState.askQuestionText,
                        onValueChange = { viewModel.onAskQuestionChanged(it) },
                        placeholder = { Text("Ask a question about the notes...") },
                        enabled = hasNotes && !workspaceState.isAskingAi,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ask_ai_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = { viewModel.askAiQuestion() },
                        enabled = hasNotes && workspaceState.askQuestionText.isNotBlank() && !workspaceState.isAskingAi,
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("ask_ai_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (workspaceState.isAskingAi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Ask")
                        }
                    }
                }

                workspaceState.askAiError?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // Q&A History stream
                AnimatedVisibility(
                    visible = workspaceState.qaList.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Conversation History",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        workspaceState.qaList.forEach { qa ->
                            QaItemCard(qa = qa)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun QaItemCard(qa: AskAiResult) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (qa.isMissingInformation) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Student question
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Q: ",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = qa.question,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // AI Answer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (qa.isMissingInformation) {
                        Surface(
                            color = BrandAmberHighlight.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = BrandAmberHighlight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Missing from provided notes",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandAmberHighlight
                                )
                            }
                        }
                    }
                    Text(
                        text = qa.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(qa.answer))
                        Toast.makeText(context, "Answer copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
