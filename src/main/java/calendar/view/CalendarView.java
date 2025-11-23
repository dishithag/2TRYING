package calendar.view;

import calendar.Event;
import java.util.List;

/**
 * Represents an interface for calendar views.
 */
public interface CalendarView {

  /**
   * Displays a list of events.
   *
   * @param events the events to display
   */
  void displayEvents(List<Event> events);

  /**
   * Displays a message to the user.
   *
   * @param message the message to display
   */
  void displayMessage(String message);

  /**
   * Displays an error message to the user.
   *
   * @param error the error message to be displayed
   */
  void displayError(String error);

  /**
   * Displays the busy/available status.
   *
   * @param isBusy true if busy, false if available
   */
  void displayStatus(boolean isBusy);

  /**
   * Displays an input prompt to the user.
   */
  void displayPrompt();
}
