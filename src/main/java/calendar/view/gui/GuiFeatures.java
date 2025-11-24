package calendar.view.gui;

import calendar.controller.EditScope;
import java.time.LocalDate;
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
   * Exports the active calendar to the given file path.
   *
   * @param filePath path whose extension determines format
   */
  void exportCalendar(String filePath);
}
