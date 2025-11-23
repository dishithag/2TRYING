package calendar.controller;

/**
 * Scope for edit operations.
 */
public enum EditScope {
  EVENT,
  EVENTS,
  SERIES;

  /**
   * Parses a case-insensitive edit-scope token into an {@link EditScope}.
   *
   * <p>Leading/trailing whitespace is ignored. Valid tokens are:
   * <ul>
   *   <li>{@code "event"}  → {@link #EVENT}</li>
   *   <li>{@code "events"} → {@link #EVENTS}</li>
   *   <li>{@code "series"} → {@link #SERIES}</li>
   * </ul>
   *
   * @param token textual scope specifier (e.g., {@code "event"}, {@code "events"}, etc.)
   * @return the matching {@code EditScope} constant
   * @throws IllegalArgumentException if {@code token} is {@code null} or not a recognized scope
   */

  public static EditScope fromToken(String token) {
    if (token == null) {
      throw new IllegalArgumentException("Edit scope required");
    }
    switch (token.trim().toLowerCase()) {
      case "event": return EVENT;
      case "events": return EVENTS;
      case "series": return SERIES;
      default: throw new IllegalArgumentException("Unknown edit scope: " + token);
    }
  }
}
