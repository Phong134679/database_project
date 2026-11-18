# Digital Library Management System & Open Repository
> Hệ thống Quản lý Thư viện số và Lưu trữ mở — Dự án Cơ sở dữ liệu & Ứng dụng Java tương tác MySQL.

Dự án được xây dựng theo mô hình kiến trúc **MVC (Model - View - Controller)** kết hợp **Repository Pattern**, kết nối và xử lý dữ liệu trực tiếp với hệ quản trị cơ sở dữ liệu **MySQL**.

---

## Những Điểm Mới & Cập nhật Nâng cấp Dự án (Changelog & Upgrades)

So với phiên bản khung cơ bản ban đầu, dự án đã được nâng cấp và hoàn thiện toàn diện với các bổ sung trọng tâm:

* **Tách bạch & Tối ưu luồng kết nối CSDL**: Tách riêng chức năng xác thực tài khoản MySQL Server và kết nối đến database cụ thể; bọc tập trung trong khối `try-catch`, tự động ném Exception khi gặp lỗi kết nối và yêu cầu người dùng khởi tạo thủ công nếu database trả về `null`.
* **Bổ sung Modun Validation Chuyên biệt (`com.library.validation`)**: Phân tách toàn bộ logic kiểm tra dữ liệu và ràng buộc nghiệp vụ ra khỏi Service (gồm `DataValidator`, `MemberValidator`, `BorrowValidator`, `FineValidator`, `LicenseValidator`).
* **Bổ sung Modun Exception Chuẩn hóa (`com.library.exception`)**: Xây dựng cây ngoại lệ kế thừa `RuntimeException`, trang bị mã lỗi nghiệp vụ riêng (`errorCode`) cho từng quy chế (BR01, BR02, BR04, BR05, BR10, BR11); bọc `SQLException` thành `DatabaseException` ném lên tầng Service.
* **Bổ sung Bộ Kiểm thử Đơn vị (`com.library.test`)**: Cung cấp bộ test độc lập [`ValidationAndExceptionTest.java`](Project/src/com/library/test/ValidationAndExceptionTest.java) chạy 13 test cases tự động kiểm chứng toàn bộ ngoại lệ mà không phụ thuộc CSDL live.
* **Hiện thực trọn vẹn 4 Validation Cases**: Hoàn thành 4 kịch bản kiểm thử nghiệp vụ theo SRS Phase 1 trên MySQL thật (phân quyền mượn sách BR01/BR05, kiểm soát bản quyền tài liệu số BR04/BR12/BR13, tự động sinh phạt trễ hạn FR05/BR02, và thanh toán trả góp tiền phạt FR06/BR11).
* **Mở rộng kiến trúc MVC + Repository Pattern**: Hiện thực đầy đủ 15 Model POJOs, 9 Repositories và 4 Services bao quát toàn bộ 15 bảng dữ liệu trong CSDL.
* **Tiện ích khởi tạo CSDL & Chuẩn hóa cấu hình**: Cung cấp công cụ [`DatabaseInitializer.java`](Project/src/com/library/util/DatabaseInitializer.java) hỗ trợ nạp schema và dữ liệu mẫu; chuẩn hóa classpath tương đối cho Eclipse và cung cấp template cấu hình mẫu an toàn.

---

## 1. Hướng dẫn Setup & Cài đặt Môi trường (Setup Guide)

Mục này dành cho mọi thành viên trong nhóm khi clone mã nguồn về máy, giúp phân định rõ những gì **cần cài đặt thêm** và những gì **đã có sẵn trong dự án**, đảm bảo môi trường và thư viện hoạt động đồng nhất.

### 1.1. Những thành phần ĐÃ CÓ SẴN trong mã nguồn (Không cần tải thêm)
* **Thư viện MySQL JDBC Driver**:
  * File [`Project/lib/mysql-connector-j-26.7.0.jar`](Project/lib/mysql-connector-j-26.7.0.jar) đã được tích hợp sẵn trong thư mục `lib/` của dự án.
  * Đã được cấu hình liên kết sẵn trong file [`.classpath`](Project/.classpath) của Eclipse bằng đường dẫn tương đối. Bạn **không cần** phải tải thêm file `.jar` từ bên ngoài.
* **Cấu hình dự án Eclipse**:
  * Các file metadata dự án [`.project`](Project/.project), [`.classpath`](Project/.classpath) và [`.settings`](Project/.settings) đã chuẩn hóa cấu trúc source folder (`src/`), output folder (`bin/`), bảng mã UTF-8 và liên kết thư viện driver.
