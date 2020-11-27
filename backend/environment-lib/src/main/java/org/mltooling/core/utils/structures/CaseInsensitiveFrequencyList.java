package org.mltooling.core.utils.structures;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import org.mltooling.core.utils.StringUtils;

public class CaseInsensitiveFrequencyList extends FrequencyList<String> {

  // ================ Constants =========================================== //
  private static final String DEFAULT_DELIMITER = ";";

  // ================ Members ============================================= //
  public CaseInsensitiveFrequencyList() {
    super();
  }

  public CaseInsensitiveFrequencyList(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  // ================ Constructors & Main ================================= //

  public static CaseInsensitiveFrequencyList loadFromFile(File file) {
    return loadFromFile(file, DEFAULT_DELIMITER);
  }

  public static CaseInsensitiveFrequencyList loadFromFile(File file, String outputDelimiter) {
    CaseInsensitiveFrequencyList frequencyList = new CaseInsensitiveFrequencyList();
    try {
      BufferedReader reader = null;
      try {
        reader =
            new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StringUtils.UTF_8_CHARSET));
        String line = null;
        boolean firstLineFlag = true;
        while ((line = reader.readLine()) != null) {
          if (!StringUtils.isNullOrEmpty(line)) {
            if (firstLineFlag) {
              firstLineFlag = false;
              continue;
            }
            String[] slittedLine = line.split(outputDelimiter);
            if (slittedLine.length != 2) {
              continue;
            }
            String key = slittedLine[0];
            int frequency = Integer.valueOf(slittedLine[1]);
            frequencyList.add(key, frequency);
          }
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } finally {
        if (reader != null) {
          reader.close();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return frequencyList;
  }

  @Override
  public boolean contains(Object key) {
    return super.contains(lowerCaseKey(key));
  }

  public boolean add(String key, int keyFreq) {
    return super.add(lowerCaseKey(key), keyFreq);
  }

  @Override
  public boolean add(String key) {
    return super.add(lowerCaseKey(key));
  }

  @Override
  public boolean remove(Object key) {
    return super.remove(lowerCaseKey(key));
  }

  @Override
  public boolean containsAll(Collection<?> items) {
    return super.containsAll(lowerCaseCollection(items));
  }

  @Override
  public boolean addAll(Collection<? extends String> items) {
    Collection<String> newCollection = new ArrayList<>();
    for (Object key : items) {
      newCollection.add(lowerCaseKey(key));
    }

    return super.addAll(newCollection);
  }

  @Override
  public boolean retainAll(Collection<?> items) {
    return super.retainAll(lowerCaseCollection(items));
  }

  @Override
  public boolean removeAll(Collection<?> items) {
    return super.removeAll(lowerCaseCollection(items));
  }

  @Override
  public int getFreq(String key) {
    return super.getFreq(lowerCaseKey(key));
  }

  @Override
  public String toString() {
    StringBuilder statsOutput = new StringBuilder();
    for (String key : this.getSortedList()) {
      statsOutput.append(key).append(": ").append(this.getFreq(key)).append("; ");
    }
    return statsOutput.toString();
  }

  // ================ Public Methods ====================================== //
  public void writeToFile(File file, int minFreq) {
    writeToFile(file, minFreq, DEFAULT_DELIMITER);
  }

  public void writeToFile(File file, int minFreq, String outputDelimiter) {
    try {
      FileOutputStream fileStream = new FileOutputStream(file, true);
      BufferedWriter fileWriter =
          new BufferedWriter(new OutputStreamWriter(fileStream, StringUtils.UTF_8_CHARSET));
      fileWriter.write("key" + outputDelimiter + "frequency");
      for (String key : this.getSortedList(true, minFreq)) {
        int freq = getFreq(key);
        key = key.replace(outputDelimiter, ",");
        fileWriter.write(System.getProperty("line.separator") + key + outputDelimiter + freq);
      }
      fileWriter.close();
      fileStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //
  private Collection<?> lowerCaseCollection(Collection<?> items) {
    Collection<String> newCollection = new ArrayList<>();
    for (Object key : items) {
      newCollection.add(lowerCaseKey(key));
    }
    return newCollection;
  }

  private String lowerCaseKey(Object key) {
    return ((String) key).toLowerCase();
  }
  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
