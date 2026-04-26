package player.music.ancient.interfaces

import android.view.View
import player.music.ancient.model.Genre

interface IGenreClickListener {
    fun onClickGenre(genre: Genre, view: View)
}