package calendar.view.gui;

import calendar.EventProperty;
import calendar.controller.EditScope;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Dialog for editing an event's properties.
 */
public class EventEditDialog {
  private final JFrame owner;
  private final EventViewModel selected;

  /**
   * Creates a dialog tied to the owning frame and the selected event.
   *
   * @param owner parent frame
   * @param selected event to edit
   */
  public EventEditDialog(JFrame owner, EventViewModel selected) {
    this.owner = owner;
    this.selected = selected;
  }

  /**
   * Prompts the user for edits and returns the chosen updates and scope.
   *
   * @return edit result or null when cancelled
   */
  public EditResult prompt() {
    JPanel panel = new JPanel(new java.awt.GridLayout(0, 1, 4, 4));
    panel.add(new JLabel("Select properties to edit and supply new values."));

    JCheckBox subjectBox = new JCheckBox("Subject", true);
    JTextField subjectField = new JTextField(selected.getSubject());
    panel.add(subjectBox);
    panel.add(subjectField);

    JCheckBox startBox = new JCheckBox("Start", false);
    panel.add(startBox);
    JTextField startDateField = new JTextField(selected.getStart().toLocalDate().toString());
    panel.add(startDateField);
    JComboBox<String> startTimeField = new JComboBox<>(timeOptions());
    startTimeField.setEditable(true);
    startTimeField.setSelectedItem(selected.getStart().toLocalTime().toString());
    panel.add(startTimeField);

    JCheckBox endBox = new JCheckBox("End", false);
    panel.add(endBox);
    JTextField endDateField = new JTextField(selected.getEnd().toLocalDate().toString());
    panel.add(endDateField);
    JComboBox<String> endTimeField = new JComboBox<>(timeOptions());
    endTimeField.setEditable(true);
    endTimeField.setSelectedItem(selected.getEnd().toLocalTime().toString());
    panel.add(endTimeField);

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
        LocalDateTime start = parseEditDateTime(startDateField.getText(), startTimeField);
        if (start == null) {
          continue;
        }
        updates.add(new EventUpdate(EventProperty.START, start, null));
      }
      if (endBox.isSelected()) {
        LocalDateTime end = parseEditDateTime(endDateField.getText(), endTimeField);
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
      updates.sort(Comparator.comparingInt(u -> propertyOrder(u.getProperty())));
      EditScope scope = EditScope.fromToken((String) scopeBox.getSelectedItem());
      return new EditResult(updates, scope);
    }
  }

  private LocalDateTime parseEditDateTime(String dateText, JComboBox<String> timeField) {
    String dateValue = dateText == null ? "" : dateText.trim();
    if (dateValue.isEmpty()) {
      showError("Enter a date in yyyy-MM-dd");
      return null;
    }
    LocalDate date;
    try {
      date = LocalDate.parse(dateValue);
    } catch (DateTimeParseException ex) {
      showError("Date must be yyyy-MM-dd");
      return null;
    }
    LocalTime time = parseComboTime(timeField);
    if (time == null) {
      showError("Enter a valid time as HH:mm");
      return null;
    }
    return LocalDateTime.of(date, time);
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

  private int propertyOrder(EventProperty property) {
    switch (property) {
      case SUBJECT:
        return 0;
      case DESCRIPTION:
        return 1;
      case LOCATION:
        return 2;
      case STATUS:
        return 3;
      case END:
        return 4;
      case START:
        return 5;
      default:
        return 6;
    }
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

  private void showError(String message) {
    JOptionPane.showMessageDialog(owner, message, "Error", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Result of the edit dialog.
   */
  public static final class EditResult {
    private final List<EventUpdate> updates;
    private final EditScope scope;

    /**
     * Creates an edit result.
     *
     * @param updates updates to apply
     * @param scope scope for application
     */
    public EditResult(List<EventUpdate> updates, EditScope scope) {
      this.updates = updates;
      this.scope = scope;
    }

    public List<EventUpdate> getUpdates() {
      return updates;
    }

    public EditScope getScope() {
      return scope;
    }
  }
}
