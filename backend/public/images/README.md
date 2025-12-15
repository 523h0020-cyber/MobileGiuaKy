# Hướng dẫn sử dụng Static Images

## Cách thêm ảnh vào folder này

1. **Copy ảnh từ máy tính vào folder này**
   - Đặt các file ảnh (JPG, PNG, GIF) vào folder `backend/public/images/`
   - Ví dụ: `photo1.jpg`, `sunset.png`, `landscape.jpg`

2. **Định dạng URL để dùng trong Admin**
   - Khi backend server đang chạy (npm start)
   - URL format: `http://localhost:3000/images/TEN_FILE.jpg`
   - Ví dụ:
     - `http://localhost:3000/images/photo1.jpg`
     - `http://localhost:3000/images/sunset.png`
     - `http://localhost:3000/images/landscape.jpg`

3. **Để demo trên điện thoại thật**
   - Tìm địa chỉ IP của máy tính (cmd: `ipconfig` → IPv4)
   - Ví dụ IP: `192.168.1.100`
   - URL format: `http://192.168.1.100:3000/images/TEN_FILE.jpg`
   - **LƯU Ý**: Máy tính và điện thoại phải cùng mạng WiFi

## Ví dụ URL mẫu

Sau khi bạn copy ảnh `demo1.jpg` vào folder này:
- Emulator: `http://10.0.2.2:3000/images/demo1.jpg`
- Real Device: `http://192.168.x.x:3000/images/demo1.jpg`

## Kiểm tra
Mở browser và truy cập: `http://localhost:3000/images/pic1.jpg`
Nếu thấy ảnh hiển thị → OK!
