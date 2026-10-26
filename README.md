# Android Tools

Bộ công cụ nhỏ gọn cho Android: cảnh báo pin khi sạc, nút âm lượng nổi và giữ màn hình sáng.

## ⬇️ Tải APK

**[👉 Tải bản mới nhất tại trang Releases](https://github.com/LampardNguyen/android-tools/releases/latest)**

Mở link trên điện thoại → kéo xuống mục **Assets** → chọn file `.apk` để tải và cài đặt (cần cho phép "Cài ứng dụng không rõ nguồn gốc").

Sau khi cài, ứng dụng tự kiểm tra và cập nhật phiên bản mới từ GitHub Releases.

## 📱 Tính năng

### 🔋 Battery Alert
- Cảnh báo khi pin sạc đạt ngưỡng (mặc định 80%) để bảo vệ pin
- Âm thanh, rung, số lần phát và khoảng lặp tùy chỉnh
- Thông báo có nút Dismiss / Snooze, tự dừng khi rút sạc
- Chạy nền và tự khởi động lại sau khi bật máy

### 🔊 Float Volume
- Nút điều chỉnh âm lượng nổi trên màn hình, kéo thả được

### ☕ Caffeine
- Giữ màn hình luôn sáng, bật/tắt nhanh từ Quick Settings

## ⚙️ Yêu cầu

- Android 5.0 (API 21) trở lên
- Để hoạt động ổn định: cấp quyền thông báo và **tắt tối ưu hóa pin** cho ứng dụng

## 🐛 Khắc phục sự cố

- **Không có thông báo**: kiểm tra quyền thông báo và chế độ Không làm phiền.
- **Ứng dụng bị dừng khi chạy nền**: tắt tối ưu hóa pin; một số máy (Xiaomi, Oppo...) cần bật thêm "Tự khởi động".
- **Không có âm thanh**: kiểm tra âm lượng và chế độ im lặng.

## 🛠️ Build từ mã nguồn

```bash
./gradlew assembleDebug
```

## 📄 License

MIT — xem [LICENSE](LICENSE).
