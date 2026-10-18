package com.library;

import com.library.controller.LibraryController;
import com.library.model.Borrow;
import com.library.repository.BorrowRepository;
import com.library.service.BorrowService;
import com.library.util.DatabaseConnection;

import java.util.List;

/**
 * Main application entry point to demonstrate and test interaction with MySQL.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  DIGITAL LIBRARY MANAGEMENT SYSTEM - MYSQL INTERACTION   ");
        System.out.println("==========================================================");

        // 1. Kiểm tra kết nối tới MySQL
        System.out.print("[1] Kiểm tra kết nối MySQL: ");
        boolean isConnected = DatabaseConnection.testConnection();
        if (isConnected) {
            System.out.println("KẾT NỐI THÀNH CÔNG! (URL: " + DatabaseConnection.getUrl() + ")");
        } else {
            System.out.println("CHƯA KẾT NỐI ĐƯỢC.");
            System.out.println("    Vui lòng đảm bảo MySQL service đang chạy và kiểm tra thông tin trong db.properties.");
        }

        // 2. Khởi tạo tầng Repository, Service, Controller theo mô hình MVC
        BorrowRepository repository = new BorrowRepository();
        BorrowService service = new BorrowService(repository);
        LibraryController controller = new LibraryController(service);

        // 3. Hiển thị danh sách tất cả các giao dịch mượn trong database
        System.out.println("\n[2] Lấy danh sách toàn bộ phiếu mượn từ bảng BORROW:");
        List<Borrow> allBorrows = repository.findAll();
        if (allBorrows.isEmpty()) {
            System.out.println("    (Hiện chưa có dữ liệu hoặc chưa chạy script sample_data.sql)");
        } else {
            for (Borrow b : allBorrows) {
                System.out.println("    -> " + b);
            }
        }

        // 4. Hiển thị danh sách trễ hạn
        System.out.println("\n[3] Kiểm tra các giao dịch trễ hạn:");
        controller.displayOverdueMembers();

        System.out.println("\n==========================================================");
        System.out.println("  HOÀN TẤT KIỂM TRA MÃ NGUỒN TƯƠNG TÁC MYSQL              ");
        System.out.println("==========================================================");
    }
}
