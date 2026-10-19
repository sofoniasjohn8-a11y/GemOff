package com.sofonias.gemoff.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import java.util.concurrent.Future

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
    private var activeFuture: Future<*>? = null
    @Volatile
    private var needsSessionReset = false
    private val engineLock = Any()

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

    /** Instantly flags the session to reset on the next prompt, without blocking the caller thread. */
    fun stopAndMarkReset() {
        try {
            activeFuture?.cancel(true)
        } catch (_: Exception) {
        }
        activeFuture = null
        needsSessionReset = true
    }

    fun resetSession() {
        stopAndMarkReset()
    }

    private fun performSessionResetIfNeeded() {
        if (needsSessionReset) {
            try {
                session.close()
            } catch (_: Exception) {
            }
            try {
                session = newSession()
            } catch (_: Exception) {
            }
            needsSessionReset = false
        }
    }

    /**
     * Streams a response for [prompt]. [onPartial] is invoked on a
     * background thread with each new chunk of text.
     */
    fun generateResponseAsync(
        prompt: String,
        onPartial: (chunk: String, done: Boolean) -> Unit
    ) {
        synchronized(engineLock) {
            performSessionResetIfNeeded()
            try {
                session.addQueryChunk(prompt)
                val future = session.generateResponseAsync(
                    ProgressListener { chunk, done -> onPartial(chunk, done) }
                )
                activeFuture = future
                try {
                    future.get()
                } finally {
                    activeFuture = null
                }
            } catch (e: Exception) {
                activeFuture = null
                if (e.message?.contains("Previous invocation still processing", ignoreCase = true) == true) {
                    // C++ engine was busy - reset session and retry once cleanly
                    try {
                        session.close()
                    } catch (_: Exception) {
                    }
                    try {
                        session = newSession()
                        session.addQueryChunk(prompt)
                        val retryFuture = session.generateResponseAsync(
                            ProgressListener { chunk, done -> onPartial(chunk, done) }
                        )
                        activeFuture = retryFuture
                        try {
                            retryFuture.get()
                        } finally {
                            activeFuture = null
                        }
                    } catch (retryException: Exception) {
                        activeFuture = null
                        throw retryException
                    }
                } else {
                    throw e
                }
            }
        }
    }

    fun close() {
        synchronized(engineLock) {
            try {
                activeFuture?.cancel(true)
            } catch (_: Exception) {
            }
            activeFuture = null

            try {
                session.close()
            } catch (_: Exception) {
            }

            try {
                llm.close()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val MAX_TOKENS = 2048

        /** Loads the model. Slow (can take seconds) - call off the main thread. */
        fun create(context: Context, modelPath: String): InferenceEngine =
            InferenceEngine(context.applicationContext, modelPath)
    }
}
