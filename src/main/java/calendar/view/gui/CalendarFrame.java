package calendar.view.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import calendar.EventProperty;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    EventInput input = new EventCreateDialog(this).prompt(currentState.getSelectedDate());
    if (input != null) {
      try {
        features.createEvent(input);
      } catch (Exception ex) {
        showError(ex.getMessage());
      }
    }
  }

  private void promptEditEvent() {
    EventViewModel selected = eventList.getSelectedValue();
    if (selected == null) {
      showError("Select an event to edit");
      return;
    }
    EventReference ref = new EventReference(selected.getSubject(), selected.getStart());
    EventEditDialog.EditResult result = new EventEditDialog(this, selected).prompt();
    if (result == null) {
      return;
    }
    String currentSubject = ref.getSubject();
    LocalDateTime currentStart = ref.getStart();
    for (EventUpdate update : result.getUpdates()) {
      try {
        EventReference currentRef = new EventReference(currentSubject, currentStart);
        features.editEvent(currentRef, update, result.getScope());
        if (update.getProperty() == EventProperty.SUBJECT) {
          currentSubject = update.getNewValue();
        }
        if (update.getProperty() == EventProperty.START && update.getNewDateTime() != null) {
          currentStart = update.getNewDateTime();
        }
      } catch (Exception ex) {
        showError(ex.getMessage());
        return;
      }
    }
  }

}
