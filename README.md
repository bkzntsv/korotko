# Korotko

![Build Status](https://github.com/bkzntsv/korotko/actions/workflows/ci.yml/badge.svg)

**Korotko** is an intelligent Telegram bot that transforms lengthy web articles and voice messages into structured, executive summaries. It's filtering noise and highlighting key insights.

## Key Features

    **Executive Summaries**: Delivers the main idea and key takeaways, answering "Why does this matter?"
    **Voice Transcription**: Transcribes voice messages and audio files, then summarizes long transcripts automatically.
    **Smart Analysis**: Evaluates article sentiment and detects clickbait (0-10 score).
    **Clean Extraction**: Uses `Readability4J` to strip ads, navigation, and paywalls, focusing only on the content.
    **Multilingual Input**: Summarizes articles in any language into Russian.
    **Privacy-First**: No data storage, processes links and voice messages on-the-fly.

## Tech Stack

Built with **Kotlin** and **Clean Architecture** principles.

    **Core**: Kotlin 1.9, Coroutines
    **Network**: Ktor Client (CIO engine)
    **Parsing**: Jsoup, Readability4J
    **AI**: OpenAI API (GPT-4o-mini for summaries, Whisper for transcription)
    **Bot**: Kotlin Telegram Bot API
    **DI**: Koin
    **File Handling**: Okio
    **Testing**: Kotest (Property-based testing), MockK

## Getting Started

### Prerequisites
    JDK 21+
    Telegram Bot Token
    OpenAI API Key

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

## 📝 Usage

### Text Messages
Send a link to any article, and the bot will extract and summarize it.

### Voice Messages
Send a voice message or audio file (up to 20 MB). The bot will:
Transcribe the audio using Whisper
If the transcript is short (< 50 chars), return the text as-is
If longer, automatically generate a structured summary

## 🧪 Testing

The project includes comprehensive unit and property-based tests covering extraction logic, AI integration, voice transcription, and error handling.

```bash
./gradlew test
```

---
*Built with ❤️ by [bkzntsv](https://github.com/bkzntsv)*
