import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import calendar.Calendar;
import calendar.CalendarImpl;
import calendar.Event;
import calendar.SeriesIndex;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for SeriesIndex.
 */
public class SeriesIndexTest {

  private SeriesIndex index;

  /**
   * Sets up a fresh SeriesIndex instance before each test.
   */
  @Before
  public void setUp() {
    index = new SeriesIndex();
  }

  /**
   * Test rebuild clears existing entries before rebuilding.
   * Kills mutation on line 28 (removed call to clear).
   */
  @Test
  public void testRebuild_clearsExistingEntries() {

    index.add("SERIES_1", LocalDateTime.of(2025, 11, 10, 9, 0));
    index.add("SERIES_1", LocalDateTime.of(2025, 11, 11, 9, 0));


    List<LocalDateTime> before = index.starts("SERIES_1");
    assertEquals(2, before.size());


    Calendar cal = new CalendarImpl();
    List<Event> events = cal.createEventSeries("New Series",
        LocalDateTime.of(2025, 11, 15, 10, 0),
        LocalDateTime.of(2025, 11, 15, 11, 0),
        EnumSet.of(DayOfWeek.MONDAY), 2);


    index.rebuild(events);


    List<LocalDateTime> oldSeries = index.starts("SERIES_1");
    assertEquals(0, oldSeries.size());


    String newSeriesId = events.get(0).getSeriesId().get();
    List<LocalDateTime> newSeries = index.starts(newSeriesId);
    assertEquals(2, newSeries.size());
  }

  /**
   * Test remove when seriesId doesn't exist (set is null).
   * Kills mutation on line 54 (negated conditional).
   */
  @Test
  public void testRemove_seriesNotFound_doesNotThrow() {

    index.remove("NON_EXISTENT", LocalDateTime.of(2025, 11, 10, 9, 0));


    List<LocalDateTime> starts = index.starts("NON_EXISTENT");
    assertEquals(0, starts.size());
  }

  /**
   * Test remove when it empties the set, causing removal from index.
   * Kills mutation on line 56 (negated conditional).
   */
  @Test
  public void testRemove_lastOccurrence_removesSeriesFromIndex() {
    LocalDateTime start = LocalDateTime.of(2025, 11, 10, 9, 0);


    index.add("SERIES_1", start);


    List<LocalDateTime> before = index.starts("SERIES_1");
    assertEquals(1, before.size());


    index.remove("SERIES_1", start);

    List<LocalDateTime> after = index.starts("SERIES_1");
    assertEquals(0, after.size());
  }

  /**
   * Test replaceStart creates new set if series doesn't exist.
   * Provides coverage for line 70 (computeIfAbsent lambda).
   */
  @Test
  public void testReplaceStart_nonExistentSeries_createsNewSet() {
    LocalDateTime oldStart = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime newStart = LocalDateTime.of(2025, 11, 10, 10, 0);


    index.replaceStart("NEW_SERIES", oldStart, newStart);


    List<LocalDateTime> starts = index.starts("NEW_SERIES");
    assertEquals(1, starts.size());
    assertEquals(newStart, starts.get(0));
  }

  /**
   * Test starts returns a new empty list each time when series not found.
   * Kills mutation on line 84 (replaced return with Collections.emptyList).
   */
  @Test
  public void testStarts_notFound_returnsNewListEachTime() {
    List<LocalDateTime> list1 = index.starts("NON_EXISTENT");
    List<LocalDateTime> list2 = index.starts("NON_EXISTENT");


    assertTrue(list1.isEmpty());
    assertTrue(list2.isEmpty());
    assertTrue(list1 != list2);  // Different instances
  }

  /**
   * Test add and starts basic functionality.
   */
  @Test
  public void testAdd_andStarts_basicFunctionality() {
    LocalDateTime start1 = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 11, 12, 9, 0);

    index.add("SERIES_1", start1);
    index.add("SERIES_1", start2);

    List<LocalDateTime> starts = index.starts("SERIES_1");
    assertEquals(2, starts.size());
    assertEquals(start1, starts.get(0));  // Should be sorted
    assertEquals(start2, starts.get(1));
  }

  /**
   * Test replaceStart with existing series.
   */
  @Test
  public void testReplaceStart_existingSeries_updatesCorrectly() {
    LocalDateTime oldStart = LocalDateTime.of(2025, 11, 10, 9, 0);
    LocalDateTime newStart = LocalDateTime.of(2025, 11, 10, 10, 0);
    LocalDateTime otherStart = LocalDateTime.of(2025, 11, 12, 9, 0);


    index.add("SERIES_1", oldStart);
    index.add("SERIES_1", otherStart);


    index.replaceStart("SERIES_1", oldStart, newStart);


    List<LocalDateTime> starts = index.starts("SERIES_1");
    assertEquals(2, starts.size());
    assertEquals(newStart, starts.get(0));
    assertEquals(otherStart, starts.get(1));
  }
}