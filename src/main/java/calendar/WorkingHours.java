package calendar;

import java.time.LocalTime;

/**
 * Canonical working hours used across the app for “all-day” defaults
 * and any normalization of times.
 */
public final class WorkingHours {
  public static final LocalTime START = LocalTime.of(8, 0);
  public static final LocalTime END = LocalTime.of(17, 0);

  private WorkingHours() {}
}
