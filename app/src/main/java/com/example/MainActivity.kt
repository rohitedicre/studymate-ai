package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.StudyMateViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.OrganizeNotesScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.MyApplicationTheme

enum class StudyMateScreen(val label: String) {
    WORKSPACE("Workspace"),
    ORGANIZE("Folders"),
    FLASHCARDS("Flashcards"),
    QUIZ("Quiz"),
    ACCOUNT("Account")
}

class MainActivity : ComponentActivity() {
    private val viewModel: StudyMateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Step 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                StudyMateApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StudyMateApp(viewModel: StudyMateViewModel) {
    var currentScreen by remember { mutableStateOf(StudyMateScreen.WORKSPACE) }

    // Hardware back press handler: return to Workspace if in another tab
    if (currentScreen != StudyMateScreen.WORKSPACE) {
        BackHandler {
            currentScreen = StudyMateScreen.WORKSPACE
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar"),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == StudyMateScreen.WORKSPACE,
                    onClick = { currentScreen = StudyMateScreen.WORKSPACE },
                    icon = {
                        Icon(
                            if (currentScreen == StudyMateScreen.WORKSPACE) Icons.Filled.Create else Icons.Outlined.Create,
                            contentDescription = "Workspace"
                        )
                    },
                    label = { Text("Notes") },
                    modifier = Modifier.testTag("nav_tab_workspace")
                )

                NavigationBarItem(
                    selected = currentScreen == StudyMateScreen.ORGANIZE,
                    onClick = { currentScreen = StudyMateScreen.ORGANIZE },
                    icon = {
                        Icon(
                            if (currentScreen == StudyMateScreen.ORGANIZE) Icons.Filled.Folder else Icons.Outlined.Folder,
                            contentDescription = "Folders"
                        )
                    },
                    label = { Text("Folders") },
                    modifier = Modifier.testTag("nav_tab_organize")
                )

                NavigationBarItem(
                    selected = currentScreen == StudyMateScreen.FLASHCARDS,
                    onClick = { currentScreen = StudyMateScreen.FLASHCARDS },
                    icon = {
                        Icon(
                            if (currentScreen == StudyMateScreen.FLASHCARDS) Icons.Filled.Style else Icons.Outlined.Style,
                            contentDescription = "Flashcards"
                        )
                    },
                    label = { Text("Cards") },
                    modifier = Modifier.testTag("nav_tab_flashcards")
                )

                NavigationBarItem(
                    selected = currentScreen == StudyMateScreen.QUIZ,
                    onClick = { currentScreen = StudyMateScreen.QUIZ },
                    icon = {
                        Icon(
                            if (currentScreen == StudyMateScreen.QUIZ) Icons.Filled.Quiz else Icons.Outlined.Quiz,
                            contentDescription = "Quiz"
                        )
                    },
                    label = { Text("Quiz") },
                    modifier = Modifier.testTag("nav_tab_quiz")
                )

                NavigationBarItem(
                    selected = currentScreen == StudyMateScreen.ACCOUNT,
                    onClick = { currentScreen = StudyMateScreen.ACCOUNT },
                    icon = {
                        Icon(
                            if (currentScreen == StudyMateScreen.ACCOUNT) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                            contentDescription = "Account"
                        )
                    },
                    label = { Text("Account") },
                    modifier = Modifier.testTag("nav_tab_account")
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition",
            modifier = Modifier.padding(innerPadding)
        ) { screen ->
            when (screen) {
                StudyMateScreen.WORKSPACE -> WorkspaceScreen(
                    viewModel = viewModel,
                    onNavigateToQuiz = { currentScreen = StudyMateScreen.QUIZ },
                    onNavigateToFlashcards = { currentScreen = StudyMateScreen.FLASHCARDS }
                )
                StudyMateScreen.ORGANIZE -> OrganizeNotesScreen(
                    viewModel = viewModel,
                    onOpenNoteInWorkspace = { currentScreen = StudyMateScreen.WORKSPACE }
                )
                StudyMateScreen.FLASHCARDS -> FlashcardsScreen(
                    viewModel = viewModel
                )
                StudyMateScreen.QUIZ -> QuizScreen(
                    viewModel = viewModel
                )
                StudyMateScreen.ACCOUNT -> AuthScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
