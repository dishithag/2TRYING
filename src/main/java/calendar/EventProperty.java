package calendar;

import java.time.LocalDateTime;

/**
 * Editable properties of an event.
 */
public enum EventProperty {
  SUBJECT,
  START,
  END,
  DESCRIPTION,
  LOCATION,
  STATUS;

  /**
   * Case-insensitive parser for property tokens.
   *
   * @param token property name such as "subject", "start", "end"
   * @return matching EventProperty
   */
  public static EventProperty fromToken(String token) {
    if (token == null) {
      throw new IllegalArgumentException("Property cannot be null");
    }
    switch (token.trim().toLowerCase()) {
      case "subject": return SUBJECT;
      case "start": return START;
      case "end": return END;
      case "description": return DESCRIPTION;
      case "location": return LOCATION;
      case "status": return STATUS;
      default: throw new IllegalArgumentException("Unknown property: " + token);
    }
  }

  /**
   * Applies a string value to the given builder for this property.
   * For START/END, {@code value} must be an ISO-8601 local date-time
   * in the form {@code yyyy-MM-dd'T'HH:mm}.
   *
   * @param b builder to mutate
   * @param value new value in string form
   * @return the same builder for chaining
   */
  public EventBuilder apply(EventBuilder b, String value) {
    switch (this) {
      case SUBJECT:
        return b.subject(value);
      case START:
        return b.startDateTime(LocalDateTime.parse(value));
      case END:
        return b.endDateTime(LocalDateTime.parse(value));
      case DESCRIPTION:
        return b.description(value);
      case LOCATION:
        return b.location(value);
      case STATUS:
        return b.isPublic("public".equalsIgnoreCase(value));
      default:
        throw new IllegalArgumentException("Unsupported property");
    }
  }
}
