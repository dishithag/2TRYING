import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import calendar.Event;
import calendar.EventBuilder;
import calendar.EventProperty;
import java.time.LocalDateTime;
import org.junit.Test;

/**
 * Tests for EventProperty enum.
 */
public class EventPropertyTest {

  /**
   * Test apply with SUBJECT returns the same builder.
   */
  @Test
  public void testApply_subject_returnsSameBuilder() {
    EventBuilder builder = new EventBuilder()
        .startDateTime(LocalDateTime.of(2025, 11, 10, 9, 0));

    EventBuilder result = EventProperty.SUBJECT.apply(builder, "Meeting");

    assertSame(builder, result);
    Event event = result.build();
    assertEquals("Meeting", event.getSubject());
  }

  /**
   * Test apply with START returns the same builder.
   */
  @Test
  public void testApply_start_returnsSameBuilder() {
    EventBuilder builder = new EventBuilder()
        .subject("Test")
        .startDateTime(LocalDateTime.of(2025, 11, 10, 9, 0));

    EventBuilder result = EventProperty.START.apply(builder, "2025-11-10T10:00");

    assertSame(builder, result);
    Event event = result.build();
    assertEquals(LocalDateTime.of(2025, 11, 10, 10, 0), event.getStartDateTime());
  }

  /**
   * Test apply with END returns the same builder.
   */
  @Test
  public void testApply_end_returnsSameBuilder() {
    EventBuilder builder = new EventBuilder()
        .subject("Test")
        .startDateTime(LocalDateTime.of(2025, 11, 10, 9, 0))
        .endDateTime(LocalDateTime.of(2025, 11, 10, 10, 0));

    EventBuilder result = EventProperty.END.apply(builder, "2025-11-10T11:00");

    assertSame(builder, result);
    Event event = result.build();
    assertEquals(LocalDateTime.of(2025, 11, 10, 11, 0), event.getEndDateTime());
  }

  /**
   * Test apply with DESCRIPTION returns the same builder.
   */
  @Test
  public void testApply_description_returnsSameBuilder() {
    EventBuilder builder = new EventBuilder()
        .subject("Test")
        .startDateTime(LocalDateTime.of(2025, 11, 10, 9, 0));

    EventBuilder result = EventProperty.DESCRIPTION.apply(builder, "Important meeting");

    assertSame(builder, result);
    Event event = result.build();
    assertTrue(event.getDescription().isPresent());
    assertEquals("Important meeting", event.getDescription().get());
  }

  /**
   * Test apply with LOCATION returns the same builder.
   */
  @Test
  public void testApply_location_returnsSameBuilder() {
    EventBuilder builder = new EventBuilder()
        .subject("Test")
        .startDateTime(LocalDateTime.of(2025, 11, 10, 9, 0));

    EventBuilder result = EventProperty.LOCATION.apply(builder, "Room 101");

    assertSame(builder, result);
    Event event = result.build();
    assertTrue(event.getLocation().isPresent());
    assertEquals("Room 101", event.getLocation().get());
  }

  /**
   * Test apply with STATUS returns the same builder.
   */
  @Test
  public void testApply_status_returnsSameBuilder() {
    EventBuilder builder = new EventBuilder()
        .subject("Test")
        .startDateTime(LocalDateTime.of(2025, 11, 10, 9, 0));

    EventBuilder result = EventProperty.STATUS.apply(builder, "private");

    assertSame(builder, result);
    Event event = result.build();
    assertFalse(event.isPublic());
  }

  /**
   * Test fromToken with all valid tokens.
   */
  @Test
  public void testFromToken_allValidTokens() {
    assertEquals(EventProperty.SUBJECT, EventProperty.fromToken("subject"));
    assertEquals(EventProperty.START, EventProperty.fromToken("start"));
    assertEquals(EventProperty.END, EventProperty.fromToken("end"));
    assertEquals(EventProperty.DESCRIPTION, EventProperty.fromToken("description"));
    assertEquals(EventProperty.LOCATION, EventProperty.fromToken("location"));
    assertEquals(EventProperty.STATUS, EventProperty.fromToken("status"));
  }
}