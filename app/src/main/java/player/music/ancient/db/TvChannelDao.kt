package player.music.ancient.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TvChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTvChannel(tvChannel: TvChannelEntity): Long

    @Delete
    suspend fun deleteTvChannel(tvChannel: TvChannelEntity)

    @Update
    suspend fun updateTvChannel(tvChannel: TvChannelEntity)

    @Query("UPDATE tv_channels SET category_id = NULL WHERE category_id = :categoryId")
    suspend fun clearCategoryFromChannels(categoryId: Long)

    @Query("SELECT * FROM tv_channels ORDER BY name COLLATE NOCASE ASC")
    fun getAllTvChannels(): LiveData<List<TvChannelEntity>>
}
