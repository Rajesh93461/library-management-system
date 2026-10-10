package com.codec.library.ui;

import com.codec.library.model.Book;
import com.codec.library.model.Loan;
import com.codec.library.model.Member;
import com.codec.library.service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;

public class LoansPanel extends JPanel implements Refreshable {

    private final LibraryService service;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Loan ID", "Book", "Member", "Issued", "Due", "Returned", "Fine (Rs)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<Book> bookBox = new JComboBox<>();
    private final JComboBox<Member> memberBox = new JComboBox<>();
    private final JCheckBox activeOnly = new JCheckBox("Show active loans only");

    public LoansPanel(LibraryService service) {
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton issue = new JButton("Issue Book");
        top.add(new JLabel("Book"));
        top.add(bookBox);
        top.add(new JLabel("Member"));
        top.add(memberBox);
        top.add(issue);
        issue.addActionListener(e -> issueBook());

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        // Overdue, not-yet-returned loans are shown in red.
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                if (!selected) {
                    int mr = t.convertRowIndexToModel(row);
                    boolean notReturned = model.getValueAt(mr, 5).toString().isEmpty();
                    boolean overdue = LocalDate.parse(model.getValueAt(mr, 4).toString()).isBefore(LocalDate.now());
                    setForeground(notReturned && overdue ? Color.RED : Color.BLACK);
                }
                return this;
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton returnBtn = new JButton("Return Selected Book");
        JButton refreshBtn = new JButton("Refresh");
        bottom.add(returnBtn);
        bottom.add(refreshBtn);
        bottom.add(activeOnly);
        bottom.add(new JLabel("   Loan period: " + LibraryService.LOAN_DAYS + " days, late fee: Rs "
                + LibraryService.FINE_PER_DAY + "/day"));
        returnBtn.addActionListener(e -> returnBook());
        refreshBtn.addActionListener(e -> refresh());
        activeOnly.addActionListener(e -> refresh());

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        try {
            bookBox.removeAllItems();
            for (Book b : service.searchBooks("")) {
                if (b.availableCopies() > 0) {
                    bookBox.addItem(b);
                }
            }
            memberBox.removeAllItems();
            for (Member m : service.listMembers("")) {
                memberBox.addItem(m);
            }

            model.setRowCount(0);
            LocalDate today = LocalDate.now();
            for (Loan l : service.listLoans(activeOnly.isSelected())) {
                // For loans still out, show the fine that would be charged if returned today.
                double fine = l.isReturned() ? l.fine() : LibraryService.calculateFine(l.dueDate(), today);
                model.addRow(new Object[]{l.id(), l.bookTitle(), l.memberName(),
                        l.issueDate().toString(), l.dueDate().toString(),
                        l.isReturned() ? l.returnDate().toString() : "",
                        String.format("%.2f", fine)});
            }
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void issueBook() {
        Book book = (Book) bookBox.getSelectedItem();
        Member member = (Member) memberBox.getSelectedItem();
        if (book == null || member == null) {
            Ui.info(this, "Choose an available book and a member.");
            return;
        }
        try {
            service.issueBook(book.id(), member.id());
            refresh();
            Ui.info(this, "Issued \"" + book.title() + "\" to " + member.name()
                    + ".\nDue in " + LibraryService.LOAN_DAYS + " days.");
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void returnBook() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            Ui.info(this, "Select a loan in the table first.");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        try {
            double fine = service.returnBook((Integer) model.getValueAt(row, 0));
            refresh();
            Ui.info(this, fine > 0
                    ? String.format("Book returned late. Fine to collect: Rs %.2f", fine)
                    : "Book returned on time. No fine.");
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }
}
