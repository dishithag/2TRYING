# USEME

## Build
./gradlew clean jar

## Run (interactive)
java -jar build/libs/calendar-1.0.jar --mode interactive

Example (type one per line):
create calendar --name Fall --timezone America/New_York
use calendar --name Fall
create event "Lecture 1" from 2024-09-05T10:00 to 2024-09-05T11:00
print events on 2024-09-05
export cal fall_calendar.csv
export cal fall_calendar.ical
exit

## Run (headless)
java -jar build/libs/calendar-1.0.jar --mode headless res/commands.txt

Notes:
- Run from project root (same folder as `gradlew`).
- Scripts must have no blank lines. Format is auto-detected by extension (.csv / .ical).

## Checking recent changes
If you need to inspect the latest commit from the repository terminal (including IntelliJ's built-in terminal), run:

- `git log --oneline -n 3` to see the most recent commits and their hashes.
- `git show <commit-hash>` to view the diff for a specific commit from the list above.

This is useful if a shared hash looks missing—`git log` will show the correct hash present in your local clone.
