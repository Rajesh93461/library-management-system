package com.codec.library.ui;

import com.codec.library.model.Member;
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

public class MembersPanel extends JPanel implements Refreshable {

    private final LibraryService service;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Name", "Email", "Phone"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField searchField = new JTextField(22);
    private final JTextField nameField = new JTextField(14);
    private final JTextField emailField = new JTextField(16);
    private final JTextField phoneField = new JTextField(10);

    public MembersPanel(LibraryService service) {
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchBtn = new JButton("Search");
        top.add(new JLabel("Search (name / email):"));
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
        form.add(new JLabel("Name"));
        form.add(nameField);
        form.add(new JLabel("Email"));
        form.add(emailField);
        form.add(new JLabel("Phone"));
        form.add(phoneField);
        JButton add = new JButton("Add");
        JButton update = new JButton("Update");
        JButton delete = new JButton("Delete");
        JButton clear = new JButton("Clear");
        form.add(add);
        form.add(update);
        form.add(delete);
        form.add(clear);
        add.addActionListener(e -> addMember());
        update.addActionListener(e -> updateMember());
        delete.addActionListener(e -> deleteMember());
        clear.addActionListener(e -> clearForm());

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(form, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        try {
            model.setRowCount(0);
            for (Member m : service.listMembers(searchField.getText())) {
                model.addRow(new Object[]{m.id(), m.name(), m.email(), m.phone()});
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
        nameField.setText(model.getValueAt(row, 1).toString());
        emailField.setText(model.getValueAt(row, 2).toString());
        Object phone = model.getValueAt(row, 3);
        phoneField.setText(phone == null ? "" : phone.toString());
    }

    private void clearForm() {
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        table.clearSelection();
    }

    private void addMember() {
        try {
            service.addMember(nameField.getText(), emailField.getText(), phoneField.getText());
            clearForm();
            refresh();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void updateMember() {
        int row = selectedModelRow();
        if (row < 0) {
            Ui.info(this, "Select a member in the table first.");
            return;
        }
        try {
            service.updateMember((Integer) model.getValueAt(row, 0),
                    nameField.getText(), emailField.getText(), phoneField.getText());
            refresh();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void deleteMember() {
        int row = selectedModelRow();
        if (row < 0) {
            Ui.info(this, "Select a member in the table first.");
            return;
        }
        if (!Ui.confirm(this, "Delete the selected member?")) {
            return;
        }
        try {
            service.deleteMember((Integer) model.getValueAt(row, 0));
            clearForm();
            refresh();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }
}
