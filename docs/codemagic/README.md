# Кредити на картку: подпись и AAB на Codemagic

Адаптировано из прочитанных файлов NEW LOOK `docs/codemagic/` и исправленного корневого YAML Safora. Это подготовка файлов; реальный облачный запуск этого приложения ещё не выполнялся.

## Параметры приложения

- Application ID: `com.cruaoctkred.app`; исходники Android в `android/`, модуль `app`.
- Java 17, Gradle 9.6.1, AGP 9.4.1; compileSdk 37, targetSdk 36, minSdk 24.
- versionCode 1, versionName 1.0.0; дизайн `blue-ua-3`. Значения не менялись для CI.
- Четыре ABI: armeabi-v7a, arm64-v8a, x86, x86_64.
- R8 и shrinkResources включены; mapping.txt сохраняется именно от данного билда.
- Gradle читает `android/signing.properties`; `storeFile` разрешается относительно `android/`.
- В CI bridge создаёт `android/signing/upload-keystore.jks`, `storeFile=signing/upload-keystore.jks`.
- Пользователь подтвердил: существующий локальный upload key в Play ещё не зарегистрирован и не использовался. Локальные JKS, сертификат, пароли и конфигурация сохранены без изменений.

## 1. Git

В момент подготовки папка не является Git-репозиторием, remote отсутствует. Codemagic забирает код из Git, а не с Mac. Создайте приватный репозиторий у своего Git-провайдера. Пример команд из корня `my-crua/`:

```sh
git init
git branch -M main
git add .gitignore codemagic.yaml docs/codemagic \
  android/build.gradle.kts android/settings.gradle.kts android/gradle.properties \
  android/gradlew android/gradlew.bat android/gradle \
  android/app/build.gradle.kts android/app/proguard-rules.pro android/app/src \
  android/core android/modules android/optional
git diff --cached --name-only
git diff --cached --stat
git commit -m "Configure Codemagic signing workflows"
git remote add origin YOUR_PRIVATE_REPOSITORY_URL
git push -u origin main
```

Подставьте свой URL. Не используйте `git add -f` для ключей, JSON Firebase, local.properties, serviceAccountKey.json, APK/AAB, build, artifacts или приватных архивов. Ignore-правила не удаляют ранее отслеживаемые файлы. Здесь staged files проверить нельзя до создания репозитория; временный индекс проверяет только правила исключения и список кандидатов.

Стандартная debug-подпись Android используется автоматически: приватного debug key в конфигурации нет. Не добавляйте его в Git ради тестов.

## 2. Одноразово создать ключ НА Codemagic

Для этапа генерации используется `my_crua-create-upload-key-once` из `docs/codemagic/generate-upload-key.yaml`. В текущей рабочей копии корневой `codemagic.yaml` уже переключён на обычную AAB-сборку; повторно генерировать ключ не нужно.

1. Подключите Git-репозиторий через Codemagic Applications → Add application.
2. В Environment variables создайте группу **`my_crua_key_generation`**.
3. Добавьте **`NEW_KEYSTORE_PASSWORD`** и **`NEW_KEY_PASSWORD`**, оба с включённым Secret. Сохраните пароли в менеджере паролей; не отправляйте в чат и не записывайте в YAML.
4. Subject сертификата — `CN=Кредити на картку`, без вымышленного владельца, организации или страны. При необходимости замените на фактические данные владельца до запуска.
5. На нужной ветке вручную запустите **Кредити на картку - ONE-TIME create upload key**.
6. Скачайте `generated-signing/my-crua-upload.jks` и `generated-signing/upload_certificate.pem` и сохраните резервную копию.

Генератор: Linux X2, Java 17; JKS, RSA 4096, SHA256withRSA, 10000 дней, alias `upload`. Генерация keystore одновременно создаёт upload key. Она не выполнялась на Mac; локальные ключи не заменены.

Запускайте генератор один раз: повторный запуск на чистой машине создаст ДРУГОЙ ключ даже при прежнем alias и паролях. Если какой-либо upload key уже будет зарегистрирован в Play, сохраняйте его — замена потребует процедуры reset upload key.

## 3. Загрузить новый JKS в identities

Team settings → codemagic.yaml settings → Code signing identities → Android keystores:

| Поле | Значение |
|---|---|
| File | скачанный `my-crua-upload.jks` |
| Keystore password | сохранённое значение `NEW_KEYSTORE_PASSWORD` |
| Key alias | `upload` |
| Key password | сохранённое значение `NEW_KEY_PASSWORD` |
| Reference name | **`my_crua_upload`** |

Alias находится внутри JKS; Reference name используется в YAML. Это разные значения. Не загружайте старый локальный ключ вместо облачного без осознанного выбора.

## 4. Runtime secrets

Создайте группу **`my_crua_runtime`**, доступную обычной AAB-сборке. Все три значения пометьте Secret:

| Имя | Содержимое |
|---|---|
| `GOOGLE_SERVICES_JSON_BASE64` | Base64 содержимого собственного `android/app/google-services.json` |
| `APPSFLYER_DEV_KEY` | действующий Dev Key этого приложения |
| `BINOM_ANALYTICS_URL` | полный действующий HTTPS URL Binom с шаблоном t1–t10 |

На Mac скопировать Firebase JSON в буфер, не выводя содержимое в терминал:

