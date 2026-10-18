package com.sofonias.gemoff

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sofonias.gemoff.llm.InferenceEngine
import com.sofonias.gemoff.llm.ModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ModelState { NO_MODEL, LOADING, READY, ERROR }

enum class Role { USER, ASSISTANT }

data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String
)

data class ChatUiState(
    val modelState: ModelState = ModelState.NO_MODEL,
    val modelName: String? = null,
    val modelError: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var engine: InferenceEngine? = null
    private var nextMessageId = 0L

    init {
        tryLoadModel()
    }

    /** Loads the saved model, if the user imported one before. */
    fun tryLoadModel() {
        val path = ModelStore.getSavedModelPath(getApplication())
        if (path == null) {
            _uiState.update { it.copy(modelState = ModelState.NO_MODEL, modelName = null) }
            return
        }
        _uiState.update { it.copy(modelState = ModelState.LOADING, modelError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                engine?.close()
                engine = InferenceEngine.create(getApplication(), path)
                _uiState.update {
                    it.copy(
                        modelState = ModelState.READY,
                        modelName = File(path).name,
                        messages = emptyList()
                    )
                }
            } catch (e: Exception) {
                engine = null
                _uiState.update {
                    it.copy(
                        modelState = ModelState.ERROR,
                        modelError = "Could not load the model: ${e.message}"
                    )
                }
            }
        }
    }

    /** Copies a picked .task/.litertlm file into app storage, then loads it. */
    fun importModel(uri: Uri) {
        _uiState.update {
            it.copy(modelState = ModelState.LOADING, modelError = null, messages = emptyList())
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val name = queryDisplayName(uri) ?: "model"
                val path = ModelStore.importModel(getApplication(), uri)
                engine?.close()
                engine = InferenceEngine.create(getApplication(), path)
                _uiState.update {
                    it.copy(modelState = ModelState.READY, modelName = name, messages = emptyList())
                }
            } catch (e: Exception) {
                engine = null
                _uiState.update {
                    it.copy(
                        modelState = ModelState.ERROR,
                        modelError = "Import failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun removeModel() {
        viewModelScope.launch(Dispatchers.IO) {
            engine?.close()
            engine = null
            ModelStore.deleteModel(getApplication())
            _uiState.update {
                ChatUiState(modelState = ModelState.NO_MODEL)
            }
        }
    }

    fun sendMessage(text: String) {
        val prompt = text.trim()
        val currentEngine = engine ?: return
        if (prompt.isEmpty() || _uiState.value.isGenerating) return

        val userMessage = ChatMessage(nextMessageId++, Role.USER, prompt)
        val assistantId = nextMessageId++
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + ChatMessage(assistantId, Role.ASSISTANT, ""),
                isGenerating = true
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val accumulated = StringBuilder()
            try {
                currentEngine.generateResponseAsync(prompt) { chunk, done ->
                    synchronized(accumulated) { accumulated.append(chunk) }
                    _uiState.update { state ->
                        val text = synchronized(accumulated) { accumulated.toString() }
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantId) msg.copy(text = text) else msg
                            },
                            isGenerating = !done
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main.immediate) {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantId) {
                                    val soFar = synchronized(accumulated) { accumulated.toString() }
                                    msg.copy(
                                        text = if (soFar.isNotEmpty()) soFar
                                        else "Something went wrong: ${e.message}"
                                    )
                                } else msg
                            },
                            isGenerating = false
                        )
                    }
                }
            }
        }
    }

    /** Clears the conversation and the model's context window. */
    fun clearChat() {
        val currentEngine = engine ?: return
        _uiState.update { it.copy(messages = emptyList(), isGenerating = false) }
        viewModelScope.launch(Dispatchers.IO) {
            currentEngine.resetSession()
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        getApplication<Application>().contentResolver
            .query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
    } catch (_: Exception) {
        null
    }

    override fun onCleared() {
        engine?.close()
        engine = null
    }
}
