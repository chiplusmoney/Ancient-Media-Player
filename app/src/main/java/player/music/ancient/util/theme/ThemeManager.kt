package player.music.ancient.util.theme

import android.content.Context
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import player.music.ancient.R
import player.music.ancient.extensions.generalThemeValue
import player.music.ancient.util.PreferenceUtil
import player.music.ancient.util.theme.ThemeMode.*

@StyleRes
fun Context.getThemeResValue(): Int =
    if (PreferenceUtil.materialYou) {
        if (generalThemeValue == BLACK) R.style.Theme_AncientMusic_MD3_Black
        else R.style.Theme_AncientMusic_MD3
    } else {
        when (generalThemeValue) {
            LIGHT -> R.style.Theme_AncientMusic_Light
            DARK -> R.style.Theme_AncientMusic
            BLACK -> R.style.Theme_AncientMusic_Black
            AUTO -> R.style.Theme_AncientMusic_FollowSystem
        }
    }

fun Context.getNightMode(): Int = when (generalThemeValue) {
    LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    DARK -> AppCompatDelegate.MODE_NIGHT_YES
    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}