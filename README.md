# College Alert Application (Task 1)

Full-stack Android Studio project:
- Android app: Java + XML layouts
- Backend: Node.js + Express
- Database: SQLite (created automatically)
- Notifications: Android notifications when newly-created events are detected while the app is refreshed/opened

## Project structure
- `android/` - Android Studio project
- `backend/` - REST API and SQLite database
- `docs/` - API and setup notes

## 1. Start the backend
Install Node.js 18+ then:

```bash
cd backend
npm install
npm start
```

The API starts at `http://localhost:3000`.
Test it at `http://localhost:3000/health` and see events at `http://localhost:3000/api/events`.

### Add an event (admin API)
The demo admin token is `college-admin-123`. Change it in `backend/.env` (copy `.env.example` to `.env`) before real use.

```bash
curl -X POST http://localhost:3000/api/events \
  -H "Content-Type: application/json" \
  -H "x-admin-token: college-admin-123" \
  -d '{"title":"Sports Day","description":"Annual college sports day","event_date":"2026-11-20","event_time":"10:00","venue":"Main Ground"}'
```

## 2. Open Android app
1. Open Android Studio.
2. Choose **Open** and select the `android` folder.
3. Let Gradle sync finish.
4. Start an Android emulator and run the `app` configuration.

The default API address is `http://10.0.2.2:3000/` (Android emulator -> your computer's localhost).
For a physical phone, edit `API_BASE_URL` in `android/app/build.gradle` to your computer's LAN IP, e.g. `http://192.168.1.5:3000/`, and ensure both devices use the same Wi-Fi. Allow port 3000 through your firewall.

## 3. Notifications
On Android 13+, allow notifications when prompted. Tap **Refresh** to check for new events. The app stores event IDs already seen and posts notifications for events added after the first successful load. Notifications are checked when the app is opened/refreshed; this starter project does not claim background push delivery.

## API
- `GET /health` - backend status
- `GET /api/events` - list events (newest first)
- `GET /api/events/:id` - event details
- `POST /api/events` - create event (requires `x-admin-token`)
- `DELETE /api/events/:id` - delete event (requires `x-admin-token`)

## Important production notes
This is a runnable educational starter project. Before production deployment:
- Replace the demo admin token with secure authentication and store secrets outside source control.
- Use HTTPS and deploy the backend to a public host.
- Add proper admin login/roles, rate limiting, input validation, backups and privacy policy.
- For reliable notifications when the app is closed, connect Firebase Cloud Messaging (FCM) and send push messages from the backend.
