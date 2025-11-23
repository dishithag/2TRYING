package calendar.view.gui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Set;

/**
 * Input payload for creating events from the GUI.
 */
public final class EventInput {
  private final String subject;
  private final LocalDate date;
  private final LocalTime startTime;
  private final LocalTime endTime;
  private final boolean publicEvent;
  private final String description;
  private final String location;
  private final RecurrenceRule recurrenceRule;

  /**
   * Constructs an event input.
   *
   * @param subject subject
   * @param date date
   * @param startTime start time or null for all-day
   * @param endTime end time or null for all-day
   * @param publicEvent whether the event is public
   * @param description optional description
   * @param location optional location
   * @param recurrenceRule recurrence configuration
   */
  public EventInput(String subject, LocalDate date, LocalTime startTime, LocalTime endTime,
                    boolean publicEvent, String description, String location,
                    RecurrenceRule recurrenceRule) {
    this.subject = Objects.requireNonNull(subject, "subject");
    this.date = Objects.requireNonNull(date, "date");
    this.startTime = startTime;
    this.endTime = endTime;
    this.publicEvent = publicEvent;
    this.description = description;
    this.location = location;
    this.recurrenceRule = recurrenceRule == null ? RecurrenceRule.none() : recurrenceRule;
  }

  public String getSubject() {
    return subject;
  }

  public LocalDate getDate() {
    return date;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public boolean isPublicEvent() {
    return publicEvent;
  }

  public String getDescription() {
    return description;
  }

  public String getLocation() {
    return location;
  }

  public RecurrenceRule getRecurrenceRule() {
    return recurrenceRule;
  }

  /**
   * Describes recurrence for series creation.
   */
  public static final class RecurrenceRule {
    private final Set<DayOfWeek> weekdays;
    private final Integer occurrences;
    private final LocalDate untilDate;

    private RecurrenceRule(Set<DayOfWeek> weekdays, Integer occurrences, LocalDate untilDate) {
      this.weekdays = weekdays;
      this.occurrences = occurrences;
      this.untilDate = untilDate;
    }

    public static RecurrenceRule none() {
      return new RecurrenceRule(null, null, null);
    }

    public static RecurrenceRule forOccurrences(Set<DayOfWeek> weekdays, int occurrences) {
      return new RecurrenceRule(Objects.requireNonNull(weekdays, "weekdays"), occurrences, null);
    }

    public static RecurrenceRule untilDate(Set<DayOfWeek> weekdays, LocalDate untilDate) {
      return new RecurrenceRule(Objects.requireNonNull(weekdays, "weekdays"), null,
          Objects.requireNonNull(untilDate, "untilDate"));
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

    public boolean isRecurring() {
      return weekdays != null;
    }
  }
}
