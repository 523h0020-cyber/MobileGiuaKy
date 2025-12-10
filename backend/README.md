# Photo API Backend

Simple Node.js REST API server for Android Performance Demo App.

## Quick Start

### Option 1: Without MySQL (Demo Mode)
```bash
cd backend
npm install
npm start
```
Server will run with in-memory demo data (100 sample photos).

### Option 2: With MySQL Database
1. Install MySQL and create database:
```bash
mysql -u root -p < database_setup.sql
```

2. Update database credentials in `server.js`:
```javascript
const dbConfig = {
    host: 'localhost',
    user: 'root',
    password: 'your_password',
    database: 'heavy_gallery_db'
};
```

3. Start server:
```bash
npm install
npm start
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/photos` | Get all photos |
| GET | `/api/photos/:id` | Get single photo |
| POST | `/api/photos` | Create new photo |
| DELETE | `/api/photos/:id` | Delete photo |
| GET | `/api/health` | Health check |

## Testing with Postman/curl

### Get all photos
```bash
curl http://localhost:3000/api/photos
```

### Get single photo
```bash
curl http://localhost:3000/api/photos/1
```

### Create new photo
```bash
curl -X POST http://localhost:3000/api/photos \
  -H "Content-Type: application/json" \
  -d '{"title":"New Photo","description":"Test photo","image_url":"https://picsum.photos/800/600","file_name":"test.jpg","file_size_kb":1024}'
```

### Delete photo
```bash
curl -X DELETE http://localhost:3000/api/photos/1
```

## Android Configuration

For Android Emulator, use `http://10.0.2.2:3000` as base URL.
For real device, use your computer's local IP (e.g., `http://192.168.1.x:3000`).
