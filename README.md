# AI Content Summarizer Bot

Telegram-бот, который выжимает самое главное из текста. Достаточно отправить ссылку.

## Функциональность

*   **Извлечение контента**: Умное распознавание основного текста статьи (без рекламы и навигации) с помощью Readability4J.
*   **Суммаризация**: Генерация Ключевых тезисов
*   **Мультиязычность**: Понимает статьи на любом языке, результат всегда выдает на русском.
*   **Удобство**: Форматирование в Markdown, индикация процесса (typing...), поддержка ссылок.

## Требования

*   Java 17+
*   Telegram Bot Token
*   OpenAI API Key

## Настройка

Перед запуском необходимо установить переменные окружения:

```bash
export TELEGRAM_BOT_TOKEN="ваш_токен"
export OPENAI_API_KEY="ваш_ключ"
# Опционально
export USER_AGENT="Custom User Agent"
```

## Запуск

### Через Gradle

```bash
./gradlew run
```

### Сборка и запуск JAR

1.  Собрать fat JAR:
    ```bash
    ./gradlew shadowJar
    ```
2.  Запустить:
    ```bash
    java -jar build/libs/ai-content-summarizer-all.jar
    ```

## Разработка

Проект использует Clean Architecture:
*   `domain` - Бизнес-логика и модели.
*   `infrastructure` - Реализация сервисов (OpenAI, Jsoup).
*   `presentation` - Telegram бот и форматирование.
*   `config` - Загрузка конфигурации.
*   `di` - Koin модули.

### Тестирование

Запуск всех тестов:
```bash
./gradlew test
```
