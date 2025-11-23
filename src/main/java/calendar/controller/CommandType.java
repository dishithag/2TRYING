package calendar.controller;

/**
 * Command categories parsed from user input.
 */
public enum CommandType {
  EXIT,
  CREATE_CALENDAR,
  EDIT_CALENDAR,
  USE_CALENDAR,
  EXPORT,
  STATUS,
  PRINT_ON,
  PRINT_RANGE,
  CREATE_SINGLE,
  CREATE_SERIES,
  CREATE_SERIES_UNTIL,
  EDIT,
  COPY_EVENT,
  COPY_ON_DATE,
  COPY_BETWEEN
}
