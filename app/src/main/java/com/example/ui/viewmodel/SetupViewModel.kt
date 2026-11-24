package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AddonIntegrationMode
import com.example.data.model.CatalogCollectionItem
import com.example.data.model.CatalogPresetSpec
import com.example.data.model.MarketplaceAddonSpec
import com.example.data.model.SetupCatalogDefaults
import com.example.data.model.SetupProfile
import com.example.data.model.VerificationState
import com.example.data.network.VerificationService
import com.example.data.repository.SetupRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DataLoadState<out T> {
    data object Loading : DataLoadState<Nothing>
    data class Ready<T>(val data: T) : DataLoadState<T>
    data class Error(val message: String) : DataLoadState<Nothing>
}

data class ProgressSubTask(
    val id: String,
    val label: String,
    val completed: Boolean,
    val required: Boolean = true,
)

data class SetupMilestoneProgress(
    val stepNumber: Int,
    val id: String,
    val shortLabel: String,
    val title: String,
    val subtitle: String,
    val route: String,
    val subTasks: List<ProgressSubTask>,
    val isCompleted: Boolean,
) {
    val requiredSubTasks: List<ProgressSubTask> get() = subTasks.filter { it.required }
    val completedRequiredCount: Int get() = requiredSubTasks.count { it.completed }
    val totalRequiredCount: Int get() = requiredSubTasks.size
    val fraction: Float
        get() = if (totalRequiredCount == 0) 0f else completedRequiredCount.toFloat() / totalRequiredCount.toFloat()
    val inProgress: Boolean get() = !isCompleted && completedRequiredCount > 0
}

data class SetupProgressSummary(
    val completedSteps: Int,
    val totalSteps: Int = 5,
    val nuvioDone: Boolean,
    val debridDone: Boolean,
    val aioStreamsDone: Boolean,
    val aioMetaDone: Boolean,
    val deployDone: Boolean,
    val milestones: List<SetupMilestoneProgress> = emptyList(),
    val completedTasks: Int = 0,
    val totalTasks: Int = 13,
    val currentActiveStepNumber: Int = 1,
    val nextPendingTask: String = "Create your Nuvio account",
) {
    val fraction: Float get() = completedSteps.toFloat() / totalSteps.toFloat()
    val percent: Int get() = (fraction * 100).toInt()
    val granularFraction: Float
        get() = if (totalTasks <= 0) fraction else (completedTasks.toFloat() / totalTasks.toFloat()).coerceIn(0f, 1f)
    val granularPercent: Int get() = (granularFraction * 100).toInt()
}

data class SetupUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val profiles: List<SetupProfile> = emptyList(),
    val activeProfile: SetupProfile? = null,
    val activeCatalogs: List<CatalogCollectionItem> = emptyList(),
    val progress: SetupProgressSummary = SetupProgressSummary(0, 5, false, false, false, false, false),
    val rdVerification: VerificationState = VerificationState.Idle,
    val tmdbVerification: VerificationState = VerificationState.Idle,
    val aioStreamsManifestVerification: VerificationState = VerificationState.Idle,
    val aioMetaManifestVerification: VerificationState = VerificationState.Idle,
    val snackbarMessage: String? = null,
    val isSaving: Boolean = false,
)

