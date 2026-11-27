# PROMPT — CREATE SQL REFERENCE FROM JAVA + JDBC IMPLEMENTATION

## 1. MỤC TIÊU

Dự án Library Management System đã hoàn tất chuyển đổi từ kiến trúc sử dụng Database Views, Stored Procedures và Stored Functions sang kiến trúc Java + JDBC.

Hiện tại:
- Java Service chịu trách nhiệm xử lý nghiệp vụ.
- Java Repository sử dụng JDBC để truy vấn và thao tác trực tiếp trên các bảng MySQL.
- Ứng dụng không còn phụ thuộc vào Database Views.
- Ứng dụng không còn gọi Stored Procedures hoặc Stored Functions.

Tôi muốn tạo lại một bộ **SQL Reference độc lập**, tương ứng với những truy vấn và nghiệp vụ đang được triển khai trong Java.

Mục đích của bộ SQL này là:
- Tham khảo cách triển khai các truy vấn bằng SQL.
- Tham khảo cách triển khai nghiệp vụ bằng Stored Procedures và Functions.
- Đối chiếu giữa cách triển khai Java + JDBC và SQL.
- Phục vụ việc nghiên cứu, trình bày và đánh giá thiết kế cơ sở dữ liệu.

Đây là bộ SQL phục vụ tham khảo, không phải thành phần của hệ thống runtime.

## 2. QUY ĐỊNH VỀ VỊ TRÍ THƯ MỤC

Trước tiên, kiểm tra cấu trúc repository hiện tại.

Tạo thư mục chứa prompt tại:

`Prompts/SQL_Reference/`

Lưu prompt này thành:

`Prompts/SQL_Reference/CREATE_SQL_REFERENCE.md`

Tạo thư mục chứa SQL Reference tại cấp gốc repository:

`SQL_Reference/`

Thư mục `SQL_Reference/` phải nằm độc lập, cùng cấp với các thư mục hiện có như `Project/` và `Source/`.

Không đặt SQL Reference:
- Bên trong thư mục Java source.
- Bên trong package Service hoặc Repository.
- Bên trong thư mục chứa frontend.
- Bên trong thư mục chứa schema triển khai chính.
- Bên trong thư mục chứa sample data.

Cấu trúc đầu ra dự kiến:

```text
database_project/
│
├── Project/
│   ├── src/
│   ├── web/
│   ├── 01_create_database_final_v2.sql
│   ├── 02_insert_data_final_v2.sql
│   └── ...
│
├── Source/
│   └── ...
│
├── Prompts/
│   └── SQL_Reference/
│       └── CREATE_SQL_REFERENCE.md
│
└── SQL_Reference/
    ├── README.md
    ├── 01_views_reference.sql
    ├── 02_functions_reference.sql
    ├── 03_procedures_reference.sql
    ├── 04_verification_queries.sql
    └── tests/
        └── ...
```

Có thể điều chỉnh tên file hoặc bổ sung file nếu cần thiết, nhưng phải giữ nguyên nguyên tắc tách biệt giữa thư mục prompt, thư mục SQL Reference và mã nguồn ứng dụng.

Không di chuyển hoặc đổi tên các thư mục hiện tại chỉ để phù hợp với cấu trúc đề xuất.

## 3. NGUỒN THÔNG TIN VÀ THỨ TỰ ƯU TIÊN

Phải khảo sát repository trước khi tạo SQL.

Sử dụng thứ tự ưu tiên sau:

**Nguồn 1 — Java hiện tại:**

Đây là nguồn thông tin có độ ưu tiên cao nhất.

Kiểm tra:
- Toàn bộ Service.
- Toàn bộ Repository.
- Model và DTO.
- Validator.
- Các phương thức xử lý nghiệp vụ.
- Các câu SQL trong Java.
- Transaction management.
- Exception handling.
- API request và response liên quan đến nghiệp vụ.

Java hiện tại thể hiện hành vi thực tế của hệ thống sau chuyển đổi. SQL Reference phải phản ánh đúng hành vi này.

**Nguồn 2 — Database Schema:**

Sử dụng schema hiện tại để xác định:
- Table.
- Primary Key.
- Foreign Key.
- Unique Constraint.
- Check Constraint.
- Index.
- Column type.
- Nullable.
- Default value.
- Quan hệ giữa các bảng.

Không được tự suy đoán cấu trúc bảng nếu schema đã có định nghĩa.

