package imitjesus.servantspreps.org.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import imitjesus.servantspreps.org.data.model.ImitationQuote
import imitjesus.servantspreps.org.data.repository.QuoteRepository
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed class QuoteState {
    object Loading : QuoteState()
    data class Success(val quotes: List<ImitationQuote>, val initialPosition: Int) : QuoteState()
    data class Error(val message: String) : QuoteState()
}

class MainViewModel : ViewModel() {
    private val repository = QuoteRepository()
    private val _state = MutableLiveData<QuoteState>(QuoteState.Loading)
    val state: LiveData<QuoteState> = _state

    private var allQuotes: List<ImitationQuote>? = null

    fun loadQuotes(targetId: Int? = null) {
        _state.value = QuoteState.Loading
        viewModelScope.launch {
            repository.getAllQuotes().fold(
                onSuccess = { quotes ->
                    allQuotes = quotes
                    val position = if (targetId != null) {
                        quotes.indexOfFirst { it.id == targetId }.coerceAtLeast(0)
                    } else {
                        // Find today's quote index
                        repository.getTodayQuote().fold(
                            onSuccess = { today ->
                                quotes.indexOfFirst { it.id == today.id }.coerceAtLeast(0)
                            },
                            onFailure = { 0 }
                        )
                    }
                    _state.value = QuoteState.Success(quotes, position)
                },
                onFailure = { _state.value = QuoteState.Error(it.message ?: "Unknown error") }
            )
        }
    }
}
