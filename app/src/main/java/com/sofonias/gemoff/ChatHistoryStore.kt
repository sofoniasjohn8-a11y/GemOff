package com.sofonias.gemoff

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ChatSession(
    val id: String,
    val title: String,
    val timestamp: Long,
    val messages: List<ChatMessage>
)

object ChatHistoryStore {
    private const val CHATS_DIR = "chat_history"

    private fun getChatsDirectory(context: Context): File {
        val dir = File(context.filesDir, CHATS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveSession(context: Context, session: ChatSession) {
        try {
            val file = File(getChatsDirectory(context), "${session.id}.json")
            val json = JSONObject().apply {
                put("id", session.id)
                put("title", session.title)
                put("timestamp", session.timestamp)

                val msgArray = JSONArray()
                session.messages.forEach { msg ->
                    msgArray.put(JSONObject().apply {
                        put("id", msg.id)
                        put("role", msg.role.name)
                        put("text", msg.text)
                    })
                }
                put("messages", msgArray)
            }
            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAllSessions(context: Context): List<ChatSession> {
        val sessions = mutableListOf<ChatSession>()
        val dir = getChatsDirectory(context)
        val files = dir.listFiles { _, name -> name.endsWith(".json") } ?: return emptyList()

        for (file in files) {
            try {
                val json = JSONObject(file.readText())
                val id = json.getString("id")
                val title = json.optString("title", "Chat Conversation")
                val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                val msgArray = json.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()
                for (i in 0 until msgArray.length()) {
                    val msgObj = msgArray.getJSONObject(i)
                    val msgId = msgObj.getLong("id")
                    val role = Role.valueOf(msgObj.getString("role"))
                    val text = msgObj.getString("text")
                    messages.add(ChatMessage(msgId, role, text))
                }
                sessions.add(ChatSession(id, title, timestamp, messages))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return sessions.sortedByDescending { it.timestamp }
    }

    fun deleteSession(context: Context, id: String) {
        try {
            val file = File(getChatsDirectory(context), "$id.json")
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
