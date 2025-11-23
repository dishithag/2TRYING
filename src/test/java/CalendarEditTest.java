import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarImpl;
import calendar.Event;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests for editing calendar events and series.
 */
public class CalendarEditTest {

  private Calendar calendar;

  /**
   * Sets up a fresh calendar instance before each test so that
   * tests do not share state.
   */
  @Before
  public void setUp() {
    calendar = new CalendarImpl();
  }

  /**
   * Verifies that a single event can be edited.
   */
  @Test
  public void testEditSingleEvent() {
    calendar.createEvent("Standup",
        LocalDateTime.parse("2025-11-03T09:00"),
        LocalDateTime.parse("2025-11-03T09:30"));

    calendar.editEvent("Standup",
        LocalDateTime.parse("2025-11-03T09:00"),
        "location",
        "Zoom");

    List<calendar.Event> events =
        calendar.getEventsOnDate(LocalDateTime.parse("2025-11-03T00:00").toLocalDate());

    assertEquals(1, events.size());
    assertTrue(events.get(0).getLocation().isPresent());
    assertEquals("Zoom", events.get(0).getLocation().get());
  }

  /**
   * Test editing a non-existent event throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditEventNotFound() {
    Calendar calendar = new CalendarImpl();

    calendar.editEvent("Nonexistent Meeting",
        LocalDateTime.parse("2025-11-03T10:00"),
        "description",
        "New description");
  }

  /**
   * Test editing event subject.
   * Tests case SUBJECT branch.
   */
  @Test
  public void testEditEventSubject() {
    Calendar calendar = new CalendarImpl();

    calendar.createEvent("Old Subject",
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));

    calendar.editEvent("Old Subject",
        LocalDateTime.parse("2025-11-03T10:00"),
        "subject",
        "New Subject");

    List<Event> events = calendar.getAllEvents();
    assertEquals(1, events.size());
    assertEquals("New Subject", events.get(0).getSubject());
  }

  /**
   * Test editing event with "start" property in buildModifiedEvent.
   */

  @Test

  public void testEditEventStartPropertyCoverage() {

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);

    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);

    calendar.createEvent("Meeting", start, end);

    calendar.editEvent("Meeting", start, "start", "2025-11-10T14:30");

    List<Event> events = calendar.getAllEvents();

    assertEquals(1, events.size());

    assertEquals(LocalDateTime.of(2025, 11, 10, 14, 30),

        events.get(0).getStartDateTime());

  }

  /**
   * Test editing event with "end" property in buildModifiedEvent.
   */

  @Test

  public void testEditEventEndPropertyCoverage() {

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);

    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);

    calendar.createEvent("Meeting", start, end);

    calendar.editEvent("Meeting", start, "end", "2025-11-10T17:30");

    List<Event> events = calendar.getAllEvents();

    assertEquals(1, events.size());

    assertEquals(LocalDateTime.of(2025, 11, 10, 17, 30),

        events.get(0).getEndDateTime());

  }

  /**
   * Test editing event with "status" property in buildModifiedEvent.
   * Covers the case STATUS branch and the equalsIgnoreCase check.
   */

  @Test

  public void testEditEventStatusPropertyCoverage() {

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);

    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);

    calendar.createEvent("Meeting", start, end);

    calendar.editEvent("Meeting", start, "status", "private");

    List<Event> events = calendar.getAllEvents();

    assertEquals(1, events.size());

    assertFalse(events.get(0).isPublic());

  }

  /**
   * Test editing event with "status" using different case variations.
   */

  @Test

  public void testEditEventStatusPublicCaseInsensitive() {

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);

    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);

    calendar.createEvent("Meeting", start, end);

    calendar.editEvent("Meeting", start, "status", "private");

    assertFalse(calendar.getAllEvents().get(0).isPublic());

    calendar.editEvent("Meeting", start, "status", "PUBLIC");

    assertTrue(calendar.getAllEvents().get(0).isPublic());

  }

  /**
   * Test the default case in buildModifiedEvent switch.
   */

  @Test(expected = IllegalArgumentException.class)

  public void testBuildModifiedEventUnsupportedProperty() {

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);

    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);

    calendar.createEvent("Meeting", start, end);

    calendar.editEvent("Meeting", start, "invalidprop", "value");

  }


  /**
   * Test editing events from date when no event is found throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditEventsFromDate_notFound() {
    calendar.editEventsFromDate("Nonexistent",
        LocalDateTime.parse("2025-11-03T10:00"),
        "description",
        "New description");
  }

  /**
   * Test editing an event to create a duplicate throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditEvent_createsDuplicate() {

    calendar.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    calendar.createEvent("Standup",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    calendar.editEvent("Standup",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        "subject",
        "Meeting");
  }

  /**
   * Test editing events from date would create duplicate throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditEventsFromDate_createsDuplicate() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);

    calendar.createEventSeries("Daily Standup", start, end,
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY), 3);

    calendar.createEvent("Daily Standup",
        LocalDateTime.of(2025, 11, 12, 8, 0),
        LocalDateTime.of(2025, 11, 12, 9, 0));

    calendar.editEventsFromDate("Daily Standup",
        LocalDateTime.of(2025, 11, 11, 9, 0),
        "start",
        "2025-11-11T08:00");
  }

  /**
   * Test editing series when no event is found throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditSeries_notFound() {
    calendar.editSeries("Nonexistent Series",
        LocalDateTime.parse("2025-11-03T10:00"),
        "location",
        "New Location");
  }

  /**
   * Test getEventsBySeriesId returns empty list when series not found.
   * This tests the private method indirectly through editSeries on a non-series event.
   */
  @Test
  public void testEditSeries_nonSeriesEvent_handlesGracefully() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);

    calendar.createEvent("Single Event", start, end);

    calendar.editSeries("Single Event", start, "description", "Updated");

    List<Event> events = calendar.getAllEvents();
    assertEquals(1, events.size());
    assertEquals("Updated", events.get(0).getDescription().orElse(""));
  }

}
