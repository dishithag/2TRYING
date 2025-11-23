package calendar.view;

import calendar.Event;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Text-based implementation of the CalendarView interface.
 */
public class TextCalendarView implements CalendarView {

  private final PrintStream out;
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm");

  /**
   * Constructs a text view that writes to the given output stream.
   *
   * @param out the output stream to write to
   */
  public TextCalendarView(PrintStream out) {
    if (out == null) {
      throw new IllegalArgumentException("Output stream cannot be null");
    }
    this.out = out;
  }

  @Override
  public void displayEvents(List<Event> events) {
    if (events == null || events.isEmpty()) {
      out.println("No events found.");
      return;
    }
    for (Event event : events) {
      out.println("• " + formatEvent(event));
    }
  }

  @Override
  public void displayMessage(String message) {
    out.println(message);
  }

  @Override
  public void displayError(String error) {
    out.println("Error: " + error);
  }

  @Override
  public void displayStatus(boolean isBusy) {
    out.println(isBusy ? "busy" : "available");
  }

  @Override
  public void displayPrompt() {
    out.print("> ");
  }

  private String formatEvent(Event event) {
    StringBuilder sb = new StringBuilder();
    sb.append(event.getSubject());
    sb.append(" starting on ");
    sb.append(event.getStartDateTime().format(DATE_FORMATTER));
    sb.append(" at ");
    sb.append(event.getStartDateTime().format(TIME_FORMATTER));
    sb.append(", ending on ");
    sb.append(event.getEndDateTime().format(DATE_FORMATTER));
    sb.append(" at ");
    sb.append(event.getEndDateTime().format(TIME_FORMATTER));
    event.getLocation().ifPresent(loc -> sb.append(" at ").append(loc));
    return sb.toString();
  }
}
