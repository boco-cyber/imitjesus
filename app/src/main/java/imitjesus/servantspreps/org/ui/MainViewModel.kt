package imitjesus.servantspreps.org.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import imitjesus.servantspreps.org.data.model.DailyEntry
import imitjesus.servantspreps.org.data.repository.QuoteRepository
import kotlinx.coroutines.launch

sealed class QuoteState {
    object Loading : QuoteState()
    data class Success(val entry: DailyEntry) : QuoteState()
    data class Error(val message: String) : QuoteState()
}

class MainViewModel : ViewModel() {
    private val repository = QuoteRepository()
    private val _state = MutableLiveData<QuoteState>(QuoteState.Loading)
    val state: LiveData<QuoteState> = _state

    init {
        loadTodayQuote()
    }

    fun loadTodayQuote() {
        _state.value = QuoteState.Loading
        viewModelScope.launch {
            repository.getTodayQuote().fold(
                onSuccess = { _state.value = QuoteState.Success(it) },
                onFailure = { _state.value = QuoteState.Error(it.message ?: "Unknown error") }
            )
        }
    }
}
