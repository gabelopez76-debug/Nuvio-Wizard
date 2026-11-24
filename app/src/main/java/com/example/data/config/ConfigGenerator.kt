package com.example.data.config

import com.example.data.model.CatalogCollectionItem
import com.example.data.model.SetupProfile
import org.json.JSONArray
import org.json.JSONObject

object ConfigGenerator {

    /**
     * Generates a unified AIOStreams configuration JSON where Real-Debrid credentials
     * and global stream filters are defined ONCE and automatically propagated to every
     * built-in and custom stream add-on.
     */
    fun generateAioStreamsConfigJson(profile: SetupProfile): String {
        val hasRd = profile.realDebridApiKey.isNotBlank()
        val masterService = JSONObject().apply {
            put("id", "realdebrid")
            put("enabled", hasRd)
            put("credentials", JSONObject().apply {
                put("apiKey", if (hasRd) profile.realDebridApiKey else "<PASTE_REAL_DEBRID_API_KEY>")
            })
        }

        val addonsArray = JSONArray()
        profile.enabledAddons.forEach { addonId ->
            addonsArray.put(
                JSONObject().apply {
                    put("type", addonId)
                    put("enabled", true)
                    put("inheritMasterDebrid", true)
                    put("serviceBinding", "realdebrid")
                    put("cachedOnly", profile.cachedOnly)
                }
            )
        }
        profile.customStreamSources.forEach { source ->
            addonsArray.put(
                JSONObject().apply {
                    put("type", "custom")
                    put("manifestUrl", source)
                    put("enabled", true)
                    put("inheritMasterDebrid", true)
                    put("serviceBinding", "realdebrid")
                    put("cachedOnly", profile.cachedOnly)
                }
            )
        }

        val root = JSONObject().apply {
            put("schema", "aiostreams-v2-nuvio-unified")
            put("instanceUrl", profile.aioStreamsInstanceUrl.ifBlank { "https://aiostreams.elfhosted.com" })
            put("singleConfigPropagation", true)
            put("services", JSONArray().apply { put(masterService) })
            put("addons", addonsArray)
            put("filters", JSONObject().apply {
                put("resolutions", JSONArray(profile.preferredResolutions))
                put("cachedOnly", profile.cachedOnly)
                put("excludeCamAndScreener", profile.excludeCam)
                put("preferDolbyVisionAndHdr", profile.preferDolbyVision)
                put("maxFileSizeGb", profile.maxFileSizeGb)
            })
            put("formatter", JSONObject().apply {
                put("preset", profile.formatterPreset.lowercase())
            })
        }
        return root.toString(2)
    }

    /**
     * Generates a structured AIOMetadata configuration JSON combining metadata providers,
     * TMDB/MDBList/RPDB API keys, and the user's ordered catalogs & collections.
     */
    fun generateAioMetadataConfigJson(
        profile: SetupProfile,
        catalogs: List<CatalogCollectionItem>,
    ): String {
        val enabledOrdered = catalogs
            .filter { it.enabled }
            .sortedBy { it.sortOrder }

        val catalogsArray = JSONArray()
        enabledOrdered.forEachIndexed { idx, item ->
            catalogsArray.put(
                JSONObject().apply {
                    put("order", idx + 1)
                    put("name", item.title)
                    put("kind", item.itemKind.lowercase())
                    put("mediaType", item.mediaType.lowercase())
                    put("source", item.sourceProvider)
                    put("reference", item.sourceReference)
                    put("enabled", true)
                }
            )
        }

        val root = JSONObject().apply {
            put("schema", "aiometadata-nuvio-board")
            put("instanceUrl", profile.aioMetadataInstanceUrl.ifBlank { "https://aiometadata.elfhosted.com" })
            put("providers", JSONObject().apply {
                put("movies", profile.movieProvider)
                put("series", profile.seriesProvider)
                put("anime", profile.animeProvider)
            })
            put("apiKeys", JSONObject().apply {
                put("tmdb", profile.tmdbApiKey.ifBlank { "<OPTIONAL_TMDB_KEY>" })
                put("mdblist", profile.mdblistApiKey.ifBlank { "<OPTIONAL_MDBLIST_KEY>" })
                put("rpdb", if (profile.rpdbPostersEnabled) profile.rpdbApiKey.ifBlank { "<RPDB_KEY>" } else "")
            })
            put("rpdbRatingPosters", profile.rpdbPostersEnabled)
            put("animeCatalogsEnabled", profile.animeCatalogsEnabled)
            put("catalogsAndCollections", catalogsArray)
        }
        return root.toString(2)
    }

    /**
     * Generates a shareable full Nuvio setup backup sheet (redacts raw secrets if requested).
     */
    fun generateFullExportSummary(
        profile: SetupProfile,
        catalogs: List<CatalogCollectionItem>,
        includeSecrets: Boolean = false,
    ): String {
        val enabledCatalogs = catalogs.filter { it.enabled }.sortedBy { it.sortOrder }
        val sb = StringBuilder()
        sb.appendLine("=== NUVIO SETUP EXPORT: ${profile.profileName} ===")
        sb.appendLine()
        sb.appendLine("1. NUVIO ACCOUNT")
        sb.appendLine("   Username: ${profile.nuvioUsername.ifBlank { "(not set)" }}")
        sb.appendLine("   Email:    ${profile.nuvioEmail.ifBlank { "(not set)" }}")
        if (includeSecrets && profile.nuvioPasswordHint.isNotBlank()) {
            sb.appendLine("   Password: ${profile.nuvioPasswordHint}")
        }
        sb.appendLine()
        sb.appendLine("2. NUVIO ADDON INSTALL ORDER (IMPORTANT)")
        sb.appendLine("   #1 (TOP) AIOMetadata Manifest:")
        sb.appendLine("      ${profile.aioMetadataManifestUrl.ifBlank { "(not generated yet)" }}")
        sb.appendLine("   #2 (BELOW) AIOStreams Manifest:")
        sb.appendLine("      ${profile.aioStreamsManifestUrl.ifBlank { "(not generated yet)" }}")
        sb.appendLine()
        sb.appendLine("3. AIOSTREAMS MASTER STREAM ENGINE")
        sb.appendLine("   Instance:       ${profile.aioStreamsInstanceUrl}")
        sb.appendLine("   Real-Debrid:    ${if (profile.realDebridApiKey.isNotBlank()) "Configured once & inherited by all addons" else "Pending"}")
        sb.appendLine("   Built-in Addons: ${profile.enabledAddons.joinToString(", ").ifBlank { "None" }}")
        if (profile.customStreamSources.isNotEmpty()) {
            sb.appendLine("   Custom Sources:  ${profile.customStreamSources.joinToString(", ")}")
        }
        sb.appendLine("   Resolutions:    ${profile.preferredResolutions.joinToString(", ")} | Max ${profile.maxFileSizeGb} GB | Formatter: ${profile.formatterPreset}")
        sb.appendLine()
        sb.appendLine("4. AIOMETADATA CATALOGS & COLLECTIONS (${enabledCatalogs.size} active)")
        sb.appendLine("   Providers: Movies=${profile.movieProvider}, Series=${profile.seriesProvider}, Anime=${profile.animeProvider}")
        enabledCatalogs.forEachIndexed { idx, item ->
            sb.appendLine("   ${idx + 1}. [${item.itemKind}] ${item.title} (${item.mediaType} • ${item.sourceProvider}: ${item.sourceReference})")
        }
        return sb.toString()
    }
}
