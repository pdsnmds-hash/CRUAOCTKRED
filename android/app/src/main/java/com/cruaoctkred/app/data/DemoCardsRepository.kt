package com.cruaoctkred.app.data

import android.content.Context
import androidx.annotation.DrawableRes
import com.cruaoctkred.app.R
import com.cruaoctkred.app.config.ProductConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.NumberFormat
import java.util.Locale
import java.util.Currency
import com.cruaoctkred.app.domain.model.CardsContent
import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.domain.model.StreamResult
import com.cruaoctkred.app.domain.model.ValueRange
import com.cruaoctkred.app.domain.repository.CardsRepository

/** Local test offers while the product's Firebase cards are unavailable. */
class DemoCardsRepository(private val context: Context) : CardsRepository {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(ProductConfig.LOCALE)).apply {
        currency = Currency.getInstance(ProductConfig.CURRENCY)
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }

    private fun logoUri(@DrawableRes drawableId: Int): String =
        "android.resource://${context.packageName}/$drawableId"

    private val cards = listOf(
        demoCard("demo-moneycat", "MoneyCat", R.drawable.demo_moneycat_logo, 0, 4.8, 18_000, 24_000, "0.1%", 5),
        demoCard("demo-kviku", "Kviku", R.drawable.demo_kviku_logo, 1, 4.9, 32_000, 40_000, "0.3%", 6),
    )

    private fun demoCard(
        id: String,
        title: String,
        @DrawableRes logo: Int,
        order: Int,
        rating: Double,
        firstAmount: Long,
        repeatAmount: Long,
        rate: String,
        decisionMinutes: Int,
    ) = LoanCard(
        id = id,
        title = title,
        rating = rating,
        firstCredit = currencyFormatter.format(firstAmount),
        repeatCredit = currencyFormatter.format(repeatAmount),
        rate = rate,
        age = "18–70",
        term = "65–365",
        decisionTime = "$decisionMinutes min",
        website = "",
        phone = "",
        email = "",
        address = "",
        legalEntity = "",
        license = "",
        apr = "",
        aprCalculation = "",
        logoUrl = logoUri(logo),
        linkUrl = null,
        order = order,
        firstCreditRange = ValueRange(null, firstAmount),
        repeatCreditRange = ValueRange(null, repeatAmount),
        termRange = ValueRange(65, 365),
        ageRange = ValueRange(18, 70),
    )

    override fun observePublishedCards(): Flow<StreamResult<CardsContent>> =
        flowOf(StreamResult.Data(CardsContent(cards)))

    override fun observeCard(id: String): Flow<StreamResult<LoanCard>> {
        val result: StreamResult<LoanCard> = cards.firstOrNull { it.id == id }
            ?.let { StreamResult.Data(it) }
            ?: StreamResult.Error(NoSuchElementException("card-not-found"))
        return flowOf(result)
    }

    override suspend fun refresh() = Unit
}
