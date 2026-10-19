package com.sofonias.gemoff.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sofonias.gemoff.ChatViewModel
import com.sofonias.gemoff.ModelState

@Composable
fun GemOffApp(viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    GemOffTheme(appTheme = state.appTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (state.modelState) {
                ModelState.READY -> ChatScreen(
                    state = state,
                    onSendMessage = viewModel::sendMessage,
                    onStopGeneration = viewModel::stopGeneration,
                    onEditMessage = viewModel::editMessage,
                    onNewChat = viewModel::startNewChat,
                    onSelectSession = viewModel::selectSession,
                    onDeleteSession = viewModel::deleteSession,
                    onClearChat = viewModel::clearChat,
                    onOpenSettings = { showSettings = true }
                )

                ModelState.LOADING -> LoadingScreen()

                else -> SetupScreen(
                    state = state,
                    onImportModel = viewModel::importModel,
                    onRetry = viewModel::tryLoadModel,
                    onRemoveModel = viewModel::removeModel,
                    onOpenSettings = { showSettings = true }
                )
            }

            if (showSettings) {
                SettingsDialog(
                    currentTheme = state.appTheme,
                    onThemeSelected = viewModel::setTheme,
                    onDismiss = { showSettings = false }
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
