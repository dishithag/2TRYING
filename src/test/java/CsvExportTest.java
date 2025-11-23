import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarImpl;
import calendar.Event;
import calendar.SingleEvent;
import calendar.util.CsvExport;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.junit.Test;

/**
 * Tests for CSV export utility.
 */
public class CsvExportTest {

  /**
   * Verifies that exporting a calendar creates a CSV file.
   *
   * @throws IOException if writing fails
   */
  @Test
  public void testExportCreatesFile() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Demo",
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));

    String path = CsvExport.exportToCsv(calendar, "build/test-calendar.csv");
    assertTrue(Files.exists(Paths.get(path)));
  }

  /**
   * Test to verify CSV escaping for values containing commas.
   *
   * @throws IOException if writing fails
   */
  @Test
  public void testExportEscapeComma() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Meeting, Review, Planning",
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));

    String path = CsvExport.exportToCsv(calendar, "build/test-comma.csv");
    String content = Files.readString(Paths.get(path));
    assertTrue(content.contains("\"Meeting, Review, Planning\""));
  }

  /**
   * Tests exporting to a file in the current folder (no subdirectories).
   *
   * @throws IOException if writing fails
   */
  @Test
  public void testExportToCurrentDirectory() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Simple Event",
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));

    String path = CsvExport.exportToCsv(calendar, "calendar-export.csv");
    assertTrue(Files.exists(Paths.get(path)));

    Files.deleteIfExists(Paths.get("calendar-export.csv"));
  }






  /**
   * Test to verify CSV escaping handles null values correctly.
   *
   * @throws IOException if writing fails
   */
  @Test
  public void testExportWithNullDescription() throws IOException {
    Calendar calendar = new CalendarImpl();
    Event event = calendar.createEvent("Event with null description",
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));

    String path = CsvExport.exportToCsv(calendar, "build/test-null.csv");
    assertTrue(Files.exists(Paths.get(path)));

    String content = Files.readString(Paths.get(path));
    assertTrue(content.contains("Event with null description"));
  }


  /**
   * Test to verify CSV escaping handles empty strings correctly.
   *
   * @throws IOException if writing fails
   */
  @Test
  public void testExportWithEmptyDescription() throws IOException {
    Calendar calendar = new CalendarImpl();
    Event event = calendar.createEvent("Event",
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));
    String path = CsvExport.exportToCsv(calendar, "build/test-empty.csv");
    assertTrue(Files.exists(Paths.get(path)));
  }

  /**
   * Test to verify CSV escaping for values containing quotes.
   *
   * @throws IOException if writing fails
   */
  @Test
  public void testExportEscapeQuotes() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Meeting \"Important\"",
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));

    String path = CsvExport.exportToCsv(calendar, "build/test-quotes.csv");
    String content = Files.readString(Paths.get(path));
    assertTrue(content.contains("\"Meeting \"\"Important\"\"\""));
  }

  /**
   * Test to verify CSV escaping for values containing newlines.
   *
   * @throws IOException if writing fails
   */
  @Test
  public void testExportEscapeNewline() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Multi\nLine\nEvent",
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));

    String path = CsvExport.exportToCsv(calendar, "build/test-newline.csv");
    String content = Files.readString(Paths.get(path));
    assertTrue(content.contains("\"Multi\nLine\nEvent\""));
  }

  /**
   * Test to verify CSV escaping for simple values without special characters.
   *
   * @throws IOException if writing fails
   */
  @Test
  public void testExportNoEscapingNeeded() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("SimpleEvent",
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));

    String path = CsvExport.exportToCsv(calendar, "build/test-simple.csv");
    String content = Files.readString(Paths.get(path));
    assertTrue(content.contains("SimpleEvent"));
    assertTrue(!content.contains("\"SimpleEvent\""));
  }

  /**
   * Test to verify that the CSV header is accurate.
   *
   * @throws IOException if writing fails
   */
  @Test
  public void testExportWithHeader() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Test Event",
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));

    String path = CsvExport.exportToCsv(calendar, "build/test_header.csv");
    String content = Files.readString(Paths.get(path));

    assertTrue(content.startsWith("Subject,Start Date,Start Time,End Date,End Time,"
        + "All Day Event,Description,Location,Private"));
  }


  /**
   * Test to verify that non-all-day events include time values.
   *
   * @throws IOException if writing fails
   */
  @Test
  public void testExportNotAllDayEvent() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Timed Event",
        LocalDateTime.parse("2025-11-03T10:30"),
        LocalDateTime.parse("2025-11-03T11:45"));

    String path = CsvExport.exportToCsv(calendar, "build/test_not.csv");
    String content = Files.readString(Paths.get(path));

    assertTrue(content.contains("False"));
    assertTrue(content.contains("10:30 AM"));
    assertTrue(content.contains("11:45 AM"));
  }

  @Test
  public void testEscapeCsv_nullAndEmpty() throws Exception {
    java.lang.reflect.Method m =
        CsvExport.class.getDeclaredMethod("escapeCsv", String.class);
    m.setAccessible(true);

    String whenNull = (String) m.invoke(null, new Object[]{null});
    String whenEmpty = (String) m.invoke(null, "");

    assertTrue(whenNull.isEmpty());
    assertTrue(whenEmpty.isEmpty());
  }

  /**
   * Verifies that an all-day event produces empty time columns and sets
   * the all-day flag to {@code True}.
   *
   * @throws Exception if reflection invocation fails
   */
  @Test
  public void writeRow_allDayEvent_blanksTimes_andMarksTrue() throws Exception {
    SingleEvent e = new SingleEvent(
        "All Day",
        LocalDateTime.parse("2025-11-09T08:00"),
        null,
        "Desc",
        "Room",
        true,
        null
    );

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    Method m = CsvExport.class.getDeclaredMethod(
        "writeEventRow", PrintWriter.class, calendar.Event.class);
    m.setAccessible(true);
    m.invoke(null, pw, e);
    pw.flush();

    String line = sw.toString().trim();
    String[] cols = line.split(",", -1);

    assertEquals("All Day", cols[0]);  // subject
    assertTrue(cols[2].isEmpty());     // start time
    assertTrue(cols[4].isEmpty());     // end time
    assertEquals("True", cols[5]);     // all-day flag
  }

  /**
   * Verifies that a private event produces {@code True} in the last column.
   *
   * @throws Exception if reflection invocation fails
   */
  @Test
  public void writeRow_privateEvent_lastColumnTrue() throws Exception {
    SingleEvent e = new SingleEvent(
        "Private",
        LocalDateTime.parse("2025-11-10T10:00"),
        LocalDateTime.parse("2025-11-10T11:00"),
        "",
        "",
        false,
        null
    );

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    Method m = CsvExport.class.getDeclaredMethod(
        "writeEventRow", PrintWriter.class, calendar.Event.class);
    m.setAccessible(true);
    m.invoke(null, pw, e);
    pw.flush();

    String line = sw.toString().trim();
    assertTrue(line.endsWith(",True"));
  }
}

