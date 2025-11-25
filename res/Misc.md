# Misc

## Design changes and justifications
- **Sorted event store with fast duplicate checks:** Swapped the calendar’s primary collection to a `NavigableSet` ordered by start/end/subject plus an `eventIndex` map for O(log n) traversal and O(1) uniqueness checks, improving date/range queries and edit validation without resorting every call.
- **All-day normalization and series-aware edits:** Missing end times are normalized to working hours (8am–5pm), and start-time edits detach single occurrences from their series while forward/series scopes retime occurrences with duration preservation and duplicate protection.
- **Rich Swing GUI in the view layer:** Added `CalendarFrame` month view with selectable days, event list, tooltips/detail dialogs, and dialogs for creating/editing events (required markers, editable time pickers, recurrence toggles, description visibility). UI stays in the view package to preserve MVC separation.
- **GUI controller bridging model and view with series metadata:** `GuiController` seeds a default calendar, keeps month/selection state, builds view models with series counts/spans for richer tooltips, and routes create/edit/export actions back to the model.
- **GUI-friendly event projections:** `EventViewModel` carries optional description/location and series statistics so tooltips and detail dialogs can surface recurrence metadata without exposing model internals.

## Features that work
- **All three execution modes:** GUI is the default when no args are provided; interactive and headless modes remain available via `--mode interactive` and `--mode headless <file>`, matching the spec’s launch requirements.
- **Multi-calendar with timezones:** Users can create/select calendars (GUI and CLI), with the header showing active name/zone; controller refreshes views in the correct zone and seeds a default calendar on startup.
- **Event creation (single and recurring):** GUI dialog validates required fields, supports editable time combos (all-day when blank), weekday-based recurrence with count or until date, and passes the rule to the controller; model enforces duplicate prevention and all-day normalization.
- **Event editing with scopes:** GUI edit dialog lets users pick multiple properties and scope (event/events/series); controller routes to model edits that enforce uniqueness and correct series semantics, detaching when start times diverge.
- **Exports:** CLI/headless export calendar to CSV/iCal based on extension; GUI delegates export via the controller when invoked.

## Known limitations / notes for graders
- **Copy operations are CLI/headless only:** Copy commands remain supported via text modes (`copy event`, `copy events on`, `copy events between`), but the Swing GUI does not expose copy controls per the updated GUI requirements.
- **GUI header trimmed to essentials:** The GUI removes rename/change-timezone/export buttons from the header; those actions are available via CLI and, for export, through the controller hook when invoked programmatically.
- **Event descriptions shown on demand:** Descriptions are not in the list row text but appear in tooltips and the double-click detail dialog for any selected event.
