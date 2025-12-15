# 🎯 KỊCH BẢN DEMO - CẬP NHẬT MỚI
## Photo Gallery Performance Testing App

> **Người demo:** Developer (Người 2)  
> **Thời gian demo:** ~15-20 phút  
> **Mục tiêu:** Chứng minh 3 loại lỗi performance và cách tối ưu

---

## 📋 CHECKLIST CHUẨN BỊ

### Trước khi demo (30 phút trước):
```
✅ Backend Setup:
   □ Node.js đã cài đặt (v18.x+)
   □ cd backend && npm install (nếu chưa)
   □ npm start → Server running on port 3000
   □ Test API: curl http://localhost:3000/api/photos
   □ Copy 10-20 ảnh vào backend/public/images/ (nếu demo real device)

✅ Android Studio:
   □ Mở project MobileGiuaKy
   □ Gradle sync hoàn tất
   □ Profiler mở sẵn: View → Tool Windows → Profiler
   □ Device/Emulator đã kết nối

✅ App Configuration:
   □ ApiClient.java có đúng BASE_URL:
     - Emulator: http://10.0.2.2:3000/
     - Real Device: http://192.168.x.x:3000/ (thay IP của bạn)
   □ Build và install app thành công
   □ App mở được và load danh sách ảnh

✅ Backup:
   □ Chụp screenshots Profiler graphs trước (để backup nếu demo fail)
   □ Prepare 1-2 slides giải thích code (optional)
```

---

## 🎬 KỊCH BẢN DEMO CHI TIẾT

### PHẦN 1: GIỚI THIỆU (2 phút)

**Nội dung trình bày:**

> "Xin chào mọi người. Hôm nay em sẽ demo 3 loại lỗi performance phổ biến trong Android:
> 
> 1. **JANK/LAG** - UI bị giật, khựng do xử lý nặng trên Main Thread
> 2. **HIGH CPU** - CPU spike 100% do thuật toán kém hiệu quả (O(n²))
> 3. **MEMORY LEAK** - Activity không được giải phóng, memory tăng liên tục
>
> Ứng dụng này là một Photo Gallery có toggle BAD/GOOD mode để so sánh hiệu năng.
> Em đã cố ý cài cắm các lỗi vào code để demonstate cách phát hiện và fix.
>
> Các công cụ sử dụng:
> - Android Studio Profiler (CPU, Memory)
> - LeakCanary (Memory Leak detection)
> - Logcat (Performance metrics)
>
> Bắt đầu demo!"

---

### PHẦN 2: DEMO JANK/LAG - SCENARIO A (4 phút)

**Mục tiêu:** Chứng minh UI blocking khi cuộn RecyclerView

#### 🎬 Demo Steps:

**1. Mở app → MainActivity hiển thị danh sách ảnh**
```
[Màn hình hiện]: RecyclerView với ~10-50 ảnh
[Nói]: "Đây là màn hình chính, hiển thị danh sách ảnh từ backend API."
```

**2. Mở Android Profiler**
```
[Action]: View → Tool Windows → Profiler
[Nói]: "Em mở Profiler để theo dõi CPU và frame rendering realtime."
[Chọn]: Device → com.example.mobilegiuaky → CPU tab
```

**3. Bật BAD MODE trong app**
```
[Action]: Toggle switch "BAD MODE" sang ON (màu accent)
[Nói]: "BAD MODE sẽ trigger các heavy operations trong onBindViewHolder()."
```

**4. Cuộn danh sách nhanh**
```
[Action]: Swipe lên xuống nhiều lần, tốc độ vừa phải
[Quan sát & nói]: 
   "Mọi người thấy UI bị giật, khựng rõ rệt. Không mượt mà như mong đợi.
    Profiler hiển thị nhiều red frames - đây là janky frames.
    Frame time vượt quá 16ms (đường xanh) → Drop frames → Lag."

[Point to Logcat]:
   "D/PhotoAdapter: Processed title length: 2550"
   "D/PhotoAdapter: Sorted array length: 100"
   → "Đây là các heavy operations đang chạy trên Main Thread."
```

