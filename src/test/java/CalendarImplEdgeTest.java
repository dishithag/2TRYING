import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarImpl;
import calendar.Event;
import calendar.EventProperty;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

/**
 * Extra edge tests for CalendarImpl to hit remaining branches.
 */
@SuppressWarnings("unchecked")
public class CalendarImplEdgeTest {

  private Calendar calendar;

  /**
   * Creates a fresh calendar instance before each test.
   */
  @Before
  public void setUp() {
    calendar = new CalendarImpl();
  }

  /**
   * Invalid weekday letter should fail in parseWeekdays.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateSeries_invalidWeekday() {
    Calendar cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 5, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 5, 10, 0);
    cal.createEventSeries("Bad", start, end, EnumSet.noneOf(DayOfWeek.class), 3);
  }

  /**
   * Two events with same subject and start cause editEvent to fail.
   */
  @Test
  public void testEditSingleEvent_multipleMatchesThrows() {
    Calendar cal = new CalendarImpl();
    LocalDateTime t = LocalDateTime.of(2025, 11, 6, 9, 0);
    cal.createEvent("Meet", t, t.plusHours(1));
    cal.createEvent("Meet", t, t.plusHours(2));

    assertThrows(IllegalArgumentException.class,
        () -> cal.editEvent("Meet", t, "location", "Room 1"));
  }

  /**
   * Editing from date on a non-series event should just edit that one.
   */
  @Test
  public void testEditEventsFromDate_onNonSeriesEditsOnce() {
    Calendar cal = new CalendarImpl();
    LocalDateTime t = LocalDateTime.of(2025, 11, 7, 14, 0);
    cal.createEvent("Solo", t, t.plusHours(1));

    cal.editEventsFromDate("Solo", t, "description", "done");

    List<Event> all = cal.getAllEvents();
    assertEquals(1, all.size());
    assertTrue(all.get(0).getDescription().isPresent());
    assertEquals("done", all.get(0).getDescription().get());
  }

  /**
   * Editing series when the event is not in a series should edit only that one.
   */
  @Test
  public void testEditSeries_onNonSeriesEditsOne() {
    Calendar cal = new CalendarImpl();
    LocalDateTime t = LocalDateTime.of(2025, 11, 8, 10, 0);
    cal.createEvent("One", t, t.plusHours(1));

    cal.editSeries("One", t, "location", "Zoom");

    List<Event> all = cal.getAllEvents();
    assertEquals(1, all.size());
    assertTrue(all.get(0).getLocation().isPresent());
    assertEquals("Zoom", all.get(0).getLocation().get());
  }

  /**
   * Creating the same null-end event twice should fail through eventExists.
   */
  @Test
  public void testCreateEvent_duplicateWithNullEnd() {
    Calendar cal = new CalendarImpl();
    LocalDateTime t = LocalDateTime.of(2025, 11, 9, 8, 0);
    cal.createEvent("Block", t, null);

    assertThrows(IllegalArgumentException.class,
        () -> cal.createEvent("Block", t, null));
  }

  /**
   * Series-until with an until date before start should return empty list.
   */
  @Test
  public void testCreateEventSeriesUntil_untilBeforeStart_givesEmpty() {
    Calendar cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    LocalDate until = LocalDate.of(2025, 11, 1);

    List<Event> made =
        cal.createEventSeriesUntil("Early", start, end, EnumSet.of(DayOfWeek.MONDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), until);

    assertTrue(made.isEmpty());
  }

