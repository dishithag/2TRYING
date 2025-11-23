package calendar;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of a {@link Calendar}.
 * Stores events in memory and supports single events and series with simple edits.
 * All LocalDateTime values are interpreted in this calendar's ZoneId.
 */
public class CalendarImpl implements Calendar {

  private final List<Event> events;
  private final SeriesIndex seriesIndex;

  private String name;
  private ZoneId zoneId;

  /**
   * Creates an empty calendar named "default" in America/New_York time zone.
   */
  public CalendarImpl() {
    this("default", ZoneId.of("America/New_York"));
  }

  /**
   * Creates an empty calendar with the given name and time zone.
   *
   * @param name   calendar name
   * @param zoneId calendar ZoneId
   */
  public CalendarImpl(String name, ZoneId zoneId) {
    if (name == null || name.isBlank() || zoneId == null) {
      throw new IllegalArgumentException("Name and ZoneId are required");
    }
    this.name = name;
    this.zoneId = zoneId;
    this.events = new ArrayList<>();
    this.seriesIndex = new SeriesIndex();
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void rename(String newName) {
    if (newName == null || newName.isBlank()) {
      throw new IllegalArgumentException("Name cannot be blank");
    }
    this.name = newName;
  }

  @Override
  public ZoneId getZoneId() {
    return this.zoneId;
  }

  @Override
  public void setZoneId(ZoneId zone) {
    if (zone == null) {
      throw new IllegalArgumentException("ZoneId cannot be null");
    }
    if (!zone.equals(this.zoneId)) {
      convertAllEventsToZone(this.zoneId, zone);
      this.zoneId = zone;
      seriesIndex.rebuild(events);
    }
  }

  @Override
  public Event createEvent(String subject, LocalDateTime start, LocalDateTime end) {
    if (eventExists(subject, start, end)) {
      throw new IllegalArgumentException(
          "Event with same subject, start, and end already exists");
    }
    Event event = new EventBuilder()
        .subject(subject)
        .startDateTime(start)
        .endDateTime(end)
        .build();
    events.add(event);
    event.getSeriesId().ifPresent(id -> seriesIndex.add(id, event.getStartDateTime()));
    return event;
  }

  @Override
  public List<Event> createEventSeries(String subject,
                                       LocalDateTime start,
                                       LocalDateTime end,
                                       Set<DayOfWeek> weekdays,
                                       int occurrences) {
    if (occurrences <= 0) {
      throw new IllegalArgumentException("Occurrences must be positive");
    }
    validateSeriesInstanceShape(start, end, weekdays);

    String seriesId = generateSeriesId();
    List<Event> created = createSeries(
        subject, start, end, weekdays, seriesId, occurrences, null
    );
    for (Event e : created) {
      seriesIndex.add(seriesId, e.getStartDateTime());
    }
    return created;
  }

  @Override
  public List<Event> createEventSeriesUntil(String subject,
                                            LocalDateTime start,
                                            LocalDateTime end,
                                            Set<DayOfWeek> weekdays,
                                            LocalDate endDate) {
    validateSeriesInstanceShape(start, end, weekdays);

    String seriesId = generateSeriesId();
    List<Event> created = createSeries(
        subject, start, end, weekdays, seriesId, Integer.MAX_VALUE, endDate
    );
    for (Event e : created) {
      seriesIndex.add(seriesId, e.getStartDateTime());
    }
    return created;
  }

  @Override
  public List<Event> findEvents(String subject, LocalDateTime start, LocalDateTime end) {
    return events.stream()
        .filter(e -> e.getSubject().equals(subject))
        .filter(e -> e.getStartDateTime().equals(start))
        .filter(e -> end == null || e.getEndDateTime().equals(end))
        .collect(Collectors.toList());
  }

  @Override
  public List<Event> findEvents(String subject, LocalDateTime start) {
    return findEvents(subject, start, null);
  }

  @Override
  public void editEvent(String subject, LocalDateTime start,
                        String property, String newValue) {
    List<Event> matches = findEvents(subject, start, null);
    if (matches.isEmpty()) {
      throw new IllegalArgumentException("No event found with given criteria");
    }
    if (matches.size() > 1) {
      throw new IllegalArgumentException("Multiple events match criteria");
    }
    Event target = matches.get(0);
    EventProperty prop = parseProperty(property);
    Event updated = applyProperty(target, prop, newValue);
    enforceNoDuplicateOnReplace(target, updated);
    replaceEvent(target, updated);
  }

  /**
   * Edits a single event identified by {@code subject} and {@code start}.
   *
   * <p>Typed convenience overload: for {@link EventProperty#START} or {@link EventProperty#END}
   * supply {@code newDateTime}; for all other properties supply {@code newText}.</p>
   *
   * @param subject     subject of the existing event to edit
   * @param start       exact start timestamp of the event to disambiguate
   * @param property    which event field to change
   * @param newDateTime new timestamp when {@code property} is START or END; otherwise ignored
   * @param newText     new textual value when {@code property} is not a time field;
   *                    otherwise ignored
   * @throws IllegalArgumentException if the event is not found/ambiguous, the new value is
   *                                  missing or invalid,
   *                                  or the change would create a duplicate event.
   */

  public void editEvent(String subject,
                        LocalDateTime start,
                        EventProperty property,
                        LocalDateTime newDateTime,
                        String newText) {
    String val = coerceValue(property, newDateTime, newText);
    editEvent(subject, start, property.name().toLowerCase(), val);
  }

  @Override
  public void editEventsFromDate(String subject, LocalDateTime start,
                                 String property, String newValue) {
    List<Event> matches = findEvents(subject, start, null);
    if (matches.isEmpty()) {
      throw new IllegalArgumentException("No event found");
    }

    Event pivot = matches.get(0);
    if (!pivot.isSeriesPart()) {
      EventProperty p = parseProperty(property);
      Event updated = applyProperty(pivot, p, newValue);
      enforceNoDuplicateOnReplace(pivot, updated);
      replaceEvent(pivot, updated);
      return;
    }

    String originalSeriesId = pivot.getSeriesId().orElse(null);
    List<Event> seriesToEdit = events.stream()
        .filter(e -> e.getSeriesId().map(originalSeriesId::equals).orElse(false))
        .filter(e -> !e.getStartDateTime().isBefore(start))
        .collect(Collectors.toList());

    EventProperty prop = parseProperty(property);

    if (prop == EventProperty.START) {
      String newSeriesId = generateSeriesId();
      for (Event e : seriesToEdit) {
        LocalDateTime templ = LocalDateTime.parse(newValue);
        Duration duration = Duration.between(
            e.getStartDateTime().toLocalTime(), e.getEndDateTime().toLocalTime()
        );
        LocalDateTime adjustedStart = e.getStartDateTime()
            .toLocalDate()
            .atTime(templ.toLocalTime());
        LocalDateTime adjustedEnd = adjustedStart.plus(duration);

        Event modified = EventBuilder.from(e)
            .startDateTime(adjustedStart)
            .endDateTime(adjustedEnd)
            .seriesId(newSeriesId)
            .build();

        enforceNoDuplicateOnReplace(e, modified);
        replaceEvent(e, modified);
      }
      return;
    }

    for (Event e : seriesToEdit) {
      Event modified = applyProperty(e, prop, newValue);
      enforceNoDuplicateOnReplace(e, modified);
      replaceEvent(e, modified);
    }
  }

  /**
   * Edits events from (and including) {@code start} going forward for the given {@code subject}.
   *
   * <p>If the matched event is not part of a series, only that one event is edited.
   * If it is part of a series, the change is applied to that occurrence and all future
   * occurrences in the same series. For time fields (START/END) use {@code newDateTime};
   * for other fields use {@code newText}.</p>
   *
   * @param subject     subject used to locate the series or single event
   * @param start       pivot start timestamp (inclusive) from which edits apply
   * @param property    which event field to change
   * @param newDateTime new timestamp when {@code property} is START or END; otherwise ignored
   * @param newText     new textual value when {@code property} is not a time field;
   *                    otherwise ignored
   * @throws IllegalArgumentException if no matching event is found, a value is missing/invalid,
   *                                  or the change would create a duplicate event.
   */

  public void editEventsFromDate(String subject,
                                 LocalDateTime start,
                                 EventProperty property,
                                 LocalDateTime newDateTime,
                                 String newText) {
    String val = coerceValue(property, newDateTime, newText);
    editEventsFromDate(subject, start, property.name().toLowerCase(), val);
  }

  @Override
  public void editSeries(String subject, LocalDateTime start,
                         String property, String newValue) {
    List<Event> matches = findEvents(subject, start, null);
    if (matches.isEmpty()) {
      throw new IllegalArgumentException("No event found");
    }

    Event target = matches.get(0);
    String seriesId = target.getSeriesId().orElse(null);

    if (seriesId == null) {
      EventProperty p = parseProperty(property);
      Event updated = applyProperty(target, p, newValue);
      enforceNoDuplicateOnReplace(target, updated);
      replaceEvent(target, updated);
      return;
    }

    EventProperty prop = parseProperty(property);

    if (prop == EventProperty.START) {
      LocalDateTime templ = LocalDateTime.parse(newValue);
      for (Event e : getEventsBySeriesId(seriesId)) {
        java.time.Duration dur = java.time.Duration.between(
            e.getStartDateTime(), e.getEndDateTime());
        LocalDateTime newStart = e.getStartDateTime()
            .toLocalDate()
            .atTime(templ.toLocalTime());
        LocalDateTime newEnd = newStart.plus(dur);
        Event updated = EventBuilder.from(e)
            .startDateTime(newStart)
            .endDateTime(newEnd)
            .build();
        enforceNoDuplicateOnReplace(e, updated);
        replaceEvent(e, updated);
      }
      return;
    }

    for (Event e : getEventsBySeriesId(seriesId)) {
      Event updated = applyProperty(e, prop, newValue);
      enforceNoDuplicateOnReplace(e, updated);
      replaceEvent(e, updated);
    }
  }

  /**
   * Edits an entire series (or the single event if it is not in a series) identified by
   * {@code subject} and the occurrence {@code start}.
   *
   * <p>For time fields ({@link EventProperty#START} or {@link EventProperty#END})
   * supply {@code newDateTime}. For all other fields, supply {@code newText}.
   * The non-applicable parameter is ignored.</p>
   *
   * @param subject     subject used to locate the series
   * @param start       start timestamp of a known occurrence (identifies the series)
   * @param property    which event field to change
   * @param newDateTime new timestamp when {@code property} is START or END;
   *                    otherwise ignored
   * @param newText     new textual value when {@code property} is not a time field;
   *                    otherwise ignored
   * @throws IllegalArgumentException if no matching event is found, the new value is
   *                                  missing/invalid, or the change would create a duplicate event.
   */

  public void editSeries(String subject,
                         LocalDateTime start,
                         EventProperty property,
                         LocalDateTime newDateTime,
                         String newText) {
    String val = coerceValue(property, newDateTime, newText);
    editSeries(subject, start, property.name().toLowerCase(), val);
  }

  @Override
  public List<Event> getEventsOnDate(LocalDate date) {
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

    return events.stream()
        .filter(e -> !e.getEndDateTime().isBefore(startOfDay)
            && e.getStartDateTime().isBefore(endOfDay))
        .sorted(Comparator.comparing(Event::getStartDateTime))
        .collect(Collectors.toList());
  }

  @Override
  public List<Event> getEventsInRange(LocalDateTime start, LocalDateTime end) {
    return events.stream()
        .filter(e -> !e.getEndDateTime().isBefore(start)
            && !e.getStartDateTime().isAfter(end))
        .sorted(Comparator.comparing(Event::getStartDateTime))
        .collect(Collectors.toList());
  }

  @Override
  public boolean isBusyAt(LocalDateTime dateTime) {
    return events.stream().anyMatch(e ->
        !dateTime.isBefore(e.getStartDateTime())
            && dateTime.isBefore(e.getEndDateTime()));
  }

  @Override
  public List<Event> getAllEvents() {
    return new ArrayList<>(events);
  }

  private List<Event> createSeries(String subject,
                                   LocalDateTime start,
                                   LocalDateTime end,
                                   Set<DayOfWeek> repeatDays,
                                   String seriesId,
                                   int maxOccurrences,
                                   LocalDate untilDate) {
    List<Event> created = new ArrayList<>();
    LocalDateTime currentStart = start;
    LocalDateTime currentEnd = end;
    int count = 0;
    Set<DayOfWeek> days = new HashSet<>(repeatDays);

    while (true) {
      LocalDate currentDate = currentStart.toLocalDate();
      if (untilDate != null && currentDate.isAfter(untilDate)) {
        break;
      }
      if (days.contains(currentStart.getDayOfWeek())) {
        if (!eventExists(subject, currentStart, currentEnd)) {
          Event event = new EventBuilder()
              .subject(subject)
              .startDateTime(currentStart)
              .endDateTime(currentEnd)
              .seriesId(seriesId)
              .build();
          events.add(event);
          created.add(event);
          count++;
          if (count >= maxOccurrences) {
            break;
          }
        }
      }
      currentStart = currentStart.plusDays(1);
      if (currentEnd != null) {
        currentEnd = currentEnd.plusDays(1);
      }
    }
    return created;
  }

  private boolean eventExists(String subject, LocalDateTime start, LocalDateTime end) {
    LocalDateTime actualStart =
        (end == null) ? start.toLocalDate().atTime(WorkingHours.START) : start;
    LocalDateTime actualEnd =
        (end == null) ? start.toLocalDate().atTime(WorkingHours.END) : end;

    return events.stream().anyMatch(e ->
        e.getSubject().equals(subject)
            && e.getStartDateTime().equals(actualStart)
            && e.getEndDateTime().equals(actualEnd));
  }

  private String generateSeriesId() {
    return "SERIES_" + UUID.randomUUID();
  }

  private void enforceNoDuplicateOnReplace(Event oldEvent, Event newEvent) {
    if (!oldEvent.equals(newEvent)
        && eventExists(newEvent.getSubject(),
        newEvent.getStartDateTime(),
        newEvent.getEndDateTime())) {
      throw new IllegalArgumentException("Edit would create duplicate event");
    }
  }

  private void replaceEvent(Event oldEvent, Event newEvent) {
    int index = events.indexOf(oldEvent);
    if (index >= 0) {
      events.set(index, newEvent);

      String oldSid = oldEvent.getSeriesId().orElse(null);
      String newSid = newEvent.getSeriesId().orElse(null);

      if (Objects.equals(oldSid, newSid)) {
        if (oldSid != null) {
          seriesIndex.replaceStart(oldSid, oldEvent.getStartDateTime(),
              newEvent.getStartDateTime());
        }
      } else {
        if (oldSid != null) {
          seriesIndex.remove(oldSid, oldEvent.getStartDateTime());
        }
        if (newSid != null) {
          seriesIndex.add(newSid, newEvent.getStartDateTime());
        }
      }
    }
  }

  private Event applyProperty(Event source, EventProperty property, String newValue) {
    EventBuilder b = EventBuilder.from(source);
    property.apply(b, newValue);
    return b.build();
  }

  private EventProperty parseProperty(String property) {
    return EventProperty.fromToken(property);
  }

  private List<Event> getEventsBySeriesId(String seriesId) {
    List<LocalDateTime> starts = seriesIndex.starts(seriesId);
    List<Event> result = new ArrayList<>();
    for (LocalDateTime s : starts) {
      for (Event e : events) {
        if (e.getSeriesId().map(seriesId::equals).orElse(false)
            && e.getStartDateTime().equals(s)) {
          result.add(e);
          break;
        }
      }
    }
    return result;
  }

  private void convertAllEventsToZone(ZoneId from, ZoneId to) {
    for (int i = 0; i < events.size(); i++) {
      Event e = events.get(i);
      ZonedDateTime s = e.getStartDateTime().atZone(from);
      ZonedDateTime t = e.getEndDateTime().atZone(from);
      LocalDateTime newStart = s.withZoneSameInstant(to).toLocalDateTime();
      LocalDateTime newEnd = t.withZoneSameInstant(to).toLocalDateTime();
      Event converted = EventBuilder.from(e)
          .startDateTime(newStart)
          .endDateTime(newEnd)
          .build();
      events.set(i, converted);
    }
  }

  @Override
  public Event copyFrom(Event template, LocalDateTime newStart, LocalDateTime newEnd) {
    if (eventExists(template.getSubject(), newStart, newEnd)) {
      throw new IllegalArgumentException("Duplicate event in destination calendar");
    }
    Event copied = EventBuilder.from(template)
        .startDateTime(newStart)
        .endDateTime(newEnd)
        .build();
    events.add(copied);
    copied.getSeriesId().ifPresent(id -> seriesIndex.add(id, copied.getStartDateTime()));
    return copied;
  }

  private void validateSeriesInstanceShape(LocalDateTime start,
                                           LocalDateTime end,
                                           Set<DayOfWeek> weekdays) {
    if (end != null && !start.toLocalDate().equals(end.toLocalDate())) {
      throw new IllegalArgumentException("Event series instances must be single-day");
    }
    if (weekdays == null || weekdays.isEmpty()) {
      throw new IllegalArgumentException("Weekdays set must not be empty");
    }
  }

  private String coerceValue(EventProperty property,
                             LocalDateTime newDateTime,
                             String newText) {
    boolean isTime = property == EventProperty.START || property == EventProperty.END;
    String val = isTime ? (newDateTime == null ? null : newDateTime.toString()) : newText;
    if (val == null) {
      throw new IllegalArgumentException("New value required for property " + property);
    }
    return val;
  }

}
