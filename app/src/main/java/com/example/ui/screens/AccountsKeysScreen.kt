package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.ConsoleSectionCard
import com.example.ui.components.ExternalPortalButton
import com.example.ui.components.NumberedInstructionRow
import com.example.ui.components.SecretInputField
import com.example.ui.components.VerificationBanner
import com.example.ui.components.copyToClipboard
import com.example.ui.components.openExternalUrl
import com.example.ui.theme.AmberDebrid
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.IndigoMeta
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseAlert
import com.example.ui.viewmodel.SetupUiState
import java.security.SecureRandom

@Composable
fun AccountsKeysScreen(
    state: SetupUiState,
    onSaveNuvioAccount: (username: String, email: String, password: String, created: Boolean) -> Unit,
    onSaveApiKeys: (rdKey: String, rdReady: Boolean, tmdbKey: String, mdblistKey: String, rpdbKey: String) -> Unit,
    onVerifyRealDebrid: (String) -> Unit,
    onVerifyTmdb: (String) -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val profile = state.activeProfile ?: return

    var nuvioUsername by remember(profile.id) { mutableStateOf(profile.nuvioUsername) }
    var nuvioEmail by remember(profile.id) { mutableStateOf(profile.nuvioEmail) }
    var nuvioPassword by remember(profile.id) { mutableStateOf(profile.nuvioPasswordHint) }
    var nuvioCreated by remember(profile.id) { mutableStateOf(profile.nuvioAccountCreated) }

    var rdApiKey by remember(profile.id) { mutableStateOf(profile.realDebridApiKey) }
    var rdReady by remember(profile.id) { mutableStateOf(profile.realDebridAccountReady) }
    var tmdbKey by remember(profile.id) { mutableStateOf(profile.tmdbApiKey) }
    var mdblistKey by remember(profile.id) { mutableStateOf(profile.mdblistApiKey) }
    var rpdbKey by remember(profile.id) { mutableStateOf(profile.rpdbApiKey) }

    // Sync updates from Firestore verification callbacks
    LaunchedEffect(profile.realDebridAccountReady) {
        if (profile.realDebridAccountReady) rdReady = true
    }

    val passwordStrength = remember(nuvioPassword) { evaluatePasswordStrength(nuvioPassword) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // STEP 1: Nuvio Account Creation & Credentials
        item {
            ConsoleSectionCard(
                title = "1. Nuvio Account Setup",
                subtitle = "Prepare & store your Nuvio login credentials for this profile",
                icon = Icons.Filled.AccountCircle,
                accentColor = CyanSignal,
                badgeText = if (nuvioCreated) "CREATED" else "STEP 1",
            ) {
                NumberedInstructionRow(
                    stepNumber = 1,
                    text = "Choose a username, email, and strong password below for your Nuvio account.",
                    accentColor = CyanSignal,
                )
                NumberedInstructionRow(
                    stepNumber = 2,
                    text = "Open Nuvio on your TV, phone, or computer and tap Sign Up / Create Account.",
                    accentColor = CyanSignal,
                )
                NumberedInstructionRow(
                    stepNumber = 3,
                    text = "Copy your saved credentials below to register without typos, then check off completion.",
                    accentColor = CyanSignal,
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = nuvioUsername,
                    onValueChange = { nuvioUsername = it },
                    label = { Text("Nuvio Username") },
                    placeholder = { Text("e.g., alex_theater") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nuvio_username_input"),
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = nuvioEmail,
                    onValueChange = { nuvioEmail = it },
                    label = { Text("Nuvio Account Email") },
                    placeholder = { Text("you@example.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nuvio_email_input"),
                )
                Spacer(Modifier.height(10.dp))

                SecretInputField(
                    value = nuvioPassword,
                    onValueChange = { nuvioPassword = it },
                    label = "Nuvio Account Password",
                    placeholder = "Minimum 10 characters recommended",
                    testTagName = "nuvio_password_input",
                )

                Spacer(Modifier.height(8.dp))

                // Password Strength Bar + Generator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Password strength: ${passwordStrength.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = passwordStrength.color,
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { passwordStrength.score },
                            color = passwordStrength.color,
                            trackColor = ObsidianOutline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = {
                            nuvioPassword = generateStrongPassword()
                            onNotify("Generated strong password.")
                        },
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("generate_password_button"),
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Generate")
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Copy credentials row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = {
                            if (nuvioUsername.isNotBlank()) {
                                copyToClipboard(context, "Nuvio Username", nuvioUsername, onNotify)
                            } else {
                                onNotify("Enter a username first.")
                            }
                        },
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("copy_nuvio_username_button"),
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy User")
                    }
                    OutlinedButton(
                        onClick = {
                            if (nuvioPassword.isNotBlank()) {
                                copyToClipboard(context, "Nuvio Password", nuvioPassword, onNotify)
                            } else {
                                onNotify("Enter or generate a password first.")
                            }
                        },
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("copy_nuvio_password_button"),
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Pass")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Surface(
                    color = ObsidianSurfaceContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Checkbox(
                            checked = nuvioCreated,
                            onCheckedChange = { nuvioCreated = it },
                            modifier = Modifier.testTag("nuvio_created_checkbox"),
                        )
                        Text(
                            text = "I have created and signed into my Nuvio account",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        onSaveNuvioAccount(nuvioUsername, nuvioEmail, nuvioPassword, nuvioCreated)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("save_nuvio_account_button"),
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Nuvio Account to Profile")
                }
            }
        }

        // STEP 2: Real-Debrid Account & API Key Acquisition
        item {
            ConsoleSectionCard(
                title = "2. Real-Debrid Account & API Token",
                subtitle = "Enter once here — auto-propagated to every AIOStreams add-on",
                icon = Icons.Filled.Key,
                accentColor = AmberDebrid,
                badgeText = if (profile.realDebridVerified) "VERIFIED" else "REQUIRED",
            ) {
                NumberedInstructionRow(
                    stepNumber = 1,
                    text = "Register at real-debrid.com and confirm your email address.",
                    accentColor = AmberDebrid,
                )
                NumberedInstructionRow(
                    stepNumber = 2,
                    text = "Activate a Premium subscription (required for cached instant 4K streams).",
                    accentColor = AmberDebrid,
                )
                NumberedInstructionRow(
                    stepNumber = 3,
                    text = "Open real-debrid.com/apitoken, copy your Private API Token, and paste it below.",
                    accentColor = AmberDebrid,
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ExternalPortalButton(
                        label = "Real-Debrid",
                        onClick = { openExternalUrl(context, "https://real-debrid.com/", onNotify) },
                        testTagName = "open_rd_portal_button",
                        modifier = Modifier.weight(1f),
                    )
                    ExternalPortalButton(
                        label = "Get API Token",
                        onClick = { openExternalUrl(context, "https://real-debrid.com/apitoken", onNotify) },
                        testTagName = "open_rd_apitoken_button",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(12.dp))

                SecretInputField(
                    value = rdApiKey,
                    onValueChange = { rdApiKey = it },
                    label = "Real-Debrid Secret API Key",
                    placeholder = "Paste private token (no spaces)",
                    testTagName = "rd_api_key_input",
                    supportingText = "Automatically injected into all enabled AIOStreams stream sources.",
                )

                Spacer(Modifier.height(10.dp))

                VerificationBanner(
                    state = state.rdVerification,
                    persistedVerifiedLabel = if (profile.realDebridVerified) {
                        "Verified: ${profile.realDebridUserLabel} • ${profile.realDebridPremiumDays} days remaining"
                    } else null,
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = { onVerifyRealDebrid(rdApiKey) },
                        border = BorderStroke(1.dp, AmberDebrid.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("verify_rd_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = AmberDebrid,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Verify Live", color = AmberDebrid)
                    }
                    Surface(
                        color = ObsidianSurfaceContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Checkbox(
                                checked = rdReady,
                                onCheckedChange = { rdReady = it },
                                modifier = Modifier.testTag("rd_ready_checkbox"),
                            )
                            Text("Account Active", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // STEP 2B: Metadata & Collection API Keys Vault
        item {
            ConsoleSectionCard(
                title = "Metadata & Catalog API Keys",
                subtitle = "Power AIOMetadata with TMDB, MDBList collections, and RPDB rating posters",
                icon = Icons.Filled.CheckCircle,
                accentColor = IndigoMeta,
                badgeText = "AIOMETA KEYS",
            ) {
                // TMDB
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "TMDB API Key (Movies & TV Metadata)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ExternalPortalButton(
                        label = "TMDB Portal",
                        onClick = { openExternalUrl(context, "https://www.themoviedb.org/settings/api", onNotify) },
                        testTagName = "open_tmdb_portal_button",
                    )
                }
                Spacer(Modifier.height(8.dp))
                SecretInputField(
                    value = tmdbKey,
                    onValueChange = { tmdbKey = it },
                    label = "TMDB v3 Key or Read Access Token",
                    placeholder = "Free at themoviedb.org/settings/api",
                    testTagName = "tmdb_api_key_input",
                )
                Spacer(Modifier.height(8.dp))
                VerificationBanner(
                    state = state.tmdbVerification,
                    persistedVerifiedLabel = if (profile.tmdbVerified) "TMDB API key verified live" else null,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onVerifyTmdb(tmdbKey) },
                    border = BorderStroke(1.dp, IndigoMeta.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("verify_tmdb_button"),
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = IndigoMeta, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Verify TMDB API Key Live", color = IndigoMeta)
                }

                Spacer(Modifier.height(16.dp))

                // MDBList
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "MDBList API Key (Curated Collections)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ExternalPortalButton(
                        label = "MDBList",
                        onClick = { openExternalUrl(context, "https://mdblist.com/preferences/", onNotify) },
                        testTagName = "open_mdblist_portal_button",
                    )
                }
                Spacer(Modifier.height(8.dp))
                SecretInputField(
                    value = mdblistKey,
                    onValueChange = { mdblistKey = it },
                    label = "MDBList API Key",
                    placeholder = "Sync custom lists & watchlists into AIOMetadata",
                    testTagName = "mdblist_api_key_input",
                )

                Spacer(Modifier.height(16.dp))

                // RPDB
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "RPDB Key (Rating Posters — Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ExternalPortalButton(
                        label = "RPDB",
                        onClick = { openExternalUrl(context, "https://ratingposterdb.com/", onNotify) },
                        testTagName = "open_rpdb_portal_button",
                    )
                }
                Spacer(Modifier.height(8.dp))
                SecretInputField(
                    value = rpdbKey,
                    onValueChange = { rpdbKey = it },
                    label = "Rating Poster DB Key",
                    placeholder = "Optional: stamps IMDb/RT ratings onto posters",
                    testTagName = "rpdb_api_key_input",
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSaveApiKeys(rdApiKey, rdReady, tmdbKey, mdblistKey, rpdbKey)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberDebrid,
                        contentColor = ObsidianVoid,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("save_api_keys_button"),
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save All API Keys & Propagate")
                }
            }
        }
    }
}

private data class PasswordStrength(val score: Float, val label: String, val color: androidx.compose.ui.graphics.Color)

private fun evaluatePasswordStrength(password: String): PasswordStrength {
    if (password.isBlank()) return PasswordStrength(0.05f, "Not entered", ObsidianOutline)
    var pts = 0
    if (password.length >= 8) pts++
    if (password.length >= 12) pts++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) pts++
    if (password.any { it.isDigit() }) pts++
    if (password.any { !it.isLetterOrDigit() }) pts++
    return when {
        pts <= 2 -> PasswordStrength(0.3f, "Weak", RoseAlert)
        pts == 3 -> PasswordStrength(0.62f, "Fair", AmberDebrid)
        pts == 4 -> PasswordStrength(0.84f, "Strong", CyanSignal)
        else -> PasswordStrength(1.0f, "Excellent", EmeraldVerified)
    }
}

private fun generateStrongPassword(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*"
    val rnd = SecureRandom()
    return (1..14).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
}