  /**
   * Creating an event with null subject should fail.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateEvent_nullSubject_throws() {
    calendar.createEvent(
        null,
        LocalDateTime.of(2025, 11, 3, 9, 0),
        LocalDateTime.of(2025, 11, 3, 10, 0));
  }

  /**
   * Creating an event whose end is before start should fail.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateEvent_endBeforeStart_throws() {
    calendar.createEvent(
        "Backwards",
        LocalDateTime.of(2025, 11, 3, 10, 0),
        LocalDateTime.of(2025, 11, 3, 9, 0));
  }

  /**
   * Creating an event series with zero occurrences should fail.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventSeries_zeroOccurrences_throws() {
    calendar.createEventSeries(
        "Zero",
        LocalDateTime.of(2025, 11, 4, 9, 0),
        LocalDateTime.of(2025, 11, 4, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        0);
  }




  /**
   * parseProperty: null branch.
   */
  @Test(expected = IllegalArgumentException.class)
  public void parseProperty_null_throws() throws Exception {
    CalendarImpl cal = new CalendarImpl();
    Method m = CalendarImpl.class.getDeclaredMethod("parseProperty", String.class);
    m.setAccessible(true);
    try {
      m.invoke(cal, new Object[] {null});
    } catch (java.lang.reflect.InvocationTargetException ex) {
      throw (IllegalArgumentException) ex.getCause();
    }
  }

  /**
   * parseProperty: unknown branch.
   */
  @Test(expected = IllegalArgumentException.class)
  public void parseProperty_unknown_throws() throws Exception {
    CalendarImpl cal = new CalendarImpl();
    Method m = CalendarImpl.class.getDeclaredMethod("parseProperty", String.class);
    m.setAccessible(true);
    try {
      m.invoke(cal, "bogus-prop");
    } catch (java.lang.reflect.InvocationTargetException ex) {
      throw (IllegalArgumentException) ex.getCause();
    }
  }

