import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import org.junit.Test;

/**
 * Tests for the CalendarRunner main entry point.
 */
public class CalendarRunnerTest {

  /**
   * Security manager that intercepts System.exit.
   */
  private static final class ExitInterceptSecurityManager extends SecurityManager {
    private Integer code;

    @Override
    public void checkPermission(Permission perm) {
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
    }

    @Override
    public void checkExit(int status) {
      this.code = status;
      throw new SecurityException("exit");
    }
  }


}
