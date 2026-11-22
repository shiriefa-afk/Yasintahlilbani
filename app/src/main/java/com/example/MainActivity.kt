package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ReaderSettingsRepository
import com.example.data.UserDataRepository
import com.example.model.ReaderSettings
import com.example.ui.components.ReaderDisplaySheet
import com.example.ui.screens.ArwahListScreen
import com.example.ui.screens.SurahTahlilDashboardScreen
import com.example.ui.screens.SurahTahlilSubSection
import com.example.ui.screens.TasbihScreen
import com.example.ui.theme.YasinTahlilTheme
import com.example.ui.viewmodel.YasinTahlilViewModel
import kotlinx.coroutines.flow.collectLatest

const val ROUTE_SURAH_TAHLIL = "surah_tahlil"
const val ROUTE_TASBIH = "tasbih"
const val ROUTE_ALMARHUM_LIST = "almarhum_list"

private data class DashboardNavItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val dashboardDestinations = listOf(
  DashboardNavItem(
    route = ROUTE_SURAH_TAHLIL,
    label = "Surah/Tahlil",
    selectedIcon = Icons.Filled.AutoStories,
    unselectedIcon = Icons.Outlined.AutoStories,
    testTag = "nav_surah_tahlil",
  ),
  DashboardNavItem(
    route = ROUTE_TASBIH,
    label = "Tasbih",
    selectedIcon = Icons.Filled.TouchApp,
    unselectedIcon = Icons.Outlined.TouchApp,
    testTag = "nav_tasbih",
  ),
  DashboardNavItem(
    route = ROUTE_ALMARHUM_LIST,
    label = "Almarhum List",
    selectedIcon = Icons.Filled.People,
    unselectedIcon = Icons.Outlined.People,
    testTag = "nav_almarhum_list",
  ),
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val settingsRepo = ReaderSettingsRepository(applicationContext)

    setContent {
      val settings by settingsRepo.settingsFlow.collectAsStateWithLifecycle(
        initialValue = ReaderSettings()
      )
      YasinTahlilTheme(darkTheme = settings.nightMode) {
        AppRoot(settingsRepo = settingsRepo, rootSettings = settings)
      }
    }
  }
}

@Composable
private fun AppRoot(
  settingsRepo: ReaderSettingsRepository,
  rootSettings: ReaderSettings,
) {
  val viewModel: YasinTahlilViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        YasinTahlilViewModel(
          userDataRepository = UserDataRepository(app),
          settingsRepository = settingsRepo,
        )
      }
    },
  )

  MainDashboardShell(
    viewModel = viewModel,
    fallbackSettings = rootSettings,
  )
}

