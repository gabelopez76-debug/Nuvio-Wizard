package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class SetupProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val profileName: String = "Main Theater Setup",
    val activeProfile: Boolean = true,
    // Step 1: Nuvio Account
    val nuvioUsername: String = "",
    val nuvioEmail: String = "",
    val nuvioPasswordHint: String = "",
    val nuvioAccountCreated: Boolean = false,
    // Step 2: Real-Debrid & Metadata API Keys Vault
    val realDebridApiKey: String = "",
    val realDebridAccountReady: Boolean = false,
    val realDebridVerified: Boolean = false,
    val realDebridUserLabel: String = "",
    val realDebridPremiumDays: Long = 0L,
    val tmdbApiKey: String = "",
    val tmdbVerified: Boolean = false,
    val mdblistApiKey: String = "",
    val rpdbApiKey: String = "",
    // Step 3: AIOStreams Single-Config Hub
    val aioStreamsInstanceUrl: String = "https://aiostreams.elfhosted.com",
    val aioStreamsPassword: String = "",
    val enabledAddons: List<String> = listOf("torrentio", "mediafusion", "comet"),
    val customStreamSources: List<String> = emptyList(),
    val preferredResolutions: List<String> = listOf("2160p", "1080p"),
    val cachedOnly: Boolean = true,
    val excludeCam: Boolean = true,
    val preferDolbyVision: Boolean = true,
    val maxFileSizeGb: Long = 35L,
    val formatterPreset: String = "PRISM",
    val aioStreamsManifestUrl: String = "",
    val aioStreamsVerified: Boolean = false,
    // Step 4: AIOMetadata Settings
    val aioMetadataInstanceUrl: String = "https://aiometadata.elfhosted.com",
    val movieProvider: String = "TMDB",
    val seriesProvider: String = "TMDB",
    val animeProvider: String = "Kitsu",
    val rpdbPostersEnabled: Boolean = false,
    val animeCatalogsEnabled: Boolean = true,
    val aioMetadataManifestUrl: String = "",
    val aioMetadataVerified: Boolean = false,
    // Step 5: Deploy Checklist
    val nuvioMetaInstalled: Boolean = false,
    val nuvioStreamsInstalled: Boolean = false,
    val nuvioOrderVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CatalogCollectionItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val profileId: String = "",
    val title: String = "",
    val mediaType: String = "Movies",
    val itemKind: String = "CATALOG",
    val sourceProvider: String = "TMDB",
    val sourceReference: String = "",
    val description: String = "",
    val enabled: Boolean = true,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class StreamAddonSpec(
    val id: String,
    val name: String,
    val tagline: String,
    val tags: List<String>,
    val recommended: Boolean = false,
)

data class FormatterPresetSpec(
    val id: String,
    val label: String,
    val summary: String,
    val previewTitle: String,
    val previewSubtitle: String,
)

data class CatalogPresetSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val items: List<CatalogTemplate>,
)

data class CatalogTemplate(
    val title: String,
    val mediaType: String,
    val itemKind: String,
    val sourceProvider: String,
    val sourceReference: String,
    val description: String,
)

sealed interface VerificationState {
    data object Idle : VerificationState
    data object Checking : VerificationState
    data class Verified(val message: String) : VerificationState
    data class Failed(val message: String) : VerificationState
}

object SetupCatalogDefaults {
    val builtInStreamAddons: List<StreamAddonSpec> = listOf(
        StreamAddonSpec(
            id = "torrentio",
            name = "Torrentio",
            tagline = "Fast primary scraper with deep Real-Debrid instant cache coverage.",
            tags = listOf("Fast", "4K Remux", "Core"),
            recommended = true,
        ),
        StreamAddonSpec(
            id = "mediafusion",
            name = "MediaFusion",
            tagline = "Rich multi-region & HDR/Dolby Vision catalog scraper.",
            tags = listOf("HDR / DV", "Multi-Language", "Core"),
            recommended = true,
        ),
        StreamAddonSpec(
            id = "comet",
            name = "Comet",
            tagline = "High-speed ElfHosted proxy scraper for reliable fallback streams.",
            tags = listOf("Reliable", "Cached RD", "Core"),
            recommended = true,
        ),
        StreamAddonSpec(
            id = "jackettio",
            name = "Jackettio",
            tagline = "Great backup for older classics, indie cinema, and complete season packs.",
            tags = listOf("Season Packs", "Backlog"),
            recommended = false,
        ),
        StreamAddonSpec(
            id = "stremthru",
            name = "StremThru",
            tagline = "Debrid caching accelerator & cross-service resolver inside AIOStreams.",
            tags = listOf("Proxy", "Instant Cache"),
            recommended = false,
        ),
        StreamAddonSpec(
            id = "knightcrawler",
            name = "KnightCrawler",
            tagline = "Community indexer specialized in anime & niche TV series.",
            tags = listOf("Anime", "TV Shows"),
            recommended = false,
        ),
    )

