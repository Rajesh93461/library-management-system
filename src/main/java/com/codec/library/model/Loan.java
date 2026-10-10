package com.codec.library.model;

import java.time.LocalDate;

public record Loan(int id, String bookTitle, String memberName,
                   LocalDate issueDate, LocalDate dueDate, LocalDate returnDate, double fine) {
    public boolean isReturned() {
        return returnDate != null;
    }
}
