package player.music.ancient.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS LyricsEntity")
        database.execSQL("DROP TABLE IF EXISTS BlackListStoreEntity")
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `radio_stations` (`station_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `uri` TEXT NOT NULL, `image_uri` TEXT)"
        )
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `radio_categories` (`category_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `image_uri` TEXT)"
        )
        database.execSQL(
            "ALTER TABLE `radio_stations` ADD COLUMN `category_id` INTEGER"
        )
    }
}

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `tv_categories` (`category_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `image_uri` TEXT)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `tv_channels` (`channel_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `image_uri` TEXT, `category_id` INTEGER)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `youtube_channels` (`channel_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `image_uri` TEXT)"
        )
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `youtube_videos` (" +
                "`video_id` TEXT NOT NULL, " +
                "`source_channel_id` INTEGER NOT NULL, " +
                "`youtube_channel_id` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, " +
                "`published_at` TEXT NOT NULL, " +
                "`channel_title` TEXT NOT NULL, " +
                "`thumbnail_url` TEXT, " +
                "PRIMARY KEY(`video_id`)" +
                ")"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_youtube_videos_source_channel_id` " +
                "ON `youtube_videos` (`source_channel_id`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_youtube_videos_published_at` " +
                "ON `youtube_videos` (`published_at`)"
        )
    }
}