    val formatterPresets: List<FormatterPresetSpec> = listOf(
        FormatterPresetSpec(
            id = "PRISM",
            label = "Prism (Recommended)",
            summary = "Clean TV-friendly badges with resolution, HDR, audio, and file size.",
            previewTitle = "[RD+] 4K DV • Atmos • 18.4 GB",
            previewSubtitle = "Dune.Part.Two.2024.2160p.WEB-DL • Torrentio via AIOStreams",
        ),
        FormatterPresetSpec(
            id = "MINIMALISTIC",
            label = "Minimalistic",
            summary = "Ultra-fast compact rows built for quick remote browsing on Nuvio TV.",
            previewTitle = "RD+ 4K • 18.4 GB",
            previewSubtitle = "HDR10+ • EAC3 5.1 • Torrentio",
        ),
        FormatterPresetSpec(
            id = "DETAILED",
            label = "Detailed Technical",
            summary = "Full release group, codec, bit-depth, language tracks, and indexer info.",
            previewTitle = "[RD+ CACHED] 2160p REMUX HEVC 10bit DV/HDR10+",
            previewSubtitle = "TrueHD 7.1 Atmos • EN/ES • 42.8 GB • MediaFusion",
        ),
        FormatterPresetSpec(
            id = "GDRIVE",
            label = "Compact Drive Style",
            summary = "File-centric clean naming ideal for mobile screens.",
            previewTitle = "2160p • 18.4 GB • RD Instant",
            previewSubtitle = "HEVC • Dolby Vision • Comet",
        ),
    )

    val resolutionOptions = listOf("2160p", "1080p", "720p")
    val movieProviders = listOf("TMDB", "IMDb")
    val seriesProviders = listOf("TMDB", "TVDB")
    val animeProviders = listOf("Kitsu", "MAL", "AniList")
    val mediaTypes = listOf("Movies", "Series", "Anime", "Mixed")
    val sourceProviders = listOf("TMDB", "MDBList", "Trakt", "Streaming", "Curated")

