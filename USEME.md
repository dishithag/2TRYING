# USEME

## Build
- From the project root (same folder as `gradlew`), run:
  - `./gradlew clean jar`
- The assembled JAR is placed at `build/libs/calendar-1.0.jar`.
- Double-clicking the JAR opens the GUI; terminal commands are listed below.

## Run modes (from project root)
- **GUI (default / double-click)**
  - `java -jar build/libs/calendar-1.0.jar`
  - Behavior: opens the Swing month view; if no calendars exist, a default calendar is created in your system timezone.
- **Interactive text mode**
  - `java -jar build/libs/calendar-1.0.jar --mode interactive`
  - Behavior: prompts for commands one per line until `exit`.
- **Headless script mode**
  - `java -jar build/libs/calendar-1.0.jar --mode headless <path-to-script>`
  - Example: `java -jar build/libs/calendar-1.0.jar --mode headless res/commands.txt`
  - Behavior: executes commands from the file in order; the last command must be `exit`.

## Quick start (interactive CLI example)
```
create calendar --name Fall --timezone America/New_York
use calendar --name Fall
create event "Lecture 1" from 2024-09-05T10:00 to 2024-09-05T11:00
print events on 2024-09-05
export cal fall_calendar.csv
export cal fall_calendar.ical
exit
```

## GUI usage guide
- **Opening**: Run the JAR with no arguments. A default calendar in your system timezone is created automatically if none exist; the active calendar name and timezone appear in the header.
- **Calendar controls** (top bar):
  - *Create calendar*: enter name and timezone (IANA format like `America/New_York`).
  - *Switch calendar*: pick from the dropdown; the header updates to the active calendar.
  - *Rename* / *Change timezone*: acts on the active calendar.
  - *Export*: enter a filename ending in `.csv` or `.ical`; the absolute path is shown after export.
- **Month view**:
  - Use **Prev** / **Next** to change months.
  - Click any day cell to select it; days with events are marked with a dot and the selection is highlighted.
  - The right panel updates with events for the selected day in the active calendar’s timezone.
- **Create an event**:
  - Click **Create Event**; subject is required. Start/end default to the selected day.
  - Time pickers offer common 30-minute slots but are editable—type any `HH:mm` (e.g., `01:45`) or leave both blank for an all-day event.
  - Optional fields: description, location, status (public/private).
  - Recurring events: choose weekdays plus either number of occurrences or an end date.
- **Edit events**:
  - Select an event in the list, click **Edit**, choose which property to change and the scope (single event, this-and-forward, or entire series), then apply.
  - **Error handling**: Dialogs explain missing fields, bad times, unsupported timezones, or conflicts. Stack traces are never shown.

## Headless scripts
  - Scripts must contain one command per line and end with `exit`.
  - File extension determines export format automatically when using `export cal <filename>` (e.g., `.csv` or `.ical`).

## Troubleshooting
- If a mode argument is incorrect, the program prints an error and exits.
- If the GUI does not open on double-click, run from a terminal with `java -jar build/libs/calendar-1.0.jar` to see any error message.
