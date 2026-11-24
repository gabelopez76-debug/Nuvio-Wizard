package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CatalogCollectionItem
import com.example.data.model.CatalogPresetSpec
import com.example.data.model.SetupProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SetupRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireVerifiedUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    fun observeProfiles(userId: String): Flow<List<SetupProfile>> = flow {
        val uid = requireVerifiedUid(userId)
        val path = "users/$uid/profiles"
        emitAll(
            db.collection("users").document(uid).collection("profiles")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SetupProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCatalogs(userId: String): Flow<List<CatalogCollectionItem>> = flow {
        val uid = requireVerifiedUid(userId)
        val path = "users/$uid/catalogs"
        emitAll(
            db.collection("users").document(uid).collection("catalogs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CatalogCollectionItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.sortOrder }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createProfile(
        userId: String,
        profileName: String,
        nuvioUsername: String = "",
        nuvioEmail: String = "",
        nuvioPasswordHint: String = "",
        activeProfile: Boolean = true,
    ): Result<String> {
        val cleanName = profileName.trim().take(60)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Profile name cannot be empty."))
        }
        var docPath = "users/$userId/profiles"
        return try {
            val uid = requireVerifiedUid(userId)
            val docRef = db.collection("users").document(uid).collection("profiles").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "profileName" to cleanName,
                "activeProfile" to activeProfile,
                "nuvioUsername" to nuvioUsername.trim().take(80),
                "nuvioEmail" to nuvioEmail.trim().take(120),
                "nuvioPasswordHint" to nuvioPasswordHint.take(120),
                "nuvioAccountCreated" to false,
                "realDebridApiKey" to "",
                "realDebridAccountReady" to false,
                "realDebridVerified" to false,
                "realDebridUserLabel" to "",
                "realDebridPremiumDays" to 0L,
                "tmdbApiKey" to "",
                "tmdbVerified" to false,
                "mdblistApiKey" to "",
                "rpdbApiKey" to "",
                "aioStreamsInstanceUrl" to "https://aiostreams.elfhosted.com",
                "aioStreamsPassword" to "",
                "enabledAddons" to listOf("torrentio", "mediafusion", "comet"),
                "customStreamSources" to emptyList<String>(),
                "preferredResolutions" to listOf("2160p", "1080p"),
                "cachedOnly" to true,
                "excludeCam" to true,
                "preferDolbyVision" to true,
                "maxFileSizeGb" to 35L,
                "formatterPreset" to "PRISM",
                "aioStreamsManifestUrl" to "",
                "aioStreamsVerified" to false,
                "aioMetadataInstanceUrl" to "https://aiometadata.elfhosted.com",
                "movieProvider" to "TMDB",
                "seriesProvider" to "TMDB",
                "animeProvider" to "Kitsu",
                "rpdbPostersEnabled" to false,
                "animeCatalogsEnabled" to true,
                "aioMetadataManifestUrl" to "",
                "aioMetadataVerified" to false,
                "nuvioMetaInstalled" to false,
                "nuvioStreamsInstalled" to false,
                "nuvioOrderVerified" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun switchActiveProfile(
        userId: String,
        profiles: List<SetupProfile>,
        targetProfileId: String,
    ): Result<Unit> {
        val batchPath = "users/$userId/profiles/$targetProfileId"
        return try {
            val uid = requireVerifiedUid(userId)
            val batch = db.batch()
            val col = db.collection("users").document(uid).collection("profiles")
            profiles.forEach { profile ->
                if (profile.id.isNotBlank()) {
                    val shouldBeActive = profile.id == targetProfileId
                    if (profile.activeProfile != shouldBeActive) {
                        batch.update(
                            col.document(profile.id),
                            mapOf(
                                "activeProfile" to shouldBeActive,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        )
                    }
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, batchPath)
            Result.failure(e)
        }
    }

    suspend fun renameProfile(userId: String, profileId: String, newName: String): Result<Unit> {
        val clean = newName.trim().take(60)
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Profile name cannot be empty."))
        val path = "users/$userId/profiles/$profileId"
        return try {
            val uid = requireVerifiedUid(userId)
            db.collection("users").document(uid).collection("profiles").document(profileId)
                .update(
                    mapOf(
                        "profileName" to clean,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProfile(
        userId: String,
        profileId: String,
        relatedCatalogIds: List<String>,
    ): Result<Unit> {
        val path = "users/$userId/profiles/$profileId"
        return try {
            val uid = requireVerifiedUid(userId)
            val batch = db.batch()
            val userDoc = db.collection("users").document(uid)
            relatedCatalogIds.forEach { catId ->
                if (catId.isNotBlank()) {
                    batch.delete(userDoc.collection("catalogs").document(catId))
                }
            }
            batch.delete(userDoc.collection("profiles").document(profileId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateNuvioAccount(
        userId: String,
        profileId: String,
        username: String,
        email: String,
        passwordHint: String,
        accountCreated: Boolean,
    ): Result<Unit> {
        val path = "users/$userId/profiles/$profileId"
        return try {
            val uid = requireVerifiedUid(userId)
            db.collection("users").document(uid).collection("profiles").document(profileId)
                .update(
                    mapOf(
                        "nuvioUsername" to username.trim().take(80),
                        "nuvioEmail" to email.trim().take(120),
                        "nuvioPasswordHint" to passwordHint.take(120),
                        "nuvioAccountCreated" to accountCreated,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateApiKeysVault(
        userId: String,
        profileId: String,
        realDebridApiKey: String,
        realDebridAccountReady: Boolean,
        realDebridVerified: Boolean,
        realDebridUserLabel: String,
        realDebridPremiumDays: Long,
        tmdbApiKey: String,
        tmdbVerified: Boolean,
        mdblistApiKey: String,
        rpdbApiKey: String,
    ): Result<Unit> {
        val path = "users/$userId/profiles/$profileId"
        return try {
            val uid = requireVerifiedUid(userId)
            db.collection("users").document(uid).collection("profiles").document(profileId)
                .update(
                    mapOf(
                        "realDebridApiKey" to realDebridApiKey.trim().take(200),
                        "realDebridAccountReady" to realDebridAccountReady,
                        "realDebridVerified" to realDebridVerified,
                        "realDebridUserLabel" to realDebridUserLabel.take(100),
                        "realDebridPremiumDays" to realDebridPremiumDays.coerceIn(0L, 10_000L),
                        "tmdbApiKey" to tmdbApiKey.trim().take(300),
                        "tmdbVerified" to tmdbVerified,
                        "mdblistApiKey" to mdblistApiKey.trim().take(200),
                        "rpdbApiKey" to rpdbApiKey.trim().take(200),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAioStreamsConfig(
        userId: String,
        profileId: String,
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
        manifestVerified: Boolean,
    ): Result<Unit> {
        val path = "users/$userId/profiles/$profileId"
        val validPreset = if (formatterPreset in listOf("PRISM", "MINIMALISTIC", "DETAILED", "GDRIVE")) {
            formatterPreset
        } else {
            "PRISM"
        }
        return try {
            val uid = requireVerifiedUid(userId)
            db.collection("users").document(uid).collection("profiles").document(profileId)
                .update(
                    mapOf(
                        "aioStreamsInstanceUrl" to instanceUrl.trim().take(250),
                        "aioStreamsPassword" to password.take(120),
                        "enabledAddons" to enabledAddons.distinct().take(20),
                        "customStreamSources" to customStreamSources.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(20),
                        "preferredResolutions" to preferredResolutions.distinct().take(10),
                        "cachedOnly" to cachedOnly,
                        "excludeCam" to excludeCam,
                        "preferDolbyVision" to preferDolbyVision,
                        "maxFileSizeGb" to maxFileSizeGb.coerceIn(1L, 200L),
                        "formatterPreset" to validPreset,
                        "aioStreamsManifestUrl" to manifestUrl.trim().take(500),
                        "aioStreamsVerified" to manifestVerified,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAioMetadataConfig(
        userId: String,
        profileId: String,
        instanceUrl: String,
        movieProvider: String,
        seriesProvider: String,
        animeProvider: String,
        rpdbPostersEnabled: Boolean,
        animeCatalogsEnabled: Boolean,
        manifestUrl: String,
        manifestVerified: Boolean,
    ): Result<Unit> {
        val path = "users/$userId/profiles/$profileId"
        val safeMovie = if (movieProvider in listOf("TMDB", "IMDb")) movieProvider else "TMDB"
        val safeSeries = if (seriesProvider in listOf("TMDB", "TVDB")) seriesProvider else "TMDB"
        val safeAnime = if (animeProvider in listOf("Kitsu", "MAL", "AniList")) animeProvider else "Kitsu"
        return try {
            val uid = requireVerifiedUid(userId)
            db.collection("users").document(uid).collection("profiles").document(profileId)
                .update(
                    mapOf(
                        "aioMetadataInstanceUrl" to instanceUrl.trim().take(250),
                        "movieProvider" to safeMovie,
                        "seriesProvider" to safeSeries,
                        "animeProvider" to safeAnime,
                        "rpdbPostersEnabled" to rpdbPostersEnabled,
                        "animeCatalogsEnabled" to animeCatalogsEnabled,
                        "aioMetadataManifestUrl" to manifestUrl.trim().take(500),
                        "aioMetadataVerified" to manifestVerified,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDeployChecklist(
        userId: String,
        profileId: String,
        metaInstalled: Boolean,
        streamsInstalled: Boolean,
        orderVerified: Boolean,
    ): Result<Unit> {
        val path = "users/$userId/profiles/$profileId"
        return try {
            val uid = requireVerifiedUid(userId)
            db.collection("users").document(uid).collection("profiles").document(profileId)
                .update(
                    mapOf(
                        "nuvioMetaInstalled" to metaInstalled,
                        "nuvioStreamsInstalled" to streamsInstalled,
                        "nuvioOrderVerified" to orderVerified,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addCatalogItem(
        userId: String,
        profileId: String,
        title: String,
        mediaType: String,
        itemKind: String,
        sourceProvider: String,
        sourceReference: String,
        description: String,
        sortOrder: Long,
    ): Result<Unit> {
        val cleanTitle = title.trim().take(80)
        if (cleanTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Title is required."))
        }
        val safeMedia = if (mediaType in listOf("Movies", "Series", "Anime", "Mixed")) mediaType else "Movies"
        val safeKind = if (itemKind in listOf("CATALOG", "COLLECTION")) itemKind else "CATALOG"
        val safeSource = if (sourceProvider in listOf("TMDB", "MDBList", "Trakt", "Streaming", "Curated")) sourceProvider else "TMDB"
        var docPath = "users/$userId/catalogs"
        return try {
            val uid = requireVerifiedUid(userId)
            val docRef = db.collection("users").document(uid).collection("catalogs").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "profileId" to profileId,
                "title" to cleanTitle,
                "mediaType" to safeMedia,
                "itemKind" to safeKind,
                "sourceProvider" to safeSource,
                "sourceReference" to sourceReference.trim().take(300),
                "description" to description.trim().take(240),
                "enabled" to true,
                "sortOrder" to sortOrder.coerceIn(0L, 1000L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleCatalogEnabled(
        userId: String,
        catalogId: String,
        enabled: Boolean,
    ): Result<Unit> {
        val path = "users/$userId/catalogs/$catalogId"
        return try {
            val uid = requireVerifiedUid(userId)
            db.collection("users").document(uid).collection("catalogs").document(catalogId)
                .update(
                    mapOf(
                        "enabled" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun swapCatalogOrder(
        userId: String,
        first: CatalogCollectionItem,
        second: CatalogCollectionItem,
    ): Result<Unit> {
        val path = "users/$userId/catalogs/${first.id}"
        return try {
            val uid = requireVerifiedUid(userId)
            val col = db.collection("users").document(uid).collection("catalogs")
            val batch = db.batch()
            batch.update(
                col.document(first.id),
                mapOf(
                    "sortOrder" to second.sortOrder.coerceIn(0L, 1000L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(
                col.document(second.id),
                mapOf(
                    "sortOrder" to first.sortOrder.coerceIn(0L, 1000L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCatalogItem(userId: String, catalogId: String): Result<Unit> {
        val path = "users/$userId/catalogs/$catalogId"
        return try {
            val uid = requireVerifiedUid(userId)
            db.collection("users").document(uid).collection("catalogs").document(catalogId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun applyCatalogStarterPack(
        userId: String,
        profileId: String,
        existingForProfile: List<CatalogCollectionItem>,
        pack: CatalogPresetSpec,
    ): Result<Unit> {
        val path = "users/$userId/catalogs"
        return try {
            val uid = requireVerifiedUid(userId)
            val col = db.collection("users").document(uid).collection("catalogs")
            val batch = db.batch()
            existingForProfile.forEach { existingItem ->
                if (existingItem.id.isNotBlank()) {
                    batch.delete(col.document(existingItem.id))
                }
            }
            pack.items.forEachIndexed { idx, template ->
                val ref = col.document()
                batch.set(
                    ref,
                    mapOf(
                        "userId" to uid,
                        "profileId" to profileId,
                        "title" to template.title.take(80),
                        "mediaType" to template.mediaType,
                        "itemKind" to template.itemKind,
                        "sourceProvider" to template.sourceProvider,
                        "sourceReference" to template.sourceReference.take(300),
                        "description" to template.description.take(240),
                        "enabled" to true,
                        "sortOrder" to idx.toLong(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