**5. TẮT BAD MODE**
```
[Action]: Toggle switch sang OFF
[Nói]: "Bây giờ em tắt BAD MODE, RecyclerView sẽ tự động refresh."
```

**6. Cuộn lại**
```
[Action]: Swipe lên xuống với cùng tốc độ
[Quan sát & nói]:
   "UI giờ mượt mà hoàn toàn. Không còn giật nữa.
    Profiler: Frame time < 16ms, FPS = 60.
    Không còn red frames."
```

**7. Giải thích nguyên nhân**
```
[Nói]: 
"Tại sao BAD MODE lại lag?

Trong PhotoAdapter.bindViewHolderBad(), em cố ý gọi:
- heavyStringProcessing() → 50 iterations string operations (~20ms)
- inefficientSort() → Bubble Sort O(n²) (~15ms)
- heavyImageProcessing() → Pixel-by-pixel processing (~300ms)
- downloadImageSync() → Download trên Main Thread

→ TOTAL: ~350ms mỗi item bind
→ Android chỉ có 16.67ms cho 1 frame (60 FPS)
→ Vượt quá → Drop frames → Jank

GOOD MODE:
- Chỉ bind data đơn giản
- Images load bằng Glide (background thread)
- Không có heavy operations

→ Chỉ mất ~5ms mỗi item
→ UI mượt mà 60 FPS"
```

---

### PHẦN 3: DEMO JANK/LAG - SCENARIO B VỚI ADMIN PANEL (3 phút)

**Mục tiêu:** Chứng minh performance khi bulk add nhiều items

#### 🎬 Demo Steps:

**1. Từ MainActivity, nhấn nút "🔐 Admin"**
```
[Action]: Tap nút Admin góc trên bên phải
[Nói]: "App có Admin Panel để quản lý ảnh - thêm và xóa.
       Em dùng nó để test performance trong scenario thực tế."
```

**2. Bật BAD MODE trong Admin**
```
[Action]: Toggle "BAD MODE" trong AdminActivity
[Nói]: "Bật lại BAD MODE để thấy sự khác biệt."
```

**3. Bulk Add 50 ảnh**
```
[Action]: Nhấn "Add Multiple" → Nhập 50 → Create
[Quan sát & nói]:
   "Em đang thêm 50 ảnh cùng lúc.
    → UI freeze, đơ hoàn toàn trong ~8-10 giây.
    → ProgressBar hiện nhưng không smooth.
    → CPU spike ~100% trong Profiler."

[Point to Logcat after complete]:
   "D/AdminActivity: API time: 5234ms"
   "D/AdminActivity: UI update time (BAD): 8932ms"
   → "API nhanh, nhưng UI update chậm vì gọi notifyDataSetChanged()
      → Rebind ALL items với heavy operations."
```

**4. TẮT BAD MODE và thử lại**
```
[Action]: 
   - Nhấn "Delete All" → Xóa 50 ảnh vừa thêm
   - Toggle BAD MODE sang OFF
   - Nhấn "Add Multiple" → 50 → Create

[Quan sát & nói]:
   "UI giờ mượt mà, không freeze.
    ProgressBar update smooth.
    CPU chỉ ~40%."

[Point to Logcat]:
   "D/AdminActivity: API time: 5156ms" (tương tự)
   "D/AdminActivity: UI update time (GOOD): 245ms"
   → "Nhanh hơn 36 lần! Vì chỉ bind items mới, không rebind tất cả."
```

**5. Giải thích**
```
[Nói]:
"Sự khác biệt:
- BAD: notifyDataSetChanged() → Rebind ALL → Mỗi item chạy heavy ops
- GOOD: notifyItemRangeInserted() hoặc DiffUtil → Chỉ bind items mới

Lesson: Bulk operations + BAD pattern = UI freeze → UX tệ"
```

---

### PHẦN 4: DEMO HIGH CPU (3 phút)

**Mục tiêu:** Chứng minh CPU spike do thuật toán O(n²)

#### 🎬 Demo Steps:

**1. Quay lại MainActivity**
```
[Action]: Nhấn Back từ Admin
[Nói]: "Tiếp theo em demo CPU issue."
```

