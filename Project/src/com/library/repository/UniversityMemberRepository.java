package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.UniversityMember;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class handling MySQL data persistence for UNIVERSITY_MEMBER subclass.
 */
public class UniversityMemberRepository {

    /**
     * Inserts a new UniversityMember record (assuming Person superclass record exists).
     */
    public void insertMember(UniversityMember member) {
        String sql = "INSERT INTO UNIVERSITY_MEMBER (person_id, member_id, department, member_type, major, academic_rank) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, member.getPersonId());
            ps.setString(2, member.getMemberId());
            ps.setString(3, member.getDepartment());
            ps.setString(4, member.getMemberType());
            ps.setString(5, member.getMajor());
            ps.setString(6, member.getAcademicRank());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi thêm mới thành viên (insertMember): " + e.getMessage(), e);
        }
    }

    /**
     * Finds a UniversityMember by personId joining with PERSON superclass table.
     */
    public UniversityMember findByPersonId(int personId) {
        String sql = "SELECT p.person_id, p.ssn, p.name, p.address, p.email, p.phone, p.person_type, " +
                     "       m.member_id, m.department, m.member_type, m.major, m.academic_rank " +
                     "FROM UNIVERSITY_MEMBER m " +
                     "JOIN PERSON p ON m.person_id = p.person_id " +
                     "WHERE m.person_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMember(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu thành viên Person ID " + personId + ": " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Finds a UniversityMember by internal student/faculty code (memberId).
     */
    public UniversityMember findByMemberCode(String memberCode) {
        String sql = "SELECT p.person_id, p.ssn, p.name, p.address, p.email, p.phone, p.person_type, " +
                     "       m.member_id, m.department, m.member_type, m.major, m.academic_rank " +
                     "FROM UNIVERSITY_MEMBER m " +
                     "JOIN PERSON p ON m.person_id = p.person_id " +
                     "WHERE m.member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, memberCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMember(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu thành viên theo mã " + memberCode + ": " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Retrieves all UniversityMember records.
     */
    public List<UniversityMember> findAll() {
        List<UniversityMember> list = new ArrayList<>();
        String sql = "SELECT p.person_id, p.ssn, p.name, p.address, p.email, p.phone, p.person_type, " +
                     "       m.member_id, m.department, m.member_type, m.major, m.academic_rank " +
                     "FROM UNIVERSITY_MEMBER m " +
                     "JOIN PERSON p ON m.person_id = p.person_id " +
                     "ORDER BY p.person_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToMember(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi truy xuất toàn bộ danh sách thành viên: " + e.getMessage(), e);
        }
        return list;
    }

    private UniversityMember mapRowToMember(ResultSet rs) throws SQLException {
        return new UniversityMember(
                rs.getInt("person_id"),
                rs.getString("ssn"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("person_type"),
                rs.getString("member_id"),
                rs.getString("department"),
                rs.getString("member_type"),
                rs.getString("major"),
                rs.getString("academic_rank")
        );
    }
}
