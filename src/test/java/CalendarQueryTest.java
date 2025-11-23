import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarImpl;
import calendar.Event;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for querying calendar events.
 */
public class CalendarQueryTest {

  private Calendar calendar;

  /**
   * Sets up a calendar with baseline events.
   */
  @Before
  public void setUp() {
    calendar = new CalendarImpl();
    calendar.createEvent("Morning",
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 10, 0));
    calendar.createEvent("Afternoon",
        LocalDateTime.of(2025, 11, 3, 14, 0),
        LocalDateTime.of(2025, 11, 3, 15, 0));
    calendar.createEvent("NextDay",
        LocalDateTime.of(2025, 11, 4, 11, 0),
        LocalDateTime.of(2025, 11, 4, 12, 0));
  }

  /**
   * Gets events on a date.
   */
  @Test
  public void testGetEventsOnDate() {
    List<Event> onDate = calendar.getEventsOnDate(LocalDate.of(2025, 11, 3));
    assertEquals(2, onDate.size());
  }

  /**
   * Gets events in a range.
   */
  @Test
  public void testGetEventsInRange() {
    List<Event> inRange = calendar.getEventsInRange(
        LocalDateTime.of(2025, 11, 3, 8, 0),
        LocalDateTime.of(2025, 11, 3, 16, 0));
    assertEquals(2, inRange.size());
  }

  /**
   * Checks busy logic at boundaries.
   */
  @Test
  public void testIsBusyAtBoundaries() {
    assertTrue(calendar.isBusyAt(LocalDateTime.of(2025, 11, 3, 9, 0)));
    assertFalse(calendar.isBusyAt(LocalDateTime.of(2025, 11, 3, 10, 0)));
    assertTrue(calendar.isBusyAt(LocalDateTime.of(2025, 11, 3, 14, 30)));
  }

  /**
   * At the exact end time the calendar should not be busy.
   */
  @Test
  public void testBusyBoundaryAtEnd() {
    calendar.createEvent(
        "Slot",
        LocalDateTime.of(2025, 11, 3, 10, 0),
        LocalDateTime.of(2025, 11, 3, 11, 0)
    );

    assertTrue(calendar.isBusyAt(LocalDateTime.of(2025, 11, 3, 10, 30)));
    assertFalse(calendar.isBusyAt(LocalDateTime.of(2025, 11, 3, 11, 0)));
  }

  /**
   * Test findEvents with non-null end that matches.
   */
  @Test
  public void testFindEvents_nonNullEndMatches() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);

    calendar.createEvent("Meeting", start, end);

    List<Event> found = calendar.findEvents("Meeting", start, end);

    assertEquals(1, found.size());
    assertEquals(end, found.get(0).getEndDateTime());
  }


  /**
   * Test findEvents filters out events with non-matching end time.
   */
  @Test
  public void testFindEvents_endDoesNotMatch_filtered() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    calendar.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 10, 0));
    calendar.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 11, 0));

    List<Event> found = calendar.findEvents("Meeting", start,
        LocalDateTime.of(2025, 11, 10, 10, 0));

    assertEquals(1, found.size());
    assertEquals(LocalDateTime.of(2025, 11, 10, 10, 0),
        found.get(0).getEndDateTime());
  }

  /**
   * Test findEvents with non-null end returns empty when no match.
   */
  @Test
  public void testFindEvents_nonNullEnd_noMatches() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);

    calendar.createEvent("Meeting", start, end);

    List<Event> found = calendar.findEvents("Meeting", start,
        LocalDateTime.of(2025, 11, 10, 12, 0));

    assertEquals(0, found.size());
  }

  /**
   * Test findEvents with two parameters (subject and start only).
   */
  @Test
  public void testFindEvents_twoParameters() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    calendar.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 10, 0));
    calendar.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 11, 0));

    List<Event> found = calendar.findEvents("Meeting", start);

    assertEquals(2, found.size());
    assertEquals("Meeting", found.get(0).getSubject());
    assertEquals("Meeting", found.get(1).getSubject());
  }

  /**
   * Test getEventsInRange boundary - event ending at range start is excluded.
   */
  @Test
  public void testGetEventsInRange_eventEndEqualsRangeStart_excluded() {
    calendar.createEvent("Boundary",
        LocalDateTime.of(2025, 11, 3, 8, 0),
        LocalDateTime.of(2025, 11, 3, 9, 0));

    List<Event> inRange = calendar.getEventsInRange(
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 10, 0));

    assertEquals(2, inRange.size());
  }
}
