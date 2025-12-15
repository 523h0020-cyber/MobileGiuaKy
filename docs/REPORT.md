# 📊 Báo cáo Phân tích Hiệu năng Android
## Photo Gallery App - Performance Profiling Demo

---

# CHAPTER 04: DEMO - PERFORMANCE ISSUES IMPLEMENTATION
## (Triển khai các vấn đề hiệu năng vào Source Code)

> **Người thực hiện:** Developer (Người 2)  
> **Mục tiêu:** Giải thích chi tiết cách "cài cắm" lỗi vào mã nguồn và lý do tại sao những đoạn code này gây ra vấn đề hiệu năng.

---

## 4.1 Tổng quan kiến trúc Source Code

### 4.1.1 Cấu trúc thư mục chính
```
app/src/main/java/com/example/mobilegiuaky/
├── MainActivity.java              # Màn hình danh sách ảnh + nút Admin
├── AdminActivity.java             # ⭐ Quản lý ảnh (POST/DELETE) - Test performance
├── PhotoDetailActivity.java       # Màn hình chi tiết (Memory Leak Demo)
├── adapter/
│   └── PhotoAdapter.java          # ⚠️ JANK DEMO - RecyclerView Adapter
├── api/
│   ├── ApiClient.java             # Retrofit client
│   └── ApiService.java            # API endpoints (GET/POST/DELETE)
├── utils/
│   ├── HeavyProcessor.java        # ⚠️ CPU DEMO - Thuật toán kém hiệu quả
│   ├── LeakyManager.java          # ⚠️ MEMORY LEAK DEMO - Singleton rò rỉ
│   ├── NonLeakyManager.java       # ✅ Phiên bản đã sửa
│   └── ImageDownloader.java       # Download ảnh (Bad/Good)
└── model/
    └── Photo.java                 # Data class

backend/
├── server.js                      # Node.js Express server
├── database_setup.sql             # MySQL schema
└── public/images/                 # ⭐ Static images folder
```

### 4.1.2 Tính năng Admin Panel (Performance Testing)
**File:** `AdminActivity.java`

Admin Panel được thiết kế để test performance của app trong các tình huống thực tế:

```java
// Thêm nhiều ảnh cùng lúc để test UI performance
public void createMultiplePhotos(int count) {
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < count; i++) {
        Photo photo = new Photo(0, "Demo Photo " + i, 
                               "Performance test photo",
                               imageUrl, "", 0);
        // POST to API...
    }
    
    long apiTime = System.currentTimeMillis() - startTime;
    updateList();  // ⚠️ Có thể gây Jank nếu BAD Mode
    Log.d(TAG, "Added " + count + " photos in " + apiTime + "ms");
}
```

**Mục đích:**
- Bulk add 1-50 ảnh → Test RecyclerView update performance
- Bulk delete → Test memory cleanup
- BAD/GOOD mode toggle → So sánh hiệu năng

**Cách truy cập:** Từ MainActivity, nhấn nút **🔐 Admin**

---

### 4.1.3 Cơ chế Toggle Bad/Good Mode
Ứng dụng sử dụng biến `useBadImplementation` để chuyển đổi giữa 2 chế độ:

```java
// Trong PhotoAdapter.java
private boolean useBadImplementation = true;

public void setUseBadImplementation(boolean useBad) {
    this.useBadImplementation = useBad;
}

@Override
public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
    if (useBadImplementation) {
        bindViewHolderBad(holder, photo);   // ⚠️ Gây Jank
    } else {
        bindViewHolderGood(holder, photo);  // ✅ Mượt mà
    }
}
```

---

## 4.2 LỖI A: JANK/LAG (UI Blocking)

### 4.2.1 Vị trí cài cắm lỗi
- **File:** `adapter/PhotoAdapter.java`
- **Method:** `bindViewHolderBad()`
- **Dòng:** 94-131

### 4.2.2 Nguyên lý gây lỗi
Android UI hoạt động ở **60 FPS**, nghĩa là mỗi frame chỉ có **16.67ms** để render. Nếu `onBindViewHolder()` mất hơn 16ms, frame sẽ bị **drop** → người dùng thấy UI **giật/khựng**.

### 4.2.3 Code gây lỗi (5 BUGs)

#### 🐛 BUG 1: Heavy String Processing trên Main Thread
```java
// File: PhotoAdapter.java - Line 102-103
// ⚠️ BUG 1: Heavy string processing on MAIN THREAD
String processedTitle = HeavyProcessor.heavyStringProcessing(photo.getTitle(), 50);
Log.d(TAG, "Processed title length: " + processedTitle.length());
```

**Tại sao sai?**
- Method `heavyStringProcessing()` chạy 50 iterations với các thao tác `append()`, `reverse()`, `toUpperCase()`, `toLowerCase()`
- Tạo ra hàng trăm đối tượng String mới trong mỗi lần bind
- Thời gian thực thi: **~15-30ms mỗi item**

**Code chi tiết của HeavyProcessor:**
```java
// File: HeavyProcessor.java - Line 43-58
public static String heavyStringProcessing(String input, int iterations) {
    StringBuilder result = new StringBuilder(input);
    
    for (int i = 0; i < iterations; i++) {
        // ⚠️ BAD: Multiple string operations
        result.append(input);
        result.reverse();                              // O(n) mỗi lần
        result.append(String.valueOf(i));
        
        // ⚠️ EXTRA BAD: Creating new objects in loop
        String temp = result.toString()                // Tạo String mới
                           .toUpperCase()              // Tạo String mới  
                           .toLowerCase();             // Tạo String mới
        result = new StringBuilder(temp);              // Tạo StringBuilder mới
    }
    return result.toString();
}
```

---

#### 🐛 BUG 2: Bubble Sort O(n²) trên Main Thread
```java
// File: PhotoAdapter.java - Line 105-108
// ⚠️ BUG 2: Sorting array on MAIN THREAD in onBindViewHolder
int[] randomData = HeavyProcessor.generateLargeDataset(500);
int[] sortedData = HeavyProcessor.inefficientSort(randomData);
Log.d(TAG, "Sorted data, first element: " + sortedData[0]);
```

