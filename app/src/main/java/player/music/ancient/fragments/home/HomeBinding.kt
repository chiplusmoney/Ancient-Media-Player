package player.music.ancient.fragments.home

import player.music.ancient.databinding.FragmentHomeBinding

class HomeBinding(
    homeBinding: FragmentHomeBinding
) {
    val root = homeBinding.root
    val container = homeBinding.container
    val contentContainer = homeBinding.contentContainer
    val appBarLayout = homeBinding.appBarLayout
    val toolbar = homeBinding.appBarLayout.toolbar
    val homeActions = homeBinding.homeContent.absPlaylists.root
    val bannerImage = homeBinding.imageLayout.bannerImage
    val userImage = homeBinding.imageLayout.userImage
    val lastAdded = homeBinding.homeContent.absPlaylists.lastAdded
    val topPlayed = homeBinding.homeContent.absPlaylists.topPlayed
    val actionShuffle = homeBinding.homeContent.absPlaylists.actionShuffle
    val chooseFolder = homeBinding.homeContent.absPlaylists.chooseFolder
    val actionRadio = homeBinding.homeContent.absPlaylists.actionRadio
    val actionTv = homeBinding.homeContent.absPlaylists.actionTv
    val actionVideos = homeBinding.homeContent.absPlaylists.actionVideos
    val actionYoutube = homeBinding.homeContent.absPlaylists.actionYoutube
    val history = homeBinding.homeContent.absPlaylists.history
    val recyclerView = homeBinding.homeContent.recyclerView
    val titleWelcome = homeBinding.imageLayout.titleWelcome
    val suggestions = homeBinding.homeContent.suggestions
}
