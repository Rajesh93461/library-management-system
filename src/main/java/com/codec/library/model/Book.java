package com.codec.library.model;

public record Book(int id, String title, String author, String isbn, int totalCopies, int availableCopies) {
    /** Used by combo boxes in the UI. */
    @Override
    public String toString() {
        return id + " - " + title + " (" + availableCopies + " available)";
    }
}
