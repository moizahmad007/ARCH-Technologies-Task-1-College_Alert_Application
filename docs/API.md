# REST API reference

Base URL: `http://localhost:3000`

## GET /health
Returns service health.

## GET /api/events
Returns `{ "events": [...] }`.

## GET /api/events/:id
Returns `{ "event": {...} }`, or 404.

## POST /api/events
Header: `x-admin-token: <ADMIN_TOKEN>`
JSON body:
```json
{
  "title": "Sports Day",
  "description": "Annual sports day",
  "event_date": "2026-11-20",
  "event_time": "10:00",
  "venue": "Main Ground"
}
```

## DELETE /api/events/:id
Requires the same admin header.