**Nguồn 3 — Dữ liệu mẫu:**

Sử dụng sample data để hiểu các trường hợp dữ liệu và kiểm chứng các truy vấn.

**Nguồn 4 — SQL cũ:**

Có thể tham khảo:
- `views_final_v2.sql`
- `procedures_final_v2.sql`
- Các Stored Function cũ.

Tuy nhiên, các file này chỉ có giá trị tham khảo lịch sử.

Không được mặc định chúng phản ánh chính xác nghiệp vụ hiện tại.

Không sao chép nguyên trạng SQL cũ nếu nó không còn tương thích với Java hoặc schema hiện tại.

**Lưu ý:** Không sử dụng README cũ làm nguồn xác định kiến trúc hay nghiệp vụ hiện tại vì tài liệu đó có thể chưa được cập nhật sau quá trình tái cấu trúc.

## 4. GIAI ĐOẠN 1 — AUDIT VÀ LẬP BẢN ĐỒ CHUYỂN ĐỔI

Trước khi viết SQL, hãy kiểm kê toàn bộ mã nguồn.

### 4.1. Repository Audit

Với từng Repository, xác định:

- Tên Repository.
- Các phương thức truy vấn dữ liệu.
- Câu SQL tương ứng.
- Các bảng được sử dụng.
- Các phép JOIN.
- GROUP BY và Aggregate Functions.
- Các phép tính trong quá trình ánh xạ ResultSet.
- Điều kiện lọc.
- Thứ tự sắp xếp.
- Các trường dữ liệu trả về.

Phân loại từng truy vấn thành:

1. Truy vấn SELECT đơn giản.
2. Truy vấn JOIN nhiều bảng.
3. Truy vấn tổng hợp.
4. Truy vấn phục vụ kiểm tra điều kiện nghiệp vụ.
5. Truy vấn INSERT.
6. Truy vấn UPDATE.
7. Truy vấn DELETE.
8. Truy vấn phục vụ khóa dữ liệu và Transaction.

### 4.2. Service Audit

Với từng Service, xác định:

- Tên nghiệp vụ.
- Các Repository được gọi.
- Trình tự các truy vấn.
- Điều kiện kiểm tra.
- Công thức tính toán.
- Các nhánh xử lý.
- Các thao tác ghi dữ liệu.
- Transaction boundary.
- Điều kiện rollback.
- Các trường hợp thành công và thất bại.

### 4.3. Mapping

Tạo một bảng đối chiếu:

| Java Component | Java Method | Business/Query Purpose | SQL Reference |
|---|---|---|---|
| Repository | Method A | Retrieve data | View hoặc Query |
| Service | Method B | Borrow book | Procedure |
| Service | Method C | Return book | Procedure |
| Service | Method D | Check license | Function |

Đây chỉ là định dạng minh họa. Phải lập bảng từ mã nguồn thật.

Không được bỏ sót các phương thức chỉ vì chúng không trực tiếp được gọi từ API nếu chúng vẫn thuộc phạm vi nghiệp vụ của dự án.

Sau khi hoàn thành audit, tiếp tục triển khai SQL Reference. Không dừng lại chỉ ở báo cáo.

## 5. GIAI ĐOẠN 2 — TẠO DATABASE VIEWS REFERENCE

Tạo file:

`SQL_Reference/01_views_reference.sql`

Mục tiêu là tái hiện những truy vấn đọc dữ liệu quan trọng đang được Java Repository thực hiện.

### Yêu cầu

- Chuyển những truy vấn SELECT phức tạp, truy vấn JOIN nhiều bảng và truy vấn tổng hợp phù hợp thành View.
- Không bắt buộc chuyển mọi câu SELECT đơn giản thành View.
- Không tạo View chỉ để bao bọc một câu SELECT không cần thiết.
- Mỗi View phải có mục đích rõ ràng.
- Tên View phải nhất quán và dễ hiểu.

Có thể sử dụng tên View theo nhóm nghiệp vụ:
- Members.
- Resources.
- Books.
- Journals.
- Borrows.
- Fines.
- Access Logs.
- Granted Licenses.
- Authors.

Đây là các nhóm tham khảo, không phải danh sách bắt buộc. Phải căn cứ vào các truy vấn thực tế trong Java.

### Quy tắc tương đương

Mỗi View cần bảo toàn:

