# CLAUDE.md

## Quản lý task

**Đầu mỗi session:** đọc `tasks/task-index.md`. Chỉ mở file `tasks/task-<ngày>.md` nào có trạng thái chưa xong (`todo` / `in-progress` / `testing`); không đọc các file đã `done`.

**Khi có task mới liên quan UI hoặc logic ứng dụng** (yêu cầu nhỏ như build, cấu hình, docs, câu hỏi thì không tạo task):
1. Phân tích, lên plan xong thì tạo `tasks/task-<YYYY-MM-DD>.md` (theo ngày tạo; trùng ngày thì thêm hậu tố `-2`, `-3`...). Nội dung: tiêu đề, yêu cầu, plan dạng checklist `- [ ]`, file liên quan, cách test, trạng thái.
2. Thêm 1 dòng vào `tasks/task-index.md`.
3. Trong lúc làm: tick `- [x]` từng bước, cập nhật trạng thái ở cả file task và index.

**Trạng thái:** `todo` → `in-progress` → `testing` (code xong, chờ test) → `done` (đã test hoàn chỉnh).

**Khi task `done`:** cập nhật `CHANGELOG.md` (mục `[Unreleased]`), rồi đổi trạng thái trong index thành `done`.

Mẫu dòng index (mỗi task đúng 1 dòng, giữ file ngắn):
`| task-2026-10-26.md | Caffeine giữ màn hình sáng | testing |`

## Build release APK

Mỗi lần build release (prod) PHẢI tăng version trước, rồi mới build từ version mới nhất:

1. Trong `app/build.gradle.kts` (`defaultConfig`):
   - `versionCode` tăng thêm 1.
   - `versionName` tăng số cuối (patch): `1.0` → `1.0.1`, `1.0.1` → `1.0.2`, ...
2. Trong `CHANGELOG.md`: đổi `## [Unreleased]` thành `## [<versionName mới>] - <YYYY-MM-DD>`, thêm lại mục `## [Unreleased]` trống ở trên.
3. Build: `./gradlew assembleRelease`
4. Kiểm tra chữ ký: `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`
5. Báo lại versionName/versionCode vừa build và đường dẫn APK.

Không tăng version khi build debug (`assembleDebug`).

## Signing

- Keystore: `keystore/androidtools-release.jks`, mật khẩu trong `keystore.properties` (gốc repo).
- Cả hai đã nằm trong `.gitignore` — không bao giờ commit, không in mật khẩu ra chat.
