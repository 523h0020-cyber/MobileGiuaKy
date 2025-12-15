# 📸 Hướng dẫn Setup Static Images cho Demo

## Bước 1: Chuẩn bị ảnh

Copy các file ảnh từ máy tính vào folder này: `backend/public/images/`

**Gợi ý:**
- Dùng ảnh có kích thước vừa phải (500KB - 2MB) để demo performance
- Đặt tên file đơn giản: `photo1.jpg`, `photo2.jpg`, `landscape.png`...
- Các định dạng hỗ trợ: JPG, PNG, GIF, WebP

## Bước 2: Khởi động Backend Server

```bash
cd backend
npm install
npm start
```

Server sẽ chạy tại `http://localhost:3000`

## Bước 3: Lấy địa chỉ IP máy tính (cho demo trên điện thoại thật)

### Windows:
```bash
ipconfig
```
Tìm dòng **IPv4 Address**, ví dụ: `192.168.1.105`

### Kiểm tra trong PowerShell:
```powershell
(Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.InterfaceAlias -like "*Wi-Fi*"}).IPAddress
```

## Bước 4: Test ảnh có load được không

Mở browser và truy cập:
- Từ máy tính: `http://localhost:3000/images/photo1.jpg`
- Từ điện thoại: `http://192.168.1.105:3000/images/photo1.jpg` (thay IP của bạn)

**Nếu thấy ảnh hiển thị → Thành công!** ✅

## Bước 5: Dùng trong Admin App

1. Mở app trên điện thoại
2. Nhấn nút **🔐 Admin**
3. Nhấn **➕ Add Photo**
4. Nhập:
   - **Title**: Ảnh Demo 1
   - **Description**: Test ảnh từ máy tính
   - **Image URL**: `http://192.168.1.105:3000/images/photo1.jpg` (thay IP của bạn)

## Troubleshooting

❌ **Ảnh không load được:**
- Kiểm tra backend server có đang chạy không (`npm start`)
- Kiểm tra máy tính và điện thoại cùng mạng WiFi
- Thử ping IP từ điện thoại (dùng app Network Tools)
- Kiểm tra firewall Windows có block port 3000 không

❌ **404 Not Found:**
- Kiểm tra tên file có chính xác không (case-sensitive)
- Kiểm tra file có trong folder `backend/public/images/` không

## URL Format

| Môi trường | URL Format | Ví dụ |
|------------|-----------|-------|
| Emulator | `http://10.0.2.2:3000/images/FILENAME` | `http://10.0.2.2:3000/images/photo1.jpg` |
| Real Device | `http://YOUR_IP:3000/images/FILENAME` | `http://192.168.1.105:3000/images/photo1.jpg` |
| Public URLs | Dùng Picsum/Unsplash | `https://picsum.photos/800/600?random=1` |

## Demo Performance Testing

Để test performance với nhiều ảnh:
1. Copy 20-50 ảnh vào folder này
2. Trong Admin, dùng **Add Multiple** → nhập 50
3. Nhập URL pattern: `http://YOUR_IP:3000/images/photo{1-50}.jpg`
4. Quan sát thời gian load và FPS trong MainActivity

## Tips

💡 **Dùng ảnh lớn để demo Jank/Lag:**
- Ảnh > 2MB sẽ làm UI lag rõ rệt khi load sync
- Toggle **BAD Mode** để thấy difference

💡 **Dùng nhiều ảnh để demo Memory Leak:**
- Add 100+ photos
- Scroll nhiều lần
- Xem LeakCanary report

💡 **Quick test với public URLs:**
Nếu không muốn setup local images, dùng Picsum:
```
https://picsum.photos/800/600?random=1
https://picsum.photos/800/600?random=2
...
```
