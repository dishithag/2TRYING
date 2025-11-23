package calendar;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * A calendar event with subject, start/end, optional description/location/status,
 * and optional series membership. Times are interpreted in the owning calendar's zone.
 * Two events are identical if they share subject, start and end.
 */
public interface Event {

  /**
   * Subject of the event.
   *
   * @return subject
   */
  String getSubject();

  /**
   * Start date-time of the event.
   *
   * @return start
   */
  LocalDateTime getStartDateTime();

  /**
   * End date-time of the event.
   *
   * @return end
   */
  LocalDateTime getEndDateTime();

  /**
   * Optional description of the event.
   *
   * @return description if present
   */
  Optional<String> getDescription();

  /**
   * Optional location of the event.
   *
   * @return location if present
   */
  Optional<String> getLocation();

  /**
   * Whether this is an all-day event (8:00–17:00).
   *
   * @return true if all-day
   */
  boolean isAllDayEvent();

  /**
   * Whether the event is public.
   *
   * @return true if public, false if private
   */
  boolean isPublic();

  /**
   * Whether this event is part of a series.
   *
   * @return true if part of a series
   */
  boolean isSeriesPart();

  /**
   * Series identifier when part of a series.
   *
   * @return series id if present
   */
  Optional<String> getSeriesId();
}
