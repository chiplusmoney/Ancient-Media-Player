/*
 * Copyright (c) 2020 Future Eternal.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package player.music.ancient.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        SongEntity::class,
        HistoryEntity::class,
        PlayCountEntity::class,
        RadioStationEntity::class,
        RadioCategoryEntity::class,
        TvChannelEntity::class,
        TvCategoryEntity::class,
        YoutubeChannelEntity::class,
        YoutubeVideoEntity::class
    ],
    version = 28,
    exportSchema = false
)
abstract class AncientDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playCountDao(): PlayCountDao
    abstract fun historyDao(): HistoryDao
    abstract fun radioStationDao(): RadioStationDao
    abstract fun radioCategoryDao(): RadioCategoryDao
    abstract fun tvChannelDao(): TvChannelDao
    abstract fun tvCategoryDao(): TvCategoryDao
    abstract fun youtubeChannelDao(): YoutubeChannelDao
    abstract fun youtubeVideoDao(): YoutubeVideoDao
}
