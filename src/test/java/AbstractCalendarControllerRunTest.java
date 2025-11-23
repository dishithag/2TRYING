import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarBook;
import calendar.CalendarBookImpl;
import calendar.CalendarImpl;
import calendar.controller.AbstractCalendarController;
import calendar.view.CalendarView;
import calendar.view.TextCalendarView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.ZoneId;
import org.junit.Test;

/**
 * Tests the shared run loop in AbstractCalendarController.
 */
public class AbstractCalendarControllerRunTest {

  /**
   * Controller used only for tests.
   */
  private static class TestController extends AbstractCalendarController {

    private final StringBuilder log = new StringBuilder();

    TestController(CalendarBook book, CalendarView view, StringReader reader) {
      super(book, view, reader);
    }

    @Override
    protected void displayWelcome() {
      log.append("welcome|");
    }

    @Override
    protected void showPrompt() {
      log.append("prompt|");
    }

    @Override
    protected void handleExit() {
      log.append("exit|");
    }

    @Override
    protected void handleError(String message, int lineNumber) {
      log.append("error|").append(lineNumber).append('|');
    }

    @Override
    protected void handleMissingExit() {
      log.append("missing|");
    }

    String getLog() {
      return log.toString();
    }
  }

  /**
   * A single exit line ends the loop and calls handleExit.
   */
  @Test
  public void testInteractive_exitImmediately() {
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    TestController c =
        new TestController(book, view, new StringReader("exit\n"));

    c.run();

    String log = c.getLog();
    assertTrue(log.contains("welcome|"));
    assertTrue(log.contains("prompt|"));
    assertTrue(log.contains("exit|"));
  }

  /**
   * Blank line is ignored, then exit is processed.
   */
  @Test
  public void testInteractive_blankThenExit() {
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    TestController c =
        new TestController(book, view, new StringReader("\nexit\n"));

    c.run();

    String log = c.getLog();
    assertTrue(log.startsWith("welcome|"));
    assertTrue(log.contains("prompt|"));
    assertTrue(log.contains("exit|"));
  }

  /**
   * End of file without exit in headless flow calls handleMissingExit.
   */
  @Test
  public void testHeadless_missingExit() {
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    TestController c =
        new TestController(book, view, new StringReader(""));

    c.run();

    String log = c.getLog();
    assertTrue(log.contains("missing|"));
  }

  /**
   * An unparsable line is reported through handleError with line number 1.
   */
  @Test
  public void testHeadless_badLine_reportsError() {
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    TestController c =
        new TestController(book, view, new StringReader("nonsense command\n"));

    c.run();

    String log = c.getLog();
    assertTrue(log.contains("error|1|"));
  }



}
