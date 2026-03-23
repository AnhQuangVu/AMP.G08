# Tổng quan dự án: TILT ARENA (AMPG08)

Tài liệu này tổng hợp cấu trúc thư mục và các tính năng đã được triển khai trong dự án Android Studio.

## 1. Thông tin chung
- **Tên dự án:** TILT ARENA
- **Package:** `com.example.ampg08`
- **Ngôn ngữ:** Java
- **Phong cách thiết kế:** Neon/Cyberpunk (Sử dụng font Orbitron, Rajdhani và hiệu ứng Particle).

## 2. Cấu trúc thư mục (Source Code)

### Thư mục gốc: `com.example.ampg08`
- **Activities chính:**
    - `MainActivity.java`: Màn hình menu chính (Play, Score, Settings, Profile).
    - `GameActivity.java`: Màn hình điều phối, nhận dữ liệu map từ setup và truyền vào GameView.
    - `LoginActivity.java`: Xử lý đăng nhập người dùng.
    - `SplashActivity.java`: Màn hình chào khi khởi động ứng dụng.
    - `PlaySetupActivity.java`: Thiết lập trận đấu (chọn chế độ, bản đồ).
    - `ResultActivity.java`: Hiển thị kết quả sau mỗi ván chơi.
    - `ScoreActivity.java`: Hiển thị bảng xếp hạng/điểm cao.
    - `SettingsActivity.java`: Tùy chỉnh cài đặt game.
    - `BaseActivity.java`: Lớp cơ sở chứa các thiết lập chung cho Activity.

### Package: `view` (Custom Views)
- `GameView.java`: **Trái tim của gameplay**. Chứa vòng lặp Game Loop (Thread), xử lý vẽ (Canvas) và tính toán vật lý/va chạm thực tế.
- `ParticleView.java`: View xử lý hiệu ứng hạt (particles) chuyển động làm nền cho menu.

### Package: `model` (Dữ liệu)
- `MapModel.java`: Định nghĩa cấu trúc dữ liệu bản đồ (tọa độ tường, chướng ngại vật).
- `ScoreModel.java`: Cấu trúc dữ liệu cho thông tin điểm số.

### Package: `adapter` (Hiển thị danh sách)
- `MapAdapter.java`: Quản lý hiển thị danh sách bản đồ trong phần setup.
- `ScoreAdapter.java`: Quản lý hiển thị danh sách điểm số trong bảng xếp hạng.

## 3. Tài nguyên (Resources - `res`)

### Layouts (`layout`)
- `activity_main.xml`: Giao diện menu chính với các nút Neon.
- `activity_game.xml`: Giao diện trong trận đấu.
- `activity_play_setup.xml`: Giao diện chọn map và chế độ chơi (sử dụng Fragments).
- `item_map_card.xml`: Giao diện hiển thị từng thẻ bản đồ.
- `pause_overlay.xml`: Giao diện tạm dừng game.

## 4. Xử lý Logic Game & Bản đồ (Quy trình)
1. **Dữ liệu bản đồ:** Được lưu trữ trong `MapModel`.
2. **Khởi tạo:** `GameActivity` nhận ID bản đồ được chọn, khởi tạo `GameView` với dữ liệu tương ứng.
3. **Vòng lặp Game (Game Loop):** Chạy trong `GameView.java`.
    - **Update:** Tính toán vị trí Player dựa trên cảm biến nghiêng (Tilt Sensor). Kiểm tra va chạm với các vật thể trong map.
    - **Draw:** Vẽ lại khung cảnh, người chơi và các hiệu ứng Neon lên Canvas mỗi 16ms (~60 FPS).
4. **Kết thúc:** Khi đạt điều kiện thắng/thua, `GameView` thông báo cho `GameActivity` để chuyển sang `ResultActivity`.

## 5. Những gì đã thực hiện
1. **Giao diện người dùng (UI):** Hoàn thiện bộ UI mang phong cách hiện đại, sử dụng hiệu ứng hạt động và các nút bấm hiệu ứng Neon.
2. **Cấu trúc ứng dụng:** Thiết lập bộ khung Activity/Fragment rõ ràng, tách biệt logic (Model-View-Adapter).
3. **Hệ thống Setup:** Triển khai luồng thiết lập trận đấu qua nhiều bước (chọn mode -> chọn map).
4. **Game Engine cơ bản:** Xây dựng `GameView` và `ParticleView` để xử lý đồ họa tùy chỉnh.

---
*Tài liệu được cập nhật tự động dựa trên cấu trúc hiện tại của dự án.*