/**
 * Node.js Backend Server for Android Performance Demo
 * 
 * This server provides REST API for photo data:
 * - GET /api/photos - Get all photos
 * - GET /api/photos/:id - Get single photo
 * - POST /api/photos - Create new photo
 * - DELETE /api/photos/:id - Delete photo
 */

const express = require('express');
const cors = require('cors');
const mysql = require('mysql2/promise');

const app = express();
const PORT = 3000;

// Middleware
app.use(cors());
app.use(express.json());

// MySQL Connection Configuration
const dbConfig = {
    host: 'localhost',
    user: 'root',           // Change to your MySQL username
    password: '',           // Change to your MySQL password
    database: 'heavy_gallery_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
};

let pool;

// Initialize Database Connection
async function initializeDatabase() {
    try {
        pool = mysql.createPool(dbConfig);
        console.log('✅ Connected to MySQL database');
        
        // Test connection
        const connection = await pool.getConnection();
        await connection.ping();
        connection.release();
        
        console.log('✅ Database connection test successful');
    } catch (error) {
        console.error('❌ Database connection failed:', error.message);
        console.log('⚠️ Server will use in-memory demo data instead');
        pool = null;
    }
}

// In-memory demo data (fallback when MySQL is not available)
let demoPhotos = generateDemoPhotos(100);

function generateDemoPhotos(count) {
    const photos = [];
    const descriptions = [
        "A beautiful landscape capturing the essence of nature in its purest form. The vibrant colors and stunning composition make this a memorable piece.",
        "Urban photography at its finest. This shot captures the bustling city life with incredible detail and dynamic lighting.",
        "Portrait photography showcasing human emotion and connection. The subject's expression tells a story beyond words.",
        "Wildlife photography capturing a rare moment in nature. The patience required for this shot was well worth the result.",
        "Architectural marvel captured in stunning detail. The play of light and shadow creates a mesmerizing effect."
    ];
    
    const imageUrls = [
        "https://picsum.photos/800/600?random=",
        "https://loremflickr.com/800/600/nature?random=",
        "https://source.unsplash.com/800x600/?landscape,nature&sig="
    ];
    
    for (let i = 1; i <= count; i++) {
        photos.push({
            id: i,
            title: `Amazing Photo ${i}`,
            description: descriptions[i % descriptions.length] + ` Photo ID: ${i}`,
            image_url: `${imageUrls[i % imageUrls.length]}${i}`,
            file_name: `photo_${i}.jpg`,
            file_size_kb: 500 + Math.floor(Math.random() * 2000),
            created_at: new Date(Date.now() - Math.random() * 10000000000).toISOString(),
            updated_at: new Date().toISOString()
        });
    }
    
    return photos;
}

// Routes

// GET /api/photos - Get all photos
app.get('/api/photos', async (req, res) => {
    try {
        if (pool) {
            const [rows] = await pool.query('SELECT * FROM photos ORDER BY created_at DESC');
            res.json(rows);
        } else {
            // Use demo data
            res.json(demoPhotos);
        }
    } catch (error) {
        console.error('Error fetching photos:', error);
        res.json(demoPhotos); // Fallback to demo data
    }
});

// GET /api/photos/:id - Get single photo
app.get('/api/photos/:id', async (req, res) => {
    const { id } = req.params;
    
    try {
        if (pool) {
            const [rows] = await pool.query('SELECT * FROM photos WHERE id = ?', [id]);
            if (rows.length === 0) {
                return res.status(404).json({ error: 'Photo not found' });
            }
            res.json(rows[0]);
        } else {
            // Use demo data
            const photo = demoPhotos.find(p => p.id === parseInt(id));
            if (!photo) {
                return res.status(404).json({ error: 'Photo not found' });
            }
            res.json(photo);
        }
    } catch (error) {
        console.error('Error fetching photo:', error);
        res.status(500).json({ error: 'Database error' });
    }
});

// POST /api/photos - Create new photo
app.post('/api/photos', async (req, res) => {
    const { title, description, image_url, file_name, file_size_kb } = req.body;
    
    if (!title || !image_url) {
        return res.status(400).json({ error: 'Title and image_url are required' });
    }
    
    try {
        if (pool) {
            const [result] = await pool.query(
                'INSERT INTO photos (title, description, image_url, file_name, file_size_kb) VALUES (?, ?, ?, ?, ?)',
                [title, description || '', image_url, file_name || '', file_size_kb || 0]
            );
            
            const [newPhoto] = await pool.query('SELECT * FROM photos WHERE id = ?', [result.insertId]);
            res.status(201).json(newPhoto[0]);
        } else {
            // Add to demo data
            const newId = Math.max(...demoPhotos.map(p => p.id)) + 1;
            const newPhoto = {
                id: newId,
                title,
                description: description || '',
                image_url,
                file_name: file_name || '',
                file_size_kb: file_size_kb || 0,
                created_at: new Date().toISOString(),
                updated_at: new Date().toISOString()
            };
            demoPhotos.unshift(newPhoto);
            res.status(201).json(newPhoto);
        }
    } catch (error) {
        console.error('Error creating photo:', error);
        res.status(500).json({ error: 'Database error' });
    }
});

// DELETE /api/photos/:id - Delete photo
app.delete('/api/photos/:id', async (req, res) => {
    const { id } = req.params;
    
    try {
        if (pool) {
            const [result] = await pool.query('DELETE FROM photos WHERE id = ?', [id]);
            if (result.affectedRows === 0) {
                return res.status(404).json({ error: 'Photo not found' });
            }
            res.json({ message: 'Photo deleted successfully', id: parseInt(id) });
        } else {
            // Remove from demo data
            const index = demoPhotos.findIndex(p => p.id === parseInt(id));
            if (index === -1) {
                return res.status(404).json({ error: 'Photo not found' });
            }
            demoPhotos.splice(index, 1);
            res.json({ message: 'Photo deleted successfully', id: parseInt(id) });
        }
    } catch (error) {
        console.error('Error deleting photo:', error);
        res.status(500).json({ error: 'Database error' });
    }
});

// Health check
app.get('/api/health', (req, res) => {
    res.json({ 
        status: 'ok', 
        database: pool ? 'connected' : 'demo mode',
        timestamp: new Date().toISOString()
    });
});

// Root endpoint
app.get('/', (req, res) => {
    res.json({
        message: 'Photo API Server for Android Performance Demo',
        endpoints: {
            'GET /api/photos': 'Get all photos',
            'GET /api/photos/:id': 'Get single photo',
            'POST /api/photos': 'Create new photo',
            'DELETE /api/photos/:id': 'Delete photo',
            'GET /api/health': 'Health check'
        }
    });
});

// Start server
async function startServer() {
    await initializeDatabase();
    
    app.listen(PORT, () => {
        console.log(`\n🚀 Server is running on http://localhost:${PORT}`);
        console.log(`📱 For Android Emulator, use: http://10.0.2.2:${PORT}`);
        console.log(`\n📋 Available endpoints:`);
        console.log(`   GET  /api/photos     - Get all photos`);
        console.log(`   GET  /api/photos/:id - Get single photo`);
        console.log(`   POST /api/photos     - Create new photo`);
        console.log(`   DELETE /api/photos/:id - Delete photo`);
        console.log(`   GET  /api/health     - Health check`);
    });
}

startServer();
