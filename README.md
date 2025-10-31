# Android Tools - Battery Alert

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-21-green.svg)](https://developer.android.com/)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35-orange.svg)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Ứng dụng Android giúp bảo vệ pin điện thoại bằng cách theo dõi mức pin và cảnh báo khi pin đạt ngưỡng cấu hình (mặc định 80%) để giúp người dùng bảo vệ sức khỏe pin.

## 📱 Tính năng chính

### 🔋 Battery Alert (Tính năng chính)
- ✅ **Theo dõi pin theo thời gian thực**: Giám sát mức pin và trạng thái sạc liên tục
- ✅ **Ngưỡng tùy chỉnh**: Cấu hình ngưỡng cảnh báo từ 0-100% (mặc định: 80%)
- ✅ **Cảnh báo đa phương tiện**: 
  - Thông báo với âm thanh tùy chỉnh
  - Rung khi cảnh báo (có thể bật/tắt)
  - Hiển thị notification với các hành động Dismiss và Snooze
- ✅ **Chạy nền**: Foreground Service để đảm bảo hoạt động liên tục
- ✅ **Tự động khởi động**: Tự động bắt đầu khi thiết bị khởi động lại
- ✅ **Tối ưu pin**: Quản lý tối ưu hóa pin và Doze mode
- ✅ **Tùy chỉnh âm thanh**: 
  - Chọn nhạc chuông từ hệ thống
  - Điều chỉnh âm lượng thông báo
  - Cấu hình số lần phát âm thanh (1-10 lần)
  - Cấu hình khoảng thời gian lặp lại (giây)

### 🔊 Float Volume (Tính năng phụ)
- ✅ **Nút âm lượng nổi**: Hiển thị nút điều khiển âm lượng nổi trên màn hình
- ✅ **Điều khiển nhanh**: Truy cập nhanh các mức âm lượng khác nhau

## 🏗️ Kiến trúc

Dự án được xây dựng theo kiến trúc **MVVM (Model-View-ViewModel)** với các thành phần sau:

### Layer Architecture
```
Presentation Layer (UI)
    ├── Activities (MenuActivity, MainActivity)
    ├── ViewModels (MainViewModel)
    └── Views (XML Layouts)

Domain Layer
    ├── Use Cases
    └── Repository Interfaces

Data Layer
    ├── Repository Implementation
    ├── Data Models (BatteryInfo, ChargingType, BatteryHealth)
    └── Data Sources (PreferencesHelper, DataStore)

Service Layer
    ├── BatteryMonitorService (Foreground Service)
    ├── FloatingVolumeService
    └── NotificationHelper

System Layer
    ├── BroadcastReceivers (BatteryBroadcastReceiver, BootCompletedReceiver)
    └── Utilities (PermissionHelper, BatteryOptimizationHelper)
```

## 🛠️ Công nghệ sử dụng

### Core Technologies
- **Language**: Kotlin 2.0.21
- **Min SDK**: API 21 (Android 5.0 Lollipop)
- **Target SDK**: API 35 (Android 15)
- **Build System**: Gradle với Kotlin DSL
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt (Dagger 2.48)

### Key Libraries
```kotlin
// Architecture Components
- androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0
- androidx.lifecycle:lifecycle-livedata-ktx:2.7.0
- androidx.navigation:navigation-fragment-ktx:2.7.6

// Coroutines & Flow
- kotlinx-coroutines-android:1.7.3
- kotlinx-coroutines-core:1.7.3

// Dependency Injection
- com.google.dagger:hilt-android:2.48

// Background Processing
- androidx.work:work-runtime-ktx:2.9.0

// Data Storage
- androidx.datastore:datastore-preferences:1.0.0

// UI
- com.google.android.material:material:1.11.0
- androidx.compose (cho một số thành phần)
```

## 📂 Cấu trúc dự án

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/techvibe/androidtools/
│   │   │   ├── data/
│   │   │   │   ├── model/
│   │   │   │   │   └── BatteryInfo.kt              # Model dữ liệu pin
│   │   │   │   ├── preferences/
│   │   │   │   │   ├── PreferencesHelper.kt        # Quản lý SharedPreferences
│   │   │   │   │   └── FloatVolumePreferences.kt
│   │   │   │   └── repository/
│   │   │   │       └── BatteryRepositoryImpl.kt    # Repository implementation
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── repository/
│   │   │   │   │   └── BatteryRepository.kt        # Repository interface
│   │   │   │   └── usecase/
│   │   │   │
│   │   │   ├── presentation/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── menu/
│   │   │   │   │   │   └── MenuActivity.kt         # Activity menu chính
│   │   │   │   │   ├── floatvolume/
│   │   │   │   │   │   └── FloatVolumeSettingsActivity.kt
│   │   │   │   │   └── common/
│   │   │   │   └── viewmodel/
│   │   │   │       └── MainViewModel.kt            # ViewModel cho MainActivity
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── BatteryMonitorService.kt        # Foreground Service chính
│   │   │   │   ├── FloatingVolumeService.kt        # Service cho float volume
│   │   │   │   └── NotificationHelper.kt           # Helper cho notifications
│   │   │   │
│   │   │   ├── receiver/
│   │   │   │   ├── BatteryBroadcastReceiver.kt     # Nhận broadcast pin
│   │   │   │   └── BootCompletedReceiver.kt        # Khởi động sau boot
│   │   │   │
│   │   │   ├── utils/
│   │   │   │   ├── Constants.kt                    # Các hằng số
│   │   │   │   ├── PermissionHelper.kt             # Helper xử lý permissions
│   │   │   │   └── BatteryOptimizationHelper.kt    # Helper tối ưu pin
│   │   │   │
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt                    # Hilt Module
│   │   │   │
│   │   │   ├── MainActivity.kt                     # Activity chính (Battery Alert)
│   │   │   └── BatteryAlertApplication.kt          # Application class
│   │   │
│   │   └── res/
│   │       ├── layout/                             # XML layouts
│   │       ├── values/                             # Strings, colors, themes
│   │       └── drawable/                           # Icons và images
│   │
│   ├── androidTest/                                # Android instrumentation tests
│   └── test/                                       # Unit tests
│
├── build.gradle.kts                                # App module build config
└── proguard-rules.pro                              # ProGuard rules
```

## 🚀 Cài đặt và Build

### Yêu cầu
- Android Studio Hedgehog | 2023.1.1 hoặc cao hơn
- JDK 11 hoặc cao hơn
- Android SDK API 21+
- Gradle 8.10.1+

### Các bước cài đặt

1. **Clone repository**
```bash
git clone <repository-url>
cd AndroidTools
```

2. **Mở project trong Android Studio**
- Mở Android Studio
- Chọn "Open an existing project"
- Chọn thư mục AndroidTools

3. **Sync Gradle**
- Android Studio sẽ tự động sync Gradle
- Hoặc chọn `File > Sync Project with Gradle Files`

4. **Build APK**
```bash
./gradlew assembleDebug
```

5. **Chạy trên thiết bị/emulator**
- Kết nối thiết bị Android hoặc khởi động emulator
- Chọn `Run > Run 'app'`

## 📋 Permissions

Ứng dụng yêu cầu các quyền sau:

```xml
<!-- Battery Monitoring -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Foreground Service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- Notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Vibration -->
<uses-permission android:name="android.permission.VIBRATE" />

<!-- Wake Lock -->
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Battery Optimization -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Float Volume (Optional) -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

## 📖 Hướng dẫn sử dụng

### Battery Alert

1. **Khởi động ứng dụng**
   - Mở ứng dụng từ launcher
   - Chọn "Battery Alert" từ menu chính

2. **Cấu hình ngưỡng pin**
   - Bật/tắt tính năng theo dõi bằng switch
   - Điều chỉnh ngưỡng pin bằng SeekBar (0-100%)
   - Giá trị mặc định: 80%

3. **Cấu hình thông báo**
   - Chọn nhạc chuông: Nhấn "Chọn nhạc chuông" và chọn từ danh sách
   - Bật/tắt rung: Sử dụng switch "Bật rung"
   - Cấu hình số lần phát: Nhập số lần từ 1-10
   - Cấu hình khoảng lặp lại: Nhập số giây giữa các lần lặp lại

4. **Cấp quyền**
   - Ứng dụng sẽ tự động yêu cầu các quyền cần thiết
   - Quan trọng: Cấp quyền "Tắt tối ưu hóa pin" để ứng dụng hoạt động tốt nhất

5. **Sử dụng**
   - Kết nối sạc vào thiết bị
   - Khi pin đạt ngưỡng đã cấu hình, ứng dụng sẽ:
     - Phát âm thanh cảnh báo
     - Rung (nếu đã bật)
     - Hiển thị notification với các hành động Dismiss/Snooze
   - Khi rút sạc, cảnh báo sẽ tự động dừng

### Float Volume

1. **Khởi động tính năng**
   - Chọn "Float Volume" từ menu chính
   - Cấp quyền "Hiển thị trên các ứng dụng khác"
   - Bật tính năng bằng switch

2. **Sử dụng**
   - Nút điều khiển âm lượng sẽ xuất hiện trên màn hình
   - Nhấn để điều chỉnh âm lượng
   - Kéo thả để di chuyển vị trí

## 🔧 Cấu hình và Tùy chỉnh

### Các hằng số có thể thay đổi

Trong file `Constants.kt`:

```kotlin
const val DEFAULT_BATTERY_THRESHOLD = 80        // Ngưỡng pin mặc định
const val DEFAULT_REPEAT_INTERVAL_SECONDS = 30 // Khoảng lặp lại mặc định (giây)
const val DEFAULT_NOTIFICATION_VOLUME = 80      // Âm lượng mặc định (0-100)
```

### Notification Channels

Ứng dụng tạo 2 notification channels:
- **Battery Alert Channel**: Thông báo cảnh báo pin (High Priority)
- **Service Channel**: Thông báo dịch vụ chạy nền (Low Priority)

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
```bash
./gradlew jacocoTestReport
```

## 🐛 Troubleshooting

### Vấn đề: Thông báo không xuất hiện
- **Giải pháp**: 
  - Kiểm tra quyền thông báo (Android 13+)
  - Kiểm tra Do Not Disturb mode
  - Đảm bảo Notification Channel đã được tạo

### Vấn đề: Service bị dừng trong background
- **Giải pháp**:
  - Tắt tối ưu hóa pin cho ứng dụng
  - Đảm bảo ứng dụng không bị đưa vào danh sách tối ưu hóa pin
  - Kiểm tra Auto-start permissions (một số thiết bị)

### Vấn đề: Âm thanh không phát
- **Giải pháp**:
  - Kiểm tra âm lượng hệ thống
  - Kiểm tra chế độ im lặng
  - Kiểm tra URI nhạc chuông có hợp lệ không

### Vấn đề: Không tự động khởi động sau boot
- **Giải pháp**:
  - Kiểm tra quyền RECEIVE_BOOT_COMPLETED
  - Một số thiết bị cần cấp quyền Auto-start thủ công trong Settings

## 📝 Flow hoạt động của Battery Alert

### 1. Khởi động ứng dụng
- **UI**: MenuActivity (Activity launcher)
- **Vị trí**: `presentation/ui/menu/MenuActivity.kt`
- **Hành động**: Hiển thị menu với các lựa chọn

### 2. Chọn Battery Alert
- **UI**: Nhấn vào card "Battery Alert" trên MenuActivity
- **Vị trí**: `presentation/ui/menu/MenuActivity.kt` (line 71-73)
- **Hành động**: 
  - Gọi `navigateToBatterySettings()` 
  - Khởi động MainActivity với animation slide

### 3. MainActivity được khởi động
- **UI**: MainActivity
- **Vị trí**: `MainActivity.kt` (line 53-64)
- **Hành động**:
  - `onCreate()` được gọi
  - Setup status bar, UI components
  - Đăng ký BatteryBroadcastReceiver
  - Load dữ liệu từ preferences
  - Kiểm tra và yêu cầu permissions

### 4. Đăng ký BatteryBroadcastReceiver
- **Vị trí**: `MainActivity.kt` (line 212-227)
- **Hành động**:
  - Tạo instance của BatteryBroadcastReceiver với callback
  - Inject dependencies (PreferencesHelper, BatteryRepository)
  - Đăng ký receiver với IntentFilter cho:
    - `ACTION_BATTERY_CHANGED`
    - `ACTION_POWER_CONNECTED`
    - `ACTION_POWER_DISCONNECTED`

### 5. Cấu hình ngưỡng pin
- **UI**: SeekBar trên MainActivity
- **Vị trí**: `MainActivity.kt` (line 94-113)
- **Hành động**:
  - Khi user kéo SeekBar, `onProgressChanged()` được gọi
  - Cập nhật TextView hiển thị giá trị
  - Gọi `viewModel.updateBatteryThreshold(progress)`
  - Khi user thả SeekBar, `onStopTrackingTouch()` được gọi
  - Lưu giá trị vào preferences qua ViewModel

### 6. Bật/tắt monitoring
- **UI**: Switch "Bật thông báo pin"
- **Vị trí**: `MainActivity.kt` (line 116-119)
- **Hành động**:
  - Khi user bật/tắt switch
  - Gọi `viewModel.setMonitoringEnabled(isChecked)`
  - Lưu trạng thái vào preferences
  - Hiển thị toast message

### 7. Kết nối sạc
- **Điều kiện**: User cắm sạc vào thiết bị
- **Hành động**:
  - Hệ thống phát broadcast `ACTION_POWER_CONNECTED`
  - `BatteryBroadcastReceiver.onReceive()` được gọi
  - **Vị trí**: `receiver/BatteryBroadcastReceiver.kt` (line 71-73)
  - Gọi `handlePowerConnection()` với `isConnected = true`

### 8. Theo dõi pin khi đang sạc
- **Điều kiện**: Thiết bị đang sạc
- **Hành động**:
  - Hệ thống phát broadcast `ACTION_BATTERY_CHANGED` liên tục
  - `BatteryBroadcastReceiver.onReceive()` được gọi
  - **Vị trí**: `receiver/BatteryBroadcastReceiver.kt` (line 68-69)
  - Gọi `handleBatteryChanged()`:
    - Extract thông tin pin từ Intent
    - Tạo BatteryInfo object
    - Cập nhật repository
    - Gọi callback để cập nhật UI
    - Kiểm tra nếu monitoring enabled và đang sạc → gọi `checkBatteryThreshold()`

### 9. Kiểm tra ngưỡng pin
- **Điều kiện**: Monitoring enabled, đang sạc, và pin thay đổi
- **Vị trí**: `receiver/BatteryBroadcastReceiver.kt` (line 168-177)
- **Hành động**:
  - `checkBatteryThreshold()` được gọi
  - So sánh `batteryInfo.level >= threshold`
  - Nếu đạt ngưỡng → gọi `triggerThresholdAlert()`

### 10. Kích hoạt cảnh báo
- **Vị trí**: `receiver/BatteryBroadcastReceiver.kt` (line 182-210)
- **Hành động**:
  - Tạo Intent để khởi động BatteryMonitorService
  - Đặt action: `ACTION_BATTERY_THRESHOLD_REACHED`
  - Gửi extras: battery level, charging status
  - Khởi động foreground service

### 11. BatteryMonitorService xử lý cảnh báo
- **Vị trí**: `service/BatteryMonitorService.kt` (line 124-159)
- **Hành động**:
  - `onStartCommand()` nhận Intent với action `ACTION_BATTERY_THRESHOLD_REACHED`
  - Gọi `handleBatteryThresholdReached()`
  - Kiểm tra nếu vẫn đang sạc:
    - Gọi `startForegroundService()` → Tạo persistent notification
    - Gọi `startBatteryAlert()` → Bắt đầu cảnh báo

### 12. Phát cảnh báo
- **Vị trí**: `service/BatteryMonitorService.kt` (line 221-245)
- **Hành động**:
  - `startBatteryAlert()` được gọi:
    - Acquire wake lock
    - Hiển thị notification qua `NotificationHelper.showBatteryAlert()`
    - Phát âm thanh qua `playSoundSequence()`
    - Rung thiết bị qua `startVibration()`
    - Lên lịch lặp lại qua `ringtoneHandler.postDelayed()`

### 13. Xử lý âm thanh
- **Vị trí**: `service/BatteryMonitorService.kt` (line 289-351)
- **Hành động**:
  - `playSoundSequence()` được gọi
  - Đọc số lần phát từ preferences
  - Gọi `soundRepeatRunnable` để phát nhiều lần
  - `playRingtone()` tạo MediaPlayer với URI nhạc chuông đã chọn
  - Phát với âm lượng đã cấu hình

### 14. Xử lý khi rút sạc
- **Điều kiện**: User rút sạc
- **Vị trí**: `receiver/BatteryBroadcastReceiver.kt` (line 75-80)
- **Hành động**:
  - Hệ thống phát broadcast `ACTION_POWER_DISCONNECTED`
  - `onReceive()` được gọi
  - Gọi `handlePowerConnection()` với `isConnected = false`
  - Gọi `stopBatteryAlert()` → Dừng service
  - Service tự động dừng và giải phóng tài nguyên

### 15. Dừng cảnh báo từ Notification
- **UI**: Nút "Dismiss" hoặc "Snooze" trên notification
- **Vị trí**: `service/BatteryMonitorService.kt` (line 164-201)
- **Hành động**:
  - Nếu nhấn "Dismiss": `handleDismissAction()` được gọi
    - Dừng cảnh báo
    - Dừng service
  - Nếu nhấn "Snooze": `handleSnoozeAction()` được gọi
    - Dừng cảnh báo tạm thời
    - Lên lịch phát lại sau 5 phút

### Điều kiện để tính năng hoạt động:
1. ✅ Quyền thông báo (POST_NOTIFICATIONS) đã được cấp
2. ✅ Quyền VIBRATE đã được cấp (nếu muốn rung)
3. ✅ Monitoring đã được bật trong UI
4. ✅ Thiết bị đang được sạc
5. ✅ Pin đạt ngưỡng đã cấu hình
6. ✅ Tối ưu hóa pin đã được tắt (khuyến nghị)

## 📄 License

MIT License - Xem file [LICENSE](LICENSE) để biết thêm chi tiết.

## 👨‍💻 Phát triển

### Code Style
- Sử dụng Kotlin coding conventions
- Sử dụng ViewBinding thay vì findViewById
- Comment code bằng tiếng Anh
- Tên biến và hàm bằng tiếng Anh

### Contributing
1. Fork repository
2. Tạo feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## 📞 Liên hệ

Nếu có câu hỏi hoặc đề xuất, vui lòng tạo issue trên GitHub repository.

---

**Lưu ý**: Ứng dụng này được thiết kế để giúp bảo vệ pin điện thoại bằng cách nhắc nhở người dùng ngừng sạc khi đạt ngưỡng. Tuy nhiên, không có ứng dụng nào có thể đảm bảo 100% tuổi thọ pin và các yếu tố khác như nhiệt độ, cách sử dụng cũng ảnh hưởng đến sức khỏe pin.

