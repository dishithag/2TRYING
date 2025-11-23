package calendar.view.gui;

import calendar.EventProperty;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Payload describing an update to an event property.
 */
public final class EventUpdate {
  private final EventProperty property;
  private final LocalDateTime newDateTime;
  private final String newValue;

  /**
   * Creates an update for the given property.
   *
   * @param property target property
   * @param newDateTime replacement date-time for start/end edits
   * @param newValue replacement text for other properties
   */
  public EventUpdate(EventProperty property, LocalDateTime newDateTime, String newValue) {
    this.property = Objects.requireNonNull(property, "property");
    this.newDateTime = newDateTime;
    this.newValue = newValue;
  }

  public EventProperty getProperty() {
    return property;
  }

  public LocalDateTime getNewDateTime() {
    return newDateTime;
  }

  public String getNewValue() {
    return newValue;
  }
}
