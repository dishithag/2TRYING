package calendar.view.gui;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Identifier for an event used by the GUI layer.
 */
public final class EventReference {
  private final String subject;
  private final LocalDateTime start;

  /**
   * Creates a reference to an event.
   *
   * @param subject event subject
   * @param start start date-time
   */
  public EventReference(String subject, LocalDateTime start) {
    this.subject = Objects.requireNonNull(subject, "subject");
    this.start = Objects.requireNonNull(start, "start");
  }

  public String getSubject() {
    return subject;
  }

  public LocalDateTime getStart() {
    return start;
  }
}
