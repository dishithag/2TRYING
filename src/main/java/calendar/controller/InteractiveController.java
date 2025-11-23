package calendar.controller;

import calendar.CalendarBook;
import calendar.view.CalendarView;
import java.io.Reader;

/**
 * Interactive mode controller.
 */
public class InteractiveController extends AbstractCalendarController
    implements CalendarController {

  /**
   * Creates an interactive controller.
   *
   * @param book  calendar book
   * @param view  view
   * @param input input reader
   */
  public InteractiveController(CalendarBook book, CalendarView view, Reader input) {
    super(book, view, input);
  }

  @Override
  protected void displayWelcome() {
    view.displayMessage("Calendar: interactive mode. Type commands, 'exit' to quit.");
  }

  @Override
  protected void showPrompt() {
    view.displayPrompt();
  }


  @Override
  protected void handleExit() {
    view.displayMessage("Goodbye.");
  }

  @Override
  protected void handleError(String message, int lineNumber) {
    view.displayError("Line " + lineNumber + ": " + message);
  }

  @Override
  protected void handleMissingExit() {
    view.displayError("Input ended without 'exit'.");
  }
}
