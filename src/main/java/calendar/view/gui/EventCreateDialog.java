package calendar.view.gui;

import calendar.view.gui.EventInput.RecurrenceRule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Dialog for gathering user input to create an event.
 */
public class EventCreateDialog {
  private final JFrame owner;

  /**
   * Constructs a dialog bound to the given owner frame.
   *
   * @param owner parent frame
   */
  public EventCreateDialog(JFrame owner) {
    this.owner = owner;
  }

  /**
   * Prompts for event creation input using the provided default date.
   *
   * @param date the initially selected date
   * @return event input when confirmed; otherwise null
   */
  public EventInput prompt(LocalDate date) {
    JPanel panel = new JPanel(new java.awt.GridLayout(0, 1, 4, 4));
    panel.add(new JLabel("Fields marked * are required."));

    JLabel subjectLabel = requiredLabel("Subject");
    JTextField subjectField = new JTextField();
    panel.add(subjectLabel);
    panel.add(subjectField);

    JLabel startLabel = requiredLabel("Start time");
    JComboBox<String> startCombo = new JComboBox<>(timeOptions());
    startCombo.setEditable(true);
    panel.add(startLabel);
    panel.add(startCombo);

    JLabel endLabel = new JLabel("End time");
    JComboBox<String> endCombo = new JComboBox<>(timeOptions());
    endCombo.setEditable(true);
    panel.add(endLabel);
    panel.add(endCombo);

    JTextField locationField = new JTextField();
    panel.add(new JLabel("Location"));
    panel.add(locationField);

    JTextField descriptionField = new JTextField();
    panel.add(new JLabel("Description"));
    panel.add(descriptionField);

    JComboBox<String> visibility = new JComboBox<>(new String[] {"Public", "Private"});
    panel.add(new JLabel("Visibility"));
    panel.add(visibility);

    JComboBox<String> recurrenceChoice = new JComboBox<>(
        new String[] {"None", "Repeat for N times", "Repeat until date"});
    panel.add(new JLabel("Recurrence"));
    panel.add(recurrenceChoice);

    JLabel weekdaysLabel = new JLabel("Recurrence weekdays (e.g., MTWRFSU)");
    JTextField weekdaysField = new JTextField();
    panel.add(weekdaysLabel);
    panel.add(weekdaysField);

    JLabel countLabel = new JLabel("Occurrences (for N times)");
    JTextField countField = new JTextField();
    panel.add(countLabel);
    panel.add(countField);

    JLabel untilLabel = new JLabel("Until date");
    JComboBox<String> untilYear = buildYearCombo();
    JComboBox<String> untilMonth = buildMonthCombo();
    JComboBox<String> untilDay = buildDayCombo();
    attachUntilListeners(untilYear, untilMonth, untilDay);
    panel.add(untilLabel);
    panel.add(untilYear);
    panel.add(untilMonth);
    panel.add(untilDay);

    updateRecurrenceVisibility((String) recurrenceChoice.getSelectedItem(), weekdaysLabel,
        weekdaysField, countLabel, countField, untilLabel, untilYear, untilMonth, untilDay);
    recurrenceChoice.addItemListener(evt -> updateRecurrenceVisibility(
        (String) recurrenceChoice.getSelectedItem(), weekdaysLabel, weekdaysField, countLabel,
        countField, untilLabel, untilYear, untilMonth, untilDay));

    while (true) {
      int result = JOptionPane.showConfirmDialog(owner, panel, "Create Event",
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
      if (result != JOptionPane.OK_OPTION) {
        return null;
      }

      String subject = subjectField.getText().trim();
      if (subject.isEmpty()) {
        showError("Subject is required");
        continue;
      }

      LocalTime startTime = parseComboTime(startCombo);
      LocalTime endTime = parseComboTime(endCombo);

      if (startTime == null && endTime != null) {
        showError("Select a start time or leave both blank for all-day");
        continue;
      }

      String recurrenceSelection = (String) recurrenceChoice.getSelectedItem();
      LocalDate untilDate = "Repeat until date".equals(recurrenceSelection)
          ? parseUntilDate(untilYear, untilMonth, untilDay) : null;

      RecurrenceRule rule = parseRecurrence(recurrenceSelection,
          weekdaysField.getText(), countField.getText(), untilDate);
      if (rule == null) {
        continue;
      }

      boolean publicEvent = "Public".equals(visibility.getSelectedItem());
      String desc = descriptionField.getText().isBlank() ? null : descriptionField.getText().trim();
      String loc = locationField.getText().isBlank() ? null : locationField.getText().trim();
      return new EventInput(subject, date, startTime, endTime, publicEvent, desc, loc, rule);
    }
  }

  private LocalTime parseComboTime(JComboBox<String> combo) {
    String value = (String) combo.getSelectedItem();
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      String trimmed = value.trim();
      if (trimmed.isEmpty()) {
        return null;
      }
      return LocalTime.parse(trimmed);
    } catch (DateTimeParseException e) {
      showError("Time must be HH:mm");
      return null;
    }
  }

