# 📱 Android Performance Profiling Demo

Ứng dụng demo phân tích và tối ưu hóa hiệu năng Android, bao gồm các lỗi cố ý để học cách phát hiện và sửa chữa.

## 🎯 Mục đích

Đề tài tập trung vào kỹ năng phân tích, tìm kiếm và sửa chữa các vấn đề về hiệu năng:
- **Lag/Jank** - UI bị khựng, FPS thấp
- **High CPU Usage** - Ngốn CPU, máy nóng
- **Memory Leaks** - Rò rỉ bộ nhớ, crash OutOfMemoryError

## 🏗️ Cấu trúc Project

```
MobileGiuaKy/
├── app/                          # Android Application
│   └── src/main/java/com/example/mobilegiuaky/
│       ├── MainActivity.java      # Danh sách ảnh (RecyclerView)
│       ├── PhotoDetailActivity.java # Chi tiết ảnh
│       ├── adapter/
│       │   └── PhotoAdapter.java   # Adapter với lỗi jank
│       ├── api/
│       │   ├── ApiClient.java      # Retrofit client
│       │   └── ApiService.java     # API interface
│       ├── model/
│       │   └── Photo.java          # Model class
│       └── utils/
│           ├── LeakyManager.java   # ⚠️ Memory Leak Demo
│           ├── NonLeakyManager.java # ✅ Fixed version
│           ├── HeavyProcessor.java  # ⚠️ CPU Heavy Demo
│           ├── OptimizedProcessor.java # ✅ Fixed version
│           ├── ImageDownloader.java # ⚠️ Main Thread Demo
│           └── OptimizedImageDownloader.java # ✅ Fixed version
├── backend/                       # Node.js API Server
│   ├── server.js                  # Express server
│   ├── package.json
│   └── database_setup.sql         # MySQL setup
└── docs/                          # Documentation
```

## 🐛 Các Lỗi Cố Ý (Demo)

### A. Lag/Jank (UI Blocking)
**File:** `PhotoAdapter.java` - `bindViewHolderBad()`

```java
// ⚠️ BAD: Heavy operations in onBindViewHolder
- Heavy string processing on main thread
- Bubble sort O(n²) on main thread
- Download image synchronously (blocks UI)
- Pixel-by-pixel image processing
- Creating unnecessary objects in loops
```

**Cách phát hiện:**
- Android Profiler → CPU → Record trace
- Xem frame time > 16ms
- GPU Rendering profiling

### B. High CPU Usage
**File:** `HeavyProcessor.java`

```java
// ⚠️ BAD: Inefficient algorithms
- Bubble sort O(n²) thay vì Arrays.sort() O(n log n)
- Linear search O(n) thay vì binary search O(log n)
- Repeated string operations
- Heavy computation on main thread
```

**Cách phát hiện:**
- Android Profiler → CPU
- Biểu đồ CPU usage
- Battery profiler

### C. Memory Leaks
**File:** `LeakyManager.java`

```java
// ⚠️ BAD: Memory leak patterns
- Singleton holds Activity Context
- Unregistered listeners/callbacks
- Handler with delayed Runnable
- Inner classes holding outer reference
```

**Cách phát hiện:**
- LeakCanary (tự động)
- Android Profiler → Memory → Heap dump
- Memory tăng dần không giảm

## 📱 Cách Chạy

### 1. Backend Server
```bash
cd backend
npm install
npm start
```
Server chạy tại `http://localhost:3000`

### 2. Android App
1. Mở project trong Android Studio
2. Chạy trên Emulator hoặc thiết bị thật
3. Đảm bảo đã bật LeakCanary (debug build)

### 3. Cấu hình API URL
Trong `ApiClient.java`, thay đổi BASE_URL:
- Emulator: `http://10.0.2.2:3000/`
- Real device: `http://192.168.x.x:3000/`

## 🔧 Cách Demo

### Demo Jank/Lag
1. Bật "BAD Mode" switch
2. Cuộn danh sách ảnh
3. Quan sát FPS trong Android Profiler
4. Tắt "BAD Mode" và so sánh

### Demo CPU High
1. Nhấn nút "Stress CPU" trong BAD mode
2. Xem CPU spike trong Profiler
3. So sánh với GOOD mode

### Demo Memory Leak
1. Mở PhotoDetailActivity
2. Bật "Memory Leak Mode"
3. Nhấn "Cause Memory Leak"
4. Nhấn Back, lặp lại 3-4 lần
5. Chờ LeakCanary notification
6. Xem Memory trong Profiler không giảm

## 📊 Công cụ Phân tích

### Android Studio Profiler
- **CPU Profiler:** Phát hiện bottlenecks
- **Memory Profiler:** Theo dõi heap, phát hiện leaks
- **Network Profiler:** Theo dõi requests
- **Energy Profiler:** Đo battery usage

### LeakCanary
```gradle
debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
```
Tự động phát hiện memory leaks trong debug builds.

### Systrace
```bash
python systrace.py --time=10 -o trace.html gfx view
```

## ✅ Best Practices (Cách Sửa)

### Sửa Jank
- Dùng Glide/Picasso cho image loading
- ViewHolder pattern đúng cách
- Không block main thread
- DiffUtil cho RecyclerView updates

### Sửa CPU High
- Dùng efficient algorithms (Arrays.sort)
- Background threads cho heavy work
- Caching kết quả
- Lazy loading

### Sửa Memory Leaks
- Application Context cho Singletons
- WeakReference cho callbacks
- Cleanup trong onDestroy()
- Avoid static references to Activity

## 📝 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/photos` | Lấy danh sách ảnh |
| GET | `/api/photos/:id` | Lấy chi tiết ảnh |
| POST | `/api/photos` | Thêm ảnh mới |
| DELETE | `/api/photos/:id` | Xóa ảnh |

## 📚 Tài liệu tham khảo

- [Android Performance](https://developer.android.com/topic/performance)
- [LeakCanary](https://square.github.io/leakcanary/)
- [Android Profiler](https://developer.android.com/studio/profile)

## 👨‍💻 Author

Mobile Development - Performance Profiling Demo
