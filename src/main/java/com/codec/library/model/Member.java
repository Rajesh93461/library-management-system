package com.codec.library.model;

public record Member(int id, String name, String email, String phone) {
    @Override
    public String toString() {
        return id + " - " + name;
    }
}
