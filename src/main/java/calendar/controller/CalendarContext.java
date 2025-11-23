package calendar.controller;

import calendar.Calendar;
import calendar.CalendarBook;
import java.util.Objects;

/**
 * Holds the active calendar selection for controllers using a CalendarBook.
 * Keeps calendar selection logic in one place.
 */
public final class CalendarContext {

  private final CalendarBook book;
  private String currentName;

  /**
   * Creates a context with the given book and no active calendar.
   *
   * @param book calendar book
   */
  public CalendarContext(CalendarBook book) {
    this.book = Objects.requireNonNull(book, "book");
  }

  /**
   * Sets the active calendar by name.
   *
   * @param name calendar name
   */
  public void use(String name) {
    Objects.requireNonNull(name, "name");
    if (!book.hasCalendar(name)) {
      throw new IllegalArgumentException("No such calendar: " + name);
    }
    this.currentName = name;
  }

  /**
   * Returns the active calendar or throws if none is selected.
   *
   * @return current calendar
   */
  public Calendar current() {
    if (currentName == null) {
      throw new IllegalStateException("No calendar in use. Use: use calendar --name <name>");
    }
    return book.getCalendar(currentName);
  }

  /**
   * Returns the name of the active calendar, or null if none.
   *
   * @return current calendar name
   */
  public String currentName() {
    return currentName;
  }
}
