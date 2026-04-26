package player.music.ancient.activities.bugreport.model

import player.music.ancient.activities.bugreport.model.github.ExtraInfo

class Report(
    val title: String,
    private val description: String,
    private val deviceInfo: DeviceInfo?,
    private val extraInfo: ExtraInfo
) {
    fun getDescription(): String {
        return """
            $description
            
            -
            
            ${deviceInfo?.toMarkdown()}
            
            ${extraInfo.toMarkdown()}
            """.trimIndent()
    }
}