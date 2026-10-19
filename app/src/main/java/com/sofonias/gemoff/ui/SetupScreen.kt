package com.sofonias.gemoff.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sofonias.gemoff.AppStrings
import com.sofonias.gemoff.ChatUiState
import com.sofonias.gemoff.ModelState

@Composable
fun SetupScreen(
    state: ChatUiState,
    onImportModel: (android.net.Uri) -> Unit,
    onRetry: () -> Unit,
    onRemoveModel: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val pickModel = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImportModel) }

    GeminiBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top right settings icon
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = AppStrings.settings
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    AppStrings.appName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    AppStrings.setupHeadline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                Text(
                    AppStrings.setupDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    AppStrings.setupSteps,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = { pickModel.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(AppStrings.importModelBtn)
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    AppStrings.modelSizeNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (state.modelState == ModelState.ERROR) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        state.modelError ?: "Something went wrong.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    RowButtons(AppStrings.retry, AppStrings.deleteModel, onRetry, onRemoveModel)
                }
            }
        }
    }
}

@Composable
private fun RowButtons(
    retryLabel: String,
    deleteLabel: String,
    onRetry: () -> Unit,
    onRemoveModel: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onRetry) { Text(retryLabel) }
        OutlinedButton(onClick = onRemoveModel) { Text(deleteLabel) }
    }
}
