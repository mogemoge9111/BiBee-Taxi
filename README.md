# BiBee-Taxi
Курсовая работа / 3 курс / Замотаева Анастасия НИУ ВШЭ ФКН БПИ237
**BiBeeTaxi** — учебный проект приложения для заказа такси, реализованный на Java в Android Studio. Поддерживает две роли: пассажир и водитель, карту на основе Яндекс.Карт, чат, историю поездок и систему рейтинга.

# 🚖 BiBeeTaxi — сервис заказа такси на Android

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Яндекс.Карты](https://img.shields.io/badge/Яндекс.Карты-FFCC00?style=for-the-badge&logo=yandex&logoColor=black)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)

## ✨ Возможности

- 🔐 Регистрация и вход по email/паролю через Firebase Authentication
- 🗺️ Интерактивная карта с поиском адресов (Яндекс.Карты)
- 📍 Выбор точек отправления и назначения на карте
- 💰 Автоматический расчёт стоимости по расстоянию
- 👥 Два интерфейса: пассажир и водитель
- 🚘 Водитель управляет статусом «Готов принимать заказы»
- 📋 Создание заказов с указанием адресов, цены и описания
- ✅ Процесс подтверждения: пассажир видит профиль водителя и подтверждает поездку
- 💬 Чат между пассажиром и водителем (Realtime Database)
- ⭐ Рейтинг и отзывы после завершения поездки
- 📜 История поездок с цветовой индикацией статусов
- 📸 Профиль с возможностью загрузки фото (Base64 в Firestore)
- ✉️ Форма обратной связи (сообщения сохраняются в Firestore)

## 🛠️ Технологии

- **Язык**: Java
- **Среда разработки**: Android Studio
- **Карты**: Яндекс.Карты SDK (MapKit 4.8.0-full)
- **База данных**: Firebase Firestore (основная), Firebase Realtime Database (чат)
- **Аутентификация**: Firebase Authentication (Email/Password)
- **Хранение фото**: Base64 в Firestore
- **Архитектура**: Fragment + Activity, BottomNavigationView

## 📩 Контакты
По всем вопросам: Telegram (@djkoshka) или создайте Issue.
