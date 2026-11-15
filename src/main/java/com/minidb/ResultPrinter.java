package com.minidb;

import java.util.ArrayList;
import java.util.List;

// Форматирование QueryResult в ASCII-таблицу
public class ResultPrinter {

    private static final int MIN_COL_WIDTH = 4;

    private ResultPrinter() { }

    /**
     * Форматирует результат выполнения запроса {@link QueryResult} в виде ASCII-таблицы.
     * При наличии ошибок или отсутствии строк возвращает текстовое сообщение.
     *
     * @param result результат выполнения запроса
     * @return отформатированная строка с псевдографической таблицей
     */
    public static String format(QueryResult result) {
        if (!result.isSuccess()) {
            return "ERROR: " + result.getMessage();
        }

        if (!result.hasData()) {
            return result.getMessage();
        }

        List<String> headers = result.getColumns();
        List<Row>    rows    = result.getRows();

        if (headers == null || headers.isEmpty()) {
            return result.getMessage();
        }

        int[] widths = computeWidths(headers, rows);

        StringBuilder sb = new StringBuilder();
        String divider = buildDivider(widths);

        sb.append(divider).append('\n');
        sb.append(buildRow(headers, widths)).append('\n');
        sb.append(divider).append('\n');

        for (Row row : rows) {
            List<String> cells = new ArrayList<>();
            for (String col : headers) {
                Object val = row.get(col);
                cells.add(val == null ? "NULL" : val.toString());
            }
            sb.append(buildRow(cells, widths)).append('\n');
        }

        sb.append(divider).append('\n');
        sb.append(result.getMessage());
        return sb.toString();
    }

    public static void print(QueryResult result) {
        System.out.println(format(result));
    }

    /**
     * Вычисляет максимальную ширину отображения для каждой колонки
     * с учетом длины заголовка и содержимого всех строк.
     *
     * @param headers имена колонок
     * @param rows    список строк данных
     * @return массив целочисленных ширин для каждой колонки
     */
    private static int[] computeWidths(List<String> headers, List<Row> rows) {
        int n = headers.size();
        int[] widths = new int[n];

        for (int i = 0; i < n; i++) {
            widths[i] = Math.max(MIN_COL_WIDTH, headers.get(i).length());
        }

        for (Row row : rows) {
            for (int i = 0; i < n; i++) {
                Object val = row.get(headers.get(i));
                String s   = (val == null) ? "NULL" : val.toString();
                widths[i] = Math.max(widths[i], s.length());
            }
        }
        return widths;
    }

    private static String buildDivider(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            for (int i = 0; i < w + 2; i++) sb.append('-');
            sb.append('+');
        }
        return sb.toString();
    }

    private static String buildRow(List<String> cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < cells.size(); i++) {
            String cell = cells.get(i);
            sb.append(' ').append(cell);
            for (int p = cell.length(); p < widths[i]; p++) sb.append(' ');
            sb.append(" |");
        }
        return sb.toString();
    }
}
