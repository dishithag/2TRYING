package calendar.util;

import calendar.Calendar;
import calendar.Event;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Exports calendars to iCalendar (.ics / .ical). Times are emitted in UTC.
 */
public final class IcalExport {

  private static final DateTimeFormatter UTC_TS =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter DATE_ONLY =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  private IcalExport() {}

  /**
   * Writes the calendar to an iCalendar file.
   *
   * @param calendar calendar to export
   * @param filePath destination path (ends with .ics or .ical)
   * @return absolute path written
   * @throws IOException on write failure
   */
  public static String exportToIcs(Calendar calendar, String filePath) throws IOException {
    Path path = Paths.get(filePath);
    if (path.getParent() != null) {
      Files.createDirectories(path.getParent());
    }

    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path))) {
      out.println("BEGIN:VCALENDAR");
      out.println("PRODID:-//PDP Calendar//EN");
      out.println("VERSION:2.0");
      out.println("CALSCALE:GREGORIAN");
      out.println("METHOD:PUBLISH");

      List<Event> events = calendar.getAllEvents();
      ZoneId zone = calendar.getZoneId();
      ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);

      for (Event e : events) {
        out.println("BEGIN:VEVENT");
        out.println("UID:" + uidFor(e, calendar));
        out.println("DTSTAMP:" + UTC_TS.format(nowUtc));

        if (e.isAllDayEvent()) {
          LocalDate startDate = e.getStartDateTime().toLocalDate();
          LocalDate endDateExclusive = e.getEndDateTime().toLocalDate().plusDays(1);
          out.println("DTSTART;VALUE=DATE:" + DATE_ONLY.format(startDate));
          out.println("DTEND;VALUE=DATE:" + DATE_ONLY.format(endDateExclusive));
        } else {
          ZonedDateTime startUtc =
              e.getStartDateTime().atZone(zone).withZoneSameInstant(ZoneOffset.UTC);
          ZonedDateTime endUtc =
              e.getEndDateTime().atZone(zone).withZoneSameInstant(ZoneOffset.UTC);
          out.println("DTSTART:" + UTC_TS.format(startUtc));
          out.println("DTEND:" + UTC_TS.format(endUtc));
        }

        out.println("SUMMARY:" + escape(e.getSubject()));
        out.println("CLASS:" + (e.isPublic() ? "PUBLIC" : "PRIVATE"));
        e.getDescription().ifPresent(d -> out.println("DESCRIPTION:" + escape(d)));
        e.getLocation().ifPresent(l -> out.println("LOCATION:" + escape(l)));
        out.println("END:VEVENT");
      }

      out.println("END:VCALENDAR");
    }

    return path.toAbsolutePath().toString();
  }

  private static String uidFor(Event e, Calendar cal) {
    String seed =
        cal.getName() + "|" + e.getSubject() + "|" + e.getStartDateTime() + "|"
            + e.getEndDateTime();
    return UUID.nameUUIDFromBytes(seed.getBytes()).toString() + "@pdp-calendar";
  }

  private static String escape(String s) {
    String x = s.replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace(",", "\\,")
        .replace(";", "\\;");
    return fold75(x);
  }

  private static String fold75(String s) {
    if (s.length() <= 75) {
      return s;
    }
    StringBuilder b = new StringBuilder();
    int i = 0;
    while (i < s.length()) {
      int end = Math.min(i + 75, s.length());
      b.append(s, i, end);
      i = end;
      if (i < s.length()) {
        b.append("\r\n ");
      }
    }
    return b.toString();
  }
}