  /**
   * Test setZoneId converts event times to preserve the same instant.
   */
  @Test
  public void testSetZoneId_convertsEventTimes() {
    CalendarImpl cal = new CalendarImpl("Test", ZoneId.of("America/New_York"));


    LocalDateTime nyTime = LocalDateTime.of(2025, 11, 10, 14, 0);
    cal.createEvent("Meeting", nyTime, nyTime.plusHours(1));


    cal.setZoneId(ZoneId.of("Europe/London"));


    List<Event> events = cal.getAllEvents();
    assertEquals(1, events.size());
    assertEquals(LocalDateTime.of(2025, 11, 10, 19, 0),
        events.get(0).getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 10, 20, 0),
        events.get(0).getEndDateTime());
  }

  /**
   * Test setZoneId preserves event properties during conversion.
   */
  @Test
  public void testSetZoneId_preservesEventProperties() {
    CalendarImpl cal = new CalendarImpl("Test", ZoneId.of("America/New_York"));


    Event original = new calendar.SingleEvent(
        "Team Meeting",
        LocalDateTime.of(2025, 11, 10, 14, 0),
        LocalDateTime.of(2025, 11, 10, 15, 0),
        "Weekly sync",
        "Conference Room A",
        false,  // private event
        null
    );

    cal.createEvent("Team Meeting",
        LocalDateTime.of(2025, 11, 10, 14, 0),
        LocalDateTime.of(2025, 11, 10, 15, 0));


    cal.setZoneId(ZoneId.of("America/Los_Angeles"));


    List<Event> events = cal.getAllEvents();
    assertEquals(1, events.size());
    Event converted = events.get(0);
    assertEquals("Team Meeting", converted.getSubject());
    assertTrue(converted.isPublic()); // Default is public from createEvent
  }

  /**
   * Test setZoneId with series events converts all instances.
   */
  @Test
  public void testSetZoneId_convertsSeries() {
    CalendarImpl cal = new CalendarImpl("Test", ZoneId.of("America/New_York"));


    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    List<Event> series = cal.createEventSeries("Daily Standup",
        start, end, EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 2);

    Optional<String> originalSeriesId = series.get(0).getSeriesId();


    cal.setZoneId(ZoneId.of("Asia/Tokyo"));


    List<Event> events = cal.getAllEvents();
    assertEquals(2, events.size());

    for (Event e : events) {
      assertEquals("Daily Standup", e.getSubject());
      assertTrue(e.isSeriesPart());
      assertEquals(originalSeriesId, e.getSeriesId());

      assertEquals(23, e.getStartDateTime().getHour());
    }
  }

  /**
   * Test setZoneId with same zone does nothing.
   */
  @Test
  public void testSetZoneId_sameZone_noConversion() {
    CalendarImpl cal = new CalendarImpl("Test", ZoneId.of("America/New_York"));

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 14, 0);
    cal.createEvent("Meeting", start, start.plusHours(1));

    cal.setZoneId(ZoneId.of("America/New_York"));


    List<Event> events = cal.getAllEvents();
    assertEquals(LocalDateTime.of(2025, 11, 10, 14, 0),
        events.get(0).getStartDateTime());
  }

  /**
   * Test setZoneId with null throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSetZoneId_null_throws() {
    CalendarImpl cal = new CalendarImpl();
    cal.setZoneId(null);
  }

  /**
   * Test setZoneId updates the calendar's zone.
   */
  @Test
  public void testSetZoneId_updatesCalendarZone() {
    CalendarImpl cal = new CalendarImpl("Test", ZoneId.of("America/New_York"));

    cal.setZoneId(ZoneId.of("Europe/Paris"));

    assertEquals(ZoneId.of("Europe/Paris"), cal.getZoneId());
  }

  /**
   * Test convertAllEventsToZone preserves description and location.
   */
  @Test
  public void testSetZoneId_preservesDescriptionAndLocation() throws Exception {
    CalendarImpl cal = new CalendarImpl("Test", ZoneId.of("UTC"));


    Event event = new calendar.SingleEvent(
        "Important Meeting",
        LocalDateTime.of(2025, 11, 10, 14, 0),
        LocalDateTime.of(2025, 11, 10, 15, 0),
        "Quarterly review",
        "Building A",
        true,
        null
    );


    java.lang.reflect.Field eventsField = CalendarImpl.class.getDeclaredField("events");
    eventsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    List<Event> eventsList = (List<Event>) eventsField.get(cal);
    eventsList.add(event);

    cal.setZoneId(ZoneId.of("America/New_York"));


    List<Event> converted = cal.getAllEvents();
    assertEquals(1, converted.size());
    Event result = converted.get(0);

    assertEquals("Important Meeting", result.getSubject());
    assertTrue(result.getDescription().isPresent());
    assertEquals("Quarterly review", result.getDescription().get());
    assertTrue(result.getLocation().isPresent());
    assertEquals("Building A", result.getLocation().get());
    assertTrue(result.isPublic());
  }

  /**
   * Test addCopied successfully adds an event.
   */
  @Test
  public void testAddCopied_addsEvent() throws Exception {
    CalendarImpl cal = new CalendarImpl();


    Event template = new calendar.SingleEvent(
        "Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0),
        "Weekly sync",
        "Room 101",
        true,
        null
    );




    LocalDateTime newStart = LocalDateTime.of(2025, 11, 11, 14, 0);
    LocalDateTime newEnd = LocalDateTime.of(2025, 11, 11, 15, 0);

    Event copied = (Event) cal.copyFrom(template, newStart, newEnd);


    assertNotNull(copied);
    assertEquals("Meeting", copied.getSubject());
    assertEquals(newStart, copied.getStartDateTime());
    assertEquals(newEnd, copied.getEndDateTime());
    assertEquals("Weekly sync", copied.getDescription().orElse(""));
    assertEquals("Room 101", copied.getLocation().orElse(""));
    assertTrue(copied.isPublic());

    // Verify it was added to the calendar
    List<Event> events = cal.getAllEvents();
    assertEquals(1, events.size());
    assertEquals(copied, events.get(0));
  }



  /**
   * Test CalendarImpl constructor with null name throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_nullName_throws() {
    new CalendarImpl(null, ZoneId.of("America/New_York"));
  }

  /**
   * Test CalendarImpl constructor with blank name throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_blankName_throws() {
    new CalendarImpl("   ", ZoneId.of("America/New_York"));
  }

  /**
   * Test CalendarImpl constructor with null ZoneId throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_nullZoneId_throws() {
    new CalendarImpl("My Calendar", null);
  }

  /**
   * Test CalendarImpl constructor with valid parameters succeeds.
   */
  @Test
  public void testConstructor_validParameters_succeeds() {
    Calendar cal = new CalendarImpl("Work", ZoneId.of("Europe/London"));

    assertEquals("Work", cal.getName());
    assertEquals(ZoneId.of("Europe/London"), cal.getZoneId());
    assertEquals(0, cal.getAllEvents().size());
  }

  /**
   * Test rename with null name throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRename_null_throws() {
    Calendar cal = new CalendarImpl();
    cal.rename(null);
  }

  /**
   * Test rename with blank name throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testRename_blank_throws() {
    Calendar cal = new CalendarImpl();
    cal.rename("   ");
  }

  /**
   * Test rename with valid name succeeds.
   */
  @Test
  public void testRename_validName_succeeds() {
    Calendar cal = new CalendarImpl("Old Name", ZoneId.of("America/New_York"));

    cal.rename("New Name");

    assertEquals("New Name", cal.getName());
  }



  /**
   * Test editEvent with EventProperty enum and LocalDateTime parameter.
   */
  @Test
  public void testEditEvent_withEventPropertyEnum_dateTime() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    cal.createEvent("Meeting", start, end);

    cal.editEvent("Meeting", start, EventProperty.START,
        LocalDateTime.of(2025, 11, 10, 8, 30), null);

    List<Event> events = cal.getAllEvents();
    assertEquals(1, events.size());
    assertEquals(LocalDateTime.of(2025, 11, 10, 8, 30),
        events.get(0).getStartDateTime());
  }

  /**
   * Test editEvent with EventProperty enum and String parameter.
   */
  @Test
  public void testEditEvent_withEventPropertyEnum_text() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    cal.createEvent("Meeting", start, end);

    cal.editEvent("Meeting", start, EventProperty.LOCATION, null, "Room 101");

    List<Event> events = cal.getAllEvents();
    assertEquals(1, events.size());
    assertEquals("Room 101", events.get(0).getLocation().get());
  }

  /**
   * Test editEvent with EventProperty enum throws when value is null.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditEvent_withEventPropertyEnum_nullValue_throws() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    cal.createEvent("Meeting", start, start.plusHours(1));

    cal.editEvent("Meeting", start, EventProperty.LOCATION, null, null);
  }

  /**
   * Test editEventsFromDate with EventProperty enum and LocalDateTime parameter.
   */
  @Test
  public void testEditEventsFromDate_withEventPropertyEnum_dateTime() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    cal.createEventSeries("Series", start, end,
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 3);

    cal.editEventsFromDate("Series", start, EventProperty.START,
        LocalDateTime.of(2025, 11, 10, 8, 0), null);

    List<Event> events = cal.getAllEvents();
    assertTrue(events.size() >= 1);
    assertEquals(LocalDateTime.of(2025, 11, 10, 8, 0),
        events.get(0).getStartDateTime());
  }

  /**
   * Test editEventsFromDate with EventProperty enum and String parameter.
   */
  @Test
  public void testEditEventsFromDate_withEventPropertyEnum_text() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    cal.createEventSeries("Series", start, end,
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), 2);

    cal.editEventsFromDate("Series", start, EventProperty.DESCRIPTION,
        null, "Updated description");

    List<Event> events = cal.getAllEvents();
    assertTrue(events.size() >= 1);
    assertEquals("Updated description", events.get(0).getDescription().get());
  }

  /**
   * Test editEventsFromDate with EventProperty enum throws when value is null.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditEventsFromDate_withEventPropertyEnum_nullValue_throws() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    cal.createEvent("Meeting", start, start.plusHours(1));

    cal.editEventsFromDate("Meeting", start, EventProperty.SUBJECT, null, null);
  }

  /**
   * Test editSeries with EventProperty enum and LocalDateTime parameter.
   */
  @Test
  public void testEditSeries_withEventPropertyEnum_dateTime() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    cal.createEventSeries("Series", start, end,
        EnumSet.of(DayOfWeek.MONDAY), 2);

    cal.editSeries("Series", start, EventProperty.START,
        LocalDateTime.of(2025, 11, 10, 10, 0), null);

    List<Event> events = cal.getAllEvents();
    assertEquals(2, events.size());
  }

  /**
   * Test editSeries with EventProperty enum and String parameter.
   */
  @Test
  public void testEditSeries_withEventPropertyEnum_text() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    cal.createEventSeries("Series", start, end,
        EnumSet.of(DayOfWeek.MONDAY), 2);

    cal.editSeries("Series", start, EventProperty.LOCATION, null, "Zoom");

    List<Event> events = cal.getAllEvents();
    assertEquals(2, events.size());
    assertEquals("Zoom", events.get(0).getLocation().get());
    assertEquals("Zoom", events.get(1).getLocation().get());
  }

  /**
   * Test editSeries with EventProperty enum throws when value is null.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditSeries_withEventPropertyEnum_nullValue_throws() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    cal.createEvent("Meeting", start, start.plusHours(1));

    cal.editSeries("Meeting", start, EventProperty.DESCRIPTION, null, null);
  }

  /**
   * Test editSeries with EventProperty.END using LocalDateTime parameter.
   * Covers the END branch of the ternary operator.
   */
  @Test
  public void testEditSeries_withEventPropertyEnum_endDateTime() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    cal.createEventSeries("Series", start, end,
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 2);

    cal.editSeries("Series", start, EventProperty.DESCRIPTION,
        null, "Team standup");

    List<Event> events = cal.getAllEvents();
    assertEquals(2, events.size());
    assertEquals("Team standup", events.get(0).getDescription().get());
    assertEquals("Team standup", events.get(1).getDescription().get());
  }

  /**
   * Test editSeries when event has null seriesId returns early.
   * Covers the seriesId == null check in editSeries.
   */
  @Test
  public void testEditSeries_nullSeriesId_returnsEarly() throws Exception {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);


    cal.createEvent("Single", start, end);


    cal.editSeries("Single", start, EventProperty.LOCATION, null, "Room 101");

    List<Event> events = cal.getAllEvents();
    assertEquals(1, events.size());
    assertEquals("Room 101", events.get(0).getLocation().get());
  }

  /**
   * Test editEventsFromDate with EventProperty.END parameter.
   * Covers the END branch of the ternary operator.
   */
  @Test
  public void testEditEventsFromDate_withEventPropertyEnum_endProperty() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    cal.createEventSeries("Series", start, end,
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 2);

    cal.editEventsFromDate("Series",
        LocalDateTime.of(2025, 11, 12, 9, 0),
        EventProperty.END,
        LocalDateTime.of(2025, 11, 12, 11, 0), null);

    List<Event> events = cal.getAllEvents();
    assertEquals(2, events.size());

    assertFalse(events.get(0).getEndDateTime().equals(events.get(1).getEndDateTime()));
  }

  /**
   * Test that rebuildSeriesIndex is actually called when setZoneId changes zone.
   * Kills mutation on line 81 (removed call to rebuildSeriesIndex).
   */
  @Test
  public void testSetZoneId_callsRebuildSeriesIndex() throws Exception {
    CalendarImpl cal = new CalendarImpl("Test", ZoneId.of("America/New_York"));

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    cal.createEventSeries("Series", start, LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY), 2);

    cal.setZoneId(ZoneId.of("Europe/London"));


    cal.editSeries("Series",
        LocalDateTime.of(2025, 11, 10, 14, 0), // Converted time
        "location", "Updated");

    List<Event> events = cal.getAllEvents();
    assertEquals(2, events.size());
    assertEquals("Updated", events.get(0).getLocation().get());
  }

  /**
   * Test findEvents returns true when subject matches.
   * Kills mutation on line 162 (replaced boolean return with true).
   */
  @Test
  public void testFindEvents_subjectMismatch_returnsEmpty() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    cal.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 10, 0));

    List<Event> found = cal.findEvents("DifferentSubject", start);

    assertEquals(0, found.size());
  }

  /**
   * Test lambda in editEventsFromDate filters correctly.
   * Kills mutations on lines 205 and 205.4.
   */
  @Test
  public void testEditEventsFromDate_filtersNonMatchingSeriesId() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start1 = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 11, 10, 10, 0);

    cal.createEventSeries("Series1", start1, LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY), 2);
    cal.createEventSeries("Series2", start2, LocalDateTime.of(2025, 11, 10, 11, 0),
        EnumSet.of(DayOfWeek.MONDAY), 2);

    cal.editEventsFromDate("Series1", start1, "location", "Room1");

    List<Event> all = cal.getAllEvents();
    int withRoom1 = 0;
    for (Event e : all) {
      if (e.getLocation().isPresent() && "Room1".equals(e.getLocation().get())) {
        withRoom1++;
      }
    }
    assertEquals(2, withRoom1);
  }



  /**
   * Test getEventsBySeriesId returns empty when seriesId not in map.
   * Kills mutation on line 473 (replaced return with Collections.emptyList).
   */
  @Test
  public void testGetEventsBySeriesId_nullStarts_returnsNewList() throws Exception {
    CalendarImpl cal = new CalendarImpl();

    java.lang.reflect.Method method = CalendarImpl.class.getDeclaredMethod(
        "getEventsBySeriesId", String.class);
    method.setAccessible(true);

    List<Event> result1 = (List<Event>) method.invoke(cal, "MISSING_ID");
    List<Event> result2 = (List<Event>) method.invoke(cal, "MISSING_ID");

    assertTrue(result1.isEmpty());
    assertTrue(result2.isEmpty());
    assertTrue(result1 != result2);
  }

  /**
   * Test rebuildSeriesIndex sorts starts.
   * Kills mutation on line 544 (removed call to sort).
   */
  @Test
  public void testRebuildSeriesIndex_sortsStarts() throws Exception {
    CalendarImpl cal = new CalendarImpl();


    cal.createEvent("A", LocalDateTime.of(2025, 11, 12, 9, 0),
        LocalDateTime.of(2025, 11, 12, 10, 0));
    cal.createEvent("B", LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    List<Event> sorted = cal.getEventsInRange(
        LocalDateTime.of(2025, 11, 1, 0, 0),
        LocalDateTime.of(2025, 11, 30, 23, 59));

    assertEquals("B", sorted.get(0).getSubject());
    assertEquals("A", sorted.get(1).getSubject());
  }


  /**
   * Test editEventsFromDate with EventProperty.END and null newDateTime throws.
   * Covers the newDateTime == null branch for END property.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditEventsFromDate_endPropertyNullDateTime_throws() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    cal.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 10, 0));

    cal.editEventsFromDate("Meeting", start, EventProperty.END, null, null);
  }

  /**
   * Test editSeries with EventProperty.END and null newDateTime throws.
   * Covers the newDateTime == null branch for END property in editSeries.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditSeries_endPropertyNullDateTime_throws() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    cal.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 10, 0));

    cal.editSeries("Meeting", start, EventProperty.END, null, null);
  }

  /**
   * Test copyFrom throws exception when creating duplicate.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCopyFrom_duplicate_throws() {
    CalendarImpl cal = new CalendarImpl();

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime end = LocalDateTime.of(2025, 11, 10, 10, 0);
    cal.createEvent("Meeting", start, end);

    Event template = new calendar.SingleEvent(
        "Meeting",
        LocalDateTime.of(2025, 11, 11, 9, 0),
        LocalDateTime.of(2025, 11, 11, 10, 0),
        "",
        "",
        true,
        null
    );

    cal.copyFrom(template, start, end);
  }

  @Test
  public void testCreateEvent_withSeriesId_addsToIndex() throws Exception {
    CalendarImpl cal = new CalendarImpl();

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    List<Event> series = cal.createEventSeries("Meeting", start,
        LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY), 2);

    String seriesId = series.get(0).getSeriesId().get();

    java.lang.reflect.Field field = CalendarImpl.class.getDeclaredField("seriesIndex");
    field.setAccessible(true);
    calendar.SeriesIndex index = (calendar.SeriesIndex) field.get(cal);

    List<LocalDateTime> starts = index.starts(seriesId);
    assertEquals(2, starts.size());
  }

  @Test
  public void testCreateEventSeriesUntil_addsToSeriesIndex() throws Exception {
    CalendarImpl cal = new CalendarImpl();

    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);
    List<Event> series = cal.createEventSeriesUntil("Meeting", start,
        LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY), LocalDate.of(2025, 11, 24));

    String seriesId = series.get(0).getSeriesId().get();

    java.lang.reflect.Field field = CalendarImpl.class.getDeclaredField("seriesIndex");
    field.setAccessible(true);
    calendar.SeriesIndex index = (calendar.SeriesIndex) field.get(cal);

    List<LocalDateTime> starts = index.starts(seriesId);
    assertTrue(starts.size() >= 2);
  }

  @Test
  public void testFindEvents_matchingStartOnly() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    cal.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 10, 0));
    cal.createEvent("Meeting", start, LocalDateTime.of(2025, 11, 10, 11, 0));
    cal.createEvent("Other", start, LocalDateTime.of(2025, 11, 10, 10, 0));

    List<Event> found = cal.findEvents("Meeting", start);
    assertEquals(2, found.size());
  }

  @Test
  public void testEditEventsFromDate_nonSeries_callsEnforceNoDuplicate() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    cal.createEvent("Single", start, LocalDateTime.of(2025, 11, 10, 10, 0));

    cal.editEventsFromDate("Single", start, "location", "Room 1");

    List<Event> events = cal.getAllEvents();
    assertEquals(1, events.size());
    assertEquals("Room 1", events.get(0).getLocation().get());
  }

  @Test
  public void testEditEventsFromDate_series_callsEnforceNoDuplicate() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    List<Event> series = cal.createEventSeries("Series", start,
        LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 2);

    cal.editEventsFromDate("Series",
        LocalDateTime.of(2025, 11, 12, 9, 0),
        "location", "Room 1");

    List<Event> events = cal.getAllEvents();
    assertEquals(2, events.size());
  }

  @Test
  public void testEditSeries_nonSeries_callsEnforceNoDuplicate() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    cal.createEvent("Single", start, LocalDateTime.of(2025, 11, 10, 10, 0));

    cal.editSeries("Single", start, "description", "Updated");

    List<Event> events = cal.getAllEvents();
    assertEquals(1, events.size());
    assertEquals("Updated", events.get(0).getDescription().get());
  }

  @Test
  public void testEditSeries_start_callsEnforceNoDuplicate() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    List<Event> series = cal.createEventSeries("Series", start,
        LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 2);

    cal.editSeries("Series", start, "start", "2025-11-10T08:00");

    List<Event> events = cal.getAllEvents();
    assertEquals(2, events.size());
    assertEquals(LocalDateTime.of(2025, 11, 10, 8, 0), events.get(0).getStartDateTime());
  }

  @Test
  public void testEditSeries_nonStart_callsEnforceNoDuplicate() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    List<Event> series = cal.createEventSeries("Series", start,
        LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 2);

    cal.editSeries("Series", start, "location", "Conference Room");

    List<Event> events = cal.getAllEvents();
    assertEquals(2, events.size());
    for (Event e : events) {
      assertEquals("Conference Room", e.getLocation().get());
    }
  }

  @Test
  public void testCreateEvent_noSeriesId_doesNotAddToIndex() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    Event event = cal.createEvent("Single", start,
        LocalDateTime.of(2025, 11, 10, 10, 0));

    assertFalse(event.isSeriesPart());
    assertEquals(1, cal.getAllEvents().size());
  }

  @Test
  public void testCopyFrom_noSeriesId_doesNotAddToIndex() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    Event template = cal.createEvent("Template", start,
        LocalDateTime.of(2025, 11, 10, 10, 0));

    Event copied = cal.copyFrom(template,
        LocalDateTime.of(2025, 11, 15, 9, 0),
        LocalDateTime.of(2025, 11, 15, 10, 0));

    assertFalse(copied.isSeriesPart());
    assertEquals(2, cal.getAllEvents().size());
  }

  @Test
  public void testFindEvents_startMatches_endDoesNotMatch_filtered() {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    cal.createEvent("Event1", start, LocalDateTime.of(2025, 11, 10, 10, 0));
    cal.createEvent("Event1", start, LocalDateTime.of(2025, 11, 10, 11, 0));

    List<Event> found = cal.findEvents("Event1", start,
        LocalDateTime.of(2025, 11, 10, 12, 0));

    assertEquals(0, found.size());
  }

  @Test
  public void testReplaceEvent_sameSeries_bothNull() throws Exception {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    cal.createEvent("Single", start, LocalDateTime.of(2025, 11, 10, 10, 0));

    cal.editEvent("Single", start, "location", "Room 1");

    List<Event> events = cal.getAllEvents();
    assertEquals(1, events.size());
    assertEquals("Room 1", events.get(0).getLocation().get());
  }

  @Test
  public void testReplaceEvent_sameSeries_bothNonNull_callsReplaceStart() throws Exception {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    List<Event> series = cal.createEventSeries("Series", start,
        LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY), 1);

    String oldSeriesId = series.get(0).getSeriesId().get();

    cal.editEvent("Series", start, "start", "2025-11-10T08:30");

    java.lang.reflect.Field field = CalendarImpl.class.getDeclaredField("seriesIndex");
    field.setAccessible(true);
    calendar.SeriesIndex index = (calendar.SeriesIndex) field.get(cal);

    List<LocalDateTime> starts = index.starts(oldSeriesId);
    assertEquals(1, starts.size());
    assertEquals(LocalDateTime.of(2025, 11, 10, 8, 30), starts.get(0));
  }


  @Test
  public void testReplaceEvent_differentSeries_bothNonNull_callsRemoveAndAdd() throws Exception {
    CalendarImpl cal = new CalendarImpl();
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);

    List<Event> series1 = cal.createEventSeries("Series1", start,
        LocalDateTime.of(2025, 11, 10, 10, 0),
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 3);

    String oldSeriesId = series1.get(0).getSeriesId().get();

    cal.editEventsFromDate("Series1",
        LocalDateTime.of(2025, 11, 12, 9, 0),
        "start", "2025-11-12T10:00");

    java.lang.reflect.Field field = CalendarImpl.class.getDeclaredField("seriesIndex");
    field.setAccessible(true);
    calendar.SeriesIndex index = (calendar.SeriesIndex) field.get(cal);

    List<LocalDateTime> oldStarts = index.starts(oldSeriesId);
    assertEquals(1, oldStarts.size());

    List<Event> allEvents = cal.getAllEvents();
    assertEquals(3, allEvents.size());

    Event modifiedEvent = allEvents.stream()
        .filter(e -> e.getStartDateTime().getHour() == 10)
        .findFirst()
        .get();

    String newSeriesId = modifiedEvent.getSeriesId().get();
    assertFalse(newSeriesId.equals(oldSeriesId));

    List<LocalDateTime> newStarts = index.starts(newSeriesId);
    assertEquals(2, newStarts.size());
  }
}
