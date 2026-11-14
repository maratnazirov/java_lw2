package com.minidb;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndAndBenchmarkTest {

    private static final String TEST_DIR = "./test-data-e2e";
    private static StorageManager storage;
    private static IndexManager indexManager;
    private static Database db;
    private static SqlParser parser;
    private static QueryExecutor executor;

    @BeforeAll
    static void setup() throws IOException {
        cleanup();
        storage = new JsonStorageManager(TEST_DIR);
        indexManager = new IndexManager(TEST_DIR + "/indexes");
        db = Database.getInstance();
        db.clear();
        parser = new SqlParser();
        executor = new QueryExecutor(db, storage, indexManager);
    }

    @AfterAll
    static void cleanup() throws IOException {
        Path p = Paths.get(TEST_DIR);
        if (Files.exists(p)) {
            Files.walk(p)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private QueryResult exec(String sql) {
        Statement stmt = parser.parse(sql);
        return executor.execute(stmt);
    }

    @Test
    @Order(1)
    void testFullWorkflowRequiredByTask() {
        QueryResult res1 = exec("CREATE TABLE owners (id INTEGER PRIMARY KEY, name VARCHAR, phone VARCHAR)");
        assertTrue(res1.isSuccess());

        QueryResult res2 = exec("CREATE TABLE cars (id INTEGER PRIMARY KEY, brand VARCHAR, price FLOAT, active BOOLEAN)");
        assertTrue(res2.isSuccess());

        QueryResult ins1 = exec("INSERT INTO cars VALUES (1, 'Toyota', 25000.0, TRUE)");
        assertTrue(ins1.isSuccess());
        assertEquals(1, ins1.getAffectedRows());

        QueryResult ins2 = exec("INSERT INTO cars (id, brand) VALUES (2, 'BMW')");
        assertTrue(ins2.isSuccess());
        assertEquals(1, ins2.getAffectedRows());

        // Проверка уникальности PRIMARY KEY
        QueryResult insDup = exec("INSERT INTO cars (id, brand) VALUES (1, 'Audi')");
        assertFalse(insDup.isSuccess(), "Дубликат PRIMARY KEY должен возвращать ошибку");
        assertTrue(insDup.getMessage().contains("PRIMARY KEY"));

        QueryResult selAll = exec("SELECT * FROM cars");
        assertTrue(selAll.isSuccess());
        assertEquals(2, selAll.getRows().size());

        QueryResult selPrice = exec("SELECT brand, price FROM cars WHERE price > 20000");
        assertTrue(selPrice.isSuccess());
        assertEquals(1, selPrice.getRows().size());
        assertEquals("Toyota", selPrice.getRows().get(0).get("brand"));

        QueryResult upd = exec("UPDATE cars SET price = 30000 WHERE id = 2");
        assertTrue(upd.isSuccess());
        assertEquals(1, upd.getAffectedRows());

        QueryResult selBmw = exec("SELECT brand, price FROM cars WHERE id = 2");
        assertEquals(30000.0, selBmw.getRows().get(0).get("price"));

        QueryResult del = exec("DELETE FROM cars WHERE brand = 'Toyota'");
        assertTrue(del.isSuccess());
        assertEquals(1, del.getAffectedRows());

        QueryResult selAfterDel = exec("SELECT * FROM cars");
        assertEquals(1, selAfterDel.getRows().size());
        assertEquals("BMW", selAfterDel.getRows().get(0).get("brand"));
    }

    @Test
    @Order(2)
    void testIndexingAndPersistenceRecovery() throws IOException {
        exec("INSERT INTO cars VALUES (1, 'Toyota', 25000.0, TRUE)");

        QueryResult idxRes = exec("CREATE INDEX idx_cars_brand ON cars (brand)");
        assertTrue(idxRes.isSuccess());
        assertTrue(indexManager.hasIndex("cars", "brand"));

        QueryResult selIdx = exec("SELECT brand, price FROM cars WHERE brand = 'Toyota'");
        assertEquals(1, selIdx.getRows().size());
        assertEquals(25000.0, selIdx.getRows().get(0).get("price"));

        // Имитация перезапуска СУБД с восстановлением данных с диска
        Database.getInstance().clear();
        assertEquals(0, Database.getInstance().getTables().size());

        List<Table> loadedTables = storage.loadAll();
        for (Table t : loadedTables) {
            Database.getInstance().putTable(t);
        }
        IndexManager reloadedIndexMgr = new IndexManager(TEST_DIR + "/indexes");
        reloadedIndexMgr.loadAndRebuild(Database.getInstance().getTables());

        assertTrue(Database.getInstance().hasTable("cars"));
        assertEquals(2, Database.getInstance().getTable("cars").rowCount());
        assertTrue(reloadedIndexMgr.hasIndex("cars", "brand"));

        QueryExecutor newExec = new QueryExecutor(Database.getInstance(), storage, reloadedIndexMgr);
        QueryResult afterReloadSel = newExec.execute(parser.parse("SELECT * FROM cars WHERE brand = 'BMW'"));
        assertEquals(1, afterReloadSel.getRows().size());
        assertEquals("BMW", afterReloadSel.getRows().get(0).get("brand"));
    }

    @Test
    @Order(3)
    void testPerformanceBenchmark10kRows() {
        exec("CREATE TABLE benchmark (id INTEGER PRIMARY KEY, category VARCHAR, amount FLOAT)");
        Table table = db.getTable("benchmark");

        int N = 10000;
        for (int i = 0; i < N; i++) {
            Row r = new Row();
            r.set("id", i);
            r.set("category", "cat_" + (i % 100));
            r.set("amount", (double) i);
            table.addRowUnchecked(r);
        }

        // 1. Сканирование без индекса
        int queryCount = 200;
        long startScan = System.nanoTime();
        int matchedScan = 0;
        for (int q = 0; q < queryCount; q++) {
            String targetCat = "cat_" + (q % 100);
            QueryResult res = exec("SELECT * FROM benchmark WHERE category = '" + targetCat + "'");
            matchedScan += res.getRows().size();
        }
        double scanMs = (System.nanoTime() - startScan) / 1_000_000.0;

        QueryResult idxRes = exec("CREATE INDEX idx_bm_cat ON benchmark (category)");
        assertTrue(idxRes.isSuccess());

        // 2. Поиск через индекс
        long startIndex = System.nanoTime();
        int matchedIndex = 0;
        for (int q = 0; q < queryCount; q++) {
            String targetCat = "cat_" + (q % 100);
            QueryResult res = exec("SELECT * FROM benchmark WHERE category = '" + targetCat + "'");
            matchedIndex += res.getRows().size();
        }
        double indexMs = (System.nanoTime() - startIndex) / 1_000_000.0;

        assertEquals(matchedScan, matchedIndex);
        double speedup = scanMs / indexMs;
        System.out.printf("Speedup factor: %.2fx faster with Index!%n", speedup);
        assertTrue(speedup > 1.5, "Поиск по индексу должен быть значительно быстрее полного сканирования");
    }
}
