# 📋 Hướng dẫn Demo Chi tiết

## Chuẩn bị

### 1. Khởi động Backend Server
```powershell
cd backend
npm install
npm start
```

Server sẽ chạy tại `http://localhost:3000` với dữ liệu demo.

### 2. Cấu hình Android App
Trong `ApiClient.java`, đảm bảo BASE_URL đúng:
- **Emulator**: `http://10.0.2.2:3000/`
- **Real device**: `http://[IP_máy_tính]:3000/`

### 3. Build & Run App
- Mở project trong Android Studio
- Run trên emulator hoặc thiết bị
- Đảm bảo build variant là **debug** (để có LeakCanary)

---

## Demo 1: Jank/Lag (UI Blocking)

### Mục tiêu
Chứng minh sự khác biệt giữa bad practices và good practices trong RecyclerView.

### Bước thực hiện

#### A. Demo BAD Mode
1. Mở app, đảm bảo **"Use BAD Implementation"** đang **BẬT** (switch màu)
2. Mở **Android Studio → View → Tool Windows → Profiler**
3. Chọn app process, click vào **CPU**
4. Nhấn **Record** để bắt đầu trace
5. **Cuộn danh sách ảnh** lên xuống liên tục
6. Quan sát:
   - UI bị **khựng/giật**
   - Trong Profiler, thấy **main thread** bị block
   - Frame time **> 16ms** (không đạt 60 FPS)

#### B. Demo GOOD Mode
1. **TẮT** switch "Use BAD Implementation"
2. Record trace mới
3. Cuộn danh sách
4. Quan sát:
   - UI **mượt mà**, không giật
   - Main thread **nhẹ**
   - Frame time **< 16ms**

### Giải thích
| Mode | Vấn đề | Kết quả |
|------|--------|---------|
| BAD | Download ảnh sync, heavy processing trong onBindViewHolder | FPS ~20-30, UI freeze |
| GOOD | Glide async loading, minimal binding work | FPS ~60, smooth |

---

## Demo 2: High CPU Usage

### Mục tiêu
Chứng minh algorithm complexity ảnh hưởng đến CPU.

### Bước thực hiện

#### A. Demo BAD Mode (CPU Spike)
1. Bật **"Use BAD Implementation"**
2. Mở **CPU Profiler**, nhấn **Record**
3. Nhấn nút **"Stress CPU"**
4. Quan sát:
   - CPU spike lên **~100%**
   - App bị **đơ** vài giây
   - Profiler hiển thị `inefficientSort()` và `heavyStringProcessing()` chiếm CPU

#### B. Demo GOOD Mode
1. Tắt switch BAD mode
2. Nhấn "Stress CPU" lại
3. Quan sát:
   - CPU spike **thấp hơn nhiều**
   - App **vẫn responsive**
   - Toast hiện nhanh hơn

### So sánh kết quả
| Metric | BAD | GOOD |
|--------|-----|------|
| Sort 2000 items | ~2500ms | ~15ms |
| CPU Peak | 95% | 25% |
| UI Responsive | No | Yes |

---

## Demo 3: Memory Leak

### Mục tiêu
Chứng minh memory leak và cách LeakCanary phát hiện.

### Bước thực hiện

#### A. Gây Memory Leak
1. Mở **Memory Profiler** trong Android Studio
2. Tap vào **bất kỳ photo** để mở Detail screen
3. Đảm bảo **"Enable Memory Leak Mode"** đang **BẬT**
4. Nhấn nút **"Cause Memory Leak"**
5. Nhấn **nút Back** để quay lại
6. **Lặp lại bước 2-5** khoảng **5 lần**
7. Quan sát trong Memory Profiler:
   - Memory **tăng dần** mỗi lần vào Detail
   - Nhấn **Force GC** - Memory **KHÔNG giảm**

#### B. LeakCanary Notification
1. Sau vài lần leak, **LeakCanary** sẽ hiện notification
2. Tap vào notification để xem **leak trace**
3. LeakCanary chỉ ra:
   - `LeakyManager` đang giữ `PhotoDetailActivity`
   - Chain of references preventing GC

#### C. Phân tích Heap Dump
1. Trong Memory Profiler, nhấn **Heap Dump**
2. Tìm `PhotoDetailActivity` trong heap
3. Sẽ thấy **nhiều instances** (thay vì 0 sau khi back)
4. Xem **References** - thấy `LeakyManager` giữ reference

### Giải thích Leak Pattern
```
LeakyManager (Singleton - sống mãi)
    └── context: PhotoDetailActivity (LEAKED!)
    └── listener: PhotoDetailActivity (LEAKED!)
```

---

## Demo 4: Search/Sort Performance

### Mục tiêu
So sánh inefficient vs efficient algorithms.

### Bước thực hiện

#### A. Search Performance
1. Bật **BAD Mode**
2. Gõ từ khóa vào ô search
3. Quan sát: **Chậm**, có delay đáng kể
4. Tắt BAD Mode
5. Search lại: **Nhanh**, instant results

#### B. Sort Performance
1. Bật **BAD Mode**
2. Nhấn **"Sort A-Z"**
3. Observe: Delay **vài trăm ms** (Toast hiện time)
4. Tắt BAD Mode
5. Sort lại: **< 50ms**

---

## Demo 5: Main Thread Download

### Mục tiêu
Chứng minh network trên main thread gây ANR.

### Bước thực hiện

1. Mở **Detail screen** của bất kỳ photo
2. Nhấn **"Download on MAIN THREAD (BAD)"**
3. Quan sát:
   - App **đơ hoàn toàn** ~3-5 giây
   - Không thể interact
   - Sau khi xong mới responsive lại

4. Nhấn **"Download on BACKGROUND THREAD (GOOD)"**
5. Quan sát:
   - App **vẫn responsive** trong khi download
   - Progress indicator hoạt động
   - UI vẫn có thể scroll/interact

---

## Checklist Demo

### Trước Demo
- [ ] Backend server đang chạy
- [ ] App đã cài đặt (debug build)
- [ ] Android Profiler sẵn sàng
- [ ] LeakCanary đã được init

### Trong Demo
- [ ] Demo Jank với BAD/GOOD toggle
- [ ] Demo CPU với Stress button
- [ ] Demo Memory Leak với Detail screen
- [ ] Demo Download BAD/GOOD
- [ ] Show Profiler graphs
- [ ] Show LeakCanary notification

### Metrics cần ghi nhận
- [ ] FPS khi scroll (BAD vs GOOD)
- [ ] Sort time (BAD vs GOOD)
- [ ] Memory after leaks
- [ ] CPU peak during stress

---

## Troubleshooting

### App không kết nối được server
- Kiểm tra BASE_URL trong `ApiClient.java`
- Emulator: `10.0.2.2:3000`
- Real device: IP máy tính (cùng WiFi)

### LeakCanary không hiện notification
- Đảm bảo build variant là **debug**
- Leak detection cần vài giây để analyze
- Thử gây leak nhiều lần hơn

### Profiler không attach được
- Restart Android Studio
- Rebuild app
- Check USB debugging enabled
