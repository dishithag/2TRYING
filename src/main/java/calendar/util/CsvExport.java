package calendar.util;

import calendar.Calendar;
import calendar.Event;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility for exporting calendars to CSV format.
 * Follows Single Responsibility Principle.
 */
public class CsvExport {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final DateTimeFormatter TIME_FMT =
      DateTimeFormatter.ofPattern("hh:mm a");

  private CsvExport() {

  }

  /**
   * Exports calendar to CSV file compatible with Google Calendar.
   *
   * @param calendar the calendar to export
   * @param filePath the file path
   * @return absolute path of created file
   * @throws IOException if file cannot be created
   */
  public static String exportToCsv(Calendar calendar, String filePath)
      throws IOException {
    Path path = Paths.get(filePath);
    if (path.getParent() != null) {
      Files.createDirectories(path.getParent());
    }

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
      writer.println("Subject,Start Date,Start Time,End Date,End Time,"
          + "All Day Event,Description,Location,Private");

      List<Event> events = calendar.getAllEvents();
      for (Event event : events) {
        writeEventRow(writer, event);
      }
    }

    return path.toAbsolutePath().toString();
  }

  private static void writeEventRow(PrintWriter writer, Event event) {
    writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
        escapeCsv(event.getSubject()),
        event.getStartDateTime().format(DATE_FMT),
        event.isAllDayEvent() ? "" : event.getStartDateTime().format(TIME_FMT),
        event.getEndDateTime().format(DATE_FMT),
        event.isAllDayEvent() ? "" : event.getEndDateTime().format(TIME_FMT),
        event.isAllDayEvent() ? "True" : "False",
        escapeCsv(event.getDescription().orElse("")),
        escapeCsv(event.getLocation().orElse("")),
        event.isPublic() ? "False" : "True");
  }

  private static String escapeCsv(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}