**Tại sao sai?**
- **Bubble Sort** có độ phức tạp **O(n²)** = 500 × 500 = **250,000 phép so sánh**
- Còn thêm phép tính toán vô nghĩa `Math.sin() × Math.cos()` trong mỗi vòng lặp
- Thời gian thực thi: **~50-100ms mỗi item**

**Code chi tiết:**
```java
// File: HeavyProcessor.java - Line 20-40
public static int[] inefficientSort(int[] array) {
    int n = array.length;
    int[] result = array.clone();
    
    // Bubble sort - O(n²) - very inefficient for large arrays
    for (int i = 0; i < n - 1; i++) {
        for (int j = 0; j < n - i - 1; j++) {
            if (result[j] > result[j + 1]) {
                // Swap
                int temp = result[j];
                result[j] = result[j + 1];
                result[j + 1] = temp;
            }
            
            // ⚠️ EXTRA BAD: Unnecessary computation inside loop
            double wastedComputation = Math.sin(Math.random()) 
                                     * Math.cos(Math.random());
        }
    }
    return result;
}
```

---

#### 🐛 BUG 3: Download Image đồng bộ trên Main Thread
```java
// File: PhotoAdapter.java - Line 110-125
// ⚠️ BUG 3: Download image on MAIN THREAD (blocks UI)
try {
    if (photo.getImageUrl() != null && !photo.getImageUrl().isEmpty()) {
        Bitmap bitmap = downloadImageSync(photo.getImageUrl());  // ⚠️ BLOCKING!
        // ...
    }
} catch (Exception e) {
    // ...
}
```

**Tại sao sai?**
- Network I/O **KHÔNG BAO GIỜ** được thực hiện trên Main Thread
- Từ Android 3.0+, điều này gây ra `NetworkOnMainThreadException`
- Nếu bypass được, UI sẽ **đóng băng hoàn toàn** trong 1-5 giây

**Code chi tiết download đồng bộ:**
```java
// File: PhotoAdapter.java - Line 152-170
private Bitmap downloadImageSync(String imageUrl) {
    try {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setConnectTimeout(5000);    // Chờ tối đa 5 giây
        connection.setReadTimeout(5000);       // Chờ tối đa 5 giây
        connection.connect();                  // ⚠️ BLOCKING CALL!
        
        InputStream input = connection.getInputStream();
        Bitmap bitmap = BitmapFactory.decodeStream(input);  // ⚠️ BLOCKING!
        input.close();
        connection.disconnect();
        
        return bitmap;
    } catch (Exception e) {
        return null;
    }
}
```

---

#### 🐛 BUG 4: Xử lý ảnh Pixel-by-Pixel trên Main Thread
```java
// File: PhotoAdapter.java - Line 117-118
// ⚠️ BUG 4: Heavy image processing on main thread
Bitmap processed = HeavyProcessor.heavyImageProcessing(bitmap);
holder.ivPhoto.setImageBitmap(processed != null ? processed : bitmap);
```

**Tại sao sai?**
- Duyệt từng pixel của ảnh (ví dụ 1000×1000 = **1 triệu pixel**)
- Mỗi pixel thực hiện `getPixel()`, tính toán, `setPixel()` 
- Thời gian thực thi: **~200-500ms cho ảnh HD**

**Code chi tiết:**
```java
// File: HeavyProcessor.java - Line 107-138
public static Bitmap heavyImageProcessing(Bitmap original) {
    int width = original.getWidth();
    int height = original.getHeight();
    
    Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);
    
    // ⚠️ BAD: Pixel-by-pixel manipulation - very slow
    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
            int pixel = result.getPixel(x, y);           // Đọc pixel
            
            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);
            int gray = (red + green + blue) / 3;
            
            // ⚠️ EXTRA BAD: Unnecessary math in pixel loop
            double brightness = Math.pow(gray / 255.0, 0.8) * 255;
            gray = (int) brightness;
            
            result.setPixel(x, y, Color.rgb(gray, gray, gray));  // Ghi pixel
        }
    }
    return result;
}
```

---

#### 🐛 BUG 5: Tạo Object thừa trong vòng lặp
```java
// File: PhotoAdapter.java - Line 127-130
// ⚠️ BUG 5: Creating new objects unnecessarily
for (int i = 0; i < 100; i++) {
    String waste = new String("Wasted memory " + i);  // 100 String mới
    StringBuilder sb = new StringBuilder(waste);       // 100 StringBuilder mới
}
```

**Tại sao sai?**
- Mỗi lần bind tạo ra **200 object vô dụng**
- Gây áp lực lên **Garbage Collector**
- GC chạy → App bị pause → Jank

---

### 4.2.4 Code đúng (Good Implementation)
```java
// File: PhotoAdapter.java - Line 136-150
private void bindViewHolderGood(PhotoViewHolder holder, Photo photo) {
    // Set text directly (no heavy processing)
    holder.tvTitle.setText(photo.getTitle());
    holder.tvDescription.setText(photo.getDescription());
    holder.tvFileSize.setText(photo.getFileSizeKb() + " KB");
    
    // ✅ Use Glide for efficient image loading
    Glide.with(context)
            .load(photo.getImageUrl())
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .centerCrop()
            .into(holder.ivPhoto);
}
```

**Tại sao đúng?**
- Không xử lý nặng, chỉ setText đơn giản
- Glide tự động: download trên background thread, cache, decode hiệu quả
- Thời gian bind: **< 1ms**

---

## 4.3 LỖI B: HIGH CPU USAGE

### 4.3.1 Vị trí cài cắm lỗi
- **File:** `utils/HeavyProcessor.java`
- **File:** `MainActivity.java` (nút Stress CPU)

