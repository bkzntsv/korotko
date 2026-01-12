# Инструкция по развертыванию на сервере

## Требования

- Доступ к серверу по SSH
- Права sudo на сервере (для установки Docker при необходимости)

## Быстрое развертывание

### 1. Подготовка локально

```bash
# Скопируйте env.template в .env
cp env.template .env

# Отредактируйте .env и укажите ваши ключи
nano .env
```

### 2. Запуск деплоя

```bash
# Сделайте скрипт исполняемым
chmod +x deploy.sh

# Запустите деплой (замените user на вашего пользователя, если не root)
./deploy.sh root@80.92.206.165
```

Скрипт автоматически:
- Создаст архив проекта
- Загрузит файлы на сервер
- Установит Docker и Docker Compose (если нужно)
- Соберет и запустит контейнер

## Ручное развертывание

Если предпочитаете ручное развертывание:

### 1. Подключитесь к серверу

```bash
ssh root@80.92.206.165
```

### 2. Установите Docker (если не установлен)

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
rm get-docker.sh
```

### 3. Установите Docker Compose (если не установлен)

```bash
apt-get update
apt-get install -y docker-compose-plugin
```

### 4. Клонируйте или загрузите проект

```bash
mkdir -p /opt/ochemeto
cd /opt/ochemeto
# Загрузите файлы проекта (git clone, scp, и т.д.)
```

### 5. Создайте файл .env

```bash
nano .env
```

Укажите ваши ключи:
```
TELEGRAM_BOT_TOKEN=your_token
OPENAI_API_KEY=your_key
USER_AGENT=Mozilla/5.0 (compatible; Bot/1.0)
```

### 6. Запустите контейнер

```bash
docker compose up -d --build
```

## Управление после развертывания

### Просмотр логов

```bash
ssh root@80.92.206.165
cd /opt/ochemeto
docker compose logs -f
```

### Остановка бота

```bash
cd /opt/ochemeto
docker compose down
```

### Перезапуск бота

```bash
cd /opt/ochemeto
docker compose restart
```

### Обновление (после изменений в коде)

```bash
cd /opt/ochemeto
docker compose down
docker compose build --no-cache
docker compose up -d
```

### Просмотр статуса

```bash
docker compose ps
```

## Устранение проблем

### Проверка переменных окружения

```bash
docker compose exec bot env | grep -E "TELEGRAM_BOT_TOKEN|OPENAI_API_KEY"
```

### Просмотр ошибок в контейнере

```bash
docker compose logs bot
```

### Пересборка образа

```bash
docker compose build --no-cache
docker compose up -d
```

## Безопасность

- Не коммитьте файл `.env` в git
- Используйте SSH ключи для доступа к серверу
- Регулярно обновляйте зависимости проекта
- Ограничьте доступ к серверу firewall'ом
