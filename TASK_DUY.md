# NHIỆM VỤ CÁ NHÂN – DUY

**Vai trò:** Duy – Backend đăng nhập, payment và bảo mật

## 1. Danh sách task được giao

- **D08 – Khóa thanh toán giả lập khi deploy thật:** Nút/API giả lập chuyển khoản chỉ bật ở bản demo nội bộ; bản đưa lên mạng không được tự đánh dấu đã thanh toán.
- **D09 – Chống tạo đơn hoặc thanh toán hai lần:** Khóa nút gửi nhiều lần, dùng mã idempotency và kiểm tra chữ ký/số tiền callback từ cổng thanh toán.
- **D10 – Hoàn thiện thanh toán thử nghiệm:** Chạy VNPAY/MoMo sandbox, tạo hoàn tiền cơ bản, đối chiếu trạng thái và trả hàng/mã khi hủy.
- **G01 – Xóa và đổi các mật khẩu đã lộ:** Tìm khóa DB/JWT/payment/Gemini từng ghi trong code, đổi khóa mới và không ghi giá trị vào báo cáo.
- **G02 – Không để mật khẩu trong mã nguồn:** Đọc secret từ env; khi deploy thiếu biến quan trọng thì báo lỗi rõ và không chạy bằng mật khẩu mặc định.
- **G03 – Đăng nhập bằng cookie an toàn:** Ưu tiên HttpOnly cookie, token ngắn, đăng xuất/đổi mật khẩu làm token cũ mất hiệu lực; có thể làm sau khi luồng đăng nhập cơ bản chạy.
- **G04 – Sửa quên mật khẩu:** Không trả token trực tiếp; gửi link qua email, trả thông báo chung và chỉ dùng link một lần; MFA admin là phần mở rộng.
- **G05 – Giới hạn số lần gọi API:** Giới hạn đăng nhập, đăng ký, quên mật khẩu, chatbot, gợi ý, coupon và payment theo IP hoặc tài khoản.
- **G06 – Bảo vệ API nội bộ và dữ liệu chat:** Không cho người lạ ghi rating/chat hoặc xem phiên chat của tài khoản khác.
- **G07 – Bảo vệ phòng WebSocket:** Từ chối kết nối thiếu token và chỉ cho user/admin vào đúng phòng của mình.
- **G08 – Ẩn lỗi kỹ thuật khỏi người dùng:** Lỗi 500 trả thông báo chung và mã lỗi; log không in token, mật khẩu, SQL hoặc dữ liệu cá nhân.
- **G09 – Thiết lập bảo mật trình duyệt cơ bản:** Chỉ cho domain đúng gọi API, bật HTTPS headers và cấu hình cookie/CORS phù hợp.
- **G10 – Kiểm tra quyền truy cập:** Viết test để user không xem/sửa đơn, chat, payment hoặc dữ liệu admin của người khác.
- **H04 – Trang quản lý người dùng:** Admin khóa/mở khóa và đổi vai trò; không cho tự khóa mình hoặc xóa admin cuối.
- **H07 – Trang quản lý thanh toán:** Xem lịch sử, trạng thái lệch, hoàn tiền và không hiện nút giả lập trên bản deploy thật.

## 2. Kế hoạch thực hiện từ 12/07/2026 đến 15/07/2026

## Ngày 12/07/2026

**Mục tiêu chung của ngày:** chạy được mã nguồn, sửa dữ liệu/cấu hình, deploy frontend và chatbot bản đầu.

- **G01 – Xóa và đổi các mật khẩu đã lộ**
  - Việc cần làm: Tìm khóa DB/JWT/payment/Gemini từng ghi trong code, đổi khóa mới và không ghi giá trị vào báo cáo.
- **G02 – Không để mật khẩu trong mã nguồn**
  - Việc cần làm: Đọc secret từ env; khi deploy thiếu biến quan trọng thì báo lỗi rõ và không chạy bằng mật khẩu mặc định.
- **G08 – Ẩn lỗi kỹ thuật khỏi người dùng**
  - Việc cần làm: Lỗi 500 trả thông báo chung và mã lỗi; log không in token, mật khẩu, SQL hoặc dữ liệu cá nhân.
- **D08 – Khóa thanh toán giả lập khi deploy thật**
  - Việc cần làm: Nút/API giả lập chuyển khoản chỉ bật ở bản demo nội bộ; bản đưa lên mạng không được tự đánh dấu đã thanh toán.

## Ngày 13/07/2026

**Mục tiêu chung của ngày:** deploy backend/database, hoàn thiện API nền và nối các dịch vụ.

- **G03 – Đăng nhập bằng cookie an toàn**
  - Việc cần làm: Ưu tiên HttpOnly cookie, token ngắn, đăng xuất/đổi mật khẩu làm token cũ mất hiệu lực; có thể làm sau khi luồng đăng nhập cơ bản chạy.
- **G04 – Sửa quên mật khẩu**
  - Việc cần làm: Không trả token trực tiếp; gửi link qua email, trả thông báo chung và chỉ dùng link một lần; MFA admin là phần mở rộng.
- **G06 – Bảo vệ API nội bộ và dữ liệu chat**
  - Việc cần làm: Không cho người lạ ghi rating/chat hoặc xem phiên chat của tài khoản khác.
- **G09 – Thiết lập bảo mật trình duyệt cơ bản**
  - Việc cần làm: Chỉ cho domain đúng gọi API, bật HTTPS headers và cấu hình cookie/CORS phù hợp.

## Ngày 14/07/2026

**Mục tiêu chung của ngày:** hoàn thiện luồng mua hàng, chatbot và quản trị; kiểm tra lỗi chính.

- **D09 – Chống tạo đơn hoặc thanh toán hai lần**
  - Việc cần làm: Khóa nút gửi nhiều lần, dùng mã idempotency và kiểm tra chữ ký/số tiền callback từ cổng thanh toán.
- **G05 – Giới hạn số lần gọi API**
  - Việc cần làm: Giới hạn đăng nhập, đăng ký, quên mật khẩu, chatbot, gợi ý, coupon và payment theo IP hoặc tài khoản.
- **G07 – Bảo vệ phòng WebSocket**
  - Việc cần làm: Từ chối kết nối thiếu token và chỉ cho user/admin vào đúng phòng của mình.
- **G10 – Kiểm tra quyền truy cập**
  - Việc cần làm: Viết test để user không xem/sửa đơn, chat, payment hoặc dữ liệu admin của người khác.
- **H04 – Trang quản lý người dùng**
  - Việc cần làm: Admin khóa/mở khóa và đổi vai trò; không cho tự khóa mình hoặc xóa admin cuối.

## Ngày 15/07/2026

**Mục tiêu chung của ngày:** hoàn thiện phần còn lại, chạy demo toàn hệ thống và sửa lỗi.

- **D10 – Hoàn thiện thanh toán thử nghiệm**
  - Việc cần làm: Chạy VNPAY/MoMo sandbox, tạo hoàn tiền cơ bản, đối chiếu trạng thái và trả hàng/mã khi hủy.
- **H07 – Trang quản lý thanh toán**
  - Việc cần làm: Xem lịch sử, trạng thái lệch, hoàn tiền và không hiện nút giả lập trên bản deploy thật.

## 4. Yêu cầu cá nhân
- Commit nên ghi mã task, ví dụ: `feat(b04): chọn màu và dung lượng trên trang sản phẩm`.
