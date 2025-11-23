package calendar;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of CalendarBook.
 */
public class CalendarBookImpl implements CalendarBook {

  private final Map<String, Calendar> calendars = new ConcurrentHashMap<>();

  @Override
  public Calendar createCalendar(String name, ZoneId zone) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(zone, "zone");
    if (calendars.containsKey(name)) {
      throw new IllegalArgumentException("Calendar already exists: " + name);
    }
    Calendar cal = new CalendarImpl(name, zone);
    calendars.put(name, cal);
    return cal;
  }

  @Override
  public void renameCalendar(String oldName, String newName) {
    Objects.requireNonNull(oldName, "oldName");
    Objects.requireNonNull(newName, "newName");
    Calendar cal = calendars.remove(oldName);
    if (cal == null) {
      throw new IllegalArgumentException("No such calendar: " + oldName);
    }
    if (calendars.containsKey(newName)) {
      calendars.put(oldName, cal);
      throw new IllegalArgumentException("Calendar already exists: " + newName);
    }
    cal.rename(newName);
    calendars.put(newName, cal);
  }

  @Override
  public void changeTimezone(String name, ZoneId newZone) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(newZone, "zone");
    Calendar cal = calendars.get(name);
    if (cal == null) {
      throw new IllegalArgumentException("No such calendar: " + name);
    }
    cal.setZoneId(newZone);
  }

  @Override
  public Calendar getCalendar(String name) {
    Objects.requireNonNull(name, "name");
    Calendar cal = calendars.get(name);
    if (cal == null) {
      throw new IllegalArgumentException("No such calendar: " + name);
    }
    return cal;
  }

  @Override
  public List<String> listCalendarNames() {
    return Collections.unmodifiableList(
        calendars.keySet().stream().sorted().collect(Collectors.toList()));
  }

  @Override
  public boolean hasCalendar(String name) {
    return calendars.containsKey(name);
  }

  @Override
  public void copyEvent(String sourceCalendar, String targetCalendar,
                        LocalDateTime sourceStart, String subject, LocalDateTime targetStart) {
    Calendar src = getCalendar(sourceCalendar);
    Calendar dst = getCalendar(targetCalendar);

    List<Event> matches = src.findEvents(subject, sourceStart, null);
    if (matches.isEmpty()) {
      throw new IllegalArgumentException("No matching event to copy");
    }
    if (matches.size() > 1) {
      throw new IllegalArgumentException("Ambiguous event to copy");
    }

    Event e = matches.get(0);
    LocalDateTime newStart = targetStart;
    LocalDateTime newEnd = newStart.plus(durationOf(e));
    dst.copyFrom(e, newStart, newEnd);
  }

  @Override
  public void copyEventsOnDate(String sourceCalendar, String targetCalendar,
                               LocalDate sourceDate, LocalDate targetDate) {
    Calendar src = getCalendar(sourceCalendar);
    Calendar dst = getCalendar(targetCalendar);

    List<Event> dayEvents = src.getEventsOnDate(sourceDate);
    ZoneId srcZone = src.getZoneId();
    ZoneId dstZone = dst.getZoneId();

    for (Event e : dayEvents) {
      LocalDateTime projectedStartLocal = projectInstantToZone(e.getStartDateTime(),
          srcZone, dstZone);
      LocalDateTime newStart = targetDate.atTime(projectedStartLocal.toLocalTime());
      LocalDateTime newEnd = newStart.plus(durationOf(e));
      dst.copyFrom(e, newStart, newEnd);
    }
  }

  @Override
  public void copyEventsBetween(String sourceCalendar, String targetCalendar,
                                LocalDate startInclusive, LocalDate endInclusive,
                                LocalDate targetStart) {
    Calendar src = getCalendar(sourceCalendar);
    Calendar dst = getCalendar(targetCalendar);

    LocalDateTime srcStart = startInclusive.atStartOfDay();
    LocalDateTime srcEnd = endInclusive.plusDays(1).atStartOfDay().minusSeconds(1);

    List<Event> inRange = src.getEventsInRange(srcStart, srcEnd);
    ZoneId srcZone = src.getZoneId();
    ZoneId dstZone = dst.getZoneId();

    for (Event e : inRange) {
      long dayOffset = Duration.between(
          startInclusive.atStartOfDay(), e.getStartDateTime().toLocalDate().atStartOfDay()
      ).toDays();

      LocalDateTime projectedStartLocal = projectInstantToZone(e.getStartDateTime(),
          srcZone, dstZone);
      LocalDate targetDay = targetStart.plusDays(dayOffset);
      LocalDateTime newStart = targetDay.atTime(projectedStartLocal.toLocalTime());
      LocalDateTime newEnd = newStart.plus(durationOf(e));
      dst.copyFrom(e, newStart, newEnd);
    }
  }

  private static Duration durationOf(Event e) {
    return Duration.between(e.getStartDateTime(), e.getEndDateTime());
  }

  private static LocalDateTime projectInstantToZone(LocalDateTime dt, ZoneId from, ZoneId to) {
    ZonedDateTime z = dt.atZone(from).withZoneSameInstant(to);
    return z.toLocalDateTime();
  }
}
