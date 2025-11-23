package calendar.controller;

import calendar.CalendarBook;
import calendar.view.CalendarView;
import java.io.Reader;

/**
 * Headless (script) mode controller.
 */
public class HeadlessController extends AbstractCalendarController implements CalendarController {

  /**
   * Creates a headless controller.
   *
   * @param book calendar book
   * @param view view
   * @param input script reader
   */
  public HeadlessController(CalendarBook book, CalendarView view, Reader input) {
    super(book, view, input);
  }

  @Override
  protected void displayWelcome() {
    // no-op for headless
  }

  @Override
  protected void showPrompt() {
    // no prompt in headless mode
  }

  @Override
  protected void handleExit() {
    // no-op for headless
  }

  @Override
  protected void handleError(String message, int lineNumber) {
    view.displayError("Line " + lineNumber + ": " + message);
  }

  @Override
  protected void handleMissingExit() {
    view.displayError("Script ended without 'exit'.");
  }
}
