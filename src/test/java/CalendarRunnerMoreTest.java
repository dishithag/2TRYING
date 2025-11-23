import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import org.junit.Test;

/**
 * Extra tests for CalendarRunner to cover remaining branches.
 */
public class CalendarRunnerMoreTest {

  private static final class ExitCatcher extends SecurityManager {
    private Integer code;

    @Override
    public void checkPermission(Permission perm) {
    }

    @Override
    public void checkPermission(Permission perm, Object ctx) {
    }

    @Override
    public void checkExit(int status) {
      this.code = status;
      throw new SecurityException("exit");
    }
  }

  /**
   * Interactive mode via args should run and exit on input.
   */
  @Test
  public void testInteractiveModeWithArgs() throws Exception {
    ByteArrayInputStream in =
        new ByteArrayInputStream("exit\n".getBytes("UTF-8"));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    PrintStream oldOut = System.out;
    java.io.InputStream oldIn = System.in;
    System.setIn(in);
    System.setOut(new PrintStream(out));

    try {
      CalendarRunner.main(new String[]{"--mode", "interactive"});
      String printed = out.toString("UTF-8");
      assertTrue(printed.contains("Goodbye"));
    } finally {
      System.setOut(oldOut);
      System.setIn(oldIn);
    }
  }

  /**
   * Headless with missing file should go to catch block and exit 1.
   */
  @Test
  public void testHeadlessBadFile_goesToCatchAndExit() throws Exception {
    SecurityManager oldSm = System.getSecurityManager();
    ExitCatcher sm = new ExitCatcher();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream oldErr = System.err;

    System.setSecurityManager(sm);
    System.setErr(new PrintStream(err));

    try {
      CalendarRunner.main(
          new String[]{"--mode", "headless", "no-such-file.txt"});
      fail("expected exit");
    } catch (SecurityException e) {
      assertEquals(Integer.valueOf(1), sm.code);
      String text = err.toString(StandardCharsets.UTF_8);
      assertTrue(text.contains("Error:"));
    } finally {
      System.setErr(oldErr);
      System.setSecurityManager(oldSm);
    }
  }

  @Test
  public void testNoArgs_defaultsToInteractive() throws Exception {
    java.io.InputStream oldIn = System.in;
    PrintStream oldOut = System.out;

    ByteArrayInputStream in =
        new ByteArrayInputStream("exit\n".getBytes("UTF-8"));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    System.setIn(in);
    System.setOut(new PrintStream(out));

    try {
      CalendarRunner.main(new String[0]);
      String s = out.toString(StandardCharsets.UTF_8);
      assertTrue(s.contains("Goodbye"));
    } finally {
      System.setIn(oldIn);
      System.setOut(oldOut);
    }
  }

  /**
   * Test when first argument is not --mode.
   */
  @Test
  public void testFirstArgNotMode_printsUsageAndExits() throws Exception {
    SecurityManager oldSm = System.getSecurityManager();
    ExitCatcher sm = new ExitCatcher();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream oldErr = System.err;

    System.setSecurityManager(sm);
    System.setErr(new PrintStream(err));

    try {
      CalendarRunner.main(new String[]{"--wrong", "interactive"});
      fail("expected exit");
    } catch (SecurityException e) {
      assertEquals(Integer.valueOf(1), sm.code);
      String text = err.toString(StandardCharsets.UTF_8);
      assertTrue(text.contains("first argument must be --mode") || text.contains("use --mode"));
      assertTrue(text.contains("Usage:"));
    } finally {
      System.setErr(oldErr);
      System.setSecurityManager(oldSm);
    }
  }

  /**
   * Test invalid mode value prints usage and exits.
   */
  @Test
  public void testInvalidMode_printsUsageAndExits() throws Exception {
    SecurityManager oldSm = System.getSecurityManager();
    ExitCatcher sm = new ExitCatcher();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream oldErr = System.err;

    System.setSecurityManager(sm);
    System.setErr(new PrintStream(err));

    try {
      CalendarRunner.main(new String[]{"--mode", "invalid"});
      fail("expected exit");
    } catch (SecurityException e) {
      assertEquals(Integer.valueOf(1), sm.code);
      String text = err.toString(StandardCharsets.UTF_8);
      assertTrue(text.contains("invalid mode") || text.contains("Invalid mode"));
      assertTrue(text.contains("Use 'interactive' or 'headless'"));
    } finally {
      System.setErr(oldErr);
      System.setSecurityManager(oldSm);
    }
  }

