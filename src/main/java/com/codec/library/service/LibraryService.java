package com.codec.library.service;

import com.codec.library.db.Database;
import com.codec.library.model.Book;
import com.codec.library.model.Loan;
import com.codec.library.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** All business rules and JDBC access for the library. */
public class LibraryService {

    public static final double FINE_PER_DAY = 5.0;   // Rs per late day
    public static final int LOAN_DAYS = 14;
    public static final int MAX_ACTIVE_LOANS = 3;

    // ------------------------------------------------------------------ Books

    public List<Book> searchBooks(String query) throws SQLException {
        String like = "%" + (query == null ? "" : query.trim()) + "%";
        String sql = "SELECT id, title, author, isbn, total_copies, available_copies FROM books "
                + "WHERE title LIKE ? OR author LIKE ? OR isbn LIKE ? ORDER BY title";
        List<Book> books = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(new Book(rs.getInt(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getInt(5), rs.getInt(6)));
                }
            }
        }
        return books;
    }

    public void addBook(String title, String author, String isbn, int copies) throws SQLException {
        validateBook(title, author, isbn, copies);
        execute("INSERT INTO books(title, author, isbn, total_copies, available_copies) VALUES (?,?,?,?,?)",
                title.trim(), author.trim(), isbn.trim(), copies, copies);
    }

    public void updateBook(int id, String title, String author, String isbn, int copies) throws SQLException {
        validateBook(title, author, isbn, copies);
        // available_copies shifts by the change in total copies (SQLite uses the old row values on the right).
        execute("UPDATE books SET title = ?, author = ?, isbn = ?, "
                        + "available_copies = available_copies + (? - total_copies), total_copies = ? WHERE id = ?",
                title.trim(), author.trim(), isbn.trim(), copies, copies, id);
    }

    public void deleteBook(int id) throws SQLException {
        execute("DELETE FROM books WHERE id = ?", id);
    }

    // ---------------------------------------------------------------- Members

    public List<Member> listMembers(String query) throws SQLException {
        String like = "%" + (query == null ? "" : query.trim()) + "%";
        String sql = "SELECT id, name, email, phone FROM members WHERE name LIKE ? OR email LIKE ? ORDER BY name";
        List<Member> members = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(new Member(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
                }
            }
        }
        return members;
    }

    public void addMember(String name, String email, String phone) throws SQLException {
        validateMember(name, email);
        execute("INSERT INTO members(name, email, phone) VALUES (?,?,?)", name.trim(), email.trim(), phone.trim());
    }

    public void updateMember(int id, String name, String email, String phone) throws SQLException {
        validateMember(name, email);
        execute("UPDATE members SET name = ?, email = ?, phone = ? WHERE id = ?",
                name.trim(), email.trim(), phone.trim(), id);
    }

    public void deleteMember(int id) throws SQLException {
        execute("DELETE FROM members WHERE id = ?", id);
    }

    // ------------------------------------------------------------------ Loans

    public List<Loan> listLoans(boolean activeOnly) throws SQLException {
        String sql = "SELECT l.id, b.title, m.name, l.issue_date, l.due_date, l.return_date, l.fine "
                + "FROM loans l JOIN books b ON b.id = l.book_id JOIN members m ON m.id = l.member_id "
                + (activeOnly ? "WHERE l.return_date IS NULL " : "")
                + "ORDER BY l.id DESC";
        List<Loan> loans = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String returned = rs.getString(6);
                loans.add(new Loan(rs.getInt(1), rs.getString(2), rs.getString(3),
                        LocalDate.parse(rs.getString(4)), LocalDate.parse(rs.getString(5)),
                        returned == null ? null : LocalDate.parse(returned), rs.getDouble(7)));
            }
        }
        return loans;
    }

    /** Issues a book. Runs in one transaction so stock and loan records never disagree. */
    public void issueBook(int bookId, int memberId) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                Integer available = scalarInt(c, "SELECT available_copies FROM books WHERE id = ?", bookId);
                if (available == null) {
                    throw new IllegalArgumentException("Book not found");
                }
                if (available <= 0) {
                    throw new IllegalStateException("No copies of this book are currently available");
                }
                if (scalarInt(c, "SELECT id FROM members WHERE id = ?", memberId) == null) {
                    throw new IllegalArgumentException("Member not found");
                }
                Integer active = scalarInt(c,
                        "SELECT COUNT(*) FROM loans WHERE member_id = ? AND return_date IS NULL", memberId);
                if (active != null && active >= MAX_ACTIVE_LOANS) {
                    throw new IllegalStateException("Member already has " + MAX_ACTIVE_LOANS + " books issued");
                }
                Integer sameBook = scalarInt(c,
                        "SELECT COUNT(*) FROM loans WHERE member_id = ? AND book_id = ? AND return_date IS NULL",
                        memberId, bookId);
                if (sameBook != null && sameBook > 0) {
                    throw new IllegalStateException("Member already has a copy of this book");
                }

                LocalDate today = LocalDate.now();
                update(c, "INSERT INTO loans(book_id, member_id, issue_date, due_date) VALUES (?,?,?,?)",
                        bookId, memberId, today.toString(), today.plusDays(LOAN_DAYS).toString());
                update(c, "UPDATE books SET available_copies = available_copies - 1 WHERE id = ?", bookId);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Returns a book and gives back the late fee that was charged (0 if on time). */
    public double returnBook(int loanId) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                int bookId = 0;
                LocalDate due = null;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT book_id, due_date, return_date FROM loans WHERE id = ?")) {
                    ps.setInt(1, loanId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Loan not found");
                        }
                        if (rs.getString(3) != null) {
                            throw new IllegalStateException("This book has already been returned");
                        }
                        bookId = rs.getInt(1);
                        due = LocalDate.parse(rs.getString(2));
                    }
                }
                LocalDate today = LocalDate.now();
                double fine = calculateFine(due, today);
                update(c, "UPDATE loans SET return_date = ?, fine = ? WHERE id = ?", today.toString(), fine, loanId);
                update(c, "UPDATE books SET available_copies = available_copies + 1 WHERE id = ?", bookId);
                c.commit();
                return fine;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Late fee = days past the due date x FINE_PER_DAY. Never negative. */
    public static double calculateFine(LocalDate dueDate, LocalDate returnDate) {
        long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
        return daysLate > 0 ? daysLate * FINE_PER_DAY : 0.0;
    }

    // ---------------------------------------------------------------- Helpers

    private static void validateBook(String title, String author, String isbn, int copies) {
        requireText(title, "Title");
        requireText(author, "Author");
        requireText(isbn, "ISBN");
        if (copies < 1) {
            throw new IllegalArgumentException("Copies must be at least 1");
        }
    }

    private static void validateMember(String name, String email) {
        requireText(name, "Name");
        requireText(email, "Email");
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email address looks invalid");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    /** Runs a single write statement on its own connection and turns constraint errors into friendly messages. */
    private static void execute(String sql, Object... params) throws SQLException {
        try (Connection c = Database.getConnection()) {
            update(c, sql, params);
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("UNIQUE")) {
                throw new SQLException("That ISBN or email already exists");
            }
            if (msg.contains("FOREIGN KEY")) {
                throw new SQLException("Cannot delete: this record is referenced by loan history");
            }
            if (msg.contains("CHECK")) {
                throw new SQLException("Cannot reduce total copies below the number currently issued");
            }
            throw e;
        }
    }

    private static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }

    private static Integer scalarInt(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }
}
