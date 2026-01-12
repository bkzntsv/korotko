#!/bin/bash

# Скрипт для развертывания проекта (Локальная сборка -> Сервер)
# Использование: ./deploy.sh [user@]host

set -e

SERVER="${1:-root@80.92.206.165}"
REMOTE_DIR="/opt/ochemeto"
JAR_FILE="build/libs/ai-content-summarizer-1.0-SNAPSHOT-all.jar"

echo "🚀 Начинаю деплой на $SERVER"

# 1. Проверки
if [ ! -f .env ]; then
    echo "❌ Файл .env не найден! Скопируйте env.template в .env"
    exit 1
fi

# 2. Локальная сборка
echo "🔨 Собираю проект локально..."
./gradlew shadowJar --no-daemon

if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Ошибка: файл $JAR_FILE не найден после сборки!"
    exit 1
fi

echo "📦 Готовлю файлы для отправки..."
# Создаем временную директорию для деплоя
mkdir -p deploy_tmp
cp "$JAR_FILE" deploy_tmp/app.jar
cp Dockerfile deploy_tmp/
cp docker-compose.yml deploy_tmp/
# Явно копируем .env
cp .env deploy_tmp/.env

# 3. Отправка на сервер
echo "📤 Отправляю файлы на сервер..."
ssh -T $SERVER "mkdir -p $REMOTE_DIR"
# Копируем содержимое, включая скрытые файлы
scp -r deploy_tmp/. $SERVER:$REMOTE_DIR/

# 4. Запуск на сервере
echo "🔄 Перезапускаю сервис на сервере..."
ssh -T $SERVER << EOF
set -e
cd $REMOTE_DIR

echo "Проверяю наличие .env файла..."
ls -la .env || echo "❌ ФАЙЛ .env НЕ НАЙДЕН НА СЕРВЕРЕ!"

# Установка Docker (если нет)
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
fi

# Проверка/Установка docker-compose
if ! docker compose version &> /dev/null; then
    if ! command -v docker-compose &> /dev/null; then
        curl -SL https://github.com/docker/compose/releases/download/v2.24.6/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
        chmod +x /usr/local/bin/docker-compose
    fi
fi

# Определение команды
if docker compose version &> /dev/null; then
    DC="docker compose"
else
    DC="docker-compose"
fi

echo "Пересобираю контейнер..."
# Явно указываем файл .env
\$DC --env-file .env down || true
\$DC --env-file .env up -d --build --remove-orphans

echo "Статус:"
\$DC ps

echo "Последние логи (проверка старта):"
sleep 5
\$DC logs --tail=20
EOF

# 5. Уборка
rm -rf deploy_tmp
echo "✅ Деплой успешно завершен!"
