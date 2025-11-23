import calendar.CalendarBook;
import calendar.CalendarBookImpl;
import calendar.controller.CalendarController;
import calendar.controller.HeadlessController;
import calendar.controller.InteractiveController;
import calendar.view.CalendarView;
import calendar.view.TextCalendarView;
import java.io.FileReader;
import java.io.InputStreamReader;

/**
 * Main entry point for the Calendar application.
 */
public class CalendarRunner {

  /**
   * Runs the calendar application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    CalendarBook model = new CalendarBookImpl();
    CalendarView view = new TextCalendarView(System.out);

    try {
      if (args.length == 0) {
        AppMode.INTERACTIVE.run(model, view, null);
        return;
      }
      if (!"--mode".equalsIgnoreCase(args[0]) || args.length < 2) {
        printUsageAndExit("Error: use --mode <interactive|headless> [commandsFile]");
        return;
      }
      AppMode mode = AppMode.from(args[1]);
      String file = args.length >= 3 ? args[2] : null;
      mode.run(model, view, file);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void printUsageAndExit(String msg) {
    System.err.println(msg);
    System.err.println("Usage:");
    System.err.println("  java -jar app.jar --mode interactive");
    System.err.println("  java -jar app.jar --mode headless <commandsFile>");
    System.exit(1);
  }

  /**
   * Application modes.
   */
  enum AppMode {
    INTERACTIVE {
      @Override
      void run(CalendarBook model, CalendarView view, String file) throws Exception {
        CalendarController c = new InteractiveController(model, view,
            new InputStreamReader(System.in));
        c.run();
      }
    },
    HEADLESS {
      @Override
      void run(CalendarBook model, CalendarView view, String file) throws Exception {
        if (file == null) {
          throw new IllegalArgumentException("Headless mode requires a commands file");
        }
        try (FileReader reader = new FileReader(file)) {
          CalendarController c = new HeadlessController(model, view, reader);
          c.run();
        }
      }
    };

    abstract void run(CalendarBook model, CalendarView view, String file) throws Exception;

    static AppMode from(String token) {
      if (token == null) {
        throw new IllegalArgumentException("Mode is required");
      }
      String t = token.trim().toLowerCase();
      if ("interactive".equals(t)) {
        return INTERACTIVE;
      }
      if ("headless".equals(t)) {
        return HEADLESS;
      }
      throw new IllegalArgumentException("Invalid mode '" + token + "'. Use 'interactive' or"
          +
          " 'headless'.");
    }
  }
}
