import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.Calendar;
import calendar.CalendarBook;
import calendar.CalendarBookImpl;
import calendar.CalendarImpl;
import calendar.Event;
import calendar.controller.AbstractCalendarController;
import calendar.controller.CommandParser;
import calendar.controller.CommandType;
import calendar.view.CalendarView;
import calendar.view.TextCalendarView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.Test;

/**
 * Extra controller tests to exercise executeCommand and handleEdit branches.
 */
public class AbstractCalendarControllerExecTest {

  /**
   * Controller that exposes executeCommand for testing.
   */
  private static class ExposedController extends AbstractCalendarController {

    private final StringBuilder log = new StringBuilder();

    ExposedController(CalendarBook book, CalendarView view, StringReader reader) {
      super(book, view, reader);
    }

    @Override
    protected void displayWelcome() {
    }

    @Override
    protected void showPrompt() {
    }

    @Override
    protected void handleExit() {
    }

    @Override
    protected void handleError(String message, int lineNumber) {
      log.append("err:").append(lineNumber).append(':').append(message).append('\n');
    }

    @Override
    protected void handleMissingExit() {
      log.append("missing\n");
    }

    void runCommand(CommandParser.Command cmd) {
      executeCommand(cmd);
    }

    String getLog() {
      return log.toString();
    }
  }

  /**
   * Verifies the null check in the base controller constructor.
   */
  @Test
  public void testConstructorNullArguments() {
    CalendarBook book = new CalendarBookImpl();
    book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(System.out);
    StringReader reader = new StringReader("");

    try {
      new ExposedController(null, view, reader);
      fail("Expected exception for null model");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("cannot be null"));
    }

