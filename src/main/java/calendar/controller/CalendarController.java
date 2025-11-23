package calendar.controller;


/**
 * Interface for calendar controllers.
 * Controller will handle user input and coordinate between model and view.
 */
public interface CalendarController {

  /**
   * Starts the controller and handles user interaction.
   * Returns when user exits or end of input is reached.
   */
  void run();
}