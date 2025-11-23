import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarBook;
import calendar.CalendarBookImpl;
import calendar.Event;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for CalendarBookImpl.
 */
public class CalendarBookImplTest {

  private CalendarBook book;

  /**
   * Setting up a new calendarbookimpl.
   */
  @Before
  public void setUp() {
    book = new CalendarBookImpl();
  }

  /**
   * Test copyEventsBetween copies all events in range.
   */
  @Test
  public void testCopyEventsBetween_copiesAllInRange() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));


    source.createEvent("Event1",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));
    source.createEvent("Event2",
        LocalDateTime.of(2025, 11, 12, 14, 0),
        LocalDateTime.of(2025, 11, 12, 15, 0));
    source.createEvent("Event3",
        LocalDateTime.of(2025, 11, 15, 11, 0),
        LocalDateTime.of(2025, 11, 15, 12, 0));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));
    book.copyEventsBetween("source", "target",
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 12),
        LocalDate.of(2025, 11, 20));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(2, targetEvents.size());
    assertEquals(LocalDate.of(2025, 11, 20),
        targetEvents.get(0).getStartDateTime().toLocalDate());
    assertEquals(LocalDate.of(2025, 11, 22),
        targetEvents.get(1).getStartDateTime().toLocalDate());
  }

  /**
   * Test copyEventsBetween with timezone conversion.
   */
  @Test
  public void testCopyEventsBetween_convertsTimezones() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("Europe/London"));

    source.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 14, 0),
        LocalDateTime.of(2025, 11, 10, 15, 0));

    book.copyEventsBetween("source", "target",
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 20));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(1, targetEvents.size());
    assertEquals(19, targetEvents.get(0).getStartDateTime().getHour());
  }

  /**
   * Test copyEventsOnDate copies all events from one day.
   */
  @Test
  public void testCopyEventsOnDate_copiesAllFromDay() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));


    source.createEvent("Morning",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));
    source.createEvent("Afternoon",
        LocalDateTime.of(2025, 11, 10, 14, 0),
        LocalDateTime.of(2025, 11, 10, 15, 0));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));
    book.copyEventsOnDate("source", "target",
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(2, targetEvents.size());
    assertEquals(LocalDate.of(2025, 11, 15),
        targetEvents.get(0).getStartDateTime().toLocalDate());
  }

  /**
   * Test copyEvent copies single event with duration preserved.
   */
  @Test
  public void testCopyEvent_preservesDuration() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    source.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 30));

    book.copyEvent("source", "target",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        "Meeting",
        LocalDateTime.of(2025, 11, 15, 14, 0));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(1, targetEvents.size());
    assertEquals(LocalDateTime.of(2025, 11, 15, 14, 0),
        targetEvents.get(0).getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 15, 15, 30),
        targetEvents.get(0).getEndDateTime());
  }

  /**
   * Test createCalendar with null name throws.
   */
  @Test(expected = NullPointerException.class)
  public void testCreateCalendar_nullName_throws() {
    book.createCalendar(null, ZoneId.of("America/New_York"));
  }

  /**
   * Test createCalendar with null zone throws.
   */
  @Test(expected = NullPointerException.class)
  public void testCreateCalendar_nullZone_throws() {
    book.createCalendar("Test", null);
  }

  /**
   * Test createCalendar with duplicate name throws.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateCalendar_duplicateName_throws() {
    book.createCalendar("Calendar", ZoneId.of("America/New_York"));
    book.createCalendar("Calendar", ZoneId.of("Europe/London"));
  }

  /**
   * Test renameCalendar success.
   */
  @Test
  public void testRenameCalendar_success() {
    book.createCalendar("OldName", ZoneId.of("America/New_York"));
    book.renameCalendar("OldName", "NewName");

    assertTrue(book.hasCalendar("NewName"));
    assertFalse(book.hasCalendar("OldName"));
  }

  /**
   * Test renameCalendar with non-existent calendar throws.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRenameCalendar_notFound_throws() {
    book.renameCalendar("NonExistent", "NewName");
  }

  /**
   * Test renameCalendar to existing name throws and restores original.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRenameCalendar_duplicateNewName_throws() {
    book.createCalendar("Cal1", ZoneId.of("America/New_York"));
    book.createCalendar("Cal2", ZoneId.of("Europe/London"));

    book.renameCalendar("Cal1", "Cal2");
  }

  /**
   * Test changeTimezone updates zone.
   */
  @Test
  public void testChangeTimezone_updates() {
    book.createCalendar("Test", ZoneId.of("America/New_York"));
    book.changeTimezone("Test", ZoneId.of("Asia/Tokyo"));

    Calendar cal = book.getCalendar("Test");
    assertEquals(ZoneId.of("Asia/Tokyo"), cal.getZoneId());
  }

  /**
   * Test getCalendar with non-existent name throws.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testGetCalendar_notFound_throws() {
    book.getCalendar("NonExistent");
  }

  /**
   * Test listCalendarNames returns sorted names.
   */
  @Test
  public void testListCalendarNames_sorted() {
    book.createCalendar("Zebra", ZoneId.of("America/New_York"));
    book.createCalendar("Alpha", ZoneId.of("Europe/London"));
    book.createCalendar("Beta", ZoneId.of("Asia/Tokyo"));

    List<String> names = book.listCalendarNames();
    assertEquals(3, names.size());
    assertEquals("Alpha", names.get(0));
    assertEquals("Beta", names.get(1));
    assertEquals("Zebra", names.get(2));
  }

  /**
   * Test copyEvent with no matching event throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCopyEvent_noMatch_throws() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    book.copyEvent("source", "target",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        "NonExistent",
        LocalDateTime.of(2025, 11, 15, 14, 0));
  }

  /**
   * Test copyEvent with multiple matching events throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCopyEvent_multipleMatches_throws() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    source.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 10, 0));
    source.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 11, 0));

    book.copyEvent("source", "target", start, "Meeting",
        LocalDateTime.of(2025, 11, 15, 14, 0));
  }

  @Test

  public void testCopyEventsBetween_acrossDstBoundary() {

    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));

    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    source.createEvent("Spring Forward Event",

        LocalDateTime.of(2025, 3, 9, 1, 30),

        LocalDateTime.of(2025, 3, 9, 2, 30));

    book.copyEventsBetween("source", "target",

        LocalDate.of(2025, 3, 9),

        LocalDate.of(2025, 3, 9),

        LocalDate.of(2025, 3, 16));

    List<Event> targetEvents = target.getAllEvents();

    assertEquals(1, targetEvents.size());

    assertEquals(LocalDateTime.of(2025, 3, 16, 1, 30),

        targetEvents.get(0).getStartDateTime());

  }


  @Test
  public void testCopyEventsBetween_partialSeriesOverlap_retainsSeriesId() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    List<Event> series = source.createEventSeries("MWF Class",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        5);

    String originalSeriesId = series.get(0).getSeriesId().get();

    book.copyEventsBetween("source", "target",
        LocalDate.of(2025, 11, 12),
        LocalDate.of(2025, 11, 18),
        LocalDate.of(2025, 12, 1));

    List<Event> targetEvents = target.getAllEvents();
    assertTrue(targetEvents.size() >= 2);

    for (Event e : targetEvents) {
      assertTrue(e.isSeriesPart());
      assertEquals(originalSeriesId, e.getSeriesId().get());
    }
  }

  /**
   * Test for copy preserving event properties.
   */
  @Test
  public void testCopyEvent_preservesAllProperties() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    Event original = new calendar.SingleEvent(
        "Team Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 30),
        "Weekly sync",
        "Conference Room A",
        false,
        null
    );

    source.createEvent("Team Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 30));

    book.copyEvent("source", "target",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        "Team Meeting",
        LocalDateTime.of(2025, 11, 15, 14, 0));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(1, targetEvents.size());
    Event copied = targetEvents.get(0);

    assertEquals("Team Meeting", copied.getSubject());
    assertEquals(LocalDateTime.of(2025, 11, 15, 14, 0), copied.getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 15, 15, 30), copied.getEndDateTime());
  }

  /**
   * Test for invalid time zone.
   */
  @Test(expected = Exception.class)
  public void testCreateCalendar_invalidTimezone_throws() {
    book.createCalendar("test", ZoneId.of("Invalid/Timezone"));
  }

  /**

   * Test copying event to the same calendar creates a duplicate at different time.

   * Requirement: source and target calendars may or may not be the same.

   */

  @Test

  public void testCopyEvent_sameCalendar_succeeds() {

    Calendar cal = book.createCalendar("calendar", ZoneId.of("America/New_York"));

    cal.createEvent("Meeting",

        LocalDateTime.of(2025, 11, 10, 9, 0),

        LocalDateTime.of(2025, 11, 10, 10, 0));

    book.copyEvent("calendar", "calendar",

        LocalDateTime.of(2025, 11, 10, 9, 0),

        "Meeting",

        LocalDateTime.of(2025, 11, 15, 14, 0));

    List<Event> events = cal.getAllEvents();

    assertEquals(2, events.size());

  }

  /**
   * Test copying events on empty source day copies nothing.
   */
  @Test
  public void testCopyEventsOnDate_emptySourceDay_copiesNothing() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    book.copyEventsOnDate("source", "target",
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15));

    assertEquals(0, target.getAllEvents().size());
  }

  /**
   * Test copying multi-day event that partially overlaps with range.
   */
  @Test
  public void testCopyEventsBetween_multiDayEvent_partialOverlap_copies() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    source.createEvent("Conference",
        LocalDateTime.of(2025, 11, 9, 9, 0),
        LocalDateTime.of(2025, 11, 11, 17, 0));

    book.copyEventsBetween("source", "target",
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 20));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(1, targetEvents.size());
    assertEquals("Conference", targetEvents.get(0).getSubject());
  }

  /**
   * Test changing calendar timezone converts existing event times.
   */
  @Test
  public void testChangeTimezone_convertsExistingEvents() {
    Calendar cal = book.createCalendar("test", ZoneId.of("America/New_York"));

    cal.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 14, 0),
        LocalDateTime.of(2025, 11, 10, 15, 0));

    book.changeTimezone("test", ZoneId.of("Europe/London"));

    List<Event> events = cal.getAllEvents();
    assertEquals(1, events.size());
    assertEquals(19, events.get(0).getStartDateTime().getHour());
    assertEquals(ZoneId.of("Europe/London"), cal.getZoneId());
  }

  /**
   * Test copying events between range with timezone conversion to Asia.
   */
  @Test
  public void testCopyEventsBetween_acrossTimezones_convertsCorrectly() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("Asia/Tokyo"));

    source.createEvent("Event1",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    book.copyEventsBetween("source", "target",
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 20));

    List<Event> targetEvents = target.getAllEvents();
    assertEquals(1, targetEvents.size());
    assertEquals(23, targetEvents.get(0).getStartDateTime().getHour());
  }

  /**
   * Test copying events between dates with empty range copies nothing.
   */
  @Test
  public void testCopyEventsBetween_emptyRange_copiesNothing() {
    Calendar source = book.createCalendar("source", ZoneId.of("America/New_York"));
    Calendar target = book.createCalendar("target", ZoneId.of("America/New_York"));

    source.createEvent("Before Range",
        LocalDateTime.of(2025, 11, 5, 9, 0),
        LocalDateTime.of(2025, 11, 5, 10, 0));

    book.copyEventsBetween("source", "target",
        LocalDate.of(2025, 11, 10),
        LocalDate.of(2025, 11, 15),
        LocalDate.of(2025, 11, 20));

    assertEquals(0, target.getAllEvents().size());
  }
}