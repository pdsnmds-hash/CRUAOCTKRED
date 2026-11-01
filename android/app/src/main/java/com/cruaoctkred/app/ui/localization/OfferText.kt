package com.cruaoctkred.app.ui.localization

/** Translate only structured offer units; lender names and legal/contact text stay intact. */
internal fun localizeOfferText(value: String, language: String): String {
    val uk = language == "uk"
    val replacements = listOf(
        "день|дня|дней|дні|днів|days?|дн\\.?" to if (uk) "днів" else "дней",
        "год|года|лет|рік|роки|років|years?" to if (uk) "років" else "лет",
        "минут|минута|минуты|хвилина|хвилини|хвилин|minutes?|mins?|мин\\.?|хв\\.?" to if (uk) "хв" else "мин",
        "час|часа|часов|година|години|годин|hours?" to if (uk) "год" else "ч",
        "месяц|месяца|месяцев|місяць|місяці|місяців|months?" to if (uk) "місяців" else "месяцев",
        "от|від|from" to if (uk) "від" else "от",
        "до|to" to "до",
    )
    val currency = localizeCurrency(value)
    return replacements.fold(currency) { text, (words, replacement) ->
        text.replace(Regex("(?<![\\p{L}])(?:$words)(?![\\p{L}])", RegexOption.IGNORE_CASE)) { match ->
            val count = Regex("(\\d+)\\s*$").find(text.substring(0, match.range.first))
                ?.groupValues?.get(1)?.toLongOrNull()
            val forms = when (replacement) {
                "днів" -> listOf("день", "дні", "днів")
                "дней" -> listOf("день", "дня", "дней")
                "років" -> listOf("рік", "роки", "років")
                "лет" -> listOf("год", "года", "лет")
                "місяців" -> listOf("місяць", "місяці", "місяців")
                "месяцев" -> listOf("месяц", "месяца", "месяцев")
                else -> null
            }
            if (count == null || forms == null) replacement else forms[when {
                count % 100 in 11L..14L -> 2
                count % 10 == 1L -> 0
                count % 10 in 2L..4L -> 1
                else -> 2
            }]
        }
    }
}

internal fun localizeCurrency(value: String): String = value.replace(
    Regex("(?<![\\p{L}])(?:PHP|UAH|₱|₴)(?![\\p{L}])", RegexOption.IGNORE_CASE), "грн")
