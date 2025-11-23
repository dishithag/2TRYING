import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarImpl;
import calendar.Event;
import calendar.EventBuilder;
import calendar.SingleEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for creating, editing, and querying calendar events.
 * Designed to match the current CalendarImpl behaviour where a null end stays null.
 */
public class CalendarCreateTest {

  private Calendar calendar;

  /**
   * Creates a fresh calendar before every test.
   */
  @Before
  public void setUp() {
    calendar = new CalendarImpl();
  }

  /**
   * Creating with a null end should auto-fill 17:00 on the same day.
   */
  @Test
  public void testCreateEvent_nullEnd_setsEndTo1700() {
    Event event = calendar.createEvent(
        "Independence Day block",
        LocalDateTime.of(2025, 7, 4, 0, 0),
        null
    );

    assertEquals(LocalDateTime.of(2025, 7, 4, 0, 0), event.getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 7, 4, 17, 0), event.getEndDateTime());
  }

  /**
   * Midday start with null end should still end at 17:00.
   */
  @Test
  public void testCreateEvent_middayStart_nullEnd_setsEndTo1700() {
    Event event = calendar.createEvent(
        "Standup spillover",
        LocalDateTime.of(2025, 7, 4, 11, 45),
        null
    );

    assertEquals(LocalDateTime.of(2025, 7, 4, 11, 45), event.getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 7, 4, 17, 0), event.getEndDateTime());
  }

  /**
   * Creating an event with explicit start and end uses them as is.
   */
  @Test
  public void testCreateEvent_withExplicitEnd_usesGivenTimes() {
    LocalDateTime start = LocalDateTime.of(2025, 7, 4, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 7, 4, 10, 30);

    Event event = calendar.createEvent("1:1", start, end);

    assertNotNull(event);
    assertEquals(start, event.getStartDateTime());
    assertEquals(end, event.getEndDateTime());
    assertFalse(event.isAllDayEvent());
  }

  /**
   * Creating a timed event should not make it all-day.
   */
  @Test
  public void testCreateTimedEventIsNotAllDay() {
    Event event = calendar.createEvent(
        "Timed",
        LocalDateTime.of(2025, 7, 4, 10, 0),
        LocalDateTime.of(2025, 7, 4, 11, 0)
    );
    assertFalse(event.isAllDayEvent());
  }

  /**
   * Multi-day event appears on all days it spans.
   */
  @Test
  public void testMultiDayEventAppearsOnAllDays() {
    calendar.createEvent(
        "Conference",
        LocalDateTime.of(2025, 7, 4, 9, 0),
        LocalDateTime.of(2025, 7, 6, 17, 0)
    );

    assertEquals(1, calendar.getEventsOnDate(LocalDate.of(2025, 7, 4)).size());
    assertEquals(1, calendar.getEventsOnDate(LocalDate.of(2025, 7, 5)).size());
    assertEquals(1, calendar.getEventsOnDate(LocalDate.of(2025, 7, 6)).size());
  }

  /**
   * Creating another event with the exact same subject/start/end is rejected.
   */
  @Test
  public void testCreateEvent_duplicateSameSubjectStartEnd_throws() {
    LocalDateTime start = LocalDateTime.of(2025, 12, 1, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 12, 1, 10, 0);

    calendar.createEvent("Standup", start, end);

    assertThrows(IllegalArgumentException.class,
        () -> calendar.createEvent("Standup", start, end));
  }

  /**
   * Creating a weekday series for a fixed number of occurrences.
   */
  @Test
  public void testCreateEventSeries_occurrencesAndDays() {
    LocalDateTime start = LocalDateTime.of(2025, 7, 7, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 7, 7, 10, 0);

    List<Event> created = calendar.createEventSeries(
        "MWF Class", start, end, EnumSet.of(DayOfWeek.MONDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), 5);

    assertEquals(5, created.size());
    assertEquals(LocalDate.of(2025, 7, 7),
        created.get(0).getStartDateTime().toLocalDate());
    assertEquals(LocalDate.of(2025, 7, 9),
        created.get(1).getStartDateTime().toLocalDate());
    assertEquals(LocalDate.of(2025, 7, 11),
        created.get(2).getStartDateTime().toLocalDate());
  }

  /**
   * Creating a series until a specific date stops at that date.
   */
  @Test
  public void testCreateEventSeriesUntil_stopsAtEndDate() {
    LocalDateTime start = LocalDateTime.of(2025, 7, 8, 14, 0);
    LocalDateTime end = LocalDateTime.of(2025, 7, 8, 15, 0);
    LocalDate until = LocalDate.of(2025, 7, 29);

    List<Event> created = calendar.createEventSeriesUntil(
        "Review", start, end, EnumSet.of(DayOfWeek.TUESDAY), until);

    assertEquals(4, created.size());
    assertEquals(LocalDate.of(2025, 7, 8),
        created.get(0).getStartDateTime().toLocalDate());
    assertEquals(LocalDate.of(2025, 7, 29),
        created.get(3).getStartDateTime().toLocalDate());
  }

  /**
   * Series instances must be single-day; cross-day end throws.
   */
  @Test
  public void testCreateEventSeries_singleDayConstraint_violationThrows() {
    LocalDateTime start = LocalDateTime.of(2025, 7, 4, 23, 30);
    LocalDateTime end = LocalDateTime.of(2025, 7, 5, 0, 30);

    assertThrows(IllegalArgumentException.class,
        () -> calendar.createEventSeries("Overnight", start, end, EnumSet.of(DayOfWeek.FRIDAY), 3));
  }

  /**
   * Editing description of a single non-series event keeps other fields.
   */
  @Test
  public void testEditSingleEventDescription() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);

    calendar.createEvent("OneOff", start, end);

    calendar.editEvent("OneOff", start, "description", "bring slides");

    List<Event> all = calendar.getAllEvents();
    assertEquals(1, all.size());
    assertEquals("bring slides", all.get(0).getDescription().orElse(""));
    assertEquals(start, all.get(0).getStartDateTime());
    assertEquals(end, all.get(0).getEndDateTime());
  }

  /**
   * Editing location of a single non-series event keeps other fields.
   */
  @Test
  public void testEditSingleEventLocation() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);

    calendar.createEvent("Briefing", start, end);

    calendar.editEvent("Briefing", start, "location", "Zoom");

    List<Event> events = calendar.getAllEvents();
    assertEquals(1, events.size());
    assertTrue(events.get(0).getLocation().isPresent());
    assertEquals("Zoom", events.get(0).getLocation().get());
  }

  /**
   * Editing from a given date in a series changes only the tail and creates a new series id.
   */
  @Test
  public void testEditEventsFromDate_changeStartTime_onTailOnly() {
    LocalDateTime start = LocalDateTime.of(2025, 7, 8, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 7, 8, 11, 0);

    List<Event> series = calendar.createEventSeries("TuTh", start, end,
        EnumSet.of(DayOfWeek.TUESDAY,
        DayOfWeek.THURSDAY), 4);
    assertEquals(4, series.size());

    calendar.editEventsFromDate(
        "TuTh",
        LocalDateTime.of(2025, 7, 15, 10, 0),
        "start",
        LocalDateTime.of(2025, 1, 1, 9, 0).toString()
    );

    Event first = findOne("TuTh", LocalDate.of(2025, 7, 8), 10, 0);
    assertNotNull(first);
    Event second = findOne("TuTh", LocalDate.of(2025, 7, 10), 10, 0);
    assertNotNull(second);
    Event third = findOne("TuTh", LocalDate.of(2025, 7, 15), 9, 0);
    assertNotNull(third);
    Event fourth = findOne("TuTh", LocalDate.of(2025, 7, 17), 9, 0);
    assertNotNull(fourth);

    Optional<String> headId = first.getSeriesId();
    Optional<String> tailId = third.getSeriesId();
    assertFalse(headId.equals(tailId));
  }

  /**
   * Editing the whole series updates all instances.
   */
  @Test
  public void testEditSeries_updateLocation_allInSeries() {
    LocalDateTime start = LocalDateTime.of(2025, 7, 7, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 7, 7, 10, 0);

    List<Event> series = calendar.createEventSeries("Course",
        start, end, EnumSet.of(DayOfWeek.MONDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), 3);
    assertEquals(3, series.size());

    calendar.editSeries("Course", start, "location", "Room 101");

    List<Event> all = calendar.getAllEvents();
    int withLocation = 0;
    for (Event e : all) {
      if ("Course".equals(e.getSubject())
          && e.getLocation().isPresent()
          && "Room 101".equals(e.getLocation().get())) {
        withLocation++;
      }
    }
    assertEquals(3, withLocation);
  }

  /**
   * Getting events on specific dates returns the correct counts.
   */
  @Test
  public void testGetEventsOnDate_countsPerDay() {
    calendar.createEvent(
        "A",
        LocalDateTime.of(2025, 7, 4, 8, 0),
        LocalDateTime.of(2025, 7, 4, 9, 0));
    calendar.createEvent(
        "B",
        LocalDateTime.of(2025, 7, 5, 10, 0),
        LocalDateTime.of(2025, 7, 5, 11, 0));
    calendar.createEvent(
        "C",
        LocalDateTime.of(2025, 7, 6, 15, 0),
        LocalDateTime.of(2025, 7, 6, 16, 0));

    assertEquals(1, calendar.getEventsOnDate(LocalDate.of(2025, 7, 4)).size());
    assertEquals(1, calendar.getEventsOnDate(LocalDate.of(2025, 7, 5)).size());
    assertEquals(1, calendar.getEventsOnDate(LocalDate.of(2025, 7, 6)).size());
  }

  /**
   * Getting events in a range returns only events in that range and ordered by time.
   */
  @Test
  public void testGetEventsInRange_inclusive() {
    calendar.createEvent(
        "E1",
        LocalDateTime.of(2025, 7, 1, 9, 0),
        LocalDateTime.of(2025, 7, 1, 10, 0));
    calendar.createEvent(
        "E2",
        LocalDateTime.of(2025, 7, 2, 9, 0),
        LocalDateTime.of(2025, 7, 2, 10, 0));
    calendar.createEvent(
        "E3",
        LocalDateTime.of(2025, 7, 3, 9, 0),
        LocalDateTime.of(2025, 7, 3, 10, 0));

    List<Event> inRange = calendar.getEventsInRange(
        LocalDateTime.of(2025, 7, 1, 0, 0),
        LocalDateTime.of(2025, 7, 2, 23, 59));

    assertEquals(2, inRange.size());
    assertEquals("E1", inRange.get(0).getSubject());
    assertEquals("E2", inRange.get(1).getSubject());
  }

  /**
   * Busy check returns true only inside an event.
   */
  @Test
  public void testIsBusyAt_trueAndFalse() {
    calendar.createEvent(
        "Window",
        LocalDateTime.of(2025, 8, 20, 13, 0),
        LocalDateTime.of(2025, 8, 20, 14, 0));

    assertTrue(calendar.isBusyAt(LocalDateTime.of(2025, 8, 20, 13, 30)));
    assertFalse(calendar.isBusyAt(LocalDateTime.of(2025, 8, 20, 12, 59)));
    assertFalse(calendar.isBusyAt(LocalDateTime.of(2025, 8, 20, 14, 0)));
  }

  /**
   * Helper to find exactly one event by subject, date, and time.
   *
   * @param subject subject to match
   * @param date    date to match
   * @param hour    hour to match
   * @param minute  minute to match
   * @return matching event or null
   */
  private Event findOne(String subject, LocalDate date, int hour, int minute) {
    for (Event e : calendar.getAllEvents()) {
      if (subject.equals(e.getSubject())
          && e.getStartDateTime().toLocalDate().equals(date)
          && e.getStartDateTime().getHour() == hour
          && e.getStartDateTime().getMinute() == minute) {
        return e;
      }
    }
    return null;
  }

  /**
   * Test SingleEvent with null subject throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSingleEventNullSubject() {
    new SingleEvent(null,
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));
  }

  /**
   * Test SingleEvent with empty subject throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSingleEventEmptySubject() {
    new SingleEvent("",
        LocalDateTime.parse("2025-11-03T10:00"),
        LocalDateTime.parse("2025-11-03T11:00"));
  }

  /**
   * Test SingleEvent with null start date throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSingleEventNullStartDate() {
    new SingleEvent("Meeting",
        null,
        LocalDateTime.parse("2025-11-03T11:00"));
  }


  /**
   * Test that null end uses start date to determine all-day times.
   */
  @Test
  public void testSingleEventNullEndUsesStartDate() {
    Event event = new SingleEvent("New Year Holiday",
        LocalDateTime.parse("2026-01-01T23:59"),
        null);

    assertEquals(LocalDateTime.parse("2026-01-01T08:00"), event.getStartDateTime());
    assertEquals(LocalDateTime.parse("2026-01-01T17:00"), event.getEndDateTime());
    assertTrue(event.isAllDayEvent());
  }

  /**
   * Test for series with invalid Weekdays.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateSeriesInvalidWeekday() {
    calendar.createEventSeries("Invalid",
        LocalDateTime.of(2025, 5, 5, 10, 0),
        LocalDateTime.of(2025, 5, 5, 11, 0),
        EnumSet.noneOf(DayOfWeek.class), 3);
  }

  /**
   * Test editing event with "end" property.
   */
  @Test
  public void testEditEventEndProperty() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);
    calendar.createEvent("Meeting", start, end);
    calendar.editEvent("Meeting", start, "end", "2025-11-10T17:00");
    List<Event> events = calendar.getAllEvents();
    assertEquals(1, events.size());
    assertEquals(LocalDateTime.of(2025, 11, 10, 17, 0), events.get(0).getEndDateTime());
  }

  /**
   * Test editing event with "status" property.
   */
  @Test
  public void testEditEventStatusProperty() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);
    calendar.createEvent("Meeting", start, end);
    calendar.editEvent("Meeting", start, "status", "private");
    List<Event> events = calendar.getAllEvents();
    assertEquals(1, events.size());
    assertFalse(events.get(0).isPublic());
  }

  /**
   * Test editing event with "status" set to "public".
   */
  @Test
  public void testEditEventStatusToPublic() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);
    Event event = new EventBuilder()
        .subject("Private Meeting")
        .startDateTime(start)
        .endDateTime(end)
        .isPublic(false)
        .build();
    calendar.createEvent("Meeting", start, end);
    calendar.editEvent("Meeting", start, "status", "public");
    List<Event> events = calendar.getAllEvents();
    assertEquals(1, events.size());
    assertTrue(events.get(0).isPublic());
  }

  /**
   * Test parseProperty with unknown/default property.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParsePropertyUnknown() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);
    calendar.createEvent("Meeting", start, end);
    calendar.editEvent("Meeting", start, "invalid", "some value");
  }

  /**
   * Test parseProperty with null property.
   * Covers the if (property == null) branch.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParsePropertyNull() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);
    calendar.createEvent("Meeting", start, end);
    calendar.editEvent("Meeting", start, null, "some value");
  }

  /**
   * Test getEventsBySeriesId when seriesId is not in seriesMap.
   */
  @Test
  public void testGetEventsBySeriesIdNotFound() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);

    List<Event> series = calendar.createEventSeries(
        "Meeting", start, end, EnumSet.of(DayOfWeek.MONDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), 3);

    calendar.createEvent("Single",
        LocalDateTime.of(2025, 12, 1, 10, 0),
        LocalDateTime.of(2025, 12, 1, 11, 0));
    calendar.editSeries("Single",
        LocalDateTime.of(2025, 12, 1, 10, 0),
        "description", "updated");
    List<Event> events = calendar.findEvents("Single",
        LocalDateTime.of(2025, 12, 1, 10, 0), null);
    assertEquals("updated", events.get(0).getDescription().orElse(""));
  }

  /**
   * Test editing event with "subject" property.
   */
  @Test
  public void testEditEventSubjectProperty() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);
    calendar.createEvent("OldSubject", start, end);
    calendar.editEvent("OldSubject", start, "subject", "NewSubject");
    List<Event> events = calendar.getAllEvents();
    assertEquals(1, events.size());
    assertEquals("NewSubject", events.get(0).getSubject());
  }

  /**
   * Test editing event with "start" property.
   */
  @Test
  public void testEditEventStartProperty() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);
    calendar.createEvent("Meeting", start, end);
    calendar.editEvent("Meeting", start, "start", "2025-11-10T14:00");
    List<Event> events = calendar.getAllEvents();
    assertEquals(1, events.size());
    assertEquals(LocalDateTime.of(2025, 11, 10, 14, 0),
        events.get(0).getStartDateTime());
  }

  @Test(expected = IllegalArgumentException.class)
  public void eventBuilder_missingSubject_throws() {
    new EventBuilder()
        .startDateTime(LocalDateTime.of(2025, 11, 3, 9, 0))
        .endDateTime(LocalDateTime.of(2025, 11, 3, 10, 0))
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void eventBuilder_missingStart_throws() {
    new EventBuilder()
        .subject("No start")
        .build();
  }

  @Test
  public void eventBuilder_noEnd_defaultsToFivePm() {
    Event e = new EventBuilder()
        .subject("No end")
        .startDateTime(LocalDateTime.of(2025, 11, 3, 9, 0))
        .build();

    assertEquals(LocalDateTime.of(2025, 11, 3, 17, 0), e.getEndDateTime());
  }

  @Test(expected = IllegalArgumentException.class)
  public void eventBuilder_endBeforeStart_throws() {
    new EventBuilder()
        .subject("Bad")
        .startDateTime(LocalDateTime.of(2025, 11, 3, 12, 0))
        .endDateTime(LocalDateTime.of(2025, 11, 3, 10, 0))
        .build();
  }

  @Test
  public void eventBuilder_allDay_sets8To5() {
    Event e = new EventBuilder()
        .subject("All day")
        .allDay(LocalDate.of(2025, 11, 3))
        .build();

    assertEquals(LocalDateTime.of(2025, 11, 3, 8, 0), e.getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 3, 17, 0), e.getEndDateTime());
  }

  @Test
  public void eventBuilder_full_success_keepsFields() {
    Event e = new EventBuilder()
        .subject("Team")
        .startDateTime(LocalDateTime.of(2025, 11, 3, 9, 30))
        .endDateTime(LocalDateTime.of(2025, 11, 3, 11, 0))
        .description("weekly")
        .location("Zoom")
        .isPublic(true)
        .build();

    assertEquals("Team", e.getSubject());
    assertEquals(LocalDateTime.of(2025, 11, 3, 9, 30), e.getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 3, 11, 0), e.getEndDateTime());
    assertTrue(e.getDescription().isPresent());
    assertTrue(e.getLocation().isPresent());
  }

  /**
   * Test EventBuilder with end date before start date throws exception.
   * Covers the if (actualEnd.isBefore(start)) branch.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEventBuilderEndBeforeStart() {
    new EventBuilder()
        .subject("Meeting")
        .startDateTime(LocalDateTime.of(2025, 11, 10, 16, 0))
        .endDateTime(LocalDateTime.of(2025, 11, 10, 15, 0))
        .build();
  }

  /**
   * Test EventBuilder with null end date - should set default all-day times.
   */
  @Test
  public void testEventBuilderNullEndDateTime() {
    Event event = new EventBuilder()
        .subject("All Day Event")
        .startDateTime(LocalDateTime.of(2025, 11, 10, 12, 30))
        .endDateTime(null)
        .build();


    assertEquals(LocalDateTime.of(2025, 11, 10, 17, 0), event.getEndDateTime());

    assertEquals(LocalDateTime.of(2025, 11, 10, 12, 30), event.getStartDateTime());
  }

  /**
   * Test EventBuilder with valid start and end dates.
   * Covers the else branch (actualEnd != null and not before start).
   */
  @Test
  public void testEventBuilderValidStartAndEnd() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);

    Event event = new EventBuilder()
        .subject("Meeting")
        .startDateTime(start)
        .endDateTime(end)
        .build();

    assertEquals("Meeting", event.getSubject());
    assertEquals(start, event.getStartDateTime());
    assertEquals(end, event.getEndDateTime());
  }

  /**
   * Test EventBuilder with subject that contains only whitespace (isBlank but not isEmpty).
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEventBuilderWhitespaceSubject() {
    new EventBuilder()
        .subject("   \t\n   ")  // whitespace only
        .startDateTime(LocalDateTime.of(2025, 11, 10, 15, 0))
        .endDateTime(LocalDateTime.of(2025, 11, 10, 16, 0))
        .build();
  }

  /**
   * Test EventBuilder with all optional fields set.
   * Ensures the builder works correctly with description, location, isPublic, and seriesId.
   */
  @Test
  public void testEventBuilderWithAllFields() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 15, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 16, 0);

    Event event = new EventBuilder()
        .subject("Full Event")
        .startDateTime(start)
        .endDateTime(end)
        .description("Important meeting")
        .location("Conference Room A")
        .isPublic(false)
        .seriesId("SERIES_123")
        .build();

    assertEquals("Full Event", event.getSubject());
    assertEquals(start, event.getStartDateTime());
    assertEquals(end, event.getEndDateTime());
    assertEquals("Important meeting", event.getDescription().orElse(""));
    assertEquals("Conference Room A", event.getLocation().orElse(""));
    assertFalse(event.isPublic());
    assertTrue(event.isSeriesPart());
    assertEquals("SERIES_123", event.getSeriesId().get());
  }

  /**
   * Test creating event series until date with multi-day event throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventSeriesUntil_multiDay_throws() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 23, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 11, 1, 0);
    LocalDate until = LocalDate.of(2025, 11, 30);

    calendar.createEventSeriesUntil("Overnight", start, end, EnumSet.of(DayOfWeek.MONDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), until);
  }

  /**
   * Test creating event series on Saturday.
   */
  @Test
  public void testCreateEventSeries_saturday() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 8, 10, 0); // Saturday
    LocalDateTime end = LocalDateTime.of(2025, 11, 8, 11, 0);

    List<Event> series = calendar.createEventSeries("Weekend Work", start, end,
        EnumSet.of(DayOfWeek.SATURDAY),
        3);

    assertTrue(series.size() > 0);
    assertEquals(java.time.DayOfWeek.SATURDAY,
        series.get(0).getStartDateTime().getDayOfWeek());
  }

  /**
   * Test creating event series on Sunday.
   */
  @Test
  public void testCreateEventSeries_sunday() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 9, 10, 0); // Sunday
    LocalDateTime end = LocalDateTime.of(2025, 11, 9, 11, 0);

    List<Event> series = calendar.createEventSeries("Sunday Brunch",
        start, end, EnumSet.of(DayOfWeek.SUNDAY), 2);

    assertTrue(series.size() > 0);
    assertEquals(java.time.DayOfWeek.SUNDAY,
        series.get(0).getStartDateTime().getDayOfWeek());
  }

  /**
   * Test creating event series on weekend (Saturday and Sunday).
   */
  @Test
  public void testCreateEventSeries_weekend() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 8, 10, 0); // Saturday
    LocalDateTime end = LocalDateTime.of(2025, 11, 8, 11, 0);

    List<Event> series = calendar.createEventSeries("Weekend Class",
        start, end, EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), 4);

    assertEquals(4, series.size());
  }

  /**
   * Test hashCode method is consistent with equals.
   */
  @Test
  public void testEventHashCode() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);

    Event event1 = calendar.createEvent("Meeting", start, end);
    Event event2 = calendar.createEvent("Different", start, end);

    int hash1 = event1.hashCode();
    int hash2 = event2.hashCode();

    Event event3 = new SingleEvent("Meeting", start, end);
    assertEquals(event1.hashCode(), event3.hashCode());

    assertTrue(hash1 != hash2);
  }

  /**
   * Test hashCode consistency - calling multiple times returns same value.
   */
  @Test
  public void testEventHashCodeConsistency() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);

    Event event = calendar.createEvent("Meeting", start, end);

    int hash1 = event.hashCode();
    int hash2 = event.hashCode();

    assertEquals(hash1, hash2);
  }

  /**
   * Test toString method formats event correctly.
   */
  @Test
  public void testEventToString() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);

    Event event = calendar.createEvent("Meeting", start, end);

    String str = event.toString();

    assertNotNull(str);
    assertTrue(str.contains("Meeting"));
    assertTrue(str.contains("2025-11-10"));
    assertTrue(str.contains("09:00"));
    assertTrue(str.contains("10:00"));
  }

  /**
   * Test toString with location included.
   */
  @Test
  public void testEventToStringWithLocation() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);

    Event event = new SingleEvent("Meeting", start, end, "", "Room 101", true, null);

    String str = event.toString();

    assertTrue(str.contains("Meeting"));
    assertTrue(str.contains("Room 101"));
    assertTrue(str.contains(" at Room 101"));
  }

  /**
   * Test that a multi-day event with all-day times is not considered all-day.
   */
  @Test
  public void testIsAllDayEvent_multiDay_returnsFalse() {
    // Create event with all-day times (8:00-17:00) but spanning multiple days
    Event event = new SingleEvent("Conference",
        LocalDateTime.of(2025, 11, 10, 8, 0),
        LocalDateTime.of(2025, 11, 11, 17, 0), // Next day
        "", "", true, null);

    assertFalse(event.isAllDayEvent());
  }

  /**
   * Test that an event with correct times but wrong hours is not all-day.
   */
  @Test
  public void testIsAllDayEvent_wrongTimes_returnsFalse() {
    Event event = new SingleEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 18, 0),
        "", "", true, null);

    assertFalse(event.isAllDayEvent());
  }

  /**
   * Test that a true all-day event returns true.
   */
  @Test
  public void testIsAllDayEvent_trueAllDay_returnsTrue() {
    Event event = new SingleEvent("All Day",
        LocalDateTime.of(2025, 11, 10, 8, 0),
        LocalDateTime.of(2025, 11, 10, 17, 0),
        "", "", true, null);

    assertTrue(event.isAllDayEvent());
  }

  /**
   * Test that an event starting at 8:00 but not ending at 17:00 is not all-day.
   */
  @Test
  public void testIsAllDayEvent_wrongEndTime_returnsFalse() {
    Event event = new SingleEvent("Partial Day",
        LocalDateTime.of(2025, 11, 10, 8, 0),
        LocalDateTime.of(2025, 11, 10, 16, 0), // Not 17:00
        "", "", true, null);

    assertFalse(event.isAllDayEvent());
  }

  /**
   * Test getEventsBySeriesId with non-existent seriesId returns empty list.
   */
  @Test
  public void testGetEventsBySeriesId_nonExistent_returnsEmpty() throws Exception {
    CalendarImpl cal = new CalendarImpl();

    java.lang.reflect.Method method = CalendarImpl.class.getDeclaredMethod(
        "getEventsBySeriesId", String.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    List<Event> result = (List<Event>) method.invoke(cal, "NON_EXISTENT_ID");

    assertTrue(result.isEmpty());
  }

  /**
   * Test creating event series with null weekdays throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventSeries_nullWeekdays_throws() {
    calendar.createEventSeries("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0),
        null, 3);
  }

  /**
   * Test creating event series with empty weekdays throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventSeries_emptyWeekdays_throws() {
    calendar.createEventSeries("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.noneOf(DayOfWeek.class), 3);
  }

  /**
   * Test creating event series until with null weekdays throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventSeriesUntil_nullWeekdays_throws() {
    calendar.createEventSeriesUntil("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0),
        null,
        LocalDate.of(2025, 11, 30));
  }

  /**
   * Test creating event series until with empty weekdays throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventSeriesUntil_emptyWeekdays_throws() {
    calendar.createEventSeriesUntil("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.noneOf(DayOfWeek.class),
        LocalDate.of(2025, 11, 30));
  }

  /**
   * Test SingleEvent with end before start throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSingleEvent_endBeforeStartWithNonNullEnd_throws() {
    new SingleEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 15, 0),
        LocalDateTime.of(2025, 11, 10, 14, 0),
        "", "", true, null);
  }

  /**
   * Test EventBuilder.from() creates builder with all fields from existing event.
   */
  @Test
  public void testEventBuilderFrom_copiesAllFields() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);


    List<Event> series = calendar.createEventSeries("Original", start, end,
        EnumSet.of(DayOfWeek.MONDAY), 1);
    Event original = series.get(0);


    Event eventWithAllFields = new SingleEvent("Team Meeting", start, end,
        "Weekly sync", "Conference Room A", false, "SERIES_123");


    Event copied = EventBuilder.from(eventWithAllFields)
        .build();

    assertEquals(eventWithAllFields.getSubject(), copied.getSubject());
    assertEquals(eventWithAllFields.getStartDateTime(), copied.getStartDateTime());
    assertEquals(eventWithAllFields.getEndDateTime(), copied.getEndDateTime());
    assertEquals(eventWithAllFields.getDescription(), copied.getDescription());
    assertEquals(eventWithAllFields.getLocation(), copied.getLocation());
    assertEquals(eventWithAllFields.isPublic(), copied.isPublic());
    assertEquals(eventWithAllFields.getSeriesId(), copied.getSeriesId());
  }

  /**
   * Test EventBuilder.from() with event that has no description or location.
   */
  @Test
  public void testEventBuilderFrom_withEmptyOptionals() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);

    Event simple = calendar.createEvent("Simple", start, end);

    Event copied = EventBuilder.from(simple).build();

    assertEquals("Simple", copied.getSubject());
    assertFalse(copied.getDescription().isPresent());
    assertFalse(copied.getLocation().isPresent());
  }

  /**
   * Test createEventSeries with multi-day event throws exception.
   * Covers the multi-day validation branch in createEventSeries.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventSeries_multiDayEvent_throws() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 23, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 11, 1, 0);

    calendar.createEventSeries("Overnight", start, end,
        EnumSet.of(DayOfWeek.TUESDAY), 3);
  }

  /**
   * Test createEventSeries with null end skips multi-day validation.
   * Covers the false branch of the multi-day check (end == null).
   */
  @Test
  public void testCreateEventSeries_nullEnd_createsAllDaySeries() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    List<Event> series = calendar.createEventSeries("Daily", start, null,
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 2);

    assertEquals(2, series.size());
    assertEquals(LocalDateTime.of(2025, 11, 10, 9, 0),
        series.get(0).getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 10, 17, 0),
        series.get(0).getEndDateTime());
    assertEquals(LocalDate.of(2025, 11, 12),
        series.get(1).getStartDateTime().toLocalDate());
  }

  @Test
  public void testCopyFrom_withSeriesEvent_addsToIndex() throws Exception {
    CalendarImpl source = new CalendarImpl();
    CalendarImpl target = new CalendarImpl();

    List<Event> series = source.createEventSeries("Series",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY), 1);

    Event template = series.get(0);
    assertTrue(template.isSeriesPart());

    Event copied = target.copyFrom(template,
        LocalDateTime.of(2025, 11, 17, 9, 0),
        LocalDateTime.of(2025, 11, 17, 10, 0));

    assertTrue(copied.isSeriesPart());
    assertEquals(template.getSeriesId(), copied.getSeriesId());

    java.lang.reflect.Field field = CalendarImpl.class.getDeclaredField("seriesIndex");
    field.setAccessible(true);
    calendar.SeriesIndex index = (calendar.SeriesIndex) field.get(target);

    String seriesId = copied.getSeriesId().get();
    List<LocalDateTime> starts = index.starts(seriesId);
    assertEquals(1, starts.size());
  }

  @Test
  public void testCopyFrom_withoutSeriesEvent_noSeriesId() {
    CalendarImpl source = new CalendarImpl();
    CalendarImpl target = new CalendarImpl();

    Event template = source.createEvent("Single",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    assertFalse(template.isSeriesPart());

    Event copied = target.copyFrom(template,
        LocalDateTime.of(2025, 11, 17, 9, 0),
        LocalDateTime.of(2025, 11, 17, 10, 0));

    assertFalse(copied.isSeriesPart());
  }
}
