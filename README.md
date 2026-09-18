# AlignAI Studio - Android Native (Aligh-AI-ADR)

> **Hệ Thống Máy Ảnh Bố Cục Thông Minh & Tracking Không Gian Tự Động**  
> Porting trực tiếp từ phiên bản iOS (`AISmartFramingCamera`) sang nền tảng **Android Native** bằng **Kotlin**, **Jetpack Compose**, **CameraX**, và **Google ML Kit / TensorFlow Lite**.

---

## 🌟 Tính Năng Cốt Lõi (Core Features)

### 1. Kiến Trúc Kính Ngắm Bất Biến (Non-distorting Viewfinder Architecture)
- **Live View cố định**: Sử dụng CameraX `PreviewView` đặt tại tầng nền đáy (`Box` hierarchy), duy trì đúng tỷ lệ nguyên bản của cảm biến (`4:3` cho chế độ Ảnh, `16:9` cho chế độ Video).
- **Floating Overlays**: Bảng điều khiển Pro Drawer, HUD trạng thái, biểu đồ Histogram và các vạch bố cục được xếp chồng dạng floating overlay. Mọi thao tác đóng/mở Pro Drawer **tuyệt đối không** làm dịch chuyển, co nhỏ hay bóp méo kính ngắm.

### 2. Hệ Thống AI Target Tracking & Lấy Nét Tự Động
- **Nhận diện thời gian thực**: Kết hợp Google ML Kit Face Detection và Stream Object Tracking đạt tốc độ 30 FPS.
- **Bộ lọc làm mượt Adaptive 1-Euro Filter**:
  - Triệt tiêu 100% rung giật vi mô sinh học khi giữ yên máy ($f_c = 1.50\text{ Hz}$).
  - Thích nghi tức thì khi lia máy với $\beta = 1.80$, bám dính chuyển động không độ trễ.
- **Dung hợp Cảm biến Quán tính (Visual-Inertial Fusion)**: Sử dụng con quay hồi chuyển 60Hz (`TYPE_GYROSCOPE`) bù trừ góc quay dead-reckoning khi chủ thể bị che khuất hoặc camera quét nhanh.
- **Khóa nét CameraX**: Tâm mỏ neo được kết nối trực tiếp vào `FocusMeteringAction` để tự động điều chỉnh nét và đo sáng liên tục.

### 3. Động Cơ Bố Cục Toán Học (Composition Engine)
- **Quy tắc 1/3 (Rule of Thirds)**: Tự động tính toán 4 điểm vàng, ưu tiên hướng nhìn của ánh mắt chủ thể.
- **Tỷ lệ vàng (Golden Ratio 1:1.618)**: Căn chuẩn theo tỷ lệ $\Phi \approx 1.618$.
- **Xoắn ốc Fibonacci (Golden Spiral)**: Xác định 4 tâm xoắn ốc và chọn tiêu điểm gần nhất.
- **Tâm đối xứng (Center Symmetry)**: Căn đối xứng hoàn hảo tại $(0.5, 0.5)$.
- **Dynamic AI**: Tự động nhận diện bối cảnh để chọn quy tắc tối ưu.
- **Auto-Zoom Thông Minh**: Tự động tính tỷ lệ kích thước chủ thể và đề xuất mức phóng đại quang học ($1.3\times, 1.6\times, 2.0\times, 2.5\times$).

### 4. Động Cơ Màu Sắc Studio & 18 Presets Phim
Đầy đủ 18 bộ màu chuẩn Leica / Hasselblad / Phim cổ điển qua GPU ColorMatrix:
1. **Standard Clean**: Chân thực dải sáng tối đa
2. **Fuji Pro 400H**: Tone xanh pastel nhẹ, da trắng hồng
3. **Kodak Portra 400**: Tone ấm vàng dịu, chuyển màu da êm ái
4. **Classic Chrome**: Phong cách phóng sự tài liệu
5. **Cinema Teal & Orange**: Tương phản điện ảnh Hollywood
6. **Fuji Velvia 50**: Bùng nổ sắc màu thiên nhiên
7. **Sunset Glow**: Rực rỡ ánh hoàng hôn ven tóc
8. **Tokyo Clean**: Mơ màng phong cách Nhật Bản
9. **HK Cinema 90s**: Đậm chất Vương Gia Vệ
10. **CineStill 800T**: Điện ảnh đêm, quầng sáng ấm
11. **Leica Monochrom**: Đen trắng thuần khiết Leica
12. **Noir High Contrast**: Tương phản kịch tính
13. **Kodak Tri-X 400**: Hạt phim báo chí đời thường
14. **Vintage Warm 70s**: Hoài niệm thập niên 70
15. **Street Classic**: Sắc nét đường phố tương phản cao
16. **Nordic Minimal**: Tối giản lạnh thanh khiết
17. **Kodak Ektar 100**: Siêu sắc nét đỏ và xanh dương
18. **Cyberpunk Night**: Tím hồng neon viễn tưởng
19. **AI Full Auto Color**: Tự động phân tích và tối ưu theo cảnh vật

### 5. Chế Độ Pro Video Chuyên Nghiệp
- Điều chỉnh thủ công: **ISO**, **Shutter Speed**, **EV Bias**, **White Balance (Kelvin)**, **Manual Focus (Lens Position)**.
- Tích hợp **Focus Peaking** báo nét neon trên cạnh vật thể.
- Biểu đồ **Realtime Color Histogram** (RGB & Luma).

---

## 🛠️ Yêu Cầu Môi Trường & Biên Dịch

- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **JDK**: Java 17 (Temurin)
- **Gradle**: 8.7
- **Android Gradle Plugin**: 8.3.2
- **Kotlin**: 1.9.23

### Biên dịch Local:
```bash
./gradlew assembleRelease
```
File APK xuất xưởng tại: `app/build/outputs/apk/release/app-release.apk`

---

## 🚀 CI/CD Tự Động Hóa (GitHub Actions)
Repository được tích hợp sẵn pipeline tự động hóa tại `.github/workflows/android.yml`:
- Chạy trên `ubuntu-latest`
- Tự động đóng gói Release APK khi push lên nhánh `main`
- Tải artifact và tự động phát hành bản Release trên GitHub.