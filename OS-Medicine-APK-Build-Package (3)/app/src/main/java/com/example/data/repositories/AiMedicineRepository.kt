package com.example.data.repositories

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class AiMedicineRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are OS Medicine's helpful pharmacy discovery assistant for patients in Ethiopia. " +
                        "Help patients read prescription photos, clarify medicine names/dosages, and guide them on using OS Medicine " +
                        "(English and Amharic supported). Always include a brief reminder to confirm dosages with a licensed Ethiopian pharmacist or doctor."
                )
            }
        )
    }

    private val chatSession by lazy { model.startChat() }

    suspend fun analyzePrescription(bitmap: Bitmap, language: String): Result<String> {
        return try {
            val langPrompt = if (language == "am") {
                "Analyze this prescription image and list the likely medicine names, dosages, and brief guidance in Amharic (አማርኛ) with English medicine names."
            } else {
                "Analyze this prescription image and list the likely medicine names, dosages, and brief patient-friendly guidance."
            }
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(bitmap)
                        text(langPrompt)
                    }
                )
            }
            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("AI assistant returned an empty response."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Prescription AI analysis timed out", e)
            Result.failure(Exception("AI analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Prescription AI analysis failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI analysis failed."))
        }
    }

    fun sendChatMessageStream(message: String): Flow<String> = flow {
        val buffer = StringBuilder()
        withTimeout(60_000L) {
            chatSession.sendMessageStream(message).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    buffer.append(part)
                    emit(buffer.toString())
                }
            }
        }
        if (buffer.isEmpty()) {
            throw IllegalStateException("AI assistant returned an empty response.")
        }
    }

    companion object {
        private const val TAG = "AiMedicineRepository"
    }
}