  /**
   * Test headless mode without file argument prints usage and exits.
   */
  @Test
  public void testHeadlessWithoutFile_printsUsageAndExits() throws Exception {
    SecurityManager oldSm = System.getSecurityManager();
    ExitCatcher sm = new ExitCatcher();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream oldErr = System.err;

    System.setSecurityManager(sm);
    System.setErr(new PrintStream(err));

    try {
      CalendarRunner.main(new String[]{"--mode", "headless"});
      fail("expected exit");
    } catch (SecurityException e) {
      assertEquals(Integer.valueOf(1), sm.code);
      String text = err.toString(StandardCharsets.UTF_8);
      assertTrue(text.contains("headless mode requires a commands file")
          || text.contains("Headless mode requires"));
      assertTrue(text.contains("Usage:") || text.contains("Error:"));
    } finally {
      System.setErr(oldErr);
      System.setSecurityManager(oldSm);
    }
  }

  /**
   * Test headless mode with valid file runs successfully.
   */
  @Test
  public void testHeadlessWithValidFile_runs() throws Exception {

    Path tempFile = Files.createTempFile("test-commands", ".txt");

    try {
      Files.writeString(tempFile, "exit\n");

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PrintStream oldOut = System.out;
      System.setOut(new PrintStream(out));

      try {
        CalendarRunner.main(new String[]{"--mode", "headless", tempFile.toString()});


      } finally {
        System.setOut(oldOut);
      }
    } finally {

      try {
        Files.deleteIfExists(tempFile);
      } catch (Exception e) {

        tempFile.toFile().deleteOnExit();
      }
    }
  }

  /**
   * Test the printUsageAndExit method is called for various errors.
   */
  @Test
  public void testPrintUsageAndExit_allBranches() throws Exception {
    SecurityManager oldSm = System.getSecurityManager();
    ExitCatcher sm = new ExitCatcher();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream oldErr = System.err;

    System.setSecurityManager(sm);
    System.setErr(new PrintStream(err));

    try {

      CalendarRunner.main(new String[]{"notmode", "interactive"});
      fail("expected exit");
    } catch (SecurityException e) {
      String text = err.toString(StandardCharsets.UTF_8);
      assertTrue(text.contains("Usage:"));
      assertTrue(text.contains("java -jar app.jar --mode interactive"));
      assertTrue(text.contains("java -jar app.jar --mode headless <commandsFile>"));
    } finally {
      System.setErr(oldErr);
      System.setSecurityManager(oldSm);
    }
  }

  /**
   * Test missing mode value after --mode flag.
   */
  @Test
  public void testMissingModeValue_printsUsageAndExits() throws Exception {
    SecurityManager oldSm = System.getSecurityManager();
    ExitCatcher sm = new ExitCatcher();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream oldErr = System.err;

    System.setSecurityManager(sm);
    System.setErr(new PrintStream(err));

    try {
      CalendarRunner.main(new String[]{"--mode"});
      fail("expected exit");
    } catch (SecurityException e) {
      assertEquals(Integer.valueOf(1), sm.code);
      String text = err.toString(StandardCharsets.UTF_8);
      assertTrue(text.contains("use --mode") || text.contains("missing mode value"));
      assertTrue(text.contains("Use 'interactive' or 'headless'") || text.contains("use --mode"));
    } finally {
      System.setErr(oldErr);
      System.setSecurityManager(oldSm);
    }
  }

  /**
   * Test args.length is exactly 1 (only --mode, no value).
   */
  @Test
  public void testOnlyModeFlag_printsUsageAndExits() throws Exception {
    SecurityManager oldSm = System.getSecurityManager();
    ExitCatcher sm = new ExitCatcher();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream oldErr = System.err;

    System.setSecurityManager(sm);
    System.setErr(new PrintStream(err));

    try {
      CalendarRunner.main(new String[]{"--mode"});
      fail("expected exit");
    } catch (SecurityException e) {
      assertEquals(Integer.valueOf(1), sm.code);
      String text = err.toString(StandardCharsets.UTF_8);
      assertTrue(text.contains("use --mode") || text.contains("missing mode value"));
    } finally {
      System.setErr(oldErr);
      System.setSecurityManager(oldSm);
    }
  }

  @Test

  public void testAppModeFrom_null_throws() throws Exception {

    SecurityManager oldSm = System.getSecurityManager();

    ExitCatcher sm = new ExitCatcher();

    ByteArrayOutputStream err = new ByteArrayOutputStream();

    PrintStream oldErr = System.err;

    System.setSecurityManager(sm);

    System.setErr(new PrintStream(err));

    try {

      CalendarRunner.main(new String[]{"--mode", null});

      fail("expected exit");

    } catch (SecurityException e) {

      assertEquals(Integer.valueOf(1), sm.code);

      String text = err.toString(StandardCharsets.UTF_8);

      assertTrue(text.contains("Mode is required") || text.contains("Error:"));

    } finally {

      System.setErr(oldErr);

      System.setSecurityManager(oldSm);

    }

  }



}