- Các trường dữ liệu.
- Alias.
- Kiểu dữ liệu.
- NULL semantics.
- Điều kiện JOIN.
- Điều kiện WHERE.
- GROUP BY.
- Aggregate Functions.
- HAVING.
- ORDER BY khi cần thiết cho truy vấn sử dụng View.
- Cardinality và kết quả tổng hợp.

Không được làm thay đổi kết quả nghiệp vụ chỉ để SQL ngắn hơn.

### Đặc biệt: RESOURCE và các subtype

RESOURCE có thể đồng thời có:
- PHYSICAL_BOOK.
- DIGITAL_JOURNAL.

Không được giả định một RESOURCE chỉ có duy nhất một subtype.

Khi xây dựng View:
- Không tạo JOIN khiến Resource bị nhân bản ngoài ý muốn.
- Không loại bỏ Resource chỉ có Physical Book.
- Không loại bỏ Resource chỉ có Digital Journal.
- Không loại bỏ Resource có đồng thời cả hai subtype.
- Phải giữ đúng quan hệ giữa RESOURCE, PHYSICAL_BOOK, DIGITAL_JOURNAL và BOOK_COPY.

## 6. GIAI ĐOẠN 3 — TẠO STORED FUNCTIONS REFERENCE

Tạo file:

`SQL_Reference/02_functions_reference.sql`

Tìm toàn bộ logic kiểm tra hoặc tính toán dùng chung trong Java có thể được biểu diễn hợp lý bằng Stored Function.

Ví dụ quan trọng:

`fn_has_all_required_licenses`

Phải đối chiếu với logic Java hiện tại, không lấy nguyên định nghĩa Function cũ nếu đã thay đổi.

### Yêu cầu

- Function phải phản ánh đúng điều kiện trong Java.
- Giữ nguyên ý nghĩa kết quả trả về.
- Xử lý NULL đúng cách.
- Xử lý trường hợp không có dữ liệu.
- Không tự ý thay đổi điều kiện cấp quyền.
- Không tạo Function chỉ vì một đoạn code Java tồn tại nếu đoạn code đó không phù hợp với bản chất Stored Function.

Với từng Function, ghi rõ:

- Tên.
- Input Parameters.
- Return Type.
- Điều kiện xử lý.
- Các bảng được truy cập.
- Ý nghĩa nghiệp vụ.
- Java method tương ứng.

Nếu một logic không thể biểu diễn chính xác hoặc hợp lý bằng Stored Function, hãy ghi nhận lý do trong README thay vì tạo Function giả lập không tương đương.

## 7. GIAI ĐOẠN 4 — TẠO STORED PROCEDURES REFERENCE

Tạo file:

`SQL_Reference/03_procedures_reference.sql`

Chuyển các nghiệp vụ ghi dữ liệu hoặc nghiệp vụ phức tạp đang thực hiện trong Java Service thành Stored Procedure Reference tương ứng.

Kiểm tra toàn bộ Service hiện tại để xác định phạm vi.

Các nhóm cần đặc biệt đối chiếu với nghiệp vụ cũ:

### A. Borrow Book

Bao gồm những điều kiện thực tế đang được Java triển khai:
- Kiểm tra Member.
- Kiểm tra Borrowing Quota.
- Kiểm tra thời hạn mượn.
- Kiểm tra Borrow quá hạn.
- Kiểm tra Fine chưa thanh toán.
- Kiểm tra Book Copy.
- Kiểm tra trạng thái Book Copy.
- Kiểm tra điều kiện Author.
- Kiểm tra License.
- Tạo Borrow.
- Cập nhật Book Copy.

### B. Return Book

- Kiểm tra Borrow.
- Kiểm tra trạng thái trả.
- Kiểm tra ngày trả.
- Cập nhật Borrow.
- Cập nhật Book Copy.
- Tính Fine.
- Tạo hoặc cập nhật Fine.

### C. Fine Payment

- Kiểm tra Fine.
- Kiểm tra trạng thái thanh toán.
- Tạo Payment Transaction.
- Cập nhật Fine.
- Ngăn thanh toán trùng.

### D. Digital Journal Access

- Kiểm tra Digital Journal.
- Xử lý Guest.
- Kiểm tra Person.
- Kiểm tra Author.
- Kiểm tra Resource License.
- Kiểm tra Granted License.
- Xác định quyền truy cập.
- Ghi Access Log theo đúng điều kiện.

### E. License Management

- Grant License.
- Revoke License.
- Kiểm tra điều kiện tồn tại.
- Kiểm tra cấp trùng.
- Kiểm tra quyền thu hồi.

