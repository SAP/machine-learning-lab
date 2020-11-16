package org.mltooling.core.utils.structures;

import java.util.*;

public class FrequencyList<Key> implements Set<Key> {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  private Map<Key, MutableInt> freqHashMap = new HashMap<>();

  // ================ Constructors & Main ================================= //
  public FrequencyList() {
    freqHashMap = new HashMap<>();
  }

  public FrequencyList(int initialCapacity, float loadFactor) {
    freqHashMap = new HashMap<>(initialCapacity, loadFactor);
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //
  @Override
  public int size() {
    return freqHashMap.size();
  }

  @Override
  public boolean isEmpty() {
    return freqHashMap.isEmpty();
  }

  @Override
  public boolean contains(Object key) {
    return freqHashMap.containsKey(key);
  }

  @Override
  public Iterator<Key> iterator() {
    return freqHashMap.keySet().iterator();
  }

  @Override
  public Object[] toArray() {
    return freqHashMap.keySet().toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return freqHashMap.keySet().toArray(a);
  }

  public boolean add(Key key, int keyFreq) {
    freqHashMap.put(key, new MutableInt(keyFreq));
    return true;
  }

  @Override
  public boolean add(Key key) {
    MutableInt count = freqHashMap.get(key);
    if (count == null) {
      freqHashMap.put(key, new MutableInt());
    } else {
      count.increment();
    }
    return true;
  }

  @Override
  public boolean remove(Object key) {
    return freqHashMap.remove(key) != null;
  }

  @Override
  public boolean containsAll(Collection<?> items) {
    boolean containsAll = true;
    for (Object key : items) {
      if (freqHashMap.containsKey(key)) {
        containsAll = false;
        break;
      }
    }
    return containsAll;
  }

  @Override
  public boolean addAll(Collection<? extends Key> items) {
    for (Key key : items) {
      add(key);
    }
    return true;
  }

  @Override
  public boolean retainAll(Collection<?> items) {
    for (Key key : freqHashMap.keySet()) {
      if (!items.contains(key)) {
        items.remove(key);
      }
    }
    return true;
  }

  @Override
  public boolean removeAll(Collection<?> items) {
    for (Object key : items) {
      freqHashMap.remove(key);
    }
    return true;
  }

  @Override
  public void clear() {
    freqHashMap.clear();
  }

  // ================ Public Methods ====================================== //
  public List<Key> getSortedList() {
    return getSortedList(true, 0);
  }

  public List<Key> getSortedList(final boolean descending, int minFreq) {

    List<Map.Entry<Key, MutableInt>> list = new LinkedList<>(freqHashMap.entrySet());

    // Sorting the list based on values
    Collections.sort(
        list,
        new Comparator<Map.Entry<Key, MutableInt>>() {

          @Override
          public int compare(Map.Entry<Key, MutableInt> o1, Map.Entry<Key, MutableInt> o2) {
            if (descending) {
              return Integer.compare(o2.getValue().get(), o1.getValue().get());
            } else {
              return Integer.compare(o1.getValue().get(), o2.getValue().get());
            }
          }
        });

    List<Key> sortedKeys = new LinkedList<>();

    for (Map.Entry<Key, MutableInt> entry : list) {
      if (entry.getValue().get() >= minFreq) {
        sortedKeys.add(entry.getKey());
      }
    }

    return sortedKeys;
  }

  public int getFreq(Key key) {
    MutableInt count = freqHashMap.get(key);
    if (count == null) {
      return 0;
    }
    return count.get();
  }

  public Key getMostFreqKey() {
    int highestFreq = 0;
    Key mostFreqKey = null;

    for (Key key : freqHashMap.keySet()) {
      int freq = getFreq(key);
      if (freq > highestFreq) {
        highestFreq = freq;
        mostFreqKey = key;
      }
    }
    return mostFreqKey;
  }

  // ================ Private Methods ===================================== //
  private void init() {}

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
  class MutableInt {

    int value = 1; // note that we start at 1 since we're counting

    public MutableInt() {}

    public MutableInt(int value) {
      this.value = value;
    }

    public void increment() {
      ++value;
    }

    public int get() {
      return value;
    }
  }
}
