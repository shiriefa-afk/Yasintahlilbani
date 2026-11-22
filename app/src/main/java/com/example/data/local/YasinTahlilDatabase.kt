package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "deceased_prayers")
data class DeceasedPrayerEntity(
  @PrimaryKey val id: String,
  val honorific: String,
  val fullName: String,
  val parentPrefix: String,
  val parentName: String,
  val relationNote: String?,
  val includedInPrayer: Boolean,
  val createdAtMillis: Long,
  val updatedAtMillis: Long,
)

@Entity(tableName = "reading_state")
data class ReadingStateEntity(
  @PrimaryKey val id: Int = 1,
  val lastYasinVerse: Long = 1L,
  val lastTahlilStep: Long = 0L,
  val bookmarkedVersesCsv: String = "",
  val updatedAtMillis: Long = 0L,
)

@Entity(tableName = "tasbih_state")
data class TasbihStateEntity(
  @PrimaryKey val id: Int = 1,
  val selectedDhikrId: String = "subhanallah",
  val currentCount: Long = 0L,
  val targetCount: Long = 33L,
  val lifetimeCount: Long = 0L,
  val updatedAtMillis: Long = 0L,
)

@Dao
interface YasinTahlilDao {
  @Query("SELECT * FROM deceased_prayers ORDER BY createdAtMillis DESC")
  fun observeDeceasedPrayers(): Flow<List<DeceasedPrayerEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDeceasedPrayer(entity: DeceasedPrayerEntity)

  @Query(
    """
    UPDATE deceased_prayers
    SET honorific = :honorific,
        fullName = :fullName,
        parentPrefix = :parentPrefix,
        parentName = :parentName,
        relationNote = :relationNote,
        includedInPrayer = :includedInPrayer,
        updatedAtMillis = :updatedAtMillis
    WHERE id = :id
    """
  )
  suspend fun updateDeceasedPrayer(
    id: String,
    honorific: String,
    fullName: String,
    parentPrefix: String,
    parentName: String,
    relationNote: String?,
    includedInPrayer: Boolean,
    updatedAtMillis: Long,
  ): Int

  @Query(
    """
    UPDATE deceased_prayers
    SET includedInPrayer = :includedInPrayer,
        updatedAtMillis = :updatedAtMillis
    WHERE id = :id
    """
  )
  suspend fun updateDeceasedPrayerIncluded(
    id: String,
    includedInPrayer: Boolean,
    updatedAtMillis: Long,
  ): Int

  @Query("DELETE FROM deceased_prayers WHERE id = :id")
  suspend fun deleteDeceasedPrayerById(id: String): Int

  @Query("SELECT * FROM reading_state WHERE id = 1")
  fun observeReadingState(): Flow<ReadingStateEntity?>

  @Query("SELECT * FROM reading_state WHERE id = 1")
  suspend fun getReadingStateOnce(): ReadingStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertReadingState(entity: ReadingStateEntity)

  @Transaction
  suspend fun updateLastYasinVerse(verse: Long, updatedAtMillis: Long) {
    val current = getReadingStateOnce() ?: ReadingStateEntity()
    upsertReadingState(
      current.copy(
        id = 1,
        lastYasinVerse = verse,
        updatedAtMillis = updatedAtMillis,
      )
    )
  }

  @Transaction
  suspend fun updateLastTahlilStep(step: Long, updatedAtMillis: Long) {
    val current = getReadingStateOnce() ?: ReadingStateEntity()
    upsertReadingState(
      current.copy(
        id = 1,
        lastTahlilStep = step,
        updatedAtMillis = updatedAtMillis,
      )
    )
  }

  @Transaction
  suspend fun updateBookmarkedVerses(csv: String, updatedAtMillis: Long) {
    val current = getReadingStateOnce() ?: ReadingStateEntity()
    upsertReadingState(
      current.copy(
        id = 1,
        bookmarkedVersesCsv = csv,
        updatedAtMillis = updatedAtMillis,
      )
    )
  }

  @Query("SELECT * FROM tasbih_state WHERE id = 1")
  fun observeTasbihState(): Flow<TasbihStateEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTasbihState(entity: TasbihStateEntity)
}

@Database(
  entities = [
    DeceasedPrayerEntity::class,
    ReadingStateEntity::class,
    TasbihStateEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class YasinTahlilDatabase : RoomDatabase() {
  abstract fun yasinTahlilDao(): YasinTahlilDao

  companion object {
    @Volatile
    private var instance: YasinTahlilDatabase? = null

    fun getInstance(context: Context): YasinTahlilDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          YasinTahlilDatabase::class.java,
          "yasin_tahlil_db",
        ).build().also { instance = it }
      }
    }
  }
}
