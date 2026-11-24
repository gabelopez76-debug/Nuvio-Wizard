package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CatalogCollectionItem
import com.example.data.model.SetupProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
)

data class SupportChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            isUser = false,
            text = "Hi! I'm your Nuvio Setup Support Assistant. I can see your current profile progress, AIOStreams add-ons, and AIOMetadata board (never your private passwords or raw API keys) and remember our conversation. Ask me anything about Real-Debrid, AIOStreams, AIOMetadata catalogs, or troubleshooting Nuvio!",
        )
    ),
    val isResponding: Boolean = false,
    val errorMessage: String? = null,
)

class SupportChatViewModel : ViewModel() {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are an expert, friendly technical support assistant built into the Nuvio Setup Android companion app.
                    Your specialty is guiding users step-by-step through:
                    1. Creating and configuring their Nuvio account and managing setup profiles.
                    2. Setting up Real-Debrid (real-debrid.com), activating Premium, retrieving the Secret API Token at real-debrid.com/apitoken, and troubleshooting cached vs uncached streams.
                    3. Configuring AIOStreams (e.g., aiostreams.elfhosted.com) as a unified single-configuration stream engine so Real-Debrid, resolution filters, cached-only mode, and formatters (Prism, Minimalistic, Detailed) are entered ONCE and automatically inherited by all stream add-ons (Torrentio, MediaFusion, Comet, Jackettio, StremThru, KnightCrawler, or custom sources).
                    4. Configuring AIOMetadata (e.g., aiometadata.elfhosted.com) with TMDB, TVDB, Kitsu/MAL, MDBList, and RPDB rating posters, plus building and organizing catalogs and curated collections.
                    5. Deploying into Nuvio in the correct priority order: #1 AIOMetadata at the top, #2 AIOStreams underneath.
                    
                    Always use the live profile context included with user turns to give tailored, actionable advice.
                    Keep responses concise, structured with numbered steps or bullet points when helpful, and never ask users to paste their raw passwords or private API keys into chat.
                    """.trimIndent()
                )
            }
        )
    }

    private var chatSession: Chat? = null

    private val _uiState = MutableStateFlow(SupportChatUiState())
    val uiState: StateFlow<SupportChatUiState> = _uiState.asStateFlow()

    fun sendMessage(
        userText: String,
        activeProfile: SetupProfile?,
        activeCatalogs: List<CatalogCollectionItem>,
    ) {
        val cleanText = userText.trim()
        if (cleanText.isBlank() || _uiState.value.isResponding) return

        val userMsg = ChatMessage(isUser = true, text = cleanText)
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderAssistant = ChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "",
            isStreaming = true,
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMsg + placeholderAssistant,
                isResponding = true,
                errorMessage = null,
            )
        }

        val contextHeader = buildSanitizedContextHeader(activeProfile, activeCatalogs)
        val contextualPrompt = "$contextHeader\nUser question: $cleanText"

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                val activeChat = chatSession ?: model.startChat().also { chatSession = it }
                withTimeout(60_000L) {
                    activeChat.sendMessageStream(contextualPrompt).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            accumulated.append(piece)
                            val snapshot = accumulated.toString()
                            _uiState.update { state ->
                                state.copy(
                                    messages = state.messages.map { msg ->
                                        if (msg.id == assistantMsgId) {
                                            msg.copy(text = snapshot, isStreaming = true)
                                        } else {
                                            msg
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotBlank()) {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMsgId) {
                                    msg.copy(text = finalReply, isStreaming = false)
                                } else {
                                    msg
                                }
                            },
                            isResponding = false,
                            errorMessage = null,
                        )
                    }
                } else {
                    removePlaceholderAndSetError(assistantMsgId, "Assistant returned an empty response. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini chat timed out", e)
                removePlaceholderAndSetError(assistantMsgId, "Response timed out after 60 seconds. Please check your connection and try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI chat failed", e)
                removePlaceholderAndSetError(
                    assistantMsgId,
                    e.localizedMessage ?: "Unable to reach Gemini assistant. Check network or App Check configuration."
                )
            }
        }
    }

    fun resetConversation() {
        chatSession = null
        _uiState.value = SupportChatUiState()
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun removePlaceholderAndSetError(placeholderId: String, message: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.filterNot { it.id == placeholderId && it.text.isBlank() },
                isResponding = false,
                errorMessage = message,
            )
        }
    }

    /**
     * Builds a strictly sanitized snapshot of the user's current setup state (no passwords, no raw API keys).
     */
    private fun buildSanitizedContextHeader(
        profile: SetupProfile?,
        catalogs: List<CatalogCollectionItem>,
    ): String {
        if (profile == null) return "[Current Setup Context: No active profile loaded yet]"
        val enabledCatalogTitles = catalogs.filter { it.enabled }.sortedBy { it.sortOrder }.map { "${it.title} (${it.itemKind}/${it.sourceProvider})" }
        return buildString {
            appendLine("[Current User Setup Context — sanitized, no secrets included]")
            appendLine("- Active Profile: ${profile.profileName}")
            appendLine("- Nuvio Account: username=${if (profile.nuvioUsername.isNotBlank()) "set" else "missing"}, registered=${profile.nuvioAccountCreated}")
            appendLine("- Real-Debrid: keyEntered=${profile.realDebridApiKey.isNotBlank()}, verified=${profile.realDebridVerified}, label=${profile.realDebridUserLabel.ifBlank { "none" }}, premiumDays=${profile.realDebridPremiumDays}")
            appendLine("- Metadata Keys: TMDB=${profile.tmdbApiKey.isNotBlank()} (verified=${profile.tmdbVerified}), MDBList=${profile.mdblistApiKey.isNotBlank()}, RPDB=${profile.rpdbApiKey.isNotBlank()}")
            appendLine("- AIOStreams: instance=${profile.aioStreamsInstanceUrl}, builtInAddons=${profile.enabledAddons.joinToString(",")}, customSourcesCount=${profile.customStreamSources.size}, resolutions=${profile.preferredResolutions.joinToString(",")}, cachedOnly=${profile.cachedOnly}, maxSizeGb=${profile.maxFileSizeGb}, formatter=${profile.formatterPreset}, manifestSaved=${profile.aioStreamsManifestUrl.isNotBlank()}, manifestVerified=${profile.aioStreamsVerified}")
            appendLine("- AIOMetadata: instance=${profile.aioMetadataInstanceUrl}, movies=${profile.movieProvider}, series=${profile.seriesProvider}, anime=${profile.animeProvider}, rpdbPosters=${profile.rpdbPostersEnabled}, manifestSaved=${profile.aioMetadataManifestUrl.isNotBlank()}, manifestVerified=${profile.aioMetadataVerified}")
            appendLine("- Active Catalogs/Collections (${enabledCatalogTitles.size}): ${enabledCatalogTitles.take(10).joinToString("; ")}")
            appendLine("- Deploy Checklist: metaInstalled=${profile.nuvioMetaInstalled}, streamsInstalled=${profile.nuvioStreamsInstalled}, orderVerified=${profile.nuvioOrderVerified}")
        }
    }

    private companion object {
        const val TAG = "SupportChatVM"
    }
}
