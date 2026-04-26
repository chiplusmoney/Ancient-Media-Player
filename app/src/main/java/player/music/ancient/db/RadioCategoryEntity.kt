package player.music.ancient.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "radio_categories")
data class RadioCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null
) : Parcelable
