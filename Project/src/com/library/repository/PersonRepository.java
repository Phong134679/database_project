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
 * Repository class handling MySQL data persistence for PERSON via direct JDBC.
 */
public class PersonRepository {

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

    public Person findById(Connection conn, int personId) throws SQLException {
        String sql = "SELECT person_id, name, email, phone FROM PERSON WHERE person_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPerson(rs);
                }
            }
        }
        return null;
    }

    public Person findById(int personId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, personId);
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu người dùng ID " + personId + ": " + e.getMessage(), e);
        }
    }

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
     * Retrieves raw person, university member, borrow/overdue/fine counts, and author flag
     * directly from base tables without using VIEW_MEMBERS_DETAILED.
     */
    public List<Map<String, Object>> findAllDetailedRaw() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.person_id, p.name, p.email, p.phone, " +
                     "       um.member_id, um.member_type, um.major, um.academic_rank, " +
                     "       COALESCE(bs.active_borrow_count, 0) AS active_borrow_count, " +
                     "       COALESCE(bs.overdue_borrow_count, 0) AS overdue_borrow_count, " +
                     "       COALESCE(fs.unpaid_fine_count, 0) AS unpaid_fine_count, " +
                     "       CASE WHEN EXISTS (SELECT 1 FROM AUTHOR_OF ao WHERE ao.person_id = p.person_id) " +
                     "            THEN TRUE ELSE FALSE END AS is_author " +
                     "FROM PERSON p " +
                     "LEFT JOIN UNIVERSITY_MEMBER um ON um.person_id = p.person_id " +
                     "LEFT JOIN ( " +
                     "    SELECT b.member_id, " +
                     "           SUM(CASE WHEN b.return_date IS NULL THEN 1 ELSE 0 END) AS active_borrow_count, " +
                     "           SUM(CASE WHEN b.return_date IS NULL AND b.due_date < CURDATE() THEN 1 ELSE 0 END) AS overdue_borrow_count " +
                     "    FROM BORROW b " +
                     "    GROUP BY b.member_id " +
                     ") bs ON bs.member_id = um.member_id " +
                     "LEFT JOIN ( " +
                     "    SELECT b.member_id, COUNT(*) AS unpaid_fine_count " +
                     "    FROM FINE f " +
                     "    INNER JOIN BORROW b ON b.borrow_id = f.borrow_id " +
                     "    WHERE f.payment_status = 'Unpaid' " +
                     "    GROUP BY b.member_id " +
                     ") fs ON fs.member_id = um.member_id " +
                     "ORDER BY p.person_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("personId", rs.getInt("person_id"));
                map.put("name", rs.getString("name"));
                map.put("email", rs.getString("email"));
                map.put("phone", rs.getString("phone"));
                map.put("memberId", rs.getString("member_id"));
                map.put("memberType", rs.getString("member_type"));
                map.put("major", rs.getString("major"));
                map.put("academicRank", rs.getString("academic_rank"));
                map.put("activeBorrowsCount", rs.getInt("active_borrow_count"));
                map.put("overdueBorrowsCount", rs.getInt("overdue_borrow_count"));
                map.put("unpaidFinesCount", rs.getInt("unpaid_fine_count"));
                map.put("isAuthor", rs.getBoolean("is_author"));
                list.add(map);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất danh sách người dùng chi tiết: " + e.getMessage(), e);
        }
        return list;
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
