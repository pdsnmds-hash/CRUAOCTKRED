# ui-header-fintech-2

Светлый Soft Finance header: вертикальный градиент, заголовок с уведомлениями,
поиск сразу под текстом и прокручиваемые фильтры с подписями 12 sp SemiBold.
Тексты и состояние принадлежат продукту. Цвет фона передаётся через `gradient`,
акцент — через `accent`; нейтральные цвета текста и белой поверхности локальны модулю.
Собственных ресурсов и обращения к MaterialTheme нет.

## Wiring — сверх контракта AppHeader

Все параметры ниже опциональны. Смена импорта сама по себе не подключает функции.

| Параметр | Умолчание | Что передать продукту |
|---|---|---|
| amountSlider | null | HeaderSlider с текущей суммой, диапазоном, строками и onValueChange |
| termSlider | null | HeaderSlider со сроком, диапазоном, строками и onValueChange |
| buttonText | null | Готовая подпись CTA; без неё кнопки нет |
| onShowOffers | {} | Обработчик CTA; без него видимая кнопка ничего не делает |
| accent | #15945B | Брендовый акцент; это визуальный параметр, не отключённая функция |
| scrollState | null | rememberHeaderScrollState(); его nestedScrollConnection прикрепить к скроллирующему родителю через Modifier.nestedScroll |
| search | null | HeaderSearch: query, placeholder, onQueryChange. Продукт обновляет query и фильтрует данные; settingsDescription + onSettingsClick включают кнопку настроек |
| filters | null | HeaderFilters: items, selectedId, onSelect. Продукт обновляет selectedId и применяет фильтр к данным |
| notification | null | HeaderNotification: описание для доступности и обработчик открытия |

Сначала сворачиваются поиск, фильтры и слайдеры, затем заголовок.
`bottomContent` остаётся закреплённым. Разворачивание — когда дочерний список достиг верха.
HeaderSlider, HeaderFeedActions и rememberDailyNumber сохранены для совместимости.
HeaderFeedActions получает свои строки, цвета и обработчики явно; они не подключаются
автоматически при вызове AppHeader.

## Ширины

- 320 dp: боковые отступы 16 dp, заголовок 23 sp, фильтры прокручиваются.
- 360 dp: боковые отступы 20 dp, заголовок 25 sp.
- 411 dp: боковые отступы 20 dp, заголовок 25 sp, больше места поиску.

Подписи фильтров переносятся; опциональная кнопка калькулятора занимает ширину хедера.
При ограниченном titleMaxLines заголовок обрезается многоточием по контракту слота.
Снимки делает общий :catalog-previews; локальных previews/, androidTest и Roborazzi нет.
