package com.minidb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Консольный REPL интерфейс для MiniDB
public class ConsoleRepl {

    private static final String RESET  = "\u001B[0m";
    private static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";

    private final StorageManager storage;
    private final Database       db;
    private final IndexManager   indexManager;
    private final SqlParser      parser;
    private final QueryExecutor  executor;

    public ConsoleRepl(String dataDir) throws IOException {
        this.storage      = new JsonStorageManager(dataDir);
        this.db           = Database.getInstance();
        this.indexManager = new IndexManager(dataDir + "/indexes");
        this.parser       = new SqlParser();
        this.executor     = new QueryExecutor(db, storage, indexManager);

        loadDatabase();
    }

    private void loadDatabase() throws IOException {
        List<Table> tables = storage.loadAll();
        for (Table t : tables) {
            db.putTable(t);
            System.out.println(CYAN + "  Loaded table: " + t.getName()
                    + " (" + t.rowCount() + " rows)" + RESET);
        }

        indexManager.loadAndRebuild(db.getTables());

        if (!db.hasTable("owners") || !db.hasTable("cars") || !db.hasTable("services")) {
            seedDefault(db, storage, indexManager);
        }

        System.out.println(CYAN + "  " + db.getTableNames().size() + " table(s) ready: "
                + String.join(", ", db.getTableNames()) + RESET);
    }

