# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копируем готовый jar файл, который мы соберем локально
COPY app.jar app.jar

# Ограничиваем память для Java приложения (512 МБ вполне достаточно для работы)
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
