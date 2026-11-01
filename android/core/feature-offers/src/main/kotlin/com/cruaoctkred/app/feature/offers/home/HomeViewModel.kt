package com.cruaoctkred.app.feature.offers.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.cruaoctkred.app.analytics.BinomAnalytics
import com.cruaoctkred.app.analytics.CtaCoordinator
import com.cruaoctkred.app.analytics.CtaResult
import com.cruaoctkred.app.domain.model.CardsContent
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.domain.model.StreamResult
import com.cruaoctkred.app.domain.repository.AttributionRepository
import com.cruaoctkred.app.domain.repository.CardsRepository
import com.cruaoctkred.app.domain.repository.PushTokenRepository
import com.cruaoctkred.app.domain.repository.SelectionRepository

data class HomeUiState(
    val loading: Boolean = true,
    val cards: List<LoanCard> = emptyList(),
    val savedIds: Set<String> = emptySet(),
    val comparedIds: List<String> = emptyList(),
    val offline: Boolean = false,
    val error: String? = null,
)

sealed interface HomeEffect {
    data class OpenWeb(val url: String, val cardId: String) : HomeEffect
    data class OpenDetail(val cardId: String) : HomeEffect
    data class Message(val kind: Kind) : HomeEffect {
        enum class Kind { Offline, InvalidLink, CompareLimit }
    }
    data object RequestPushPermission : HomeEffect
}

class HomeViewModel(
    private val cardsRepository: CardsRepository,
    private val selectionRepository: SelectionRepository,
    private val ctaCoordinator: CtaCoordinator,
    private val binom: BinomAnalytics,
    private val attributionRepository: AttributionRepository,
    private val pushTokenRepository: PushTokenRepository,
) : ViewModel() {
    private val refreshGeneration = MutableStateFlow(0)
    private val cardResults = refreshGeneration.flatMapLatest { cardsRepository.observePublishedCards() }
        .scan<StreamResult<CardsContent>, StreamResult<CardsContent>>(StreamResult.Loading, ::retainVisibleCards)
    private val effectsChannel = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()
    private var pushRequested = false
    val state: StateFlow<HomeUiState> = combine(
        cardResults,
        selectionRepository.savedIds,
        selectionRepository.comparedIds,
    ) { result, saved, compared ->
        when (result) {
            StreamResult.Loading -> HomeUiState(loading = true, savedIds = saved, comparedIds = compared)
            is StreamResult.Data -> result.value.toState(saved, compared)
            is StreamResult.Error -> result.cached?.toState(saved, compared, error = result.cause.message)
                ?: HomeUiState(loading = false, savedIds = saved, comparedIds = compared, error = result.cause.message)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            state.collect { value ->
                if (!pushRequested && value.cards.isNotEmpty()) {
                    pushRequested = true
                    effectsChannel.send(HomeEffect.RequestPushPermission)
                }
            }
        }
    }

    fun refresh() { refreshGeneration.value += 1 }

    fun toggleSaved(cardId: String) = viewModelScope.launch {
        selectionRepository.toggleSaved(cardId)
    }

    fun toggleCompared(cardId: String) = viewModelScope.launch {
        if (!selectionRepository.toggleCompared(cardId)) {
            effectsChannel.send(HomeEffect.Message(HomeEffect.Message.Kind.CompareLimit))
        }
    }

    val routing = ctaCoordinator.routing
    fun allowsWebView(card: LoanCard): Boolean = ctaCoordinator.allows(card)
    fun openDetail(cardId: String) {
        state.value.cards.firstOrNull { it.id == cardId }?.let { openOffer(it, "card") }
    }

    fun openOffer(card: LoanCard, placement: String) = viewModelScope.launch {
        when (val result = ctaCoordinator.open(card, placement)) {
            is CtaResult.Open -> effectsChannel.send(HomeEffect.OpenWeb(result.url, card.id))
            CtaResult.NativeDetails -> effectsChannel.send(HomeEffect.OpenDetail(card.id))
        }
    }

    fun storefrontVisible() = viewModelScope.launch {
        binom.home(attributionRepository.read())
    }

    fun registerPushToken() = viewModelScope.launch { pushTokenRepository.registerCurrentToken() }

    private fun CardsContent.toState(saved: Set<String>, compared: List<String>, error: String? = null) = HomeUiState(
        loading = false,
        cards = cards,
        savedIds = saved,
        comparedIds = compared,
        offline = isOffline || error != null,
        error = error,
    )

    class Factory(
        private val cardsRepository: CardsRepository,
        private val selectionRepository: SelectionRepository,
        private val ctaCoordinator: CtaCoordinator,
        private val binom: BinomAnalytics,
        private val attributionRepository: AttributionRepository,
        private val pushTokenRepository: PushTokenRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(
            cardsRepository, selectionRepository, ctaCoordinator, binom, attributionRepository, pushTokenRepository,
        ) as T
    }
}

/** A locale refresh must not replace visible offers with an empty loading state. */
internal fun retainVisibleCards(previous: StreamResult<CardsContent>, next: StreamResult<CardsContent>): StreamResult<CardsContent> {
    val cached = when (previous) {
        is StreamResult.Data -> previous.value
        is StreamResult.Error -> previous.cached
        StreamResult.Loading -> null
    }
    return when (next) {
        StreamResult.Loading -> cached?.let { StreamResult.Data(it) } ?: next
        is StreamResult.Error -> StreamResult.Error(next.cause, next.cached ?: cached)
        is StreamResult.Data -> next
    }
}
