-- Database Setup Script for Photo Gallery App
-- Run this in MySQL to create the database and tables

CREATE DATABASE IF NOT EXISTS heavy_gallery_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE heavy_gallery_db;

-- Create photos table
CREATE TABLE IF NOT EXISTS photos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255),
    file_size_kb INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert sample data (100 photos for performance testing)
INSERT INTO photos (title, description, image_url, file_name, file_size_kb) VALUES
('Sunset Beach', 'A beautiful sunset at the beach with vibrant orange and pink colors reflecting on the calm water. The silhouettes of palm trees create a perfect tropical atmosphere.', 'https://picsum.photos/800/600?random=1', 'sunset_beach.jpg', 1250),
('Mountain Peak', 'Snow-capped mountain peak rising above the clouds. The crisp air and stunning views make this a perfect destination for adventure seekers.', 'https://picsum.photos/800/600?random=2', 'mountain_peak.jpg', 1480),
('City Lights', 'Urban nightscape showing the city that never sleeps. Thousands of lights creating a mesmerizing pattern across the metropolitan skyline.', 'https://picsum.photos/800/600?random=3', 'city_lights.jpg', 980),
('Forest Path', 'A winding path through an ancient forest. Sunlight filtering through the canopy creates magical patterns on the forest floor.', 'https://picsum.photos/800/600?random=4', 'forest_path.jpg', 1120),
('Ocean Waves', 'Powerful waves crashing against rocky cliffs. The raw power of nature captured in a single moment of spray and foam.', 'https://picsum.photos/800/600?random=5', 'ocean_waves.jpg', 1350),
('Desert Dunes', 'Golden sand dunes stretching to the horizon. The play of light and shadow creates ever-changing patterns on the sand.', 'https://picsum.photos/800/600?random=6', 'desert_dunes.jpg', 890),
('Autumn Leaves', 'Colorful autumn foliage in a peaceful park. Red, orange, and yellow leaves create a carpet of warmth on the ground.', 'https://picsum.photos/800/600?random=7', 'autumn_leaves.jpg', 1150),
('Night Sky', 'Milky Way galaxy stretching across the night sky. Thousands of stars visible in this remote location far from city lights.', 'https://picsum.photos/800/600?random=8', 'night_sky.jpg', 1680),
('Waterfall', 'Majestic waterfall cascading down mossy rocks. The mist creates rainbows when sunlight hits at the right angle.', 'https://picsum.photos/800/600?random=9', 'waterfall.jpg', 1420),
('Flower Garden', 'Vibrant flower garden in full bloom. Butterflies and bees dancing among the colorful petals in the warm sunshine.', 'https://picsum.photos/800/600?random=10', 'flower_garden.jpg', 1280);

-- Generate more sample data for performance testing
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_sample_photos()
BEGIN
    DECLARE i INT DEFAULT 11;
    WHILE i <= 100 DO
        INSERT INTO photos (title, description, image_url, file_name, file_size_kb)
        VALUES (
            CONCAT('Photo ', i),
            CONCAT('This is a detailed description for photo number ', i, '. It contains important information about the image, including composition, lighting, and subject matter. Lorem ipsum dolor sit amet, consectetur adipiscing elit.'),
            CONCAT('https://picsum.photos/800/600?random=', i),
            CONCAT('photo_', i, '.jpg'),
            FLOOR(500 + RAND() * 2000)
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

CALL generate_sample_photos();

-- Verify data
SELECT COUNT(*) as total_photos FROM photos;
SELECT * FROM photos LIMIT 5;
