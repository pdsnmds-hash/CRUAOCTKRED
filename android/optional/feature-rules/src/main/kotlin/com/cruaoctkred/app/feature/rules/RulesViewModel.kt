package com.cruaoctkred.app.feature.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.cruaoctkred.app.domain.model.RulesPage
import com.cruaoctkred.app.domain.model.StreamResult
import com.cruaoctkred.app.domain.repository.PagesRepository

sealed interface RulesUiState {
    data object Loading : RulesUiState
    data class Content(val page: RulesPage) : RulesUiState
    data object Error : RulesUiState
}

class RulesViewModel(pagesRepository: PagesRepository) : ViewModel() {
    val state: StateFlow<RulesUiState> = pagesRepository.observeRules()
        .map { result ->
            when (result) {
                StreamResult.Loading -> RulesUiState.Loading
                is StreamResult.Data -> RulesUiState.Content(result.value)
                is StreamResult.Error -> result.cached?.let(RulesUiState::Content) ?: RulesUiState.Error
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState.Loading)

    class Factory(private val pagesRepository: PagesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RulesViewModel(pagesRepository) as T
    }
}
