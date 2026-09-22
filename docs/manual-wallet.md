# Nạp tiền thủ công

## Cách sử dụng

1. Người dùng đăng nhập, mở `/wallet`, tạo yêu cầu từ 1.000 đến 100.000.000 VNĐ (số nguyên). Backend lấy UUID người dùng từ JWT, không nhận chủ ví từ trình duyệt.
2. Trang hiển thị mã `NAP{id}`. Người dùng chuyển đúng số tiền và nội dung này tới tài khoản được cấu hình. Nếu chưa cấu hình tài khoản, giao diện yêu cầu liên hệ quản trị viên; không hiển thị thông tin ngân hàng giả.
3. ADMIN mở `/admin/wallet-deposits` (menu **Đối soát nạp tiền**), đối chiếu sao kê với số tiền và nội dung NAP.
4. Nhập mã giao dịch trên sao kê, đánh dấu đã nhận đủ tiền, chọn xác nhận. Nếu không hợp lệ, nhập lý do rồi từ chối. Ảnh chụp chuyển khoản không thay thế việc kiểm tra sao kê.
5. Người dùng bấm **Cập nhật** để xem số dư và trạng thái. Yêu cầu chờ duyệt không tăng số dư.

## Cấu hình

Đặt các biến môi trường cho `nihongo-user`, rồi khởi động lại:

- `WALLET_BANK_NAME`: ngân hàng nhận tiền.
- `WALLET_BANK_ACCOUNT`: số tài khoản.
- `WALLET_BANK_ACCOUNT_NAME`: tên chủ tài khoản.

Không cần cổng thanh toán. Chỉ role ADMIN có quyền đối soát; STAFF và USER không có quyền này. Backend vẫn thực thi phân quyền nếu người dùng tự gọi API.

## Database và dữ liệu ví cũ

Tài khoản trong service `user` dùng UUID dạng chuỗi. Code ví trước đây lại dùng Long, nên không thể xác định chủ sở hữu ví cũ chỉ từ số đó.

- Cột `user_wallet.user_id` nay là VARCHAR(255).
- Bảng mới `wallet_deposit` lưu yêu cầu, khóa chống gửi trùng, mã giao dịch ngân hàng duy nhất và người/thời gian đối soát.
- `wallet_transaction` tiếp tục lưu lịch sử số dư trước/sau, tham chiếu tới `wallet_deposit`.
- File `wallet-manual-migration.sql` là script cho DBA kiểm tra và áp dụng trước khi khởi chạy bản mới. Chưa chạy script lên database của bạn. Cấu hình hiện tại của ứng dụng có `ddl-auto: update`, nên Hibernate cũng có thể thay đổi schema khi khởi chạy.
- Nếu ví cũ có số dư thật, sao lưu và đối chiếu chủ sở hữu trước khi triển khai. Các ID số cũ được giữ lại dưới dạng chuỗi; không tự chuyển số dư sang UUID hoặc gán ví ID 0 cho bất kỳ tài khoản nào. Việc đối chiếu cần dữ liệu ngoài mã nguồn.
- Triển khai BE và FE cùng phiên bản vì API nạp trả về yêu cầu thay vì số dư.

## Tính nhất quán

- Tạo ví lần đầu dùng upsert theo khóa duy nhất `user_id` và khóa ghi trong transaction.
- Một UUID `requestKey` dùng lại với cùng dữ liệu trả về yêu cầu cũ. Dùng cùng khóa với số tiền/ghi chú khác trả 409. FE giữ khóa và dữ liệu khi mất kết nối, kể cả tải lại trang trong cùng tab.
- Phê duyệt khóa ví và yêu cầu; số dư, lịch sử và trạng thái được cập nhật trong một transaction.
- Duyệt lại cùng yêu cầu với cùng mã ngân hàng không cộng thêm tiền.
- Hai yêu cầu không được dùng chung mã giao dịch ngân hàng. Unique constraint đảm bảo trường hợp duyệt đồng thời cũng rollback lần trùng.
- Mã ngân hàng được trim, viết hoa; hãy dùng nhất quán mã tham chiếu duy nhất trên sao kê (có thể kèm mã ngân hàng nếu cần).
- Từ chối là trạng thái kết thúc. Không dùng nút từ chối để hoàn tiền cho yêu cầu đã được xác nhận; hoàn tiền là nghiệp vụ khác.
- Danh sách người dùng hiển thị 100 yêu cầu mới nhất. Danh sách ADMIN ưu tiên 100 yêu cầu chờ lâu nhất và 100 yêu cầu đã xử lý gần nhất; tải lại để lấy nhóm tiếp theo sau khi xử lý.

## Kiểm thử

Tại thư mục cha `microservice`:

```text
mvn -f wallet-verification-pom.xml -Dtest=Wallet*Test -Dsurefire.failIfNoSpecifiedTests=false test
```

File này build `common-security` cùng `nihongo-user`, không cần cài thư viện dùng chung trước. Bộ test gồm logic nghiệp vụ, phân quyền HTTP và transaction/concurrency trên H2 chế độ MySQL. Test H2 sử dụng database trong bộ nhớ riêng, không kết nối MySQL cấu hình của ứng dụng. H2 không thay thế hoàn toàn kiểm thử MySQL staging trước khi triển khai thật.

Tại `nihongo-app-fe`:

```text
npm run build
npm run test:unit -- --run src/users/__tests__/Wallet.spec.ts
```

Chưa thực hiện chuyển khoản, chưa đối soát giao dịch thật và chưa chạy migration lên database đang cấu hình.
