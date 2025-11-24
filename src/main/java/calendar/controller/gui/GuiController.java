package calendar.controller.gui;

import calendar.Calendar;
import calendar.CalendarBook;
import calendar.Event;
import calendar.EventProperty;
import calendar.WorkingHours;
import calendar.controller.EditScope;
import calendar.util.ExportUtil;
import calendar.view.gui.CalendarSummary;
import calendar.view.gui.CalendarUiState;
import calendar.view.gui.EventInput;
import calendar.view.gui.EventReference;
import calendar.view.gui.EventUpdate;
import calendar.view.gui.EventViewModel;
import calendar.view.gui.GuiFeatures;
import calendar.view.gui.GuiView;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller coordinating the Swing GUI with the calendar model.
 */
public class GuiController implements GuiFeatures {

  private final CalendarBook calendarBook;
  private final GuiView view;
  private YearMonth visibleMonth;
  private String activeCalendar;
  private LocalDate selectedDate;
  private final ZoneId defaultZone;

  /**
   * Constructs a GUI controller.
   *
   * @param calendarBook backing calendar book
   * @param view swing view
   * @param defaultZone fallback zone for the initial calendar
   */
  public GuiController(CalendarBook calendarBook, GuiView view, ZoneId defaultZone) {
    this.calendarBook = Objects.requireNonNull(calendarBook, "calendarBook");
    this.view = Objects.requireNonNull(view, "view");
    this.defaultZone = Objects.requireNonNull(defaultZone, "defaultZone");
    this.view.setFeatures(this);
  }

  @Override
  public void start() {
    ensureDefaultCalendar();
    if (activeCalendar == null) {
      List<String> names = calendarBook.listCalendarNames();
      activeCalendar = names.isEmpty() ? null : names.get(0);
    }
    Calendar cal = calendarBook.getCalendar(activeCalendar);
    visibleMonth = YearMonth.now(cal.getZoneId());
    selectedDate = LocalDate.now(cal.getZoneId());
    refresh();
    view.start();
  }

  @Override
  public void selectCalendar(String calendarName) {
    Objects.requireNonNull(calendarName, "calendarName");
    if (!calendarBook.hasCalendar(calendarName)) {
      view.showError("No such calendar: " + calendarName);
      return;
    }
    activeCalendar = calendarName;
    Calendar cal = calendarBook.getCalendar(activeCalendar);
    visibleMonth = YearMonth.from(selectedDate == null ? LocalDate.now(cal.getZoneId())
        : selectedDate);
    refresh();
  }

  @Override
  public void createCalendar(String name, ZoneId zone) {
    calendarBook.createCalendar(name, zone);
    activeCalendar = name;
    visibleMonth = YearMonth.now(zone);
    selectedDate = LocalDate.now(zone);
    refresh();
  }

  @Override
  public void renameActiveCalendar(String newName) {
    if (activeCalendar == null) {
      view.showError("No calendar selected");
      return;
    }
    calendarBook.renameCalendar(activeCalendar, newName);
    activeCalendar = newName;
    refresh();
  }

  @Override
  public void changeActiveCalendarZone(ZoneId zone) {
    if (activeCalendar == null) {
      view.showError("No calendar selected");
      return;
    }
    calendarBook.changeTimezone(activeCalendar, zone);
    visibleMonth = visibleMonth.withMonth(visibleMonth.getMonthValue());
    refresh();
  }

  @Override
  public void goToMonth(YearMonth month) {
    this.visibleMonth = Objects.requireNonNull(month, "month");
    int day = selectedDate == null ? 1
        : Math.min(selectedDate.getDayOfMonth(), month.lengthOfMonth());
    selectedDate = month.atDay(day);
    refresh();
  }

  @Override
  public void selectDate(LocalDate date) {
    this.selectedDate = Objects.requireNonNull(date, "date");
    this.visibleMonth = YearMonth.from(date);
    refresh();
  }

  @Override
  public void createEvent(EventInput input) {
    Calendar cal = requireActiveCalendar();
    if (cal == null) {
      return;
    }
    LocalTime startTime = input.getStartTime() == null ? WorkingHours.START : input.getStartTime();
    LocalTime endTime = input.getEndTime() == null ? WorkingHours.END : input.getEndTime();
    LocalDateTime start = LocalDateTime.of(input.getDate(), startTime);
    LocalDateTime end = LocalDateTime.of(input.getDate(), endTime);
    EventInput.RecurrenceRule rule = input.getRecurrenceRule();
    if (rule.isRecurring()) {
      createSeries(cal, input, start, end, rule);
    } else {
      createSingle(cal, input, start, end);
    }
    refresh();
  }

  @Override
  public void editEvent(EventReference reference, EventUpdate update, EditScope scope) {
    Calendar cal = requireActiveCalendar();
    if (cal == null) {
      return;
    }
    switch (scope) {
      case EVENT:
        cal.editEvent(reference.getSubject(), reference.getStart(),
            update.getProperty(), update.getNewDateTime(), update.getNewValue());
        break;
      case EVENTS:
        cal.editEventsFromDate(reference.getSubject(), reference.getStart(),
            update.getProperty(), update.getNewDateTime(), update.getNewValue());
        break;
      case SERIES:
        cal.editSeries(reference.getSubject(), reference.getStart(), update.getProperty(),
            update.getNewDateTime(), update.getNewValue());
        break;
      default:
        throw new IllegalArgumentException("Unknown scope");
    }
    refresh();
  }

