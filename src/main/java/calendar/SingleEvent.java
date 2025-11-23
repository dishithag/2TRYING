package calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Immutable implementation of an {@link Event}.
 * If {@code end} is omitted, the event is treated as an all-day entry
 * using {@link WorkingHours#START} and {@link WorkingHours#END}.
 */
public class SingleEvent implements Event {

  private final String subject;
  private final LocalDateTime startDateTime;
  private final LocalDateTime endDateTime;
  private final String description;
  private final String location;
  private final boolean isPublic;
  private final String seriesId;

  /**
   * Creates an event with optional end; if {@code end} is {@code null},
   * it becomes an all-day event on the start date.
   *
   * @param subject the event subject
   * @param start   start date/time (required)
   * @param end     end date/time, or {@code null} to default to working hours
   * @throws IllegalArgumentException if subject is blank, start is null,
   *                                  or end is before start
   */
  public SingleEvent(String subject, LocalDateTime start, LocalDateTime end) {
    this(subject, start, end, "", "", true, null);
  }

  /**
   * Full constructor used when specifying all optional fields.
   *
   * @param subject   subject text
   * @param start     start date/time (required)
   * @param end       end date/time, or {@code null} to default to working hours
   * @param description description text (may be empty)
   * @param location  location text (may be empty)
   * @param isPublic  visibility flag
   * @param seriesId  series identifier or {@code null} if not part of a series
   * @throws IllegalArgumentException if subject is blank, start is null,
   *                                  or end is before start
   */
  public SingleEvent(String subject, LocalDateTime start, LocalDateTime end,
                     String description, String location, boolean isPublic, String seriesId) {
    if (subject == null || subject.isEmpty()) {
      throw new IllegalArgumentException("Event subject cannot be null or empty");
    }
    if (start == null) {
      throw new IllegalArgumentException("Start date/time cannot be null");
    }

    this.subject = subject;
    this.description = description;
    this.location = location;
    this.isPublic = isPublic;
    this.seriesId = seriesId;

    if (end == null) {
      LocalDate d = start.toLocalDate();
      this.startDateTime = d.atTime(WorkingHours.START);
      this.endDateTime = d.atTime(WorkingHours.END);
    } else {
      if (end.isBefore(start)) {
        throw new IllegalArgumentException("End date/time cannot be before start date/time");
      }
      this.startDateTime = start;
      this.endDateTime = end;
    }
  }

  @Override
  public String getSubject() {
    return subject;
  }

  @Override
  public LocalDateTime getStartDateTime() {
    return startDateTime;
  }

  @Override
  public LocalDateTime getEndDateTime() {
    return endDateTime;
  }

  @Override
  public Optional<String> getDescription() {
    return description.isEmpty() ? Optional.empty() : Optional.of(description);
  }

  @Override
  public Optional<String> getLocation() {
    return location.isEmpty() ? Optional.empty() : Optional.of(location);
  }

  @Override
  public boolean isPublic() {
    return isPublic;
  }

  @Override
  public boolean isAllDayEvent() {
    return startDateTime.toLocalTime().equals(WorkingHours.START)
        && endDateTime.toLocalTime().equals(WorkingHours.END)
        && startDateTime.toLocalDate().equals(endDateTime.toLocalDate());
  }

  @Override
  public boolean isSeriesPart() {
    return seriesId != null;
  }

  @Override
  public Optional<String> getSeriesId() {
    return Optional.ofNullable(seriesId);
  }

  @Override
  public String toString() {
    return String.format("%s starting on %s at %s, ending on %s at %s%s",
        subject,
        startDateTime.toLocalDate(),
        startDateTime.toLocalTime(),
        endDateTime.toLocalDate(),
        endDateTime.toLocalTime(),
        location.isEmpty() ? "" : " at " + location);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Event)) {
      return false;
    }
    Event that = (Event) o;
    return subject.equals(that.getSubject())
        && startDateTime.equals(that.getStartDateTime())
        && endDateTime.equals(that.getEndDateTime());
  }

  @Override
  public int hashCode() {
    int r = subject.hashCode();
    r = 31 * r + startDateTime.hashCode();
    r = 31 * r + endDateTime.hashCode();
    return r;
  }
}
