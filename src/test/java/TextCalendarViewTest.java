import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarImpl;
import calendar.Event;
import calendar.view.TextCalendarView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for TextCalendarView.
 */
public class TextCalendarViewTest {

  private ByteArrayOutputStream buffer;
  private PrintStream out;
  private TextCalendarView view;

  /**
   * Sets up a fresh view and captured output.
   */
  @Before
  public void setUp() {
    buffer = new ByteArrayOutputStream();
    out = new PrintStream(buffer);
    view = new TextCalendarView(out);
  }

  /**
   * Null list should print the no-events message.
   */
  @Test
  public void testDisplayEvents_nullList() {
    view.displayEvents(null);
    String printed = buffer.toString().trim();
    assertEquals("No events found.", printed);
  }

  /**
   * Empty list should print the no-events message.
   */
  @Test
  public void testDisplayEvents_emptyList() {
    view.displayEvents(new ArrayList<Event>());
    String printed = buffer.toString().trim();
    assertEquals("No events found.", printed);
  }

  /**
   * A single event should produce some non-empty output and not the no-events message.
   */
  @Test
  public void testDisplayEvents_singleEvent() {
    Calendar calendar = new CalendarImpl();
    Event event = calendar.createEvent(
        "Daily standup",
        LocalDateTime.of(2025, 7, 4, 9, 30),
        LocalDateTime.of(2025, 7, 4, 10, 0)
    );

    view.displayEvents(Collections.singletonList(event));
    String printed = buffer.toString().trim();

    assertFalse(printed.isEmpty());
    assertFalse("No events found.".equals(printed));
  }

  /**
   * Multiple events should print multiple lines.
   */
  @Test
  public void testDisplayEvents_multipleEvents() {
    Calendar calendar = new CalendarImpl();
    Event first = calendar.createEvent(
        "A",
        LocalDateTime.of(2025, 7, 4, 9, 0),
        LocalDateTime.of(2025, 7, 4, 10, 0)
    );
    Event second = calendar.createEvent(
        "B",
        LocalDateTime.of(2025, 7, 4, 11, 0),
        LocalDateTime.of(2025, 7, 4, 12, 0)
    );

    List<Event> list = new ArrayList<>();
    list.add(first);
    list.add(second);

    view.displayEvents(list);
    String printed = buffer.toString().trim();

    assertTrue(printed.contains(first.getSubject()));
    assertTrue(printed.contains(second.getSubject()));
  }

  /**
   * Error messages are prefixed.
   */
  @Test
  public void testDisplayError() {
    view.displayError("bad input");
    String printed = buffer.toString().trim();
    assertEquals("Error: bad input", printed);
  }

  /**
   * Busy status prints busy.
   */
  @Test
  public void testDisplayStatus_busy() {
    view.displayStatus(true);
    String printed = buffer.toString().trim();
    assertEquals("busy", printed);
  }

  /**
   * Available status prints available.
   */
  @Test
  public void testDisplayStatus_available() {
    view.displayStatus(false);
    String printed = buffer.toString().trim();
    assertEquals("available", printed);
  }

  /**
   * Prompt prints the expected symbol.
   */
  @Test
  public void testDisplayPrompt() {
    view.displayPrompt();
    String printed = buffer.toString();
    assertEquals("> ", printed);
  }

  /**
   * Message printing is passed straight through.
   */
  @Test
  public void testDisplayMessage() {
    view.displayMessage("hello");
    String printed = buffer.toString().trim();
    assertEquals("hello", printed);
  }

  /**
   * Constructor should reject null print stream.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_nullStream_throws() {
    new TextCalendarView(null);
  }

  /**
   * Test that formatEvent includes location when present.
   */
  @Test
  public void testDisplayEvents_withLocation() {
    Calendar calendar = new CalendarImpl();


    Event event = new calendar.SingleEvent(
        "Team Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0),
        "",
        "Conference Room A",
        true,
        null
    );

    view.displayEvents(Collections.singletonList(event));
    String printed = buffer.toString();

    assertTrue(printed.contains("Team Meeting"));
    assertTrue(printed.contains("Conference Room A"));
    assertTrue(printed.contains(" at Conference Room A"));
  }
}
