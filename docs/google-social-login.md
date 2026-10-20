# Общая регистрация Google и Яндекс

Кнопки главной страницы не меняются. Gateway направляет Google и Яндекс в SSO с login_hint.
Google проходит OpenID Connect с проверкой ID token; новый подтверждённый профиль хранится только в серверной сессии 10 минут.
Форма в отдельном репозитории frontend: /social-registration; /yandex-registration сохранён как совместимый маршрут.
Старая учётная запись не связывается автоматически по email. Социальные ID раздельные и уникальные.
Новый пользователь выбирает TRACKER/ADMIN и принимает три согласия. Доступ появляется после активации администратором.

Переменные SSO и gateway: GOOGLE_LOGIN_ENABLED=true и YANDEX_LOGIN_ENABLED=true; GOOGLE_CLIENT_ID и GOOGLE_CLIENT_SECRET.
У SSO: GOOGLE_REDIRECT_URI=https://sso.trackme.test.startup-poligon.com/login/oauth2/code/google.
Для localhost: GOOGLE_REDIRECT_URI=http://localhost:9000/login/oauth2/code/google.
Добавить соответствующие адреса в разрешённые redirect URI Google Cloud, не удалять действующие callback API.
У gateway должен быть публичный SSO_URI либо публичный YANDEX_REDIRECT_URI/GOOGLE_REDIRECT_URI для /oauth2/social/config.
FRONTEND_URI SSO должен соответствовать origin фронтенда для CORS.

Необязательное получение телефона на сервере включается через GOOGLE_PHONE_ENABLED=true у SSO: включить People API в Google Cloud
и настроить consent screen для https://www.googleapis.com/auth/user.phonenumbers.read.
Могут потребоваться тестовые пользователи или проверка приложения Google для дополнительных прав.
Google не гарантирует наличие номера, в том числе номера восстановления аккаунта.
Отказ в доступе к телефону или сбой People API не прерывает вход. В форме телефон необязателен и доступен для ручного ввода и исправления.
Номер не используется как подтверждённый идентификатор или основание для объединения аккаунтов.
Токен передаётся People API только сервером в Authorization, а не в URL и не во фронтенд.
API профиля и регистрации: /client/social/profile и /client/social/registration (CSRF + cookie SSO).
Номер хранится в существующем поле phone_number, google_id добавляется отдельной миграцией.

Телефон Яндекса: сначала включить право «Доступ к номеру телефона» в настройках приложения Яндекс ID,
затем задать YANDEX_PHONE_ENABLED=true у SSO. При новом входе запрашивается login:default_phone,
а default_phone.number передаётся в общую форму и сохраняется при регистрации.
Без этого флага действуют прежние права login:info и login:email. Отсутствующий или некорректный телефон
не блокирует вход; пользователь может ввести номер вручную. Уже полученный профиль в сессии не обновляется:
после изменения прав нужно заново начать вход через Яндекс с главной страницы.
