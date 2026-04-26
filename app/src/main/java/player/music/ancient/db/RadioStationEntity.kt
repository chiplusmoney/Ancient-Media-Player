package player.music.ancient.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "radio_stations")
data class RadioStationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "station_id")
    val id: Long = 0,
    val name: String,
    val uri: String,
    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null
) : Parcelable