**2. Bật BAD MODE + Mở CPU Profiler**
```
[Action]: 
   - Toggle "BAD MODE" ON
   - Profiler → CPU tab (nếu chưa mở)
```

**3. Nhấn "STRESS CPU" (hoặc scroll để trigger)**
```
[Action]: Nhấn nút trong MainActivity (nếu có) hoặc scroll
[Quan sát & nói]:
   "CPU spike lên ~95-100%.
    App không phản hồi trong ~3-5 giây.
    ANR (App Not Responding) có thể xuất hiện nếu >5 giây."

[Point to Logcat]:
   "D/HeavyProcessor: Bubble sort of 2000 items took 2543ms"
   "D/HeavyProcessor: Inefficient search took 1823ms"
```

**4. TẮT BAD MODE và thử lại**
```
[Action]: Toggle OFF → Nhấn lại "STRESS CPU"
[Quan sát & nói]:
   "CPU chỉ ~25%, hoàn thành < 1 giây."

[Logcat]:
   "D/HeavyProcessor: Arrays.sort of 2000 items took 12ms"
   → "Nhanh hơn 200 lần!"
```

**5. Giải thích**
```
[Nói]:
"Nguyên nhân:
- BAD: Bubble Sort O(n²) → 2000 items = 4,000,000 comparisons
- GOOD: Arrays.sort() O(n log n) → ~22,000 comparisons

Bubble Sort:
for (i = 0; i < n; i++) {
    for (j = 0; j < n-i; j++) {
        if (arr[j] > arr[j+1]) swap();
    }
}
→ Nested loops → Exponential growth

Arrays.sort() uses Dual-Pivot Quicksort → Much faster.

Lesson: Chọn đúng thuật toán rất quan trọng!"
```

---

### PHẦN 5: DEMO MEMORY LEAK (5 phút)

**Mục tiêu:** Chứng minh Activity không được GC thu hồi

#### 🎬 Demo Steps:

**1. Mở Memory Profiler**
```
[Action]: Profiler → Memory tab
[Nói]: "Bây giờ em demo Memory Leak."
```

**2. Ghi nhận baseline**
```
[Action]: Đọc số memory hiện tại
[Nói]: "Memory baseline: ~85MB"
```

**3. Mở PhotoDetailActivity**
```
[Action]: Tap vào 1 ảnh bất kỳ
[Nói]: "Em mở màn hình chi tiết ảnh."
```

**4. Bật LEAK MODE**
```
[Action]: Toggle "LEAK MODE" ON
[Nói]: "LEAK MODE sẽ trigger các anti-pattern gây leak."
```

**5. Cause Leak**
```
[Action]: Nhấn nút "CAUSE MEMORY LEAK"
[Nói]: "Nút này gọi:
       LeakyManager.getInstance().init(this) 
       → Singleton giữ Activity Context
       → Root cause của leak."

[Logcat]:
   "W/PhotoDetailActivity: ⚠️ MEMORY LEAK CAUSED!"
```

**6. Back về MainActivity**
```
[Action]: Nhấn Back
[Nói]: "Activity bị destroy, nhưng Singleton vẫn giữ reference."
```

**7. Lặp lại 5-10 lần**
```
[Action]: Tap ảnh → Leak Mode ON → Cause Leak → Back → Repeat
[Nói]: "Em mở và đóng Activity 10 lần liên tiếp."
[Quan sát Memory]: Memory tăng từ 85MB → ~140MB
```

**8. Force GC**
```
[Action]: Profiler → Icon thùng rác (Force GC)
[Chờ 3 giây]
[Quan sát & nói]:
   "Sau GC, memory KHÔNG giảm (vẫn ~138MB).
    Lý do: GC không thể thu hồi vì còn strong reference."
```

**9. LeakCanary notification xuất hiện**
```
[Action]: Pull down notification
[Nói]: "LeakCanary tự động phát hiện leak và hiện notification."
[Đọc notification]:
   "┬───
    │ GC Root: Global variable in LeakyManager.instance
    │
    ├─ LeakyManager.context
    │    Leaking: YES
    │
    ╰→ PhotoDetailActivity
         Leaking: YES (Activity destroyed but still in memory)
         
    → 5 instances leaked (50MB)"
```

