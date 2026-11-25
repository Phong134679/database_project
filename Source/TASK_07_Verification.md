# TASK 07 — Verification & Regression Testing

## Mục tiêu
Sau khi hoàn thành TASK 01–06, chạy kiểm tra đầy đủ để đảm bảo:
1. Tất cả test hiện tại vẫn pass ở mức baseline
2. Business logic do SP thực hiện hoạt động đúng
3. JSON API contract không bị phá vỡ
4. Web UI vẫn tương tác được bình thường

---

## Điều kiện tiên quyết

Trước khi chạy verification:
- [ ] TASK 01: `views.sql` đã deploy lên `library_db`
- [ ] TASK 02–05: Tất cả SP đã deploy lên `library_db`
- [ ] TASK 06: Java đã được refactor và compile thành công
- [ ] MySQL đang chạy với data mẫu đầy đủ
- [ ] `ApiServer` không đang chạy (kill trước nếu có)

---

## Bước 1: Compile

```bash
cd /home/yuzu/Downloads/database_project/Project

# Xóa class cũ
rm -rf bin && mkdir -p bin

# Compile toàn bộ
javac -d bin \
  -cp "src:lib/mysql-connector-j-26.7.0.jar:lib/gson-2.10.1.jar" \
  $(find src -name "*.java") 2>&1

echo "Compile exit code: $?"
```

**Mong đợi:** Không có lỗi compile. Exit code = 0.

---

## Bước 2: LicenseAccessControlTest (15/15)

```bash
cd /home/yuzu/Downloads/database_project/Project

java -cp "bin:lib/mysql-connector-j-26.7.0.jar:lib/gson-2.10.1.jar" \
  com.library.test.LicenseAccessControlTest 2>&1

echo "Exit code: $?"
```

**Mong đợi:** `PASSED: 15/15 test cases`

> [!IMPORTANT]
> Đây là baseline đã pass từ trước. Nếu số lượng PASSED giảm, cần điều tra ngay.

---

## Bước 3: ValidationAndExceptionTest (baseline 14/19)

```bash
cd /home/yuzu/Downloads/database_project/Project

java -cp "bin:lib/mysql-connector-j-26.7.0.jar:lib/gson-2.10.1.jar" \
  com.library.test.ValidationAndExceptionTest 2>&1

echo "Exit code: $?"
```

