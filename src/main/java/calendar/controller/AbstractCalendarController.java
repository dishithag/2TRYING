package calendar.controller;

import calendar.Calendar;
import calendar.CalendarBook;
import calendar.CalendarProperty;
import calendar.Event;
import calendar.EventProperty;
import calendar.util.ExportUtil;
import calendar.view.CalendarView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Base controller with shared execution logic for the calendar app.
 */
public abstract class AbstractCalendarController implements CalendarController {

  protected final CalendarBook book;
  protected final CalendarContext context;
  protected final CalendarView view;
  protected final BufferedReader reader;
  protected final CommandParser parser;

  /**
   * Constructs a controller wired to a {@link calendar.CalendarBook} model,
   * a {@link calendar.view.CalendarView}, and a character {@link java.io.Reader}.
   * Initializes the {@link CalendarContext}, buffered input, and command parser.
   *
   * @param book  calendar-book model; must not be {@code null}
   * @param view  view used for user-facing output; must not be {@code null}
   * @param input character stream to read commands from; must not be {@code null}
   * @throws IllegalArgumentException if any argument is {@code null}
   */

  protected AbstractCalendarController(CalendarBook book, CalendarView view, Reader input) {
    if (book == null || view == null || input == null) {
      throw new IllegalArgumentException("Arguments cannot be null");
    }
    this.book = book;
    this.context = new CalendarContext(book);
    this.view = view;
    this.reader = new BufferedReader(input);
    this.parser = new CommandParser();
  }

  @Override
  public void run() {
    displayWelcome();
    boolean foundExit = false;
    int lineNumber = 0;
    try {
      while (true) {
        showPrompt();
        String line = reader.readLine();
        if (line == null) {
          break;
        }
        lineNumber++;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        try {
          CommandParser.Command cmd = parser.parse(trimmed);
          if (cmd.getTypeEnum() == CommandType.EXIT) {
            handleExit();
            foundExit = true;
            break;
          }
          executeCommand(cmd);
        } catch (Exception e) {
          handleError(e.getMessage(), lineNumber);
        }
      }
      if (!foundExit) {
        handleMissingExit();
      }
    } catch (IOException e) {
      view.displayError("IO error: " + e.getMessage());
    }
  }

  /**
   * Executes a parsed {@link CommandParser.Command} by dispatching on
   * {@code cmd.getTypeEnum()}. Depending on the type, this method creates or edits
   * calendars/events, prints listings or status, exports data, or copies events,
   * delegating to helpers such as {@link #handleEdit(CommandParser.Command)},
   * {@link #handleEditCalendar(CommandParser.Command)},
   * {@link #handleCopySingle(CommandParser.Command)},
   * {@link #handleCopyOnDate(CommandParser.Command)}, and
   * {@link #handleCopyBetween(CommandParser.Command)}.
   * Any runtime errors are caught and surfaced through the {@link calendar.view.CalendarView}.
   *
   * @param cmd the parsed command to execute (must not be {@code null})
   */

  protected void executeCommand(CommandParser.Command cmd) {
    try {
      switch (cmd.getTypeEnum()) {
        case CREATE_CALENDAR:
          {
          book.createCalendar(cmd.getCalendarName(), ZoneId.of(cmd.getTimezoneId()));
          view.displayMessage("Created calendar: " + cmd.getCalendarName());
          break;
          }
        case EDIT_CALENDAR:
          {
          handleEditCalendar(cmd);
          break;
          }
        case USE_CALENDAR:
          {
          context.use(cmd.getCalendarName());
          view.displayMessage("Using calendar: " + cmd.getCalendarName());
          break;
          }
        case CREATE_SINGLE:
          {
          Calendar cal = context.current();
          Event e = cal.createEvent(cmd.getSubject(), cmd.getStartDateTime(), cmd.getEndDateTime());
          view.displayMessage("Event created: " + e.getSubject());
          break;
          }
        case CREATE_SERIES:
          {
          Calendar cal = context.current();
          List<Event> series = cal.createEventSeries(
              cmd.getSubject(), cmd.getStartDateTime(), cmd.getEndDateTime(),
              cmd.getWeekdays(), cmd.getOccurrences());
          view.displayMessage("Created " + series.size() + " events in series");
          break;
          }
        case CREATE_SERIES_UNTIL:
          {
          Calendar cal = context.current();
          List<Event> seriesUntil = cal.createEventSeriesUntil(
              cmd.getSubject(), cmd.getStartDateTime(),
              cmd.getEndDateTime(), cmd.getWeekdays(), cmd.getUntilDate());
          view.displayMessage("Created " + seriesUntil.size() + " events in series");
          break;
          }
        case EDIT:
          {
          handleEdit(cmd);
          break;
          }
        case PRINT_ON:
          {
          Calendar cal = context.current();
          view.displayEvents(cal.getEventsOnDate(cmd.getStartDateTime().toLocalDate()));
          break;
          }
        case PRINT_RANGE:
          {
          Calendar cal = context.current();
          view.displayEvents(cal.getEventsInRange(cmd.getStartDateTime(), cmd.getEndDateTime()));
          break;
          }
        case STATUS:
          {
          Calendar cal = context.current();
          view.displayStatus(cal.isBusyAt(cmd.getStartDateTime()));
          break;
          }
        case EXPORT:
          {
          Calendar cal = context.current();
          String path = ExportUtil.export(cal, cmd.getFileName());
          view.displayMessage("Exported to: " + path);
          break;
          }
        case COPY_EVENT:
          {
          handleCopySingle(cmd);
          break;
          }
        case COPY_ON_DATE:
          {
          handleCopyOnDate(cmd);
          break;
          }
        case COPY_BETWEEN:
          {
          handleCopyBetween(cmd);
          break;
          }
        default:
          view.displayError("Unknown command type: " + cmd.getType());
      }
    } catch (Exception e) {
      view.displayError(e.getMessage());
    }
  }

