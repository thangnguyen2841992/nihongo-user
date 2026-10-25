# Phát âm tiếng Anh bằng Kokoro

Trang **Tiếng Anh cá nhân** (`/user/english`) đọc câu ví dụ ngữ pháp và mục **Luyện phát âm** bằng giọng AI Kokoro-82M (giấy phép Apache 2.0). Giọng Anh–Anh mặc định `bf_emma`, Anh–Mỹ `af_heart`.

Luồng: trình duyệt gọi `GET /api/nihongo-user/english-speech?text=…&voice=uk|us` (cần đăng nhập) → nihongo-user-service gọi Kokoro `POST /v1/audio/speech` → lưu audio (MP3, hoặc WAV) theo giọng + nội dung vào `${user.home}/.nihongo/kokoro-cache`. Câu đã đọc một lần thì lần sau lấy từ file, trình duyệt giữ thêm 7 ngày. Mỗi tài khoản tạo tối đa 20 câu **mới** mỗi phút (câu đã có trong cache không giới hạn), Kokoro chạy tối đa 2 câu cùng lúc.

Nếu Kokoro chưa chạy, API trả 503 và trang tự dùng giọng của trình duyệt (thử lại Kokoro sau 1 phút).

## Chạy Kokoro trên máy chạy nihongo-user-service (không cần Docker)

Dùng server nhỏ trong `scripts/kokoro/` (Python + [kokoro-onnx](https://pypi.org/project/kokoro-onnx/), chạy bằng ONNX Runtime trên CPU, không cần PyTorch, không cần GPU).

1. Cài **Python 3.10–3.13** từ python.org (khi cài tick “Add python.exe to PATH”). Đã có thì bỏ qua.
2. Ở thư mục gốc project, chạy trong PowerShell:

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\kokoro\start-kokoro.ps1
   ```

   Lần đầu script tự tạo môi trường `scripts\kokoro\.venv`, cài `kokoro-onnx` (+ `lameenc` để xuất MP3) và tải model `kokoro-v1.0.onnx` + `voices-v1.0.bin` (~300 MB) vào `scripts\kokoro\models`. Các lần sau khởi động trong vài giây. Thấy dòng `Kokoro ready on http://127.0.0.1:8880` là xong; để cửa sổ đó mở trong lúc học (Ctrl+C để tắt).
3. Kiểm tra: `curl http://127.0.0.1:8880/v1/audio/voices` phải có `bf_emma`, `af_heart`.
4. Khởi động lại nihongo-user-service, mở `/user/english`, bấm **Nghe câu ví dụ**. Chú thích dưới nút ghi “Giọng đọc AI Kokoro” là đang dùng Kokoro.

Server chỉ nghe ở `127.0.0.1`, đọc từng câu một (đã dùng hết nhân CPU). Nếu không cài được `lameenc`, server trả WAV thay MP3 — nihongo-user nhận cả hai, chỉ là file lớn hơn. Nếu báo lỗi liên quan `espeak`, cài espeak-ng bản Windows (.msi từ trang Releases của espeak-ng) rồi chạy lại.

**Chạy nền, tự bật khi đăng nhập Windows** (không cần mở PowerShell mỗi lần, không cần quyền admin): sau khi `start-kokoro.ps1` đã chạy được một lần, tắt cửa sổ đó (Ctrl+C) rồi chạy

```powershell
powershell -ExecutionPolicy Bypass -File scripts\kokoro\install-autostart.ps1
```

Kokoro chạy nền ngay (không có cửa sổ), và tự chạy mỗi lần đăng nhập (shortcut “Nihongo Kokoro” trong thư mục Startup). Log ở `scripts\kokoro\kokoro.log`. Gỡ: `scripts\kokoro\uninstall-autostart.ps1` (tắt Kokoro và bỏ khỏi Startup).

### Cách khác: Docker

`docker run -d --name kokoro --restart unless-stopped -p 127.0.0.1:8880:8880 ghcr.io/remsky/kokoro-fastapi-cpu:latest` ([Kokoro-FastAPI](https://github.com/remsky/Kokoro-FastAPI)). Cùng API, không cần đổi cấu hình.

## Cấu hình (biến môi trường)

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `KOKORO_URL` | `http://127.0.0.1:8880` | Địa chỉ Kokoro-FastAPI |
| `KOKORO_CACHE_DIR` | `${user.home}/.nihongo/kokoro-cache` | Thư mục audio đã tạo (cần quyền ghi; xóa được bất cứ lúc nào) |
| `KOKORO_VOICE_UK` | `bf_emma` | Giọng Anh–Anh (ví dụ `bf_isabella`, `bm_george`) |
| `KOKORO_VOICE_US` | `af_heart` | Giọng Anh–Mỹ (ví dụ `af_bella`, `am_michael`) |

Đổi giọng thì audio mới được tạo lại (khóa cache gồm tên giọng); trình duyệt có thể còn giữ bản cũ tối đa 7 ngày.

## Ghi âm so sánh

Mục **Luyện phát âm** cho phép ghi âm giọng người học (tối đa 15 giây) và nghe “mẫu rồi giọng tôi”. Bản ghi chỉ nằm trong bộ nhớ trình duyệt, không gửi lên server. Trình duyệt sẽ hỏi quyền micro; trang phải chạy qua HTTPS hoặc `localhost`.
