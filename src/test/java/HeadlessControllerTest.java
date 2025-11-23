import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarBook;
import calendar.CalendarBookImpl;
import calendar.CalendarImpl;
import calendar.controller.CalendarController;
import calendar.controller.HeadlessController;
import calendar.view.CalendarView;
import calendar.view.TextCalendarView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.ZoneId;
import org.junit.Test;

/**
 * Tests for HeadlessController error handling.
 */
public class HeadlessControllerTest {

  /**
   * Test handleError is called with line number when command fails.
   */
  @Test
  public void testHandleError_displayedWithLineNumber() {
    String input = "invalid command\nexit\n";

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(out));
    CalendarController controller =
        new HeadlessController(book, view, new StringReader(input));

    controller.run();

    String output = out.toString();
    assertTrue(output.contains("Error: Line 1:"));
    assertTrue(output.contains("Unknown command"));
  }

  /**
   * Test handleMissingExit is called when file ends without exit.
   */
  @Test
  public void testHandleMissingExit_displayedWhenNoExit() {
    String input = "create event \"Test\" from 2025-11-03T09:00 to 2025-11-03T10:00\n";


    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(out));
    CalendarController controller =
        new HeadlessController(book, view, new StringReader(input));

    controller.run();

    String output = out.toString();
    assertTrue(output.contains("Script ended without 'exit'"));
  }


}