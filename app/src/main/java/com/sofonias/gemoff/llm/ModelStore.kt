package com.sofonias.gemoff.llm

import android.content.Context
import java.io.File

/**
 * Stores the imported model file and remembers which one is active.
 */
object ModelStore {

    private const val PREFS = "gemoff"
    private const val KEY_MODEL_PATH = "model_path"

    fun getSavedModelPath(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MODEL_PATH, null)?.takeIf { File(it).exists() }
    }

    fun saveModelPath(context: Context, path: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODEL_PATH, path)
            .apply()
    }

    fun modelFile(context: Context): File =
        File(File(context.filesDir, "models"), "gemoff_model.task")

    /**
     * Copies the picked model file (.task / .litertlm) into app storage.
     * Returns the absolute path of the stored copy.
     */
    fun importModel(context: Context, source: android.net.Uri): String {
        val target = modelFile(context)
        target.parentFile?.mkdirs()

        // Remove any previous model first so we never hold two big files.
        target.delete()

        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output, bufferSize = 1024 * 1024)
            }
        } ?: throw IllegalStateException("Could not open the selected file")

        if (target.length() == 0L) {
            target.delete()
            throw IllegalStateException("The selected file is empty")
        }

        saveModelPath(context, target.absolutePath)
        return target.absolutePath
    }

    fun deleteModel(context: Context) {
        val path = getSavedModelPath(context)
        if (path != null) {
            File(path).delete()
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_MODEL_PATH)
            .apply()
    }
}
