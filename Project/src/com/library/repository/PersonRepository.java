package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.Person;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class handling MySQL data persistence for PERSON superclass.
 */
public class PersonRepository {

    /**
     * Inserts a new Person record and updates personId.
     */
    public void insertPerson(Person person) {
        String sql = "INSERT INTO PERSON (ssn, name, address, email, phone, person_type) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, person.getSsn());
            ps.setString(2, person.getName());
            ps.setString(3, person.getAddress());
            ps.setString(4, person.getEmail());
            ps.setString(5, person.getPhone());
            ps.setString(6, person.getPersonType());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        person.setPersonId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi thêm mới người dùng (insertPerson): " + e.getMessage(), e);
        }
    }

    /**
     * Finds a Person by personId.
     */
    public Person findById(int personId) {
        String sql = "SELECT person_id, ssn, name, address, email, phone, person_type " +
                     "FROM PERSON WHERE person_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPerson(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu người dùng ID " + personId + ": " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Finds a Person by national ID / SSN.
     */
    public Person findBySsn(String ssn) {
        String sql = "SELECT person_id, ssn, name, address, email, phone, person_type " +
                     "FROM PERSON WHERE ssn = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ssn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPerson(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu người dùng theo SSN " + ssn + ": " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Retrieves all Person records.
     */
    public List<Person> findAll() {
        List<Person> list = new ArrayList<>();
        String sql = "SELECT person_id, ssn, name, address, email, phone, person_type " +
                     "FROM PERSON ORDER BY person_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToPerson(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất toàn bộ danh sách người dùng: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieves all persons with their university membership attributes and
     * borrow eligibility summary from VIEW_MEMBERS_DETAILED (single query).
     */
    public List<java.util.Map<String, Object>> findAllDetailed() {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT person_id, ssn, name, address, email, phone, person_type, " +
                     "       member_id, department, member_type, major, academic_rank, " +
                     "       borrow_quota, active_borrow_count, overdue_borrow_count, " +
                     "       unpaid_fine_count, is_eligible " +
                     "FROM VIEW_MEMBERS_DETAILED " +
                     "ORDER BY person_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                String name = rs.getString("name");
                String personType = rs.getString("person_type");
                int activeCount = rs.getInt("active_borrow_count");
                boolean hasOverdue = rs.getInt("overdue_borrow_count") > 0;
                boolean hasUnpaidFines = rs.getInt("unpaid_fine_count") > 0;
                int quota = rs.getInt("borrow_quota");
                boolean isEligible = rs.getBoolean("is_eligible");

                map.put("personId", rs.getInt("person_id"));
                map.put("ssn", rs.getString("ssn"));
                map.put("name", name);
                map.put("address", rs.getString("address"));
                map.put("email", rs.getString("email"));
                map.put("phone", rs.getString("phone"));
                map.put("personType", personType);
                map.put("memberId", rs.getString("member_id"));
                map.put("department", rs.getString("department"));
                map.put("memberType", rs.getString("member_type"));
                map.put("major", rs.getString("major"));
                map.put("academicRank", rs.getString("academic_rank"));
                map.put("activeBorrowsCount", activeCount);
                map.put("hasOverdue", hasOverdue);
                map.put("hasUnpaidFines", hasUnpaidFines);
                map.put("isEligibleToBorrow", isEligible);
                map.put("ineligibilityReason", buildIneligibilityReason(
                        name, personType, hasOverdue, hasUnpaidFines, activeCount, quota, isEligible));
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất danh sách người dùng chi tiết: " + e.getMessage(), e);
        }
        return list;
    }

    private String buildIneligibilityReason(String name, String personType, boolean hasOverdue,
                                            boolean hasUnpaidFines, int activeCount, int quota, boolean isEligible) {
        if (isEligible) {
            return null;
        }
        if (!"STUDENT".equalsIgnoreCase(personType) && !"FACULTY".equalsIgnoreCase(personType)) {
            return "Độc giả [" + name + "] mang vai trò '" + personType
                    + "' là đối tượng bên ngoài, chỉ được phép đọc tài liệu tại chỗ theo quy chế BR01.";
        }
        if (hasOverdue) {
            return "Độc giả [" + name + "] đang có sách mượn quá hạn chưa trả (BR05).";
        }
        if (hasUnpaidFines) {
            return "Độc giả [" + name + "] có phiếu phạt quá hạn chưa thanh toán. "
                    + "Yêu cầu tất toán công nợ phạt trước khi tiếp tục mượn mới (BR05).";
        }
        if (activeCount >= quota) {
            return "Độc giả [" + name + "] đã đạt hạn mức mượn tối đa (" + activeCount + "/" + quota + ") theo quy chế BR05.";
        }
        return "Không đủ điều kiện mượn sách.";
    }

    private Person mapRowToPerson(ResultSet rs) throws SQLException {
        return new Person(
                rs.getInt("person_id"),
                rs.getString("ssn"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("person_type")
        );
    }
}
