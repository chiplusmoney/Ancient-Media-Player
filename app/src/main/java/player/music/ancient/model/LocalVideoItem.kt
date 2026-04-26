package player.music.ancient.model

data class LocalVideoItem(
    val id: Long,
    val title: String,
    val uri: String,
    val folderName: String,
    val durationMs: Long,
    val dateAdded: Long
)
