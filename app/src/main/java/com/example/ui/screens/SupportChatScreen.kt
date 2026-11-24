package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.components.StatusPill
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.IndigoMeta
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.RoseContainer
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.SetupUiState
import com.example.ui.viewmodel.SupportChatUiState

private val QUICK_PROMPTS = listOf(
    "Check what I still need to finish in my setup",
    "Why aren't Real-Debrid cached streams showing?",
    "How does AIOStreams configure all add-ons once?",
    "Recommend a 4K catalog & collection setup for AIOMetadata",
    "Fix slow loading in Nuvio",
)

@Composable
fun SupportChatScreen(
    setupState: SetupUiState,
    chatState: SupportChatUiState,
    onSendMessage: (String) -> Unit,
    onResetChat: () -> Unit,
    onDismissError: () -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatState.messages.size, chatState.messages.lastOrNull()?.text?.length) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        // Context-aware status header
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianOutline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(IndigoMeta.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = IndigoMeta,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nuvio AI Support Agent",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Context-aware • Profile: ${setupState.activeProfile?.profileName ?: "Default"} (${setupState.progress.percent}% ready)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onResetChat,
                    enabled = !chatState.isResponding,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("reset_chat_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Start new conversation",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            items(chatState.messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }

            if (!chatState.errorMessage.isNullOrBlank()) {
                item {
                    Surface(
                        color = RoseContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RoseAlert.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = "Chat error",
                                tint = RoseAlert,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = chatState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = RoseAlert,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onDismissError) {
                                Text("Dismiss", color = RoseAlert)
                            }
                        }
                    }
                }
            }
        }

        // Quick Troubleshooting Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            QUICK_PROMPTS.forEachIndexed { idx, prompt ->
                AssistChip(
                    onClick = {
                        if (!chatState.isResponding) {
                            onSendMessage(prompt)
                        }
                    },
                    label = { Text(prompt, style = MaterialTheme.typography.labelMedium) },
                    enabled = !chatState.isResponding,
                    modifier = Modifier.testTag("quick_prompt_$idx"),
                )
            }
        }

        // Composer Row
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianOutline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about Real-Debrid, AIOStreams, AIOMetadata...") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !chatState.isResponding) {
                                val toSend = inputText
                                inputText = ""
                                onSendMessage(toSend)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanSignal,
                        unfocusedBorderColor = ObsidianOutline,
                        focusedContainerColor = ObsidianVoid,
                        unfocusedContainerColor = ObsidianVoid,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input"),
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = if (inputText.isNotBlank() && !chatState.isResponding) CyanSignal else ObsidianSurfaceContainer,
                ) {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !chatState.isResponding) {
                                val toSend = inputText
                                inputText = ""
                                onSendMessage(toSend)
                            }
                        },
                        enabled = inputText.isNotBlank() && !chatState.isResponding,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("send_chat_button"),
                    ) {
                        if (chatState.isResponding) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CyanSignal,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = if (inputText.isNotBlank()) ObsidianVoid else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (message.isUser) CyanContainer else ObsidianSurface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp,
            ),
            border = BorderStroke(
                1.dp,
                if (message.isUser) CyanSignal.copy(alpha = 0.5f) else ObsidianOutline,
            ),
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusPill(
                        text = if (message.isUser) "YOU" else "GEMINI SUPPORT",
                        color = if (message.isUser) CyanSignal else IndigoMeta,
                    )
                    if (message.isStreaming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.8.dp,
                            color = IndigoMeta,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = message.text.ifBlank { "Thinking..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
