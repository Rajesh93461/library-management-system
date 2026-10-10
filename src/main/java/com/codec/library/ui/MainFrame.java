package com.codec.library.ui;

import com.codec.library.service.LibraryService;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.util.List;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("Library Management System");
        LibraryService service = new LibraryService();

        BooksPanel books = new BooksPanel(service);
        MembersPanel members = new MembersPanel(service);
        LoansPanel loans = new LoansPanel(service);
        List<Refreshable> panels = List.of(books, members, loans);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Books", books);
        tabs.addTab("Members", members);
        tabs.addTab("Issue / Return", loans);
        // Reload data whenever a tab is opened so combos and tables are never stale.
        tabs.addChangeListener(e -> panels.get(tabs.getSelectedIndex()).refresh());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        add(tabs);
        books.refresh();
    }
}