### 4.3.2 Nguyên lý gây lỗi
Sử dụng thuật toán có **độ phức tạp cao** (O(n²), O(n³)) khiến CPU phải tính toán nhiều, gây:
- Hao pin nhanh
- Thiết bị nóng lên
- Các app khác bị chậm

### 4.3.3 Code gây lỗi

#### 🐛 Bubble Sort thay vì Arrays.sort()
```java
// File: HeavyProcessor.java - Line 20-40
// ⚠️ BAD: Inefficient bubble sort on large dataset - O(n²)
public static int[] inefficientSort(int[] array) {
    int n = array.length;
    int[] result = array.clone();
    
    for (int i = 0; i < n - 1; i++) {
        for (int j = 0; j < n - i - 1; j++) {
            if (result[j] > result[j + 1]) {
                int temp = result[j];
                result[j] = result[j + 1];
                result[j + 1] = temp;
            }
            // ⚠️ Phép tính thừa gây ngốn CPU thêm
            double wastedComputation = Math.sin(Math.random()) 
                                     * Math.cos(Math.random());
        }
    }
    return result;
}
```

| Dataset Size | Bubble Sort O(n²) | Arrays.sort() O(n log n) |
|--------------|-------------------|--------------------------|
| 500 items    | ~100ms            | ~1ms                     |
| 2000 items   | ~2500ms           | ~5ms                     |
| 10000 items  | ~60000ms          | ~15ms                    |

#### 🐛 Linear Search lặp lại nhiều lần
```java
// File: HeavyProcessor.java - Line 63-82
public static int inefficientSearch(String query, String[] items) {
    int matchCount = 0;
    
    for (int repeat = 0; repeat < 100; repeat++) {  // ⚠️ Lặp vô nghĩa 100 lần!
        for (int i = 0; i < items.length; i++) {
            String lowerItem = items[i].toLowerCase();   // Tạo String mới
            String lowerQuery = query.toLowerCase();      // Tạo String mới (mỗi vòng!)
            
            if (lowerItem.contains(lowerQuery) || 
                lowerItem.startsWith(lowerQuery) ||
                lowerItem.endsWith(lowerQuery) ||
                calculateLevenshteinDistance(lowerItem, lowerQuery) < 3) {
                matchCount++;
            }
        }
    }
    return matchCount;
}
```

**Tại sao sai?**
- Lặp lại search 100 lần vô nghĩa
- `toLowerCase()` được gọi lại mỗi vòng (thay vì cache)
- `calculateLevenshteinDistance()` có độ phức tạp O(m×n)

### 4.3.4 Code đúng
```java
// ✅ GOOD: Sử dụng thuật toán hiệu quả
import java.util.Arrays;

public static int[] efficientSort(int[] array) {
    int[] result = array.clone();
    Arrays.sort(result);  // O(n log n) - Dual-Pivot Quicksort
    return result;
}

// ✅ GOOD: Cache kết quả toLowerCase
public static int efficientSearch(String query, String[] items) {
    String lowerQuery = query.toLowerCase();  // Chỉ gọi 1 lần
    int matchCount = 0;
    
    for (String item : items) {
        if (item.toLowerCase().contains(lowerQuery)) {
            matchCount++;
        }
    }
    return matchCount;
}
```

---

## 4.4 LỖI C: MEMORY LEAK

### 4.4.1 Vị trí cài cắm lỗi
- **File:** `utils/LeakyManager.java` (Singleton gây rò rỉ)
- **File:** `PhotoDetailActivity.java` (Activity bị leak)

### 4.4.2 Nguyên lý gây lỗi
**Memory Leak** xảy ra khi một object không còn được sử dụng nhưng vẫn có reference trỏ đến, khiến **Garbage Collector không thể thu hồi**.

**Pattern thường gặp:**
- Singleton giữ Activity Context
- Static reference đến View/Activity
- Handler với delayed Runnable
- Listener/Callback không được unregister

### 4.4.3 Code gây lỗi

#### 🐛 LEAK 1: Singleton giữ Activity Context
```java
// File: LeakyManager.java - Line 18-48
public class LeakyManager {
    
    private static LeakyManager instance;       // ⚠️ Static - sống mãi với app
    
    // ⚠️ BAD: Holding Activity Context in Singleton
    private Context context;
    
    public static LeakyManager getInstance() {
        if (instance == null) {
            instance = new LeakyManager();
        }
        return instance;
    }
    
    /**
     * ⚠️ BUG: This method stores Activity Context in singleton
     * The Activity will never be garbage collected!
     */
    public void init(Context context) {
        // ⚠️ BAD: Should use context.getApplicationContext() instead
        this.context = context;  // Giữ reference đến Activity!
    }
}
```

**Tại sao sai?**
```
[App Start]
    ↓
LeakyManager.instance (static, sống mãi)
    ↓
LeakyManager.context → PhotoDetailActivity #1
    
[User nhấn Back - Activity #1 bị destroy]
    ↓
GC muốn thu hồi Activity #1
    ↓
NHƯNG LeakyManager.context vẫn giữ reference!
    ↓
Activity #1 KHÔNG THỂ bị thu hồi → LEAK!

[User mở lại PhotoDetail - Activity #2 được tạo]
    ↓
LeakyManager.context → PhotoDetailActivity #2
    ↓
Activity #1 vẫn còn trong memory (leaked)
Activity #2 cũng sẽ bị leak khi Back...
```

---

#### 🐛 LEAK 2: Listener không được unregister
```java
// File: LeakyManager.java - Line 50-53
// ⚠️ BAD: Callback holding reference to Activity
private OnDataLoadedListener listener;

public void setOnDataLoadedListener(OnDataLoadedListener listener) {
    this.listener = listener;  // ⚠️ Giữ reference đến Activity (implement interface)
}
```

