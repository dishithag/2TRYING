import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import calendar.controller.CommandParser;
import calendar.controller.CommandParser.Command;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.Test;

/**
 * Tests for parsing valid and invalid commands.
 */
public class CommandParserTest {

  private final CommandParser parser = new CommandParser();

  /**
   * A timed create event should parse subject, start and end.
   */
  @Test
  public void testParseTimedCreate() {
    Command c = parser.parse(
        "create event \"Team Standup\" from 2025-11-03T09:00 to 2025-11-03T09:30");
    assertEquals("create_single", c.getType());
    assertEquals("Team Standup", c.getSubject());
    assertEquals("2025-11-03T09:00", c.getStartDateTime().toString());
    assertEquals("2025-11-03T09:30", c.getEndDateTime().toString());
  }

  /**
   * An all day repeating event should carry weekdays and occurrences.
   */
  @Test
  public void testParseAllDayRepeating() {
    Command c = parser.parse(
        "create event Holiday on 2025-11-05 repeats WF for 4 times");
    assertEquals("create_series", c.getType());
    assertEquals("Holiday", c.getSubject());
    assertTrue(c.getWeekdays().contains(DayOfWeek.WEDNESDAY));
    assertTrue(c.getWeekdays().contains(DayOfWeek.FRIDAY));
    assertEquals(Integer.valueOf(4), c.getOccurrences());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPrintRange() {
    parser.parse("print events from 2025-11-01T10:00 to-bad");
  }

  /**
   * Invalid edit syntax should throw.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidEdit() {
    parser.parse("edit event subject Meeting 2025-11-01T10:00");
  }

  /**
   * Export command should capture filename.
   */
  @Test
  public void testExportCommand() {
    Command c = parser.parse("export cal res/calendar.csv");
    assertEquals("export", c.getType());
    assertEquals("res/calendar.csv", c.getFileName());
  }


  /**
   * Parses print range command.
   */

  @Test
  public void testParsePrintRange() {
    CommandParser.Command c = parser.parse(
        "print events from 2025-11-03T00:00 to 2025-11-05T23:59");
    assertEquals("print_range", c.getType());
    assertEquals(LocalDateTime.of(2025, 11, 3, 0, 0), c.getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 5, 23, 59), c.getEndDateTime());
  }

  /**
   * Parses export calendar command.
   */
  @Test
  public void testParseExport() {
    CommandParser.Command c = parser.parse("export cal res/calendar.csv");
    assertEquals("export", c.getType());
    assertEquals("res/calendar.csv", c.getFileName());
  }

