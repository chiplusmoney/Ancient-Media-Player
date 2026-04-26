package player.music.ancient.fragments.tv

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import player.music.ancient.db.TvCategoryEntity
import player.music.ancient.db.TvChannelEntity
import player.music.ancient.repository.Repository

class TvViewModel(private val repository: Repository) : ViewModel() {
    val tvChannels: LiveData<List<TvChannelEntity>> = repository.getAllTvChannels()
    val tvCategories: LiveData<List<TvCategoryEntity>> = repository.getAllTvCategories()

    fun insertTvChannel(channel: TvChannelEntity) {
        viewModelScope.launch(IO) {
            repository.insertTvChannel(channel)
        }
    }

    fun updateTvChannel(channel: TvChannelEntity) {
        viewModelScope.launch(IO) {
            repository.updateTvChannel(channel)
        }
    }

    fun deleteTvChannel(channel: TvChannelEntity) {
        viewModelScope.launch(IO) {
            repository.deleteTvChannel(channel)
        }
    }

    fun insertTvCategory(category: TvCategoryEntity) {
        viewModelScope.launch(IO) {
            repository.insertTvCategory(category)
        }
    }

    fun updateTvCategory(category: TvCategoryEntity) {
        viewModelScope.launch(IO) {
            repository.updateTvCategory(category)
        }
    }

    fun deleteTvCategory(category: TvCategoryEntity) {
        viewModelScope.launch(IO) {
            repository.clearCategoryFromTvChannels(category.id)
            repository.deleteTvCategory(category)
        }
    }
}
