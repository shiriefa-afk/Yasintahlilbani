package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ReaderSettingsRepository
import com.example.data.TahlilAndSurahsData
import com.example.data.UserDataRepository
import com.example.model.DeceasedPrayer
import com.example.model.ReaderSettings
import com.example.model.ReadingState
import com.example.model.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TasbihUiState(
  val selectedDhikrId: String = "subhanallah",
  val currentCount: Long = 0L,
  val targetCount: Long = 33L,
  val lifetimeCount: Long = 0L,
  val initializedFromDb: Boolean = false,
)

class YasinTahlilViewModel(
  private val userDataRepository: UserDataRepository,
  private val settingsRepository: ReaderSettingsRepository,
) : ViewModel() {

  private val _cloudAccount = MutableStateFlow(userDataRepository.cloudAccountEmail().orEmpty())
  val cloudAccount: StateFlow<String> = _cloudAccount.asStateFlow()
  private val _cloudStatus = MutableStateFlow("")
  val cloudStatus: StateFlow<String> = _cloudStatus.asStateFlow()

  fun signInWithGoogle(idToken: String) {
    viewModelScope.launch {
      _cloudStatus.value = "Menghubungkan dan menyinkronkan..."
      userDataRepository.signInWithGoogle(idToken)
        .onSuccess { email ->
          _cloudAccount.value = email
          _cloudStatus.value = "Daftar nama arwah telah disinkronkan."
        }
        .onFailure { error ->
          _cloudStatus.value = error.localizedMessage ?: "Gagal menghubungkan akun."
        }
    }
  }

  fun syncCloud() {
    viewModelScope.launch {
      _cloudStatus.value = "Menyinkronkan daftar..."
      userDataRepository.syncCloud()
        .onSuccess { _cloudStatus.value = "Sinkronisasi selesai." }
        .onFailure { error -> _cloudStatus.value = error.localizedMessage ?: "Sinkronisasi gagal." }
    }
  }

  fun signOutCloud() {
    userDataRepository.signOutCloud()
    _cloudAccount.value = ""
    _cloudStatus.value = "Akun dilepas. Salinan lokal tetap tersimpan di perangkat ini."
  }

  val readerSettings: StateFlow<ReaderSettings> = settingsRepository.settingsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = ReaderSettings(),
    )

  val deceasedPrayersState: StateFlow<UiState<List<DeceasedPrayer>>> =
    userDataRepository.observeDeceasedPrayers()
      .map<List<DeceasedPrayer>, UiState<List<DeceasedPrayer>>> { UiState.Success(it) }
      .catch { error ->
        if (error is CancellationException) throw error
        Log.w(TAG, "Error observing deceased prayers", error)
        emit(UiState.Error(error.localizedMessage ?: "Gagal memuat daftar nama arwah."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val readingState: StateFlow<UiState<ReadingState>> =
    userDataRepository.observeReadingState()
      .map<ReadingState, UiState<ReadingState>> { UiState.Success(it) }
      .catch { error ->
        if (error is CancellationException) throw error
        Log.w(TAG, "Error observing reading state", error)
        emit(UiState.Error(error.localizedMessage ?: "Gagal memuat penanda bacaan."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private val _tasbihUi = MutableStateFlow(TasbihUiState())
  val tasbihUi: StateFlow<TasbihUiState> = _tasbihUi.asStateFlow()

  private val _tahlilStepCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
  val tahlilStepCounts: StateFlow<Map<String, Int>> = _tahlilStepCounts.asStateFlow()

  private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val events: SharedFlow<String> = _events.asSharedFlow()

  private var tasbihSyncJob: Job? = null

  init {
    viewModelScope.launch {
      userDataRepository.observeTasbihState()
        .catch { error ->
          if (error is CancellationException) throw error
          Log.w(TAG, "Error observing tasbih state", error)
        }
        .collectLatest { saved ->
          _tasbihUi.update { current ->
            if (!current.initializedFromDb || saved.lifetimeCount > current.lifetimeCount) {
              TasbihUiState(
                selectedDhikrId = saved.selectedDhikrId.ifBlank { "subhanallah" },
                currentCount = saved.currentCount,
                targetCount = saved.targetCount,
                lifetimeCount = saved.lifetimeCount,
                initializedFromDb = true,
              )
            } else {
              current.copy(initializedFromDb = true)
            }
          }
        }
    }
  }

  fun updateArabicFontSize(sizeSp: Float) {
    viewModelScope.launch { settingsRepository.updateArabicFontSize(sizeSp) }
  }

  fun setShowLatin(show: Boolean) {
    viewModelScope.launch { settingsRepository.setShowLatin(show) }
  }

  fun setShowTranslation(show: Boolean) {
    viewModelScope.launch { settingsRepository.setShowTranslation(show) }
  }

  fun setNightMode(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setNightMode(enabled) }
  }

  fun setHapticEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setHapticEnabled(enabled) }
  }

  fun saveLastYasinVerse(verseNumber: Int) {
    val current = (readingState.value as? UiState.Success)?.data
    if (current != null && current.lastYasinVerse.toInt() == verseNumber) return
    viewModelScope.launch {
      userDataRepository.saveLastYasinVerse(verseNumber)
        .onSuccess {
          _events.tryEmit("Batas bacaan Yasin disimpan di ayat $verseNumber.")
        }
    }
  }

  fun saveLastTahlilStep(stepIndex: Int) {
    val current = (readingState.value as? UiState.Success)?.data
    if (current != null && current.lastTahlilStep.toInt() == stepIndex) return
    viewModelScope.launch {
      userDataRepository.saveLastTahlilStep(stepIndex)
    }
  }

  fun toggleYasinBookmark(verseNumber: Int) {
    viewModelScope.launch {
      userDataRepository.toggleYasinBookmark(verseNumber)
        .onSuccess { outcome ->
          val msg = if (outcome.isBookmarked) {
            "Ayat $verseNumber ditandai."
          } else {
            "Penanda ayat $verseNumber dihapus."
          }
          _events.tryEmit(msg)
        }
        .onFailure {
          _events.tryEmit("Gagal menyimpan penanda ayat.")
        }
    }
  }

  fun incrementTahlilStepCount(stepId: String, target: Int) {
    _tahlilStepCounts.update { map ->
      val current = map[stepId] ?: 0
      val next = if (current >= target) 0 else current + 1
      map + (stepId to next)
    }
  }

  fun resetTahlilStepCount(stepId: String) {
    _tahlilStepCounts.update { it + (stepId to 0) }
  }

  fun incrementTasbih() {
    _tasbihUi.update { state ->
      val nextCount = if (state.targetCount > 0L && state.currentCount >= state.targetCount) {
        1L
      } else {
        state.currentCount + 1L
      }
      state.copy(
        currentCount = nextCount,
        lifetimeCount = state.lifetimeCount + 1L,
        initializedFromDb = true,
      )
    }
    persistTasbihNow()
  }

  fun resetTasbihCurrent() {
    _tasbihUi.update { it.copy(currentCount = 0L, initializedFromDb = true) }
    persistTasbihNow()
  }

  fun selectDhikrPreset(dhikrId: String) {
    val preset = TahlilAndSurahsData.dhikrPresets.firstOrNull { it.id == dhikrId } ?: return
    _tasbihUi.update {
      it.copy(
        selectedDhikrId = preset.id,
        currentCount = 0L,
        targetCount = preset.defaultTarget.toLong(),
        initializedFromDb = true,
      )
    }
    persistTasbihNow()
  }

  fun setTasbihTarget(target: Long) {
    _tasbihUi.update { it.copy(targetCount = target, initializedFromDb = true) }
    persistTasbihNow()
  }

  private fun persistTasbihNow() {
    tasbihSyncJob?.cancel()
    val snap = _tasbihUi.value
    tasbihSyncJob = viewModelScope.launch {
      userDataRepository.saveTasbihState(
        selectedDhikrId = snap.selectedDhikrId,
        currentCount = snap.currentCount,
        targetCount = snap.targetCount,
        lifetimeCount = snap.lifetimeCount,
      )
    }
  }

  fun addDeceasedPrayer(
    honorific: String,
    fullName: String,
    parentPrefix: String,
    parentName: String,
    relationNote: String?,
    onComplete: (Boolean) -> Unit,
  ) {
    viewModelScope.launch {
      val result = userDataRepository.addDeceasedPrayer(
        honorific = honorific,
        fullName = fullName,
        parentPrefix = parentPrefix,
        parentName = parentName,
        relationNote = relationNote,
        includedInPrayer = true,
      )
      result.onSuccess {
        _events.tryEmit("Nama arwah berhasil disimpan.")
        onComplete(true)
      }.onFailure { err ->
        _events.tryEmit(err.localizedMessage ?: "Gagal menyimpan nama arwah.")
        onComplete(false)
      }
    }
  }

  fun updateDeceasedPrayer(
    entryId: String,
    honorific: String,
    fullName: String,
    parentPrefix: String,
    parentName: String,
    relationNote: String?,
    includedInPrayer: Boolean,
    onComplete: (Boolean) -> Unit,
  ) {
    viewModelScope.launch {
      val result = userDataRepository.updateDeceasedPrayer(
        entryId = entryId,
        honorific = honorific,
        fullName = fullName,
        parentPrefix = parentPrefix,
        parentName = parentName,
        relationNote = relationNote,
        includedInPrayer = includedInPrayer,
      )
      result.onSuccess {
        _events.tryEmit("Data arwah diperbarui.")
        onComplete(true)
      }.onFailure { err ->
        _events.tryEmit(err.localizedMessage ?: "Gagal memperbarui data arwah.")
        onComplete(false)
      }
    }
  }

  fun toggleDeceasedIncluded(entry: DeceasedPrayer) {
    viewModelScope.launch {
      val result = userDataRepository.toggleDeceasedPrayerIncluded(
        entryId = entry.id,
        included = !entry.includedInPrayer,
      )
      result.onFailure {
        _events.tryEmit("Gagal mengubah status doa arwah.")
      }
    }
  }

  fun deleteDeceasedPrayer(entryId: String) {
    viewModelScope.launch {
      val result = userDataRepository.deleteDeceasedPrayer(entryId)
      result.onSuccess {
        _events.tryEmit("Nama arwah dihapus.")
      }.onFailure {
        _events.tryEmit("Gagal menghapus nama arwah.")
      }
    }
  }

  private companion object {
    const val TAG = "YasinTahlilVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
