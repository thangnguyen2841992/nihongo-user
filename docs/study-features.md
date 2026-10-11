# Tiến độ và ôn tập cá nhân

Đã nối backend `nihongo-user` với trang **Học hôm nay**, **Ôn tập**, **Sổ tay** và **Lịch sử làm bài** của `nihongo-app-fe`.

## Hành vi

- Dashboard: số bài đã luyện (bài có ít nhất một lần nộp), tổng lượt nộp, điểm trung bình, số bài yếu theo lần nộp mới nhất, điểm trung bình từng ngày trong 30 ngày.
- Mục tiêu: 0–200 thẻ và 0–20 lượt làm bài/ngày; ít nhất một mục tiêu phải lớn hơn 0. Hoàn thành khi đạt tất cả mục tiêu đang bật. Luyện riêng câu sai được đếm riêng, không tạo điểm hay lượt nộp giả cho toàn bài.
- Mỗi thẻ/câu sai chỉ tính một lần/ngày dù ôn nhiều lần. Chuỗi ngày học tính ngày có ôn thẻ, luyện câu sai hoặc nộp bài; hôm nay chưa học vẫn giữ chuỗi đến hôm qua. Ngày nghỉ làm đứt chuỗi.
- Flashcard: Quên = 10 phút; Nhớ = 1/3/7/14/30 ngày theo số lần nhớ liên tiếp; Khó = một nửa lịch chuẩn (bắt đầu 12 giờ); Dễ = gấp đôi lịch chuẩn. Quên đặt lại số lần nhớ liên tiếp. Endpoint mới khóa thẻ trong giao dịch và từ chối ôn sớm/gửi lặp sau khi lịch đã chuyển.
- Luyện câu sai: tải câu hỏi gốc, chọn A–D, backend kiểm tra lại đáp án hiện tại và quyền học qua dịch vụ staff. Đúng thì bỏ câu khỏi danh sách, sai thì cập nhật đáp án đã chọn. Nút **Đã nắm vững** cho phép bỏ thủ công, không tính là một hoạt động học.
- Nhắc học **trong ứng dụng trên trang Học hôm nay**: số thẻ đến hạn và khóa ACTIVE còn hạn nhưng hết hạn trong 7 ngày tới; có tùy chọn bật/tắt được lưu theo tài khoản. Nhắc được tính khi mở/tải lại trang, không gửi email hoặc thông báo ngoài ứng dụng.
- Phân trang tại server: sổ tay (lọc loại thẻ trên toàn bộ dữ liệu), câu sai, bài yếu, lịch sử theo bài. Thẻ đến hạn tải theo lượt tối đa 30 thẻ, tự lấy lượt tiếp theo khi hết. Thống kê và biểu đồ trang lịch sử chỉ áp dụng các lượt trên trang đang xem; dashboard tổng hợp toàn bộ lượt.

Ngày học dùng cùng múi giờ JVM với thời điểm nộp bài hiện có; dashboard trả `today` và `timeZone`. Khi triển khai cho người học Việt Nam, cấu hình JVM `-Duser.timezone=Asia/Ho_Chi_Minh` nhất quán giữa các phiên bản. Lịch sử nộp bài sẵn có góp vào chuỗi ngày; thẻ/câu sai chỉ bắt đầu được ghi hoạt động kể từ bản này.

## API mới

Tất cả yêu cầu xác thực ADMIN/STAFF/USER; chủ dữ liệu luôn lấy từ JWT subject.

| Method | Path (tiền tố `/api/nihongo-user`) | Nội dung |
|---|---|---|
| GET | `/study/dashboard` | Dashboard, mục tiêu, chuỗi ngày, nhắc học |
| PUT | `/study/goal` | `{dailyCards,dailyAttempts,remindersEnabled}` |
| GET | `/study/cards/page?page=0&size=20&kind=VOCAB` | Sổ tay, kind tùy chọn |
| GET | `/study/cards/due/batch?size=30` | Lượt thẻ đến hạn |
| POST | `/study/cards/{id}/rating` | `{rating:"AGAIN"/"HARD"/"GOOD"/"EASY"}` |
| GET | `/study/weak-lessons/page?page=0&size=20` | Bài yếu, không tải snapshot bài nộp |
| GET | `/study/wrong-answers/page?page=0&size=20` | Danh sách câu sai |
| POST | `/study/wrong-answers/{id}/practice` | `{answer:"A"/"B"/"C"/"D"}` |
| DELETE | `/study/wrong-answers/{id}` | Đánh dấu đã nắm vững |
| GET | `/lesson-result/{lessonId}/page?page=0&size=20` | Lịch sử phân trang |

Page trả `{content,totalElements,number,totalPages}`; số trang từ 0; size từ 1–100. API cũ được giữ để tương thích. `/study/overview` dùng truy vấn tổng hợp thay vì đọc toàn bộ lịch sử và snapshot. Thứ tự ổn định khi thời gian bằng nhau nhờ ID; bài yếu lấy lần nộp có ID lớn nhất khi trùng thời điểm.

## Cơ sở dữ liệu và chạy lại

Thêm bảng `study_preference`, `study_activity` và hai index cho `user_exercise_attempt`. Cấu hình hiện tại `ddl-auto=update` tạo các mục này khi khởi động lại backend. Môi trường quản lý schema thủ công dùng `study-features-migration.sql` trước khi chạy bản mới. Không sửa hoặc xóa dữ liệu lịch sử.

Build lại frontend và khởi động lại `nihongo-user` để sử dụng API mới. Kiểm thử dùng H2 trong bộ nhớ và API giả lập; không thay đổi cơ sở dữ liệu triển khai.
