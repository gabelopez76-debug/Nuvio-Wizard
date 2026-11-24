package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.SetupProfile
import com.example.ui.components.SecretInputField
import com.example.ui.components.StatusPill
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.RoseAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagerSheet(
    userEmail: String?,
    profiles: List<SetupProfile>,
    activeProfile: SetupProfile?,
    onDismiss: () -> Unit,
    onSwitchProfile: (String) -> Unit,
    onCreateProfile: (name: String, nuvioUsername: String, nuvioEmail: String, nuvioPassword: String) -> Unit,
    onRenameActiveProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = CyanSignal,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nuvio Profiles",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = "Signed in as $userEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                text = "Each profile saves its own Nuvio credentials, Real-Debrid & metadata keys, AIOStreams add-ons, and AIOMetadata catalog layout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            profiles.forEach { profile ->
                val isCurrent = profile.id == activeProfile?.id
                Surface(
                    onClick = {
                        if (!isCurrent) onSwitchProfile(profile.id)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCurrent) ObsidianSurfaceContainer else ObsidianSurface,
                    border = BorderStroke(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = if (isCurrent) CyanSignal else ObsidianOutline,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_item_${profile.id}"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.profileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (isCurrent) {
                                    Spacer(Modifier.width(8.dp))
                                    StatusPill(text = "ACTIVE", color = EmeraldVerified)
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = buildString {
                                    append("Nuvio: ")
                                    append(profile.nuvioUsername.ifBlank { "Not set" })
                                    append(" • ")
                                    append("${profile.enabledAddons.size + profile.customStreamSources.size} stream sources")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isCurrent) {
                            IconButton(
                                onClick = { showRenameDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("rename_profile_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Rename profile",
                                    tint = CyanSignal,
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Switch to profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        if (profiles.size > 1) {
                            IconButton(
                                onClick = { onDeleteProfile(profile.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_profile_${profile.id}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete profile",
                                    tint = RoseAlert,
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("create_profile_button"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create New Nuvio Profile")
            }

            HorizontalDivider(color = ObsidianOutline)

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onSignOut()
                },
                border = BorderStroke(1.dp, RoseAlert.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("sign_out_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = RoseAlert,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = RoseAlert)
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var email by remember { mutableStateOf(userEmail.orEmpty()) }
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Nuvio Setup Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a profile label and optional initial Nuvio account details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Profile Name") },
                        placeholder = { Text("e.g., Bedroom 4K TV") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_profile_name_input"),
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Nuvio Username") },
                        placeholder = { Text("e.g., cinema_pro") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_profile_username_input"),
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Nuvio Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecretInputField(
                        value = password,
                        onValueChange = { password = it },
                        label = { "Nuvio Password" }.invoke(),
                        placeholder = "Choose a strong password",
                        testTagName = "new_profile_password_input",
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreateProfile(name, username, email, password)
                            showCreateDialog = false
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.testTag("confirm_create_profile_button"),
                ) {
                    Text("Create Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showRenameDialog && activeProfile != null) {
        var editedName by remember(activeProfile.id) { mutableStateOf(activeProfile.profileName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Profile") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            onRenameActiveProfile(editedName)
                            showRenameDialog = false
                        }
                    },
                    enabled = editedName.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
