package calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Registry of named calendars. Ensures unique names and manages their time zones.
 */
public interface CalendarBook {

  /**
   * Creates a new calendar with the given unique name and time zone.
   *
   * @param name unique calendar name
   * @param zone time zone for the calendar
   * @return the created calendar
   * @throws IllegalArgumentException if name exists or args are null
   */
  Calendar createCalendar(String name, ZoneId zone);

  /**
   * Renames an existing calendar.
   *
   * @param oldName existing calendar name
   * @param newName new unique name
   * @throws IllegalArgumentException if oldName missing or newName exists
   */
  void renameCalendar(String oldName, String newName);

  /**
   * Changes the time zone of a calendar.
   *
   * @param name calendar name
   * @param newZone new time zone
   * @throws IllegalArgumentException if name missing or zone null
   */
  void changeTimezone(String name, ZoneId newZone);

  /**
   * Returns the calendar with the given name.
   *
   * @param name calendar name
   * @return the calendar
   * @throws IllegalArgumentException if missing
   */
  Calendar getCalendar(String name);

  /**
   * Lists all calendar names in sorted order.
   *
   * @return names
   */
  List<String> listCalendarNames();

  /**
   * Whether a calendar exists.
   *
   * @param name calendar name
   * @return true if present
   */
  boolean hasCalendar(String name);

  /**
   * Copies a single event identified by subject and start from the source calendar
   * to a target start in the target calendar.
   *
   * @param sourceCalendar source calendar name
   * @param targetCalendar target calendar name
   * @param sourceStart start date-time in the source calendar
   * @param subject event subject
   * @param targetStart start date-time in the target calendar
   * @throws IllegalArgumentException if the source event is ambiguous or missing
   */
  void copyEvent(String sourceCalendar, String targetCalendar,
                 LocalDateTime sourceStart, String subject, LocalDateTime targetStart);

  /**
   * Copies all events scheduled on the given source date to the target date in the target calendar.
   * Times are converted to the target calendar's time zone while keeping wall-clock times aligned.
   *
   * @param sourceCalendar source calendar name
   * @param targetCalendar target calendar name
   * @param sourceDate source date
   * @param targetDate target date
   */
  void copyEventsOnDate(String sourceCalendar, String targetCalendar,
                        LocalDate sourceDate, LocalDate targetDate);

  /**
   * Copies all events overlapping the inclusive source date range to a range anchored
   * at the target start date in the target calendar. Series membership is retained.
   *
   * @param sourceCalendar source calendar name
   * @param targetCalendar target calendar name
   * @param startInclusive inclusive start date
   * @param endInclusive inclusive end date
   * @param targetStart start date for the pasted range in the target calendar
   */
  void copyEventsBetween(String sourceCalendar, String targetCalendar,
                         LocalDate startInclusive, LocalDate endInclusive,
                         LocalDate targetStart);
}
