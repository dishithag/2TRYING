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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
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

    JButton renameCalendar = new JButton("Rename");
    renameCalendar.addActionListener(e -> promptRenameCalendar());
    panel.add(renameCalendar);

    JButton changeZone = new JButton("Change Timezone");
    changeZone.addActionListener(e -> promptChangeZone());
    panel.add(changeZone);

    JButton export = new JButton("Export");
    export.addActionListener(e -> promptExport());
    panel.add(export);

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
    int offset = first.getDayOfWeek().getValue() % 7;
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
    if (vm.isSeriesPart()) {
      sb.append("\nPart of a series");
    }
    showMessage(sb.toString());
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

  private void promptRenameCalendar() {
    String name = JOptionPane.showInputDialog(this, "New calendar name:");
    if (name == null || name.isBlank()) {
      return;
    }
    try {
      features.renameActiveCalendar(name.trim());
    } catch (Exception ex) {
      showError(ex.getMessage());
    }
  }

  private void promptChangeZone() {
    String tz = JOptionPane.showInputDialog(this, "Timezone (e.g., Europe/Paris):");
    if (tz == null || tz.isBlank()) {
      return;
    }
    try {
      features.changeActiveCalendarZone(ZoneId.of(tz.trim()));
    } catch (Exception ex) {
      showError(ex.getMessage());
    }
  }

  private void promptExport() {
    String path = JOptionPane.showInputDialog(this, "Export file (.csv/.ics/.ical):");
    if (path == null || path.isBlank()) {
      return;
    }
    try {
      features.exportCalendar(path.trim());
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
    JComboBox<String> startCombo = new JComboBox<>(timeOptions());
    startCombo.setEditable(true);
    JComboBox<String> endCombo = new JComboBox<>(timeOptions());
    endCombo.setEditable(true);
    JLabel subjectLabel = requiredLabel("Subject");
    JTextField subjectField = new JTextField();
    JLabel startLabel = requiredLabel("Start time");
    JLabel endLabel = new JLabel("End time");
    JTextField locationField = new JTextField();
    JTextField descriptionField = new JTextField();
    JComboBox<String> visibility = new JComboBox<>(new String[] {"Public", "Private"});
    JComboBox<String> recurrenceChoice = new JComboBox<>(
        new String[] {"None", "Repeat for N times", "Repeat until date"});
    JTextField weekdaysField = new JTextField();
    JTextField countField = new JTextField();
    JTextField untilField = new JTextField();

    JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
    panel.add(new JLabel("Fields marked * are required."));
    panel.add(subjectLabel);
    panel.add(subjectField);
    panel.add(startLabel);
    panel.add(startCombo);
    panel.add(endLabel);
    panel.add(endCombo);
    panel.add(new JLabel("Location"));
    panel.add(locationField);
    panel.add(new JLabel("Description"));
    panel.add(descriptionField);
    panel.add(new JLabel("Visibility"));
    panel.add(visibility);
    panel.add(new JLabel("Recurrence weekdays (e.g., MTWRFSU)"));
    panel.add(weekdaysField);
    panel.add(new JLabel("Recurrence"));
    panel.add(recurrenceChoice);
    panel.add(new JLabel("Occurrences (for N times)"));
    panel.add(countField);
    panel.add(new JLabel("Until date (yyyy-MM-dd)"));
    panel.add(untilField);

    int result = JOptionPane.showConfirmDialog(this, panel, "Create Event",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return null;
    }

    String subject = subjectField.getText().trim();
    if (subject.isEmpty()) {
      showError("Subject is required");
      return null;
    }

    LocalTime startTime = parseComboTime(startCombo);
    LocalTime endTime = parseComboTime(endCombo);

    if (startTime == null && endTime != null) {
      showError("Select a start time or leave both blank for all-day");
      return null;
    }

    EventInput.RecurrenceRule rule = parseRecurrence(
        (String) recurrenceChoice.getSelectedItem(), weekdaysField.getText(),
        countField.getText(), untilField.getText());
    if (rule == null) {
      return null;
    }

    boolean publicEvent = "Public".equals(visibility.getSelectedItem());
    String desc = descriptionField.getText().isBlank() ? null : descriptionField.getText().trim();
    String loc = locationField.getText().isBlank() ? null : locationField.getText().trim();
    return new EventInput(subject, date, startTime, endTime, publicEvent, desc, loc, rule);
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
      String count, String until) {
    if (choice == null || "None".equals(choice)) {
      if (!weekdays.trim().isEmpty() || !count.trim().isEmpty() || !until.trim().isEmpty()) {
        showError("Leave recurrence fields blank or choose a recurrence option");
        return null;
      }
      return EventInput.RecurrenceRule.none();
    }
    Set<DayOfWeek> weekdaySet = parseWeekdays(weekdays);
    if (weekdaySet.isEmpty()) {
      showError("Specify weekdays for recurrence");
      return null;
    }
    if ("Repeat for N times".equals(choice)) {
      try {
        int occurrences = Integer.parseInt(count.trim());
        return EventInput.RecurrenceRule.forOccurrences(weekdaySet, occurrences);
      } catch (NumberFormatException e) {
        showError("Enter a valid occurrence count");
        return null;
      }
    }
    try {
      LocalDate untilDate = LocalDate.parse(until.trim());
      return EventInput.RecurrenceRule.untilDate(weekdaySet, untilDate);
    } catch (DateTimeParseException e) {
      showError("Until date must be yyyy-MM-dd");
      return null;
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

  private void promptEditEvent() {
    EventViewModel selected = eventList.getSelectedValue();
    if (selected == null) {
      showError("Select an event to edit");
      return;
    }
    EventReference ref = new EventReference(selected.getSubject(), selected.getStart());
    EditDialog dialog = new EditDialog(this);
    EditDialog.EditResult result = dialog.prompt();
    if (result == null) {
      return;
    }
    try {
      features.editEvent(ref, result.update, result.scope);
    } catch (Exception ex) {
      showError(ex.getMessage());
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

  private static final class EditDialog {
    private final JFrame owner;

    private EditDialog(JFrame owner) {
      this.owner = owner;
    }

    private EditResult prompt() {
      String[] properties = {"subject", "start", "end", "description", "location", "status"};
      String property = (String) JOptionPane.showInputDialog(owner, "Property to edit:",
          "Edit Property", JOptionPane.PLAIN_MESSAGE, null, properties, properties[0]);
      if (property == null) {
        return null;
      }
      String value = JOptionPane.showInputDialog(owner, "New value:");
      if (value == null) {
        return null;
      }
      String[] scopes = {"event", "events", "series"};
      String scope = (String) JOptionPane.showInputDialog(owner, "Scope:",
          "Scope", JOptionPane.PLAIN_MESSAGE, null, scopes, scopes[0]);
      if (scope == null) {
        return null;
      }
      EventUpdate update;
      if ("start".equalsIgnoreCase(property) || "end".equalsIgnoreCase(property)) {
        update = new EventUpdate(EventProperty.valueOf(property.toUpperCase()),
            LocalDateTime.parse(value.trim()), null);
      } else {
        update = new EventUpdate(EventProperty.valueOf(property.toUpperCase()), null, value.trim());
      }
      return new EditResult(update, EditScope.fromToken(scope));
    }

    private static final class EditResult {
      private final EventUpdate update;
      private final EditScope scope;

      private EditResult(EventUpdate update, EditScope scope) {
        this.update = update;
        this.scope = scope;
      }
    }
  }
}
