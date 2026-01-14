# ⏳ Advanced Playtime

![Version](https://img.shields.io/badge/version-1.0.0-blue?style=for-the-badge)
![License](https://img.shields.io/badge/license-MIT-green?style=for-the-badge)
![Hytale](https://img.shields.io/badge/Hytale-Server-orange?style=for-the-badge)

**Advanced Playtime** is a high-performance analytics plugin for Hytale servers. It tracks player sessions down to the millisecond, supports local and remote databases, and provides real-time live leaderboards.

## ✨ Features
- **Live Leaderboards:** Updates instantly, combining historical DB data with the current session of online players.
- **Multi-Period Tracking:** `Daily`, `Weekly`, `Monthly`, and `All-Time` stats.
- **Dual Database:** HikariCP connection pooling for **SQLite** (default) and **MySQL**.
- **Async Architecture:** All database operations run on separate threads to ensure zero server lag.
- **Fully Configurable:** Translate messages, rename commands, and customize colors.

## 📥 Installation
1. Download the latest release from the [Releases Page](../../releases).
2. Place the `.jar` file into your Hytale server's `mods` folder.
3. Start the server.

## 🛠️ Configuration
The plugin generates a `config.json` in `mods/Playtime/`.

```json
{
  "database": {
    "type": "sqlite",
    "host": "localhost",
    "databaseName": "playtime_db"
  },
  "command": {
    "name": "playtime",
    "aliases": ["pt", "stat"]
  },
  "periods": {
    "daily": "daily",
    "weekly": "weekly"
  }
}