### F. Resource Access

Kiểm tra toàn bộ logic liên quan đến:
- Resource.
- License Package.
- Resource License.
- Granted License.
- Các điều kiện truy cập khác đang tồn tại trong Java.

Không tự ý mở rộng hoặc thay đổi chính sách truy cập.

### Quy tắc triển khai Stored Procedure

1. Sử dụng cú pháp tương thích với phiên bản MySQL của dự án.
2. Sử dụng `DELIMITER` phù hợp để tạo Procedure.
3. Khai báo Input và Output Parameters rõ ràng.
4. Dùng transaction cho nghiệp vụ cần tính nguyên tử.
5. Dùng `SELECT ... FOR UPDATE` tại những vị trí cần khóa dữ liệu.
6. Sử dụng cùng transaction và cùng Connection trong một nghiệp vụ.
7. Có xử lý lỗi và rollback phù hợp.
8. Chống xử lý trùng khi nghiệp vụ yêu cầu.
9. Không dựa hoàn toàn vào kiểm tra trước đó để bảo vệ dữ liệu.
10. Tôn trọng PK, FK, UNIQUE, CHECK và các ràng buộc hiện tại.

### Transaction và sự khác biệt với Java

Java hiện tại có thể chia một nghiệp vụ thành nhiều truy vấn trên cùng Connection.

Khi chuyển thành Stored Procedure, phải xác định chính xác:
- Transaction được bắt đầu ở đâu.
- Transaction được kết thúc ở đâu.
- Procedure có tự quản lý transaction hay sử dụng transaction của caller.
- Có nguy cơ commit hoặc rollback ngoài phạm vi nghiệp vụ hay không.

Không tạo các Procedure có hành vi transaction không rõ ràng.

Nếu một nghiệp vụ trong Java phụ thuộc vào API validation hoặc một hành vi không thể tái hiện đầy đủ trong MySQL, hãy ghi rõ giới hạn đó.

## 8. GIAI ĐOẠN 5 — TẠO FILE VERIFICATION

Tạo file:

`SQL_Reference/04_verification_queries.sql`

Mục tiêu là cung cấp các câu truy vấn để kiểm tra tính nhất quán của bộ SQL Reference.

Bao gồm:

- Kiểm tra các View.
- Kiểm tra kết quả JOIN.
- Kiểm tra quan hệ giữa RESOURCE và các subtype.
- Kiểm tra Book Copy.
- Kiểm tra Borrow.
- Kiểm tra Fine.
- Kiểm tra Payment Transaction.
- Kiểm tra License.
- Kiểm tra Access Log.
- Kiểm tra các trường hợp NULL.
- Kiểm tra các dữ liệu không có quan hệ tương ứng.

Nếu phù hợp, tạo các test SQL độc lập để kiểm tra Stored Procedures và Functions.

Không được để các truy vấn kiểm thử làm thay đổi dữ liệu thật mà không có cơ chế rollback hoặc dữ liệu kiểm thử riêng.

## 9. GIAI ĐOẠN 6 — README CHO SQL REFERENCE

Tạo:

`SQL_Reference/README.md`

Tài liệu cần giải thích:

### 9.1. Purpose

Giải thích SQL Reference được tạo ra để tham khảo và đối chiếu với kiến trúc Java + JDBC.

### 9.2. Directory Structure

Mô tả chức năng từng file.

### 9.3. Java-to-SQL Mapping

Cung cấp bảng đối chiếu giữa:
- Java Repository.
- Java Service.
- Java method.
- SQL View.
- SQL Function.
- SQL Procedure.

### 9.4. Execution Instructions

Hướng dẫn cách chạy bộ SQL Reference trên một database thử nghiệm.

Nêu rõ thứ tự thực thi:
1. Schema hiện tại.
2. Views.
3. Functions.
4. Procedures.
5. Verification Queries.

Không tự động chạy những file này khi khởi động ứng dụng.

Không thêm các file Reference vào DatabaseInitializer.

### 9.5. Important Notes

Ghi rõ:
- SQL Reference không phải nguồn triển khai chính.
- Java + JDBC vẫn là kiến trúc runtime duy nhất.
- SQL Reference không được ứng dụng gọi.
- Thay đổi Java trong tương lai có thể khiến SQL Reference không còn đồng bộ.
- Cần cập nhật mapping khi nghiệp vụ Java thay đổi.