* **Script Cơ sở Dữ liệu**:
  * [`Project/schema.sql`](Project/schema.sql): Script tạo database `library_db` và định nghĩa 15 bảng chuẩn quan hệ.
  * [`Project/sample_data.sql`](Project/sample_data.sql): Script nạp dữ liệu mẫu bao quát toàn bộ thực thể và các ca kiểm thử nghiệp vụ.
* **Công cụ Khởi tạo Cơ sở Dữ liệu Tự động**:
  * [`Project/src/com/library/util/DatabaseInitializer.java`](Project/src/com/library/util/DatabaseInitializer.java): Tiện ích nạp tự động `schema.sql` và `sample_data.sql` qua JDBC mà không cần cài đặt dòng lệnh MySQL CLI.
* **Cấu hình mẫu**:
  * [`Project/src/db.properties.example`](Project/src/db.properties.example): Template mẫu cấu hình kết nối database an toàn khi đẩy lên Git.

---

### 1.2. Những phần mềm BẮT BUỘC PHẢI CÀI ĐẶT trên máy cá nhân
Trước khi chạy dự án, máy tính của bạn cần đảm bảo đã cài đặt các phần mềm sau:

1. **Java Development Kit (JDK 11 trở lên)**:
   * *Yêu cầu*: Tối thiểu JDK 11 (khuyến nghị OpenJDK 11, JDK 17 LTS, JDK 21 LTS hoặc mới hơn).
   * *Kiểm tra phiên bản trên máy*:
     ```bash
     java -version
     javac -version
     ```
   * *Cài đặt trên Ubuntu/Debian (Linux)*:
     ```bash
     sudo apt update
     sudo apt install openjdk-11-jdk
     ```
   * *Cài đặt trên Windows / macOS*: Tải bộ cài từ [Eclipse Adoptium (Temurin)](https://adoptium.net/) hoặc Oracle JDK.

2. **Hệ quản trị CSDL MySQL Server (MySQL 8.0 trở lên)**:
   * *Yêu cầu*: MySQL Server đang khởi chạy trên cổng `3306`. *(Không bắt buộc phải dùng MySQL CLI vì dự án đã hỗ trợ tự động khởi tạo bằng Java)*.
   * *Kiểm tra dịch vụ MySQL*:
     ```bash
     sudo systemctl status mysql
     ```
   * *Cài đặt trên Ubuntu/Debian (Linux)*:
     ```bash
     sudo apt update
     sudo apt install mysql-server
     sudo systemctl start mysql
     ```
   * *Cài đặt trên Windows*: Sử dụng bộ cài MySQL Installer (chọn MySQL Server 8.0) hoặc XAMPP/WampServer (chú ý cấu hình port 3306).

3. **Môi trường phát triển (IDE)**:
   * **Khuyến nghị**: **Eclipse IDE for Java Developers** (vì dự án đã chứa sẵn cấu hình `.project` và `.classpath` tương thích 100%).
   * *Lựa chọn thay thế*: Có thể sử dụng **IntelliJ IDEA** hoặc **VS Code** (kèm bộ extension *Extension Pack for Java*).

---

### 1.3. Các bước thiết lập & Chạy dự án từng bước (Step-by-Step)

#### Bước 1: Khởi tạo Cơ sở Dữ liệu MySQL

Trước khi khởi chạy hệ thống, cơ sở dữ liệu `library_db` cần được khởi tạo theo một trong hai cách sau:

* **Cách 1: Khởi tạo qua tiện ích Java (Khuyên dùng)**:
  * Sau khi cấu hình file `db.properties` (ở Bước 2), bạn mở file [`Project/src/com/library/util/DatabaseInitializer.java`](Project/src/com/library/util/DatabaseInitializer.java) trong Eclipse $\rightarrow$ Nhấp chuột phải chọn **Run As** $\rightarrow$ **Java Application**.
  * Tiện ích sẽ kết nối qua JDBC, tạo CSDL `library_db`, chạy `schema.sql` tạo 15 bảng và chạy `sample_data.sql` nạp dữ liệu mẫu hoàn tất trong vài giây.

* **Cách 2: Khởi tạo Thủ công qua Terminal / MySQL CLI**:
  * Nếu muốn chủ động khởi tạo bằng dòng lệnh MySQL CLI:
    ```bash
    # 1. Tạo database library_db và 15 bảng
    mysql -u root -p < Project/schema.sql

    # 2. Nạp dữ liệu mẫu
    mysql -u root -p < Project/sample_data.sql
    ```

#### Bước 2: Cấu hình thông tin kết nối Cơ sở Dữ liệu
1. Mở file [`Project/src/db.properties`](Project/src/db.properties).
2. Điền chính xác tài khoản (`db.username`) và mật khẩu (`db.password`) MySQL trên máy của bạn:
   ```properties
   db.url=jdbc:mysql://localhost:3306/library_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
   db.username=root
   db.password=ĐIỀN_MẬT_KHẨU_MYSQL_CỦA_BẠN
   db.driver=com.mysql.cj.jdbc.Driver
   ```

#### Bước 3: Mở dự án trong Eclipse & Đồng bộ thư viện
1. Khởi động Eclipse $\rightarrow$ Chọn menu **File** $\rightarrow$ **Import...**
2. Chọn **General** $\rightarrow$ **Existing Projects into Workspace** $\rightarrow$ Bấm **Next**.
3. Tại ô **Select root directory**, bấm **Browse...** và trỏ đến thư mục `Project` (nằm bên trong thư mục `database`). Bấm **Finish**.
4. **Đồng bộ thư viện & File mới**:
   * Nhấp chuột phải vào tên dự án `Project` trong cửa sổ *Package Explorer* $\rightarrow$ Chọn **Refresh** (hoặc nhấn phím **`F5`**).
   * Kiểm tra trong mục **Referenced Libraries**: Đảm bảo file `mysql-connector-j-26.7.0.jar` đã hiển thị bình thường.
   * (Nếu có cảnh báo): Vào menu **Project** $\rightarrow$ **Clean...** $\rightarrow$ Bấm **Clean**.

#### Bước 4: Chạy kiểm thử ứng dụng
* Mở file [`Project/src/com/library/Main.java`](Project/src/com/library/Main.java).
* Nhấp chuột phải $\rightarrow$ Chọn **Run As** $\rightarrow$ **Java Application**.
* Ứng dụng sẽ thực hiện:
  1. Kiểm tra xác thực tài khoản MySQL Server.
  2. Kiểm tra kết nối tới database `library_db` (nếu kết nối trả về `null` sẽ in thông báo yêu cầu người dùng khởi tạo thủ công).
  3. Khi kết nối thành công, tự động thực thi trực tiếp toàn bộ **4 Validation Cases** (Test 01 đến Test 04) với kết quả chi tiết trên Console.

---

## 2. Tổng quan Kiến trúc Hệ thống

Hệ thống được tổ chức phân tầng chặt chẽ theo mô hình **MVC** kết hợp **Repository Pattern**:

```
                       [ LibraryController ]                  <- Tầng Điều phối Sự kiện (Controller)
                                 │
         ┌───────────────────────┼───────────────────────┬──────────────────────┐
         ▼                       ▼                       ▼                      ▼
  [ MemberService ]       [ BorrowService ]       [ FineService ]    [ DigitalJournalService ]  <- Tầng Nghiệp vụ (Service)
         │                       │                       │                      │
         ├───────────────────────┴───────────────────────┼──────────────────────┘
         ▼                                               ▼
  [ PersonRepository ]                            [ FineRepository ]
  [ UniversityMemberRepository ]                  [ PaymentTransactionRepository ]
  [ BorrowRepository ]                            [ DigitalJournalRepository ]         <- Tầng Truy xuất Dữ liệu (9 Repositories)
  [ LicensePackageRepository ]                    [ GrantedLicenseRepository ]
  [ AccessLogRepository ]
         │                                               │
         ├───────────────────────────────────────────────┘
         ▼
[ DatabaseConnection ] ── [ DatabaseInitializer ]
         │                        │
         ▼                        ▼
[ 15 Model POJOs ]  ──▶  [ MySQL Database: library_db ]
                         (schema.sql & sample_data.sql)
```

---

## 3. Chi tiết các thành phần đã được hiện thực (Coded Components)

### 3.1. Tầng Cơ sở Dữ liệu (MySQL Layer)
* [`Project/schema.sql`](Project/schema.sql):
  * Định nghĩa **15 bảng** chuẩn InnoDB, mã hóa UTF-8 MB4.
  * Hiện thực quan hệ kế thừa (Inheritance/Subtypes):
    * `PERSON` (Superclass) $\rightarrow$ `UNIVERSITY_MEMBER` & `EXTERNAL_AUTHOR` (Subclasses).
    * `RESOURCE` (Superclass) $\rightarrow$ `PHYSICAL_BOOK` & `DIGITAL_JOURNAL` (Subclasses).
  * Các bảng quan hệ nhiều-nhiều (N:M): `AUTHOR_OF`, `RESOURCE_LICENSE`.
  * Nghiệp vụ mượn trả & tài chính: `BOOK_COPY`, `BORROW`, `FINE`, `PAYMENT_TRANSACTION`.
  * Phân quyền bản quyền tài liệu số: `LICENSE_PACKAGE`, `GRANTED_LICENSE`.
  * **Tối ưu hiệu năng ghi (Ràng buộc BR13)**: Bảng `ACCESS_LOG` tách rời Foreign Key khỏi Master tables, dùng Independent Indexes để tối ưu tốc độ ghi log tần suất cao.
* [`Project/sample_data.sql`](Project/sample_data.sql):
  * Dữ liệu mẫu kiểm thử bao quát toàn bộ 15 bảng và 4 kịch bản nghiệp vụ (Validation Cases): Phân quyền mượn sách theo vai trò người dùng, kiểm soát hạn bản quyền số, tự động phạt trễ hạn, và thanh toán trả góp tiền phạt.

---

### 3.2. Tầng Thực thể Dữ liệu (`com.library.model`)
Gồm **15 lớp POJO** ánh xạ 1:1 với cấu trúc bảng trong MySQL, đầy đủ attributes, constructors (no-arg và parameterized), getters/setters, và phương thức `toString()`:

| Nhóm | Lớp Model (POJO) | Mô tả |
| :--- | :--- | :--- |
| **Người dùng** | [`Person.java`](Project/src/com/library/model/Person.java) | Superclass lưu trữ thông tin cá nhân dùng chung |
| | [`UniversityMember.java`](Project/src/com/library/model/UniversityMember.java) | Kế thừa `Person`, đại diện cho Sinh viên hoặc Giảng viên |
| | [`ExternalAuthor.java`](Project/src/com/library/model/ExternalAuthor.java) | Kế thừa `Person`, đại diện cho Tác giả bên ngoài |
| **Tài nguyên** | [`Resource.java`](Project/src/com/library/model/Resource.java) | Superclass danh mục tài nguyên chung |
| | [`PhysicalBook.java`](Project/src/com/library/model/PhysicalBook.java) | Kế thừa `Resource`, đại diện cho Sách in |
| | [`DigitalJournal.java`](Project/src/com/library/model/DigitalJournal.java) | Kế thừa `Resource`, đại diện cho Tạp chí / Bài báo số |
| | [`BookCopy.java`](Project/src/com/library/model/BookCopy.java) | Bản sao vật lý của sách kèm mã vạch và trạng thái |
| **Bản quyền** | [`LicensePackage.java`](Project/src/com/library/model/LicensePackage.java) | Danh mục các gói bản quyền tài liệu số |
| | [`GrantedLicense.java`](Project/src/com/library/model/GrantedLicense.java) | Quyền truy cập gói tài liệu cấp cho người dùng |
| | [`ResourceLicense.java`](Project/src/com/library/model/ResourceLicense.java) | Bảng kết hợp (N:M) giữa tài nguyên số và gói bản quyền |
| **Tác giả** | [`AuthorOf.java`](Project/src/com/library/model/AuthorOf.java) | Bảng kết hợp (N:M) tác giả và tài nguyên |
| **Mượn & Phạt** | [`Borrow.java`](Project/src/com/library/model/Borrow.java) | Thông tin giao dịch mượn trả sách |
| | [`Fine.java`](Project/src/com/library/model/Fine.java) | Phiếu phạt phát sinh khi vi phạm quy chế |
| | [`PaymentTransaction.java`](Project/src/com/library/model/PaymentTransaction.java) | Lịch sử các đợt thanh toán tiền phạt |
| **Nhật ký** | [`AccessLog.java`](Project/src/com/library/model/AccessLog.java) | Lịch sử truy cập và tải tài liệu số |

---

### 3.3. Tầng Tiện ích & Kết nối Cơ sở Dữ liệu (`com.library.util`)
* [`DatabaseConnection.java`](Project/src/com/library/util/DatabaseConnection.java):
  * Nạp cấu hình tự động từ file [`db.properties`](Project/src/db.properties).
  * Tách riêng 2 chức năng kết nối:
    * Kết nối tài khoản MySQL Server (`getServerConnection()`, `testServerConnection()`) để kiểm tra quyền truy cập trước.
    * Kết nối đến database `library_db` (`getConnection()`, `testDatabaseConnection()`).
  * Cấp phát kết nối JDBC an toàn cho toàn bộ tầng Repository.
* [`DatabaseInitializer.java`](Project/src/com/library/util/DatabaseInitializer.java):
  * Tiện ích độc lập hỗ trợ kết nối qua JDBC để tạo CSDL `library_db` (UTF-8 MB4), nạp và thực thi các script [`schema.sql`](Project/schema.sql) và [`sample_data.sql`](Project/sample_data.sql) khi cần khởi tạo thủ công.
* [`db.properties.example`](Project/src/db.properties.example): File cấu hình mẫu để các thành viên thiết lập tài khoản MySQL cá nhân mà không sợ lộ mật khẩu lên Git.

---

### 3.4. Tầng Truy xuất Dữ liệu (`com.library.repository`)
Hệ thống hiện thực đầy đủ **9 lớp Repository** tương tác trực tiếp với MySQL qua `PreparedStatement` và cơ chế tự động đóng tài nguyên `try-with-resources`:

* [`BorrowRepository.java`](Project/src/com/library/repository/BorrowRepository.java): Quản lý bảng `BORROW`, đếm số sách đang mượn (`countActiveBorrowsByMember`), kiểm tra trễ hạn chưa trả (`hasOverdueBorrows`), kiểm tra và đổi trạng thái trong `BOOK_COPY`.
* [`FineRepository.java`](Project/src/com/library/repository/FineRepository.java): Thao tác bảng `FINE`, lấy ID tự sinh, cập nhật trạng thái thanh toán, kiểm tra độc giả có nợ phạt chưa nộp (`hasUnpaidFinesByMember`).
* [`PaymentTransactionRepository.java`](Project/src/com/library/repository/PaymentTransactionRepository.java): Lưu các lần thanh toán trả góp vào `PAYMENT_TRANSACTION`, tính tổng lũy kế đã nộp bằng `COALESCE(SUM(amount_paid), 0.00)`.
* [`PersonRepository.java`](Project/src/com/library/repository/PersonRepository.java): Quản lý bảng cha `PERSON`, tra cứu theo `person_id`, `ssn` và xác định vai trò (`person_type`).
* [`UniversityMemberRepository.java`](Project/src/com/library/repository/UniversityMemberRepository.java): Quản lý bảng con `UNIVERSITY_MEMBER`, liên kết với `PERSON` để lấy thông tin chi tiết sinh viên/giảng viên.
* [`DigitalJournalRepository.java`](Project/src/com/library/repository/DigitalJournalRepository.java): Quản lý tài liệu số `DIGITAL_JOURNAL` kết hợp bảng cha `RESOURCE` (DOI, file size, URL, định dạng).
* [`LicensePackageRepository.java`](Project/src/com/library/repository/LicensePackageRepository.java): Quản lý các gói bản quyền tài liệu số (`LICENSE_PACKAGE`) và liên kết `RESOURCE_LICENSE`.
* [`GrantedLicenseRepository.java`](Project/src/com/library/repository/GrantedLicenseRepository.java): Quản lý cấp phép `GRANTED_LICENSE`, truy vấn kiểm tra quyền đọc tài liệu số còn hạn hay không (`hasActiveLicenseForJournal`).
* [`AccessLogRepository.java`](Project/src/com/library/repository/AccessLogRepository.java): Lưu nhật ký truy cập vào bảng `ACCESS_LOG` với kiến trúc Decoupled Foreign Key tối ưu hiệu năng ghi (BR13).

---

### 3.5. Tầng Nghiệp vụ (`com.library.service`)
Hệ thống hiện thực đầy đủ **4 lớp Service** xử lý logic nghiệp vụ và kiểm tra các ràng buộc:

* [`MemberService.java`](Project/src/com/library/service/MemberService.java):
  * **Hiện thực Test 01 & Ràng buộc BR01, BR05**:
    * Chặn khách ngoài (`GUEST`, `EXTERNAL_AUTHOR`) mượn sách về nhà (chỉ đọc tại chỗ).
    * Chặn độc giả đang giữ sách trễ hạn chưa trả hoặc đang nợ tiền phạt chưa thanh toán.
    * Phân định hạn mức mượn (Sinh viên: tối đa 3 cuốn; Giảng viên: tối đa 10 cuốn).
    * Phân định thời hạn mượn (Sinh viên: 14 ngày; Giảng viên: 30 ngày).
* [`BorrowService.java`](Project/src/com/library/service/BorrowService.java):
  * Tích hợp `MemberService` kiểm tra điều kiện trước khi cho mượn (Test 01).
  * `processNewBorrow(int memberId, int copyId)`: Tạo phiếu mượn với thời hạn phân hóa theo vai trò.
  * `returnBook(int borrowId, LocalDate returnDate)`: **Hiện thực Test 03 (FR05, BR02)**: Tự động phát hiện trả trễ hạn và tạo phiếu phạt `FINE` với hạn nộp 14 ngày.
  * `calculateLatePenalty(int borrowId)`: Tính tiền phạt trễ hạn (10.000 VND / ngày).
* [`FineService.java`](Project/src/com/library/service/FineService.java):
  * **Hiện thực Test 04 & Ràng buộc BR11**: Xử lý trả góp tiền phạt theo từng đợt, tự động cập nhật trạng thái `Unpaid` $\rightarrow$ `Partially Paid` $\rightarrow$ `Completed`.
  * `getRemainingBalance(int fineId)` & `getPaymentHistory(int fineId)`: Theo dõi công nợ và lịch sử thanh toán.
* [`DigitalJournalService.java`](Project/src/com/library/service/DigitalJournalService.java):
  * **Hiện thực Test 02 & Ràng buộc BR04, BR12, BR13, FR07, FR08**:
    * `accessDigitalJournal(personId, journalId, ip, accessDate)`: Kiểm tra xem độc giả có sở hữu gói bản quyền còn hiệu lực cho tài liệu số này không. Nếu hết hạn $\rightarrow$ Chặn truy cập; nếu còn hạn $\rightarrow$ Cấp quyền tải file.
    * Tự động ghi nhận thông tin truy cập vào `ACCESS_LOG` (`download_success = true/false`, IP, thời gian).

---

### 3.6. Tầng Điều phối Sự kiện (`com.library.controller`)
* [`LibraryController.java`](Project/src/com/library/controller/LibraryController.java):
  * Điều phối toàn bộ các yêu cầu từ phía giao diện: mượn sách (Test 01), đọc tài liệu số (Test 02), trả sách sinh phạt (Test 03), thanh toán tiền phạt (Test 04), hiển thị danh mục và nhật ký truy cập.

---

### 3.7. Tầng Ngoại lệ Hệ thống (`com.library.exception`)
Xây dựng cây phân cấp ngoại lệ kế thừa từ `LibraryException` (Unchecked Exception):
* **Lỗi CSDL**: [`DatabaseException.java`](Project/src/com/library/exception/DatabaseException.java) (bọc `SQLException` từ tầng Repository ném lên Service).
* **Lỗi Dữ liệu (Validation)**: [`ValidationException.java`](Project/src/com/library/exception/ValidationException.java), [`InvalidInputException.java`](Project/src/com/library/exception/InvalidInputException.java), [`InvalidAmountException.java`](Project/src/com/library/exception/InvalidAmountException.java), [`InvalidDateRangeException.java`](Project/src/com/library/exception/InvalidDateRangeException.java).
* **Lỗi Nghiệp vụ (Business Rules)**:
  * [`ResourceNotFoundException.java`](Project/src/com/library/exception/ResourceNotFoundException.java): Thực thể không tồn tại.
  * [`IneligibleMemberException.java`](Project/src/com/library/exception/IneligibleMemberException.java): Vi phạm BR01 (khách ngoài mượn sách).
  * [`BorrowQuotaExceededException.java`](Project/src/com/library/exception/BorrowQuotaExceededException.java): Vi phạm BR05 (vượt quota mượn).
  * [`OverdueBooksPendingException.java`](Project/src/com/library/exception/OverdueBooksPendingException.java): Vi phạm BR05 (đang giữ sách quá hạn).
  * [`OutstandingFineException.java`](Project/src/com/library/exception/OutstandingFineException.java): Vi phạm BR05 (đang nợ tiền phạt).
  * [`BookNotAvailableException.java`](Project/src/com/library/exception/BookNotAvailableException.java): Vi phạm BR10 (bản sao không Available).
  * [`BookAlreadyReturnedException.java`](Project/src/com/library/exception/BookAlreadyReturnedException.java): Trả sách trùng lặp (BR02).
  * [`FineAlreadySettledException.java`](Project/src/com/library/exception/FineAlreadySettledException.java): Nộp tiền cho phiếu phạt đã tất toán (BR11).
  * [`LicenseExpiredException.java`](Project/src/com/library/exception/LicenseExpiredException.java): Gói bản quyền hết hạn khi đọc tài liệu số (BR04/BR12).

---

### 3.8. Tầng Kiểm tra Tính Hợp lệ (`com.library.validation`)
Bóc tách toàn bộ logic kiểm tra ra khỏi tầng Service:
* [`DataValidator.java`](Project/src/com/library/validation/DataValidator.java): Kiểm tra ID dương, chuỗi ký tự, số tiền `> 0`, dải ngày tháng hợp lệ.
* [`MemberValidator.java`](Project/src/com/library/validation/MemberValidator.java): Kiểm tra tồn tại người dùng, vai trò (BR01), sách quá hạn, nợ phạt, quota mượn (BR05).
* [`BorrowValidator.java`](Project/src/com/library/validation/BorrowValidator.java): Kiểm tra tình trạng bản sao sách (BR10) và điều kiện hoàn trả (BR02).
* [`FineValidator.java`](Project/src/com/library/validation/FineValidator.java): Kiểm tra số tiền phạt, thanh toán và trạng thái phiếu phạt (BR11).
* [`LicenseValidator.java`](Project/src/com/library/validation/LicenseValidator.java): Kiểm tra tài nguyên số và tính hiệu lực của gói bản quyền (BR04/BR12).

---

### 3.9. Chương trình Chạy Kiểm thử (`com.library.Main` & `ValidationAndExceptionTest`)
* [`Main.java`](Project/src/com/library/Main.java):
  * Điểm chạy chính (Entry Point) của ứng dụng.
  * Bọc 2 bước kiểm tra kết nối (tài khoản MySQL Server & database `library_db`) trong khối `try-catch` tập trung:
    * Ném `Exception` khi gặp sự cố kết nối tài khoản hoặc lỗi JDBC.
    * In thông báo yêu cầu người dùng khởi tạo thủ công nếu kết nối database trả về `null`.
  * Khởi tạo toàn bộ Repositories, Services và Controller.
  * Thực thi kịch bản kiểm thử toàn diện cả **4 Validation Cases** trực tiếp trên cơ sở dữ liệu thật và in log kết quả trực quan trên Console.
* [`ValidationAndExceptionTest.java`](Project/src/com/library/test/ValidationAndExceptionTest.java):
  * Bộ kiểm thử Unit Test độc lập chạy 13 kịch bản kiểm thử bao phủ toàn bộ các trường hợp ném ngoại lệ và kiểm tra hợp lệ của hệ thống.

---

## 4. Cấu trúc Thư mục Dự án

```
database/
├── .gitignore                                      # Cấu hình chặn file rác, metadata và file nhạy cảm
├── README.md                                       # Tài liệu mô tả dự án và hướng dẫn sử dụng
├── Source/                                         # Tài liệu đặc tả và thiết kế hệ thống
│   ├── G5_phase1.pdf                               # Báo cáo đặc tả yêu cầu giai đoạn 1 (SRS Phase 1)
│   ├── erDiagram.md                                # Bản vẽ mô hình thực thể quan hệ Mermaid ERD
│   └── 808050965_2457431158118432_...png           # Sơ đồ ERD trực quan
└── Project/                                        # Thư mục dự án Java (Eclipse Project)
    ├── .project                                    # Cấu hình project của Eclipse
    ├── .classpath                                  # Cấu hình classpath và thư viện của Eclipse
    ├── .settings/                                  # Cấu hình mã hóa UTF-8 và compiler compliance
    ├── schema.sql                                  # Kịch bản tạo CSDL library_db và 15 bảng InnoDB
    ├── sample_data.sql                             # Dữ liệu mẫu kiểm thử đầy đủ các thực thể
    ├── lib/
    │   └── mysql-connector-j-26.7.0.jar            # Driver kết nối MySQL JDBC
    └── src/
        ├── db.properties                           # File cấu hình kết nối MySQL cục bộ
        ├── db.properties.example                   # Template mẫu cấu hình kết nối cho thành viên mới
        └── com/
            └── library/
                ├── Main.java                       # File chạy demo kiểm thử toàn bộ luồng & 4 Validation Cases
                ├── controller/
                │   └── LibraryController.java      # Tầng điều phối các luồng nghiệp vụ
                ├── exception/                      # Tầng định nghĩa các ngoại lệ nghiệp vụ & CSDL
                │   ├── LibraryException.java       # Ngoại lệ gốc (Unchecked)
                │   ├── DatabaseException.java      # Bọc SQLException từ Repository
                │   ├── ValidationException.java    # Ngoại lệ kiểm tra dữ liệu
                │   ├── BusinessException.java      # Ngoại lệ vi phạm quy chế nghiệp vụ
                │   └── ...                         # Các ngoại lệ cụ thể (BR01, BR05, BR10, BR11, BR04)
                ├── validation/                     # Tầng kiểm tra tính hợp lệ & quy chế nghiệp vụ
                │   ├── DataValidator.java          # Kiểm tra kiểu dữ liệu, ID, tiền, dải ngày
                │   ├── MemberValidator.java        # Kiểm tra tư cách mượn sách (BR01, BR05)
                │   ├── BorrowValidator.java        # Kiểm tra tình trạng sách & hoàn trả (BR10, BR02)
                │   ├── FineValidator.java          # Kiểm tra phiếu phạt & trả góp (BR11)
                │   └── LicenseValidator.java       # Kiểm tra quyền tài liệu số (BR04, BR12)
                ├── service/                        # Tầng xử lý nghiệp vụ & điều phối validator
                │   ├── BorrowService.java          # Mượn, trả sách & tính phạt trễ hạn (Test 03)
                │   ├── FineService.java            # Xử lý trả góp tiền phạt theo đợt (Test 04)
                │   ├── MemberService.java          # Phân quyền mượn sách theo vai trò (Test 01)
                │   └── DigitalJournalService.java  # Bản quyền số & ghi log truy cập (Test 02)
                ├── repository/                     # Tầng truy xuất CSDL trực tiếp qua PreparedStatement
                │   ├── PersonRepository.java       # CRUD bảng PERSON
                │   ├── UniversityMemberRepository.java # CRUD bảng UNIVERSITY_MEMBER (JOIN PERSON)
                │   ├── BorrowRepository.java       # Thao tác BORROW, kiểm tra hạn mức & trễ hạn
                │   ├── FineRepository.java         # Thao tác FINE, kiểm tra nợ phạt chưa đóng
                │   ├── PaymentTransactionRepository.java # Thao tác PAYMENT_TRANSACTION & tính tổng nộp
                │   ├── DigitalJournalRepository.java # CRUD bảng DIGITAL_JOURNAL (JOIN RESOURCE)
                │   ├── LicensePackageRepository.java # Quản lý LICENSE_PACKAGE & RESOURCE_LICENSE
                │   ├── GrantedLicenseRepository.java # Quản lý GRANTED_LICENSE & kiểm tra thời hạn
                │   └── AccessLogRepository.java    # Ghi nhật ký truy cập tốc độ cao (BR13)
                ├── util/                           # Tầng tiện ích hệ thống
                │   ├── DatabaseConnection.java     # Cấp phát kết nối JDBC qua db.properties
                │   └── DatabaseInitializer.java    # Khởi tạo CSDL tự động qua JDBC
                ├── model/                          # 15 Lớp thực thể POJO ánh xạ 1:1 với CSDL
                │   ├── Person.java
                │   ├── UniversityMember.java
                │   ├── ExternalAuthor.java
                │   ├── Resource.java
                │   ├── PhysicalBook.java
                │   ├── DigitalJournal.java
                │   ├── BookCopy.java
                │   ├── LicensePackage.java
                │   ├── GrantedLicense.java
                │   ├── ResourceLicense.java
                │   ├── AuthorOf.java
                │   ├── Borrow.java
                │   ├── Fine.java
                │   ├── PaymentTransaction.java
                │   └── AccessLog.java
                └── test/                           # Bộ kiểm thử đơn vị
                    └── ValidationAndExceptionTest.java # Kiểm thử toàn diện 13 test cases cho Validation/Exception
```

---

## 5. Quy định Cộng tác Git (.gitignore)
* Hệ thống đã cấu hình đầy đủ file [`.gitignore`](.gitignore) ở thư mục gốc.
* **Những thành phần KHÔNG ĐƯỢC push lên Git**:
  * Thư mục cấu hình workspace của Eclipse: `/.metadata/`, `.recommenders/`.
  * Các file biên dịch nhị phân: `bin/`, `*.class`, `*.log`.
  * Thư mục cấu hình IDE cá nhân: `.idea/`, `.vscode/`, `*.iml`.
  * Các file rác hệ điều hành: `.DS_Store`, `Thumbs.db`.
  * File mật khẩu kết nối cá nhân: `Project/src/db.properties` (được chặn trên Git, chỉ chia sẻ mẫu qua `db.properties.example`).
