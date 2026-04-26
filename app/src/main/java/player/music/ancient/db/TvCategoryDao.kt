package player.music.ancient.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TvCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTvCategory(tvCategory: TvCategoryEntity): Long

    @Delete
    suspend fun deleteTvCategory(tvCategory: TvCategoryEntity)

    @Update
    suspend fun updateTvCategory(tvCategory: TvCategoryEntity)

    @Query("SELECT * FROM tv_categories ORDER BY name COLLATE NOCASE ASC")
    fun getAllTvCategories(): LiveData<List<TvCategoryEntity>>
}
