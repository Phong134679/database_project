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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class handling MySQL data persistence for PERSON.
 * In v2 schema, PERSON stores identity only (person_id, name, email, phone).
 */
public class PersonRepository {

    /**
     * Inserts a new Person record and updates personId.
     */
    public void insertPerson(Person person) {
        String sql = "INSERT INTO PERSON (name, email, phone) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, person.getName());
            ps.setString(2, person.getEmail());
            ps.setString(3, person.getPhone());

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
        String sql = "SELECT person_id, name, email, phone FROM PERSON WHERE person_id = ?";
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
     * Retrieves all Person records.
     */
    public List<Person> findAll() {
        List<Person> list = new ArrayList<>();
        String sql = "SELECT person_id, name, email, phone FROM PERSON ORDER BY person_id ASC";
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
     * Retrieves all persons with optional UNIVERSITY_MEMBER & borrow eligibility details
     * by left-joining PERSON with VIEW_MEMBERS_DETAILED and checking AUTHOR_OF.
     */
    public List<Map<String, Object>> findAllDetailed() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.person_id, p.name, p.email, p.phone, " +
                     "       v.member_id, v.member_type, v.major, v.academic_rank, " +
                     "       v.loan_duration_days, v.borrow_quota, " +
                     "       COALESCE(v.active_borrow_count, 0) AS active_borrow_count, " +
                     "       COALESCE(v.overdue_borrow_count, 0) AS overdue_borrow_count, " +
                     "       COALESCE(v.unpaid_fine_count, 0) AS unpaid_fine_count, " +
                     "       COALESCE(v.is_eligible, FALSE) AS is_eligible, " +
                     "       CASE WHEN EXISTS (SELECT 1 FROM AUTHOR_OF ao WHERE ao.person_id = p.person_id) " +
                     "            THEN TRUE ELSE FALSE END AS is_author " +
                     "FROM PERSON p " +
                     "LEFT JOIN VIEW_MEMBERS_DETAILED v ON v.person_id = p.person_id " +
                     "ORDER BY p.person_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                String name = rs.getString("name");
                String memberId = rs.getString("member_id");
                String memberType = rs.getString("member_type");
                boolean isMember = memberId != null;
                boolean isAuthor = rs.getBoolean("is_author");
                int activeCount = rs.getInt("active_borrow_count");
                boolean hasOverdue = rs.getInt("overdue_borrow_count") > 0;
                boolean hasUnpaidFines = rs.getInt("unpaid_fine_count") > 0;
                int quota = rs.getInt("borrow_quota");
                boolean isEligible = isMember && rs.getBoolean("is_eligible");

                map.put("personId", rs.getInt("person_id"));
                map.put("name", name);
                map.put("email", rs.getString("email"));
                map.put("phone", rs.getString("phone"));
                map.put("memberId", memberId);
                map.put("isMember", isMember);
                map.put("isAuthor", isAuthor);
                map.put("memberType", memberType);
                map.put("major", rs.getString("major"));
                map.put("academicRank", rs.getString("academic_rank"));
                map.put("loanDurationDays", isMember ? rs.getInt("loan_duration_days") : 0);
                map.put("borrowQuota", isMember ? quota : 0);
                map.put("activeBorrowsCount", activeCount);
                map.put("hasOverdue", hasOverdue);
                map.put("hasUnpaidFines", hasUnpaidFines);
                map.put("isEligibleToBorrow", isEligible);
                map.put("ineligibilityReason", buildIneligibilityReason(
                        name, isMember, hasOverdue, hasUnpaidFines, activeCount, quota, isEligible));
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất danh sách người dùng chi tiết: " + e.getMessage(), e);
        }
        return list;
    }

    private String buildIneligibilityReason(String name, boolean isMember, boolean hasOverdue,
                                            boolean hasUnpaidFines, int activeCount, int quota, boolean isEligible) {
        if (isEligible) {
            return null;
        }
        if (!isMember) {
            return "Người dùng [" + name + "] không phải UNIVERSITY_MEMBER; chỉ UNIVERSITY_MEMBER mới được phép mượn sách (BR01).";
        }
        if (hasOverdue) {
            return "Thành viên [" + name + "] đang có sách mượn quá hạn chưa trả (BR05).";
        }
        if (hasUnpaidFines) {
            return "Thành viên [" + name + "] còn phiếu phạt ở trạng thái Unpaid. "
                    + "Yêu cầu thanh toán trước khi tiếp tục mượn mới (BR05).";
        }
        if (activeCount >= quota) {
            return "Thành viên [" + name + "] đã đạt hạn mức mượn tối đa (" + activeCount + "/" + quota + ") theo quy chế BR05.";
        }
        return "Không đủ điều kiện mượn sách.";
    }

    private Person mapRowToPerson(ResultSet rs) throws SQLException {
        return new Person(
                rs.getInt("person_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone")
        );
    }
}