**10. Restart app, tắt LEAK MODE và thử lại**
```
[Action]: 
   - Restart app (kill & reopen)
   - Lặp lại nhưng KHÔNG bật Leak Mode
   - Force GC

[Quan sát & nói]:
   "Memory giảm về baseline (~85MB).
    Không có leak notification.
    Activities được GC thu hồi thành công."
```

**11. Giải thích root cause**
```
[Nói]:
"Tại sao bị leak?

CODE:
// LeakyManager.java
public class LeakyManager {
    private static LeakyManager instance;  // Static = sống mãi
    private Context context;               // Giữ Activity Context
    
    public void init(Context context) {
        this.context = context;  // ⚠️ BUG!
    }
}

// PhotoDetailActivity.java
LeakyManager.getInstance().init(this);  // this = Activity

PROBLEM:
1. Singleton lifecycle = Application (never dies)
2. Activity lifecycle = Short (destroyed on Back)
3. Singleton giữ reference → Activity không thể GC
4. Mỗi lần mở Activity = +1 leaked object (~10MB)

FIX:
// ✅ GOOD
public void init(Context context) {
    this.context = context.getApplicationContext();
}

Application Context lifecycle = Application → OK để giữ trong Singleton.

Hoặc dùng WeakReference:
private WeakReference<Context> contextRef;
"
```

---

### PHẦN 6: DEMO REAL DEVICE VỚI STATIC IMAGES (Optional - 3 phút)

**Nếu có thời gian và đã setup sẵn**

#### 🎬 Demo Steps:

**1. Giới thiệu**
```
[Nói]: "Em có thêm feature Admin để load ảnh từ máy tính qua WiFi."
```

**2. Show setup**
```
[Show folder]: backend/public/images/ với 10-20 ảnh
[Nói]: "Em đã copy ảnh vào folder này."
```

**3. Check IP**
```
[CMD]: ipconfig
[Nói]: "IP máy tính: 192.168.1.105"
```

**4. Mở Admin trong app (trên điện thoại thật)**
```
[Action]: Nhấn "🔐 Admin" → "➕ Add Photo"
[Nhập]:
   - Title: Demo Photo 1
   - Image URL: http://192.168.1.105:3000/images/photo1.jpg
[Nhấn]: Create
```

**5. Quay lại MainActivity**
```
[Quan sát & nói]:
   "Ảnh xuất hiện trong danh sách.
    Đang được serve từ máy tính qua WiFi.
    Glide load ảnh qua network mượt mà."
```

**6. Bulk add để test**
```
[Admin]: Add Multiple → 20
[Nói]: "Em có thể bulk add nhiều ảnh để test performance với real data."
```

---

## 📊 TỔNG KẾT (2 phút)

**Tóm tắt kết quả:**

```
[Nói]:
"Tổng kết lại:

1. JANK/LAG:
   ✗ BAD: 45ms/frame, FPS ~22, UI lag
   ✓ GOOD: 8ms/frame, FPS 60, smooth
   → Fix: Tránh heavy operations trên Main Thread

2. HIGH CPU:
   ✗ BAD: Bubble Sort O(n²) = 2543ms, CPU 95%
   ✓ GOOD: Arrays.sort O(n log n) = 12ms, CPU 25%
   → Fix: Chọn thuật toán hiệu quả

3. MEMORY LEAK:
   ✗ BAD: +50MB sau 5 lần, không GC được
   ✓ GOOD: Stable memory, GC thu hồi
   → Fix: Dùng Application Context, WeakReference, cleanup

Công cụ phát hiện:
- Android Profiler: CPU, Memory, Frame time
- LeakCanary: Auto detect leaks
- Logcat: Performance metrics
- Systrace (nếu cần deep dive)

Best Practices:
✅ Async operations cho network/DB/heavy computation
✅ Efficient algorithms (O(n log n) > O(n²))
✅ Application Context cho Singleton
✅ Cleanup trong onDestroy()
✅ Glide/Coil cho image loading
✅ DiffUtil cho RecyclerView updates

Questions?"
```

