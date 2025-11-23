package calendar.controller;

import calendar.CalendarProperty;
import calendar.EventProperty;
import calendar.WorkingHours;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses user command strings into structured commands with typed enums.
 * Behavior and error messages are preserved.
 */
public class CommandParser {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter DATETIME_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final LocalTime ALL_DAY_START = WorkingHours.START;
  private static final LocalTime ALL_DAY_END = WorkingHours.END;

  private static final Pattern FLAG_NAME = Pattern.compile(
      "--name\\s+(\"[^\"]+\"|\\S+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern FLAG_TZ = Pattern.compile(
      "--timezone\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern FLAG_PROP = Pattern.compile(
      "--property\\s+(\\S+)\\s+(\"[^\"]+\"|\\S+)", Pattern.CASE_INSENSITIVE);

  /**
   * Immutable value for a parsed command. Legacy string fields remain for compatibility.
   */
  public static class Command {
    private final String type;
    private final String subject;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final Set<DayOfWeek> weekdays;
    private final Integer occurrences;
    private final LocalDate untilDate;
    private final String property;
    private final String newValue;
    private final String editScope;
    private final String fileName;
    private final String calendarName;
    private final String timezoneId;
    private final String targetCalendar;
    private final LocalDateTime targetDateTime;
    private final LocalDate day;
    private final LocalDate rangeStart;
    private final LocalDate rangeEnd;

    private final CommandType typeEnum;
    private final EditScope editScopeEnum;
    private final EventProperty eventPropertyEnum;
    private final CalendarProperty calendarPropertyEnum;

    /**
     * Builds an immutable {@link Command} instance from the populated builder values.
     * Fields that are irrelevant for the parsed command remain {@code null}; enums capture
     * normalized values alongside legacy string tokens for compatibility with existing
     * consumers.
     *
     * @param builder source of parsed tokens and enums for this command
     */
    private Command(Builder builder) {
      this.type = builder.type;
      this.subject = builder.subject;
      this.startDateTime = builder.startDateTime;
      this.endDateTime = builder.endDateTime;
      this.weekdays = builder.weekdays;
      this.occurrences = builder.occurrences;
      this.untilDate = builder.untilDate;
      this.property = builder.property;
      this.newValue = builder.newValue;
      this.editScope = builder.editScope;
      this.fileName = builder.fileName;
      this.calendarName = builder.calendarName;
      this.timezoneId = builder.timezoneId;
      this.targetCalendar = builder.targetCalendar;
      this.targetDateTime = builder.targetDateTime;
      this.day = builder.day;
      this.rangeStart = builder.rangeStart;
      this.rangeEnd = builder.rangeEnd;
      this.typeEnum = builder.typeEnum;
      this.editScopeEnum = builder.editScopeEnum;
      this.eventPropertyEnum = builder.eventPropertyEnum;
      this.calendarPropertyEnum = builder.calendarPropertyEnum;
    }

    /**
     * Creates a builder seeded with both the legacy string type and the normalized enum value.
     *
     * @param type     raw command type token (e.g., "create_calendar", "edit")
     * @param typeEnum normalized command type enum
     * @return configured builder instance
     */
    public static Builder builder(String type, CommandType typeEnum) {
      return new Builder(type, typeEnum);
    }

    /**
     * Creates a builder seeded only with the legacy string type token.
     *
     * @param type raw command type token (e.g., "create_calendar", "edit")
     * @return configured builder instance
     */
    public static Builder builder(String type) {
      return new Builder(type, null);
    }

    /**
     * Fluent builder used to populate the appropriate fields for a parsed command variant.
     * Unset fields remain {@code null} in the resulting {@link Command}.
     */
    public static class Builder {
      private final String type;
      private final CommandType typeEnum;
      private String subject;
      private LocalDateTime startDateTime;
      private LocalDateTime endDateTime;
      private Set<DayOfWeek> weekdays;
      private Integer occurrences;
      private LocalDate untilDate;
      private String property;
      private String newValue;
      private String editScope;
      private String fileName;
      private String calendarName;
      private String timezoneId;
      private String targetCalendar;
      private LocalDateTime targetDateTime;
      private LocalDate day;
      private LocalDate rangeStart;
      private LocalDate rangeEnd;
      private EditScope editScopeEnum;
      private EventProperty eventPropertyEnum;
      private CalendarProperty calendarPropertyEnum;

