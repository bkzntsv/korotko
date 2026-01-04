# Настройка Kotlin MCP серверов

## Что такое Kotlin MCP SDK

Kotlin MCP SDK - это официальная библиотека от JetBrains для создания MCP (Model Context Protocol) серверов и клиентов на Kotlin. Это не готовый сервер, а инструмент для разработки собственных серверов.

## Варианты использования

### 1. Готовые Kotlin MCP серверы

#### Gradle MCP Server
Готовый сервер для работы с Gradle проектами:

```bash
# Скачать JAR файл
curl -L -o ~/gradle-mcp-server-all.jar https://github.com/IlyaGulya/gradle-mcp-server/releases/latest/download/gradle-mcp-server-all.jar
```

Конфигурация в `~/.kiro/settings/mcp.json`:
```json
{
  "mcpServers": {
    "gradle-mcp-server": {
      "command": "java",
      "args": [
        "-jar",
        "/Users/nadezdapsh/gradle-mcp-server-all.jar"
      ],
      "env": {},
      "disabled": false,
      "autoApprove": ["get_gradle_project_info"]
    }
  }
}
```

### 2. Создание собственного Kotlin MCP сервера

#### Шаг 1: Создание проекта
```bash
mkdir my-kotlin-mcp-server
cd my-kotlin-mcp-server
```

#### Шаг 2: build.gradle.kts
```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/mcp/maven")
}

dependencies {
    implementation("org.jetbrains.mcp:kotlin-sdk-server:0.1.0")
    implementation("org.jetbrains.mcp:kotlin-sdk-shared:0.1.0")
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-cio:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

application {
    mainClass.set("MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("my-mcp-server")
    archiveClassifier.set("all")
}
```

#### Шаг 3: Простой сервер (src/main/kotlin/Main.kt)
```kotlin
import kotlinx.coroutines.runBlocking
import org.jetbrains.mcp.server.*
import org.jetbrains.mcp.shared.*

fun main() = runBlocking {
    val server = McpServer.stdio {
        // Добавляем инструмент
        tool("hello") {
            description = "Приветствие пользователя"
            parameter("name", "string") {
                description = "Имя пользователя"
                required = true
            }
            
            handler { params ->
                val name = params["name"] as? String ?: "Мир"
                ToolResult.text("Привет, $name!")
            }
        }
        
        // Добавляем ресурс
        resource("info") {
            description = "Информация о сервере"
            mimeType = "text/plain"
            
            handler {
                ResourceResult.text("Это мой Kotlin MCP сервер!")
            }
        }
    }
    
    server.start()
}
```

#### Шаг 4: Сборка
```bash
./gradlew shadowJar
```

#### Шаг 5: Конфигурация MCP
```json
{
  "mcpServers": {
    "my-kotlin-server": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/my-kotlin-mcp-server/build/libs/my-mcp-server-all.jar"
      ],
      "env": {},
      "disabled": false,
      "autoApprove": ["hello"]
    }
  }
}
```

## Полезные ссылки

- [Kotlin MCP SDK](https://github.com/modelcontextprotocol/kotlin-sdk)
- [Документация](https://modelcontextprotocol.github.io/kotlin-sdk/)
- [Gradle MCP Server](https://github.com/IlyaGulya/gradle-mcp-server)
- [Примеры серверов](https://github.com/modelcontextprotocol/servers)

## Следующие шаги

1. Скачайте готовый Gradle MCP Server для начала
2. Обновите конфигурацию в `~/.kiro/settings/mcp.json`
3. Перезапустите Kiro или переподключите MCP серверы
4. Создайте собственный сервер используя Kotlin SDK при необходимости