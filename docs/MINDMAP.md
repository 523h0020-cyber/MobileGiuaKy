# 🗺️ Mind Map - Android Performance Profiling

## Tổng quan Mind Map

```
                                    ┌─────────────────────────────────────┐
                                    │      ANDROID PERFORMANCE            │
                                    │         PROFILING                   │
                                    └─────────────────┬───────────────────┘
                                                      │
                    ┌─────────────────────────────────┼─────────────────────────────────┐
                    │                                 │                                 │
           ┌────────┴────────┐              ┌────────┴────────┐              ┌────────┴────────┐
           │   🐌 JANK/LAG   │              │   🔥 CPU HIGH   │              │   💾 MEMORY     │
           │   (UI Blocking) │              │                 │              │   LEAK          │
           └────────┬────────┘              └────────┬────────┘              └────────┬────────┘
                    │                                │                                │
    ┌───────────────┼───────────────┐    ┌──────────┼──────────┐         ┌──────────┼──────────┐
    │               │               │    │          │          │         │          │          │
┌───┴───┐      ┌────┴────┐    ┌────┴───┐ ┌───┴──┐ ┌───┴──┐ ┌───┴──┐  ┌───┴──┐  ┌───┴──┐  ┌───┴──┐
│CAUSES │      │ DETECT  │    │  FIX   │ │CAUSE │ │DETECT│ │ FIX  │  │CAUSE │  │DETECT│  │ FIX  │
└───┬───┘      └────┬────┘    └────┬───┘ └───┬──┘ └───┬──┘ └───┬──┘  └───┬──┘  └───┬──┘  └───┬──┘
    │               │              │         │        │        │         │         │         │
    ▼               ▼              ▼         ▼        ▼        ▼         ▼         ▼         ▼
┌─────────┐  ┌──────────┐  ┌──────────┐ ┌───────┐ ┌──────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐
│•Network │  │•Android  │  │•Glide    │ │•O(n²) │ │•CPU  │ │•Arrays│ │•Static│ │•Leak  │ │•App   │
│ on Main │  │ Profiler │  │•Async    │ │ sort  │ │ Prof │ │.sort()│ │Context│ │Canary │ │Context│
│•Heavy   │  │•Frame    │  │•ViewHolder│ │•Linear│ │•Batt │ │•Binary│ │•Inner │ │•Memory│ │•Weak  │
│ Bind    │  │ Time     │  │•DiffUtil │ │ search│ │ Prof │ │Search │ │ Class │ │ Prof  │ │ Ref   │
│•Sync    │  │•GPU      │  │•Paging   │ │•No    │ │      │ │•Cache │ │•Unreg │ │•Heap  │ │•Clean │
│ Download│  │ Render   │  │         │ │ cache │ │      │ │•BG    │ │Listener│ │ Dump  │ │ up    │
└─────────┘  └──────────┘  └──────────┘ └───────┘ └──────┘ └───────┘ └───────┘ └───────┘ └───────┘
```

---

## Chi tiết từng nhánh

### 🐌 1. JANK/LAG (UI Blocking)

#### 1.1 Nguyên nhân (Causes)
```
JANK CAUSES
    │
    ├── Network on Main Thread
    │   ├── Download images synchronously
    │   ├── API calls without async
    │   └── File I/O on UI thread
    │
    ├── Heavy onBindViewHolder
    │   ├── Image processing
    │   ├── Complex calculations
    │   └── Object creation in loops
    │
    ├── Inefficient Algorithms
    │   ├── O(n²) operations
    │   └── Unoptimized loops
    │
    └── Layout Issues
        ├── Deep view hierarchy
        ├── Heavy overdraw
        └── Unnecessary requestLayout()
```

#### 1.2 Phát hiện (Detect)
```
JANK DETECTION
    │
    ├── Android Profiler
    │   ├── CPU Trace → Main thread
    │   ├── Method > 16ms = problem
    │   └── Call stack analysis
    │
    ├── Frame Metrics
    │   ├── FPS < 60 = jank
    │   ├── Frame time > 16.67ms
    │   └── Dropped frames count
    │
    ├── GPU Rendering
    │   ├── Profile GPU Rendering
    │   ├── Debug GPU Overdraw
    │   └── Layout Inspector
    │
    └── Systrace
        ├── System-wide view
        ├── Thread scheduling
        └── Frame timeline
```

#### 1.3 Giải pháp (Fix)
```
JANK SOLUTIONS
    │
    ├── Image Loading
    │   ├── Glide / Picasso
    │   ├── Async loading
    │   └── Caching
    │
    ├── Background Threading
    │   ├── AsyncTask (deprecated)
    │   ├── Coroutines
    │   ├── RxJava
    │   └── ExecutorService
    │
    ├── RecyclerView Optimization
    │   ├── ViewHolder pattern
    │   ├── DiffUtil
    │   ├── setHasFixedSize(true)
    │   └── Payload updates
    │
    └── Layout Optimization
        ├── Flatten hierarchy
        ├── ConstraintLayout
        ├── ViewStub
        └── merge tag
```

---

### 🔥 2. HIGH CPU USAGE

#### 2.1 Nguyên nhân (Causes)
```
CPU HIGH CAUSES
    │
    ├── Inefficient Algorithms
    │   ├── Bubble Sort O(n²)
    │   ├── Linear Search O(n)
    │   ├── Nested loops
    │   └── Recursive without memo
    │
    ├── Repeated Operations
    │   ├── Same calculation multiple times
    │   ├── No result caching
    │   └── Polling instead of events
    │
    ├── Heavy Processing
    │   ├── Image manipulation
    │   ├── JSON parsing
    │   └── Compression
    │
    └── Wasted Resources
        ├── Creating objects in loops
        ├── String concatenation
        └── Unnecessary operations
```

