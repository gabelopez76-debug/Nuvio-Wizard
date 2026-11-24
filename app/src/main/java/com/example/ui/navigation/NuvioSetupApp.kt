package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.signOutUser
import com.example.data.network.VerificationService
import com.example.data.repository.SetupRepository
import com.example.ui.components.PersistentSetupProgressRail
import com.example.ui.screens.AccountsKeysScreen
import com.example.ui.screens.AddonMarketplaceScreen
import com.example.ui.screens.AioMetadataScreen
import com.example.ui.screens.AioStreamsScreen
import com.example.ui.screens.CollectionPictureDesignerScreen
import com.example.ui.screens.DeployScreen
import com.example.ui.screens.GuideDashboardScreen
import com.example.ui.screens.ProfileManagerSheet
import com.example.ui.screens.SetupSummaryScreen
import com.example.ui.screens.SupportChatScreen
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanSignal
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseAlert
import com.example.ui.viewmodel.ArtworkDesignerViewModel
import com.example.ui.viewmodel.SetupViewModel
import com.example.ui.viewmodel.SupportChatViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_GUIDE = "guide"
const val ROUTE_ACCOUNTS = "accounts"
const val ROUTE_AIOSTREAMS = "aiostreams"
const val ROUTE_AIOMETADATA = "aiometadata"
const val ROUTE_SUPPORT = "support"
const val ROUTE_DEPLOY = "deploy"
const val ROUTE_SUMMARY = "summary"
const val ROUTE_MARKETPLACE = "marketplace"
const val ROUTE_ART_DESIGNER = "art_designer"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val primaryDestinations = listOf(
    NavDestination(ROUTE_GUIDE, "Guide", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_guide"),
    NavDestination(ROUTE_ACCOUNTS, "Accounts", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "nav_accounts"),
    NavDestination(ROUTE_AIOSTREAMS, "Streams", Icons.Filled.Hub, Icons.Outlined.Hub, "nav_aiostreams"),
    NavDestination(ROUTE_AIOMETADATA, "Meta", Icons.Filled.AutoAwesomeMosaic, Icons.Outlined.AutoAwesomeMosaic, "nav_aiometadata"),
    NavDestination(ROUTE_SUPPORT, "AI Help", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_support"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuvioSetupApp(
    currentUser: FirebaseUser,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val setupViewModel: SetupViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SetupViewModel(
                    repository = SetupRepository(db),
                    verificationService = VerificationService(),
                    currentUserId = currentUser.uid,
                )
            }
        }
    )
    val chatViewModel: SupportChatViewModel = viewModel(key = "chat_${currentUser.uid}")
    val artworkViewModel: ArtworkDesignerViewModel = viewModel(key = "art_${currentUser.uid}")

    val setupState by setupViewModel.uiState.collectAsStateWithLifecycle()
    val marketplaceVerification by setupViewModel.marketplaceVerification.collectAsStateWithLifecycle()
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val artworkGenState by artworkViewModel.genState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_GUIDE

    val snackbarHostState = remember { SnackbarHostState() }
    var showProfileSheet by remember { mutableStateOf(false) }
    var designerInitialTitle by remember { mutableStateOf<String?>(null) }
    var designerInitialSubtitle by remember { mutableStateOf<String?>(null) }
    var designerInitialBadge by remember { mutableStateOf<String?>(null) }

    // Track live transition to 100% completion per profile so completing the final step
    // automatically opens the celebratory Setup Summary screen (without hijacking cold start).
    val activeProfileId = setupState.activeProfile?.id
    var lastObservedSteps by remember(activeProfileId) { mutableStateOf<Int?>(null) }
    LaunchedEffect(activeProfileId, setupState.progress.completedSteps, setupState.progress.totalSteps) {
        if (activeProfileId == null || setupState.isLoading) return@LaunchedEffect
        val currentCompleted = setupState.progress.completedSteps
        val total = setupState.progress.totalSteps
        val previous = lastObservedSteps
        lastObservedSteps = currentCompleted
        if (previous != null && previous < total && currentCompleted == total && total > 0 && currentRoute != ROUTE_SUMMARY) {
            navController.navigate(ROUTE_SUMMARY) { launchSingleTop = true }
        }
    }

    LaunchedEffect(setupState.snackbarMessage) {
        val msg = setupState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            setupViewModel.clearSnackbar()
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToProgressRoute(route: String) {
        if (route == ROUTE_DEPLOY || route == ROUTE_SUMMARY || route == ROUTE_MARKETPLACE || route == ROUTE_ART_DESIGNER) {
            if (currentRoute != route) {
                navController.navigate(route) { launchSingleTop = true }
            }
        } else {
            navigateTopLevel(route)
        }
    }

    fun openArtDesigner(title: String? = null, subtitle: String? = null, badge: String? = null) {
        designerInitialTitle = title
        designerInitialSubtitle = subtitle
        designerInitialBadge = badge
        navController.navigate(ROUTE_ART_DESIGNER) { launchSingleTop = true }
    }

    fun isDestinationComplete(route: String): Boolean = when (route) {
        ROUTE_ACCOUNTS -> setupState.progress.nuvioDone && setupState.progress.debridDone
        ROUTE_AIOSTREAMS -> setupState.progress.aioStreamsDone
        ROUTE_AIOMETADATA -> setupState.progress.aioMetaDone
        else -> false
    }

    val topTitle = when (currentRoute) {
        ROUTE_GUIDE -> "Nuvio Setup"
        ROUTE_ACCOUNTS -> "Accounts & API Vault"
        ROUTE_AIOSTREAMS -> "AIOStreams Hub"
        ROUTE_AIOMETADATA -> "AIOMetadata & Catalogs"
        ROUTE_SUPPORT -> "Gemini Support"
        ROUTE_DEPLOY -> "Deploy to Nuvio"
        ROUTE_SUMMARY -> "Setup Summary"
        ROUTE_MARKETPLACE -> "Add-on Marketplace"
        ROUTE_ART_DESIGNER -> "Collection Studio"
        else -> "Nuvio Setup"
    }

    val isSecondaryScreen = currentRoute == ROUTE_DEPLOY ||
        currentRoute == ROUTE_SUMMARY ||
        currentRoute == ROUTE_MARKETPLACE ||
        currentRoute == ROUTE_ART_DESIGNER

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        val useNavRail = maxWidth >= 840.dp

        Scaffold(
            containerColor = ObsidianVoid,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ObsidianSurface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        navigationIcon = {
                            if (isSecondaryScreen) {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("secondary_back_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Navigate back",
                                    )
                                }
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    text = topTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                setupState.activeProfile?.let { active ->
                                    Text(
                                        text = active.profileName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanSignal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        },
                        actions = {
                            if (currentRoute != ROUTE_MARKETPLACE) {
                                IconButton(
                                    onClick = { navController.navigate(ROUTE_MARKETPLACE) { launchSingleTop = true } },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_marketplace_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Storefront,
                                        contentDescription = "Open compatible add-on marketplace",
                                        tint = CyanSignal,
                                    )
                                }
                            }
                            if (currentRoute != ROUTE_DEPLOY) {
                                IconButton(
                                    onClick = { navController.navigate(ROUTE_DEPLOY) { launchSingleTop = true } },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_deploy_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RocketLaunch,
                                        contentDescription = "Open Deploy to Nuvio",
                                        tint = EmeraldVerified,
                                    )
                                }
                            }
                            Surface(
                                onClick = { showProfileSheet = true },
                                color = CyanContainer,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, CyanSignal.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .testTag("top_profile_button"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Manage profiles",
                                        tint = CyanSignal,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Profiles",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanSignal,
                                    )
                                }
                            }
                        },
                    )
                    if (!setupState.isLoading && setupState.activeProfile != null) {
                        PersistentSetupProgressRail(
                            progress = setupState.progress,
                            currentRoute = currentRoute,
                            onNavigateToRoute = ::navigateToProgressRoute,
                        )
                    }
                }
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 0.dp,
                    ) {
                        primaryDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val done = isDestinationComplete(dest.route)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (done) {
                                                Badge(containerColor = EmeraldVerified)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label,
                                        )
                                    }
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanSignal,
                                    selectedTextColor = CyanSignal,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Spacer(Modifier.height(12.dp))
                        primaryDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val done = isDestinationComplete(dest.route)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (done) {
                                                Badge(containerColor = EmeraldVerified)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label,
                                        )
                                    }
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CyanSignal,
                                    selectedTextColor = CyanSignal,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    when {
                        setupState.isLoading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(color = CyanSignal)
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "Syncing your Nuvio setup profiles...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        !setupState.errorMessage.isNullOrBlank() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = RoseAlert,
                                    modifier = Modifier.size(44.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Could not sync configuration profiles",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = setupState.errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RoseAlert,
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 820.dp)
                                    .fillMaxSize()
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_GUIDE,
                                ) {
                                    composable(ROUTE_GUIDE) {
                                        GuideDashboardScreen(
                                            state = setupState,
                                            onOpenProfiles = { showProfileSheet = true },
                                            onNavigateToAccounts = { navigateTopLevel(ROUTE_ACCOUNTS) },
                                            onNavigateToAioStreams = { navigateTopLevel(ROUTE_AIOSTREAMS) },
                                            onNavigateToAioMetadata = { navigateTopLevel(ROUTE_AIOMETADATA) },
                                            onNavigateToDeploy = { navController.navigate(ROUTE_DEPLOY) { launchSingleTop = true } },
                                            onNavigateToSupport = { navigateTopLevel(ROUTE_SUPPORT) },
                                            onNavigateToSummary = { navController.navigate(ROUTE_SUMMARY) { launchSingleTop = true } },
                                            onNavigateToMarketplace = { navController.navigate(ROUTE_MARKETPLACE) { launchSingleTop = true } },
                                            onNavigateToArtDesigner = { openArtDesigner() },
                                            onVerifyRealDebrid = setupViewModel::verifyRealDebrid,
                                            onVerifyTmdb = setupViewModel::verifyTmdb,
                                            onVerifyAioStreamsManifest = setupViewModel::verifyAioStreamsManifest,
                                            onVerifyAioMetadataManifest = setupViewModel::verifyAioMetadataManifest,
                                        )
                                    }
                                    composable(ROUTE_ACCOUNTS) {
                                        AccountsKeysScreen(
                                            state = setupState,
                                            onSaveNuvioAccount = setupViewModel::saveNuvioAccount,
                                            onSaveApiKeys = setupViewModel::saveApiKeysVault,
                                            onVerifyRealDebrid = setupViewModel::verifyRealDebrid,
                                            onVerifyTmdb = setupViewModel::verifyTmdb,
                                            onNotify = setupViewModel::showMessage,
                                        )
                                    }
                                    composable(ROUTE_AIOSTREAMS) {
                                        AioStreamsScreen(
                                            state = setupState,
                                            onGoToAccounts = { navigateTopLevel(ROUTE_ACCOUNTS) },
                                            onSaveAioStreams = setupViewModel::saveAioStreamsConfig,
                                            onVerifyManifest = setupViewModel::verifyAioStreamsManifest,
                                            onOpenMarketplace = { navController.navigate(ROUTE_MARKETPLACE) { launchSingleTop = true } },
                                            onNotify = setupViewModel::showMessage,
                                        )
                                    }
                                    composable(ROUTE_AIOMETADATA) {
                                        AioMetadataScreen(
                                            state = setupState,
                                            onSaveAioMetadata = setupViewModel::saveAioMetadataConfig,
                                            onVerifyManifest = setupViewModel::verifyAioMetadataManifest,
                                            onAddCatalog = setupViewModel::addCatalogOrCollection,
                                            onToggleCatalog = setupViewModel::toggleCatalog,
                                            onMoveCatalog = setupViewModel::moveCatalog,
                                            onDeleteCatalog = setupViewModel::deleteCatalog,
                                            onApplyStarterPack = setupViewModel::applyStarterPack,
                                            onOpenDesigner = { t, s, b -> openArtDesigner(t, s, b) },
                                            onOpenMarketplace = { navController.navigate(ROUTE_MARKETPLACE) { launchSingleTop = true } },
                                            onNotify = setupViewModel::showMessage,
                                        )
                                    }
                                    composable(ROUTE_SUPPORT) {
                                        SupportChatScreen(
                                            setupState = setupState,
                                            chatState = chatState,
                                            onSendMessage = { text ->
                                                chatViewModel.sendMessage(
                                                    userText = text,
                                                    activeProfile = setupState.activeProfile,
                                                    activeCatalogs = setupState.activeCatalogs,
                                                )
                                            },
                                            onResetChat = chatViewModel::resetConversation,
                                            onDismissError = chatViewModel::dismissError,
                                        )
                                    }
                                    composable(ROUTE_DEPLOY) {
                                        DeployScreen(
                                            state = setupState,
                                            onUpdateChecklist = setupViewModel::updateDeployChecklist,
                                            onOpenSetupSummary = { navController.navigate(ROUTE_SUMMARY) { launchSingleTop = true } },
                                            onNotify = setupViewModel::showMessage,
                                        )
                                    }
                                    composable(ROUTE_SUMMARY) {
                                        SetupSummaryScreen(
                                            state = setupState,
                                            onNavigateToRoute = ::navigateToProgressRoute,
                                            onBackToDashboard = { navigateTopLevel(ROUTE_GUIDE) },
                                            onNotify = setupViewModel::showMessage,
                                        )
                                    }
                                    composable(ROUTE_MARKETPLACE) {
                                        AddonMarketplaceScreen(
                                            state = setupState,
                                            manifestVerification = marketplaceVerification,
                                            onToggleStreamAddon = setupViewModel::toggleMarketplaceStreamAddon,
                                            onInstallCatalogAddon = setupViewModel::installMarketplaceCatalogAddon,
                                            onVerifyManifest = setupViewModel::verifyMarketplaceManifest,
                                            onClearVerification = setupViewModel::clearMarketplaceVerification,
                                            onNotify = setupViewModel::showMessage,
                                        )
                                    }
                                    composable(ROUTE_ART_DESIGNER) {
                                        CollectionPictureDesignerScreen(
                                            setupState = setupState,
                                            aiGenState = artworkGenState,
                                            initialCollectionTitle = designerInitialTitle,
                                            initialCollectionSubtitle = designerInitialSubtitle,
                                            initialCollectionBadge = designerInitialBadge,
                                            onGenerateAiBackdrop = artworkViewModel::generateCollectionBackdrop,
                                            onClearAiError = artworkViewModel::clearError,
                                            onNotify = setupViewModel::showMessage,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileSheet) {
        ProfileManagerSheet(
            userEmail = currentUser.email,
            profiles = setupState.profiles,
            activeProfile = setupState.activeProfile,
            onDismiss = { showProfileSheet = false },
            onSwitchProfile = {
                setupViewModel.switchProfile(it)
                showProfileSheet = false
            },
            onCreateProfile = { name, username, email, password ->
                setupViewModel.createNewProfile(name, username, email, password)
                showProfileSheet = false
            },
            onRenameActiveProfile = setupViewModel::renameActiveProfile,
            onDeleteProfile = setupViewModel::deleteProfile,
            onSignOut = {
                signOutUser(
                    credentialManager = credentialManager,
                    scope = scope,
                )
            },
        )
    }
}
