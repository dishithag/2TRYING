import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import calendar.CalendarBook;
import calendar.CalendarBookImpl;
import calendar.controller.CalendarContext;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for CalendarContext.
 */
public class CalendarContextTest {

  private CalendarBook book;
  private CalendarContext context;

  /**
   * Sets up a fresh CalendarBook and CalendarContext before each test.
   */
  @Before
  public void setUp() {
    book = new CalendarBookImpl();
    context = new CalendarContext(book);
  }

  /**
   * Test use with null name throws exception.
   */
  @Test(expected = NullPointerException.class)
  public void testUse_nullName_throws() {
    context.use(null);
  }

  /**
   * Test use with non-existent calendar throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testUse_nonExistentCalendar_throws() {
    context.use("NonExistent");
  }

  /**
   * Test use with valid calendar name succeeds.
   */
  @Test
  public void testUse_validName_succeeds() {
    book.createCalendar("MyCalendar", ZoneId.of("America/New_York"));

    context.use("MyCalendar");

    assertEquals("MyCalendar", context.currentName());
    assertNotNull(context.current());
  }

  /**
   * Test current throws when no calendar is selected.
   */
  @Test(expected = IllegalStateException.class)
  public void testCurrent_noCalendarSelected_throws() {
    context.current();
  }

  /**
   * Test currentName returns null when no calendar selected.
   */
  @Test
  public void testCurrentName_noCalendarSelected_returnsNull() {
    assertEquals(null, context.currentName());
  }

  /**
   * Test constructor with null book throws exception.
   */
  @Test(expected = NullPointerException.class)
  public void testConstructor_nullBook_throws() {
    new CalendarContext(null);
  }

  /**
   * Test that accessing current calendar when none is in use throws exception.
   *
   */
  @Test(expected = IllegalStateException.class)
  public void testCurrent_noCalendarInUse_throws() {
    context.current();
  }
}