  /**
   * Test parsing timed event with invalid syntax throws exception.
   * Tests if (!matcher.matches()) branch.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseTimedEventInvalidSyntax() {
    CommandParser parser = new CommandParser();

    parser.parse("create event \"Meeting\" from 2025-11-03T10:00 2025-11-03T11:00");
  }

  /**
   * Test parsing null command throws exception.
   * Tests the line == null branch.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseNullCommand() {
    parser.parse(null);
  }


  /**
   * Test creating a simple all-day event (parts.length == 1).
   * Covers the case where there's no "repeats" clause.
   */
  @Test
  public void testCreateSimpleAllDayEvent() {
    Command cmd = parser.parse("create event Meeting on 2025-11-10");

    assertEquals("create_single", cmd.getType());
    assertEquals("Meeting", cmd.getSubject());
    assertEquals(LocalDateTime.of(2025, 11, 10, 8, 0), cmd.getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 10, 17, 0), cmd.getEndDateTime());
  }

  /**
   * Test creating a recurring all-day event with "for X times".
   * Covers: parts.length >= 4 && parts[1].equalsIgnoreCase("repeats")
   * and parts[3].equalsIgnoreCase("for") && parts.length >= 6
   */
  @Test
  public void testCreateAllDayEventRepeatsForOccurrences() {
    Command cmd = parser.parse("create event Standup on 2025-11-10 repeats MWF for 5 times");

    assertEquals("create_series", cmd.getType());
    assertEquals("Standup", cmd.getSubject());
    assertEquals(LocalDateTime.of(2025, 11, 10, 8, 0), cmd.getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 10, 17, 0), cmd.getEndDateTime());
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.MONDAY));
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.WEDNESDAY));
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.FRIDAY));
    assertEquals(Integer.valueOf(5), cmd.getOccurrences());
  }

  /**
   * Test creating a recurring all-day event with "until DATE".
   * Covers: parts.length >= 4 && parts[1].equalsIgnoreCase("repeats")
   * and parts[3].equalsIgnoreCase("until") && parts.length >= 5
   */
  @Test
  public void testCreateAllDayEventRepeatsUntilDate() {
    Command cmd = parser.parse("create event Review on 2025-11-10 repeats TR until 2025-12-31");

    assertEquals("create_series_until", cmd.getType());
    assertEquals("Review", cmd.getSubject());
    assertEquals(LocalDateTime.of(2025, 11, 10, 8, 0), cmd.getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 10, 17, 0), cmd.getEndDateTime());
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.TUESDAY));
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.THURSDAY));
    assertEquals(LocalDate.of(2025, 12, 31), cmd.getUntilDate());
  }

  /**
   * Test invalid all-day event syntax - has "repeats" but missing "for" or "until".
   * Covers the final throw statement when repeats syntax is incomplete.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateAllDayEventInvalidRepeatsSyntax() {
    parser.parse("create event Meeting on 2025-11-10 repeats MWF");
  }

  /**
   * Test invalid all-day event syntax - has "repeats for" but not enough parts.
   * Covers: parts[3].equalsIgnoreCase("for") but parts.length < 6
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateAllDayEventRepeatsForIncompleteSyntax() {
    parser.parse("create event Meeting on 2025-11-10 repeats MWF for");
  }

  /**
   * Test invalid all-day event syntax - has "repeats until" but not enough parts.
   * Covers: parts[3].equalsIgnoreCase("until") but parts.length < 5
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateAllDayEventRepeatsUntilIncompleteSyntax() {
    parser.parse("create event Meeting on 2025-11-10 repeats MWF until");
  }

  /**
   * Test invalid all-day event syntax - parts.length >= 4 but parts[1] is not "repeats".
   * This should fall through to the final throw statement.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateAllDayEventInvalidSecondKeyword() {
    parser.parse("create event Meeting on 2025-11-10 something MWF extra stuff");
  }

  /**
   * Test with quoted subject in all-day event.
   */
  @Test
  public void testCreateAllDayEventWithQuotedSubject() {
    Command cmd = parser.parse("create event \"Team Meeting\" on 2025-11-10");

    assertEquals("create_single", cmd.getType());
    assertEquals("Team Meeting", cmd.getSubject());
    assertEquals(LocalDateTime.of(2025, 11, 10, 8, 0), cmd.getStartDateTime());
  }


  /**
   * Test creating a timed event (not all-day).
   */
  @Test
  public void testCreateTimedEvent() {
    Command cmd = parser.parse("create event Meeting from 2025-11-10T15:00 to 2025-11-10T16:00");

    assertEquals("create_single", cmd.getType());
    assertEquals("Meeting", cmd.getSubject());
    assertEquals(LocalDateTime.of(2025, 11, 10, 15, 0), cmd.getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 10, 16, 0), cmd.getEndDateTime());
  }

  /**
   * Test creating a recurring timed event with "for X times".
   */
  @Test
  public void testCreateTimedEventRepeatsFor() {
    Command cmd = parser.parse("create event Standup from 2025-11-10T09:00 to "
        + "2025-11-10T09:30 repeats "
        + "MWF for 10 times");

    assertEquals("create_series", cmd.getType());
    assertEquals("Standup", cmd.getSubject());
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.MONDAY));
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.WEDNESDAY));
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.FRIDAY));
    assertEquals(Integer.valueOf(10), cmd.getOccurrences());
  }

  /**
   * Test creating a recurring timed event with "until DATE".
   */
  @Test
  public void testCreateTimedEventRepeatsUntil() {
    Command cmd = parser.parse("create event Review from 2025-11-10T1"
        + "4:00 to 2025-11-10T15:00 repeats TR until 2025-12-31");

    assertEquals("create_series_until", cmd.getType());
    assertEquals("Review", cmd.getSubject());
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.TUESDAY));
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.THURSDAY));
    assertEquals(LocalDate.of(2025, 12, 31), cmd.getUntilDate());
  }

  /**
   * "edit event" with only a property but no rest -> parts.length < 2 branch.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditMissingRest() {
    parser.parse("edit event description");
  }

  /**
   * edit with unclosed quote in subject -> goes to "Unclosed quote" branch.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditUnclosedQuotedSubject() {
    parser.parse(
        "edit event location \"Daily standup from 2025-11-03T09:00 with Zoom");
  }

  /**
   * edit where subject is unquoted and there is no 'from ' -> "Missing 'from'" branch.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditMissingFromForUnquotedSubject() {
    parser.parse(
        "edit event location DailyStandup 2025-11-03T09:00 with Zoom");
  }

  /**
   * edit where pattern matches "from ...", but there is no "with ..." -> invalid edit syntax.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditMissingWithClause() {
    parser.parse(
        "edit event location DailyStandup from 2025-11-03T09:00");
  }

  /**
   * Happy edit path that uses quoted subject AND quoted new value.
   * This hits the branch:
   *   if (newValue.startsWith("\"") && newValue.endsWith("\"")) { ... }
   */
  @Test
  public void testEditQuotedSubjectAndQuotedValue() {
    CommandParser.Command c = parser.parse(
        "edit event description \"Daily standup\" from 2025-11-03T09:00 with \"Updated desc\"");
    assertEquals("edit", c.getType());
    assertEquals("description", c.getProperty());
    assertEquals("Daily standup", c.getSubject());
    assertEquals("Updated desc", c.getNewValue());
    assertEquals(LocalDateTime.of(2025, 11, 3, 9, 0), c.getStartDateTime());
  }

  /**
   * edit with bad datetime -> forces parseDateTime(...) to throw.
   * This covers the catch block at the bottom of parseDateTime.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditBadDateTime() {
    parser.parse(
        "edit event location Meeting from 2025-11-03T09 to ZoomRoom");
  }

  /**
   * all-day create with wrong date format -> covers parseDate(...) catch.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateAllDayBadDateFormat() {
    parser.parse("create event Meeting on 11-10-2025");
  }

  /**
   * timed create with wrong datetime format -> covers parseDateTime(...) catch.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateTimedBadDateTimeFormat() {
    parser.parse("create event Meeting from 2025-11-10 15:00 to 2025-11-10T16:00");
  }

  /**
   * top-level parse: line is only whitespace -> hits (line == null || line.trim().isEmpty()).
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseWhitespaceOnly() {
    parser.parse("   ");
  }

  /**
   * Test parsing weekday pattern with Saturday.
   */
  @Test
  public void testParseWeekdays_saturday() {
    Command cmd = parser.parse(
        "create event Weekend on 2025-11-08 repeats S for 2 times");

    assertEquals("create_series", cmd.getType());
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.SATURDAY));
    assertEquals(1, cmd.getWeekdays().size());
  }

  /**
   * Test parsing weekday pattern with Sunday.
   */
  @Test
  public void testParseWeekdays_sunday() {
    Command cmd = parser.parse(
        "create event Brunch on 2025-11-09 repeats U for 2 times");

    assertEquals("create_series", cmd.getType());
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.SUNDAY));
    assertEquals(1, cmd.getWeekdays().size());
  }

  /**
   * Test parsing weekday pattern with Saturday and Sunday.
   */
  @Test
  public void testParseWeekdays_weekend() {
    Command cmd = parser.parse(
        "create event Weekend on 2025-11-08 repeats SU for 2 times");

    assertEquals("create_series", cmd.getType());
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.SATURDAY));
    assertTrue(cmd.getWeekdays().contains(DayOfWeek.SUNDAY));
    assertEquals(2, cmd.getWeekdays().size());
  }

  /**
   * Test parsing invalid weekday letter throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseWeekdays_invalidLetter() {
    parser.parse("create event Bad on 2025-11-08 repeats MXZ for 2 times");
  }

  /**
   * Test parsing create calendar command uses extended constructor.
   */
  @Test
  public void testParseCreateCalendar() {
    Command cmd = parser.parse("create calendar --name "
        + "\"My Calendar\" --timezone America/New_York");

    assertEquals("create_calendar", cmd.getType());
    assertEquals("My Calendar", cmd.getCalendarName());
    assertEquals("America/New_York", cmd.getTimezoneId());
  }

  /**
   * Test parsing create calendar without quotes.
   */
  @Test
  public void testParseCreateCalendar_noQuotes() {
    Command cmd = parser.parse("create calendar --name MyCalendar --timezone Europe/London");

    assertEquals("create_calendar", cmd.getType());
    assertEquals("MyCalendar", cmd.getCalendarName());
    assertEquals("Europe/London", cmd.getTimezoneId());
  }

  /**
   * Test parsing edit calendar name command.
   */
  @Test
  public void testParseEditCalendar_name() {
    Command cmd = parser.parse("edit calendar --name \"Old Name\" --property name \"New Name\"");

    assertEquals("edit_calendar", cmd.getType());
    assertEquals("Old Name", cmd.getCalendarName());
    assertEquals("name", cmd.getProperty());
    assertEquals("New Name", cmd.getNewValue());
  }

  /**
   * Test parsing edit calendar timezone command.
   */
  @Test
  public void testParseEditCalendar_timezone() {
    Command cmd = parser.parse("edit calendar --name MyCalendar"
        + " --property timezone America/Los_Angeles");

    assertEquals("edit_calendar", cmd.getType());
    assertEquals("MyCalendar", cmd.getCalendarName());
    assertEquals("timezone", cmd.getProperty());
    assertEquals("America/Los_Angeles", cmd.getNewValue());
  }

  /**
   * Test parsing use calendar command.
   */
  @Test
  public void testParseUseCalendar() {
    Command cmd = parser.parse("use calendar --name \"Work Calendar\"");

    assertEquals("use_calendar", cmd.getType());
    assertEquals("Work Calendar", cmd.getCalendarName());
  }

  /**
   * Test parsing use calendar without quotes.
   */
  @Test
  public void testParseUseCalendar_noQuotes() {
    Command cmd = parser.parse("use calendar --name WorkCalendar");

    assertEquals("use_calendar", cmd.getType());
    assertEquals("WorkCalendar", cmd.getCalendarName());
  }


  /**
   * Test parsing print events on command sets startDateTime.
   */
  @Test
  public void testParsePrintOn_usesDay() {
    Command cmd = parser.parse("print events on 2025-11-15");

    assertEquals("print_on", cmd.getType());
    assertNotNull(cmd.getStartDateTime());
    assertEquals(LocalDate.of(2025, 11, 15), cmd.getStartDateTime().toLocalDate());
  }

  /**
   * Test parsing print range uses rangeStart and rangeEnd fields.
   */
  @Test
  public void testParsePrintRange_usesRangeFields() {
    Command cmd = parser.parse("print events from 2025-11-01T00:00 to 2025-11-30T23:59");

    assertEquals("print_range", cmd.getType());
    assertNotNull(cmd.getStartDateTime());
    assertNotNull(cmd.getEndDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 1, 0, 0), cmd.getStartDateTime());
    assertEquals(LocalDateTime.of(2025, 11, 30, 23, 59), cmd.getEndDateTime());
  }

  /**
   * Test create calendar with invalid syntax throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseCreateCalendar_missingName() {
    parser.parse("create calendar --timezone America/New_York");
  }

  /**
   * Test create calendar with missing timezone throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseCreateCalendar_missingTimezone() {
    parser.parse("create calendar --name MyCalendar");
  }

  /**
   * Test edit calendar with invalid property throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseEditCalendar_invalidProperty() {
    parser.parse("edit calendar --name MyCalendar --property color blue");
  }

  /**
   * Test edit calendar with missing name throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseEditCalendar_missingName() {
    parser.parse("edit calendar --property name NewName");
  }

  /**
   * Test edit calendar with missing property throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseEditCalendar_missingProperty() {
    parser.parse("edit calendar --name MyCalendar");
  }

  /**
   * Test use calendar with missing name throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseUseCalendar_missingName() {
    parser.parse("use calendar");
  }

  /**
   * Test parsing copy event with quoted event name and unclosed quote throws.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseCopyEvent_unclosedQuote_throws() {
    parser.parse("copy event \"Meeting on 2025-11-10T09:00 --target target to 2025-11-15T14:00");
  }

  /**
   * Test parsing copy event without 'on' keyword throws.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseCopyEvent_missingOn_throws() {
    parser.parse("copy event Meeting 2025-11-10T09:00 --target target to 2025-11-15T14:00");
  }

  /**
   * Test parsing copy event with invalid syntax throws.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseCopyEvent_invalidSyntax_throws() {
    parser.parse("copy event Meeting on 2025-11-10T09:00 target to 2025-11-15T14:00");
  }

  /**
   * Test parsing copy event with quoted event name.
   */
  @Test
  public void testParseCopyEvent_quotedName() {
    CommandParser.Command cmd = parser.parse(
        "copy event \"Team Meeting\" on 2025-11-10T09:00 "
            + "--target \"Work Calendar\" to 2025-11-15T14:00");

    assertEquals("copy_event", cmd.getType());
    assertEquals("Team Meeting", cmd.getSubject());
    assertEquals(LocalDateTime.of(2025, 11, 10, 9, 0), cmd.getStartDateTime());
    assertEquals("Work Calendar", cmd.getTargetCalendar());
    assertEquals(LocalDateTime.of(2025, 11, 15, 14, 0), cmd.getTargetDateTime());
  }

  /**
   * Test parsing copy event with unquoted names.
   */
  @Test
  public void testParseCopyEvent_unquotedNames() {
    CommandParser.Command cmd = parser.parse(
        "copy event Meeting on 2025-11-10T09:00 --target target to 2025-11-15T14:00");

    assertEquals("copy_event", cmd.getType());
    assertEquals("Meeting", cmd.getSubject());
    assertEquals(LocalDateTime.of(2025, 11, 10, 9, 0), cmd.getStartDateTime());
    assertEquals("target", cmd.getTargetCalendar());
    assertEquals(LocalDateTime.of(2025, 11, 15, 14, 0), cmd.getTargetDateTime());
  }

  /**
   * Test parseDateTime with invalid format throws exception.
   * Covers the catch block in parseDateTime.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseDateTime_invalidFormat_throws() {
    parser.parse("show status on 2025-11-10T25:99");
  }

  /**
   * Test parseWeekdays with null token throws exception.
   * Uses reflection to test private method.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseWeekdays_nullToken_throws() throws Exception {
    java.lang.reflect.Method method = CommandParser.class.getDeclaredMethod(
        "parseWeekdays", String.class);
    method.setAccessible(true);

    try {
      method.invoke(parser, (String) null);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw (IllegalArgumentException) e.getCause();
    }
  }

  /**
   * Test parseWeekdays with empty token throws exception.
   * Uses reflection to test private method.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParseWeekdays_emptyToken_throws() throws Exception {
    java.lang.reflect.Method method = CommandParser.class.getDeclaredMethod(
        "parseWeekdays", String.class);
    method.setAccessible(true);

    try {
      method.invoke(parser, "");
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw (IllegalArgumentException) e.getCause();
    }
  }

  /**
   * Test EditScope.fromToken with valid tokens.
   */
  @Test
  public void testEditScopeFromToken_event() {
    assertEquals(calendar.controller.EditScope.EVENT,
        calendar.controller.EditScope.fromToken("event"));
  }

  /**
   * Test EditScope.fromToken with events.
   */
  @Test
  public void testEditScopeFromToken_events() {
    assertEquals(calendar.controller.EditScope.EVENTS,
        calendar.controller.EditScope.fromToken("events"));
  }

  /**
   * Test EditScope.fromToken with series.
   */
  @Test
  public void testEditScopeFromToken_series() {
    assertEquals(calendar.controller.EditScope.SERIES,
        calendar.controller.EditScope.fromToken("series"));
  }

  /**
   * Test EditScope.fromToken with null throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditScopeFromToken_null_throws() {
    calendar.controller.EditScope.fromToken(null);
  }

  /**
   * Test EditScope.fromToken with unknown token throws exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testEditScopeFromToken_unknown_throws() {
    calendar.controller.EditScope.fromToken("invalid");
  }

  /**
   * Test EditScope.fromToken is case insensitive.
   */
  @Test
  public void testEditScopeFromToken_caseInsensitive() {
    assertEquals(calendar.controller.EditScope.EVENT,
        calendar.controller.EditScope.fromToken("EVENT"));
    assertEquals(calendar.controller.EditScope.EVENTS,
        calendar.controller.EditScope.fromToken("EVENTS"));
    assertEquals(calendar.controller.EditScope.SERIES,
        calendar.controller.EditScope.fromToken("SERIES"));
  }


  @Test(expected = IllegalArgumentException.class)
  public void testParseCreateEvent_unclosedQuote_throws() {
    parser.parse("create event \"Unclosed quote on 2025-11-10");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseCreateEvent_noFromOrOn_throws() {
    parser.parse("create event Meeting at 2025-11-10T09:00");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseCreateEvent_invalidSyntax_throws() {
    parser.parse("create event Meeting");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseCreateEvent_remainingInvalidSyntax_throws() {
    parser.parse("create event Meeting something 2025-11-10");
  }

  @Test
  public void testParseCopyOnDate_quotedCalendar() {
    Command cmd = parser.parse(
        "copy events on 2025-11-10 --target \"My Calendar\" to 2025-11-15");

    assertEquals("copy_on_date", cmd.getType());
    assertEquals("My Calendar", cmd.getTargetCalendar());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseCopyOnDate_invalidSyntax_throws() {
    parser.parse("copy events on 2025-11-10 target to 2025-11-15");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseCopyBetween_invalidSyntax_throws() {
    parser.parse("copy events between 2025-11-10 2025-11-15 --target cal to 2025-11-20");
  }

  @Test(expected = IllegalArgumentException.class)

  public void testParseCreateEvent_quotedSubject_unclosedQuote_throws() {

    parser.parse("create event \"Meeting without closing quote"
        +
        " from 2025-11-10T09:00 to 2025-11-10T10:00");

  }

  @Test(expected = IllegalArgumentException.class)

  public void testParseCreateEvent_unquotedSubject_noFromOrOn_throws() {

    parser.parse("create event MeetingWithoutFromOrOn 2025-11-10T09:00");

  }

  @Test(expected = IllegalArgumentException.class)

  public void testParseCreateEvent_noKeywords_throws() {

    parser.parse("create event Meeting");

  }

  @Test(expected = IllegalArgumentException.class)

  public void testParseCreateEvent_invalidRemainingPart_throws() {

    parser.parse("create event \"Meeting\" something else");

  }

  @Test

  public void testParseCreateEvent_quotedSubject_withOn() {

    Command cmd = parser.parse("create event \"Team Meeting\" on 2025-11-10");

    assertEquals("create_single", cmd.getType());

    assertEquals("Team Meeting", cmd.getSubject());

    assertEquals(LocalDateTime.of(2025, 11, 10, 8, 0), cmd.getStartDateTime());

  }

  @Test

  public void testParseCreateEvent_quotedSubject_withFrom() {

    Command cmd = parser.parse("create event \"Team Meeting\" "
        +
        "from 2025-11-10T09:00 to 2025-11-10T10:00");

    assertEquals("create_single", cmd.getType());

    assertEquals("Team Meeting", cmd.getSubject());

    assertEquals(LocalDateTime.of(2025, 11, 10, 9, 0), cmd.getStartDateTime());

  }

  @Test

  public void testParseCreateEvent_unquotedSubject_withFrom() {

    Command cmd = parser.parse("create event Meeting from 2025-11-10T09:00 to 2025-11-10T10:00");

    assertEquals("create_single", cmd.getType());

    assertEquals("Meeting", cmd.getSubject());

  }

  @Test

  public void testParseCreateEvent_unquotedSubject_withOn() {

    Command cmd = parser.parse("create event Holiday on 2025-11-10");

    assertEquals("create_single", cmd.getType());

    assertEquals("Holiday", cmd.getSubject());

  }
}
