package com.minidb;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MiniDbTest {

    private static final String TEST_DATA_DIR = "./test-data-" + System.currentTimeMillis();
    private static StorageManager storage;
    private static SqlParser parser;

    @BeforeAll
    static void setup() throws IOException {
        storage = new JsonStorageManager(TEST_DATA_DIR);
        parser  = new SqlParser();
        Database.getInstance().clear();
    }

    @AfterAll
    static void cleanup() throws IOException {
        if (Files.exists(Paths.get(TEST_DATA_DIR))) {
            Files.walk(Paths.get(TEST_DATA_DIR))
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(java.io.File::delete);
        }
    }

    @Test @Order(1)
    void testRowSetGet() {
        Row row = new Row();
        row.set("id", 1);
        row.set("name", "Toyota");
        assertEquals(1, row.get("id"));
        assertEquals("Toyota", row.get("name"));
        assertNull(row.get("missing"));
    }

    @Test @Order(2)
    void testTablePrimaryKeyUniqueness() {
        List<ColumnDefinition> cols = Arrays.asList(
            new ColumnDefinition("id", DataType.INTEGER, true, false),
            new ColumnDefinition("name", DataType.STRING, false, false)
        );
        Table t = new Table("test_pk", cols);
        Row r1 = new Row(); r1.set("id", 1); r1.set("name", "A");
        Row r2 = new Row(); r2.set("id", 1); r2.set("name", "B");

        t.insertRow(r1);
        assertThrows(IllegalArgumentException.class, () -> t.insertRow(r2));
    }

    @Test @Order(3)
    void testTableDelete() {
        List<ColumnDefinition> cols = Arrays.asList(
            new ColumnDefinition("id", DataType.INTEGER, true, false)
        );
        Table t = new Table("test_del", cols);
        for (int i = 1; i <= 5; i++) {
            Row r = new Row(); r.set("id", i);
            t.insertRow(r);
        }
        int deleted = t.deleteRows(row -> (Integer) row.get("id") > 3);
        assertEquals(2, deleted);
        assertEquals(3, t.rowCount());
    }

    @Test @Order(10)
    void testParseCreateTable() {
        String sql = "CREATE TABLE cars (id INTEGER PRIMARY KEY, brand VARCHAR, price FLOAT, active BOOLEAN)";
        Statement stmt = parser.parse(sql);
        assertEquals(StatementType.CREATE_TABLE, stmt.getType());
        CreateTableStatement ct = (CreateTableStatement) stmt;
        assertEquals("cars", ct.getTableName());
        assertEquals(4, ct.getColumns().size());
        assertTrue(ct.getColumns().get(0).isPrimaryKey());
    }

    @Test @Order(11)
    void testParseInsertAllColumns() {
        String sql = "INSERT INTO cars VALUES (1, 'Toyota', 25000.0, TRUE)";
        Statement stmt = parser.parse(sql);
        assertEquals(StatementType.INSERT, stmt.getType());
        InsertStatement ins = (InsertStatement) stmt;
        assertEquals("cars", ins.getTableName());
        assertEquals(4, ins.getValues().size());
        assertEquals(1, ins.getValues().get(0));
        assertEquals("Toyota", ins.getValues().get(1));
        assertEquals(25000.0, ins.getValues().get(2));
        assertEquals(Boolean.TRUE, ins.getValues().get(3));
    }

    @Test @Order(12)
    void testParseInsertNamedColumns() {
        String sql = "INSERT INTO cars (id, brand) VALUES (2, 'BMW')";
        InsertStatement ins = (InsertStatement) parser.parse(sql);
        assertEquals(2, ins.getColumnNames().size());
        assertEquals("id", ins.getColumnNames().get(0));
        assertEquals(2, ins.getValues().get(0));
        assertEquals("BMW", ins.getValues().get(1));
    }

    @Test @Order(13)
    void testParseSelectStar() {
        SelectStatement sel = (SelectStatement) parser.parse("SELECT * FROM cars");
        assertEquals("cars", sel.getTableName());
        assertTrue(sel.getColumns().isEmpty());
        assertNull(sel.getWhere());
    }

    @Test @Order(14)
    void testParseSelectWithWhere() {
        SelectStatement sel = (SelectStatement)
                parser.parse("SELECT brand, price FROM cars WHERE price > 20000");
        assertEquals(2, sel.getColumns().size());
        assertNotNull(sel.getWhere());
        assertEquals("price", sel.getWhere().getColumnName());
        assertEquals(">", sel.getWhere().getOperator());
        assertEquals(20000, sel.getWhere().getValue());
    }

    @Test @Order(15)
    void testParseUpdate() {
        UpdateStatement upd = (UpdateStatement)
                parser.parse("UPDATE cars SET price = 30000 WHERE id = 2");
        assertEquals("cars", upd.getTableName());
        assertEquals(1, upd.getAssignments().size());
        assertNotNull(upd.getWhere());
    }

    @Test @Order(16)
    void testParseDelete() {
        DeleteStatement del = (DeleteStatement)
                parser.parse("DELETE FROM cars WHERE brand = 'Toyota'");
        assertEquals("cars", del.getTableName());
        assertEquals("brand", del.getWhere().getColumnName());
        assertEquals("Toyota", del.getWhere().getValue());
    }

    @Test @Order(17)
    void testParseDropTable() {
        DropTableStatement drop = (DropTableStatement) parser.parse("DROP TABLE cars");
        assertEquals("cars", drop.getTableName());
    }

    @Test @Order(18)
    void testParseCreateIndex() {
        CreateIndexStatement ci = (CreateIndexStatement)
                parser.parse("CREATE INDEX idx_cars_brand ON cars (brand)");
        assertEquals("idx_cars_brand", ci.getIndexName());
        assertEquals("cars", ci.getTableName());
        assertEquals("brand", ci.getColumnName());
    }

    @Test @Order(19)
    void testParseSyntaxError() {
        assertThrows(ParseException.class, () -> parser.parse("SELECT FROM"));
        assertThrows(ParseException.class, () -> parser.parse("INSERT cars VALUES (1)"));
    }

    @Test @Order(30)
    void testSaveAndLoad() throws IOException {
        List<ColumnDefinition> cols = Arrays.asList(
            new ColumnDefinition("id",    DataType.INTEGER, true,  false),
            new ColumnDefinition("brand", DataType.STRING,  false, false),
            new ColumnDefinition("price", DataType.FLOAT,   false, false)
        );
        Table original = new Table("cars_test", cols);
        Row r = new Row();
        r.set("id", 1); r.set("brand", "Toyota"); r.set("price", 25000.0);
        original.insertRow(r);

        storage.save(original);
        Table loaded = storage.load("cars_test");
        assertEquals(1, loaded.rowCount());
        assertEquals("Toyota", loaded.getRows().get(0).get("brand"));
        assertEquals(25000.0,  loaded.getRows().get(0).get("price"));
        assertEquals(1,        loaded.getRows().get(0).get("id"));
    }

    @Test @Order(40)
    void testWhereConditionEquals() {
        WhereCondition wc = new WhereCondition("price", "=", 25000.0);
        Row r1 = new Row(); r1.set("price", 25000.0);
        Row r2 = new Row(); r2.set("price", 30000.0);
        assertTrue(wc.toPredicate().test(r1));
        assertFalse(wc.toPredicate().test(r2));
    }

    @Test @Order(41)
    void testWhereConditionGreater() {
        WhereCondition wc = new WhereCondition("price", ">", 20000.0);
        Row r = new Row(); r.set("price", 25000.0);
        assertTrue(wc.toPredicate().test(r));
        Row r2 = new Row(); r2.set("price", 10000.0);
        assertFalse(wc.toPredicate().test(r2));
    }

    @Test @Order(42)
    void testWhereConditionStringEquals() {
        WhereCondition wc = new WhereCondition("brand", "=", "Toyota");
        Row r = new Row(); r.set("brand", "Toyota");
        assertTrue(wc.toPredicate().test(r));
    }

    @Test @Order(50)
    void testParseShowTables() {
        Statement stmt1 = parser.parse("SHOW TABLES");
        assertEquals(StatementType.SHOW_TABLES, stmt1.getType());

        Statement stmt2 = parser.parse("SHOW TABLES;");
        assertEquals(StatementType.SHOW_TABLES, stmt2.getType());

        Statement stmt3 = parser.parse("TABLES");
        assertEquals(StatementType.SHOW_TABLES, stmt3.getType());
    }

    @Test @Order(51)
    void testParseTableShortcut() {
        Statement stmt = parser.parse("TABLE cars;");
        assertEquals(StatementType.SELECT, stmt.getType());
        SelectStatement sel = (SelectStatement) stmt;
        assertEquals("cars", sel.getTableName());
        assertTrue(sel.getColumns().isEmpty());
    }

    @Test @Order(52)
    void testParseDescribeTable() {
        Statement stmt = parser.parse("DESCRIBE cars;");
        assertEquals(StatementType.DESCRIBE_TABLE, stmt.getType());
        DescribeTableStatement desc = (DescribeTableStatement) stmt;
        assertEquals("cars", desc.getTableName());

        Statement stmt2 = parser.parse("DESC cars;");
        assertEquals(StatementType.DESCRIBE_TABLE, stmt2.getType());
    }

    @Test @Order(53)
    void testExecuteShowTablesAndDescribe() throws IOException {
        Database db = Database.getInstance();
        Table t = new Table("test_table", Collections.singletonList(
                new ColumnDefinition("id", DataType.INTEGER, true, false)));
        db.putTable(t);
        IndexManager im = new IndexManager(TEST_DATA_DIR + "/indexes");
        QueryExecutor qe = new QueryExecutor(db, storage, im);

        QueryResult resShow = qe.execute(new ShowTablesStatement());
        assertTrue(resShow.isSuccess());
        assertTrue(resShow.hasData());

        QueryResult resDesc = qe.execute(new DescribeTableStatement("test_table"));
        assertTrue(resDesc.isSuccess());
        assertEquals(1, resDesc.getRows().size());
        assertEquals("id", resDesc.getRows().get(0).get("column_name"));
    }
}
