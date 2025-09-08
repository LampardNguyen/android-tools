
## Task 1: Thiết kế kiến trúc ứng dụng
- Xác định pattern (MVP/MVVM)
- Tạo các package: ui, service, receiver, utils, model
- Setup SharedPreferences helper class

## **Phase 2: Giao diện người dùng (UI)**

### Task 2.1: Thiết kế MainActivity Layout
- Tạo switch bật/tắt tính năng thông báo
- SeekBar hoặc NumberPicker để chọn mức pin (0-100%)
- TextView hiển thị mức pin hiện tại
- TextView hiển thị trạng thái sạc
- Button test nhạc chuông thông báo

### Task 2.2: Tạo SettingsActivity
- Tùy chọn chọn nhạc chuông thông báo
- Điều chỉnh âm lượng thông báo
- Tùy chọn rung khi thông báo
- Cài đặt thời gian lặp lại thông báo sau bao nhiêu giây

### Task 2.3: Implement UI Logic
- Xử lý sự kiện người dùng
- Lưu settings vào SharedPreferences
- Validation input từ user
- Update UI real-time với pin status

## **Phase 3: Core Functionality - Monitoring Pin**

### Task 3.1: Tạo BatteryBroadcastReceiver
- Đăng ký receiver cho ACTION_BATTERY_CHANGED
- Lấy thông tin level pin hiện tại
- Lấy trạng thái sạc (đang sạc/không sạc)
- Xử lý logic khi đạt ngưỡng
- khi user rút sạc thì không phát nhạc chuông nữa và tắt thông báo

### Task 3.2: Implement BatteryMonitorService
- Tạo Foreground Service để chạy background
- Monitor pin level liên tục
- So sánh với ngưỡng user setting
- Trigger notification khi đạt điều kiện

### Task 3.3: Xử lý Permission
- Request BATTERY_STATS permission
- Request POST_NOTIFICATIONS (Android 13+)
- Request VIBRATE permission
- Handle permission denial cases

## **Phase 4: Notification System**

### Task 4.1: Tạo NotificationHelper Class
- Setup NotificationChannel (Android O+)
- Tạo notification với custom sound
- Implement notification với high priority
- Add action buttons (Dismiss, Snooze)

### Task 4.2: Implement AlarmManager
- Tạo alarm sound player
- Loop nhạc chuông cho đến khi user tắt
- Handle audio focus
- Xử lý các trường hợp đặc biệt (DND mode)

### Task 4.3: Tạo RingtoneManager
- Load danh sách ringtone hệ thống
- Cho phép user chọn custom sound
- Preview nhạc chuông được chọn
- Lưu URI của ringtone đã chọn

## **Phase 5: Background Processing**

### Task 5.1: Optimize Battery Usage
- Implement Doze mode compatibility
- Request battery optimization exemption
- Handle App Standby Buckets
- Setup WorkManager cho periodic check

### Task 5.2: Boot Receiver
- Tạo BootCompletedReceiver
- Auto-start service sau khi boot
- Restore user settings
- Request necessary permissions


## **Công nghệ và Tools đề xuất:**

- **Language:** Kotlin (recommended) hoặc Java
- **Min SDK:** API 21 (Android 5.0)
- **Architecture:** MVVM với LiveData
- **Dependency Injection:** Hilt/Dagger2
- **Background Processing:** WorkManager + Foreground Service
- **Database:** Room (nếu cần lưu history)

lưu ý sau khi thực hiện xong mỗi task thì tiến hành build để kiểm tra lỗi và tính tương thích