      private Builder(String type, CommandType typeEnum) {
        this.type = type;
        this.typeEnum = typeEnum;
      }

      /**
       * Specifies the subject for the command.
       *
       * @param subject subject string
       * @return builder instance
       */
      public Builder subject(String subject) {
        this.subject = subject;
        return this;
      }

      /**
       * Sets the start date/time in ISO-8601.
       *
       * @param startDateTime start date-time
       * @return builder instance
       */
      public Builder startDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
        return this;
      }

      /**
       * Sets the end date/time in ISO-8601.
       *
       * @param endDateTime end date-time
       * @return builder instance
       */
      public Builder endDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
        return this;
      }

      /**
       * Specifies recurring weekdays for an event.
       *
       * @param weekdays recurrence days
       * @return builder instance
       */
      public Builder weekdays(Set<DayOfWeek> weekdays) {
        this.weekdays = weekdays;
        return this;
      }

      /**
       * Sets the number of occurrences for recurrence.
       *
       * @param occurrences count of occurrences
       * @return builder instance
       */
      public Builder occurrences(Integer occurrences) {
        this.occurrences = occurrences;
        return this;
      }

      /**
       * Sets the recurrence end date.
       *
       * @param untilDate inclusive end date
       * @return builder instance
       */
      public Builder untilDate(LocalDate untilDate) {
        this.untilDate = untilDate;
        return this;
      }

      /**
       * Specifies the property being edited.
       *
       * @param property textual property name
       * @return builder instance
       */
      public Builder property(String property) {
        this.property = property;
        return this;
      }

      /**
       * Sets a new string value for the property being edited.
       *
       * @param newValue replacement value
       * @return builder instance
       */
      public Builder newValue(String newValue) {
        this.newValue = newValue;
        return this;
      }

      /**
       * Specifies the textual edit scope.
       *
       * @param editScope scope string
       * @return builder instance
       */
      public Builder editScope(String editScope) {
        this.editScope = editScope;
        return this;
      }

      /**
       * Sets the export file name.
       *
       * @param fileName output file name
       * @return builder instance
       */
      public Builder fileName(String fileName) {
        this.fileName = fileName;
        return this;
      }

      /**
       * Sets the calendar name for create/edit/use commands.
       *
       * @param calendarName calendar identifier
       * @return builder instance
       */
      public Builder calendarName(String calendarName) {
        this.calendarName = calendarName;
        return this;
      }