    val starterPacks: List<CatalogPresetSpec> = listOf(
        CatalogPresetSpec(
            id = "minimalist",
            name = "Minimalist Cinema",
            subtitle = "4 ultra-clean rows with zero home-screen clutter.",
            items = listOf(
                CatalogTemplate(
                    title = "Trending Movies",
                    mediaType = "Movies",
                    itemKind = "CATALOG",
                    sourceProvider = "TMDB",
                    sourceReference = "trending/movie/week",
                    description = "Top weekly films powered by TMDB.",
                ),
                CatalogTemplate(
                    title = "Trending Series",
                    mediaType = "Series",
                    itemKind = "CATALOG",
                    sourceProvider = "TMDB",
                    sourceReference = "trending/tv/week",
                    description = "Most talked-about shows this week.",
                ),
                CatalogTemplate(
                    title = "Latest Digital Releases",
                    mediaType = "Movies",
                    itemKind = "COLLECTION",
                    sourceProvider = "MDBList",
                    sourceReference = "garycrawfordgc/latest-releases",
                    description = "Freshly available HD & 4K home theater releases.",
                ),
                CatalogTemplate(
                    title = "Critics' Top Picks",
                    mediaType = "Mixed",
                    itemKind = "COLLECTION",
                    sourceProvider = "Trakt",
                    sourceReference = "trakt/popular-top-rated",
                    description = "High-rated films & series curated from Trakt.",
                ),
            ),
        ),
        CatalogPresetSpec(
            id = "balanced",
            name = "Balanced Home Theater",
            subtitle = "6 rows mixing Trending, Streaming Platforms, and MDBList Collections.",
            items = listOf(
                CatalogTemplate(
                    title = "Trending Movies",
                    mediaType = "Movies",
                    itemKind = "CATALOG",
                    sourceProvider = "TMDB",
                    sourceReference = "trending/movie/week",
                    description = "Weekly box-office & streaming movie leaders.",
                ),
                CatalogTemplate(
                    title = "Popular TV Series",
                    mediaType = "Series",
                    itemKind = "CATALOG",
                    sourceProvider = "TMDB",
                    sourceReference = "trending/tv/week",
                    description = "Ongoing & returning series with rich metadata.",
                ),
                CatalogTemplate(
                    title = "Streaming: Apple TV+ & Max",
                    mediaType = "Series",
                    itemKind = "CATALOG",
                    sourceProvider = "Streaming",
                    sourceReference = "provider/apple-tv-plus,max",
                    description = "Prestige originals unified into one sleek row.",
                ),
                CatalogTemplate(
                    title = "4K Showcase Collection",
                    mediaType = "Movies",
                    itemKind = "COLLECTION",
                    sourceProvider = "MDBList",
                    sourceReference = "mdblist/4k-reference-cinema",
                    description = "Dolby Vision & Atmos reference-quality movies.",
                ),
                CatalogTemplate(
                    title = "Sci-Fi & Mind-Benders",
                    mediaType = "Movies",
                    itemKind = "COLLECTION",
                    sourceProvider = "Curated",
                    sourceReference = "genre/878?sort=vote_average",
                    description = "Curated high-concept science fiction classics.",
                ),
                CatalogTemplate(
                    title = "Award-Winning Dramas",
                    mediaType = "Mixed",
                    itemKind = "COLLECTION",
                    sourceProvider = "Trakt",
                    sourceReference = "trakt/oscar-emmy-winners",
                    description = "Academy & Emmy standout titles.",
                ),
            ),
        ),
        CatalogPresetSpec(
            id = "streaming_anime",
            name = "Streaming + Anime Hub",
            subtitle = "7 rows including Netflix, Disney+, Seasonal Anime, and Collections.",
            items = listOf(
                CatalogTemplate(
                    title = "Trending Movies",
                    mediaType = "Movies",
                    itemKind = "CATALOG",
                    sourceProvider = "TMDB",
                    sourceReference = "trending/movie/week",
                    description = "Global top films updated daily.",
                ),
                CatalogTemplate(
                    title = "Trending Series",
                    mediaType = "Series",
                    itemKind = "CATALOG",
                    sourceProvider = "TMDB",
                    sourceReference = "trending/tv/week",
                    description = "Top trending series across networks.",
                ),
                CatalogTemplate(
                    title = "Netflix Originals",
                    mediaType = "Mixed",
                    itemKind = "CATALOG",
                    sourceProvider = "Streaming",
                    sourceReference = "provider/netflix",
                    description = "Dedicated Netflix discovery row.",
                ),
                CatalogTemplate(
                    title = "Disney+ & Pixar",
                    mediaType = "Mixed",
                    itemKind = "CATALOG",
                    sourceProvider = "Streaming",
                    sourceReference = "provider/disney-plus",
                    description = "Family favorites, Star Wars, and Marvel.",
                ),
                CatalogTemplate(
                    title = "Seasonal Airing Anime",
                    mediaType = "Anime",
                    itemKind = "CATALOG",
                    sourceProvider = "TMDB",
                    sourceReference = "kitsu/airing-seasonal",
                    description = "Mapped with Kitsu/MAL IDs via AIOMetadata.",
                ),
                CatalogTemplate(
                    title = "Top Anime Classics",
                    mediaType = "Anime",
                    itemKind = "COLLECTION",
                    sourceProvider = "Curated",
                    sourceReference = "mal/top-anime-collection",
                    description = "All-time highest-rated anime series & films.",
                ),
                CatalogTemplate(
                    title = "Weekend Watchlist",
                    mediaType = "Mixed",
                    itemKind = "COLLECTION",
                    sourceProvider = "MDBList",
                    sourceReference = "mdblist/personal-watchlist",
                    description = "Synced personal collection via MDBList API.",
                ),
            ),
        ),
    )
}
