package com.cruaoctkred.app.data.firebase.cards

import com.cruaoctkred.app.domain.model.LoanCard
import com.cruaoctkred.app.domain.model.ValueRange

class CardMapper(private val locale: String, private val fallbackLocale: String, private val currentLocale: (() -> String)? = null) {
    fun map(id: String, raw: Map<String, Any?>): LoanCard {
        val translations = raw["translations"] as? Map<*, *>
        val translation = (translations?.get(currentLocale?.invoke() ?: locale) as? Map<*, *>)
            ?: (translations?.get(fallbackLocale) as? Map<*, *>)

        fun text(key: String): String = (translation?.get(key) as? String)
            ?.takeIf { it.isNotBlank() }
            ?: (raw[key] as? String).orEmpty()
        fun url(key: String): String? = (raw[key] as? String)?.trim()?.takeIf { it.startsWith("https://") }
        fun range(prefix: String) = ValueRange(
            from = (raw[prefix + "From"] as? Number)?.toLong()?.takeIf { it > 0 },
            to = (raw[prefix + "To"] as? Number)?.toLong()?.takeIf { it > 0 },
        )
        fun rate(): String = text("rate").replace(Regex("\\s*/\\s*(ngày|tháng|day|month).*$", RegexOption.IGNORE_CASE), "").trim()

        return LoanCard(
            id = id,
            title = text("title"),
            rating = (raw["rating"] as? Number)?.toDouble()?.coerceIn(0.0, 5.0) ?: 0.0,
            firstCredit = text("firstCredit"),
            repeatCredit = text("repeatCredit"),
            rate = rate(),
            age = text("age"),
            term = text("term"),
            decisionTime = text("decisionTime"),
            website = text("website"),
            phone = text("phone"),
            email = text("email"),
            address = text("address"),
            legalEntity = text("legalEntity"),
            license = text("license"),
            apr = text("apr"),
            aprCalculation = text("aprCalculation"),
            logoUrl = url("logoUrl"),
            linkUrl = url("linkUrl"),
            order = (raw["order"] as? Number)?.toInt()?.coerceIn(0, 9999) ?: 0,
            isHot = raw["isHot"] == true,
            isNew = raw["isNew"] == true,
            firstCreditRange = range("firstCredit"),
            repeatCreditRange = range("repeatCredit"),
            termRange = range("term"),
            ageRange = range("age"),
        )
    }
}
