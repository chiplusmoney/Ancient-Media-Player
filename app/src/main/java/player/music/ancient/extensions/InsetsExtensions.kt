package player.music.ancient.extensions

import androidx.core.view.WindowInsetsCompat
import player.music.ancient.util.PreferenceUtil
import player.music.ancient.util.AncientUtil

fun WindowInsetsCompat?.getBottomInsets(): Int {
    return if (PreferenceUtil.isFullScreenMode) {
        return 0
    } else {
        this?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: AncientUtil.navigationBarHeight
    }
}