```sh
base64 < android/app/google-services.json | tr -d '\n' | pbcopy
```

Затем вставьте в Secret variable. Значения аналитики возьмите из своего локального `android/local.properties` и перенесите через UI без публикации файла. Дизайн-версия `blue-ua-3` задаётся обычной переменной YAML.

`prepare_runtime.py` проверяет пакет Firebase, создаёт `android/app/google-services.json` и дополняет сгенерированный `android/local.properties`. Права — 600. Без обязательных secrets сборка завершается ошибкой вместо незаметного перехода в demo/offline режим. Service account и Firebase CLI login для сборки не нужны; не добавляйте их в репозиторий или артефакты.

## 5. Переключить корень на обычную AAB-сборку

После сохранения JKS и загрузки identity:

```sh
cp docs/codemagic/codemagic.yaml codemagic.yaml
git add codemagic.yaml docs/codemagic .gitignore
git commit -m "Enable signed Codemagic AAB builds"
git push
```

Запустите workflow **`my-crua-aab`**, имя **Кредити на картку - Signed AAB**. Он использует identity `my_crua_upload` и группу `my_crua_runtime`. Переменные `CM_KEYSTORE_PATH`, `CM_KEYSTORE_PASSWORD`, `CM_KEY_ALIAS`, `CM_KEY_PASSWORD` предоставляет Codemagic; вручную дублировать их не нужно.

Шаги:

1. Выбрать executable sdkmanager по максимальной Pkg.Revision, а не имени latest и не PATH.
2. Установить `platforms;android-37.0` и `build-tools;36.0.0`. Точные ID сверены с официальным repository2-3.xml. compileSdk остаётся 37.
3. Восстановить Firebase/аналитику из runtime secrets.
4. Соединить signing identity с фактическим Gradle через `android/signing.properties`. Защита от перезаписи локальных файлов, Java Properties escaping, права 600.
5. Из `android/` выполнить:

```sh
./gradlew --no-daemon -PbuildBundle=true :app:testDebugUnitTest :app:lintDebug :app:bundleRelease
```

`-PbuildBundle=true` обязателен: он отключает APK-сплиты для AAB, сохраняя все четыре ABI внутри bundle. Иначе AGP с R8 не может выбрать один архив shrunk resources. APK собирается отдельно, эта конфигурация выдаёт AAB.

6. Verifier проверяет JAR-подпись, совпадение сертификата с identity, четыре ABI и отсутствие приватных signing-файлов в AAB.

Обычные артефакты:

- `android/app/build/outputs/bundle/release/app-release.aab`;
- `android/app/build/outputs/mapping/release/mapping.txt`;
- `android/app/build/reports/lint-results-debug.html`;
- `android/app/build/reports/tests/testDebugUnitTest/**`.

Обычный workflow не возвращает JKS и не публикует приложение в Google Play. Стадия Publishing может означать только выдачу артефактов. Новый app signing key можно поручить Google Play App Signing; upload key остаётся отдельным.

Не включайте `set -x`, `printenv` или `cat` приватных properties. Перед последующими загрузками увеличивайте versionCode только если текущее значение уже использовано в Play.

## Проверки и границы результата

`python3 docs/codemagic/validate_local.py` проверяет YAML/shell, helpers на временных данных и подпись временного архива. Для YAML используется Ruby Psych (локально), Java 17 — для keytool, jarsigner и Properties round-trip. Тестовые ключи удаляются. Эти проверки не являются cloud build и не подтверждают успешную сборку приложения в Codemagic.

Результаты helpers — `local-validation.json`. Отдельно `clean-checkout-validation.json` подтверждает успешный `:app:signingReport` в чистой временной копии Android без приватных файлов проекта: выбрана стандартная debug-подпись. Полная сборка приложения в этой копии не запускалась.

Первый облачный AAB build выполнен пользователем и остановился на проверке runtime values (01.11.2026). Пересборка после исправления здесь не запускалась. Актуальная граница проверки — в `cloud-status.json`; нужны фактический успешный build status, commit и артефакты Codemagic.

Официальные источники:
- https://docs.codemagic.io/yaml-code-signing/signing-android/
- https://docs.codemagic.io/yaml-quick-start/building-a-native-android-app/
- https://developer.android.com/studio/publish/app-signing
- https://dl.google.com/android/repository/repository2-3.xml


## Ошибка runtime values (01.11.2026)

Первый облачный AAB build, показанный пользователем, остановился на `prepare_runtime.py`: в одном из значений аналитики или дизайн-версии оказалась кавычка, обратный слеш либо управляющий символ. Локальный скрипт теперь удаляет только пробельные символы по краям и называет проблемную переменную без вывода значения. Внутренние переносы и кавычки по-прежнему отклоняются.

В группе `my_crua_runtime` укажите URL Binom целиком в одну строку, без кавычек, обратных слешей перед `&` и префикса `BINOM_ANALYTICS_URL=`. Дизайн-версия в YAML — `blue-ua-3`; лишнее переопределение в UI не требуется. Отправьте исправленные Python scripts в Git перед новой сборкой. Генератор ключа повторно запускать не нужно.

Текущее состояние облака фиксируется в `cloud-status.json`; после исправления облачная пересборка здесь не запускалась. Корневой YAML уже переключён пользователем на обычную AAB-сборку, а не генератор.
