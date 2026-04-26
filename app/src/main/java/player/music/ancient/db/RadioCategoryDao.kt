package player.music.ancient.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RadioCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadioCategory(radioCategory: RadioCategoryEntity): Long

    @Delete
    suspend fun deleteRadioCategory(radioCategory: RadioCategoryEntity)

    @Update
    suspend fun updateRadioCategory(radioCategory: RadioCategoryEntity)

    @Query("SELECT * FROM radio_categories ORDER BY name COLLATE NOCASE ASC")
    fun getAllRadioCategories(): LiveData<List<RadioCategoryEntity>>
}
