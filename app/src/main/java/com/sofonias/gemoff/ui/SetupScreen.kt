package com.sofonias.gemoff.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sofonias.gemoff.ChatUiState
import com.sofonias.gemoff.ModelState

@Composable
fun SetupScreen(
    state: ChatUiState,
    onImportModel: (android.net.Uri) -> Unit,
    onRetry: () -> Unit,
    onRemoveModel: () -> Unit
) {
    val pickModel = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImportModel) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "GemOff",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Your private, offline AI assistant",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        Text(
            "GemOff runs a Gemma AI model entirely on your phone. " +
                "No internet, no account, no data ever leaves the device.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        Text(
            "First, get a model file (once, on any internet connection):\n" +
                "1. On a computer or your browser, download a MediaPipe/" +
                "LiteRT Gemma model, e.g. Gemma 3 1B (.task or .litertlm) " +
                "from the LiteRT community on Hugging Face.\n" +
                "2. Copy it to your phone (USB, Drive, any way you like).\n" +
                "3. Tap \"Import model file\" below and pick it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { pickModel.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Import model file (.task / .litertlm)")
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "The model file is 0.5-3 GB. Make sure you have enough free storage.",
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
            RowButtons(onRetry, onRemoveModel)
        }
    }
}

@Composable
private fun RowButtons(onRetry: () -> Unit, onRemoveModel: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onRetry) { Text("Retry") }
        OutlinedButton(onClick = onRemoveModel) { Text("Delete model") }
    }
}
