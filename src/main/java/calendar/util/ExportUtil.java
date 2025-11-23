package calendar.util;

import calendar.Calendar;
import java.io.IOException;

/**
 * Dispatches calendar exports based on file extension.
 * Supports .csv, .ics, and .ical.
 */
public final class ExportUtil {

  private ExportUtil() {}

  /**
   * Exports the given calendar to the path inferred by extension.
   *
   * @param calendar the calendar to export
   * @param filePath output path; extension determines format
   * @return absolute path of the created file
   * @throws IOException if writing fails
   */
  public static String export(Calendar calendar, String filePath) throws IOException {
    String lower = filePath.toLowerCase();
    if (lower.endsWith(".csv")) {
      return CsvExport.exportToCsv(calendar, filePath);
    }
    if (lower.endsWith(".ics") || lower.endsWith(".ical")) {
      return IcalExport.exportToIcs(calendar, filePath);
    }
    throw new IllegalArgumentException("Unsupported export extension. Use .csv, .ics, or .ical");
  }
}
