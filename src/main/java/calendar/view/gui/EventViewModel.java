package calendar.view.gui;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

/**
 * UI-friendly projection of an event.
 */
public final class EventViewModel {
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

  private final String subject;
  private final LocalDateTime start;
  private final LocalDateTime end;
  private final String location;
  private final String description;
  private final boolean publicEvent;
  private final boolean seriesPart;

  /**
   * Creates an event view model.
   *
   * @param subject subject
   * @param start start time
   * @param end end time
   * @param location optional location
   * @param description optional description
   * @param publicEvent whether public
   * @param seriesPart whether part of a series
   */
  public EventViewModel(String subject, LocalDateTime start, LocalDateTime end,
                        String location, String description,
                        boolean publicEvent, boolean seriesPart) {
    this.subject = Objects.requireNonNull(subject, "subject");
    this.start = Objects.requireNonNull(start, "start");
    this.end = Objects.requireNonNull(end, "end");
    this.location = location;
    this.description = description;
    this.publicEvent = publicEvent;
    this.seriesPart = seriesPart;
  }

  public String getSubject() {
    return subject;
  }

  public LocalDateTime getStart() {
    return start;
  }

  public LocalDateTime getEnd() {
    return end;
  }

  public Optional<String> getLocation() {
    return Optional.ofNullable(location);
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public boolean isPublicEvent() {
    return publicEvent;
  }

  public boolean isSeriesPart() {
    return seriesPart;
  }

  /**
   * Human-readable label for lists.
   *
   * @param zone rendering zone
   * @return formatted label
   */
  public String summary(ZoneId zone) {
    StringBuilder sb = new StringBuilder();
    sb.append(start.atZone(zone).toLocalTime().format(TIME));
    sb.append("-");
    sb.append(end.atZone(zone).toLocalTime().format(TIME));
    sb.append(" ");
    sb.append(subject);
    locationOptional().ifPresent(loc -> sb.append(" @ ").append(loc));
    if (!publicEvent) {
      sb.append(" (private)");
    }
    if (seriesPart) {
      sb.append(" [series]");
    }
    return sb.toString();
  }

  private Optional<String> locationOptional() {
    return Optional.ofNullable(location);
  }
}