  private RecurrenceRule parseRecurrence(String choice, String weekdays,
      String count, LocalDate until) {
    String trimmedWeekdays = weekdays.trim();
    String trimmedCount = count.trim();
    if (choice == null || "None".equals(choice)) {
      if (!trimmedWeekdays.isEmpty() || !trimmedCount.isEmpty() || until != null) {
        showError("Leave recurrence fields blank or choose a recurrence option");
        return null;
      }
      return RecurrenceRule.none();
    }
    Set<DayOfWeek> weekdaySet = parseWeekdays(trimmedWeekdays);
    if (weekdaySet.isEmpty()) {
      showError("Specify weekdays for recurrence");
      return null;
    }
    if ("Repeat for N times".equals(choice)) {
      try {
        int occurrences = Integer.parseInt(trimmedCount);
        if (occurrences <= 0) {
          showError("Occurrences must be positive");
          return null;
        }
        return RecurrenceRule.forOccurrences(weekdaySet, occurrences);
      } catch (NumberFormatException e) {
        showError("Enter a valid occurrence count");
        return null;
      }
    }
    if (until == null) {
      showError("Select an until date");
      return null;
    }
    return RecurrenceRule.untilDate(weekdaySet, until);
  }

  private void updateRecurrenceVisibility(String choice, JLabel weekdaysLabel,
      JTextField weekdaysField, JLabel countLabel, JTextField countField,
      JLabel untilLabel, JComboBox<String> untilYear, JComboBox<String> untilMonth,
      JComboBox<String> untilDay) {
    boolean none = choice == null || "None".equals(choice);

    weekdaysLabel.setVisible(!none);
    weekdaysField.setVisible(!none);
    weekdaysField.setEnabled(!none);

    boolean forCount = "Repeat for N times".equals(choice);

    countLabel.setVisible(forCount);
    countField.setVisible(forCount);
    countField.setEnabled(forCount);

    boolean until = "Repeat until date".equals(choice);

    untilLabel.setVisible(until);
    untilYear.setVisible(until);
    untilMonth.setVisible(until);
    untilDay.setVisible(until);
    untilYear.setEnabled(until);
    untilMonth.setEnabled(until);
    untilDay.setEnabled(until);

    if (none) {
      weekdaysField.setText("");
      countField.setText("");
      untilYear.setSelectedIndex(0);
      untilMonth.setSelectedIndex(0);
      untilDay.setSelectedIndex(0);
    }
  }

  private JLabel requiredLabel(String text) {
    String labelText = "<html><span style='color:red'>*</span> " + text + "</html>";
    return new JLabel(labelText);
  }

  private String[] timeOptions() {
    String[] times = new String[49];
    times[0] = "";
    for (int i = 0; i < 48; i++) {
      int minutes = i * 30;
      int hour = minutes / 60;
      int minute = minutes % 60;
      times[i + 1] = String.format("%02d:%02d", hour, minute);
    }
    return times;
  }

