package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface ArtworkGenUiState {
    data object Idle : ArtworkGenUiState
    data object Loading : ArtworkGenUiState
    data class Success(val bitmap: Bitmap, val promptUsed: String) : ArtworkGenUiState
    data class Error(val message: String) : ArtworkGenUiState
}

/**
 * Generates custom collection backdrop artwork using Firebase AI Logic (gemini-nano-banana-2.1).
 * Surfaces authentic backend errors (e.g., Blaze billing/quota or network timeout) without mock fallbacks.
 */
class ArtworkDesignerViewModel : ViewModel() {

    private val imageModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config,
        )
    }

    private val _genState = MutableStateFlow<ArtworkGenUiState>(ArtworkGenUiState.Idle)
    val genState: StateFlow<ArtworkGenUiState> = _genState.asStateFlow()

    fun generateCollectionBackdrop(
        collectionTitle: String,
        artPrompt: String,
        styleMood: String,
    ) {
        val cleanPrompt = artPrompt.trim()
        if (cleanPrompt.isBlank()) {
            _genState.value = ArtworkGenUiState.Error("Enter a visual description before generating AI artwork.")
            return
        }

        val fullPrompt = buildString {
            append("Cinematic home-theater streaming collection cover backdrop artwork for '")
            append(collectionTitle.ifBlank { "Cinema Collection" })
            append("'. Subject & scene: ")
            append(cleanPrompt)
            append(". Aesthetic style: ")
            append(styleMood)
            append(". Moody dark negative space for clean UI typography overlay, ultra-crisp lighting, no text, no letters, no logos, no watermark.")
        }

        viewModelScope.launch {
            _genState.value = ArtworkGenUiState.Loading
            try {
                val bitmap = withTimeout(60_000L) {
                    val response = imageModel.generateContent(fullPrompt)
                    response.candidates
                        .firstOrNull()
                        ?.content
                        ?.parts
                        ?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()
                        ?.image
                }
                if (bitmap != null) {
                    _genState.value = ArtworkGenUiState.Success(bitmap = bitmap, promptUsed = cleanPrompt)
                } else {
                    _genState.value = ArtworkGenUiState.Error("The AI model returned no image. Try refining your prompt.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI backdrop generation timed out", e)
                _genState.value = ArtworkGenUiState.Error("Artwork generation timed out after 60 seconds. Please try again.")
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "AI backdrop quota exceeded", e)
                _genState.value = ArtworkGenUiState.Error(
                    "Image generation quota exceeded or Blaze billing is required for Gemini image models. You can still use Studio Art, Neon Mesh, or My Photo."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI backdrop generation failed", e)
                _genState.value = ArtworkGenUiState.Error(
                    e.localizedMessage ?: "AI image generation failed. Check Firebase Blaze plan or try another prompt."
                )
            }
        }
    }

    fun clearError() {
        if (_genState.value is ArtworkGenUiState.Error) {
            _genState.value = ArtworkGenUiState.Idle
        }
    }

    private companion object {
        const val TAG = "ArtworkDesignerVM"
    }
}
