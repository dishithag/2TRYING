import calendar.CalendarBook;
import calendar.CalendarBookImpl;
import calendar.controller.CalendarController;
import calendar.controller.HeadlessController;
import calendar.controller.InteractiveController;
import calendar.controller.gui.GuiController;
import calendar.view.CalendarView;
import calendar.view.TextCalendarView;
import calendar.view.gui.CalendarFrame;
import calendar.view.gui.GuiView;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.time.ZoneId;

/**
 * Main entry point for the Calendar application.
 */
public class CalendarRunner {

  private static ExitHandler exitHandler = status -> System.exit(status);

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
        AppMode.GUI.run(model, view, null);
        return;
      }
      if (!"--mode".equalsIgnoreCase(args[0]) || args.length < 2) {
        printUsageAndExit("Error: use --mode <gui|interactive|headless> [commandsFile]");
        return;
      }
      AppMode mode = AppMode.from(args[1]);
      String file = args.length >= 3 ? args[2] : null;
      mode.run(model, view, file);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      exitHandler.exit(1);
    }
  }

  private static void printUsageAndExit(String msg) {
    System.err.println(msg);
    System.err.println("Usage:");
    System.err.println("  java -jar app.jar");
    System.err.println("  java -jar app.jar --mode gui");
    System.err.println("  java -jar app.jar --mode interactive");
    System.err.println("  java -jar app.jar --mode headless <commandsFile>");
    exitHandler.exit(1);
  }

  static void setExitHandler(ExitHandler handler) {
    exitHandler = handler == null ? status -> System.exit(status) : handler;
  }

  /**
   * Application modes.
   */
  enum AppMode {
    GUI {
      @Override
      void run(CalendarBook model, CalendarView view, String file) {
        guiLauncher.launch(model);
      }
    },
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
      if ("gui".equals(t)) {
        return GUI;
      }
      if ("interactive".equals(t)) {
        return INTERACTIVE;
      }
      if ("headless".equals(t)) {
        return HEADLESS;
      }
      throw new IllegalArgumentException("Invalid mode '" + token + "'. Use 'gui',"
          + " 'interactive', or 'headless'.");
    }
  }

  private static GuiLauncher guiLauncher = CalendarRunner::launchGui;

  private static void launchGui(CalendarBook model) {
    GuiView guiView = new CalendarFrame();
    GuiController controller = new GuiController(model, guiView, ZoneId.systemDefault());
    controller.start();
  }

  static void setGuiLauncher(GuiLauncher launcher) {
    guiLauncher = launcher == null ? CalendarRunner::launchGui : launcher;
  }

  interface GuiLauncher {
    void launch(CalendarBook model);
  }
}