**Trong PhotoDetailActivity:**
```java
// File: PhotoDetailActivity.java - Line 35
public class PhotoDetailActivity extends AppCompatActivity 
        implements LeakyManager.OnDataLoadedListener {  // ⚠️ Activity implement listener
    
    private void causeMemoryLeak() {
        // ⚠️ LEAK 2: Listener registration without cleanup
        LeakyManager.getInstance().setOnDataLoadedListener(this);  // this = Activity
    }
}
```

**Chuỗi reference:**
```
LeakyManager (static singleton)
    → listener (OnDataLoadedListener)
        → PhotoDetailActivity (this)
            → ivPhotoLarge, tvTitle, btnDownload... (tất cả View)
            → Handler, Bitmap, ... (tất cả field)
```

---

#### 🐛 LEAK 3: Handler với Delayed Runnable
```java
// File: LeakyManager.java - Line 55-67
private Handler handler;

public void loadDataWithDelay() {
    handler.postDelayed(new Runnable() {
        @Override
        public void run() {
            if (listener != null) {
                listener.onDataLoaded("Data loaded!");  // ⚠️ Gọi sau 5 giây
            }
        }
    }, 5000);  // 5 giây delay
}
```

**Tại sao sai?**
- Runnable được schedule chạy sau 5 giây
- Nếu user nhấn Back trong 5 giây đó → Activity bị destroy
- Nhưng Runnable vẫn giữ reference đến listener (Activity)
- → Activity không thể GC trong ít nhất 5 giây
- → Nếu Runnable gọi `listener.onDataLoaded()` trên destroyed Activity → Crash hoặc undefined behavior

---

### 4.4.4 Code gọi gây Leak (PhotoDetailActivity)
```java
// File: PhotoDetailActivity.java - Line 147-161
private void causeMemoryLeak() {
    // ⚠️ LEAK 1: Singleton holds Activity Context
    LeakyManager.getInstance().init(this);  // Should use getApplicationContext()
    
    // ⚠️ LEAK 2: Listener registration without cleanup
    LeakyManager.getInstance().setOnDataLoadedListener(this);
    
    // ⚠️ LEAK 3: Delayed callback that may execute after Activity destroyed
    LeakyManager.getInstance().loadDataWithDelay();
    
    Log.w(TAG, "⚠️ MEMORY LEAK CAUSED!");
}
```

### 4.4.5 onDestroy KHÔNG cleanup (Cố ý)
```java
// File: PhotoDetailActivity.java - Line 245-257
@Override
protected void onDestroy() {
    super.onDestroy();
    
    // ⚠️ BUG: We intentionally DON'T cleanup to demonstrate leak
    if (!leakModeEnabled) {
        LeakyManager.getInstance().cleanup();  // Chỉ cleanup khi tắt Leak Mode
        Log.d(TAG, "✅ Cleaned up LeakyManager");
    } else {
        Log.w(TAG, "⚠️ NOT cleaning up - Memory Leak will occur!");
    }
}
```

### 4.4.6 Code đúng (NonLeakyManager pattern)
```java
// ✅ GOOD: Sử dụng Application Context
public void init(Context context) {
    this.context = context.getApplicationContext();  // Application sống mãi - OK!
}

// ✅ GOOD: Sử dụng WeakReference cho callback
private WeakReference<OnDataLoadedListener> listenerRef;

public void setOnDataLoadedListener(OnDataLoadedListener listener) {
    this.listenerRef = new WeakReference<>(listener);
}

// ✅ GOOD: Cleanup method
public void cleanup() {
    this.context = null;
    this.listenerRef = null;
    handler.removeCallbacksAndMessages(null);  // Cancel pending runnables
}

// ✅ GOOD: Gọi cleanup trong onDestroy
@Override
protected void onDestroy() {
    super.onDestroy();
    LeakyManager.getInstance().cleanup();
}
```

---

## 4.5 Bảng tổng hợp các lỗi

| Loại lỗi | File | Method/Line | Nguyên nhân | Hậu quả |
|----------|------|-------------|-------------|---------|
| **JANK** | PhotoAdapter.java | `bindViewHolderBad()` L94-131 | Xử lý nặng trên Main Thread | FPS < 30, UI khựng |
| **JANK** | HeavyProcessor.java | `heavyStringProcessing()` L43-58 | 50 iterations string ops | ~20ms/call |
| **JANK** | HeavyProcessor.java | `heavyImageProcessing()` L107-138 | Pixel-by-pixel processing | ~300ms/image |
| **CPU** | HeavyProcessor.java | `inefficientSort()` L20-40 | Bubble Sort O(n²) | 2500ms cho 2000 items |
| **CPU** | HeavyProcessor.java | `inefficientSearch()` L63-82 | Linear search ×100 lần | CPU spike 100% |
| **LEAK** | LeakyManager.java | `init(context)` L44-48 | Singleton giữ Activity Context | +10MB mỗi lần mở Activity |
| **LEAK** | LeakyManager.java | `setOnDataLoadedListener()` L50-53 | Listener không cleanup | Activity không GC được |
| **LEAK** | LeakyManager.java | `loadDataWithDelay()` L55-67 | Handler delayed runnable | Memory giữ 5+ giây |

---

# CHAPTER 09: APPENDIX (Phụ lục)
## Hướng dẫn cài đặt môi trường và chạy Source Code

---

## 9.1 Yêu cầu hệ thống

### 9.1.1 Phần cứng tối thiểu
| Thành phần | Yêu cầu |
|------------|---------|
| RAM | 8GB (khuyến nghị 16GB) |
| Ổ cứng | 10GB trống (SSD khuyến nghị) |
| CPU | Intel i5 hoặc AMD Ryzen 5 trở lên |

### 9.1.2 Phần mềm cần cài đặt
| Phần mềm | Phiên bản | Link download |
|----------|-----------|---------------|
| Android Studio | Hedgehog 2023.1.1+ | https://developer.android.com/studio |
| Node.js | 18.x LTS | https://nodejs.org/ |
| MySQL | 8.0+ | https://dev.mysql.com/downloads/ |
| Git | Latest | https://git-scm.com/ |

---

