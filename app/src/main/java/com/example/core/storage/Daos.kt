package com.example.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_games WHERE slotId = 'active_slot' LIMIT 1")
    fun observeActiveSave(): Flow<SavedGameEntity?>

    @Query("SELECT * FROM saved_games WHERE slotId = 'active_slot' LIMIT 1")
    suspend fun getActiveSave(): SavedGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSave(entity: SavedGameEntity)

    @Query("DELETE FROM saved_games WHERE slotId = 'active_slot'")
    suspend fun clearActiveSave()
}

@Dao
interface MatchHistoryDao {
    @Query("SELECT * FROM match_history ORDER BY playedAt DESC LIMIT 40")
    fun observeHistory(): Flow<List<MatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(entity: MatchHistoryEntity)
}

@Dao
interface FavoriteGameDao {
    @Query("SELECT gameId FROM favorite_games ORDER BY addedAt DESC")
    fun observeFavoriteIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteGameEntity)

    @Query("DELETE FROM favorite_games WHERE gameId = :gameId")
    suspend fun removeFavorite(gameId: String)
}

@Dao
interface PlayerProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: PlayerProfileEntity)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(entity: AppSettingsEntity)
}