  /**
   * Applies an edit command to the active calendar.
   * Resolves the target {@link EventProperty} and the edit scope
   * (event / events-from-date / whole series), coerces {@code cmd.getNewValue()}
   * to a {@link java.time.LocalDateTime} when editing START/END or to text
   * otherwise, and delegates to the corresponding Calendar edit method.
   * Required fields in {@code cmd}: {@code subject}, {@code startDateTime},
   * property (as {@code eventPropertyEnum} or token), new value, and scope
   * (as {@code editScopeEnum} or token).
   *
   * @param cmd parsed edit command (not {@code null})
   * @throws IllegalArgumentException if fields are missing or values cannot be parsed
   */

  protected void handleEdit(CommandParser.Command cmd) {
    Calendar cal = context.current();
    EventProperty prop = cmd.getEventPropertyEnum() != null
        ? cmd.getEventPropertyEnum()
        : EventProperty.fromToken(cmd.getProperty());

    LocalDateTime dt = null;
    String text = null;
    if (prop == EventProperty.START || prop == EventProperty.END) {
      dt = LocalDateTime.parse(cmd.getNewValue());
    } else {
      text = cmd.getNewValue();
    }

    EditScope scope = cmd.getEditScopeEnum() != null
        ? cmd.getEditScopeEnum()
        : EditScope.fromToken(cmd.getEditScope());

    switch (scope) {
      case EVENT:
        cal.editEvent(cmd.getSubject(), cmd.getStartDateTime(), prop, dt, text);
        view.displayMessage("Event edited");
        break;
      case EVENTS:
        cal.editEventsFromDate(cmd.getSubject(), cmd.getStartDateTime(), prop, dt, text);
        view.displayMessage("Events edited");
        break;
      case SERIES:
        cal.editSeries(cmd.getSubject(), cmd.getStartDateTime(), prop, dt, text);
        view.displayMessage("Series edited");
        break;
      default:
        view.displayError("Unknown edit scope: " + cmd.getEditScope());
    }
  }

  /**
   * Handles an "edit calendar" command.
   *
   * <p>Supported properties:
   * <ul>
   *   <li>{@link CalendarProperty#NAME} — renames {@code cmd.getCalendarName()}
   *   to {@code cmd.getNewValue()}.</li>
   *   <li>{@link CalendarProperty#TIMEZONE} — updates the calendar
   *   timezone to {@code ZoneId.of(cmd.getNewValue())}.</li>
   * </ul>
   * On success, a confirmation is printed via the {@link calendar.view.CalendarView}.
   * If the property is not supported, an error message is shown.
   *
   * <p>Required fields in {@code cmd}: {@code calendarName}, {@code newValue},
   * and {@code calendarPropertyEnum}.</p>
   *
   * @param cmd parsed calendar-edit command (not {@code null})
   * @throws IllegalArgumentException    if the calendar does not exist or a rename conflicts
   * @throws java.time.DateTimeException if {@code newValue} is not a valid IANA timezone ID
   */

  protected void handleEditCalendar(CommandParser.Command cmd) {
    CalendarProperty cp = cmd.getCalendarPropertyEnum();
    if (cp == CalendarProperty.NAME) {
      book.renameCalendar(cmd.getCalendarName(), cmd.getNewValue());
      view.displayMessage("Calendar renamed to: " + cmd.getNewValue());
    } else if (cp == CalendarProperty.TIMEZONE) {
      book.changeTimezone(cmd.getCalendarName(), ZoneId.of(cmd.getNewValue()));
      view.displayMessage("Timezone updated for: " + cmd.getCalendarName());
    } else {
      view.displayError("Unknown calendar property: " + cmd.getProperty());
    }
  }