    static void seedDefault(Database db, StorageManager storage, IndexManager indexManager) {
        try {
            System.out.println("  Initializing default sample tables (cars, owners, services)...");
            if (!db.hasTable("owners")) {
                List<ColumnDefinition> ownerCols = new ArrayList<>();
                ownerCols.add(new ColumnDefinition("id", DataType.INTEGER, true, false));
                ownerCols.add(new ColumnDefinition("full_name", DataType.STRING, false, false));
                ownerCols.add(new ColumnDefinition("phone", DataType.STRING, false, false));

                Table owners = new Table("owners", ownerCols);
                addOwner(owners, 1, "Ivan Ivanov", "+79991112233");
                addOwner(owners, 2, "Petr Petrov", "+79994445566");
                addOwner(owners, 3, "Anna Smirnova", "+79161234567");
                addOwner(owners, 4, "Dmitry Kozlov", "+79219876543");
                addOwner(owners, 5, "Elena Vasilyeva", "+79035557788");
                addOwner(owners, 6, "Sergey Morozov", "+79183332211");
                addOwner(owners, 7, "Maria Fedorova", "+79264448899");
                addOwner(owners, 8, "Alexey Novikov", "+79051113355");
                addOwner(owners, 9, "Olga Popova", "+79117779922");
                addOwner(owners, 10, "Mikhail Sokolov", "+79852226644");
                db.putTable(owners);
                storage.save(owners);
            }

            if (!db.hasTable("cars")) {
                List<ColumnDefinition> carCols = new ArrayList<>();
                carCols.add(new ColumnDefinition("id", DataType.INTEGER, true, false));
                carCols.add(new ColumnDefinition("brand", DataType.STRING, false, false));
                carCols.add(new ColumnDefinition("price", DataType.FLOAT, false, false));
                carCols.add(new ColumnDefinition("owner_id", DataType.INTEGER, false, false));
                carCols.add(new ColumnDefinition("active", DataType.BOOLEAN, false, false));
                carCols.add(new ColumnDefinition("reg_date", DataType.DATE, false, false));

                Table cars = new Table("cars", carCols);
                addCar(cars, 1, "Toyota Camry", 25000.0, 1, true, "2023-04-12");
                addCar(cars, 2, "BMW X5", 45000.0, 2, true, "2022-09-20");
                addCar(cars, 3, "Lada Vesta", 12000.0, 1, false, "2021-11-05");
                addCar(cars, 4, "Mercedes E-Class", 55000.0, 3, true, "2023-01-18");
                addCar(cars, 5, "Audi A6", 42000.0, 4, true, "2022-06-30");
                addCar(cars, 6, "Porsche 911", 115000.0, 5, true, "2024-02-14");
                addCar(cars, 7, "Tesla Model 3", 38000.0, 6, true, "2023-08-22");
                addCar(cars, 8, "Honda Civic", 19500.0, 7, false, "2020-05-15");
                addCar(cars, 9, "Hyundai Solaris", 13500.0, 8, true, "2021-03-10");
                addCar(cars, 10, "Nissan Qashqai", 22000.0, 9, true, "2022-12-01");
                addCar(cars, 11, "Volvo XC90", 62000.0, 10, true, "2023-11-25");
                addCar(cars, 12, "Kia Sportage", 24000.0, 3, false, "2021-07-19");
                addCar(cars, 13, "Mazda CX-5", 27000.0, 4, true, "2023-03-08");
                addCar(cars, 14, "VW Tiguan", 31000.0, 2, true, "2022-10-14");
                addCar(cars, 15, "BMW M3", 68000.0, 5, true, "2023-09-05");
                addCar(cars, 16, "Toyota RAV4", 29000.0, 6, true, "2022-04-17");
                addCar(cars, 17, "Audi Q7", 59000.0, 7, true, "2023-06-21");
                addCar(cars, 18, "Ford Focus", 14000.0, 8, false, "2019-08-11");
                addCar(cars, 19, "Skoda Octavia", 21500.0, 9, true, "2022-02-28");
                addCar(cars, 20, "Lexus RX", 49000.0, 10, true, "2023-10-09");
                db.putTable(cars);
                storage.save(cars);

                Index idxBrand = indexManager.createIndex("idx_cars_brand", "cars", "brand", "TREE");
                indexManager.rebuildIndex(idxBrand, cars);
                Index idxOwner = indexManager.createIndex("idx_cars_owner", "cars", "owner_id", "TREE");
                indexManager.rebuildIndex(idxOwner, cars);
            }

            if (!db.hasTable("services")) {
                List<ColumnDefinition> serviceCols = new ArrayList<>();
                serviceCols.add(new ColumnDefinition("id", DataType.INTEGER, true, false));
                serviceCols.add(new ColumnDefinition("car_id", DataType.INTEGER, false, false));
                serviceCols.add(new ColumnDefinition("service_type", DataType.STRING, false, false));
                serviceCols.add(new ColumnDefinition("cost", DataType.FLOAT, false, false));
                serviceCols.add(new ColumnDefinition("service_date", DataType.DATE, false, false));
                serviceCols.add(new ColumnDefinition("completed", DataType.BOOLEAN, false, false));

                Table services = new Table("services", serviceCols);
                addService(services, 1, 1, "Oil Change", 150.0, "2023-05-10", true);
                addService(services, 2, 1, "Brake Pads", 350.0, "2023-10-15", true);
                addService(services, 3, 2, "Scheduled Maintenance", 600.0, "2023-01-20", true);
                addService(services, 4, 3, "Engine Diagnostics", 200.0, "2022-04-11", true);
                addService(services, 5, 4, "Tire Replacement", 800.0, "2023-08-05", true);
                addService(services, 6, 5, "Transmission Flush", 450.0, "2023-09-12", true);
                addService(services, 7, 6, "Track Inspection", 1200.0, "2024-03-01", true);
                addService(services, 8, 7, "Battery Diagnostics", 250.0, "2023-11-18", true);
                addService(services, 9, 8, "Air Filter & Spark Plugs", 180.0, "2021-06-25", true);
                addService(services, 10, 9, "Suspension Check", 320.0, "2022-05-30", true);
                addService(services, 11, 10, "Scheduled Maintenance", 550.0, "2023-07-22", true);
                addService(services, 12, 11, "Brake Fluid Change", 190.0, "2024-01-10", true);
                addService(services, 13, 14, "Wheel Alignment", 120.0, "2023-04-05", true);
                addService(services, 14, 15, "Performance Tuning", 1500.0, "2023-12-14", true);
                addService(services, 15, 20, "Annual Inspection", 400.0, "2024-02-20", false);
                db.putTable(services);
                storage.save(services);

                Index idxServCar = indexManager.createIndex("idx_serv_car", "services", "car_id", "TREE");
                indexManager.rebuildIndex(idxServCar, services);
            }

            indexManager.saveDescriptors();

            System.out.println(GREEN + "  Sample tables ready: 'owners' (10 rows), 'cars' (20 rows), 'services' (15 rows)." + RESET);
        } catch (Exception e) {
            System.err.println("Failed to seed default tables: " + e.getMessage());
        }
    }

    private static void addOwner(Table table, int id, String fullName, String phone) {
        Row r = new Row();
        r.set("id", id);
        r.set("full_name", fullName);
        r.set("phone", phone);
        table.addRowUnchecked(r);
    }

    private static void addCar(Table table, int id, String brand, double price, int ownerId, boolean active, String regDate) {
        Row r = new Row();
        r.set("id", id);
        r.set("brand", brand);
        r.set("price", price);
        r.set("owner_id", ownerId);
        r.set("active", active);
        r.set("reg_date", java.time.LocalDate.parse(regDate));
        table.addRowUnchecked(r);
    }

    private static void addService(Table table, int id, int carId, String serviceType, double cost, String serviceDate, boolean completed) {
        Row r = new Row();
        r.set("id", id);
        r.set("car_id", carId);
        r.set("service_type", serviceType);
        r.set("cost", cost);
        r.set("service_date", java.time.LocalDate.parse(serviceDate));
        r.set("completed", completed);
        table.addRowUnchecked(r);
    }

