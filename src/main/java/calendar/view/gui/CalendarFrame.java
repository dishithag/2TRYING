package calendar.view.gui;

import calendar.EventProperty;
import calendar.controller.EditScope;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Swing implementation of the calendar GUI.
 */
public class CalendarFrame extends JFrame implements GuiView {
  private GuiFeatures features;
  private final JComboBox<String> calendarCombo = new JComboBox<>();
  private final JLabel zoneLabel = new JLabel();
  private final JLabel monthLabel = new JLabel();
  private final JPanel monthPanel = new JPanel();
  private final DefaultListModel<EventViewModel> eventListModel = new DefaultListModel<>();
  private final JList<EventViewModel> eventList = new JList<>(eventListModel);
  private final JLabel selectedDateLabel = new JLabel();
  private CalendarUiState currentState;
  private boolean updatingCalendars;

  /**
   * Creates a calendar frame.
   */
  public CalendarFrame() {
    super("Calendar");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setPreferredSize(new Dimension(1100, 700));
    setLayout(new BorderLayout());
    buildNorth();
    buildCenter();
    buildEast();
    buildSouth();
    pack();
  }

  @Override
  public void setFeatures(GuiFeatures features) {
    this.features = features;
  }

  @Override
  public void render(CalendarUiState state) {
    this.currentState = state;
    if (SwingUtilities.isEventDispatchThread()) {
      updateCalendars(state);
      updateHeader(state);
      updateMonth(state);
      updateEvents(state);
      return;
    }
    SwingUtilities.invokeLater(() -> {
      updateCalendars(state);
      updateHeader(state);
      updateMonth(state);
      updateEvents(state);
    });
  }