## 10. GIAI ĐOẠN 7 — KIỂM TRA TÍNH TƯƠNG ĐƯƠNG

Sau khi tạo các file SQL, hãy kiểm tra:

### 10.1. Syntax

- Tạo được tất cả View.
- Tạo được tất cả Function.
- Tạo được tất cả Procedure.
- Không có lỗi cú pháp.
- Không có lỗi dependency.
- Thứ tự tạo SQL Object hợp lệ.

### 10.2. Functional Equivalence

Đối chiếu kết quả SQL Reference với Java:

- Các truy vấn trả về cùng dữ liệu.
- Các điều kiện lọc tương đương.
- Các phép tính tương đương.
- Các thao tác ghi dữ liệu có kết quả tương đương.
- Các điều kiện nghiệp vụ thành công và thất bại tương đương.
- Transaction và ràng buộc dữ liệu được bảo toàn trong phạm vi có thể so sánh.

### 10.3. Test Cases

Phải kiểm tra các trường hợp:
- Dữ liệu hợp lệ.
- Dữ liệu không tồn tại.
- NULL.
- Dữ liệu trùng.
- Vi phạm FK.
- Vi phạm UNIQUE.
- Mượn sách thành công và thất bại.
- Trả sách đúng hạn và quá hạn.
- Thanh toán Fine.
- Truy cập Digital Journal.
- Grant và Revoke License.
- Resource có cả hai subtype.

### 10.4. Traceability

Mỗi SQL Reference Object phải có ít nhất một liên kết đối chiếu với Java method hoặc truy vấn Java tương ứng.

Không tạo View, Function hoặc Procedure không có mục đích và không có nguồn đối chiếu.

Nếu có logic Java không thể chuyển chính xác sang SQL, phải ghi rõ trong báo cáo.

## 11. GIỚI HẠN PHẠM VI — KHÔNG ĐƯỢC VI PHẠM

Đây là những quy định bắt buộc:

**A. Không thay đổi ứng dụng**
- Không sửa Java.
- Không sửa frontend.
- Không sửa API.
- Không sửa JSON contract.
- Không thay đổi Service.
- Không thay đổi Repository.
- Không thay đổi cấu hình runtime.

**B. Không thay đổi database triển khai**
- Không sửa schema chính.
- Không sửa sample data.
- Không thêm View vào schema chính.
- Không thêm Procedure vào schema chính.
- Không thêm Function vào schema chính.
- Không thay đổi ERD.
- Không thay đổi PK, FK, UNIQUE, CHECK hoặc INDEX.

**C. Không thay đổi cấu trúc dự án hiện tại**
- Không di chuyển Java source.
- Không di chuyển frontend.
- Không đổi tên các file SQL triển khai.
- Không ghi đè SQL cũ.
- Không xóa các file lịch sử.

**D. Phạm vi được phép thay đổi**

Chỉ được:
- Tạo thư mục `Prompts/SQL_Reference/`.
- Lưu prompt trong thư mục đó.
- Tạo thư mục `SQL_Reference/`.
- Tạo các file SQL Reference bên trong.
- Tạo tài liệu và test phục vụ SQL Reference bên trong thư mục này.

## 12. BÁO CÁO KẾT QUẢ

Sau khi hoàn thành, hãy cung cấp:

1. Cấu trúc thư mục được tạo.
2. Danh sách SQL Views.
3. Danh sách Stored Functions.
4. Danh sách Stored Procedures.
5. Bảng mapping Java → SQL.
6. Các nghiệp vụ đã chuyển đổi thành công.
7. Các nghiệp vụ không thể tái hiện hoàn toàn bằng SQL và nguyên nhân.
8. Kết quả kiểm tra cú pháp.
9. Kết quả kiểm tra tương đương.
10. Những giới hạn còn tồn tại.

Không được tuyên bố hoàn thành nếu chỉ tạo file SQL mà chưa kiểm tra khả năng thực thi.

Nếu MySQL chưa được cấu hình hoặc không có database thử nghiệm, hãy phân biệt rõ:
- Đã kiểm tra bằng phân tích mã nguồn.
- Đã kiểm tra cú pháp.
- Đã thực thi thực tế.
- Chưa thể kiểm thử.

**ĐIỀU KIỆN HOÀN THÀNH:**

Bộ SQL Reference phải là một bản triển khai tham khảo độc lập, có thể đọc, hiểu, triển khai và kiểm thử riêng mà không tác động đến Java + JDBC runtime của dự án.
