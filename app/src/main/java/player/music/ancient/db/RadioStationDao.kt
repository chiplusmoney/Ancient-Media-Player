package player.music.ancient.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RadioStationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadioStation(radioStation: RadioStationEntity): Long

    @Delete
    suspend fun deleteRadioStation(radioStation: RadioStationEntity)

    @Update
    suspend fun updateRadioStation(radioStation: RadioStationEntity)

    @Query("UPDATE radio_stations SET category_id = NULL WHERE category_id = :categoryId")
    suspend fun clearCategoryFromStations(categoryId: Long)

    @Query("SELECT * FROM radio_stations ORDER BY name COLLATE NOCASE ASC")
    fun getAllRadioStations(): LiveData<List<RadioStationEntity>>

    @Query("SELECT * FROM radio_stations ORDER BY name COLLATE NOCASE ASC")
    fun getAllRadioStationsSync(): List<RadioStationEntity>

    @Query("SELECT * FROM radio_stations WHERE station_id = :id")
    suspend fun getRadioStationById(id: Long): RadioStationEntity?
}
