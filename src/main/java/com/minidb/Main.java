package com.minidb;

import java.io.File;
import java.io.IOException;

// Точка входа в приложение MiniDB
public class Main {

    /**
     * Точка входа в приложение MiniDB.
     * <p>
     * Обрабатывает аргументы командной строки:
     * <ul>
     *   <li>{@code --gui} — запуск графического интерфейса Swing;</li>
     *   <li>неименованный аргумент — пользовательский путь к каталогу хранения данных.</li>
     * </ul>
     * При отсутствии явного пути выполняет автоматический поиск директории {@code data/},
     * проверяя текущий рабочий каталог, родительский каталог и директорию рядом с исполняемым JAR-файлом.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        boolean useGui = false;
        String dataDir = null;

        for (String arg : args) {
            if ("--gui".equalsIgnoreCase(arg) || "-gui".equalsIgnoreCase(arg)) {
                useGui = true;
            } else if (!arg.startsWith("-")) {
                dataDir = arg;
            }
        }

        // Определение каталога данных
        if (dataDir == null) {
            File cwdData = new File("./data");
            File parentDataCandidate = new File("../data");
            File fixedData = new File("c:/java_lw2/data");
            if (cwdData.exists() && cwdData.isDirectory()) {
                dataDir = cwdData.getAbsolutePath();
            } else if (parentDataCandidate.exists() && parentDataCandidate.isDirectory()) {
                dataDir = parentDataCandidate.getAbsolutePath();
            } else if (fixedData.exists() && fixedData.isDirectory()) {
                dataDir = fixedData.getAbsolutePath();
            } else {
                try {
                    File jarPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    File jarDir = jarPath.getParentFile();
                    if (jarDir != null) {
                        File nextToJar = new File(jarDir, "data");
                        if (nextToJar.exists() && nextToJar.isDirectory()) {
                            dataDir = nextToJar.getAbsolutePath();
                        } else if (jarDir.getParentFile() != null) {
                            File parentData = new File(jarDir.getParentFile(), "data");
                            if (parentData.exists() && parentData.isDirectory()) {
                                dataDir = parentData.getAbsolutePath();
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            if (dataDir == null) {
                dataDir = "./data";
            }
        }

        final String finalDataDir = dataDir;
        if (useGui) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    javax.swing.UIManager.setLookAndFeel(
                            javax.swing.UIManager.getSystemLookAndFeelClassName());
                    GuiWindow gui = new GuiWindow(finalDataDir);
                    gui.setVisible(true);
                } catch (Exception e) {
                    System.err.println("Failed to launch GUI: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            try {
                ConsoleRepl repl = new ConsoleRepl(dataDir);
                repl.run();
            } catch (IOException e) {
                System.err.println("FATAL: Could not initialise storage at '" + dataDir + "': " + e.getMessage());
                System.exit(1);
            }
        }
    }
}
