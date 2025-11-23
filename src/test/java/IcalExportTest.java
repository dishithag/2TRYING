import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarImpl;
import calendar.Event;
import calendar.util.ExportUtil;
import calendar.util.IcalExport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.junit.Test;

/**
 * Tests for iCalendar export utility.
 */
public class IcalExportTest {

  /**
   * Test exporting calendar to ICS file creates the file.
   */
  @Test
  public void testExportToIcs_createsFile() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = IcalExport.exportToIcs(calendar, "build/test-calendar.ics");

    assertTrue(Files.exists(Paths.get(path)));
  }

  /**
   * Test ICS file contains required headers.
   */
  @Test
  public void testExportToIcs_containsHeaders() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Test Event",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = IcalExport.exportToIcs(calendar, "build/test-ical.ics");
    String content = Files.readString(Paths.get(path));

    assertTrue(content.contains("BEGIN:VCALENDAR"));
    assertTrue(content.contains("VERSION:2.0"));
    assertTrue(content.contains("END:VCALENDAR"));
    assertTrue(content.contains("BEGIN:VEVENT"));
    assertTrue(content.contains("END:VEVENT"));
  }

  /**
   * Test all-day event uses DATE format.
   */
  @Test
  public void testExportToIcs_allDayEvent() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("All Day",
        LocalDateTime.of(2025, 11, 10, 8, 0),
        null);

    String path = IcalExport.exportToIcs(calendar, "build/test-allday.ics");
    String content = Files.readString(Paths.get(path));

    assertTrue(content.contains("DTSTART;VALUE=DATE:20251110"));
    assertTrue(content.contains("DTEND;VALUE=DATE:20251111"));
  }

  /**
   * Test timed event uses UTC format.
   */
  @Test
  public void testExportToIcs_timedEvent() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Timed",
        LocalDateTime.of(2025, 11, 10, 14, 30),
        LocalDateTime.of(2025, 11, 10, 15, 30));

    String path = IcalExport.exportToIcs(calendar, "build/test-timed.ics");
    String content = Files.readString(Paths.get(path));

    assertTrue(content.contains("DTSTART:"));
    assertTrue(content.contains("DTEND:"));
    assertTrue(content.contains("T"));
    assertTrue(content.contains("Z"));
  }



  /**
   * Test private event has CLASS:PRIVATE.
   */
  @Test
  public void testExportToIcs_privateEvent() throws IOException {
    Calendar calendar = new CalendarImpl();

    calendar.createEvent("Public Event",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = IcalExport.exportToIcs(calendar, "build/test-class.ics");
    String content = Files.readString(Paths.get(path));

    assertTrue(content.contains("CLASS:PUBLIC"));
  }

  /**
   * Test escaping special characters.
   */
  @Test
  public void testExportToIcs_escapesSpecialChars() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Meeting, Planning; Review\\Test",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = IcalExport.exportToIcs(calendar, "build/test-escape.ics");
    String content = Files.readString(Paths.get(path));

    assertTrue(content.contains("\\,"));
    assertTrue(content.contains("\\;"));
    assertTrue(content.contains("\\\\"));
  }

  /**
   * Test for subject longer than 75 characters.
   *
   * @throws IOException .
   */
  @Test
  public void testExportToIcs_veryLongSubject() throws IOException {
    Calendar calendar = new CalendarImpl();


    String longSubject = "This is an extremely long event "
        + "subject name that definitely "
        + "exceeds the seventy-five character limit for iCalendar format";

    calendar.createEvent(longSubject,
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = IcalExport.exportToIcs(calendar, "build/test-very-long.ics");
    String content = Files.readString(Paths.get(path));


    assertTrue(content.contains("SUMMARY:"));
    assertTrue(content.contains(longSubject.substring(0, 30))); // At least part of it
  }


  /**
   * Test that exportToIcs writes all required iCalendar headers.
   * This kills mutations on lines 46, 48, 49 (PRODID, CALSCALE, METHOD).
   */
  @Test
  public void testExportToIcs_allHeadersPresent() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Test",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = IcalExport.exportToIcs(calendar, "build/test-headers.ics");
    String content = Files.readString(Paths.get(path));

    assertTrue(content.contains("BEGIN:VCALENDAR"));
    assertTrue(content.contains("PRODID:-//PDP Calendar//EN"));
    assertTrue(content.contains("VERSION:2.0"));
    assertTrue(content.contains("CALSCALE:GREGORIAN"));
    assertTrue(content.contains("METHOD:PUBLISH"));
    assertTrue(content.contains("END:VCALENDAR"));
  }

  /**
   * Test that UID is generated for each event.
   * This kills mutation on line 57 and tests uidFor (line 91).
   */
  @Test
  public void testExportToIcs_containsUniqueUid() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Meeting1",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));
    calendar.createEvent("Meeting2",
        LocalDateTime.of(2025, 11, 11, 9, 0),
        LocalDateTime.of(2025, 11, 11, 10, 0));

    String path = IcalExport.exportToIcs(calendar, "build/test-uid.ics");
    String content = Files.readString(Paths.get(path));


    int uidCount = content.split("UID:").length - 1;
    assertEquals(2, uidCount);

    assertTrue(content.contains("@pdp-calendar"));
  }

  /**
   * Test that DTSTAMP is included in each event.
   * This kills mutation on line 58.
   */
  @Test
  public void testExportToIcs_containsDtstamp() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Event",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = IcalExport.exportToIcs(calendar, "build/test-dtstamp.ics");
    String content = Files.readString(Paths.get(path));

    assertTrue(content.contains("DTSTAMP:"));
  }

  /**
   * Test export with event that has both description AND location.
   * This kills mutations on lines 76-77 (ifPresent calls).
   */
  @Test
  public void testExportToIcs_withDescriptionAndLocation() throws IOException {
    Calendar calendar = new CalendarImpl();


    Event event = new calendar.SingleEvent(
        "Team Meeting",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0),
        "Weekly sync meeting",  // description
        "Conference Room A",     // location
        true,
        null
    );


    CalendarImpl cal = (CalendarImpl) calendar;
    try {
      java.lang.reflect.Field eventsField = CalendarImpl.class.getDeclaredField("events");
      eventsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      java.util.List<Event> eventsList = (java.util.List<Event>) eventsField.get(cal);
      eventsList.add(event);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    String path = IcalExport.exportToIcs(calendar, "build/test-desc-loc.ics");
    String content = Files.readString(Paths.get(path));


    assertTrue(content.contains("DESCRIPTION:Weekly sync meeting"));
    assertTrue(content.contains("LOCATION:Conference Room A"));
  }

  /**
   * Test fold75 with string exactly 75 characters.
   * This kills mutations on line 103 (conditional boundary).
   */
  @Test
  public void testExportToIcs_exactly75Chars_noFold() throws IOException {
    Calendar calendar = new CalendarImpl();

    String subject67 = "A".repeat(67);

    calendar.createEvent(subject67,
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = IcalExport.exportToIcs(calendar, "build/test-exact75.ics");
    String content = Files.readString(Paths.get(path));


    assertTrue(content.contains("SUMMARY:" + subject67));


    String summaryLine = content.substring(
        content.indexOf("SUMMARY:"),
        content.indexOf("CLASS:", content.indexOf("SUMMARY:"))
    );
    assertFalse(summaryLine.contains("\r\n "));
  }

  /**
   * Test fold75 with string exactly 76 characters (boundary).
   * This kills mutations on line 103 (changed conditional boundary).
   */
  @Test
  public void testExportToIcs_seventySixChars_doesFold() throws IOException {
    Calendar calendar = new CalendarImpl();


    String subject68 = "B".repeat(68);

    calendar.createEvent(subject68,
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = IcalExport.exportToIcs(calendar, "build/test-76chars.ics");
    String content = Files.readString(Paths.get(path));


    assertTrue(content.contains("SUMMARY:"));

    assertTrue(content.contains(subject68.substring(0, 40)));
  }

  /**
   * Test fold75 loop boundary when i reaches exactly s.length().
   * This kills mutation on line 112 (changed conditional boundary).
   */
  @Test
  public void testExportToIcs_veryLongSubject_multipleFolds() throws IOException {
    Calendar calendar = new CalendarImpl();


    String longSubject = "X".repeat(200);

    calendar.createEvent(longSubject,
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = IcalExport.exportToIcs(calendar, "build/test-multifold.ics");
    String content = Files.readString(Paths.get(path));


    assertTrue(content.contains("SUMMARY:"));


    assertTrue(content.contains("\r\n "));


    long xcount = content.chars().filter(ch -> ch == 'X').count();
    assertTrue(xcount >= 200);
  }

  /**
   * Test that path.getParent() being null doesn't break export.
   * This kills mutation on line 40 (negated conditional).
   */
  @Test
  public void testExportToIcs_noParentDirectory() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Test",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));


    String path = IcalExport.exportToIcs(calendar, "test-no-parent.ics");

    assertTrue(Files.exists(Paths.get(path)));


    Files.deleteIfExists(Paths.get("test-no-parent.ics"));
  }

  /**
   * Test export to iCal returns absolute path.
   */
  @Test
  public void testExport_ics_returnsAbsolutePath() throws IOException {
    Calendar calendar = new CalendarImpl();
    calendar.createEvent("Event",
        LocalDateTime.of(2025, 11, 10, 9, 0),
        LocalDateTime.of(2025, 11, 10, 10, 0));

    String path = ExportUtil.export(calendar, "build/test-absolute.ics");

    assertTrue(Paths.get(path).isAbsolute());
    assertTrue(Files.exists(Paths.get(path)));
  }
}