  private JComboBox<String> buildYearCombo() {
    JComboBox<String> combo = new JComboBox<>();
    combo.addItem("");
    int currentYear = LocalDate.now().getYear();
    for (int year = currentYear - 1; year <= currentYear + 10; year++) {
      combo.addItem(String.valueOf(year));
    }
    return combo;
  }

  private JComboBox<String> buildMonthCombo() {
    JComboBox<String> combo = new JComboBox<>();
    combo.addItem("");
    for (int month = 1; month <= 12; month++) {
      combo.addItem(String.format("%02d", month));
    }
    return combo;
  }

  private JComboBox<String> buildDayCombo() {
    JComboBox<String> combo = new JComboBox<>();
    combo.addItem("");
    for (int day = 1; day <= 31; day++) {
      combo.addItem(String.format("%02d", day));
    }
    return combo;
  }

  private void attachUntilListeners(JComboBox<String> year, JComboBox<String> month,
      JComboBox<String> day) {
    Runnable refreshDays = () -> updateUntilDays(year, month, day);
    year.addActionListener(e -> refreshDays.run());
    month.addActionListener(e -> refreshDays.run());
  }

  private void updateUntilDays(JComboBox<String> year, JComboBox<String> month,
      JComboBox<String> day) {
    String yearVal = (String) year.getSelectedItem();
    String monthVal = (String) month.getSelectedItem();
    if (yearVal == null || monthVal == null || yearVal.isBlank() || monthVal.isBlank()) {
      return;
    }
    try {
      int y = Integer.parseInt(yearVal);
      int m = Integer.parseInt(monthVal);
      int length = YearMonth.of(y, m).lengthOfMonth();
      DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
      model.addElement("");
      for (int d = 1; d <= length; d++) {
        model.addElement(String.format("%02d", d));
      }
      String currentSelection = (String) day.getSelectedItem();
      day.setModel(model);
      day.setSelectedItem(currentSelection);
    } catch (NumberFormatException e) {
      showError("Invalid until date");
    }
  }

  private LocalDate parseUntilDate(JComboBox<String> year, JComboBox<String> month,
      JComboBox<String> day) {
    String yearVal = (String) year.getSelectedItem();
    String monthVal = (String) month.getSelectedItem();
    String dayVal = (String) day.getSelectedItem();
    if (yearVal == null || monthVal == null || dayVal == null
        || yearVal.isBlank() || monthVal.isBlank() || dayVal.isBlank()) {
      return null;
    }
    try {
      return LocalDate.of(Integer.parseInt(yearVal), Integer.parseInt(monthVal),
          Integer.parseInt(dayVal));
    } catch (DateTimeParseException | NumberFormatException e) {
      showError("Until date must be valid");
      return null;
    }
  }

  private Set<DayOfWeek> parseWeekdays(String token) {
    Set<DayOfWeek> result = new HashSet<>();
    if (token == null) {
      return result;
    }
    String trimmed = token.trim().toUpperCase();
    Map<Character, DayOfWeek> map = weekdayMap();
    for (char c : trimmed.toCharArray()) {
      if (map.containsKey(c)) {
        result.add(map.get(c));
      }
    }
    return result;
  }

  private Map<Character, DayOfWeek> weekdayMap() {
    Map<Character, DayOfWeek> map = new HashMap<>();
    map.put('M', DayOfWeek.MONDAY);
    map.put('T', DayOfWeek.TUESDAY);
    map.put('W', DayOfWeek.WEDNESDAY);
    map.put('R', DayOfWeek.THURSDAY);
    map.put('F', DayOfWeek.FRIDAY);
    map.put('S', DayOfWeek.SATURDAY);
    map.put('U', DayOfWeek.SUNDAY);
    return map;
  }

  private void showError(String message) {
    JOptionPane.showMessageDialog(owner, message, "Error", JOptionPane.ERROR_MESSAGE);
  }
}