## 9.2 Cài đặt Backend Server

### 9.2.1 Clone repository
```bash
git clone <repository-url>
cd MobileGiuaKy
```

### 9.2.2 Cài đặt dependencies
```bash
cd backend
npm install
```

### 9.2.3 Cấu hình MySQL
1. Mở MySQL Workbench hoặc command line
2. Chạy script tạo database:
```bash
mysql -u root -p < database_setup.sql
```

Hoặc copy nội dung `database_setup.sql` vào MySQL Workbench và Execute.

### 9.2.4 Cấu hình kết nối database
Mở file `backend/server.js`, sửa thông tin kết nối:
```javascript
const dbConfig = {
    host: 'localhost',
    user: 'root',              // Thay bằng username MySQL của bạn
    password: '',              // Thay bằng password MySQL của bạn
    database: 'heavy_gallery_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
};
```

### 9.2.5 Setup Static Images (Cho demo trên thiết bị thật)
**Backend đã được cấu hình để serve static images từ folder `public/images/`**

1. Copy ảnh từ máy tính vào folder:
```bash
backend/public/images/photo1.jpg
backend/public/images/photo2.jpg
...
```

2. Ảnh sẽ được serve tại URL:
- Emulator: `http://10.0.2.2:3000/images/photo1.jpg`
- Real Device: `http://192.168.x.x:3000/images/photo1.jpg`

**Xem chi tiết:** [backend/public/images/SETUP_GUIDE.md](../backend/public/images/SETUP_GUIDE.md)

---

### 9.2.6 Khởi động server
```bash
npm start
```

**Kết quả mong đợi:**
```
✅ Connected to MySQL database
✅ Database connection test successful
🚀 Server running on port 3000
📍 API available at: http://localhost:3000/api/photos
📸 Static images: http://localhost:3000/images/
```

### 9.2.7 Kiểm tra API
Mở browser hoặc Postman:
```
GET http://localhost:3000/api/photos
GET http://localhost:3000/images/photo1.jpg  (nếu đã copy ảnh)
```

---

## 9.3 Cài đặt Android App

### 9.3.1 Mở project trong Android Studio
1. Mở Android Studio
2. Chọn **File → Open**
3. Navigate đến thư mục `MobileGiuaKy`
4. Chờ Gradle sync hoàn tất

### 9.3.2 Cấu hình API URL
Mở file `app/src/main/java/com/example/mobilegiuaky/api/ApiClient.java`:

**Nếu chạy trên Emulator:**
```java
private static final String BASE_URL = "http://10.0.2.2:3000/";
```

**Nếu chạy trên thiết bị thật:**
1. Tìm IP máy tính: `ipconfig` (Windows) hoặc `ifconfig` (Mac/Linux)
2. Thay đổi BASE_URL:
```java
private static final String BASE_URL = "http://192.168.x.x:3000/";
```

### 9.3.3 Cấu hình Network Security (Android 9+)
File `app/src/main/res/xml/network_security_config.xml` đã được cấu hình sẵn cho phép HTTP:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">192.168.1.1</domain>
        <!-- Thêm IP của bạn nếu cần -->
    </domain-config>
</network-security-config>
```

### 9.3.4 Build và Run
1. Kết nối thiết bị hoặc khởi động Emulator
2. Nhấn **Run → Run 'app'** hoặc `Shift + F10`
3. Chờ build và cài đặt

---

## 9.4 Cài đặt công cụ Profiling

### 9.4.1 LeakCanary (đã tích hợp sẵn)
LeakCanary đã được thêm trong `build.gradle`:
```gradle
dependencies {
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
}
```

LeakCanary tự động chạy trong **Debug build** và sẽ hiện notification khi phát hiện leak.

### 9.4.2 Android Profiler
1. Run app ở chế độ **Debug** (không phải Release)
2. Trong Android Studio: **View → Tool Windows → Profiler**
3. Chọn device và process đang chạy

### 9.4.3 GPU Rendering Profile
Trên thiết bị/Emulator:
1. **Settings → Developer Options**
2. **Profile GPU rendering → On screen as bars**
3. Thanh vượt đường xanh = frame bị drop

---

## 9.5 Kịch bản Demo (Chi tiết)

### 9.5.0 Chuẩn bị môi trường
```
✅ Checklist trước khi demo:
1. Backend đang chạy: npm start (Port 3000)
2. Đã copy ảnh vào backend/public/images/ (nếu demo trên điện thoại thật)
3. Android Studio Profiler đã mở: View → Tool Windows → Profiler
4. App đã build và cài đặt trên thiết bị/emulator
5. Kiểm tra API: curl http://localhost:3000/api/photos
```

---

### 9.5.1 Demo Jank/Lag (Scenario A: Cuộn danh sách)
```
📱 BƯỚC 1: Mở app → MainActivity hiển thị danh sách ảnh

🔧 BƯỚC 2: Mở Android Profiler
   - Android Studio: View → Tool Windows → Profiler
   - Chọn device và process: com.example.mobilegiuaky
   - Chuyển sang CPU tab

⚠️ BƯỚC 3: Bật BAD MODE
   - Trong app, đảm bảo switch "BAD MODE" đang BẬT (ON)
   - Switch sẽ hiển thị màu accent color khi ON

🎬 BƯỚC 4: Cuộn danh sách ảnh nhanh (swipe lên xuống)
   Quan sát:
   ✗ UI bị khựng, giật (Jank visible)
   ✗ Profiler hiển thị nhiều "Janky frames" (màu đỏ/cam)
   ✗ Frame time > 16ms (vượt đường xanh)
   ✗ FPS < 30 (thay vì 60)
   
   Logcat sẽ hiển thị:
   "D/PhotoAdapter: Processed title length: 2550" (heavy processing)
   "D/PhotoAdapter: Sorted array length: 100" (bubble sort)

✅ BƯỚC 5: TẮT BAD MODE
   - Toggle switch sang OFF
   - RecyclerView sẽ tự động refresh

