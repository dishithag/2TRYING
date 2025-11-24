import static org.junit.Assert.assertEquals;

import calendar.view.gui.CalendarFrame;
import calendar.view.gui.CalendarSummary;
import calendar.view.gui.CalendarUiState;
import calendar.view.gui.EventInput;
import calendar.view.gui.EventReference;
import calendar.view.gui.EventUpdate;
import calendar.view.gui.GuiFeatures;
import java.lang.reflect.Field;
import java.awt.GraphicsEnvironment;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import org.junit.Assume;

/**
 * Verifies that clicking a day in the month grid notifies the controller with the chosen date.
 */
public class CalendarFrameSelectDateTest {

  private static final class RecordingFeatures implements GuiFeatures {
    private LocalDate selected;

    @Override
    public void start() {
      // unused
    }

    @Override
    public void selectCalendar(String calendarName) {
      // unused
    }

    @Override
    public void createCalendar(String name, ZoneId zone) {
      // unused
    }

    @Override
    public void renameActiveCalendar(String newName) {
      // unused
    }

    @Override
    public void changeActiveCalendarZone(ZoneId zone) {
      // unused
    }

    @Override
    public void goToMonth(YearMonth month) {
      // unused
    }

    @Override
    public void selectDate(LocalDate date) {
      this.selected = date;
    }

    @Override
    public void createEvent(EventInput input) {
      // unused
    }

    @Override
    public void editEvent(EventReference reference, EventUpdate update, calendar.controller.EditScope scope) {
      // unused
    }

    @Override
    public void exportCalendar(String filePath) {
      // unused
    }
  }

  @Test
  public void clickingDayButtonNotifiesController() throws Exception {
    Assume.assumeFalse(GraphicsEnvironment.isHeadless());

    CalendarFrame frame = new CalendarFrame();
    RecordingFeatures features = new RecordingFeatures();
    frame.setFeatures(features);

    CalendarSummary summary = new CalendarSummary("Default", ZoneId.of("America/New_York"), "#000000");
    CalendarUiState state = new CalendarUiState(List.of(summary), "Default", summary.getZoneId(),
        YearMonth.of(2023, 11), LocalDate.of(2023, 11, 1), Collections.emptyMap(), Collections.emptyList());

    CountDownLatch latch = new CountDownLatch(1);
    SwingUtilities.invokeLater(() -> {
      frame.render(state);
      latch.countDown();
    });
    latch.await();

    Field monthPanelField = CalendarFrame.class.getDeclaredField("monthPanel");
    monthPanelField.setAccessible(true);
    JPanel monthPanel = (JPanel) monthPanelField.get(frame);

    JButton target = null;
    for (var component : monthPanel.getComponents()) {
      if (component instanceof JButton button && "5".equals(button.getText())) {
        target = button;
        break;
      }
    }

    if (target == null) {
      throw new IllegalStateException("Expected day button not found");
    }

    SwingUtilities.invokeAndWait(target::doClick);
    assertEquals(LocalDate.of(2023, 11, 5), features.selected);
  }
}