---

## 🛠️ TROUBLESHOOTING

**Nếu gặp vấn đề trong lúc demo:**

### Backend không chạy:
```
Triệu chứng: App báo "Connection refused"
Fix ngay:
1. Mở terminal: cd backend && npm start
2. Nếu vẫn lỗi: Check port 3000 có bị chiếm không
   netstat -ano | findstr :3000
   taskkill /PID <PID> /F
3. Fallback: Backend có in-memory mode, app vẫn chạy được
```

### Profiler không hiện graph:
```
Triệu chứng: Profiler trống, không có data
Fix:
1. Chọn lại device và process
2. Restart Android Studio
3. Fallback: Dùng Logcat để show metrics
```

### LeakCanary không hiện notification:
```
Triệu chứng: Không có leak notification sau 10 lần
Fix:
1. Check build variant: Phải là Debug (không phải Release)
2. Chờ thêm 30 giây (LeakCanary analyze delayed)
3. Fallback: Dùng Profiler Memory tab → Heap Dump
```

### App crash khi demo:
```
Triệu chứng: App force close
Fix:
1. Check Logcat xem lỗi gì
2. Restart app
3. Fallback: Dùng screenshots đã chuẩn bị
```

### Ảnh không load (Real Device):
```
Triệu chứng: ImageView trống
Fix:
1. Check cùng WiFi: Máy tính và điện thoại
2. Check firewall: Tắt tạm hoặc allow port 3000
3. Ping test: Từ điện thoại ping IP máy tính
4. Fallback: Dùng public URLs (Picsum)
```

---

## 💡 TIPS ĐỂ DEMO THÀNH CÔNG

### Chuẩn bị kỹ:
- ✅ Test demo flow ít nhất 1 lần trước
- ✅ Chụp screenshots Profiler graphs để backup
- ✅ Có plan B nếu device/emulator crash
- ✅ Print ra cheat sheet này để tham khảo

### Trong lúc demo:
- ✅ Nói CHẬM, RÕNG RÀNG
- ✅ Point to màn hình khi giải thích
- ✅ Đợi audience hiểu trước khi chuyển bước
- ✅ Nhấn mạnh con số (45ms vs 8ms, 2543ms vs 12ms)
- ✅ Giải thích "Why" không chỉ "What"

### Khi trả lời câu hỏi:
- ✅ Lặp lại câu hỏi để mọi người nghe
- ✅ Trả lời ngắn gọn, đúng trọng tâm
- ✅ Nếu không biết: "Em cần research thêm về vấn đề này"
- ✅ Link back to demo: "Như em đã show trong demo..."

---

## 📚 TÀI LIỆU THAM KHẢO (Nếu hỏi)

- **Android Performance Best Practices:**  
  https://developer.android.com/topic/performance

- **LeakCanary Documentation:**  
  https://square.github.io/leakcanary/

- **RecyclerView Optimization:**  
  https://developer.android.com/guide/topics/ui/layout/recyclerview

- **Algorithm Complexity:**  
  Big-O Cheat Sheet: https://www.bigocheatsheet.com/

---

## ⏱️ TIMELINE SUMMARY

| Phần | Nội dung | Thời gian |
|------|----------|-----------|
| 1 | Giới thiệu | 2 phút |
| 2 | Jank - Scenario A (Scroll) | 4 phút |
| 3 | Jank - Scenario B (Bulk Add) | 3 phút |
| 4 | High CPU | 3 phút |
| 5 | Memory Leak | 5 phút |
| 6 | Real Device (Optional) | 3 phút |
| 7 | Tổng kết + Q&A | 2-5 phút |
| **TOTAL** | | **20-25 phút** |

---

**🎯 MỤC TIÊU ĐẠT ĐƯỢC:**
- ✅ Demonstate 3 loại lỗi performance với evidence (Profiler graphs, metrics)
- ✅ Giải thích root cause bằng code snippets
- ✅ Chứng minh fix works (BAD vs GOOD comparison)
- ✅ Educate audience về best practices
- ✅ Thể hiện kỹ năng debugging và profiling tools

**GOOD LUCK! 🚀**
