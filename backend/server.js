require('dotenv').config();
const express = require('express');
const cors = require('cors');
const Database = require('better-sqlite3');

const app = express();
app.use(cors());
app.use(express.json({ limit: '32kb' }));

const db = new Database('college-alert.sqlite');
db.pragma('journal_mode = WAL');
db.exec(`
  CREATE TABLE IF NOT EXISTS events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    event_date TEXT NOT NULL,
    event_time TEXT NOT NULL,
    venue TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  )
`);

const count = db.prepare('SELECT COUNT(*) AS count FROM events').get().count;
if (count === 0) {
  const insert = db.prepare(`INSERT INTO events
    (title, description, event_date, event_time, venue)
    VALUES (?, ?, ?, ?, ?)`);
  insert.run('Welcome Orientation', 'Meet your teachers, explore the campus and learn about college life.', '2026-11-12', '09:00', 'College Auditorium');
  insert.run('Science Exhibition', 'Student science projects and demonstrations.', '2026-11-18', '11:00', 'Science Block');
}

const adminOnly = (req, res, next) => {
  const expected = process.env.ADMIN_TOKEN || 'college-admin-123';
  if (!req.get('x-admin-token') || req.get('x-admin-token') !== expected) {
    return res.status(401).json({ error: 'Unauthorized: valid x-admin-token required' });
  }
  next();
};

app.get('/health', (req, res) => res.json({ status: 'ok', service: 'college-alert-api' }));

app.get('/api/events', (req, res) => {
  const events = db.prepare('SELECT * FROM events ORDER BY event_date ASC, event_time ASC, id DESC').all();
  res.json({ events });
});

app.get('/api/events/:id', (req, res) => {
  const event = db.prepare('SELECT * FROM events WHERE id = ?').get(Number(req.params.id));
  if (!event) return res.status(404).json({ error: 'Event not found' });
  res.json({ event });
});

app.post('/api/events', adminOnly, (req, res) => {
  const { title, description, event_date, event_time, venue } = req.body || {};
  if (![title, description, event_date, event_time, venue].every(v => typeof v === 'string' && v.trim())) {
    return res.status(400).json({ error: 'title, description, event_date, event_time and venue are required' });
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(event_date) || !/^\d{2}:\d{2}$/.test(event_time)) {
    return res.status(400).json({ error: 'Use date YYYY-MM-DD and time HH:MM' });
  }
  const result = db.prepare(`INSERT INTO events (title, description, event_date, event_time, venue)
    VALUES (?, ?, ?, ?, ?)`).run(title.trim(), description.trim(), event_date, event_time, venue.trim());
  const event = db.prepare('SELECT * FROM events WHERE id = ?').get(result.lastInsertRowid);
  res.status(201).json({ message: 'Event created', event });
});

app.delete('/api/events/:id', adminOnly, (req, res) => {
  const result = db.prepare('DELETE FROM events WHERE id = ?').run(Number(req.params.id));
  if (!result.changes) return res.status(404).json({ error: 'Event not found' });
  res.json({ message: 'Event deleted' });
});

const port = Number(process.env.PORT || 3000);
app.listen(port, '0.0.0.0', () => console.log(`College Alert API running on port ${port}`));