🎬 BƯỚC 6: Cuộn lại danh sách
   Quan sát:
   ✓ UI mượt mà, không giật
   ✓ Frame time < 16ms
   ✓ FPS = 60
   ✓ Profiler không hiển thị red frames

📊 BƯỚC 7: So sánh kết quả
   | Metric        | BAD Mode | GOOD Mode |
   |---------------|----------|----------|
   | Frame Time    | 45ms     | 8ms      |
   | FPS           | ~22      | ~60      |
   | Jank Visible  | Yes ✗    | No ✓     |
```

**💡 Giải thích cho audience:**
- BAD Mode: `onBindViewHolder()` chạy heavy operations (string processing 50 iterations, bubble sort, pixel processing) trên Main Thread
- GOOD Mode: Chỉ bind data đơn giản, images load bằng Glide (background thread)
- **Ngưỡng vàng: 16.67ms/frame** → Vượt = drop frame = Jank

---

### 9.5.2 Demo Jank/Lag (Scenario B: Bulk Add với Admin)
```
📱 BƯỚC 1: Từ MainActivity, nhấn nút "🔐 Admin" (góc trên bên phải)
   → Mở AdminActivity

⚠️ BƯỚC 2: Bật BAD MODE trong AdminActivity
   - Switch "BAD MODE" ở đầu màn hình

🔧 BƯỚC 3: Mở Android Profiler (nếu chưa mở)

🎬 BƯỚC 4: Nhấn nút "Add Multiple"
   - Dialog xuất hiện: "How many photos to create?"
   - Nhập: **50** (hoặc 20-50 tùy thiết bị)
   - Nhấn "Create"

⏱️ BƯỚC 5: Quan sát quá trình add
   ✗ UI freeze/đơ trong ~5-10 giây (BAD Mode)
   ✗ ProgressBar hiển thị nhưng không update smooth
   ✗ Profiler: CPU spike ~100%, nhiều red frames
   
   Logcat:
   "D/AdminActivity: API time: 5234ms"
   "D/AdminActivity: UI update time (BAD): 8932ms" ← Rất chậm!

✅ BƯỚC 6: TẮT BAD MODE

🎬 BƯỚC 7: Nhấn "Delete All" để xóa ảnh vừa thêm
   - Xác nhận xóa

🎬 BƯỚC 8: Nhấn lại "Add Multiple" → Nhập **50**
   
   Quan sát:
   ✓ UI mượt mà, không freeze
   ✓ ProgressBar update smooth
   ✓ Profiler: CPU ~40%, không có red frames
   
   Logcat:
   "D/AdminActivity: API time: 5156ms" (tương tự BAD)
   "D/AdminActivity: UI update time (GOOD): 245ms" ← Nhanh hơn 36x!

📊 BƯỚC 9: Giải thích sự khác biệt
   - **BAD Mode**: `notifyDataSetChanged()` → Rebind ALL items → Trigger heavy processing mỗi item
   - **GOOD Mode**: `DiffUtil` hoặc `notifyItemRangeInserted()` → Chỉ bind items mới
```

---

### 9.5.3 Demo High CPU
```
📱 BƯỚC 1: Quay lại MainActivity (từ Admin nhấn Back)

⚠️ BƯỚC 2: Bật BAD MODE

🔧 BƯỚC 3: Mở Android Profiler → CPU tab

🎬 BƯỚC 4: Nhấn nút "STRESS CPU" (hoặc scroll để trigger sort)
   
   Quan sát:
   ✗ CPU spike lên ~90-100%
   ✗ App không phản hồi trong ~3-5 giây
   ✗ Thiết bị có thể nóng lên (nếu lặp lại nhiều lần)
   ✗ Dialog "App not responding" có thể xuất hiện
   
   Logcat:
   "D/HeavyProcessor: Bubble sort of 2000 items took 2543ms"
   "D/HeavyProcessor: Inefficient search took 1823ms"

✅ BƯỚC 5: TẮT BAD MODE

🎬 BƯỚC 6: Nhấn lại "STRESS CPU"
   
   Quan sát:
   ✓ CPU chỉ tăng nhẹ (~20-30%)
   ✓ Hoàn thành ngay lập tức (< 1 giây)
   ✓ App vẫn phản hồi mượt mà
   
   Logcat:
   "D/HeavyProcessor: Arrays.sort of 2000 items took 12ms" ← Nhanh hơn 200x!

📊 BƯỚC 7: So sánh
   | Operation         | BAD (O(n²))  | GOOD (O(n log n)) |
   |-------------------|--------------|-------------------|
   | Sort 2000 items   | 2543ms       | 12ms              |
   | Search 100 times  | 1823ms       | 45ms              |
   | CPU Usage         | 95%          | 25%               |
```

**💡 Giải thích:**
- BAD: Bubble Sort O(n²) + Repeated linear search + Expensive math operations
- GOOD: `Arrays.sort()` O(n log n) (Dual-Pivot Quicksort) + Cached toLowerCase()

---

### 9.5.4 Demo Memory Leak
```
📱 BƯỚC 1: Từ MainActivity, mở Android Profiler → Memory tab

📊 BƯỚC 2: Ghi nhận mức Memory ban đầu
   Ví dụ: **85MB** (baseline)

🎬 BƯỚC 3: Tap vào một ảnh bất kỳ → Mở PhotoDetailActivity

⚠️ BƯỚC 4: Bật LEAK MODE
   - Switch "LEAK MODE" trong PhotoDetailActivity

💣 BƯỚC 5: Nhấn nút "CAUSE MEMORY LEAK"
   Logcat hiển thị:
   "W/PhotoDetailActivity: ⚠️ MEMORY LEAK CAUSED!"
   "D/LeakyManager: Singleton initialized with Activity Context" ← Root cause

🔙 BƯỚC 6: Nhấn Back → Quay lại MainActivity