  @Override
  public void showMessage(String message) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message,
        "Info", JOptionPane.INFORMATION_MESSAGE));
  }

  @Override
  public void showError(String error) {
    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, error,
        "Error", JOptionPane.ERROR_MESSAGE));
  }

  @Override
  public void start() {
    SwingUtilities.invokeLater(() -> setVisible(true));
  }

  private void buildNorth() {
    final JPanel panel = new JPanel();
    panel.add(new JLabel("Calendar:"));
    panel.add(calendarCombo);
    calendarCombo.addActionListener(e -> {
      if (!updatingCalendars && features != null && calendarCombo.getSelectedItem() != null) {
        features.selectCalendar(calendarCombo.getSelectedItem().toString());
      }
    });

    JButton newCalendar = new JButton("New Calendar");
    newCalendar.addActionListener(e -> promptCreateCalendar());
    panel.add(newCalendar);

    JButton prev = new JButton("◀");
    prev.addActionListener(e -> navigateMonth(-1));
    panel.add(prev);

    JButton next = new JButton("▶");
    next.addActionListener(e -> navigateMonth(1));
    panel.add(monthLabel);
    panel.add(next);
    panel.add(zoneLabel);
    add(panel, BorderLayout.NORTH);
  }

  private void buildCenter() {
    monthPanel.setLayout(new GridLayout(0, 7, 4, 4));
    monthPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    add(monthPanel, BorderLayout.CENTER);
  }

  private void buildEast() {
    JPanel wrapper = new JPanel(new BorderLayout());
    selectedDateLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    wrapper.add(selectedDateLabel, BorderLayout.NORTH);
    wrapper.add(new JScrollPane(eventList), BorderLayout.CENTER);
    eventList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          EventViewModel selected = eventList.getSelectedValue();
          if (selected != null && currentState != null) {
            showEventDetails(selected, currentState.getActiveZone());
          }
        }
      }
    });

    final JPanel buttons = new JPanel();
    JButton create = new JButton("Create Event");
    create.addActionListener(e -> promptCreateEvent());
    buttons.add(create);
    JButton edit = new JButton("Edit");
    edit.addActionListener(e -> promptEditEvent());
    buttons.add(edit);
    wrapper.add(buttons, BorderLayout.SOUTH);
    wrapper.setPreferredSize(new Dimension(420, 700));
    add(wrapper, BorderLayout.EAST);
  }

  private void buildSouth() {
    JLabel footer = new JLabel("Select a day to view and edit events");
    footer.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    add(footer, BorderLayout.SOUTH);
  }

  private void updateCalendars(CalendarUiState state) {
    updatingCalendars = true;
    try {
      DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
      for (CalendarSummary summary : state.getCalendars()) {
        model.addElement(summary.getName());
      }
      calendarCombo.setModel(model);
      calendarCombo.setSelectedItem(state.getActiveCalendar());
      zoneLabel.setText("Timezone: " + state.getActiveZone());
    } finally {
      updatingCalendars = false;
    }
  }

  private void updateHeader(CalendarUiState state) {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy");
    monthLabel.setText(state.getVisibleMonth().atDay(1).format(fmt));
    selectedDateLabel.setText("Selected: " + state.getSelectedDate());
  }

  private void updateMonth(CalendarUiState state) {
    monthPanel.removeAll();
    for (DayOfWeek day : DayOfWeek.values()) {
      JLabel label = new JLabel(day.name().substring(0, 3), JLabel.CENTER);
      label.setForeground(Color.DARK_GRAY);
      monthPanel.add(label);
    }
    LocalDate first = state.getVisibleMonth().atDay(1);
    int offset = first.getDayOfWeek().getValue() - 1;
    if (offset < 0) {
      offset = 0;
    }
    for (int i = 0; i < offset; i++) {
      monthPanel.add(new JLabel(""));
    }
    int length = state.getVisibleMonth().lengthOfMonth();
    Map<LocalDate, List<EventViewModel>> map = state.getEventsByDate();
    for (int day = 1; day <= length; day++) {
      LocalDate date = state.getVisibleMonth().atDay(day);
      JButton button = new JButton(String.valueOf(date.getDayOfMonth()));
      button.setEnabled(true);
      button.setFocusable(false);
      button.setOpaque(true);
      button.setContentAreaFilled(true);
      if (date.equals(state.getSelectedDate())) {
        button.setBackground(new Color(200, 230, 255));
      }
      if (map.containsKey(date)) {
        button.setForeground(new Color(178, 34, 34));
      }
      button.addActionListener(e -> {
        if (features != null) {
          features.selectDate(date);
        }
      });
      monthPanel.add(button);
    }
    monthPanel.revalidate();
    monthPanel.repaint();
  }

  private void updateEvents(CalendarUiState state) {
    eventListModel.clear();
    for (EventViewModel vm : state.getSelectedDayEvents()) {
      eventListModel.addElement(vm);
    }
    eventList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
      JLabel label = new JLabel(value.summary(state.getActiveZone()));
      label.setOpaque(true);
      if (isSelected) {
        label.setBackground(new Color(220, 235, 250));
      }
      label.setToolTipText(buildTooltip(value, state.getActiveZone()));
      return label;
    });
  }

  private String buildTooltip(EventViewModel vm, ZoneId zone) {
    StringBuilder sb = new StringBuilder();
    sb.append("<html><b>").append(vm.getSubject()).append("</b><br>");
    sb.append(vm.getStart().atZone(zone).toLocalTime()).append("-")
        .append(vm.getEnd().atZone(zone).toLocalTime());
    vm.getLocation().ifPresent(loc -> sb.append(" @ ").append(loc));
    if (!vm.isPublicEvent()) {
      sb.append(" (private)");
    }
    if (vm.isSeriesPart()) {
      sb.append(" [series]");
      appendSeriesMeta(vm, sb);
    }
    vm.getDescription().ifPresent(desc -> sb.append("<br>Description: ").append(desc));
    sb.append("</html>");
    return sb.toString();
  }

  private void showEventDetails(EventViewModel vm, ZoneId zone) {
    StringBuilder sb = new StringBuilder();
    sb.append("Subject: ").append(vm.getSubject()).append("\n");
    sb.append("When: ").append(vm.getStart().atZone(zone)).append(" to ")
        .append(vm.getEnd().atZone(zone)).append("\n");
    vm.getLocation().ifPresent(loc -> sb.append("Location: ").append(loc).append("\n"));
    vm.getDescription().ifPresent(desc -> sb.append("Description: ").append(desc).append("\n"));
    sb.append("Visibility: ").append(vm.isPublicEvent() ? "public" : "private");
    appendSeriesDetails(vm, sb);
    showMessage(sb.toString());
  }

  private void appendSeriesMeta(EventViewModel vm, StringBuilder sb) {
    vm.getSeriesOccurrences().ifPresent(count -> sb.append(" (" + count + " total)"));
    if (vm.getSeriesFirstDate().isPresent() || vm.getSeriesLastDate().isPresent()) {
      sb.append("<br>Span: ");
      vm.getSeriesFirstDate().ifPresent(date -> sb.append(date));
      sb.append(" – ");
      vm.getSeriesLastDate().ifPresent(date -> sb.append(date));
    }
  }

  private void appendSeriesDetails(EventViewModel vm, StringBuilder sb) {
    if (vm.isSeriesPart()) {
      sb.append("\nSeries: part of a series");
      vm.getSeriesOccurrences().ifPresent(count -> sb.append(" (" + count + " total occurrences)"));
      if (vm.getSeriesFirstDate().isPresent() || vm.getSeriesLastDate().isPresent()) {
        sb.append("\nSeries span: ");
        vm.getSeriesFirstDate().ifPresent(date -> sb.append(date));
        sb.append(" to ");
        vm.getSeriesLastDate().ifPresent(date -> sb.append(date));
      }
    } else {
      sb.append("\nSeries: single event");
    }
  }

  private void navigateMonth(int delta) {
    if (currentState != null && features != null) {
      features.goToMonth(currentState.getVisibleMonth().plusMonths(delta));
    }
  }

  private void promptCreateCalendar() {
    String name = JOptionPane.showInputDialog(this, "Calendar name:");
    if (name == null || name.isBlank()) {
      return;
    }
    String tz = JOptionPane.showInputDialog(this, "Timezone (e.g., America/New_York):");
    if (tz == null || tz.isBlank()) {
      return;
    }
    try {
      ZoneId zone = ZoneId.of(tz.trim());
      features.createCalendar(name.trim(), zone);
    } catch (Exception ex) {
      showError(ex.getMessage());
    }
  }

  private void promptCreateEvent() {
    if (currentState == null) {
      return;
    }
    EventInput input = gatherEventInput(currentState.getSelectedDate());
    if (input != null) {
      try {
        features.createEvent(input);
      } catch (Exception ex) {
        showError(ex.getMessage());
      }
    }
  }

  private EventInput gatherEventInput(LocalDate date) {
    JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
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
      int result = JOptionPane.showConfirmDialog(this, panel, "Create Event",
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

      EventInput.RecurrenceRule rule = parseRecurrence(recurrenceSelection,
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

  private EventInput.RecurrenceRule parseRecurrence(String choice, String weekdays,
      String count, LocalDate until) {
    String trimmedWeekdays = weekdays.trim();
    String trimmedCount = count.trim();
    if (choice == null || "None".equals(choice)) {
      if (!trimmedWeekdays.isEmpty() || !trimmedCount.isEmpty() || until != null) {
        showError("Leave recurrence fields blank or choose a recurrence option");
        return null;
      }
      return EventInput.RecurrenceRule.none();
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
        return EventInput.RecurrenceRule.forOccurrences(weekdaySet, occurrences);
      } catch (NumberFormatException e) {
        showError("Enter a valid occurrence count");
        return null;
      }
    }
    if (until == null) {
      showError("Select an until date");
      return null;
    }
    return EventInput.RecurrenceRule.untilDate(weekdaySet, until);
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

  private void promptEditEvent() {
    EventViewModel selected = eventList.getSelectedValue();
    if (selected == null) {
      showError("Select an event to edit");
      return;
    }
    EventReference ref = new EventReference(selected.getSubject(), selected.getStart());
    EditDialog dialog = new EditDialog(this, selected);
    EditDialog.EditResult result = dialog.prompt();
    if (result == null) {
      return;
    }
    for (EventUpdate update : result.updates) {
      try {
        features.editEvent(ref, update, result.scope);
      } catch (Exception ex) {
        showError(ex.getMessage());
        return;
      }
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

  private final class EditDialog {
    private final JFrame owner;
    private final EventViewModel selected;

    private EditDialog(JFrame owner, EventViewModel selected) {
      this.owner = owner;
      this.selected = selected;
    }

    private EditResult prompt() {
      JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
      panel.add(new JLabel("Select properties to edit and supply new values."));

      JCheckBox subjectBox = new JCheckBox("Subject", true);
      JTextField subjectField = new JTextField(selected.getSubject());
      panel.add(subjectBox);
      panel.add(subjectField);

      JCheckBox startBox = new JCheckBox("Start", false);
      JTextField startField = new JTextField(selected.getStart().toString());
      panel.add(startBox);
      panel.add(startField);

      JCheckBox endBox = new JCheckBox("End", false);
      JTextField endField = new JTextField(selected.getEnd().toString());
      panel.add(endBox);
      panel.add(endField);

      JCheckBox descriptionBox = new JCheckBox("Description", false);
      JTextField descriptionField = new JTextField(selected.getDescription().orElse(""));
      panel.add(descriptionBox);
      panel.add(descriptionField);

      JCheckBox locationBox = new JCheckBox("Location", false);
      JTextField locationField = new JTextField(selected.getLocation().orElse(""));
      panel.add(locationBox);
      panel.add(locationField);

      JCheckBox visibility = new JCheckBox("Visibility", false);
      JComboBox<String> visibilityBox = new JComboBox<>(new String[] {"Public", "Private"});
      visibilityBox.setSelectedItem(selected.isPublicEvent() ? "Public" : "Private");
      panel.add(visibility);
      panel.add(visibilityBox);

      String[] scopes = {"event", "events", "series"};
      JComboBox<String> scopeBox = new JComboBox<>(scopes);
      panel.add(new JLabel("Scope"));
      panel.add(scopeBox);

      while (true) {
        int choice = JOptionPane.showConfirmDialog(owner, panel, "Edit Event",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
          return null;
        }
        List<EventUpdate> updates = new ArrayList<>();
        if (subjectBox.isSelected()) {
          String subject = subjectField.getText().trim();
          if (subject.isEmpty()) {
            showError("Subject cannot be empty");
            continue;
          }
          updates.add(new EventUpdate(EventProperty.SUBJECT, null, subject));
        }
        if (startBox.isSelected()) {
          LocalDateTime start = parseEditDateTime(startField.getText());
          if (start == null) {
            continue;
          }
          updates.add(new EventUpdate(EventProperty.START, start, null));
        }
        if (endBox.isSelected()) {
          LocalDateTime end = parseEditDateTime(endField.getText());
          if (end == null) {
            continue;
          }
          updates.add(new EventUpdate(EventProperty.END, end, null));
        }
        if (descriptionBox.isSelected()) {
          updates.add(new EventUpdate(EventProperty.DESCRIPTION, null,
              descriptionField.getText().trim()));
        }
        if (locationBox.isSelected()) {
          updates.add(new EventUpdate(EventProperty.LOCATION, null,
              locationField.getText().trim()));
        }
        if (visibility.isSelected()) {
          String vis = (String) visibilityBox.getSelectedItem();
          updates.add(new EventUpdate(EventProperty.STATUS, null,
              "Public".equals(vis) ? "public" : "private"));
        }
        if (updates.isEmpty()) {
          showError("Select at least one property to edit");
          continue;
        }
        EditScope scope = EditScope.fromToken((String) scopeBox.getSelectedItem());
        return new EditResult(updates, scope);
      }
    }

    private LocalDateTime parseEditDateTime(String text) {
      String value = text == null ? "" : text.trim();
      if (value.isEmpty()) {
        showError("Enter a date-time in yyyy-MM-dd'T'HH:mm");
        return null;
      }
      try {
        return LocalDateTime.parse(value);
      } catch (Exception e) {
        try {
          DateTimeFormatter alt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
          return LocalDateTime.parse(value, alt);
        } catch (Exception ex) {
          showError("Date-time must be yyyy-MM-dd'T'HH:mm or yyyy-MM-dd HH:mm");
          return null;
        }
      }
    }

    private static final class EditResult {
      private final List<EventUpdate> updates;
      private final EditScope scope;

      private EditResult(List<EventUpdate> updates, EditScope scope) {
        this.updates = updates;
        this.scope = scope;
      }
    }
  }
}