      /**
       * Specifies the timezone identifier for calendar operations.
       *
       * @param timezoneId IANA timezone id
       * @return builder instance
       */
      public Builder timezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
        return this;
      }

      /**
       * Sets the target calendar for copy commands.
       *
       * @param targetCalendar target calendar name
       * @return builder instance
       */
      public Builder targetCalendar(String targetCalendar) {
        this.targetCalendar = targetCalendar;
        return this;
      }

      /**
       * Specifies the target start date/time for copies.
       *
       * @param targetDateTime destination date-time
       * @return builder instance
       */
      public Builder targetDateTime(LocalDateTime targetDateTime) {
        this.targetDateTime = targetDateTime;
        return this;
      }

      /**
       * Sets a specific day for daily copy operations.
       *
       * @param day date to copy
       * @return builder instance
       */
      public Builder day(LocalDate day) {
        this.day = day;
        return this;
      }

      /**
       * Specifies the start of a date range.
       *
       * @param rangeStart inclusive range start
       * @return builder instance
       */
      public Builder rangeStart(LocalDate rangeStart) {
        this.rangeStart = rangeStart;
        return this;
      }

      /**
       * Specifies the end of a date range.
       *
       * @param rangeEnd inclusive range end
       * @return builder instance
       */
      public Builder rangeEnd(LocalDate rangeEnd) {
        this.rangeEnd = rangeEnd;
        return this;
      }

      /**
       * Sets the parsed edit scope enum.
       *
       * @param editScopeEnum edit scope
       * @return builder instance
       */
      public Builder editScopeEnum(EditScope editScopeEnum) {
        this.editScopeEnum = editScopeEnum;
        return this;
      }

      /**
       * Sets the parsed event property enum.
       *
       * @param eventPropertyEnum event property
       * @return builder instance
       */
      public Builder eventPropertyEnum(EventProperty eventPropertyEnum) {
        this.eventPropertyEnum = eventPropertyEnum;
        return this;
      }

      /**
       * Sets the parsed calendar property enum.
       *
       * @param calendarPropertyEnum calendar property
       * @return builder instance
       */
      public Builder calendarPropertyEnum(CalendarProperty calendarPropertyEnum) {
        this.calendarPropertyEnum = calendarPropertyEnum;
        return this;
      }

      /**
       * Finalizes the builder into an immutable {@link Command} instance.
       *
       * @return constructed {@link Command}
       */
      public Command build() {
        return new Command(this);
      }
    }

    public String getType() {
      return type;
    }

    public String getSubject() {
      return subject;
    }

    public LocalDateTime getStartDateTime() {
      return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
      return endDateTime;
    }

    public Set<DayOfWeek> getWeekdays() {
      return weekdays;
    }

    public Integer getOccurrences() {
      return occurrences;
    }

    public LocalDate getUntilDate() {
      return untilDate;
    }

    public String getProperty() {
      return property;
    }

    public String getNewValue() {
      return newValue;
    }

    public String getEditScope() {
      return editScope;
    }

    public String getFileName() {
      return fileName;
    }

    public String getCalendarName() {
      return calendarName;
    }

    public String getTimezoneId() {
      return timezoneId;
    }

    public String getTargetCalendar() {
      return targetCalendar;
    }

    public LocalDateTime getTargetDateTime() {
      return targetDateTime;
    }

    public LocalDate getDay() {
      return day;
    }

    public LocalDate getRangeStart() {
      return rangeStart;
    }

    public LocalDate getRangeEnd() {
      return rangeEnd;
    }

    public CommandType getTypeEnum() {
      return typeEnum;
    }

    public EditScope getEditScopeEnum() {
      return editScopeEnum;
    }

    public EventProperty getEventPropertyEnum() {
      return eventPropertyEnum;
    }

    public CalendarProperty getCalendarPropertyEnum() {
      return calendarPropertyEnum;
    }
  }

  /**
   * Parses one user input line into a structured {@link Command}.
   *
   * <p>Trims the input, determines the command family by prefix, and
   * delegates to specialized parsers. On invalid or unsupported
   * syntax, it throws an {@link IllegalArgumentException} with a
   * user-readable message.
   * </p>
   *
   * <p>Recognized forms include:
   * {@code exit},
   * {@code create calendar ...},
   * {@code edit calendar ...},
   * {@code use calendar ...},
   * {@code export cal ...},
   * {@code show status on ...},
   * {@code print events on ...},
   * {@code print events from ...},
   * {@code copy event ...},
   * {@code copy events on ...},
   * {@code copy events between ...},
   * {@code create event ...},
   * and {@code edit event|events|series ...}.
   * </p>
   *
   * @param line raw user input; leading/trailing whitespace ignored
   * @return a populated {@link Command} describing the action to run
   * @throws IllegalArgumentException if {@code line} is null/empty
   *                                  or the detected command has invalid syntax
   */

  public Command parse(String line) {
    if (line == null || line.trim().isEmpty()) {
      throw new IllegalArgumentException("Empty command");
    }
    String trimmed = line.trim();
    String lower = trimmed.toLowerCase();

    Command simple = parseSimpleCommands(trimmed, lower);
    if (simple != null) {
      return simple;
    }

    Command calendarCommand = parseCalendarCommands(trimmed, lower);
    if (calendarCommand != null) {
      return calendarCommand;
    }

    Command printCommand = parsePrintCommands(trimmed, lower);
    if (printCommand != null) {
      return printCommand;
    }

    Command copyCommand = parseCopyCommands(trimmed, lower);
    if (copyCommand != null) {
      return copyCommand;
    }

    if (lower.startsWith("create event ")) {
      return parseCreateEvent(trimmed.substring("create event ".length()));
    }
    if (lower.startsWith("edit event ")
        || lower.startsWith("edit events ")
        || lower.startsWith("edit series ")) {
      return parseEditCommand(trimmed);
    }
    throw new IllegalArgumentException("Unknown command: " + trimmed);
  }

  private Command parseSimpleCommands(String trimmed, String lower) {
    if (lower.equals("exit")) {
      return Command.builder("exit", CommandType.EXIT).build();
    }
    if (lower.startsWith("export cal ")) {
      String fileName = trimmed.substring("export cal ".length()).trim();
      return Command.builder("export", CommandType.EXPORT)
          .fileName(fileName)
          .build();
    }
    if (lower.startsWith("show status on ")) {
      String dateTimeStr = trimmed.substring("show status on ".length()).trim();
      LocalDateTime dateTime = parseDateTime(dateTimeStr);
      return Command.builder("status", CommandType.STATUS)
          .startDateTime(dateTime)
          .build();
    }
    return null;
  }

  private Command parseCalendarCommands(String trimmed, String lower) {
    if (lower.startsWith("create calendar ")) {
      return parseCreateCalendar(trimmed);
    }
    if (lower.startsWith("edit calendar ")) {
      return parseEditCalendar(trimmed);
    }
    if (lower.startsWith("use calendar ")) {
      return parseUseCalendar(trimmed);
    }
    return null;
  }

  private Command parsePrintCommands(String trimmed, String lower) {
    if (lower.startsWith("print events on ")) {
      String dateStr = trimmed.substring("print events on ".length()).trim();
      LocalDate date = parseDate(dateStr);
      return Command.builder("print_on", CommandType.PRINT_ON)
          .startDateTime(date.atStartOfDay())
          .build();
    }
    if (lower.startsWith("print events from ")) {
      return parsePrintRange(trimmed);
    }
    return null;
  }

  private Command parseCopyCommands(String trimmed, String lower) {
    if (lower.startsWith("copy event ")) {
      return parseCopyEvent(trimmed);
    }
    if (lower.startsWith("copy events on ")) {
      return parseCopyOnDate(trimmed);
    }
    if (lower.startsWith("copy events between ")) {
      return parseCopyBetween(trimmed);
    }
    return null;
  }

  private Command parseCreateCalendar(String line) {
    Matcher nameM = FLAG_NAME.matcher(line);
    Matcher tzM = FLAG_TZ.matcher(line);
    if (!nameM.find() || !tzM.find()) {
      throw new IllegalArgumentException(
          "Invalid syntax. Expected: create calendar --name <name> --timezone <area/location>");
    }
    String name = unquote(nameM.group(1));
    String tz = tzM.group(1);
    return Command.builder("create_calendar", CommandType.CREATE_CALENDAR)
        .calendarName(name)
        .timezoneId(tz)
        .build();
  }

  private Command parseEditCalendar(String line) {
    Matcher nameM = FLAG_NAME.matcher(line);
    Matcher propM = FLAG_PROP.matcher(line);
    if (!nameM.find() || !propM.find()) {
      throw new IllegalArgumentException(
          "Invalid syntax. Expected: edit calendar --name <name>"
              +
              " --property <name|timezone> <value>");
    }
    String name = unquote(nameM.group(1));
    String prop = propM.group(1).toLowerCase();
    String val = unquote(propM.group(2));
    CalendarProperty cp;
    if ("name".equals(prop)) {
      cp = CalendarProperty.NAME;
    } else {
      if ("timezone".equals(prop)) {
        cp = CalendarProperty.TIMEZONE;
      } else {
        throw new IllegalArgumentException("Unknown calendar property: " + prop);
      }
    }
    return Command.builder("edit_calendar", CommandType.EDIT_CALENDAR)
        .property(prop)
        .newValue(val)
        .calendarName(name)
        .calendarPropertyEnum(cp)
        .build();
  }

  private Command parseUseCalendar(String line) {
    Matcher nameM = FLAG_NAME.matcher(line);
    if (!nameM.find()) {
      throw new IllegalArgumentException(
          "Invalid syntax. Expected: use calendar --name <name>");
    }
    String name = unquote(nameM.group(1));
    return Command.builder("use_calendar", CommandType.USE_CALENDAR)
        .calendarName(name)
        .build();
  }

  private Command parsePrintRange(String trimmed) {
    Pattern pattern = Pattern.compile(
        "print events from (\\S+) to (\\S+)", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(trimmed);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid print range syntax");
    }
    LocalDateTime start = parseDateTime(matcher.group(1));
    LocalDateTime end = parseDateTime(matcher.group(2));
    return Command.builder("print_range", CommandType.PRINT_RANGE)
        .startDateTime(start)
        .endDateTime(end)
        .build();
  }

  private Command parseCreateEvent(String args) {
    String subject;
    String remaining;

    if (args.startsWith("\"")) {
      int endQuote = args.indexOf("\"", 1);
      if (endQuote == -1) {
        throw new IllegalArgumentException("Unclosed quote in subject");
      }
      subject = args.substring(1, endQuote);
      remaining = args.substring(endQuote + 1).trim();
    } else {
      int fromIdx = indexOfIgnoreCase(args, " from ");
      int onIdx = indexOfIgnoreCase(args, " on ");
      if (fromIdx > 0 && (onIdx == -1 || fromIdx < onIdx)) {
        subject = args.substring(0, fromIdx);
        remaining = args.substring(fromIdx).trim();
      } else if (onIdx > 0) {
        subject = args.substring(0, onIdx);
        remaining = args.substring(onIdx).trim();
      } else {
        throw new IllegalArgumentException("Invalid syntax: expected 'from' or 'on'");
      }
    }

    if (remaining.toLowerCase().startsWith("on ")) {
      return parseAllDayEvent(subject, remaining.substring(3).trim());
    }
    if (remaining.toLowerCase().startsWith("from ")) {
      return parseTimedEvent(subject, remaining.substring(5).trim());
    }
    throw new IllegalArgumentException("Invalid create event syntax");
  }

  private Command parseAllDayEvent(String subject, String args) {
    String[] parts = args.split("\\s+");
    LocalDate date = parseDate(parts[0]);
    LocalDateTime start = date.atTime(ALL_DAY_START);
    LocalDateTime end = date.atTime(ALL_DAY_END);

    if (parts.length == 1) {
      return Command.builder("create_single", CommandType.CREATE_SINGLE)
          .subject(subject)
          .startDateTime(start)
          .endDateTime(end)
          .build();
    }

    if (parts.length >= 4 && parts[1].equalsIgnoreCase("repeats")) {
      Set<DayOfWeek> wd = parseWeekdays(parts[2]);

      if (parts[3].equalsIgnoreCase("for") && parts.length >= 6) {
        int occurrences = Integer.parseInt(parts[4]);
        return Command.builder("create_series", CommandType.CREATE_SERIES)
            .subject(subject)
            .startDateTime(start)
            .endDateTime(end)
            .weekdays(wd)
            .occurrences(occurrences)
            .build();
      }

      if (parts[3].equalsIgnoreCase("until") && parts.length >= 5) {
        LocalDate untilDate = parseDate(parts[4]);
        return Command.builder("create_series_until", CommandType.CREATE_SERIES_UNTIL)
            .subject(subject)
            .startDateTime(start)
            .endDateTime(end)
            .weekdays(wd)
            .untilDate(untilDate)
            .build();
      }
    }

    throw new IllegalArgumentException("Invalid all-day event syntax");
  }

  private Command parseTimedEvent(String subject, String args) {
    Pattern pattern = Pattern.compile(
        "(\\S+)\\s+to\\s+(\\S+)(?:\\s+repeats\\s+(\\S+)\\s+(for|until)\\s+(\\S+)(?:\\s+times)?)?");
    Matcher matcher = pattern.matcher(args);

    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid timed event syntax");
    }

    LocalDateTime start = parseDateTime(matcher.group(1));
    LocalDateTime end = parseDateTime(matcher.group(2));

    if (matcher.group(3) == null) {
      return Command.builder("create_single", CommandType.CREATE_SINGLE)
          .subject(subject)
          .startDateTime(start)
          .endDateTime(end)
          .build();
    }

    Set<DayOfWeek> wd = parseWeekdays(matcher.group(3));
    String repeatType = matcher.group(4);
    String repeatValue = matcher.group(5);

    if (repeatType.equalsIgnoreCase("for")) {
      int occurrences = Integer.parseInt(repeatValue);
      return Command.builder("create_series", CommandType.CREATE_SERIES)
          .subject(subject)
          .startDateTime(start)
          .endDateTime(end)
          .weekdays(wd)
          .occurrences(occurrences)
          .build();
    }

    LocalDate untilDate = parseDate(repeatValue);
    return Command.builder("create_series_until", CommandType.CREATE_SERIES_UNTIL)
        .subject(subject)
        .startDateTime(start)
        .endDateTime(end)
        .weekdays(wd)
        .untilDate(untilDate)
        .build();
  }

  private Command parseEditCommand(String line) {
    String scope;
    String remaining;

    if (line.toLowerCase().startsWith("edit event ")) {
      scope = "event";
      remaining = line.substring("edit event ".length());
    } else if (line.toLowerCase().startsWith("edit events ")) {
      scope = "events";
      remaining = line.substring("edit events ".length());
    } else {
      scope = "series";
      remaining = line.substring("edit series ".length());
    }

    String[] parts = remaining.split("\\s+", 2);
    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid edit syntax");
    }

    final String property = parts[0];
    String rest = parts[1];
    String subject;
    String afterSubject;

    if (rest.startsWith("\"")) {
      int endQuote = rest.indexOf("\"", 1);
      if (endQuote == -1) {
        throw new IllegalArgumentException("Unclosed quote");
      }
      subject = rest.substring(1, endQuote);
      afterSubject = rest.substring(endQuote + 1).trim();
    } else {
      int fromIdx = rest.toLowerCase().indexOf(" from ");
      if (fromIdx == -1) {
        throw new IllegalArgumentException("Missing 'from'");
      }
      subject = rest.substring(0, fromIdx).trim();
      afterSubject = rest.substring(fromIdx).trim();
    }

    Pattern pattern = Pattern.compile(
        "from\\s+(\\S+)\\s+with\\s+(.+)", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(afterSubject);

    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "Invalid edit syntax. Expected: from <dateTime> with <value>");
    }

    LocalDateTime start = parseDateTime(matcher.group(1));
    String newValue = stripQuotes(matcher.group(2).trim());

    EditScope scopeEnum = EditScope.fromToken(scope);
    EventProperty ep = EventProperty.fromToken(property);

    return Command.builder("edit", CommandType.EDIT)
        .subject(subject)
        .startDateTime(start)
        .property(property)
        .newValue(newValue)
        .editScope(scope)
        .editScopeEnum(scopeEnum)
        .eventPropertyEnum(ep)
        .build();
  }

  private Command parseCopyEvent(String line) {
    String body = line.substring("copy event ".length()).trim();

    String subject;
    String rest;
    if (body.startsWith("\"")) {
      int endQuote = body.indexOf("\"", 1);
      if (endQuote == -1) {
        throw new IllegalArgumentException("Unclosed quote in event name");
      }
      subject = body.substring(1, endQuote);
      rest = body.substring(endQuote + 1).trim();
    } else {
      int onIdx = body.toLowerCase().indexOf(" on ");
      if (onIdx < 0) {
        throw new IllegalArgumentException("Missing 'on' for copy event");
      }
      subject = body.substring(0, onIdx).trim();
      rest = body.substring(onIdx).trim();
    }

    Pattern p = Pattern.compile(
        "on\\s+(\\S+)\\s+--target\\s+(\"[^\"]+\"|\\S+)\\s+to\\s+(\\S+)",
        Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(rest);
    if (!m.matches()) {
      throw new IllegalArgumentException("Invalid copy event syntax");
    }

    LocalDateTime sourceStart = parseDateTime(m.group(1));
    String targetCal = unquote(m.group(2));
    LocalDateTime targetStart = parseDateTime(m.group(3));

    return Command.builder("copy_event", CommandType.COPY_EVENT)
        .subject(subject)
        .startDateTime(sourceStart)
        .targetCalendar(targetCal)
        .targetDateTime(targetStart)
        .build();
  }

  private Command parseCopyOnDate(String line) {
    Pattern p = Pattern.compile(
        "copy\\s+events\\s+on\\s+(\\S+)\\s+--target\\s+(\"[^\"]+\"|\\S+)\\s+to\\s+(\\S+)",
        Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(line);
    if (!m.matches()) {
      throw new IllegalArgumentException("Invalid copy events on syntax");
    }

    LocalDate sourceDate = parseDate(m.group(1));
    String targetCal = unquote(m.group(2));
    LocalDate targetDate = parseDate(m.group(3));

    return Command.builder("copy_on_date", CommandType.COPY_ON_DATE)
        .targetCalendar(targetCal)
        .targetDateTime(targetDate.atStartOfDay())
        .day(sourceDate)
        .build();
  }

  private Command parseCopyBetween(String line) {
    Pattern p = Pattern.compile(
        "copy\\s+events\\s+between\\s+(\\S+)\\s+and\\s+(\\S+)\\s"
            +
            "+--target\\s+(\"[^\"]+\"|\\S+)\\s+to\\s+(\\S+)",
        Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(line);
    if (!m.matches()) {
      throw new IllegalArgumentException("Invalid copy events between syntax");
    }

    LocalDate start = parseDate(m.group(1));
    LocalDate end = parseDate(m.group(2));
    String targetCal = unquote(m.group(3));
    LocalDate targetStart = parseDate(m.group(4));

    return Command.builder("copy_between", CommandType.COPY_BETWEEN)
        .targetCalendar(targetCal)
        .targetDateTime(targetStart.atStartOfDay())
        .rangeStart(start)
        .rangeEnd(end)
        .build();
  }

  private LocalDate parseDate(String dateStr) {
    try {
      return LocalDate.parse(dateStr, DATE_FMT);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid date: " + dateStr + ". Expected format: YYYY-MM-DD");
    }
  }

  private LocalDateTime parseDateTime(String dateTimeStr) {
    try {
      return LocalDateTime.parse(dateTimeStr, DATETIME_FMT);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid datetime: " + dateTimeStr + ". Expected format: YYYY-MM-DDTHH:mm");
    }
  }

  private Set<DayOfWeek> parseWeekdays(String token) {
    if (token == null || token.isEmpty()) {
      throw new IllegalArgumentException("Missing weekday pattern");
    }
    EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
    for (char c : token.toUpperCase().toCharArray()) {
      switch (c) {
        case 'M':
          set.add(DayOfWeek.MONDAY);
          break;
        case 'T':
          set.add(DayOfWeek.TUESDAY);
          break;
        case 'W':
          set.add(DayOfWeek.WEDNESDAY);
          break;
        case 'R':
          set.add(DayOfWeek.THURSDAY);
          break;
        case 'F':
          set.add(DayOfWeek.FRIDAY);
          break;
        case 'S':
          set.add(DayOfWeek.SATURDAY);
          break;
        case 'U':
          set.add(DayOfWeek.SUNDAY);
          break;
        default:
          throw new IllegalArgumentException("Invalid weekday letter: " + c);
      }
    }
    return set;
  }

  private static String unquote(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
      return t.substring(1, t.length() - 1);
    }
    return t;
  }

  private static String stripQuotes(String s) {
    if (s == null || s.length() < 2) {
      return s;
    }
    if (s.startsWith("\"") && s.endsWith("\"")) {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }

  private static int indexOfIgnoreCase(String haystack, String needle) {
    String h = haystack.toLowerCase();
    String n = needle.toLowerCase();
    return h.indexOf(n);
  }
}
