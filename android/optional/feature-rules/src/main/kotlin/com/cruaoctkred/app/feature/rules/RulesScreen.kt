package com.cruaoctkred.app.feature.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cruaoctkred.app.ui.components.ErrorBlock
import com.cruaoctkred.app.ui.components.LoadingBlock
import com.cruaoctkred.app.ui.components.OfflineBanner
import com.cruaoctkred.app.ui.components.Spacing

// Blank line in the admin textarea = paragraph break. Nothing else is interpreted:
// the editor offers a plain title field and a plain content field, no markup.
private val PARAGRAPH_BREAK = Regex("\\n\\s*\\n")

private fun paragraphsOf(content: String): List<String> =
    content.trim()
        .split(PARAGRAPH_BREAK)
        .map(String::trim)
        .filter(String::isNotEmpty)

@Composable
fun RulesScreen(state: RulesUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Text(stringResource(R.string.rules_title), modifier = Modifier.padding(16.dp),
            fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
        val bodyModifier = Modifier.weight(1f).fillMaxSize()
        when (state) {
            RulesUiState.Loading -> LoadingBlock(stringResource(R.string.loading), bodyModifier)

            RulesUiState.Error -> ErrorBlock(
                message = stringResource(R.string.generic_error),
                onRetry = onRetry,
                retryLabel = stringResource(R.string.retry),
                modifier = bodyModifier,
            )

            is RulesUiState.Content -> {
                val paragraphs = remember(state.page.content) { paragraphsOf(state.page.content) }

                Column(
                    bodyModifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.md)
                        .padding(bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    if (state.page.isOffline) {
                        OfflineBanner(stringResource(R.string.offline_banner))
                    }

                    if (paragraphs.isEmpty()) {
                        Text(
                            stringResource(R.string.rules_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            paragraphs.forEach { paragraph ->
                                Text(
                                    paragraph,
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
