package calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Calendar operations for creating, finding, editing, querying, and exporting events.
 * All LocalDateTime values passed to or returned from this interface are interpreted
 * in this calendar's ZoneId. A calendar has a unique name within a multi-calendar context.
 */
public interface Calendar {

  /**
   * Calendar name.
   *
   * @return name
   */
  String getName();

  /**
   * Rename the calendar.
   *
   * @param newName new unique name
   */
  void rename(String newName);

  /**
   * Time zone of this calendar.
   *
   * @return zone id
   */
  ZoneId getZoneId();

  /**
   * Set the time zone of this calendar.
   *
   * @param zone zone id
   */
  void setZoneId(ZoneId zone);

  /**
   * Create a single event.
   *
   * @param subject subject
   * @param start   start in this calendar's zone
   * @param end     end in this calendar's zone
   * @return created event
   */
  Event createEvent(String subject, LocalDateTime start, LocalDateTime end);

  /**
   * Create a repeating event series with a fixed occurrence count.
   *
   * @param subject     subject
   * @param start       local start for each occurrence
   * @param end         local end for each occurrence
   * @param weekdays    repeating weekdays
   * @param occurrences number of occurrences
   * @return created events
   */
  List<Event> createEventSeries(String subject,
                                LocalDateTime start,
                                LocalDateTime end,
                                Set<DayOfWeek> weekdays,
                                int occurrences);

  /**
   * Create a repeating event series until an inclusive end date.
   *
   * @param subject  subject
   * @param start    local start for each occurrence
   * @param end      local end for each occurrence
   * @param weekdays repeating weekdays
   * @param endDate  inclusive series end date
   * @return created events
   */
  List<Event> createEventSeriesUntil(String subject,
                                     LocalDateTime start,
                                     LocalDateTime end,
                                     Set<DayOfWeek> weekdays,
                                     LocalDate endDate);

  /**
   * Find events matching subject and exact start.
   *
   * @param subject subject
   * @param start   exact start
   * @return matches
   */
  List<Event> findEvents(String subject, LocalDateTime start);

  /**
   * Find events matching subject and exact start+end.
   *
   * @param subject subject
   * @param start   exact start
   * @param end     exact end
   * @return matches
   */
  List<Event> findEvents(String subject, LocalDateTime start, LocalDateTime end);

  /**
   * Typed edit of a single event. For START/END, supply {@code newDateTime}; for other
   * properties, supply {@code newText}. Exactly one of the two must be non-null.
   *
   * @param subject subject
   * @param start   event start to identify the instance
   * @param property mapped property token
   * @param newDateTime new start or end when editing time fields
   * @param newText new textual value for non-time fields
   */
  void editEvent(String subject, LocalDateTime start,
                 EventProperty property, LocalDateTime newDateTime, String newText);

  /**
   * Edit a single matching event instance using CLI-friendly strings.
   *
   * @param subject  subject
   * @param start    start of the event to identify
   * @param property property name
   * @param newValue new value as string
   */
  void editEvent(String subject, LocalDateTime start, String property, String newValue);

  /**
   * Typed bulk edit from a given start (inclusive) within a series.
   *
   * @param subject subject
   * @param start pivot start within the series
   * @param property mapped property token
   * @param newDateTime new start or end when editing time fields
   * @param newText new textual value for non-time fields
   */
  void editEventsFromDate(String subject, LocalDateTime start,
                          EventProperty property, LocalDateTime newDateTime, String newText);

  /**
   * Edit all events in a series from the given start (inclusive) using CLI-friendly strings.
   *
   * @param subject  subject
   * @param start    pivot start
   * @param property property name
   * @param newValue new value as string
   */
  void editEventsFromDate(String subject, LocalDateTime start, String property, String newValue);

  /**
   * Typed edit of the entire series that contains the given start.
   *
   * @param subject subject
   * @param start any start within the series
   * @param property mapped property token
   * @param newDateTime new start or end when editing time fields
   * @param newText new textual value for non-time fields
   */
  void editSeries(String subject, LocalDateTime start,
                  EventProperty property, LocalDateTime newDateTime, String newText);

  /**
   * Edit all events in the series containing the given start using CLI-friendly strings.
   *
   * @param subject  subject
   * @param start    any event start in the series
   * @param property property name
   * @param newValue new value as string
   */
  void editSeries(String subject, LocalDateTime start, String property, String newValue);

  /**
   * Events scheduled on the given date in this calendar's zone.
   *
   * @param date local date
   * @return events on that date
   */
  List<Event> getEventsOnDate(LocalDate date);

  /**
   * Events overlapping the given range [start, end] in this calendar's zone.
   *
   * @param start range start
   * @param end   range end
   * @return overlapping events
   */
  List<Event> getEventsInRange(LocalDateTime start, LocalDateTime end);

  /**
   * Whether any event covers the given moment in this calendar's zone.
   *
   * @param dateTime local date-time
   * @return busy status
   */
  boolean isBusyAt(LocalDateTime dateTime);

  /**
   * Snapshot of all events.
   *
   * @return all events
   */
  List<Event> getAllEvents();

  /**
   * Creates a new event in this calendar by copying a template event and
   * placing it at the given start/end. If the template is part of a series,
   * the copied event retains that series membership.
   *
   * @param template source event
   * @param newStart start in this calendar's zone
   * @param newEnd   end in this calendar's zone
   * @return the created event
   */
  Event copyFrom(Event template, LocalDateTime newStart, LocalDateTime newEnd);
}
