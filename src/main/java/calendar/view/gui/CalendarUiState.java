package calendar.view.gui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot of the data needed to render the GUI.
 */
public final class CalendarUiState {
  private final List<CalendarSummary> calendars;
  private final String activeCalendar;
  private final ZoneId activeZone;
  private final YearMonth visibleMonth;
  private final LocalDate selectedDate;
  private final Map<LocalDate, List<EventViewModel>> eventsByDate;
  private final List<EventViewModel> selectedDayEvents;

  /**
   * Creates a state snapshot.
   *
   * @param calendars available calendars
   * @param activeCalendar active calendar name
   * @param activeZone time zone of active calendar
   * @param visibleMonth month being shown
   * @param selectedDate selected date
   * @param eventsByDate events grouped by date for the visible month
   * @param selectedDayEvents events on the selected date
   */
  public CalendarUiState(List<CalendarSummary> calendars, String activeCalendar, ZoneId activeZone,
                         YearMonth visibleMonth, LocalDate selectedDate,
                         Map<LocalDate, List<EventViewModel>> eventsByDate,
                         List<EventViewModel> selectedDayEvents) {
    this.calendars = Collections.unmodifiableList(Objects.requireNonNull(calendars, "calendars"));
    this.activeCalendar = Objects.requireNonNull(activeCalendar, "activeCalendar");
    this.activeZone = Objects.requireNonNull(activeZone, "activeZone");
    this.visibleMonth = Objects.requireNonNull(visibleMonth, "visibleMonth");
    this.selectedDate = Objects.requireNonNull(selectedDate, "selectedDate");
    this.eventsByDate = Collections.unmodifiableMap(Objects.requireNonNull(eventsByDate, "events"));
    this.selectedDayEvents = Collections.unmodifiableList(
        Objects.requireNonNull(selectedDayEvents, "selectedDayEvents"));
  }

  public List<CalendarSummary> getCalendars() {
    return calendars;
  }

  public String getActiveCalendar() {
    return activeCalendar;
  }

  public ZoneId getActiveZone() {
    return activeZone;
  }

  public YearMonth getVisibleMonth() {
    return visibleMonth;
  }

  public LocalDate getSelectedDate() {
    return selectedDate;
  }

  public Map<LocalDate, List<EventViewModel>> getEventsByDate() {
    return eventsByDate;
  }

  public List<EventViewModel> getSelectedDayEvents() {
    return selectedDayEvents;
  }
}
