package player.music.ancient.model.smartplaylist

import androidx.annotation.DrawableRes
import player.music.ancient.R
import player.music.ancient.model.AbsCustomPlaylist

abstract class AbsSmartPlaylist(
    name: String,
    @DrawableRes val iconRes: Int = R.drawable.ic_queue_music
) : AbsCustomPlaylist(
    id = PlaylistIdGenerator(name, iconRes),
    name = name
)