🔄 BƯỚC 7: Lặp lại BƯỚC 3-6 tổng cộng **5-10 lần**
   (Tap ảnh → Bật Leak Mode → Cause Leak → Back → Repeat)

🗑️ BƯỚC 8: Trong Profiler, nhấn "Force GC" (icon thùng rác)
   - Chờ GC hoàn tất (~2-3 giây)

📊 BƯỚC 9: Quan sát Memory
   ✗ Memory KHÔNG giảm (hoặc giảm rất ít)
   ✗ Ví dụ: **140MB** → Tăng 55MB so với baseline
   ✗ LeakCanary notification xuất hiện:
     "┬───
      │ GC Root: Global variable in LeakyManager.instance
      │
      ├─ LeakyManager.context
      │    Leaking: YES
      │
      ╰→ PhotoDetailActivity
           Leaking: YES (Activity destroyed but still in memory)"

✅ BƯỚC 10: Restart app → TẮT LEAK MODE

🔄 BƯỚC 11: Lặp lại BƯỚC 3-6 (nhưng KHÔNG bật Leak Mode)

🗑️ BƯỚC 12: Force GC

📊 BƯỚC 13: Quan sát
   ✓ Memory giảm về baseline (~85MB)
   ✓ KHÔNG có LeakCanary notification
   ✓ PhotoDetailActivity được GC thu hồi thành công

📊 BƯỚC 14: So sánh
   | Scenario       | After 5 Opens | After GC |
   |----------------|---------------|----------|
   | With Leak      | 140MB         | 138MB ✗  |
   | Without Leak   | 92MB          | 85MB ✓   |
   | Leaked Objects | 5 Activities  | 0        |
```

**💡 Giải thích root cause:**
1. `LeakyManager.getInstance().init(this)` → Singleton giữ Activity Context
2. Singleton có lifecycle = Application (sống mãi)
3. Activity bị destroy nhưng Singleton vẫn giữ reference
4. GC không thể thu hồi Activity → **Memory Leak**
5. Mỗi lần mở Activity = thêm 1 leaked object (~10MB)

**Fix:** Dùng `getApplicationContext()` thay vì `this`

---

### 9.5.5 Demo Real Device với Static Images
```
💻 BƯỚC 1: Chuẩn bị trên máy tính
   1. Copy 10-20 ảnh vào: backend/public/images/
      Ví dụ: photo1.jpg, photo2.jpg, ..., photo20.jpg
   2. Tìm IP máy tính: ipconfig (Windows)
      Ví dụ: 192.168.1.105
   3. Start backend: cd backend && npm start

📱 BƯỚC 2: Kết nối điện thoại
   - Kết nối điện thoại và máy tính cùng WiFi
   - Update BASE_URL trong ApiClient.java: http://192.168.1.105:3000/
   - Build và install app lên điện thoại

✅ BƯỚC 3: Test kết nối
   - Mở browser trên điện thoại
   - Truy cập: http://192.168.1.105:3000/images/photo1.jpg
   - Nếu thấy ảnh hiển thị → OK!

🔐 BƯỚC 4: Mở Admin Panel trong app

➕ BƯỚC 5: Nhấn "Add Photo"
   - Title: Demo Photo 1
   - Description: Test image from computer
   - Image URL: http://192.168.1.105:3000/images/photo1.jpg
   - Nhấn "Create"

🎉 BƯỚC 6: Quay lại MainActivity
   - Ảnh vừa thêm hiển thị trong danh sách
   - Ảnh được load từ máy tính qua WiFi
   - Có thể tap xem chi tiết

📚 BƯỚC 7: Bulk add để test performance
   - Quay lại Admin
   - Add Multiple → 20 photos
   - Quan sát thời gian load và UI performance
```

---

### 9.5.6 Tips cho Demo thành công
```
✅ DO:
- Prepare trước: Backend chạy, Profiler mở, ảnh đã copy
- Giải thích TỪ TỪ mỗi bước cho audience
- So sánh BAD vs GOOD ngay sau mỗi scenario
- Chụp screenshot Profiler graphs để backup
- Test trước ít nhất 1 lần để ensure mọi thứ hoạt động

❌ DON'T:
- Demo trên thiết bị yếu (RAM < 4GB) → Kết quả không rõ ràng
- Quên force GC trước khi demo Memory Leak
- Skip steps → Audience sẽ không hiểu
- Demo quá nhanh → Audience không kịp quan sát Profiler
```

---

## 9.6 Troubleshooting

### Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách sửa |
|-----|-------------|----------|
| `Connection refused` | Backend chưa chạy | Chạy `npm start` trong thư mục backend |
| `NetworkOnMainThreadException` | Gọi network trên main thread | Đây là lỗi cố ý trong demo, bật StrictMode để bypass |
| `Unable to resolve host` | Sai IP hoặc thiết bị không cùng mạng | Kiểm tra IP trong ApiClient.java |
| App crash với OOM | Demo leak thành công! | Clear app data và restart |
| LeakCanary không hiện | Build Release thay vì Debug | Chạy lại với Debug build |
| Gradle sync failed | Cache cũ | File → Invalidate Caches → Restart |

### Kiểm tra kết nối
```bash
# Từ terminal/cmd
curl http://localhost:3000/api/photos