    public void run() {
        printBanner();
        Scanner scanner = new Scanner(System.in);
        StringBuilder buffer = new StringBuilder();

        while (true) {
            System.out.print(buffer.length() == 0 ? "minidb> " : "     -> ");
            if (!scanner.hasNextLine()) break;

            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            buffer.append(' ').append(line);
            String input = buffer.toString().trim();

            String upper = input.toUpperCase().trim();
            if (upper.endsWith(";")) {
                upper = upper.substring(0, upper.length() - 1).trim();
            }

            if (upper.equals("EXIT") || upper.equals("QUIT")) {
                System.out.println(YELLOW + "Bye!" + RESET);
                break;
            }
            if (upper.equals("HELP") || upper.equals("?")) {
                printHelp();
                buffer.setLength(0);
                continue;
            }
            if (upper.equals("RESET")) {
                db.clear();
                seedDefault(db, storage, indexManager);
                showTables();
                buffer.setLength(0);
                continue;
            }
            if (upper.equals("RELOAD") || upper.equals("REFRESH")) {
                db.clear();
                try {
                    loadDatabase();
                } catch (IOException e) {
                    System.err.println("Failed to reload: " + e.getMessage());
                }
                showTables();
                buffer.setLength(0);
                continue;
            }
            if (upper.equals("SHOW TABLES") || upper.equals("TABLES") || upper.equals("TABLE")
                    || upper.equals("SHOW") || upper.equals("LIST")
                    || upper.equals("\\DT") || upper.equals("\\D")) {
                showTables();
                buffer.setLength(0);
                continue;
            }

            if (input.endsWith(";") || looksComplete(input)) {
                executeInput(input);
                buffer.setLength(0);
            }
        }
        scanner.close();
    }

    private boolean looksComplete(String input) {
        String u = input.trim().toUpperCase();
        return u.startsWith("SELECT") || u.startsWith("INSERT")
                || u.startsWith("UPDATE") || u.startsWith("DELETE")
                || u.startsWith("CREATE") || u.startsWith("DROP")
                || u.startsWith("SHOW") || u.startsWith("HELP")
                || u.startsWith("EXIT") || u.startsWith("QUIT")
                || u.startsWith("TABLE") || u.startsWith("TABLES")
                || u.startsWith("DESC");
    }

    private void executeInput(String sql) {
        try {
            Statement stmt   = parser.parse(sql);
            QueryResult result = executor.execute(stmt);
            if (result.isSuccess()) {
                System.out.println(GREEN + ResultPrinter.format(result) + RESET);
            } else {
                System.out.println(RED + ResultPrinter.format(result) + RESET);
            }
        } catch (ParseException e) {
            System.out.println(RED + "Parse error: " + e.getMessage() + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Error: " + e.getMessage() + RESET);
        }
    }

    private void showTables() {
        System.out.println(CYAN + "Tables in database:" + RESET);
        if (db.getTableNames().isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (String name : db.getTableNames()) {
                Table t = db.getTable(name);
                List<String> colNames = new ArrayList<>();
                for (ColumnDefinition col : t.getColumns()) {
                    colNames.add(col.getName() + " (" + col.getType() + (col.isPrimaryKey() ? " PK" : "") + ")");
                }
                System.out.printf("  * %-8s [%d row(s)] Columns: %s%n",
                        name, t.rowCount(), String.join(", ", colNames));
            }
        }
    }

    private void printBanner() {
        System.out.println(CYAN);
        System.out.println("  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║            MiniDB v1.0.0 (Java)              ║");
        System.out.println("  ║     Lightweight SQL Engine with Indexing     ║");
        System.out.println("  ╚══════════════════════════════════════════════╝");
        System.out.println("  Available tables: " + String.join(", ", db.getTableNames()));
        System.out.println("  Quick start: SELECT * FROM cars;  or  SHOW TABLES;");
        System.out.println("  Type HELP for syntax, EXIT to quit.");
        System.out.println(RESET);
    }

    private void printHelp() {
        System.out.println(YELLOW + "Available commands:" + RESET);
        System.out.println("  CREATE TABLE t (col TYPE [PRIMARY KEY] [NOT NULL], ...)");
        System.out.println("  DROP TABLE t");
        System.out.println("  INSERT INTO t VALUES (v1, v2, ...)");
        System.out.println("  INSERT INTO t (col1, col2) VALUES (v1, v2)");
        System.out.println("  SELECT * FROM t [WHERE col op val]");
        System.out.println("  SELECT col1, col2 FROM t [WHERE col op val]");
        System.out.println("  UPDATE t SET col=val [WHERE col op val]");
        System.out.println("  DELETE FROM t [WHERE col op val]");
        System.out.println("  CREATE INDEX name ON t (col)");
        System.out.println("  SHOW TABLES");
        System.out.println("  HELP | EXIT");
        System.out.println("  Operators: = != < <= > >=");
        System.out.println("  Types: INTEGER FLOAT VARCHAR DATE BOOLEAN");
    }
}
