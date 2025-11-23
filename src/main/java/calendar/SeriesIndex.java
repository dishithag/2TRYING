package calendar;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Index of series occurrences for fast lookup and maintenance.
 * Keys are series identifiers; values are ordered start-times of occurrences.
 * This index is rebuildable from the authoritative event list.
 */
public final class SeriesIndex {

  private final ConcurrentMap<String, NavigableSet<LocalDateTime>> index =
      new ConcurrentHashMap<>();

  /**
   * Clears and rebuilds the index from a list of events.
   *
   * @param events source of truth
   */
  public void rebuild(List<Event> events) {
    index.clear();
    for (Event e : events) {
      e.getSeriesId().ifPresent(id -> add(id, e.getStartDateTime()));
    }
  }

  /**
   * Records a new series occurrence.
   *
   * @param seriesId series id
   * @param start start of occurrence
   */
  public void add(String seriesId, LocalDateTime start) {
    Objects.requireNonNull(seriesId, "seriesId");
    Objects.requireNonNull(start, "start");
    index.computeIfAbsent(seriesId, k -> new TreeSet<>()).add(start);
  }

  /**
   * Removes an existing series occurrence.
   *
   * @param seriesId series id
   * @param start start of occurrence
   */
  public void remove(String seriesId, LocalDateTime start) {
    NavigableSet<LocalDateTime> set = index.get(seriesId);
    if (set != null) {
      set.remove(start);
      if (set.isEmpty()) {
        index.remove(seriesId);
      }
    }
  }

  /**
   * Replaces an occurrence start-time within a series.
   *
   * @param seriesId series id
   * @param oldStart old start
   * @param newStart new start
   */
  public void replaceStart(String seriesId, LocalDateTime oldStart, LocalDateTime newStart) {
    NavigableSet<LocalDateTime> set = index.computeIfAbsent(seriesId, k -> new TreeSet<>());
    set.remove(oldStart);
    set.add(newStart);
  }

  /**
   * Returns the recorded start-times for a series in ascending order.
   *
   * @param seriesId series id
   * @return ascending list of starts
   */
  public List<LocalDateTime> starts(String seriesId) {
    NavigableSet<LocalDateTime> set = index.get(seriesId);
    if (set == null) {
      return new ArrayList<>();
    }
    return new ArrayList<>(set);
  }
}
