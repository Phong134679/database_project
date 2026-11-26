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
 * Repository class handling MySQL data persistence for UNIVERSITY_MEMBER.
 * In v2 schema, member_id is the independent PK and person_id is a UNIQUE FK to PERSON.
 */
public class UniversityMemberRepository {

    /**
     * Inserts a new UniversityMember record (assuming Person record exists).
     */
    public void insertMember(UniversityMember member) {
        String sql = "INSERT INTO UNIVERSITY_MEMBER (member_id, person_id, member_type, major, academic_rank) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, member.getMemberId());
            ps.setInt(2, member.getPersonId());
            ps.setString(3, member.getMemberType());
            ps.setString(4, member.getMajor());
            ps.setString(5, member.getAcademicRank());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi thêm mới thành viên (insertMember): " + e.getMessage(), e);
        }
    }

    /**
     * Finds a UniversityMember by personId joining with PERSON.
     */
    public UniversityMember findByPersonId(int personId) {
        String sql = "SELECT p.person_id, p.name, p.email, p.phone, " +
                     "       m.member_id, m.member_type, m.major, m.academic_rank " +
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
     * Finds a UniversityMember by memberId (e.g., 'SV2026001').
     */
    public UniversityMember findByMemberId(String memberId) {
        String sql = "SELECT p.person_id, p.name, p.email, p.phone, " +
                     "       m.member_id, m.member_type, m.major, m.academic_rank " +
                     "FROM UNIVERSITY_MEMBER m " +
                     "JOIN PERSON p ON m.person_id = p.person_id " +
                     "WHERE m.member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMember(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi tra cứu thành viên theo mã " + memberId + ": " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Alias for findByMemberId for backward compatibility.
     */
    public UniversityMember findByMemberCode(String memberCode) {
        return findByMemberId(memberCode);
    }

    /**
     * Retrieves all UniversityMember records.
     */
    public List<UniversityMember> findAll() {
        List<UniversityMember> list = new ArrayList<>();
        String sql = "SELECT p.person_id, p.name, p.email, p.phone, " +
                     "       m.member_id, m.member_type, m.major, m.academic_rank " +
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
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("member_id"),
                rs.getString("member_type"),
                rs.getString("major"),
                rs.getString("academic_rank")
        );
    }
}
