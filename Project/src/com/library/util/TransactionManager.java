package com.library.util;

import com.library.exception.DatabaseException;
import com.library.exception.LibraryException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utility class for managing JDBC transactions with explicit commit and rollback.
 */
public final class TransactionManager {

    private TransactionManager() {
    }

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction(Connection conn) throws SQLException;
    }

    /**
     * Executes the given callback inside a single JDBC transaction.
     * Commits on success and rolls back on any LibraryException, SQLException, or RuntimeException.
     */
    public static <T> T executeInTransaction(TransactionCallback<T> callback) {
        Connection conn = null;
        boolean originalAutoCommit = true;
        try {
            conn = DatabaseConnection.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            T result = callback.doInTransaction(conn);
            conn.commit();
            return result;
        } catch (LibraryException le) {
            rollbackQuietly(conn);
            throw le;
        } catch (SQLException se) {
            rollbackQuietly(conn);
            throw new DatabaseException("Lỗi giao dịch JDBC: " + se.getMessage(), se);
        } catch (RuntimeException re) {
            rollbackQuietly(conn);
            throw re;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {
                }
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }
}
