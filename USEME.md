# USEME

## Build
- From the project root (same folder as `gradlew`), run:
  - `./gradlew clean jar`
- The assembled JAR is placed at `build/libs/calendar-1.0.jar`.

## Run modes
- **GUI (default / double-click)**
  - `java -jar build/libs/calendar-1.0.jar`
- **Interactive text mode**
  - `java -jar build/libs/calendar-1.0.jar --mode interactive`
- **Headless script mode**
  - `java -jar build/libs/calendar-1.0.jar --mode headless <path-to-script>`
  - Example: `java -jar build/libs/calendar-1.0.jar --mode headless res/commands.txt`

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
- **Opening**: Run the JAR with no arguments. A default calendar in your system timezone is created automatically if none exist.
- **Calendar controls** (top bar):
  - *Create calendar*: choose a name and timezone (e.g., `America/New_York`).
  - *Switch calendar*: use the dropdown; the active calendar name/timezone is shown in the header.
  - *Rename* / *Change timezone*: apply to the active calendar.
  - *Export*: choose a filename ending in `.csv` or `.ical` to export the active calendar.
- **Month view**:
  - Use **Prev** / **Next** to navigate months.
  - Click any day cell to select it; days with events are marked, and the selected day is highlighted.
- **Event list and actions** (right panel for the selected day):
  - *Create event*: enter subject, start/end times (defaults to the selected day), description/location/status, and recurrence (choose weekdays plus either occurrence count or until-date for series).
  - *Edit event*: pick the event, choose property and scope (single event, this-and-forward, or entire series), and apply changes.
  - *Copy*: copy a specific event, all events on the selected day, or a date range to another calendar and start date/time.
- **Error handling**: Invalid inputs (missing fields, bad times, timezone errors, conflicts) show dialog messages describing what to fix; stack traces are not shown.

## Headless scripts
- Scripts must contain one command per line and end with `exit`.
- File extension determines export format automatically when using `export cal <filename>` (e.g., `.csv` or `.ical`).

## Troubleshooting
- If a mode argument is incorrect, the program prints an error and exits.
- If the GUI does not open on double-click, run from a terminal with `java -jar build/libs/calendar-1.0.jar` to see any error message.
