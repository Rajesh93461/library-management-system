package com.codec.library.ui;

import com.codec.library.model.Book;
import com.codec.library.service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class BooksPanel extends JPanel implements Refreshable {

    private final LibraryService service;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Title", "Author", "ISBN", "Total", "Available"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField searchField = new JTextField(22);
    private final JTextField titleField = new JTextField(14);
    private final JTextField authorField = new JTextField(12);
    private final JTextField isbnField = new JTextField(12);
    private final JTextField copiesField = new JTextField(4);

    public BooksPanel(LibraryService service) {
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchBtn = new JButton("Search");
        top.add(new JLabel("Search (title / author / ISBN):"));
        top.add(searchField);
        top.add(searchBtn);
        searchBtn.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillForm();
            }
        });

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Title"));
        form.add(titleField);
        form.add(new JLabel("Author"));
        form.add(authorField);
        form.add(new JLabel("ISBN"));
        form.add(isbnField);
        form.add(new JLabel("Copies"));
        form.add(copiesField);
        JButton add = new JButton("Add");
        JButton update = new JButton("Update");
        JButton delete = new JButton("Delete");
        JButton clear = new JButton("Clear");
        form.add(add);
        form.add(update);
        form.add(delete);
        form.add(clear);
        add.addActionListener(e -> addBook());
        update.addActionListener(e -> updateBook());
        delete.addActionListener(e -> deleteBook());
        clear.addActionListener(e -> clearForm());

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(form, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        try {
            model.setRowCount(0);
            for (Book b : service.searchBooks(searchField.getText())) {
                model.addRow(new Object[]{b.id(), b.title(), b.author(), b.isbn(),
                        b.totalCopies(), b.availableCopies()});
            }
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private int selectedModelRow() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    }

    private void fillForm() {
        int row = selectedModelRow();
        if (row < 0) {
            return;
        }
        titleField.setText(model.getValueAt(row, 1).toString());
        authorField.setText(model.getValueAt(row, 2).toString());
        isbnField.setText(model.getValueAt(row, 3).toString());
        copiesField.setText(model.getValueAt(row, 4).toString());
    }

    private void clearForm() {
        titleField.setText("");
        authorField.setText("");
        isbnField.setText("");
        copiesField.setText("");
        table.clearSelection();
    }

    private int parseCopies() {
        try {
            return Integer.parseInt(copiesField.getText().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Copies must be a whole number");
        }
    }

    private void addBook() {
        try {
            service.addBook(titleField.getText(), authorField.getText(), isbnField.getText(), parseCopies());
            clearForm();
            refresh();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void updateBook() {
        int row = selectedModelRow();
        if (row < 0) {
            Ui.info(this, "Select a book in the table first.");
            return;
        }
        try {
            int id = (Integer) model.getValueAt(row, 0);
            service.updateBook(id, titleField.getText(), authorField.getText(), isbnField.getText(), parseCopies());
            refresh();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void deleteBook() {
        int row = selectedModelRow();
        if (row < 0) {
            Ui.info(this, "Select a book in the table first.");
            return;
        }
        if (!Ui.confirm(this, "Delete the selected book?")) {
            return;
        }
        try {
            service.deleteBook((Integer) model.getValueAt(row, 0));
            clearForm();
            refresh();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }
}
