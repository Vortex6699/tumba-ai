# TUMBA AI

**Your AI. Your device. Your control.**

TUMBA AI — персональний AI-асистент для Android, який об'єднує хмарні нейромережі та локальні AI-моделі в одному мінімалістичному застосунку.

Використовуй Google Gemini, DeepSeek, OpenAI-compatible API або запускай GGUF-моделі безпосередньо на своєму пристрої.

**Приватність. Гнучкість. Контроль.**

---

## Основні можливості

- **AI Chat** — спілкування з AI, потокова генерація відповідей, Markdown, копіювання та повторна генерація.
- **Multiple Providers** — підтримка Gemini, DeepSeek та власних OpenAI-compatible API.
- **Local AI** — запуск GGUF-моделей безпосередньо на Android через llama.cpp.
- **Vision Support** — надсилання зображень моделям із підтримкою Vision.
- **Chat History** — історія розмов із пошуком, перейменуванням, закріпленням і видаленням.
- **Model Management** — вибір моделей, налаштування API, імпорт і завантаження GGUF.
- **AI Configuration** — системний промпт, температура, контекст розмови та потокова генерація.
- **Multilingual Interface** — українська та англійська мови.
- **Personalization** — світла, темна та системна теми.

## Приватність і безпека

TUMBA AI розробляється з пріоритетом на приватність та контроль користувача над власними даними.

- API-ключі шифруються за допомогою AES-256/GCM та Android Keystore.
- Історія розмов, налаштування та вкладення зберігаються локально.
- Відсутні реклама, аналітика та телеметрія.
- Локальні AI-моделі працюють без надсилання запитів у хмару.
- Хмарні запити надсилаються лише обраному провайдеру з використанням власного API-ключа.

**Your data. Your control.**

---

## AI-провайдери та моделі

### Хмарні провайдери

TUMBA AI підтримує:

- Google Gemini
- DeepSeek
- OpenAI-compatible API
- OpenRouter та сумісні API-шлюзи

Для підключення потрібен власний API-ключ відповідного провайдера.

### Локальні AI-моделі

Завдяки llama.cpp застосунок підтримує запуск моделей у форматі GGUF безпосередньо на пристрої.

Доступні моделі каталогу:

- Qwen 2.5 0.5B
- Qwen 2.5 1.5B
- Qwen 2.5 3B
- Llama 3.2 1B

Також можна імпортувати власні GGUF-файли.

Локальний рушій включений до APK для архітектур ARM64 та x86_64.

---

## Встановлення

1. Відкрий розділ [Releases](../../releases/latest).
2. Завантаж останню версію APK.
3. Відкрий файл на Android-пристрої.
4. Підтвердь встановлення, якщо система попросить дозвіл.

[**Download TUMBA AI 1.2.1**](../../releases/latest)

### Системні вимоги

| Параметр | Значення |
|---|---|
| Package | `com.tumba.ai` |
| Version | `1.2.1` |
| Version Code | `6` |
| Minimum Android | 8.0 (API 26) |
| Target SDK | 35 |
| Architecture | ARM64 / x86_64 |

---

## Технологічний стек

TUMBA AI побудований на сучасному Android-стеку:

- Kotlin 2.0.21
- Jetpack Compose
- Material 3
- MVVM Architecture
- Hilt
- Room Database
- DataStore
- OkHttp / Retrofit
- llama.cpp
- JNI / C++

---

## Для розробників

### Необхідне середовище

- Android Studio Ladybug або новіша.
- JDK 17 або 21.
- Android SDK 35.

### Запуск проєкту

1. Клонуй репозиторій.
2. Відкрий папку `tumba-ai` в Android Studio.
3. Дочекайся завершення Gradle Sync.
4. Запусти застосунок на Android-пристрої або емуляторі.

Для компіляції API-ключі не потрібні.

### Збірка APK

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

Результат:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Для Release:

```powershell
.\gradlew.bat assembleRelease
```

Release-збірки за замовчуванням не підписані. Для публікації потрібно налаштувати власний signing config або скористатися Android Studio → Generate Signed Bundle / APK.

### Native AI Engine

Нативний рушій llama.cpp вже зібраний для:

- `arm64-v8a`
- `x86_64`

Якщо бібліотеки немає після чистого клонування, її можна зібрати:

```powershell
powershell -ExecutionPolicy Bypass -File tools\build-native.ps1
```

Для цього може знадобитися Android NDK та CMake.

### Тестування

Запуск JVM-тестів:

```powershell
.\gradlew.bat testDebugUnitTest
```

Проєкт містить 35 JVM-тестів, які охоплюють парсер Markdown, потокову генерацію, провайдери, обробку помилок, каталог моделей та інші компоненти.

---

## Структура проєкту

```text
tumba-ai/
├── app/
│   ├── schemas/
│   └── src/main/
│       ├── java/com/tumba/ai/
│       │   ├── core/
│       │   ├── data/
│       │   ├── di/
│       │   ├── domain/
│       │   └── ui/
│       ├── cpp/
│       ├── jniLibs/
│       └── res/
├── tools/
├── dist/
├── gradle/
├── install.ps1
├── install.bat
├── install.sh
├── BUILD_REPORT.md
└── README.md
```

---

## Відомі обмеження

| Компонент | Статус |
|---|---|
| Local AI | Реалізований через llama.cpp та JNI |
| ARM64 | Підтримується |
| x86_64 | Підтримується |
| 32-bit ARM | Локальний рушій не включений |
| Cloud AI | Реалізація HTTP/SSE протестована на mock-сервері; реальні API-запити потребують окремої перевірки |
| Image Vision | Залежить від можливостей обраної моделі |
| Markdown | Підтримується обмежена реалізація |
| Release signing | Потребує власного підпису |

UI runtime, реальні API-запити та повне тестування на фізичних пристроях не були підтверджені в середовищі останньої збірки.

---

## Інформація про версію 1.2.1

- **Application:** TUMBA AI
- **Version:** 1.2.1
- **Version Code:** 6
- **Package:** `com.tumba.ai`
- **Minimum SDK:** 26
- **Target SDK:** 35

До складу перевіреного артефакту `dist/tumba-ai-1.2.1.apk` входить нативна бібліотека llama.cpp для ARM64 та x86_64.

APK містить рушій, але не містить ваг AI-моделей. Їх потрібно завантажити або імпортувати окремо.

Детальніше про збірку: `BUILD_REPORT.md`.

---

## Про TUMBA

TUMBA — незалежний проєкт, мета якого створювати корисні цифрові інструменти без зайвих обмежень.

Ми віримо, що хороші програми мають бути зручними, приватними та доступними.

**TUMBA — створюємо те, чого нам не вистачало.**

*Private. Powerful. Yours.*
