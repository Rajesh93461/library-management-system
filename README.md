# Library Management System

A desktop application to track book inventory, members, lending and late fees.
Built with **Java 17, Swing, JDBC and SQLite**.

## Features

- **Book inventory** - add, edit, delete and search books by title, author or ISBN
- **Member management** - add, edit, delete and search members
- **Issue / return** - issue a book to a member, return it, see all loan history
- **Availability tracking** - available copies update automatically on issue/return
- **Late fee calculation** - Rs 5 per day after the 14-day loan period; overdue loans are shown in red
- **Business rules** - max 3 active loans per member, no duplicate copy of the same book, no issuing when out of stock
- **Data integrity** - issue/return run in database transactions; foreign keys and CHECK constraints enabled

## Tech stack

| Layer      | Technology                    |
|------------|-------------------------------|
| UI         | Java Swing                    |
| Data       | JDBC + SQLite (`library.db`)  |
| Build      | Maven                         |

## Project structure

```
src/main/java/com/codec/library
 +-- Main.java                 entry point
 +-- db/Database.java          connection + schema + demo data
 +-- model/                    Book, Member, Loan (records)
 +-- service/LibraryService    business rules + all SQL
 +-- ui/                       MainFrame, BooksPanel, MembersPanel, LoansPanel
```

## Run it

Requirements: JDK 17+ and Maven.

```bash
mvn compile exec:java        # run directly
# or build a single runnable jar
mvn clean package
java -jar target/library-management-system-1.0.0.jar
```

The SQLite file `library.db` is created in the working directory on first run, with a few sample books and members.

## Database schema

```sql
books   (id, title, author, isbn UNIQUE, total_copies, available_copies)
members (id, name, email UNIQUE, phone)
loans   (id, book_id -> books, member_id -> members, issue_date, due_date, return_date, fine)
```

## How the late fee works

`fine = max(0, days between due date and return date) x Rs 5`

Change `FINE_PER_DAY`, `LOAN_DAYS` or `MAX_ACTIVE_LOANS` in `LibraryService.java`.

## Switching to MySQL

Replace the `sqlite-jdbc` dependency with `mysql-connector-j`, change `URL` in `Database.java`
(e.g. `jdbc:mysql://localhost:3306/library`) and adjust the DDL (`AUTO_INCREMENT` instead of `AUTOINCREMENT`).

## Possible improvements

- Login with librarian / member roles
- Book reservations and renewal
- Export overdue report to CSV/PDF