#### 2.2 Phát hiện (Detect)
```
CPU DETECTION
    │
    ├── Android Profiler
    │   ├── CPU Usage %
    │   ├── Sample/Trace recording
    │   └── Top methods by time
    │
    ├── Battery Profiler
    │   ├── CPU wake locks
    │   ├── Background activity
    │   └── Energy consumption
    │
    └── ADB Commands
        ├── adb shell top
        ├── adb shell dumpsys cpuinfo
        └── adb shell dumpsys batterystats
```

#### 2.3 Giải pháp (Fix)
```
CPU SOLUTIONS
    │
    ├── Better Algorithms
    │   ├── Arrays.sort() O(n log n)
    │   ├── Binary Search O(log n)
    │   ├── HashMap O(1) lookup
    │   └── Memoization
    │
    ├── Caching
    │   ├── LruCache
    │   ├── DiskLruCache
    │   └── Room/SQLite
    │
    ├── Background Processing
    │   ├── WorkManager
    │   ├── Thread pools
    │   └── Kotlin Coroutines
    │
    └── Code Optimization
        ├── StringBuilder
        ├── Object pooling
        └── Lazy initialization
```

---

### 💾 3. MEMORY LEAKS

#### 3.1 Nguyên nhân (Causes)
```
MEMORY LEAK CAUSES
    │
    ├── Static References
    │   ├── Singleton holds Activity
    │   ├── Static View reference
    │   └── Static Context
    │
    ├── Inner Classes
    │   ├── Anonymous class holds outer
    │   ├── Non-static inner class
    │   └── Handler/Runnable
    │
    ├── Listeners/Callbacks
    │   ├── Not unregistered
    │   ├── Event bus subscribers
    │   └── Broadcast receivers
    │
    └── Resources
        ├── Unclosed streams
        ├── Cursor not closed
        └── Bitmap not recycled
```

#### 3.2 Phát hiện (Detect)
```
MEMORY LEAK DETECTION
    │
    ├── LeakCanary
    │   ├── Auto detection
    │   ├── Leak trace
    │   └── Notification
    │
    ├── Android Profiler
    │   ├── Memory graph
    │   ├── Heap dump
    │   └── Allocations tracking
    │
    ├── MAT (Memory Analyzer)
    │   ├── Dominator tree
    │   ├── Leak suspects
    │   └── Histogram
    │
    └── Manual Testing
        ├── Rotate device
        ├── Navigate back/forth
        └── Force GC
```

#### 3.3 Giải pháp (Fix)
```
MEMORY LEAK SOLUTIONS
    │
    ├── Context Handling
    │   ├── Use Application Context
    │   ├── getApplicationContext()
    │   └── Avoid Activity in Singleton
    │
    ├── Reference Types
    │   ├── WeakReference
    │   ├── SoftReference
    │   └── Nullify references
    │
    ├── Lifecycle Awareness
    │   ├── Cleanup in onDestroy()
    │   ├── LifecycleObserver
    │   └── ViewModel
    │
    └── Proper Unregistration
        ├── removeCallbacks()
        ├── unregisterReceiver()
        └── removeEventListener()
```

---

## 🛠️ Tools Mind Map

```
PROFILING TOOLS
    │
    ├── Built-in
    │   ├── Android Profiler
    │   │   ├── CPU
    │   │   ├── Memory
    │   │   ├── Network
    │   │   └── Energy
    │   │
    │   ├── Layout Inspector
    │   ├── GPU Rendering
    │   └── StrictMode
    │
    ├── Third Party
    │   ├── LeakCanary
    │   ├── MAT
    │   ├── Firebase Performance
    │   └── Perfetto
    │
    └── Command Line
        ├── adb shell dumpsys
        ├── systrace
        └── perfetto
```

---

## 📱 App Structure Mind Map

```
APP ARCHITECTURE
    │
    ├── MainActivity
    │   ├── RecyclerView (Photo List)
    │   │   ├── PhotoAdapter
    │   │   │   ├── BAD: bindViewHolderBad()
    │   │   │   └── GOOD: bindViewHolderGood()
    │   │   │
    │   │   ├── Search Function
    │   │   │   ├── BAD: searchBad()
    │   │   │   └── GOOD: searchGood()
    │   │   │
    │   │   └── Sort Function
    │   │       ├── BAD: sortByTitleBad()
    │   │       └── GOOD: sortByTitleGood()
    │   │
    │   └── Stress CPU Button
    │       └── HeavyProcessor
    │
    ├── PhotoDetailActivity
    │   ├── Memory Leak Demo
    │   │   ├── BAD: LeakyManager
    │   │   └── GOOD: NonLeakyManager
    │   │
    │   └── Download Demo
    │       ├── BAD: downloadOnMainThread()
    │       └── GOOD: downloadOnBackgroundThread()
    │
    └── Backend
        ├── GET /api/photos
        ├── GET /api/photos/:id
        ├── POST /api/photos
        └── DELETE /api/photos/:id
```

---

## 📊 Comparison Mind Map

```
BAD vs GOOD PRACTICES
    │
    ├── Image Loading
    │   ├── ❌ downloadImageSync() → ANR
    │   └── ✅ Glide.with().load() → Smooth
    │
    ├── Sorting
    │   ├── ❌ Bubble Sort O(n²) → Slow
    │   └── ✅ Arrays.sort() O(n log n) → Fast
    │
    ├── Context
    │   ├── ❌ Singleton.init(activity) → Leak
    │   └── ✅ Singleton.init(appContext) → Safe
    │
    ├── Callbacks
    │   ├── ❌ Strong reference → Leak
    │   └── ✅ WeakReference → GC-able
    │
    └── Threading
        ├── ❌ Main thread I/O → Freeze
        └── ✅ Background thread → Responsive
```
