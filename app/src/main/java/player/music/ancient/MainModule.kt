package player.music.ancient

import androidx.room.Room
import player.music.ancient.auto.AutoMusicProvider
import player.music.ancient.cast.AncientWebServer
import player.music.ancient.db.MIGRATION_23_24
import player.music.ancient.db.MIGRATION_24_25
import player.music.ancient.db.MIGRATION_25_26
import player.music.ancient.db.MIGRATION_26_27
import player.music.ancient.db.MIGRATION_27_28
import player.music.ancient.db.AncientDatabase
import player.music.ancient.fragments.LibraryViewModel
import player.music.ancient.fragments.albums.AlbumDetailsViewModel
import player.music.ancient.fragments.artists.ArtistDetailsViewModel
import player.music.ancient.fragments.genres.GenreDetailsViewModel
import player.music.ancient.fragments.playlists.PlaylistDetailsViewModel
import player.music.ancient.fragments.radio.RadioViewModel
import player.music.ancient.fragments.tv.TvViewModel
import player.music.ancient.fragments.youtube.YoutubeViewModel
import player.music.ancient.model.Genre
import player.music.ancient.network.provideDefaultCache
import player.music.ancient.network.provideLastFmRest
import player.music.ancient.network.provideLastFmRetrofit
import player.music.ancient.network.provideOkHttp
import player.music.ancient.network.provideYoutubeRest
import player.music.ancient.repository.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {

    factory {
        provideDefaultCache()
    }
    factory {
        provideOkHttp(get(), get())
    }
    single {
        provideLastFmRetrofit(get())
    }
    single {
        provideLastFmRest(get())
    }
    single {
        provideYoutubeRest(get())
    }
}

private val roomModule = module {

    single {
        Room.databaseBuilder(androidContext(), AncientDatabase::class.java, "playlist.db")
            .addMigrations(
                MIGRATION_23_24,
                MIGRATION_24_25,
                MIGRATION_25_26,
                MIGRATION_26_27,
                MIGRATION_27_28
            )
            .build()
    }

    factory {
        get<AncientDatabase>().playlistDao()
    }

    factory {
        get<AncientDatabase>().playCountDao()
    }

    factory {
        get<AncientDatabase>().historyDao()
    }

    factory {
        get<AncientDatabase>().radioStationDao()
    }

    factory {
        get<AncientDatabase>().radioCategoryDao()
    }

    single {
        get<AncientDatabase>().tvChannelDao()
    }

    factory {
        get<AncientDatabase>().tvCategoryDao()
    }

    factory {
        get<AncientDatabase>().youtubeChannelDao()
    }

    factory {
        get<AncientDatabase>().youtubeVideoDao()
    }

    single {
        RealRoomRepository(get(), get(), get(), get(), get(), get(), get(), get(), get())
    } bind RoomRepository::class
}
private val autoModule = module {
    single {
        AutoMusicProvider(
            androidContext(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
}
private val mainModule = module {
    single {
        androidContext().contentResolver
    }
    single {
        AncientWebServer(get())
    }
}
private val dataModule = module {
    single {
        RealRepository(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    } bind Repository::class

    single {
        RealSongRepository(get())
    } bind SongRepository::class

    single {
        RealGenreRepository(get(), get())
    } bind GenreRepository::class

    single {
        RealAlbumRepository(get())
    } bind AlbumRepository::class

    single {
        RealArtistRepository(get(), get())
    } bind ArtistRepository::class

    single {
        RealPlaylistRepository(get())
    } bind PlaylistRepository::class

    single {
        RealTopPlayedRepository(get(), get(), get(), get())
    } bind TopPlayedRepository::class

    single {
        RealLastAddedRepository(
            get(),
            get(),
            get()
        )
    } bind LastAddedRepository::class

    single {
        RealSearchRepository(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    single {
        RealLocalDataRepository(get())
    } bind LocalDataRepository::class
}

private val viewModules = module {

    viewModel {
        LibraryViewModel(get())
    }

    viewModel { (albumId: Long) ->
        AlbumDetailsViewModel(
            get(),
            albumId
        )
    }

    viewModel { (artistId: Long?, artistName: String?) ->
        ArtistDetailsViewModel(
            get(),
            artistId,
            artistName
        )
    }

    viewModel { (playlistId: Long) ->
        PlaylistDetailsViewModel(
            get(),
            playlistId
        )
    }

    viewModel { (genre: Genre) ->
        GenreDetailsViewModel(
            get(),
            genre
        )
    }

    viewModel {
        RadioViewModel(get())
    }

    viewModel {
        TvViewModel(get())
    }

    viewModel {
        YoutubeViewModel(get(), get())
    }
}

val appModules = listOf(mainModule, dataModule, autoModule, viewModules, networkModule, roomModule)
