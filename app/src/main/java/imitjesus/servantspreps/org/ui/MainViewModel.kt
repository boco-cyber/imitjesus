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
    data class Success(val quote: ImitationQuote) : QuoteState()
    data class Error(val message: String) : QuoteState()
}

class MainViewModel : ViewModel() {
    private val repository = QuoteRepository()
    private val _state = MutableLiveData<QuoteState>(QuoteState.Loading)
    val state: LiveData<QuoteState> = _state

    private var currentQuoteId: Int? = null

    fun loadTodayQuote() {
        _state.value = QuoteState.Loading
        viewModelScope.launch {
            repository.getTodayQuote().fold(
                onSuccess = { 
                    currentQuoteId = it.id
                    _state.value = QuoteState.Success(it) 
                },
                onFailure = { _state.value = QuoteState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun loadSpecificQuote(id: Int) {
        loadQuoteById(id)
    }

    fun loadNextQuote() {
        val nextId = (currentQuoteId ?: return) + 1
        loadQuoteById(nextId)
    }

    fun loadPreviousQuote() {
        val prevId = (currentQuoteId ?: return) - 1
        if (prevId < 1) return
        loadQuoteById(prevId)
    }

    private fun loadQuoteById(id: Int) {
        _state.value = QuoteState.Loading
        viewModelScope.launch {
            repository.getQuoteById(id).fold(
                onSuccess = {
                    currentQuoteId = it.id
                    _state.value = QuoteState.Success(it)
                },
                onFailure = { _state.value = QuoteState.Error(it.message ?: "Could not load quote $id") }
            )
        }
    }
}