class SetupViewModel(
    private val repository: SetupRepository,
    private val verificationService: VerificationService,
    private val currentUserId: String,
) : ViewModel() {

    private val _rdVerification = MutableStateFlow<VerificationState>(VerificationState.Idle)
    private val _tmdbVerification = MutableStateFlow<VerificationState>(VerificationState.Idle)
    private val _streamsManifestVerification = MutableStateFlow<VerificationState>(VerificationState.Idle)
    private val _metaManifestVerification = MutableStateFlow<VerificationState>(VerificationState.Idle)
    private val _marketplaceVerification = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val marketplaceVerification: StateFlow<VerificationState> = _marketplaceVerification.asStateFlow()
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _isSaving = MutableStateFlow(false)
    private var hasAttemptedDefaultSeed = false

    private val profilesState: StateFlow<DataLoadState<List<SetupProfile>>> =
        repository.observeProfiles(currentUserId)
            .map<List<SetupProfile>, DataLoadState<List<SetupProfile>>> { DataLoadState.Ready(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing profiles", rootError)
                emit(DataLoadState.Error(rootError.message ?: "Failed to sync profiles"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = DataLoadState.Loading,
            )

    private val catalogsState: StateFlow<DataLoadState<List<CatalogCollectionItem>>> =
        repository.observeCatalogs(currentUserId)
            .map<List<CatalogCollectionItem>, DataLoadState<List<CatalogCollectionItem>>> { DataLoadState.Ready(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing catalogs", rootError)
                emit(DataLoadState.Error(rootError.message ?: "Failed to sync catalogs"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = DataLoadState.Loading,
            )

    private val verificationBundle = combine(
        _rdVerification,
        _tmdbVerification,
        _streamsManifestVerification,
        _metaManifestVerification,
        _snackbarMessage,
    ) { rd, tmdb, streams, meta, snack ->
        VerificationBundle(rd, tmdb, streams, meta, snack)
    }

    val uiState: StateFlow<SetupUiState> = combine(
        profilesState,
        catalogsState,
        verificationBundle,
        _isSaving,
    ) { profLoad, catLoad, ver, saving ->
        when {
            profLoad is DataLoadState.Error -> SetupUiState(
                isLoading = false,
                errorMessage = profLoad.message,
                snackbarMessage = ver.snackbar,
            )
            catLoad is DataLoadState.Error -> SetupUiState(
                isLoading = false,
                errorMessage = catLoad.message,
                snackbarMessage = ver.snackbar,
            )
            profLoad is DataLoadState.Loading || catLoad is DataLoadState.Loading -> SetupUiState(
                isLoading = true,
                snackbarMessage = ver.snackbar,
            )
            else -> {
                val profiles = (profLoad as DataLoadState.Ready).data
                val allCatalogs = (catLoad as DataLoadState.Ready).data
                if (profiles.isEmpty() && !hasAttemptedDefaultSeed) {
                    seedInitialDefaultProfile()
                }
                val active = profiles.firstOrNull { it.activeProfile } ?: profiles.firstOrNull()
                val activeCatalogs = if (active != null) {
                    allCatalogs.filter { it.profileId == active.id }.sortedBy { it.sortOrder }
                } else {
                    emptyList()
                }
                SetupUiState(
                    isLoading = profiles.isEmpty() && !hasAttemptedDefaultSeed,
                    errorMessage = null,
                    profiles = profiles,
                    activeProfile = active,
                    activeCatalogs = activeCatalogs,
                    progress = computeProgress(active, activeCatalogs),
                    rdVerification = ver.rd,
                    tmdbVerification = ver.tmdb,
                    aioStreamsManifestVerification = ver.streams,
                    aioMetaManifestVerification = ver.meta,
                    snackbarMessage = ver.snackbar,
                    isSaving = saving,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SetupUiState(isLoading = true),
    )

    private fun seedInitialDefaultProfile() {
        hasAttemptedDefaultSeed = true
        viewModelScope.launch {
            val created = repository.createProfile(
                userId = currentUserId,
                profileName = "Main Theater Setup",
                activeProfile = true,
            )
            created.onSuccess { newProfileId ->
                val balancedPack = SetupCatalogDefaults.starterPacks.first { it.id == "balanced" }
                repository.applyCatalogStarterPack(
                    userId = currentUserId,
                    profileId = newProfileId,
                    existingForProfile = emptyList(),
                    pack = balancedPack,
                )
            }
        }
    }

    private fun computeProgress(
        profile: SetupProfile?,
        catalogs: List<CatalogCollectionItem>,
    ): SetupProgressSummary {
        if (profile == null) {
            return SetupProgressSummary(0, 5, false, false, false, false, false)
        }

        val hasUsername = profile.nuvioUsername.isNotBlank()
        val hasPassword = profile.nuvioPasswordHint.isNotBlank()
        val nuvioCreated = profile.nuvioAccountCreated
        val nuvioDone = hasUsername && hasPassword && nuvioCreated

        val hasRdKey = profile.realDebridApiKey.isNotBlank()
        val rdReadyOrVerified = profile.realDebridVerified || profile.realDebridAccountReady
        val hasTmdbKey = profile.tmdbApiKey.isNotBlank()
        val debridDone = hasRdKey && rdReadyOrVerified

        val totalStreamModules = profile.enabledAddons.size + profile.customStreamSources.size
        val hasStreamModules = totalStreamModules > 0
        val hasStreamsManifest = profile.aioStreamsManifestUrl.isNotBlank()
        val aioStreamsDone = hasRdKey && hasStreamModules && hasStreamsManifest

        val enabledCatalogCount = catalogs.count { it.enabled }
        val hasActiveCatalogs = enabledCatalogCount > 0
        val hasMetaManifest = profile.aioMetadataManifestUrl.isNotBlank()
        val aioMetaDone = hasActiveCatalogs && hasMetaManifest

        val deployDone = profile.nuvioMetaInstalled &&
            profile.nuvioStreamsInstalled &&
            profile.nuvioOrderVerified

        val milestones = listOf(
            SetupMilestoneProgress(
                stepNumber = 1,
                id = "nuvio_account",
                shortLabel = "Account",
                title = "Create Nuvio Account",
                subtitle = if (hasUsername) {
                    "Account: ${profile.nuvioUsername} (${if (nuvioCreated) "Registered" else "Credentials drafted"})"
                } else {
                    "Set your username, email & password for Nuvio onboarding."
                },
                route = "accounts",
                subTasks = listOf(
                    ProgressSubTask("nuvio_username", "Enter Nuvio username & email", hasUsername),
                    ProgressSubTask("nuvio_password", "Save Nuvio password in profile vault", hasPassword),
                    ProgressSubTask("nuvio_created", "Confirm Nuvio account registration", nuvioCreated),
                ),
                isCompleted = nuvioDone,
            ),
            SetupMilestoneProgress(
                stepNumber = 2,
                id = "real_debrid",
                shortLabel = "Debrid",
                title = "Real-Debrid & API Keys Vault",
                subtitle = if (profile.realDebridVerified) {
                    "Verified: ${profile.realDebridUserLabel} (${profile.realDebridPremiumDays}d premium)"
                } else {
                    "Set up Real-Debrid, grab Secret API Token, plus TMDB, MDBList & RPDB."
                },
                route = "accounts",
                subTasks = listOf(
                    ProgressSubTask("rd_token", "Save Real-Debrid Secret API Token", hasRdKey),
                    ProgressSubTask("rd_ready", "Verify Real-Debrid Premium connection", rdReadyOrVerified),
                    ProgressSubTask("tmdb_key", "Add TMDB API key for enhanced metadata", hasTmdbKey, required = false),
                ),
                isCompleted = debridDone,
            ),
            SetupMilestoneProgress(
                stepNumber = 3,
                id = "aiostreams",
                shortLabel = "Streams",
                title = "AIOStreams Unified Stream Hub",
                subtitle = "$totalStreamModules stream add-on(s) inheriting your single Real-Debrid configuration.",
                route = "aiostreams",
                subTasks = listOf(
                    ProgressSubTask("streams_rd_sync", "Auto-sync master Real-Debrid token to hub", hasRdKey),
                    ProgressSubTask("streams_modules", "Enable at least one stream add-on module", hasStreamModules),
                    ProgressSubTask("streams_manifest", "Paste generated AIOStreams manifest.json URL", hasStreamsManifest),
                    ProgressSubTask("streams_verified", "Ping AIOStreams manifest over HTTPS", profile.aioStreamsVerified, required = false),
                ),
                isCompleted = aioStreamsDone,
            ),
            SetupMilestoneProgress(
                stepNumber = 4,
                id = "aiometadata",
                shortLabel = "Meta",
                title = "AIOMetadata, Catalogs & Collections",
                subtitle = "$enabledCatalogCount active home rows • ${profile.movieProvider} & ${profile.seriesProvider} metadata.",
                route = "aiometadata",
                subTasks = listOf(
                    ProgressSubTask("meta_catalogs", "Organize at least one active catalog or collection", hasActiveCatalogs),
                    ProgressSubTask("meta_manifest", "Paste generated AIOMetadata manifest.json URL", hasMetaManifest),
                    ProgressSubTask("meta_verified", "Ping AIOMetadata manifest over HTTPS", profile.aioMetadataVerified, required = false),
                ),
                isCompleted = aioMetaDone,
            ),
            SetupMilestoneProgress(
                stepNumber = 5,
                id = "deploy",
                shortLabel = "Deploy",
                title = "Deploy & Verify Order in Nuvio",
                subtitle = "Install AIOMetadata #1 on top, AIOStreams #2 below, and export backup.",
                route = "deploy",
                subTasks = listOf(
                    ProgressSubTask("deploy_meta", "Install AIOMetadata manifest as Add-on #1", profile.nuvioMetaInstalled),
                    ProgressSubTask("deploy_streams", "Install AIOStreams manifest as Add-on #2", profile.nuvioStreamsInstalled),
                    ProgressSubTask("deploy_order", "Verify add-on order & test playback in Nuvio", profile.nuvioOrderVerified),
                ),
                isCompleted = deployDone,
            ),
        )

        val completedSteps = milestones.count { it.isCompleted }
        val requiredTasks = milestones.flatMap { it.requiredSubTasks }
        val completedTasks = requiredTasks.count { it.completed }
        val totalTasks = requiredTasks.size
        val nextIncompleteMilestone = milestones.firstOrNull { !it.isCompleted }
        val currentActiveStepNumber = nextIncompleteMilestone?.stepNumber ?: 5
        val nextPendingTask = nextIncompleteMilestone
            ?.requiredSubTasks
            ?.firstOrNull { !it.completed }
            ?.let { "Next: ${it.label}" }
            ?: "All $totalTasks setup checks verified — Ready to stream!"

        return SetupProgressSummary(
            completedSteps = completedSteps,
            totalSteps = milestones.size,
            nuvioDone = nuvioDone,
            debridDone = debridDone,
            aioStreamsDone = aioStreamsDone,
            aioMetaDone = aioMetaDone,
            deployDone = deployDone,
            milestones = milestones,
            completedTasks = completedTasks,
            totalTasks = totalTasks,
            currentActiveStepNumber = currentActiveStepNumber,
            nextPendingTask = nextPendingTask,
        )
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    // --- Profile Management ---
    fun createNewProfile(
        profileName: String,
        nuvioUsername: String,
        nuvioEmail: String,
        nuvioPassword: String,
    ) {
        val currentProfiles = uiState.value.profiles
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.createProfile(
                userId = currentUserId,
                profileName = profileName,
                nuvioUsername = nuvioUsername,
                nuvioEmail = nuvioEmail,
                nuvioPasswordHint = nuvioPassword,
                activeProfile = true,
            )
            result.onSuccess { newId ->
                if (currentProfiles.isNotEmpty()) {
                    repository.switchActiveProfile(currentUserId, currentProfiles, newId)
                }
                val balancedPack = SetupCatalogDefaults.starterPacks.first { it.id == "balanced" }
                repository.applyCatalogStarterPack(currentUserId, newId, emptyList(), balancedPack)
                _snackbarMessage.value = "Profile '$profileName' created and activated."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to create profile."
            }
            _isSaving.value = false
        }
    }

    fun switchProfile(targetProfileId: String) {
        val profiles = uiState.value.profiles
        viewModelScope.launch {
            _rdVerification.value = VerificationState.Idle
            _tmdbVerification.value = VerificationState.Idle
            _streamsManifestVerification.value = VerificationState.Idle
            _metaManifestVerification.value = VerificationState.Idle
            repository.switchActiveProfile(currentUserId, profiles, targetProfileId)
                .onSuccess { _snackbarMessage.value = "Switched configuration profile." }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not switch profile." }
        }
    }

    fun renameActiveProfile(newName: String) {
        val active = uiState.value.activeProfile ?: return
        viewModelScope.launch {
            repository.renameProfile(currentUserId, active.id, newName)
                .onSuccess { _snackbarMessage.value = "Profile renamed to '$newName'." }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Rename failed." }
        }
    }

    fun deleteProfile(profileId: String) {
        val profiles = uiState.value.profiles
        if (profiles.size <= 1) {
            _snackbarMessage.value = "Keep at least one active setup profile."
            return
        }
        val catalogsToDelete = (catalogsState.value as? DataLoadState.Ready)
            ?.data
            ?.filter { it.profileId == profileId }
            ?.map { it.id }
            ?: emptyList()
        viewModelScope.launch {
            val remaining = profiles.filter { it.id != profileId }
            repository.deleteProfile(currentUserId, profileId, catalogsToDelete)
                .onSuccess {
                    remaining.firstOrNull()?.let { fallback ->
                        repository.switchActiveProfile(currentUserId, remaining, fallback.id)
                    }
                    _snackbarMessage.value = "Profile deleted."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Delete failed." }
        }
    }

    // --- Step 1: Nuvio Account ---
    fun saveNuvioAccount(
        username: String,
        email: String,
        password: String,
        accountCreated: Boolean,
    ) {
        val active = uiState.value.activeProfile ?: return
        viewModelScope.launch {
            _isSaving.value = true
            repository.updateNuvioAccount(
                userId = currentUserId,
                profileId = active.id,
                username = username,
                email = email,
                passwordHint = password,
                accountCreated = accountCreated,
            ).onSuccess {
                _snackbarMessage.value = "Nuvio account details saved to your profile."
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to save Nuvio account."
            }
            _isSaving.value = false
        }
    }

    // --- Step 2: Real-Debrid & API Keys Vault ---
    fun saveApiKeysVault(
        realDebridApiKey: String,
        realDebridAccountReady: Boolean,
        tmdbApiKey: String,
        mdblistApiKey: String,
        rpdbApiKey: String,
    ) {
        val active = uiState.value.activeProfile ?: return
        val rdChanged = realDebridApiKey.trim() != active.realDebridApiKey
        val tmdbChanged = tmdbApiKey.trim() != active.tmdbApiKey
        viewModelScope.launch {
            _isSaving.value = true
            repository.updateApiKeysVault(
                userId = currentUserId,
                profileId = active.id,
                realDebridApiKey = realDebridApiKey,
                realDebridAccountReady = realDebridAccountReady,
                realDebridVerified = if (rdChanged) false else active.realDebridVerified,
                realDebridUserLabel = if (rdChanged) "" else active.realDebridUserLabel,
                realDebridPremiumDays = if (rdChanged) 0L else active.realDebridPremiumDays,
                tmdbApiKey = tmdbApiKey,
                tmdbVerified = if (tmdbChanged) false else active.tmdbVerified,
                mdblistApiKey = mdblistApiKey,
                rpdbApiKey = rpdbApiKey,
            ).onSuccess {
                _snackbarMessage.value = "API keys saved and auto-synced to AIOStreams & AIOMetadata."
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to save API keys."
            }
            _isSaving.value = false
        }
    }

    fun verifyRealDebrid(apiKey: String) {
        val active = uiState.value.activeProfile ?: return
        viewModelScope.launch {
            _rdVerification.value = VerificationState.Checking
            val res = verificationService.verifyRealDebridKey(apiKey)
            if (res.success) {
                _rdVerification.value = VerificationState.Verified(res.message)
                repository.updateApiKeysVault(
                    userId = currentUserId,
                    profileId = active.id,
                    realDebridApiKey = apiKey,
                    realDebridAccountReady = true,
                    realDebridVerified = true,
                    realDebridUserLabel = res.userLabel,
                    realDebridPremiumDays = res.premiumDays,
                    tmdbApiKey = active.tmdbApiKey,
                    tmdbVerified = active.tmdbVerified,
                    mdblistApiKey = active.mdblistApiKey,
                    rpdbApiKey = active.rpdbApiKey,
                )
            } else {
                _rdVerification.value = VerificationState.Failed(res.message)
            }
        }
    }

    fun verifyTmdb(apiKey: String) {
        val active = uiState.value.activeProfile ?: return
        viewModelScope.launch {
            _tmdbVerification.value = VerificationState.Checking
            val res = verificationService.verifyTmdbKey(apiKey)
            if (res.success) {
                _tmdbVerification.value = VerificationState.Verified(res.message)
                repository.updateApiKeysVault(
                    userId = currentUserId,
                    profileId = active.id,
                    realDebridApiKey = active.realDebridApiKey,
                    realDebridAccountReady = active.realDebridAccountReady,
                    realDebridVerified = active.realDebridVerified,
                    realDebridUserLabel = active.realDebridUserLabel,
                    realDebridPremiumDays = active.realDebridPremiumDays,
                    tmdbApiKey = apiKey,
                    tmdbVerified = true,
                    mdblistApiKey = active.mdblistApiKey,
                    rpdbApiKey = active.rpdbApiKey,
                )
            } else {
                _tmdbVerification.value = VerificationState.Failed(res.message)
            }
        }
    }

    // --- Step 3: AIOStreams Single-Config Hub ---
    fun saveAioStreamsConfig(
        instanceUrl: String,
        password: String,
        enabledAddons: List<String>,
        customStreamSources: List<String>,
        preferredResolutions: List<String>,
        cachedOnly: Boolean,
        excludeCam: Boolean,
        preferDolbyVision: Boolean,
        maxFileSizeGb: Long,
        formatterPreset: String,
        manifestUrl: String,
    ) {
        val active = uiState.value.activeProfile ?: return
        val manifestChanged = manifestUrl.trim() != active.aioStreamsManifestUrl
        viewModelScope.launch {
            _isSaving.value = true
            repository.updateAioStreamsConfig(
                userId = currentUserId,
                profileId = active.id,
                instanceUrl = instanceUrl,
                password = password,
                enabledAddons = enabledAddons,
                customStreamSources = customStreamSources,
                preferredResolutions = preferredResolutions,
                cachedOnly = cachedOnly,
                excludeCam = excludeCam,
                preferDolbyVision = preferDolbyVision,
                maxFileSizeGb = maxFileSizeGb,
                formatterPreset = formatterPreset,
                manifestUrl = manifestUrl,
                manifestVerified = if (manifestChanged) false else active.aioStreamsVerified,
            ).onSuccess {
                val totalAddons = enabledAddons.size + customStreamSources.size
                _snackbarMessage.value = "AIOStreams updated! $totalAddons stream source(s) inherit your master config."
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to save AIOStreams config."
            }
            _isSaving.value = false
        }
    }

    fun verifyAioStreamsManifest(url: String) {
        val active = uiState.value.activeProfile ?: return
        viewModelScope.launch {
            _streamsManifestVerification.value = VerificationState.Checking
            val res = verificationService.verifyAddonManifest(url)
            if (res.success) {
                _streamsManifestVerification.value = VerificationState.Verified(res.message)
                repository.updateAioStreamsConfig(
                    userId = currentUserId,
                    profileId = active.id,
                    instanceUrl = active.aioStreamsInstanceUrl,
                    password = active.aioStreamsPassword,
                    enabledAddons = active.enabledAddons,
                    customStreamSources = active.customStreamSources,
                    preferredResolutions = active.preferredResolutions,
                    cachedOnly = active.cachedOnly,
                    excludeCam = active.excludeCam,
                    preferDolbyVision = active.preferDolbyVision,
                    maxFileSizeGb = active.maxFileSizeGb,
                    formatterPreset = active.formatterPreset,
                    manifestUrl = url,
                    manifestVerified = true,
                )
            } else {
                _streamsManifestVerification.value = VerificationState.Failed(res.message)
            }
        }
    }

    // --- Step 4: AIOMetadata & Catalogs/Collections ---
    fun saveAioMetadataConfig(
        instanceUrl: String,
        movieProvider: String,
        seriesProvider: String,
        animeProvider: String,
        rpdbPostersEnabled: Boolean,
        animeCatalogsEnabled: Boolean,
        manifestUrl: String,
    ) {
        val active = uiState.value.activeProfile ?: return
        val manifestChanged = manifestUrl.trim() != active.aioMetadataManifestUrl
        viewModelScope.launch {
            _isSaving.value = true
            repository.updateAioMetadataConfig(
                userId = currentUserId,
                profileId = active.id,
                instanceUrl = instanceUrl,
                movieProvider = movieProvider,
                seriesProvider = seriesProvider,
                animeProvider = animeProvider,
                rpdbPostersEnabled = rpdbPostersEnabled,
                animeCatalogsEnabled = animeCatalogsEnabled,
                manifestUrl = manifestUrl,
                manifestVerified = if (manifestChanged) false else active.aioMetadataVerified,
            ).onSuccess {
                _snackbarMessage.value = "AIOMetadata provider settings saved."
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to save AIOMetadata settings."
            }
            _isSaving.value = false
        }
    }

    fun verifyAioMetadataManifest(url: String) {
        val active = uiState.value.activeProfile ?: return
        viewModelScope.launch {
            _metaManifestVerification.value = VerificationState.Checking
            val res = verificationService.verifyAddonManifest(url)
            if (res.success) {
                _metaManifestVerification.value = VerificationState.Verified(res.message)
                repository.updateAioMetadataConfig(
                    userId = currentUserId,
                    profileId = active.id,
                    instanceUrl = active.aioMetadataInstanceUrl,
                    movieProvider = active.movieProvider,
                    seriesProvider = active.seriesProvider,
                    animeProvider = active.animeProvider,
                    rpdbPostersEnabled = active.rpdbPostersEnabled,
                    animeCatalogsEnabled = active.animeCatalogsEnabled,
                    manifestUrl = url,
                    manifestVerified = true,
                )
            } else {
                _metaManifestVerification.value = VerificationState.Failed(res.message)
            }
        }
    }

    fun addCatalogOrCollection(
        title: String,
        mediaType: String,
        itemKind: String,
        sourceProvider: String,
        sourceReference: String,
        description: String,
    ) {
        val active = uiState.value.activeProfile ?: return
        val nextSortOrder = (uiState.value.activeCatalogs.maxOfOrNull { it.sortOrder } ?: -1L) + 1L
        viewModelScope.launch {
            repository.addCatalogItem(
                userId = currentUserId,
                profileId = active.id,
                title = title,
                mediaType = mediaType,
                itemKind = itemKind,
                sourceProvider = sourceProvider,
                sourceReference = sourceReference,
                description = description,
                sortOrder = nextSortOrder,
            ).onSuccess {
                _snackbarMessage.value = "Added '$title' to your AIOMetadata board."
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Could not add item."
            }
        }
    }

    fun toggleCatalog(catalogId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleCatalogEnabled(currentUserId, catalogId, enabled)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to toggle row." }
        }
    }

    fun moveCatalog(index: Int, direction: Int) {
        val list = uiState.value.activeCatalogs
        val targetIdx = index + direction
        if (index !in list.indices || targetIdx !in list.indices) return
        val first = list[index]
        val second = list[targetIdx]
        viewModelScope.launch {
            repository.swapCatalogOrder(currentUserId, first, second)
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to reorder." }
        }
    }

    fun deleteCatalog(catalogId: String) {
        viewModelScope.launch {
            repository.deleteCatalogItem(currentUserId, catalogId)
                .onSuccess { _snackbarMessage.value = "Removed row from AIOMetadata board." }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Delete failed." }
        }
    }

    fun applyStarterPack(pack: CatalogPresetSpec) {
        val active = uiState.value.activeProfile ?: return
        val existing = uiState.value.activeCatalogs
        viewModelScope.launch {
            _isSaving.value = true
            repository.applyCatalogStarterPack(currentUserId, active.id, existing, pack)
                .onSuccess { _snackbarMessage.value = "Applied '${pack.name}' starter pack (${pack.items.size} rows)." }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to apply starter pack." }
            _isSaving.value = false
        }
    }

    // --- Step 5: Deploy Checklist ---
    fun updateDeployChecklist(
        metaInstalled: Boolean,
        streamsInstalled: Boolean,
        orderVerified: Boolean,
    ) {
        val active = uiState.value.activeProfile ?: return
        viewModelScope.launch {
            repository.updateDeployChecklist(
                userId = currentUserId,
                profileId = active.id,
                metaInstalled = metaInstalled,
                streamsInstalled = streamsInstalled,
                orderVerified = orderVerified,
            ).onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to update checklist."
            }
        }
    }

    // --- Compatible Nuvio / Stremio Add-on Marketplace ---
    fun toggleMarketplaceStreamAddon(addon: MarketplaceAddonSpec) {
        val active = uiState.value.activeProfile ?: return
        viewModelScope.launch {
            _isSaving.value = true
            var updatedEnabled = active.enabledAddons
            var updatedCustom = active.customStreamSources
            val actionMessage: String

            when (addon.integrationMode) {
                AddonIntegrationMode.AIOSTREAMS_BUILTIN -> {
                    val builtinId = addon.builtinAddonId ?: return@launch
                    if (builtinId in updatedEnabled) {
                        updatedEnabled = updatedEnabled - builtinId
                        actionMessage = "Removed ${addon.name} from AIOStreams."
                    } else {
                        updatedEnabled = (updatedEnabled + builtinId).distinct()
                        actionMessage = "Enabled ${addon.name} in AIOStreams — inherits your single Real-Debrid config!"
                    }
                }
                AddonIntegrationMode.AIOSTREAMS_CUSTOM -> {
                    val url = addon.manifestUrl.trim()
                    if (url in updatedCustom) {
                        updatedCustom = updatedCustom - url
                        actionMessage = "Removed ${addon.name} from AIOStreams custom sources."
                    } else {
                        updatedCustom = (updatedCustom + url).distinct()
                        actionMessage = "Added ${addon.name} to AIOStreams — inherits your master stream settings!"
                    }
                }
                else -> {
                    _isSaving.value = false
                    return@launch
                }
            }

            repository.updateAioStreamsConfig(
                userId = currentUserId,
                profileId = active.id,
                instanceUrl = active.aioStreamsInstanceUrl,
                password = active.aioStreamsPassword,
                enabledAddons = updatedEnabled,
                customStreamSources = updatedCustom,
                preferredResolutions = active.preferredResolutions,
                cachedOnly = active.cachedOnly,
                excludeCam = active.excludeCam,
                preferDolbyVision = active.preferDolbyVision,
                maxFileSizeGb = active.maxFileSizeGb,
                formatterPreset = active.formatterPreset,
                manifestUrl = active.aioStreamsManifestUrl,
                manifestVerified = active.aioStreamsVerified,
            ).onSuccess {
                _snackbarMessage.value = actionMessage
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not update AIOStreams modules."
            }
            _isSaving.value = false
        }
    }

    fun installMarketplaceCatalogAddon(addon: MarketplaceAddonSpec) {
        val active = uiState.value.activeProfile ?: return
        val template = addon.catalogTemplate ?: return
        val existing = uiState.value.activeCatalogs
        val alreadyExists = existing.any {
            it.sourceReference.equals(template.sourceReference, ignoreCase = true) ||
                it.title.equals(template.title, ignoreCase = true)
        }
        if (alreadyExists) {
            _snackbarMessage.value = "'${template.title}' is already on your AIOMetadata board."
            return
        }
        val nextSortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1L) + 1L
        viewModelScope.launch {
            repository.addCatalogItem(
                userId = currentUserId,
                profileId = active.id,
                title = template.title,
                mediaType = template.mediaType,
                itemKind = template.itemKind,
                sourceProvider = template.sourceProvider,
                sourceReference = template.sourceReference,
                description = template.description,
                sortOrder = nextSortOrder,
            ).onSuccess {
                _snackbarMessage.value = "Added '${template.title}' (${addon.name}) to AIOMetadata."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to add catalog row."
            }
        }
    }

    fun verifyMarketplaceManifest(url: String) {
        viewModelScope.launch {
            _marketplaceVerification.value = VerificationState.Checking
            val res = verificationService.verifyAddonManifest(url)
            _marketplaceVerification.value = if (res.success) {
                VerificationState.Verified(res.message)
            } else {
                VerificationState.Failed(res.message)
            }
        }
    }

    fun clearMarketplaceVerification() {
        _marketplaceVerification.value = VerificationState.Idle
    }

    private data class VerificationBundle(
        val rd: VerificationState,
        val tmdb: VerificationState,
        val streams: VerificationState,
        val meta: VerificationState,
        val snackbar: String?,
    )

    private companion object {
        const val TAG = "SetupViewModel"
    }
}
