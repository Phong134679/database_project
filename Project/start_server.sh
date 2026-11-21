#!/usr/bin/env bash
# =============================================================================
# Script Reset & Khởi chạy Web Server Library Management System
# =============================================================================

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "==============================================================================="
echo " [1/4] Kiểm tra và giải phóng cổng 8080 nếu đang bận..."
echo "==============================================================================="
PID=$(lsof -ti :8080 || true)
if [ -n "$PID" ]; then
    echo "-> Đang đóng tiến trình cũ đang chiếm cổng 8080 (PID: $PID)..."
    kill -9 $PID 2>/dev/null || true
    sleep 1
    echo "-> Cổng 8080 đã được giải phóng thành công."
else
    echo "-> Cổng 8080 đang trống."
fi

echo ""
echo "==============================================================================="
echo " [2/4] Đồng bộ cấu hình db.properties..."
echo "==============================================================================="
# Đảm bảo bin/db.properties khớp mật khẩu nếu src/db.properties để trống
if [ -f "bin/db.properties" ]; then
    echo "-> Sử dụng cấu hình từ bin/db.properties."
else
    echo "-> Copy cấu hình từ src/ sang bin/..."
    mkdir -p bin
    cp src/db.properties bin/db.properties
fi

echo ""
echo "==============================================================================="
echo " [3/4] Biên dịch mã nguồn Java..."
echo "==============================================================================="
mkdir -p bin
javac -d bin -cp "src:lib/mysql-connector-j-26.7.0.jar:lib/gson-2.10.1.jar" $(find src -name "*.java")
echo "-> Biên dịch hoàn tất không có lỗi!"

echo ""
echo "==============================================================================="
echo " [4/4] Khởi động ApiServer trên cổng 8080..."
echo "==============================================================================="
echo "-> Mở trình duyệt tại: http://localhost:8080/"
echo "-> Bấm Ctrl + C để dừng server khi không dùng nữa."
echo "==============================================================================="

java -cp "bin:lib/mysql-connector-j-26.7.0.jar:lib/gson-2.10.1.jar" com.library.api.ApiServer 8080
