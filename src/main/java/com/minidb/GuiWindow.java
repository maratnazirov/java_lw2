package com.minidb;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

// Графический интерфейс Swing для MiniDB
public class GuiWindow extends JFrame {

    private final Database db;
    private final StorageManager storage;
    private final IndexManager indexManager;
    private final SqlParser parser;
    private final QueryExecutor executor;

    private JTextArea queryArea;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private DefaultListModel<String> tablesListModel;

    public GuiWindow(String dataDir) throws IOException {
        super("MiniDB - Реляционная СУБД");

        this.storage = new JsonStorageManager(dataDir);
        this.db = Database.getInstance();
        this.indexManager = new IndexManager(dataDir + "/indexes");
        this.parser = new SqlParser();
        this.executor = new QueryExecutor(db, storage, indexManager);

        List<Table> tables = storage.loadAll();
        for (Table t : tables) {
            db.putTable(t);
        }
        indexManager.loadAndRebuild(db.getTables());

        if (!db.hasTable("owners") || !db.hasTable("cars") || !db.hasTable("services")) {
            ConsoleRepl.seedDefault(db, storage, indexManager);
        }

        initUi();
        refreshTablesList();
        executeQuery();
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("SQL Запрос"));

        queryArea = new JTextArea(4, 50);
        queryArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        queryArea.setText("SELECT * FROM cars;");
        topPanel.add(new JScrollPane(queryArea), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton runBtn = new JButton("▶ Выполнить (Ctrl+Enter)");
        runBtn.setBackground(new Color(230, 245, 230));
        runBtn.setFont(runBtn.getFont().deriveFont(Font.BOLD));
        runBtn.addActionListener(e -> executeQuery());
        actionPanel.add(runBtn);

        JComboBox<String> samplesCombo = new JComboBox<>(new String[]{
                "Быстрые примеры запросов",
                "SELECT * FROM cars;",
                "SELECT * FROM owners;",
                "SELECT * FROM services;",
                "TABLE cars;",
                "SHOW TABLES;",
                "DESCRIBE cars;",
                "SELECT brand, price FROM cars WHERE price > 20000;",
                "SELECT * FROM services WHERE cost > 300;",
                "INSERT INTO cars (id, brand, price) VALUES (21, 'Ferrari Roma', 220000.0);",
                "UPDATE cars SET price = 32000 WHERE id = 2;",
                "CREATE INDEX idx_cars_price ON cars (price);"
        });
        samplesCombo.addActionListener(e -> {
            String selected = (String) samplesCombo.getSelectedItem();
            if (selected != null && !selected.equals("Быстрые примеры запросов")) {
                queryArea.setText(selected);
            }
        });
        actionPanel.add(samplesCombo);

        JButton clearBtn = new JButton("Очистить");
        clearBtn.addActionListener(e -> queryArea.setText(""));
        actionPanel.add(clearBtn);

        topPanel.add(actionPanel, BorderLayout.SOUTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        resultTable.setRowHeight(22);
        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Результат запроса"));

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Таблицы в БД"));
        leftPanel.setPreferredSize(new Dimension(200, 0));

        tablesListModel = new DefaultListModel<>();
        JList<String> tablesList = new JList<>(tablesListModel);
        tablesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tablesList.getSelectedValue() != null) {
                String tableName = tablesList.getSelectedValue().split(" ")[0];
                queryArea.setText("SELECT * FROM " + tableName + ";");
            }
        });
        tablesList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && tablesList.getSelectedValue() != null) {
                    String tableName = tablesList.getSelectedValue().split(" ")[0];
                    queryArea.setText("SELECT * FROM " + tableName + ";");
                    executeQuery();
                }
            }
        });
        leftPanel.add(new JScrollPane(tablesList), BorderLayout.CENTER);

        JPanel leftBtns = new JPanel(new GridLayout(2, 1, 3, 3));
        JButton refreshBtn = new JButton("Обновить список");
        refreshBtn.addActionListener(e -> refreshTablesList());
        leftBtns.add(refreshBtn);

        JButton resetBtn = new JButton("Заполнить демо (RESET)");
        resetBtn.setToolTipText("Восстановить исходные таблицы cars, owners, services с полными данными");
        resetBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Перезаполнить базу демонстрационными данными (cars, owners, services)?",
                    "Подтверждение сброса", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                db.clear();
                ConsoleRepl.seedDefault(db, storage, indexManager);
                refreshTablesList();
                queryArea.setText("SELECT * FROM cars;");
                executeQuery();
            }
        });
        leftBtns.add(resetBtn);
        leftPanel.add(leftBtns, BorderLayout.SOUTH);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, tableScroll);
        centerSplit.setDividerLocation(200);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, centerSplit);
        mainSplit.setDividerLocation(170);

        mainPanel.add(mainSplit, BorderLayout.CENTER);

        statusLabel = new JLabel("Готово к работе. Введите SQL-запрос.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        KeyStroke ctrlEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.CTRL_MASK);
        queryArea.getInputMap().put(ctrlEnter, "runQuery");
        queryArea.getActionMap().put("runQuery", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeQuery();
            }
        });

        setContentPane(mainPanel);
    }

    private void refreshTablesList() {
        tablesListModel.clear();
        for (String name : db.getTableNames()) {
            Table t = db.getTable(name);
            tablesListModel.addElement(name + " (" + t.rowCount() + " строк)");
        }
    }

    private void executeQuery() {
        String sql = queryArea.getText().trim();
        if (sql.isEmpty()) {
            statusLabel.setText("Введите SQL-запрос.");
            return;
        }

        long start = System.currentTimeMillis();
        try {
            Statement stmt = parser.parse(sql);
            QueryResult result = executor.execute(stmt);
            long elapsed = System.currentTimeMillis() - start;

            if (result.isSuccess()) {
                statusLabel.setForeground(new Color(0, 120, 0));
                statusLabel.setText(result.getMessage() + " (время: " + elapsed + " мс)");

                if (result.hasData()) {
                    displayData(result.getColumns(), result.getRows());
                } else {
                    tableModel.setDataVector(new Object[][]{}, new Object[]{"Сообщение"});
                    tableModel.addRow(new Object[]{result.getMessage()});
                }
            } else {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Ошибка: " + result.getMessage());
                tableModel.setDataVector(new Object[][]{}, new Object[]{"Ошибка"});
                tableModel.addRow(new Object[]{result.getMessage()});
            }
            refreshTablesList();
        } catch (Exception ex) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Ошибка: " + ex.getMessage());
        }
    }

    private void displayData(List<String> headers, List<Row> rows) {
        Vector<String> colNames = new Vector<>(headers);
        Vector<Vector<Object>> data = new Vector<>();

        for (Row r : rows) {
            Vector<Object> rowData = new Vector<>();
            for (String col : headers) {
                Object val = r.get(col);
                rowData.add(val == null ? "NULL" : val);
            }
            data.add(rowData);
        }

        tableModel.setDataVector(data, colNames);
    }

    private void showTablesInGrid() {
        Vector<String> headers = new Vector<>();
        headers.add("Таблица");
        headers.add("Количество строк");
        headers.add("Колонок");

        Vector<Vector<Object>> data = new Vector<>();
        for (String name : db.getTableNames()) {
            Table t = db.getTable(name);
            Vector<Object> row = new Vector<>();
            row.add(name);
            row.add(t.rowCount());
            row.add(t.getColumns().size());
            data.add(row);
        }
        tableModel.setDataVector(data, headers);
    }
}
