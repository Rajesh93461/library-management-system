package com.codec.library.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

/** Small helpers shared by all panels. */
final class Ui {
    private Ui() {
    }

    static void error(Component parent, Exception e) {
        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Library", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
