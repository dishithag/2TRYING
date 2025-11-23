package calendar.view.gui;

import java.time.ZoneId;
import java.util.Objects;

/**
 * Lightweight description of a calendar for list rendering.
 */
public final class CalendarSummary {
  private final String name;
  private final ZoneId zoneId;
  private final String colorHex;

  /**
   * Creates a summary.
   *
   * @param name calendar name
   * @param zoneId calendar zone
   * @param colorHex display color
   */
  public CalendarSummary(String name, ZoneId zoneId, String colorHex) {
    this.name = Objects.requireNonNull(name, "name");
    this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    this.colorHex = Objects.requireNonNull(colorHex, "colorHex");
  }

  public String getName() {
    return name;
  }

  public ZoneId getZoneId() {
    return zoneId;
  }

  public String getColorHex() {
    return colorHex;
  }
}
