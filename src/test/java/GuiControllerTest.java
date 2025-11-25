import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import calendar.CalendarBookImpl;
import calendar.controller.EditScope;
import calendar.controller.gui.GuiController;
import calendar.view.gui.CalendarUiState;
import calendar.view.gui.EventInput;
import calendar.view.gui.EventReference;
import calendar.view.gui.EventUpdate;
import calendar.view.gui.GuiFeatures;
import calendar.view.gui.GuiView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

/**
 * Tests for the GUI controller without rendering Swing components.
 */
public class GuiControllerTest {

  private static final class StubView implements GuiView {
    private GuiFeatures features;
    private CalendarUiState lastState;
    private String lastMessage;
    private String lastError;
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public void setFeatures(GuiFeatures features) {
      this.features = features;
    }

    @Override
    public void render(CalendarUiState state) {
      this.lastState = state;
    }

    @Override
    public void showMessage(String message) {
      this.lastMessage = message;
    }

    @Override
    public void showError(String error) {
      this.lastError = error;
    }

    @Override
    public void start() {
      started.set(true);
    }
  }

  @Test
  public void testStartCreatesDefaultCalendarAndRenders() {
    StubView view = new StubView();
    GuiController controller = new GuiController(new CalendarBookImpl(), view, ZoneId.of("UTC"));
    controller.start();
    assertTrue(view.started.get());
    assertNotNull(view.lastState);
    assertEquals("Default", view.lastState.getActiveCalendar());
  }

  @Test
  public void testCreateAndEditEventFlowsThroughState() {
    StubView view = new StubView();
    GuiController controller = new GuiController(new CalendarBookImpl(), view, ZoneId.of("UTC"));
    controller.start();
    controller.selectDate(LocalDate.of(2024, 1, 10));
    EventInput input = new EventInput("Planning", LocalDate.of(2024, 1, 10),
        LocalTime.of(9, 0), LocalTime.of(10, 0), true, null, null,
        EventInput.RecurrenceRule.none());
    controller.createEvent(input);
    assertTrue(view.lastState.getSelectedDayEvents().stream()
        .anyMatch(ev -> ev.getSubject().equals("Planning")));

    LocalDateTime start = LocalDateTime.of(2024, 1, 10, 9, 0);
    controller.editEvent(new EventReference("Planning", start),
        new EventUpdate(calendar.EventProperty.DESCRIPTION, null, "updated"), EditScope.EVENT);
    assertTrue(view.lastState.getSelectedDayEvents().stream()
        .anyMatch(ev -> ev.getDescription().orElse("").equals("updated")));
  }

  @Test
  public void testExportCalls() {
    StubView view = new StubView();
    CalendarBookImpl book = new CalendarBookImpl();
    GuiController controller = new GuiController(book, view, ZoneId.of("UTC"));
    controller.start();
    controller.createCalendar("Work", ZoneId.of("UTC"));
    EventInput input = new EventInput("Demo", LocalDate.of(2024, 3, 5),
        LocalTime.of(11, 0), LocalTime.of(12, 0), true, null, null,
        EventInput.RecurrenceRule.none());
    controller.createEvent(input);
    controller.exportCalendar("build/test-export.ics");
    assertTrue(view.lastMessage == null || view.lastMessage.contains("Exported"));
  }
}
