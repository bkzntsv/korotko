#!/bin/bash

# Скрипт для развертывания HR-бота (Локальная сборка -> Сервер)
# Использование: ./deploy_hr_bot.sh [user@]host

set -e

SERVER="${1:-root@80.92.206.165}"
REMOTE_DIR="/opt/hr-bot"
JAR_FILE="hr-bot/build/libs/hr-bot-1.0-SNAPSHOT-all.jar"
ENV_FILE="hr-bot/src/main/.env"

echo "🚀 Начинаю деплой HR-бота на $SERVER"

# 1. Проверки
if [ ! -f "$ENV_FILE" ]; then
    echo "❌ Файл $ENV_FILE не найден!"
    exit 1
fi

# 2. Локальная сборка
echo "🔨 Собираю HR-бота локально..."
./gradlew :hr-bot:shadowJar --no-daemon

if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Ошибка: файл $JAR_FILE не найден после сборки!"
    exit 1
fi

echo "📦 Готовлю файлы для отправки..."
# Создаем временную директорию для деплоя
RM_DIR="deploy_hr_tmp"
mkdir -p "$RM_DIR"
cp "$JAR_FILE" "$RM_DIR/app.jar"
cp Dockerfile "$RM_DIR/"
cp docker-compose.hr.yml "$RM_DIR/docker-compose.yml"
# Копируем специфичный env файл как .env для docker-compose
cp "$ENV_FILE" "$RM_DIR/.env"

# 3. Отправка на сервер
echo "📤 Отправляю файлы на сервер..."
ssh -T $SERVER "mkdir -p $REMOTE_DIR"
# Копируем содержимое
scp -r "$RM_DIR/." $SERVER:$REMOTE_DIR/

# 4. Запуск на сервере
echo "🔄 Перезапускаю сервис на сервере..."
ssh -T $SERVER << EOF
set -e
cd $REMOTE_DIR

echo "Проверяю наличие .env файла..."
ls -la .env || echo "❌ ФАЙЛ .env НЕ НАЙДЕН НА СЕРВЕРЕ!"

# Установка Docker (если нет - хотя скорее всего уже есть)
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
fi

# Определение команды docker compose
if docker compose version &> /dev/null; then
    DC="docker compose"
else
    DC="docker-compose"
fi

echo "Пересобираю контейнер HR-бота..."
\$DC down || true
\$DC up -d --build --remove-orphans

echo "Статус:"
\$DC ps

echo "Последние логи:"
sleep 5
\$DC logs --tail=20
EOF

# 5. Уборка
rm -rf "$RM_DIR"
echo "✅ Деплой HR-бота успешно завершен!"