    try {
      new ExposedController(book, null, reader);
      fail("Expected exception for null view");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("cannot be null"));
    }

    try {
      new ExposedController(book, view, null);
      fail("Expected exception for null input");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("cannot be null"));
    }
  }

  /**
   * Creates a single event through the controller.
   */
  @Test
  public void testExecuteCreateSingle() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    controller.runCommand(parser.parse(
        "create event \"Demo\" from 2025-11-03T09:00 to 2025-11-03T10:00"));

    List<Event> events =
        model.getEventsOnDate(LocalDate.of(2025, 11, 3));
    assertEquals(1, events.size());
    assertTrue(buf.toString().contains("Event created"));
  }

  /**
   * Creates an event series for fixed occurrences.
   */
  @Test
  public void testExecuteCreateSeriesForTimes() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    controller.runCommand(parser.parse(
        "create event Series from 2025-11-04T09:00 to 2025-11-04T10:00 repeats TR for 2 times"));

    List<Event> all = model.getAllEvents();
    assertEquals(2, all.size());
    assertTrue(buf.toString().contains("Created 2 events in series"));
  }

  /**
   * Creates an event series until a date.
   */
  @Test
  public void testExecuteCreateSeriesUntil() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    String blockCmd = "create event Block from 2025-11-05T09:00 to 2025-11-05T10:00"
        + " repeats TR until 2025-11-20";
    controller.runCommand(parser.parse(blockCmd));

    List<Event> all = model.getAllEvents();
    assertTrue(all.size() >= 1);
    assertTrue(buf.toString().contains("Created"));
  }

  /**
   * Prints events on a date.
   */
  @Test
  public void testExecutePrintOn() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    model.createEvent(
        "A",
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 10, 0));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    controller.runCommand(parser.parse("print events on 2025-11-03"));

    String out = buf.toString();
    assertTrue(out.contains("A starting on 2025-11-03"));
  }

  /**
   * Prints events in a range.
   */
  @Test
  public void testExecutePrintRange() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    model.createEvent(
        "R",
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 10, 0));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    controller.runCommand(parser.parse(
        "print events from 2025-11-03T00:00 to 2025-11-03T23:59"));

    String out = buf.toString();
    assertTrue(out.contains("R starting on 2025-11-03"));
  }

  /**
   * Shows status for busy and available.
   */
  @Test
  public void testExecuteStatus() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    model.createEvent(
        "BusySlot",
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 10, 0));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    controller.runCommand(parser.parse("show status on 2025-11-03T09:30"));
    controller.runCommand(parser.parse("show status on 2025-11-03T11:30"));

    String out = buf.toString();
    assertTrue(out.contains("busy"));
    assertTrue(out.contains("available"));
  }

  /**
   * Exports calendar through controller.
   */
  @Test
  public void testExecuteExport() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    model.createEvent(
        "ToExport",
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 10, 0));
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    controller.runCommand(parser.parse("export cal build/controller-export.csv"));

    assertTrue(Files.exists(Paths.get("build/controller-export.csv")));
    String out = buf.toString();
    assertTrue(out.contains("Exported to"));
  }

  /**
   * Edits a single event through controller.
   */
  @Test
  public void testHandleEditSingle() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    model.createEvent(
        "One",
        LocalDateTime.of(2025, 11, 4, 9, 0),
        LocalDateTime.of(2025, 11, 4, 10, 0));
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    controller.runCommand(parser.parse(
        "edit event location One from 2025-11-04T09:00 with Zoom"));

    List<Event> list = model.getEventsOnDate(LocalDate.of(2025, 11, 4));
    assertEquals(1, list.size());
    assertTrue(list.get(0).getLocation().isPresent());
    assertEquals("Zoom", list.get(0).getLocation().get());
    assertTrue(buf.toString().contains("Event edited"));
  }

  /**
   * Edits events from date in a series through controller.
   */
  @Test
  public void testHandleEditEventsScope() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    controller.runCommand(parser.parse(
        "create event S from 2025-11-03T09:00 to 2025-11-03T10:00 repeats MW for 3 times"));

    controller.runCommand(parser.parse(
        "edit events location S from 2025-11-05T09:00 with Room201"));

    List<Event> all = model.getAllEvents();
    int withLocation = 0;
    for (Event e : all) {
      if ("S".equals(e.getSubject()) && e.getLocation().isPresent()) {
        withLocation++;
      }
    }
    assertTrue(withLocation >= 1);
    assertTrue(buf.toString().contains("Events edited"));
  }

  /**
   * Edits a whole series through controller.
   */
  @Test
  public void testHandleEditSeriesScope() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    controller.runCommand(parser.parse(
        "create event C from 2025-11-03T09:00 to 2025-11-03T10:00 repeats TR for 2 times"));

    controller.runCommand(parser.parse(
        "edit series description C from 2025-11-04T09:00 with weekly sync"));

    List<Event> all = model.getAllEvents();
    int withDesc = 0;
    for (Event e : all) {
      if ("C".equals(e.getSubject()) && e.getDescription().isPresent()) {
        withDesc++;
      }
    }
    assertTrue(withDesc >= 1);
    assertTrue(buf.toString().contains("Series edited"));
  }

  @Test
  public void testHandleEditUnknownScope() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));

    CommandParser.Command bogus = CommandParser.Command.builder("edit", CommandType.EDIT)
        .subject("X")
        .startDateTime(LocalDateTime.of(2025, 11, 3, 9, 0))
        .property("location")
        .newValue("Nowhere")
        .editScope("weird-scope")
        .build();

    controller.runCommand(bogus);
    String out = buf.toString();
    assertTrue(out.contains("Unknown edit scope") || out.contains("Error"));
  }

  /**
   * Forces the default branch of executeCommand.
   */
  @Test
  public void testExecuteUnknownCommandType() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller =
        new ExposedController(book, view, new StringReader(""));

    CommandParser.Command bogus = CommandParser.Command.builder("does_not_exist")
        .build();

    controller.runCommand(bogus);
    String out = buf.toString();
    assertTrue(out.contains("Error") || out.contains("NullPointer"));
  }

  /**
   * Test handleCopyBetween copies events in a date range.
   */
  @Test
  public void testHandleCopyBetween() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));


    source.createEvent("Event1",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));
    source.createEvent("Event2",
        LocalDateTime.of(2025, 11, 12, 14, 0),
        LocalDateTime.of(2025, 11, 12, 15, 0));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));


    CommandParser parser = new CommandParser();
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));
    controller.runCommand(parser.parse("use calendar --name source"));
    controller.runCommand(parser.parse(
        "copy events between 2025-11-10 and 2025-11-12 --target target to 2025-11-20"));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(2, targetEvents.size());
    assertTrue(buf.toString().contains("Copied 2 event(s) to target"));
  }

  /**
   * Test handleCopyOnDate copies all events from one day to another.
   */
  @Test
  public void testHandleCopyOnDate() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));

    source.createEvent("Morning",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));
    source.createEvent("Afternoon",
        LocalDateTime.of(2025, 11, 10, 14, 0),
        LocalDateTime.of(2025, 11, 10, 15, 0));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name source"));
    controller.runCommand(parser.parse(
        "copy events on 2025-11-10 --target target to 2025-11-15"));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(2, targetEvents.size());
    assertTrue(buf.toString().contains("Copied 2 event(s) to target"));
  }

  /**
   * Test handleCopySingle copies a single identified event.
   */
  @Test
  public void testHandleCopySingle() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));


    source.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));
    controller.runCommand(parser.parse("use calendar --name source"));
    controller.runCommand(parser.parse(
        "copy event Meeting on 2025-11-10T09:00 --target target to 2025-11-15T14:00"));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(1, targetEvents.size());
    assertEquals(LocalDateTime.of(2025, 11, 15, 14, 0),
        targetEvents.get(0).getStartDateTime());
    assertTrue(buf.toString().contains("Copied 1 event to target"));
  }

  /**
   * Test handleEditCalendar for renaming a calendar.
   */
  @Test
  public void testHandleEditCalendar_name() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    book.createCalendar("OldName", ZoneId.of("America/New_York"));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse(
        "edit calendar --name OldName --property name NewName"));

    assertTrue(book.hasCalendar("NewName"));
    assertFalse(book.hasCalendar("OldName"));
    assertTrue(buf.toString().contains("Calendar renamed to: NewName"));
  }

  /**
   * Test handleEditCalendar for changing timezone.
   */
  @Test
  public void testHandleEditCalendar_timezone() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    book.createCalendar("MyCalendar", ZoneId.of("America/New_York"));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse(
        "edit calendar --name MyCalendar --property timezone Europe/London"));

    Calendar cal = book.getCalendar("MyCalendar");
    assertEquals(ZoneId.of("Europe/London"), cal.getZoneId());
    assertTrue(buf.toString().contains("Timezone updated for: MyCalendar"));
  }

  /**
   * Test handleEditCalendar with unknown property.
   */
  @Test
  public void testHandleEditCalendar_unknownProperty() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    book.createCalendar("MyCalendar", ZoneId.of("America/New_York"));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));

    CommandParser.Command cmd = CommandParser
        .Command.builder("edit_calendar", CommandType.EDIT_CALENDAR)
        .property("unknown_property")
        .newValue("some_value")
        .calendarName("MyCalendar")
        .build();

    controller.runCommand(cmd);

    String out = buf.toString();
    assertTrue(out.contains("Error:"));
  }

  /**
   * Test handleCopySingle with multiple matching events throws exception.
   */
  @Test
  public void testHandleCopySingle_multipleMatches_throws() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    source.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 10, 0));
    source.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 11, 0));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name source"));
    controller.runCommand(parser.parse(
        "copy event Meeting on 2025-11-10T09:00 --target target to 2025-11-15T14:00"));

    String out = buf.toString();
    assertTrue(out.contains("Error") || out.contains("not unique") || out.contains("Ambiguous"));
  }

  /**
   * Test handleCopySingle when duplicate already exists returns 0 copied.
   */
  @Test
  public void testHandleCopySingle_duplicateExists_returns0() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));


    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    source.createEvent("Meeting", start, end);


    target.createEvent("Meeting", start, end);

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name source"));
    controller.runCommand(parser.parse(
        "copy event Meeting on 2025-11-10T09:00 --target target to 2025-11-10T09:00"));

    String out = buf.toString();
    assertTrue(out.contains("Copied 0 event(s) to target"));
  }

  @Test
  public void testExecuteCreateCalendar() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    CalendarView view = new TextCalendarView(new PrintStream(buf));

    AbstractCalendarControllerExecTest.ExposedController controller =
        new AbstractCalendarControllerExecTest.ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    CommandParser.Command cmd = parser.parse("create calendar --name TestCal --timezone UTC");
    controller.runCommand(cmd);

    assertTrue(buf.toString().contains("Created calendar: TestCal"));
  }

  /**
   * Test editing event START property to cover line 170.
   */
  @Test
  public void testHandleEdit_startProperty_parsesDateTime() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    model.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    controller.runCommand(parser.parse(
        "edit event start Meeting from 2025-11-10T09:00 with 2025-11-10T08:30"));

    List<Event> events = model.getAllEvents();
    assertEquals(1, events.size());
    assertEquals(LocalDateTime.of(2025, 11, 10, 8, 30),
        events.get(0).getStartDateTime());
    assertTrue(buf.toString().contains("Event edited"));
  }

  /**
   * Test editing event END property to cover line 170.
   */
  @Test
  public void testHandleEdit_endProperty_parsesDateTime() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar model = book.createCalendar("default", ZoneId.of("America/New_York"));
    model.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name default"));
    controller.runCommand(parser.parse(
        "edit event end Meeting from 2025-11-10T09:00 with 2025-11-10T11:30"));

    List<Event> events = model.getAllEvents();
    assertEquals(1, events.size());
    assertEquals(LocalDateTime.of(2025, 11, 10, 11, 30),
        events.get(0).getEndDateTime());
    assertTrue(buf.toString().contains("Event edited"));
  }
  /**
   * Test handleCopyOnDate skips duplicate events (covers line 293).
   */

  @Test
  public void testHandleCopyOnDate_skipsDuplicates() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    source.createEvent("Morning",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    target.createEvent("Morning",
        LocalDateTime.of(2025, 11, 15, 9, 0),
        LocalDateTime.of(2025, 11, 15, 10, 0));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name source"));
    controller.runCommand(parser.parse(
        "copy events on 2025-11-10 --target target to 2025-11-15"));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(1, targetEvents.size());
    assertTrue(buf.toString().contains("Copied 0 event(s) to target"));
  }

  /**
   * Test handleCopyBetween skips events with same subject on same day (covers line 293).
   */
  @Test
  public void testHandleCopyBetween_skipsSameSubjectSameDay() throws Exception {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarBook book = new CalendarBookImpl();
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    source.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    target.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 20, 14, 0),
        LocalDateTime.of(2025, 11, 20, 15, 0));

    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name source"));
    controller.runCommand(parser.parse(
        "copy events between 2025-11-10 and 2025-11-10 --target target to 2025-11-20"));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(1, targetEvents.size());
    assertTrue(buf.toString().contains("Copied 0 event(s) to target"));
  }

  /**
   * Test handleCopyBetween with multiple events, some skipped (covers line 293).
   */
  @Test
  public void testHandleCopyBetween_partialSkip() throws Exception {

    CalendarBook book = new CalendarBookImpl();
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    source.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));
    source.createEvent("Standup",
        LocalDateTime.of(2025, 11, 11, 9, 0),
        LocalDateTime.of(2025, 11, 11, 10, 0));

    target.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 20, 9, 0),
        LocalDateTime.of(2025, 11, 20, 10, 0));

    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    CalendarView view = new TextCalendarView(new PrintStream(buf));
    ExposedController controller = new ExposedController(book, view, new StringReader(""));

    CommandParser parser = new CommandParser();
    controller.runCommand(parser.parse("use calendar --name source"));
    controller.runCommand(parser.parse(
        "copy events between 2025-11-10 and 2025-11-11 --target target to 2025-11-20"));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(2, targetEvents.size());
    assertTrue(buf.toString().contains("Copied 1 event(s) to target"));
  }




}
