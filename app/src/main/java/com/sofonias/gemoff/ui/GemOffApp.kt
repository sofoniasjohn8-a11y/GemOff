package com.sofonias.gemoff.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sofonias.gemoff.ChatViewModel
import com.sofonias.gemoff.ModelState
import androidx.compose.runtime.collectAsState

@Composable
fun GemOffApp(viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    GemOffTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (state.modelState) {
                ModelState.READY -> ChatScreen(
                    state = state,
                    onSendMessage = viewModel::sendMessage,
                    onClearChat = viewModel::clearChat
                )

                ModelState.LOADING -> LoadingScreen()

                else -> SetupScreen(
                    state = state,
                    onImportModel = viewModel::importModel,
                    onRetry = viewModel::tryLoadModel,
                    onRemoveModel = viewModel::removeModel
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
