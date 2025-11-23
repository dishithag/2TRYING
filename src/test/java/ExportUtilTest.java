import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarImpl;
import calendar.util.ExportUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.junit.Test;

/**
 * Tests for ExportUtil.
 */
public class ExportUtilTest {

  /**
   * Test exporting to CSV file.
   */
  @Test
  public void testExport_csv() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = ExportUtil.export(calendar, "build/test-export.csv");

    assertTrue(Files.exists(Paths.get(path)));
    String content = Files.readString(Paths.get(path));
    assertTrue(content.contains("Meeting"));
  }

  /**
   * Test exporting to ICS file.
   */
  @Test
  public void testExport_ics() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Event",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = ExportUtil.export(calendar, "build/test-export.ics");

    assertTrue(Files.exists(Paths.get(path)));
    String content = Files.readString(Paths.get(path));
    assertTrue(content.contains("BEGIN:VCALENDAR"));
  }

  /**
   * Test exporting to ICAL file.
   */
  @Test
  public void testExport_ical() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Event",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = ExportUtil.export(calendar, "build/test-export.ical");

    assertTrue(Files.exists(Paths.get(path)));
    String content = Files.readString(Paths.get(path));
    assertTrue(content.contains("BEGIN:VCALENDAR"));
  }

  /**
   * Test exporting with unsupported extension throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testExport_unsupportedExtension_throws() throws IOException {
    Calendar calendar = new CalendarImpl();
    ExportUtil.export(calendar, "build/test-export.txt");
  }

  /**
   * Test exporting with uppercase CSV extension.
   */
  @Test
  public void testExport_csvUppercase() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = ExportUtil.export(calendar, "build/test-export.CSV");

    assertTrue(Files.exists(Paths.get(path)));
  }

  /**
   * Test exporting with mixed case ICS extension.
   */
  @Test
  public void testExport_icsMixedCase() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Event",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = ExportUtil.export(calendar, "build/test-export.IcS");

    assertTrue(Files.exists(Paths.get(path)));
  }
}