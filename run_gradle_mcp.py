import subprocess
import sys
import select

def main():
    jar_path = "/Users/nadezdapsh/gradle-mcp-server-all.jar"
    # Запускаем java процесс
    process = subprocess.Popen(
        ["java", "-jar", jar_path],
        stdin=sys.stdin,       # Прокидываем ввод от Cursor в процесс
        stdout=subprocess.PIPE, # Перехватываем вывод процесса
        stderr=sys.stderr,      # Ошибки сразу перенаправляем в stderr
        text=True,
        bufsize=1               # Построчная буферизация
    )

    # Читаем stdout процесса
    while True:
        line = process.stdout.readline()
        if not line:
            break
        
        # Фильтрация: если строка похожа на JSON-RPC сообщение (начинается с {),
        # то пишем в stdout (для Cursor). Иначе - в stderr (логи).
        if line.strip().startswith("{"):
            sys.stdout.write(line)
            sys.stdout.flush()
        else:
            sys.stderr.write(line)
            sys.stderr.flush()

    process.wait()

if __name__ == "__main__":
    main()

