package calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fluent builder for {@link Event} instances.
 * Supports convenient defaults (e.g., all-day using {@link WorkingHours}).
 */
public class EventBuilder {
  private String subject;
  private LocalDateTime start;
  private LocalDateTime end;
  private String description = "";
  private String location = "";
  private boolean isPublic = true;
  private String seriesId;

  /**
   * Sets the event subject.
   *
   * @param s subject text
   * @return this builder
   */
  public EventBuilder subject(String s) {
    this.subject = s;
    return this;
  }

  /**
   * Sets the start date/time.
   *
   * @param s start date/time
   * @return this builder
   */
  public EventBuilder startDateTime(LocalDateTime s) {
    this.start = s;
    return this;
  }

  /**
   * Sets the end date/time.
   *
   * @param e end date/time
   * @return this builder
   */
  public EventBuilder endDateTime(LocalDateTime e) {
    this.end = e;
    return this;
  }

  /**
   * Sets the description.
   *
   * @param d description text
   * @return this builder
   */
  public EventBuilder description(String d) {
    this.description = d;
    return this;
  }

  /**
   * Sets the location.
   *
   * @param l location text
   * @return this builder
   */
  public EventBuilder location(String l) {
    this.location = l;
    return this;
  }

  /**
   * Sets the public visibility flag.
   *
   * @param v true for public, false for private
   * @return this builder
   */
  public EventBuilder isPublic(boolean v) {
    this.isPublic = v;
    return this;
  }

  /**
   * Sets the series identifier.
   *
   * @param id series id
   * @return this builder
   */
  public EventBuilder seriesId(String id) {
    this.seriesId = id;
    return this;
  }

  /**
   * Marks the event as all-day on the given date using {@link WorkingHours#START}
   * and {@link WorkingHours#END}.
   *
   * @param date calendar date
   * @return this builder
   */
  public EventBuilder allDay(LocalDate date) {
    this.start = date.atTime(WorkingHours.START);
    this.end = date.atTime(WorkingHours.END);
    return this;
  }

  /**
   * Builds the {@link Event}.
   * Defaults the end time to {@link WorkingHours#END} on the start date when no end is provided.
   *
   * @return a new {@link Event}
   * @throws IllegalArgumentException if required fields are missing or invalid
   */
  public Event build() {
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("Subject required");
    }
    if (start == null) {
      throw new IllegalArgumentException("Start date/time required");
    }

    LocalDateTime finalEnd = (end == null)
        ? start.toLocalDate().atTime(WorkingHours.END)
        : end;

    if (finalEnd.isBefore(start)) {
      throw new IllegalArgumentException("End date/time before start");
    }

    return new SingleEvent(subject, start, finalEnd, description, location, isPublic, seriesId);
  }

  /**
   * Creates a builder prepopulated from an existing event.
   *
   * @param e source event
   * @return a builder initialized with the event's fields
   */
  public static EventBuilder from(Event e) {
    EventBuilder b = new EventBuilder()
        .subject(e.getSubject())
        .startDateTime(e.getStartDateTime())
        .endDateTime(e.getEndDateTime())
        .isPublic(e.isPublic());
    b.description(e.getDescription().orElse(""));
    b.location(e.getLocation().orElse(""));
    e.getSeriesId().ifPresent(b::seriesId);
    return b;
  }
}
