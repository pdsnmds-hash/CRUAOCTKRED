package com.cruaoctkred.app.feature.offers.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.feature.offers.R
import com.cruaoctkred.app.ui.components.ErrorBlock
import com.cruaoctkred.app.ui.components.LoadingBlock
import com.cruaoctkred.app.ui.components.OfferCard
import com.cruaoctkred.app.ui.components.OfflineBanner
import com.cruaoctkred.app.ui.components.Spacing

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onDetail: (String) -> Unit,
    onSave: (String) -> Unit,
    onCompare: (String) -> Unit,
    onCta: (LoanCard) -> Unit,
    onStorefrontVisible: () -> Unit,
    modifier: Modifier = Modifier,
    calculatorContent: @Composable () -> Unit = {},
    cardSort: Comparator<LoanCard>? = null,
    showOfflineBanner: Boolean = true,
    showFeedHeader: Boolean = true,
    offerCard: @Composable (card: LoanCard, saved: Boolean, compared: Boolean) -> Unit = { card, saved, compared ->
        OfferCard(
            card = card,
            saved = saved,
            compared = compared,
            onOpenDetail = { onDetail(card.id) },
            onSave = { onSave(card.id) },
            onCompare = { onCompare(card.id) },
            onCta = { onCta(card) },
            showRepeatCredit = card.linkUrl == null,
        )
    },
) {
    LaunchedEffect(state.cards.map { it.id }) {
        if (state.cards.isNotEmpty()) {
            delay(1_000)
            onStorefrontVisible()
        }
    }
    when {
        state.loading -> LoadingBlock(stringResource(R.string.loading), modifier.fillMaxSize())
        state.cards.isEmpty() && state.error != null -> ErrorBlock(
            message = if (state.error == "firebase-config-missing") stringResource(R.string.configuration_missing) else stringResource(R.string.generic_error),
            onRetry = onRefresh,
            retryLabel = stringResource(R.string.retry),
            modifier = modifier,
        )
        state.cards.isEmpty() -> Column(modifier.fillMaxSize().padding(Spacing.xl), verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.empty_body), style = MaterialTheme.typography.bodyLarge)
        }
        else -> {
        val visible = cardSort?.let { state.cards.sortedWith(it) } ?: state.cards
        val listState = rememberLazyListState()
        val orderedIds = visible.map { it.id }
        var previousOrder by rememberSaveable { mutableStateOf(orderedIds) }
        LaunchedEffect(orderedIds) {
            if (previousOrder != orderedIds) {
                // Override LazyColumn's retained key/offset when the server reorders offers.
                // Keep ordinary content updates and navigation restoration at their old position.
                listState.requestScrollToItem(0)
                previousOrder = orderedIds
            }
        }
        LazyColumn(
            modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (showOfflineBanner && state.offline) item { OfflineBanner(stringResource(R.string.offline_banner)) }
            if (showFeedHeader) {
                item { calculatorContent() }
                item {
                    Text(
                        stringResource(R.string.offers_count, visible.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (visible.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.empty_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(visible, key = { it.id }) { card ->
                offerCard(card, card.id in state.savedIds, card.id in state.comparedIds)
            }
        }
        }
    }
}
