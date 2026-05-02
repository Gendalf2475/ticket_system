# Сборка TicketSystem в plugin `.jar`

## Целевая версия

Плагин настроен под Minecraft/Paper/Spigot `1.21.8+`.

- Java bytecode: `21`, задаётся в `pom.xml` через `<release>21</release>`.
- Bukkit/Spigot API для компиляции: `1.21.8-R0.1-SNAPSHOT`.
- `plugin.yml`: `api-version: '1.21.8'`.
- Итоговый jar собирается Maven Shade Plugin и включает TelegramBots/MineDown зависимости.

Для серверов Minecraft `1.21.8+` используйте Java `21`. Более новые JDK могут скомпилировать проект с `--release 21`, но для запуска Paper 1.21.x рекомендуемый вариант - Java 21.

## Что установить

1. JDK 21.
2. Maven 3.9 или новее.

Проверка:

```bash
java -version
javac -version
mvn -version
```

В выводе Java/Javac желательно видеть `21`. Если установлен JDK новее, Maven всё равно соберёт байткод Java 21 благодаря настройке `<release>21</release>`.

## Быстрая сборка

Из корня проекта:

```bash
mvn clean package
```

Если тестов нет или их нужно пропустить:

```bash
mvn -DskipTests clean package
```

Готовый файл будет здесь:

```text
target/TicketSystem.jar
```

Именно этот jar нужно класть в папку:

```text
plugins/TicketSystem.jar
```

## Установка на сервер

1. Остановите сервер.
2. Скопируйте `target/TicketSystem.jar` в папку `plugins/`.
3. Запустите сервер один раз, чтобы плагин создал `plugins/TicketSystem/config.yml`.
4. Остановите сервер и настройте `config.yml`.
5. Для Telegram заполните:

```yaml
telegram:
  enabled: true
  bot-token: "TOKEN_FROM_BOTFATHER"
  super-admins:
    - 123456789
  topics:
    new-tickets:
      chat-id: -1001234567890
      thread-id: 10
    closed-tickets:
      chat-id: -1001234567890
      thread-id: 11
```

6. Запустите сервер снова.

## Если Maven использует не ту Java

Укажите `JAVA_HOME` на JDK 21 перед сборкой.

macOS/Linux:

```bash
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean package
```

Windows PowerShell:

```powershell
$env:JAVA_HOME="C:\Path\To\jdk-21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn clean package
```

## Проверка jar

После сборки проверьте, что jar создан:

```bash
ls -lh target/TicketSystem.jar
```

Можно также проверить, что внутри есть `plugin.yml`:

```bash
jar tf target/TicketSystem.jar | grep plugin.yml
```

## Частые ошибки

- `mvn: command not found` - Maven не установлен или не добавлен в `PATH`.
- `release version 21 not supported` - Maven запущен на JDK ниже 21.
- `UnsupportedClassVersionError` на сервере - сервер запущен на Java ниже 21.
- Telegram-бот не стартует - проверьте `telegram.enabled`, `bot-token`, `chat-id/thread-id` и права бота в группе.
