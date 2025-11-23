import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import calendar.controller.CommandParser;
import org.junit.Test;

/**
 * Minimal direct tests for the command parser.
 */
public class CommandParserExitTest {

  /**
   * Parsing exit produces a command of type exit.
   */
  @Test
  public void testParse_exit() {
    CommandParser parser = new CommandParser();
    CommandParser.Command cmd = parser.parse("exit");
    assertNotNull(cmd);
    assertEquals("exit", cmd.getType());
  }

  /**
   * Unknown text is rejected.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testParse_unknown() {
    CommandParser parser = new CommandParser();
    parser.parse("this-is-not-a-command");
  }
}
