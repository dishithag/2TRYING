import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarBook;
import calendar.CalendarBookImpl;
import calendar.CalendarImpl;
import calendar.controller.CalendarController;
import calendar.controller.InteractiveController;
import calendar.view.CalendarView;
import calendar.view.TextCalendarView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.ZoneId;
import org.junit.Test;

/**
 * Tests for the interactive controller.
 */
public class InteractiveControllerTest {

  /**
   * Runs a short interactive session.
   */
  @Test
  public void testInteractiveFlow() {
    String input = ""
        + "use calendar --name default\n"
        + "create event \"Demo\" from 2025-11-03T09:00 to 2025-11-03T10:00\n"
        + "show status on 2025-11-03T09:30\n"
        + "exit\n";

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(out);
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(ps);
    CalendarController controller =
        new InteractiveController(book, view, new StringReader(input));
    controller.run();

    String result = out.toString();
    assertTrue(result.contains("Event created: Demo"));
    assertTrue(result.contains("busy") || result.contains("Busy") || result.contains("BUSY"));
    assertTrue(result.contains("Goodbye"));
  }

  /**
   * Test handleExit displays goodbye message.
   */
  @Test
  public void testHandleExit_displaysGoodbye() {
    String input = "exit\n";

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(out));
    CalendarController controller =
        new InteractiveController(book, view, new StringReader(input));

    controller.run();

    String output = out.toString();
    assertTrue(output.contains("Goodbye"));
  }

  /**
   * Test displayWelcome shows all welcome messages.
   */
  @Test
  public void testDisplayWelcome_showsAllMessages() {
    String input = "exit\n";

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(out));
    CalendarController controller =
        new InteractiveController(book, view, new StringReader(input));

    controller.run();

    String output = out.toString();
    assertTrue(output.contains("interactive mode"));
    assertTrue(output.contains("exit'"));
  }

  @Test
  public void testShowPrompt_displaysPrompt() {
    String input = "exit\n";

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(out);
    PrintStream originalOut = System.out;
    System.setOut(ps);
    try {
      CalendarBook book = new CalendarBookImpl();
      Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
      CalendarView view = new TextCalendarView(ps);
      CalendarController controller =
          new InteractiveController(book, view, new StringReader(input));

      controller.run();

      String output = out.toString();
      assertTrue(output.contains("> "));
    } finally {
      System.setOut(originalOut);
    }
  }

  /**
   * Test handleError displays error without line number in interactive mode.
   */
  @Test
  public void testHandleError_displaysErrorMessage() {
    String input = "invalid command\nexit\n";

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(out));
    CalendarController controller =
        new InteractiveController(book, view, new StringReader(input));

    controller.run();

    String output = out.toString();
    assertTrue(output.contains("Error:"));
    assertTrue(output.contains("Unknown command"));

  }
}
