package player.music.ancient.fragments.radio

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import player.music.ancient.db.RadioCategoryEntity
import player.music.ancient.db.RadioStationEntity
import player.music.ancient.repository.Repository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class RadioViewModel(private val repository: Repository) : ViewModel() {

    val radioStations: LiveData<List<RadioStationEntity>> = repository.getAllRadioStations()
    val radioCategories: LiveData<List<RadioCategoryEntity>> = repository.getAllRadioCategories()

    fun insertRadioStation(name: String, uri: String, imageUri: String) {
        viewModelScope.launch(IO) {
            repository.insertRadioStation(RadioStationEntity(name = name, uri = uri, imageUri = imageUri))
        }
    }

    fun insertRadioStation(radioStation: RadioStationEntity) {
        viewModelScope.launch(IO) {
            repository.insertRadioStation(radioStation)
        }
    }

    fun deleteRadioStation(radioStation: RadioStationEntity) {
        viewModelScope.launch(IO) {
            repository.deleteRadioStation(radioStation)
        }
    }

    fun updateRadioStation(radioStation: RadioStationEntity) {
        viewModelScope.launch(IO) {
            repository.updateRadioStation(radioStation)
        }
    }

    fun insertRadioCategory(radioCategory: RadioCategoryEntity) {
        viewModelScope.launch(IO) {
            repository.insertRadioCategory(radioCategory)
        }
    }

    fun updateRadioCategory(radioCategory: RadioCategoryEntity) {
        viewModelScope.launch(IO) {
            repository.updateRadioCategory(radioCategory)
        }
    }

    fun deleteRadioCategory(radioCategory: RadioCategoryEntity) {
        viewModelScope.launch(IO) {
            repository.clearCategoryFromStations(radioCategory.id)
            repository.deleteRadioCategory(radioCategory)
        }
    }
}
