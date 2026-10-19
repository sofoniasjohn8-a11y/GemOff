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
import java.util.UUID

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
    val isGenerating: Boolean = false,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val currentSessionId: String = UUID.randomUUID().toString(),
    val sessions: List<ChatSession> = emptyList()
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var engine: InferenceEngine? = null
    private var nextMessageId = 0L
    private var generationJob: kotlinx.coroutines.Job? = null
    @Volatile
    private var isCancelRequested = false

    init {
        val application = getApplication<Application>()
        val savedSessions = ChatHistoryStore.loadAllSessions(application)
        val initialSession = savedSessions.firstOrNull() ?: ChatSession(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
            timestamp = System.currentTimeMillis(),
            messages = emptyList()
        )
        _uiState.update {
            it.copy(
                appTheme = AppSettings.getTheme(application),
                currentSessionId = initialSession.id,
                messages = initialSession.messages,
                sessions = savedSessions
            )
        }
        tryLoadModel()
    }

    fun setTheme(theme: AppTheme) {
        val application = getApplication<Application>()
        AppSettings.setTheme(application, theme)
        _uiState.update { it.copy(appTheme = theme) }
    }

    fun stopGeneration() {
        isCancelRequested = true
        generationJob?.cancel()
        generationJob = null
        _uiState.update { it.copy(isGenerating = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                engine?.resetSession()
            } catch (_: Exception) {}
        }
    }

    fun startNewChat() {
        stopGeneration()
        val newId = UUID.randomUUID().toString()
        _uiState.update {
            it.copy(
                currentSessionId = newId,
                messages = emptyList(),
                isGenerating = false
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            engine?.resetSession()
        }
    }

    fun selectSession(sessionId: String) {
        if (sessionId == _uiState.value.currentSessionId) return
        stopGeneration()
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = ChatHistoryStore.loadAllSessions(app)
            val selected = sessions.firstOrNull { it.id == sessionId }
            if (selected != null) {
                _uiState.update {
                    it.copy(
                        currentSessionId = selected.id,
                        messages = selected.messages,
                        isGenerating = false
                    )
                }
                engine?.resetSession()
            }
        }
    }

    fun deleteSession(sessionId: String) {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            ChatHistoryStore.deleteSession(app, sessionId)
            val remaining = ChatHistoryStore.loadAllSessions(app)
            if (sessionId == _uiState.value.currentSessionId) {
                val nextSession = remaining.firstOrNull()
                if (nextSession != null) {
                    _uiState.update {
                        it.copy(
                            currentSessionId = nextSession.id,
                            messages = nextSession.messages,
                            sessions = remaining,
                            isGenerating = false
                        )
                    }
                } else {
                    val newId = UUID.randomUUID().toString()
                    _uiState.update {
                        it.copy(
                            currentSessionId = newId,
                            messages = emptyList(),
                            sessions = emptyList(),
                            isGenerating = false
                        )
                    }
                }
                engine?.resetSession()
            } else {
                _uiState.update { it.copy(sessions = remaining) }
            }
        }
    }

    private fun saveCurrentSession(messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val sessionId = _uiState.value.currentSessionId
            val firstPrompt = messages.firstOrNull { it.role == Role.USER }?.text ?: "Chat Conversation"
            val title = if (firstPrompt.length > 30) firstPrompt.take(30) + "..." else firstPrompt
            val session = ChatSession(
                id = sessionId,
                title = title,
                timestamp = System.currentTimeMillis(),
                messages = messages
            )
            ChatHistoryStore.saveSession(app, session)
            val updatedSessions = ChatHistoryStore.loadAllSessions(app)
            _uiState.update { it.copy(sessions = updatedSessions) }
        }
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
                        modelName = File(path).name
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
            stopGeneration()
            engine?.close()
            engine = null
            ModelStore.deleteModel(getApplication())
            _uiState.update {
                ChatUiState(modelState = ModelState.NO_MODEL)
            }
        }
    }

    fun editMessage(id: Long, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            val updatedMsgs = state.messages.map { msg ->
                if (msg.id == id) msg.copy(text = trimmed) else msg
            }
            saveCurrentSession(updatedMsgs)
            state.copy(messages = updatedMsgs)
        }
    }

    fun sendMessage(text: String) {
        val prompt = text.trim()
        val currentEngine = engine ?: return
        if (prompt.isEmpty() || _uiState.value.isGenerating) return

        isCancelRequested = false
        val userMessage = ChatMessage(nextMessageId++, Role.USER, prompt)
        val assistantId = nextMessageId++

        val initialMessages = _uiState.value.messages + userMessage + ChatMessage(assistantId, Role.ASSISTANT, "")
        _uiState.update {
            it.copy(
                messages = initialMessages,
                isGenerating = true
            )
        }

        generationJob = viewModelScope.launch(Dispatchers.IO) {
            val accumulated = StringBuilder()
            try {
                currentEngine.generateResponseAsync(prompt) { chunk, done ->
                    if (isCancelRequested || generationJob?.isCancelled == true) {
                        return@generateResponseAsync
                    }
                    synchronized(accumulated) { accumulated.append(chunk) }
                    _uiState.update { state ->
                        if (isCancelRequested) return@update state
                        val textSoFar = synchronized(accumulated) { accumulated.toString() }
                        val currentMsgs = state.messages.map { msg ->
                            if (msg.id == assistantId) msg.copy(text = textSoFar) else msg
                        }
                        if (done) {
                            saveCurrentSession(currentMsgs)
                        }
                        state.copy(
                            messages = currentMsgs,
                            isGenerating = if (isCancelRequested) false else !done
                        )
                    }
                }
            } catch (e: Exception) {
                if (!isCancelRequested && e !is kotlinx.coroutines.CancellationException) {
                    withContext(Dispatchers.Main.immediate) {
                        _uiState.update { state ->
                            val currentMsgs = state.messages.map { msg ->
                                if (msg.id == assistantId) {
                                    val soFar = synchronized(accumulated) { accumulated.toString() }
                                    msg.copy(
                                        text = if (soFar.isNotEmpty()) soFar
                                        else "Something went wrong: ${e.message}"
                                    )
                                } else msg
                            }
                            saveCurrentSession(currentMsgs)
                            state.copy(
                                messages = currentMsgs,
                                isGenerating = false
                            )
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    /** Clears the conversation and the model's context window. */
    fun clearChat() {
        val currentEngine = engine ?: return
        _uiState.update { it.copy(messages = emptyList(), isGenerating = false) }
        viewModelScope.launch(Dispatchers.IO) {
            currentEngine.resetSession()
            ChatHistoryStore.deleteSession(getApplication(), _uiState.value.currentSessionId)
            val remaining = ChatHistoryStore.loadAllSessions(getApplication())
            _uiState.update { it.copy(sessions = remaining) }
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