@Composable
fun MainDashboardShell(
  viewModel: YasinTahlilViewModel,
  fallbackSettings: ReaderSettings,
) {
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }

  val settings by viewModel.readerSettings.collectAsStateWithLifecycle(initialValue = fallbackSettings)
  val readingState by viewModel.readingState.collectAsStateWithLifecycle()
  val deceasedState by viewModel.deceasedPrayersState.collectAsStateWithLifecycle()
  val tasbihUi by viewModel.tasbihUi.collectAsStateWithLifecycle()
  val stepCounts by viewModel.tahlilStepCounts.collectAsStateWithLifecycle()

  var showDisplaySheet by remember { mutableStateOf(false) }
  var surahTahlilSubSection by rememberSaveable { mutableStateOf(SurahTahlilSubSection.DASHBOARD) }
  var pendingYasinVerse by remember { mutableStateOf<Int?>(null) }
  var pendingShortSurahTab by remember { mutableStateOf(false) }

  LaunchedEffect(viewModel) {
    viewModel.events.collectLatest { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination

  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 840.dp

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useNavRail) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("main_bottom_navigation"),
          ) {
            dashboardDestinations.forEach { dest ->
              val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(dest.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = dest.label,
                  )
                },
                label = {
                  Text(
                    text = dest.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
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
          .padding(innerPadding),
      ) {
        if (useNavRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .fillMaxHeight()
              .testTag("main_navigation_rail"),
          ) {
            Spacer(modifier = Modifier.weight(1f))
            dashboardDestinations.forEach { dest ->
              val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(dest.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = dest.label,
                  )
                },
                label = {
                  Text(
                    text = dest.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                modifier = Modifier.testTag(dest.testTag),
              )
            }
            Spacer(modifier = Modifier.weight(1f))
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        ) {
          NavHost(
            navController = navController,
            startDestination = ROUTE_SURAH_TAHLIL,
          ) {
            composable(ROUTE_SURAH_TAHLIL) {
              SurahTahlilDashboardScreen(
                activeSubSection = surahTahlilSubSection,
                onSubSectionChange = { surahTahlilSubSection = it },
                readingState = readingState,
                deceasedState = deceasedState,
                settings = settings,
                lifetimeTasbihCount = tasbihUi.lifetimeCount,
                pendingYasinVerse = pendingYasinVerse,
                pendingShortSurahTab = pendingShortSurahTab,
                stepCounts = stepCounts,
                onSaveLastReadVerse = { verse -> viewModel.saveLastYasinVerse(verse) },
                onToggleBookmark = { verse -> viewModel.toggleYasinBookmark(verse) },
                onConsumeTargetVerse = { pendingYasinVerse = null },
                onIncrementStepCount = { stepId, target ->
                  viewModel.incrementTahlilStepCount(stepId, target)
                },
                onResetStepCount = { stepId -> viewModel.resetTahlilStepCount(stepId) },
                onSaveTahlilStep = { stepIdx -> viewModel.saveLastTahlilStep(stepIdx) },
                onConsumeShortSurahTab = { pendingShortSurahTab = false },
                onOpenYasinAt = { startVerse ->
                  pendingYasinVerse = startVerse
                  surahTahlilSubSection = SurahTahlilSubSection.YASIN
                },
                onOpenTahlil = { openShortSurahs ->
                  pendingShortSurahTab = openShortSurahs
                  surahTahlilSubSection = if (openShortSurahs) {
                    SurahTahlilSubSection.SHORT_SURAHS
                  } else {
                    SurahTahlilSubSection.TAHLIL
                  }
                },
                onSwitchToTasbih = { navigateToTab(ROUTE_TASBIH) },
                onSwitchToAlmarhumList = { navigateToTab(ROUTE_ALMARHUM_LIST) },
                onToggleNightMode = { viewModel.setNightMode(!settings.nightMode) },
                onOpenDisplaySettings = { showDisplaySheet = true },
              )
            }

            composable(ROUTE_TASBIH) {
              TasbihScreen(
                tasbihUi = tasbihUi,
                settings = settings,
                onIncrement = { viewModel.incrementTasbih() },
                onResetCurrent = { viewModel.resetTasbihCurrent() },
                onSelectPreset = { id -> viewModel.selectDhikrPreset(id) },
                onSelectTarget = { target -> viewModel.setTasbihTarget(target) },
                onToggleHaptic = { enabled -> viewModel.setHapticEnabled(enabled) },
              )
            }

            composable(ROUTE_ALMARHUM_LIST) {
              ArwahListScreen(
                deceasedState = deceasedState,
                onAddDeceased = { honorific, fullName, parentPrefix, parentName, relationNote, finish ->
                  viewModel.addDeceasedPrayer(
                    honorific = honorific,
                    fullName = fullName,
                    parentPrefix = parentPrefix,
                    parentName = parentName,
                    relationNote = relationNote,
                    onComplete = finish,
                  )
                },
                onUpdateDeceased = { entryId, honorific, fullName, parentPrefix, parentName, relationNote, included, finish ->
                  viewModel.updateDeceasedPrayer(
                    entryId = entryId,
                    honorific = honorific,
                    fullName = fullName,
                    parentPrefix = parentPrefix,
                    parentName = parentName,
                    relationNote = relationNote,
                    includedInPrayer = included,
                    onComplete = finish,
                  )
                },
                onToggleIncluded = { entry -> viewModel.toggleDeceasedIncluded(entry) },
                onDeleteDeceased = { entryId -> viewModel.deleteDeceasedPrayer(entryId) },
              )
            }
          }
        }
      }
    }
  }

  if (showDisplaySheet) {
    ReaderDisplaySheet(
      settings = settings,
      onFontSizeChange = { viewModel.updateArabicFontSize(it) },
      onShowLatinChange = { viewModel.setShowLatin(it) },
      onShowTranslationChange = { viewModel.setShowTranslation(it) },
      onNightModeChange = { viewModel.setNightMode(it) },
      onHapticChange = { viewModel.setHapticEnabled(it) },
      onDismiss = { showDisplaySheet = false },
    )
  }
}
