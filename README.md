# Korotko

![Build Status](https://github.com/bkzntsv/korotko/actions/workflows/ci.yml/badge.svg)

**Korotko** is an intelligent Telegram bot that transforms lengthy web articles into structured, executive summaries. It's filtering noise and highlighting key insights.

## ✨ Key Features

*   **Executive Summaries**: Delivers the main idea and key takeaways, answering "Why does this matter?"
*   **Smart Analysis**: Evaluates article sentiment and detects clickbait (0-10 score).
*   **Clean Extraction**: Uses `Readability4J` to strip ads, navigation, and paywalls, focusing only on the content.
*   **Multilingual Input**: Summarizes articles in any language into Russian.
*   **Privacy-First**: No data storage, processes links on-the-fly.

## 🛠️ Tech Stack

Built with **Kotlin** and **Clean Architecture** principles.

*   **Core**: Kotlin 1.9, Coroutines
*   **Network**: Ktor Client (CIO engine)
*   **Parsing**: Jsoup, Readability4J
*   **AI**: OpenAI API (JSON Mode)
*   **Bot**: Kotlin Telegram Bot API
*   **DI**: Koin
*   **Testing**: Kotest (Property-based testing), MockK

## 🚀 Getting Started

### Prerequisites
*   JDK 21+
*   Telegram Bot Token
*   OpenAI API Key

### Run Locally

```bash
# Export your keys
export TELEGRAM_BOT_TOKEN="your_token"
export OPENAI_API_KEY="your_openai_key"

# Run
./gradlew run
```

### Build JAR

```bash
./gradlew shadowJar
java -jar build/libs/korotko-all.jar
```

## 🧪 Testing

The project includes comprehensive unit and property-based tests covering extraction logic, AI integration, and error handling.

```bash
./gradlew test
```

---
*Built with ❤️ by [bkzntsv](https://github.com/bkzntsv)*