**Mong đợi:** `PASSED: 14/19` (5 failures là **expected** — do missing Person #5, #6 và Journal #5–6 trong live DB)

> [!NOTE]
> Nếu PASSED < 14, kiểm tra xem refactor Java có vô tình break exception mapping không.

---

## Bước 4: API Endpoint Smoke Test

```bash
# Khởi động ApiServer
cd /home/yuzu/Downloads/database_project/Project
java -cp "bin:lib/mysql-connector-j-26.7.0.jar:lib/gson-2.10.1.jar" \
  com.library.api.ApiServer &
API_PID=$!
sleep 3

echo "=== GET /api/members ==="
curl -s http://localhost:8080/api/members | python3 -m json.tool | head -30

echo "=== GET /api/resources ==="
curl -s http://localhost:8080/api/resources | python3 -m json.tool | head -30

echo "=== GET /api/borrows ==="
curl -s http://localhost:8080/api/borrows | python3 -m json.tool | head -30

echo "=== GET /api/fines ==="
curl -s http://localhost:8080/api/fines | python3 -m json.tool | head -30

echo "=== GET /api/digital-journals ==="
curl -s http://localhost:8080/api/digital-journals | python3 -m json.tool | head -30

echo "=== GET /api/access-logs ==="
curl -s http://localhost:8080/api/access-logs | python3 -m json.tool | head -30

echo "=== GET /api/licenses ==="
curl -s http://localhost:8080/api/licenses | python3 -m json.tool | head -30

echo "=== GET /api/granted-licenses ==="
curl -s http://localhost:8080/api/granted-licenses | python3 -m json.tool | head -30
```

**Mong đợi:** Tất cả endpoint trả về HTTP 200, JSON hợp lệ, có dữ liệu.

---

## Bước 5: Business Logic Integration Test (POST endpoints)

```bash
# ApiServer đang chạy từ Bước 4

echo "=== TEST: Mượn sách (POST /api/borrows) ==="
curl -s -X POST http://localhost:8080/api/borrows \
  -H "Content-Type: application/json" \
  -d '{"memberId": 1, "copyId": "COPY001"}' | python3 -m json.tool

echo "=== TEST: Mượn sách - BR01 GUEST bị chặn ==="
# Thay memberId bằng ID của GUEST trong sample_data
curl -s -X POST http://localhost:8080/api/borrows \
  -H "Content-Type: application/json" \
  -d '{"memberId": 3, "copyId": "COPY002"}' | python3 -m json.tool
# Mong đợi: HTTP 403, error message chứa "BR01"

echo "=== TEST: Trả sách (POST /api/borrows/{id}/return) ==="
# Thay {id} bằng borrow_id vừa tạo ở trên
BORROW_ID=1
curl -s -X POST http://localhost:8080/api/borrows/$BORROW_ID/return \
  -H "Content-Type: application/json" \
  -d '{}' | python3 -m json.tool

echo "=== TEST: Trả sách đã trả - BR02 ==="
curl -s -X POST http://localhost:8080/api/borrows/$BORROW_ID/return \
  -H "Content-Type: application/json" \
  -d '{}' | python3 -m json.tool
# Mong đợi: HTTP 409, error message chứa "BR02"

echo "=== TEST: Xác nhận thanh toán phiếu phạt ==="
FINE_ID=1
curl -s -X POST http://localhost:8080/api/fines/$FINE_ID/payments \
  -H "Content-Type: application/json" \
  -d '{}' | python3 -m json.tool

echo "=== TEST: Truy cập tài liệu số ==="
JOURNAL_ID=1
curl -s -X POST http://localhost:8080/api/digital-journals/$JOURNAL_ID/access \
  -H "Content-Type: application/json" \
  -d '{"personId": 1, "ipAddress": "127.0.0.1"}' | python3 -m json.tool

echo "=== TEST: Cấp License thủ công ==="
curl -s -X POST http://localhost:8080/api/granted-licenses \
  -H "Content-Type: application/json" \
  -d '{"personId": 2, "licenseId": 1}' | python3 -m json.tool

echo "=== TEST: Thu hồi License ==="
GRANT_ID=1
curl -s -X DELETE http://localhost:8080/api/granted-licenses/$GRANT_ID | python3 -m json.tool
```

---

## Bước 6: Kiểm tra VIEW hoạt động đúng

```sql
-- Chạy trực tiếp trong MySQL
USE library_db;

-- Kiểm tra VIEW_MEMBERS_DETAILED
SELECT person_id, name, person_type, borrow_quota, active_borrow_count, is_eligible
FROM VIEW_MEMBERS_DETAILED;

-- Kiểm tra VIEW_BORROWS_DETAILED
SELECT borrow_id, member_name, resource_title, borrow_date, due_date, status
FROM VIEW_BORROWS_DETAILED;

-- Kiểm tra VIEW_FINES_DETAILED
SELECT fine_id, member_name, total_fine_amount, payment_status
FROM VIEW_FINES_DETAILED;

-- Kiểm tra VIEW_ACCESS_LOGS_DETAILED
SELECT log_id, person_name, journal_title, access_result
FROM VIEW_ACCESS_LOGS_DETAILED;

-- Kiểm tra VIEW_GRANTED_LICENSES_DETAILED
SELECT grant_id, person_name, license_name
FROM VIEW_GRANTED_LICENSES_DETAILED;
```

---

## Bước 7: Kiểm tra Stored Procedures trực tiếp

```sql
USE library_db;

-- Liệt kê tất cả SP đã deploy
SHOW PROCEDURE STATUS WHERE Db = 'library_db';

-- Mong đợi thấy:
-- sp_check_resource_access
-- sp_grant_license_manual
-- sp_revoke_license_manual
-- sp_borrow_book          (mới - TASK 02)
-- sp_return_book          (mới - TASK 03)
-- sp_confirm_fine_payment (mới - TASK 04)
-- sp_access_digital_journal (mới - TASK 05)
```

---

## Checklist tổng hợp

| # | Hạng mục | Mong đợi | Kết quả |
|---|---|---|---|
| 1 | Compile thành công | Exit code = 0 | |
| 2 | LicenseAccessControlTest | 15/15 PASSED | |
| 3 | ValidationAndExceptionTest | 14/19 PASSED | |
| 4 | GET /api/members | 200, JSON hợp lệ | |
| 5 | GET /api/resources | 200, JSON hợp lệ | |
| 6 | GET /api/borrows | 200, JSON hợp lệ | |
| 7 | GET /api/fines | 200, JSON hợp lệ | |
| 8 | GET /api/digital-journals | 200, JSON hợp lệ | |
| 9 | GET /api/access-logs | 200, JSON hợp lệ | |
| 10 | GET /api/licenses | 200, JSON hợp lệ | |
| 11 | GET /api/granted-licenses | 200, JSON hợp lệ | |
| 12 | POST /api/borrows (hợp lệ) | 201, borrow_id trả về | |
| 13 | POST /api/borrows (BR01) | 403, error code đúng | |
| 14 | POST /api/borrows/{id}/return | 200, trả sách thành công | |
| 15 | POST /api/borrows/{id}/return (BR02) | 409, error code đúng | |
| 16 | POST /api/fines/{id}/payments | 200, transaction_id trả về | |
| 17 | POST /api/digital-journals/{id}/access | 200, allowed=true/false | |
| 18 | VIEW_MEMBERS_DETAILED | Dữ liệu đúng, có is_eligible | |
| 19 | VIEW_BORROWS_DETAILED | Dữ liệu đúng, có status computed | |
| 20 | Tất cả 7 SP tồn tại trong DB | SHOW PROCEDURE STATUS | |

---

## Khi gặp lỗi

### Compile error
```bash
# Xem full error
javac ... 2>&1 | grep "error:"
```

### SP error
```bash
# Xem definition của SP cụ thể
mysql -u root -p -e "SHOW CREATE PROCEDURE library_db.sp_borrow_book\G"
```

### Test thất bại
```bash
# Chạy với verbose output
java ... com.library.test.LicenseAccessControlTest 2>&1 | grep -E "(PASS|FAIL|ERROR)"
```

### JSON format thay đổi
```bash
# So sánh với output cũ (nếu có lưu)
curl -s http://localhost:8080/api/borrows | python3 -c "
import json, sys
data = json.load(sys.stdin)
print('Keys in first item:', list(data[0].keys()) if data else 'empty')
"
```

---

## Dọn dẹp sau verification

```bash
# Kill ApiServer
kill $API_PID 2>/dev/null || pkill -f "com.library.api.ApiServer"
```
