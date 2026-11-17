package com.library.repository;

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
            System.err.println("[PersonRepository] Error inserting person: " + e.getMessage());
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
            System.err.println("[PersonRepository] Error finding person " + personId + ": " + e.getMessage());
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
            System.err.println("[PersonRepository] Error finding person by SSN " + ssn + ": " + e.getMessage());
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
            System.err.println("[PersonRepository] Error fetching all persons: " + e.getMessage());
        }
        return list;
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
