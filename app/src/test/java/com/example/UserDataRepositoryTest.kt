package com.example

import com.example.data.UserDataRepository
import com.example.data.local.DeceasedPrayerEntity
import com.example.data.local.ReadingStateEntity
import com.example.data.local.TasbihStateEntity
import com.example.data.local.YasinTahlilDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeYasinTahlilDao : YasinTahlilDao {
  private val prayers = MutableStateFlow<List<DeceasedPrayerEntity>>(emptyList())
  private val reading = MutableStateFlow<ReadingStateEntity?>(null)
  private val tasbih = MutableStateFlow<TasbihStateEntity?>(null)

  override fun observeDeceasedPrayers(): Flow<List<DeceasedPrayerEntity>> =
    prayers.map { list -> list.sortedByDescending { it.createdAtMillis } }

  override suspend fun insertDeceasedPrayer(entity: DeceasedPrayerEntity) {
    prayers.update { list -> list.filterNot { it.id == entity.id } + entity }
  }

  override suspend fun updateDeceasedPrayer(
    id: String,
    honorific: String,
    fullName: String,
    parentPrefix: String,
    parentName: String,
    relationNote: String?,
    includedInPrayer: Boolean,
    updatedAtMillis: Long,
  ): Int {
    var count = 0
    prayers.update { list ->
      list.map { item ->
        if (item.id == id) {
          count++
          item.copy(
            honorific = honorific,
            fullName = fullName,
            parentPrefix = parentPrefix,
            parentName = parentName,
            relationNote = relationNote,
            includedInPrayer = includedInPrayer,
            updatedAtMillis = updatedAtMillis,
          )
        } else {
          item
        }
      }
    }
    return count
  }

  override suspend fun updateDeceasedPrayerIncluded(
    id: String,
    includedInPrayer: Boolean,
    updatedAtMillis: Long,
  ): Int {
    var count = 0
    prayers.update { list ->
      list.map { item ->
        if (item.id == id) {
          count++
          item.copy(includedInPrayer = includedInPrayer, updatedAtMillis = updatedAtMillis)
        } else {
          item
        }
      }
    }
    return count
  }

  override suspend fun deleteDeceasedPrayerById(id: String): Int {
    val before = prayers.value.size
    prayers.update { list -> list.filterNot { it.id == id } }
    return before - prayers.value.size
  }

  override fun observeReadingState(): Flow<ReadingStateEntity?> = reading

  override suspend fun getReadingStateOnce(): ReadingStateEntity? = reading.value

  override suspend fun upsertReadingState(entity: ReadingStateEntity) {
    reading.value = entity
  }

  override suspend fun updateLastYasinVerse(verse: Long, updatedAtMillis: Long) {
    val current = reading.value ?: ReadingStateEntity()
    reading.value = current.copy(id = 1, lastYasinVerse = verse, updatedAtMillis = updatedAtMillis)
  }

  override suspend fun updateLastTahlilStep(step: Long, updatedAtMillis: Long) {
    val current = reading.value ?: ReadingStateEntity()
    reading.value = current.copy(id = 1, lastTahlilStep = step, updatedAtMillis = updatedAtMillis)
  }

  override suspend fun updateBookmarkedVerses(csv: String, updatedAtMillis: Long) {
    val current = reading.value ?: ReadingStateEntity()
    reading.value = current.copy(id = 1, bookmarkedVersesCsv = csv, updatedAtMillis = updatedAtMillis)
  }

  override fun observeTasbihState(): Flow<TasbihStateEntity?> = tasbih

  override suspend fun upsertTasbihState(entity: TasbihStateEntity) {
    tasbih.value = entity
  }
}

class UserDataRepositoryTest {
  private lateinit var repository: UserDataRepository

  @Before
  fun setUp() {
    repository = UserDataRepository(FakeYasinTahlilDao())
  }

  @Test
  fun `deceased prayer CRUD updates state and formatting`() = runBlocking {
    val addResult = repository.addDeceasedPrayer(
      honorific = "Almarhum",
      fullName = "H. Ahmad",
      parentPrefix = "bin",
      parentName = "Abdullah",
      relationNote = "Ayahanda",
    )
    assertTrue(addResult.isSuccess)

    val list = repository.observeDeceasedPrayers().first()
    assertEquals(1, list.size)
    val entry = list.first()
    assertEquals("Almarhum H. Ahmad bin Abdullah", entry.formattedPrayerName())
    assertTrue(entry.includedInPrayer)

    assertTrue(repository.toggleDeceasedPrayerIncluded(entry.id, false).isSuccess)
    val toggled = repository.observeDeceasedPrayers().first().first()
    assertFalse(toggled.includedInPrayer)

    assertTrue(
      repository.updateDeceasedPrayer(
        entryId = entry.id,
        honorific = "Almarhumah",
        fullName = "Hj. Siti Aminah",
        parentPrefix = "binti",
        parentName = "Hasan",
        relationNote = "Ibunda",
        includedInPrayer = true,
      ).isSuccess
    )
    val updated = repository.observeDeceasedPrayers().first().first()
    assertEquals("Almarhumah Hj. Siti Aminah binti Hasan", updated.formattedPrayerName())
    assertTrue(updated.includedInPrayer)

    assertTrue(repository.deleteDeceasedPrayer(entry.id).isSuccess)
    assertTrue(repository.observeDeceasedPrayers().first().isEmpty())
  }

  @Test
  fun `atomic reading state updates preserve independent fields`() = runBlocking {
    assertTrue(repository.saveLastYasinVerse(36).isSuccess)
    val firstBookmark = repository.toggleYasinBookmark(58)
    assertTrue(firstBookmark.isSuccess)
    assertEquals(true, firstBookmark.getOrThrow().isBookmarked)
    val secondBookmark = repository.toggleYasinBookmark(1)
    assertTrue(secondBookmark.isSuccess)
    assertEquals(true, secondBookmark.getOrThrow().isBookmarked)
    assertTrue(repository.saveLastTahlilStep(7).isSuccess)

    val reading = repository.observeReadingState().first()
    assertEquals(36L, reading.lastYasinVerse)
    assertEquals(7L, reading.lastTahlilStep)
    assertEquals(listOf(1L, 58L), reading.bookmarkedVerses)

    val removedBookmark = repository.toggleYasinBookmark(58)
    assertTrue(removedBookmark.isSuccess)
    assertEquals(false, removedBookmark.getOrThrow().isBookmarked)
    val afterRemove = repository.observeReadingState().first()
    assertEquals(listOf(1L), afterRemove.bookmarkedVerses)
    assertEquals(36L, afterRemove.lastYasinVerse)
    assertEquals(7L, afterRemove.lastTahlilStep)
  }

  @Test
  fun `tasbih state clamps and persists values`() = runBlocking {
    assertTrue(
      repository.saveTasbihState(
        selectedDhikrId = "alhamdulillah",
        currentCount = 12L,
        targetCount = 33L,
        lifetimeCount = 145L,
      ).isSuccess
    )
    val tasbih = repository.observeTasbihState().first()
    assertEquals("alhamdulillah", tasbih.selectedDhikrId)
    assertEquals(12L, tasbih.currentCount)
    assertEquals(33L, tasbih.targetCount)
    assertEquals(145L, tasbih.lifetimeCount)
  }
}