  /**
   * Copies a single event; prevents exact duplicates only.
   */
  protected void handleCopySingle(CommandParser.Command cmd) {
    Calendar src = context.current();
    Calendar dst = book.getCalendar(cmd.getTargetCalendar());
    List<Event> matches = src.findEvents(cmd.getSubject(), cmd.getStartDateTime());
    if (matches.size() != 1) {
      throw new IllegalArgumentException("Event not found or not unique");
    }
    Event e = matches.get(0);
    Duration dur = Duration.between(e.getStartDateTime(), e.getEndDateTime());
    LocalDateTime newStart = cmd.getTargetDateTime();
    LocalDateTime newEnd = newStart.plus(dur);

    if (existsExact(dst, e.getSubject(), newStart, newEnd)) {
      view.displayMessage("Copied 0 event(s) to " + cmd.getTargetCalendar());
      return;
    }

    dst.copyFrom(e, newStart, newEnd);
    view.displayMessage("Copied 1 event to " + cmd.getTargetCalendar());
  }

  /**
   * Copies all events on a given day; skips if subject already exists on that day in the target.
   */
  protected void handleCopyOnDate(CommandParser.Command cmd) {
    Calendar src = context.current();
    Calendar dst = book.getCalendar(cmd.getTargetCalendar());
    LocalDate srcDay = cmd.getDay();
    List<Event> todays = src.getEventsOnDate(srcDay);
    int copied = 0;
    for (Event e : todays) {
      Duration dur = Duration.between(e.getStartDateTime(), e.getEndDateTime());
      LocalDateTime newStart = cmd.getTargetDateTime()
          .toLocalDate().atTime(convertStartToTargetLocalTime(e, src, dst));
      LocalDateTime newEnd = newStart.plus(dur);

      if (existsExact(dst, e.getSubject(), newStart, newEnd)
          || existsSameDaySubject(dst, e.getSubject(), newStart)) {
        continue;
      }

      dst.copyFrom(e, newStart, newEnd);
      copied++;
    }
    view.displayMessage("Copied " + copied + " event(s) to " + cmd.getTargetCalendar());
  }

  /**
   * Copies all events in a range; skips if subject already exists on that day in the target.
   */
  protected void handleCopyBetween(CommandParser.Command cmd) {
    Calendar src = context.current();
    Calendar dst = book.getCalendar(cmd.getTargetCalendar());

    LocalDate startDay = cmd.getRangeStart();
    LocalDate endDay = cmd.getRangeEnd();
    LocalDateTime srcStart = startDay.atStartOfDay();
    LocalDateTime srcEnd = endDay.plusDays(1).atStartOfDay();

    List<Event> inRange = src.getEventsInRange(srcStart, srcEnd.minusSeconds(1));
    int copied = 0;

    for (Event e : inRange) {
      LocalDate d = e.getStartDateTime().toLocalDate();
      long dayOffset = java.time.temporal.ChronoUnit.DAYS.between(startDay, d);
      Duration dur = Duration.between(e.getStartDateTime(), e.getEndDateTime());

      LocalDate targetDay = cmd.getTargetDateTime().toLocalDate().plusDays(dayOffset);
      LocalDateTime newStart = targetDay.atTime(convertStartToTargetLocalTime(e, src, dst));
      LocalDateTime newEnd = newStart.plus(dur);

      if (existsExact(dst, e.getSubject(), newStart, newEnd)
          || existsSameDaySubject(dst, e.getSubject(), newStart)) {
        continue;
      }

      dst.copyFrom(e, newStart, newEnd);
      copied++;
    }

    view.displayMessage("Copied " + copied + " event(s) to " + cmd.getTargetCalendar());
  }

  private LocalTime convertStartToTargetLocalTime(Event event, Calendar src, Calendar dst) {
    return event.getStartDateTime()
        .atZone(src.getZoneId())
        .withZoneSameInstant(dst.getZoneId())
        .toLocalTime();
  }

  private boolean existsExact(Calendar cal, String subject, LocalDateTime start,
                              LocalDateTime end) {
    if (!cal.findEvents(subject, start, end).isEmpty()) {
      return true;
    }
    return !cal.findEvents(subject, start).isEmpty();
  }

  private boolean existsSameDaySubject(Calendar cal, String subject, LocalDateTime candidateStart) {
    LocalDate day = candidateStart.toLocalDate();
    for (Event e : cal.getEventsOnDate(day)) {
      if (e.getSubject().equals(subject)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Prints the startup banner or instructions for this controller mode.
   * Called once at the beginning of {@link #run()}.
   */
  protected abstract void displayWelcome();

  /**
   * Shows the user prompt before reading each command.
   * Implementations may no-op for headless/script modes.
   */
  protected abstract void showPrompt();

  /**
   * Runs when an explicit {@code exit} command is processed.
   * Use to print a farewell and perform final cleanup.
   */
  protected abstract void handleExit();

  /**
   * Reports a recoverable error tied to a single input line.
   *
   * @param message    human-readable error text
   * @param lineNumber 1-based line number in the input
   */
  protected abstract void handleError(String message, int lineNumber);

  /**
   * Invoked when input ends without an explicit {@code exit}.
   * Useful for warning users/scripts about an incomplete session.
   */
  protected abstract void handleMissingExit();

}
