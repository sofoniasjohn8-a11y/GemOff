package com.sofonias.gemoff.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.ProgressListener

/**
 * Thin wrapper around the MediaPipe LLM Inference task.
 *
 * Runs a Gemma model entirely on the user's device: no network calls,
 * no accounts, no telemetry. A [LlmInferenceSession] keeps the multi-turn
 * conversation context so follow-up questions work like a real chat.
 */
class InferenceEngine private constructor(context: Context, modelPath: String) {

    private val llm: LlmInference
    private var session: LlmInferenceSession

    init {
        llm = LlmInference.createFromOptions(
            context,
            LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(MAX_TOKENS)
                .setMaxTopK(40)
                .build()
        )
        session = newSession()
    }

    private fun newSession(): LlmInferenceSession =
        LlmInferenceSession.createFromOptions(
            llm,
            LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(40)
                .setTemperature(0.8f)
                .setRandomSeed(0)
                .build()
        )

    /** Drops the conversation context and starts a fresh session. */
    fun resetSession() {
        try {
            session.close()
        } catch (_: Exception) {
        }
        session = newSession()
    }

    /**
     * Streams a response for [prompt]. [onPartial] is invoked on a
     * background thread with each new chunk of text; call site must
     * take care of thread-safe state updates.
     */
    fun generateResponseAsync(
        prompt: String,
        onPartial: (chunk: String, done: Boolean) -> Unit
    ) {
        session.addQueryChunk(prompt)
        session.generateResponseAsync(
            ProgressListener { chunk, done -> onPartial(chunk, done) }
        ).get() // Future completes when done=true, or throws on failure.
    }

    fun close() {
        try {
            session.close()
        } catch (_: Exception) {
        }
        try {
            llm.close()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val MAX_TOKENS = 2048

        /** Loads the model. Slow (can take seconds) - call off the main thread. */
        fun create(context: Context, modelPath: String): InferenceEngine =
            InferenceEngine(context.applicationContext, modelPath)
    }
}
