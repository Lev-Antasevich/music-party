# Music Party

Android-приложение для **совместного прослушивания музыки в реальном времени**. Хост создаёт комнату и управляет воспроизведением, гости подключаются по 6-значному коду и слушают синхронно — тот же трек, та же позиция, общий play/pause.

Backend не нужен: авторизация и синхронизация построены на **Firebase Authentication** и **Firebase Realtime Database**.

---

## Как это работает

1. Пользователь входит через email, Google или как гость.
2. Хост создаёт комнату и получает короткий код для приглашения.
3. Гости вводят код и попадают в ту же комнату.
4. Хост выбирает трек и управляет воспроизведением — изменения мгновенно отражаются у всех участников через Firebase.

```
Вход → Комната (код) → Хост управляет / Гости слушают → Firebase RTDB синхронизирует состояние
```

---

## Возможности

### Комнаты и синхронизация

- Создание комнаты с названием и вход по 6-значному коду
- Синхронизация текущего трека, play/pause и позиции воспроизведения
- Список участников с ролями Host / Guest в реальном времени
- Автоматическое закрытие комнаты при выходе хоста

### Источники музыки

| Тип | Описание |
|-----|----------|
| **Local** | Аудиофайлы с устройства через MediaStore и ExoPlayer (Media3) |
| **Link** | YouTube и другие ссылки; гости открывают URL с синхронизированной позицией |
| **Stream** | Прямой URL на аудио или перехват stream из WebView на музыкальных сайтах |

### Интерфейс

- Тёмная тема в стиле современных музыкальных приложений
- Ambient-градиент, который подстраивается под цвета текущего трека (Palette API)
- Адаптивные экраны для хоста и гостя с разным набором действий

---

## Firebase

Проект использует два сервиса Firebase — без собственного сервера и без лишних зависимостей (Analytics, FCM, Crashlytics и т.д.).

### Authentication

| Метод | Зачем |
|-------|-------|
| Email / Password | Регистрация и постоянный аккаунт |
| Google Sign-In | Быстрый вход через OAuth |
| Anonymous | Гостевой вход без регистрации |

Каждый участник комнаты идентифицируется через Firebase `uid`. Имя берётся из профиля или задаётся при гостевом входе.

### Realtime Database

RTDB хранит состояние комнаты и рассылает обновления всем подключённым клиентам через `ValueEventListener`.

| Действие | Что происходит в Firebase |
|----------|---------------------------|
| Создание комнаты | `setValue()` — запись структуры комнаты с уникальным кодом |
| Вход гостя | `setValue()` — добавление в `participants/{uid}` |
| Смена трека | `updateChildren()` — обновление `currentTrack`, сброс позиции |
| Play / Pause / Seek | `updateChildren()` — `isPlaying`, `positionMs`, `updatedAt` |
| Выход / закрытие | `removeValue()` — удаление участника или всей комнаты |

Чтобы компенсировать сетевую задержку, гость вычисляет актуальную позицию по формуле `positionMs + (now - updatedAt)` при активном воспроизведении — без постоянного polling каждой миллисекунды.

### Модель данных

```
rooms/
  {roomCode}/
    hostId, hostName, roomName
    isPlaying, positionMs, updatedAt
    currentTrack/          # type, title, artist, uri, linkUrl, durationMs
    participants/{uid}/    # name, host
```

### Правила безопасности

```json
{
  "rules": {
    "rooms": {
      "$roomCode": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

---

## Технологии

| | |
|---|---|
| **Язык** | Java 11 |
| **UI** | Material Components, ConstraintLayout, RecyclerView |
| **Медиа** | ExoPlayer (Media3) |
| **Backend** | Firebase Auth, Firebase Realtime Database |
| **OAuth** | Google Sign-In |
| **Android** | minSdk 30, targetSdk 36 |

---

## Архитектура

```
app/
├── auth/           AuthManager, AuthValidator
├── model/          Track, RoomState, Participant
├── repository/     RoomManager, FirebaseRoomMapper
├── player/         MusicPlayer (ExoPlayer)
├── adapter/        TrackAdapter, ParticipantAdapter (ListAdapter + DiffUtil)
├── ui/             AmbientGradientView, AmbientAtmosphere
└── util/           MediaStore, LinkParser, StreamUrlHelper, TimeFormatter
```

Firebase-логика вынесена в repository-слой: `RoomManager` управляет подписками и кэшем, `FirebaseRoomMapper` преобразует данные между Java-моделями и JSON в RTDB. UI подписывается на изменения через callbacks и не работает с Firebase напрямую.

---

## Запуск

### Требования

- Android Studio
- JDK 11+
- Firebase-проект с **Authentication** и **Realtime Database**

### Настройка Firebase

1. Создайте проект в [Firebase Console](https://console.firebase.google.com/)
2. Добавьте Android-приложение: `com.example.musicparty`
3. Скачайте `google-services.json` → `app/`
4. Включите провайдеры: Email/Password, Google, Anonymous
5. Создайте Realtime Database и задайте правила безопасности (см. выше)
6. Укажите URL базы в `app/src/main/res/values/strings.xml`:

```xml
<string name="firebase_database_url" translatable="false">
  https://your-project-default-rtdb.europe-west1.firebasedatabase.app
</string>
```

Для Google Sign-In добавьте SHA-1 debug-ключа в настройки приложения Firebase и обновите `google-services.json`:

```bash
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android
```

### Сборка

```bash
git clone https://github.com/Lev-Antasevich/MusicParty.git
cd MusicParty
./gradlew assembleDebug
```

Или откройте проект в Android Studio и нажмите **Run**.

Для проверки синхронизации запустите приложение на двух устройствах: создайте комнату на одном, войдите по коду на втором.

---

## Лицензия

Свободен для просмотра и использования в портфолио.
