package com.codec.library.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Opens SQLite connections and creates the schema on first run. */
public final class Database {

    private static final String URL = "jdbc:sqlite:library.db";

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    public static void init() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            s.execute("""
                    CREATE TABLE IF NOT EXISTS books (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        isbn TEXT NOT NULL UNIQUE,
                        total_copies INTEGER NOT NULL CHECK (total_copies >= 1),
                        available_copies INTEGER NOT NULL
                            CHECK (available_copies >= 0 AND available_copies <= total_copies)
                    )""");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS members (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL UNIQUE,
                        phone TEXT
                    )""");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS loans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        book_id INTEGER NOT NULL REFERENCES books(id),
                        member_id INTEGER NOT NULL REFERENCES members(id),
                        issue_date TEXT NOT NULL,
                        due_date TEXT NOT NULL,
                        return_date TEXT,
                        fine REAL NOT NULL DEFAULT 0
                    )""");
            seedIfEmpty(s);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialise database: " + e.getMessage(), e);
        }
    }

    /** Adds a little demo data so the app is not empty on first launch. */
    private static void seedIfEmpty(Statement s) throws SQLException {
        try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM books")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }
        s.execute("""
                INSERT INTO books(title, author, isbn, total_copies, available_copies) VALUES
                ('Clean Code', 'Robert C. Martin', '9780132350884', 3, 3),
                ('Effective Java', 'Joshua Bloch', '9780134685991', 2, 2),
                ('Head First Java', 'Kathy Sierra', '9780596009205', 4, 4),
                ('Introduction to Algorithms', 'Thomas H. Cormen', '9780262033848', 2, 2),
                ('The Pragmatic Programmer', 'Andrew Hunt', '9780135957059', 1, 1)""");
        s.execute("""
                INSERT INTO members(name, email, phone) VALUES
                ('Asha Reddy', 'asha@example.com', '9876543210'),
                ('Ravi Kumar', 'ravi@example.com', '9123456780')""");
    }
}