# Từ ADB shell (thiết bị)
adb shell ping 10.0.2.2
```

---

## 9.7 Cấu trúc API Endpoints

| Method | Endpoint | Mô tả | Request Body |
|--------|----------|-------|--------------|
| GET | `/api/photos` | Lấy danh sách tất cả ảnh | - |
| GET | `/api/photos/:id` | Lấy chi tiết một ảnh | - |
| POST | `/api/photos` | Thêm ảnh mới | `{ title, description, imageUrl, fileName, fileSizeKb }` |
| DELETE | `/api/photos/:id` | Xóa ảnh | - |
| GET | `/health` | Kiểm tra server status | - |

---

## 9.8 Tài liệu tham khảo

- [Android Performance Documentation](https://developer.android.com/topic/performance)
- [LeakCanary Official Guide](https://square.github.io/leakcanary/)
- [Android Profiler Guide](https://developer.android.com/studio/profile)
- [RecyclerView Best Practices](https://developer.android.com/guide/topics/ui/layout/recyclerview)

---

# Các Chapter khác (Dành cho thành viên khác)

## 2. Các vấn đề hiệu năng (Tóm tắt)

### 2.1 Lag/Jank (UI Blocking)

#### Nguyên nhân
| Lỗi | Mô tả | File |
|-----|-------|------|
| Heavy onBindViewHolder | Xử lý nặng trong bind | PhotoAdapter.java |
| Main Thread Network | Download ảnh trên main thread | ImageDownloader.java |
| Inefficient Sort | Bubble sort O(n²) | HeavyProcessor.java |

#### Triệu chứng
- FPS < 60 (frame time > 16.67ms)
- UI khựng khi cuộn
- App đơ khi tải ảnh

#### Cách phát hiện
```
1. LeakCanary (auto)
2. Android Profiler → Memory → Heap Dump
3. MAT (Memory Analyzer Tool)
```

#### Giải pháp
```java
// ❌ BAD: Activity Context
LeakyManager.getInstance().init(this);

// ✅ GOOD: Application Context
NonLeakyManager.getInstance().init(getApplicationContext());

// ❌ BAD: Strong reference
listener = callback;

// ✅ GOOD: Weak reference
listenerRef = new WeakReference<>(callback);
```

---

## 3. Demo Scenarios

### 3.1 Demo Jank
```
1. Mở app, bật BAD Mode
2. Cuộn RecyclerView
3. Quan sát: UI khựng
4. Profiler: Frame time > 16ms
5. Tắt BAD Mode → Smooth scrolling
```

### 3.2 Demo CPU
```
1. Bật BAD Mode
2. Nhấn "Stress CPU"
3. Quan sát: CPU spike ~100%
4. Tắt BAD Mode → CPU ~20%
```

### 3.3 Demo Memory Leak
```
1. Mở PhotoDetailActivity
2. Bật Leak Mode
3. Nhấn "Cause Leak" → Back
4. Lặp lại 5 lần
5. LeakCanary notification xuất hiện
6. Memory không giảm sau GC
```

---

## 4. Kết quả đo lường

### 4.1 Scrolling Performance
| Mode | Avg Frame Time | FPS | Jank |
|------|----------------|-----|------|
| BAD | 45ms | ~22 | Yes |
| GOOD | 8ms | ~60 | No |

### 4.2 CPU Usage (Sort 2000 items)
| Mode | Time | CPU Peak |
|------|------|----------|
| BAD (Bubble) | 2500ms | 95% |
| GOOD (Arrays.sort) | 15ms | 25% |

### 4.3 Memory
| Scenario | Memory After 5 Leaks |
|----------|---------------------|
| With Leak | +50MB (không giảm) |
| Without Leak | Stable |

---

## 5. Best Practices

### 5.1 UI Thread
- ✅ Chỉ update UI
- ✅ Không network/file I/O
- ✅ Không heavy computation

### 5.2 RecyclerView
- ✅ ViewHolder pattern
- ✅ Glide/Picasso cho images
- ✅ DiffUtil cho updates

### 5.3 Memory
- ✅ Application Context cho Singleton
- ✅ WeakReference cho callbacks
- ✅ Cleanup trong onDestroy()

### 5.4 Algorithms
- ✅ Efficient algorithms
- ✅ Background threading
- ✅ Caching

---

## 6. Công cụ

| Tool | Mục đích |
|------|----------|
| Android Profiler | CPU, Memory, Network |
| LeakCanary | Memory Leaks |
| Systrace | System-wide tracing |
| GPU Rendering | Frame rendering |

---

## 7. Kết luận

Ứng dụng demo thành công:
1. ✅ Tạo lỗi Jank/Lag có thể phát hiện bằng Profiler
2. ✅ Tạo lỗi CPU High có thể đo lường
3. ✅ Tạo Memory Leak có thể phát hiện bằng LeakCanary
4. ✅ Cung cấp phiên bản tối ưu để so sánh

---

## 8. Mind Map

```
                    ┌─────────────────┐
                    │  ANDROID        │
                    │  PERFORMANCE    │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
   ┌────┴────┐         ┌─────┴─────┐        ┌────┴────┐
   │  JANK   │         │ CPU HIGH  │        │ MEMORY  │
   │  /LAG   │         │           │        │ LEAK    │
   └────┬────┘         └─────┬─────┘        └────┬────┘
        │                    │                   │
   ┌────┴────┐         ┌─────┴─────┐       ┌────┴────┐
   │Causes:  │         │Causes:    │       │Causes:  │
   │-Main    │         │-O(n²) sort│       │-Static  │
   │ thread  │         │-Repeated  │       │ context │
   │ network │         │ ops       │       │-Listener│
   │-Heavy   │         │-No cache  │       │-Handler │
   │ bind    │         │           │       │         │
   └────┬────┘         └─────┬─────┘       └────┬────┘
        │                    │                   │
   ┌────┴────┐         ┌─────┴─────┐       ┌────┴────┐
   │Detect:  │         │Detect:    │       │Detect:  │
   │-Profiler│         │-Profiler  │       │-LeakCan │
   │-Frame   │         │-CPU %     │       │-Heap    │
   │ time    │         │-Battery   │       │ dump    │
   └────┬────┘         └─────┬─────┘       └────┬────┘
        │                    │                   │
   ┌────┴────┐         ┌─────┴─────┐       ┌────┴────┐
   │Fix:     │         │Fix:       │       │Fix:     │
   │-Glide   │         │-Arrays.   │       │-AppCtx  │
   │-Async   │         │ sort()    │       │-WeakRef │
   │-ViewHold│         │-BG thread │       │-Cleanup │
   └─────────┘         └───────────┘       └─────────┘
```