  @Override
  public void exportCalendar(String filePath) {
    Calendar cal = requireActiveCalendar();
    if (cal == null) {
      return;
    }
    try {
      String path = ExportUtil.export(cal, filePath);
      view.showMessage("Exported to " + path);
    } catch (IOException e) {
      view.showError("Export failed: " + e.getMessage());
    }
  }

  private void createSingle(Calendar cal, EventInput input, LocalDateTime start,
                            LocalDateTime end) {
    Event event = cal.createEvent(input.getSubject(), start, end);
    applyOptionalFields(cal, event, input);
  }

  private void createSeries(Calendar cal, EventInput input, LocalDateTime start,
                            LocalDateTime end, EventInput.RecurrenceRule rule) {
    Set<DayOfWeek> weekdays = rule.getWeekdays();
    if (rule.getOccurrences() != null) {
      List<Event> events = cal.createEventSeries(input.getSubject(), start, end, weekdays,
          rule.getOccurrences());
      events.forEach(event -> applyOptionalFields(cal, event, input));
    } else {
      List<Event> events = cal.createEventSeriesUntil(input.getSubject(), start, end, weekdays,
          rule.getUntilDate());
      events.forEach(event -> applyOptionalFields(cal, event, input));
    }
  }

  private void applyOptionalFields(Calendar cal, Event event, EventInput input) {
    if (input.getDescription() != null) {
      cal.editEvent(event.getSubject(), event.getStartDateTime(), EventProperty.DESCRIPTION,
          null, input.getDescription());
    }
    if (input.getLocation() != null) {
      cal.editEvent(event.getSubject(), event.getStartDateTime(), EventProperty.LOCATION,
          null, input.getLocation());
    }
    cal.editEvent(event.getSubject(), event.getStartDateTime(), EventProperty.STATUS, null,
        input.isPublicEvent() ? "public" : "private");
  }

  private void refresh() {
    Calendar cal = requireActiveCalendar();
    if (cal == null) {
      return;
    }
    Map<LocalDate, List<EventViewModel>> monthEvents = eventsByDate(cal, visibleMonth);
    List<EventViewModel> dayEvents = monthEvents.getOrDefault(selectedDate, List.of());
    CalendarUiState state = new CalendarUiState(calendarSummaries(), activeCalendar,
        cal.getZoneId(), visibleMonth, selectedDate, monthEvents, dayEvents);
    view.render(state);
  }

  private Map<LocalDate, List<EventViewModel>> eventsByDate(Calendar cal, YearMonth month) {
    LocalDateTime start = month.atDay(1).atStartOfDay();
    LocalDateTime end = month.atEndOfMonth().atTime(LocalTime.MAX);
    List<Event> events = cal.getEventsInRange(start, end);
    Map<LocalDate, List<EventViewModel>> grouped = new HashMap<>();
    for (Event event : events) {
      LocalDate date = event.getStartDateTime().toLocalDate();
      grouped.computeIfAbsent(date, d -> new ArrayList<>())
          .add(toViewModel(event));
    }
    for (List<EventViewModel> list : grouped.values()) {
      list.sort((a, b) -> a.getStart().compareTo(b.getStart()));
    }
    return grouped;
  }

  private EventViewModel toViewModel(Event event) {
    return new EventViewModel(event.getSubject(), event.getStartDateTime(), event.getEndDateTime(),
        event.getLocation().orElse(null), event.getDescription().orElse(null), event.isPublic(),
        event.isSeriesPart());
  }

  private Calendar requireActiveCalendar() {
    if (activeCalendar == null) {
      if (calendarBook.listCalendarNames().isEmpty()) {
        view.showError("No calendars available");
        return null;
      }
      activeCalendar = calendarBook.listCalendarNames().get(0);
    }
    return calendarBook.getCalendar(activeCalendar);
  }

  private List<CalendarSummary> calendarSummaries() {
    List<String> names = calendarBook.listCalendarNames();
    Map<String, ZoneId> zones = names.stream().collect(Collectors.toMap(name -> name,
        name -> calendarBook.getCalendar(name).getZoneId()));
    List<String> palette = colorPalette();
    List<CalendarSummary> summaries = new ArrayList<>();
    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);
      String color = palette.get(i % palette.size());
      summaries.add(new CalendarSummary(name, zones.get(name), color));
    }
    return summaries;
  }

  private List<String> colorPalette() {
    List<String> palette = new ArrayList<>();
    palette.add("#0072b1");
    palette.add("#a64ac9");
    palette.add("#00897b");
    palette.add("#d84315");
    palette.add("#6a1b9a");
    palette.add("#2e7d32");
    return palette;
  }

  private void ensureDefaultCalendar() {
    if (calendarBook.listCalendarNames().isEmpty()) {
      calendarBook.createCalendar("Default", defaultZone);
    }
    if (activeCalendar == null) {
      activeCalendar = calendarBook.listCalendarNames().get(0);
    }
  }
}
