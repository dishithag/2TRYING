package calendar.view.gui;

/**
 * GUI-facing view contract used by the controller.
 */
public interface GuiView {

  /**
   * Registers controller callbacks.
   *
   * @param features controller feature set
   */
  void setFeatures(GuiFeatures features);

  /**
   * Applies a new snapshot of model state to the GUI.
   *
   * @param state immutable view state
   */
  void render(CalendarUiState state);

  /**
   * Displays a user-facing message.
   *
   * @param message text to show
   */
  void showMessage(String message);

  /**
   * Displays an error message.
   *
   * @param error text to show
   */
  void showError(String error);

  /**
   * Makes the GUI visible.
   */
  void start();
}
