package com.cruaoctkred.app.feature.offers.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.domain.model.StreamResult
import com.cruaoctkred.app.domain.repository.CardsRepository

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Content(val card: LoanCard, val offline: Boolean = false) : DetailUiState
    data class Error(val cached: LoanCard? = null) : DetailUiState
}

class DetailViewModel(cardsRepository: CardsRepository, cardId: String) : ViewModel() {
    val state: StateFlow<DetailUiState> = cardsRepository.observeCard(cardId)
        .map { result ->
            when (result) {
                StreamResult.Loading -> DetailUiState.Loading
                is StreamResult.Data -> DetailUiState.Content(result.value)
                is StreamResult.Error -> DetailUiState.Error(result.cached)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    class Factory(private val cardsRepository: CardsRepository, private val cardId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DetailViewModel(cardsRepository, cardId) as T
    }
}
