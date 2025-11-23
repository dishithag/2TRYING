package calendar.view.gui;

import calendar.controller.EditScope;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Actions exposed by the controller for the GUI to invoke.
 */
public interface GuiFeatures {

  /**
   * Initializes the GUI with the current model state.
   */
  void start();

  /**
   * Selects the active calendar by name.
   *
   * @param calendarName name of the calendar to activate
   */
  void selectCalendar(String calendarName);

  /**
   * Creates a calendar.
   *
   * @param name calendar name
   * @param zone time zone
   */
  void createCalendar(String name, ZoneId zone);

  /**
   * Renames the current calendar.
   *
   * @param newName new unique name
   */
  void renameActiveCalendar(String newName);

  /**
   * Changes the time zone of the active calendar.
   *
   * @param zone new zone id
   */
  void changeActiveCalendarZone(ZoneId zone);

  /**
   * Navigates to the specified month.
   *
   * @param month month to show
   */
  void goToMonth(YearMonth month);

  /**
   * Selects a date for viewing and editing.
   *
   * @param date date in the active calendar zone
   */
  void selectDate(LocalDate date);

  /**
   * Creates an event using the provided input.
   *
   * @param input event definition
   */
  void createEvent(EventInput input);

  /**
   * Edits an event according to the requested scope.
   *
   * @param reference identifier for the event
   * @param update new values to apply
   * @param scope edit scope
   */
  void editEvent(EventReference reference, EventUpdate update, EditScope scope);

  /**
   * Copies a single event to a target calendar and start time.
   *
   * @param reference event identifier in the active calendar
   * @param targetCalendar destination calendar
   * @param targetStart start date-time in the destination calendar
   */
  void copyEvent(EventReference reference, String targetCalendar, LocalDateTime targetStart);

  /**
   * Copies all events on the given date to the target calendar date.
   *
   * @param sourceDate source date in the active calendar
   * @param targetCalendar destination calendar name
   * @param targetDate target date in the destination calendar
   */
  void copyEventsOn(LocalDate sourceDate, String targetCalendar, LocalDate targetDate);

  /**
   * Copies all events within the date range to a target calendar range anchored at the start.
   *
   * @param start inclusive start
   * @param end inclusive end
   * @param targetCalendar destination calendar name
   * @param targetStart first day of the destination range
   */
  void copyEventsBetween(LocalDate start, LocalDate end, String targetCalendar,
                         LocalDate targetStart);

  /**
   * Exports the active calendar to the given file path.
   *
   * @param filePath path whose extension determines format
   */
  void exportCalendar(String filePath);
}
