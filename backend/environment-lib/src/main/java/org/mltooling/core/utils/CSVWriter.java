package org.mltooling.core.utils;

import java.io.*;
import java.util.*;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVWriter {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(CSVWriter.class);

  private static final String DEFAULT_OUTPUT_DELIMITER = ";";
  private static final String DEFAULT_QUOTES = ""; // don't quote as default
  private static final String SYSTEM_LINE_SEPERATOR = System.getProperty("line.separator");

  // ================ Members ============================================= //
  private String outputDelimiter = DEFAULT_OUTPUT_DELIMITER;
  private String quoteChar = DEFAULT_QUOTES;

  private List<String> headers;
  private Writer writer;
  private boolean initialized = false;

  // ================ Constructors & Main ================================= //
  public CSVWriter(Writer writer) {
    this(writer, null);
  }

  public CSVWriter(Writer writer, @Nullable List<String> headers) {
    this.writer = writer;
    this.headers = headers;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public void write(Map output) throws IOException {
    if (!ListUtils.isNullOrEmpty(headers)) {
      if (!initialized) {
        writeHeader();
        initialized = true;
      }

      StringBuilder outputStr = new StringBuilder();
      for (String field : headers) {
        if (output.containsKey(field) && output.get(field) != null) {
          outputStr.append(cleanField(output.get(field).toString()));
        }
        outputStr.append(outputDelimiter);
      }
      write(StringUtils.removeLastChar(outputStr.toString()));
    } else {
      // no headers provided, only write values
      this.write(output.values());
    }
  }

  public void write(Collection fields) throws IOException {
    StringBuilder outputStr = new StringBuilder();
    for (Object value : fields) {
      if (value != null) {
        outputStr.append(cleanField(value.toString()));
      }
      outputStr.append(outputDelimiter);
    }
    String line = StringUtils.removeLastChar(outputStr.toString());
    write(line);
  }

  public void write(String line) throws IOException {
    if (!initialized) {
      writer.write(line);
      initialized = true;
    } else {
      writer.write(StringUtils.NEW_LINE + line);
    }
  }

  public static List<Map<String, String>> loadCSV(File csvFile) {
    return loadCSV(csvFile, DEFAULT_OUTPUT_DELIMITER);
  }

  public static List<Map<String, String>> loadCSV(File csvFile, String outputDelimiter) {
    List<Map<String, String>> experimentRows = new ArrayList<>();
    try {
      BufferedReader reader = null;
      try {
        reader =
            new BufferedReader(
                new InputStreamReader(new FileInputStream(csvFile), StringUtils.UTF_8_CHARSET));

        String line = reader.readLine();
        if (StringUtils.isNullOrEmpty(line)) {
          log.info("Header row is empty");
          return experimentRows;
        }

        List<String> columnHeaders = new ArrayList<>();
        String[] splittedLine = line.split(outputDelimiter);
        for (String columnHeader : splittedLine) {
          if (columnHeader.startsWith("\ufeff")) {
            columnHeader = columnHeader.replace("\uFEFF", "");
          }
          columnHeaders.add(columnHeader);
        }

        while ((line = reader.readLine()) != null) {
          if (!StringUtils.isNullOrEmpty(line)) {

            splittedLine = line.split(outputDelimiter, -1);
            if (splittedLine.length != columnHeaders.size()) {
              log.info("Row has different size from header.");
              continue;
            }
            Map<String, String> data = new HashMap<>();
            for (int i = 0; i < splittedLine.length; i++) {
              data.put(columnHeaders.get(i), splittedLine[i]);
            }
            experimentRows.add(data);
          }
        }
      } catch (FileNotFoundException e) {
        log.info("File not found. " + e.getMessage());
      } finally {
        if (reader != null) {
          reader.close();
        }
      }
    } catch (IOException e) {
      log.info("Exception while reading the file " + e.getMessage());
    }
    return experimentRows;
  }

  public void close() {
    if (writer != null) {
      try {
        writer.flush();
        writer.close();
        writer = null;
      } catch (IOException e) {
        log.error("Failed to close FileWriter");
      }
    }
  }

  // ================ Private Methods ===================================== //
  private String cleanField(String field) {
    if (StringUtils.isNullOrEmpty(field)) {
      return "";
    }

    field =
        field
            .replace(outputDelimiter, " ")
            .replace("\"", " ")
            .replace(SYSTEM_LINE_SEPERATOR, " ")
            .replaceAll("\\r?\\n|\\r", " ");

    if (!StringUtils.isNullOrEmpty(quoteChar)) {
      field = field.replace(quoteChar, " ");
    }

    field = field.trim();

    return quoteChar + field + quoteChar;
  }

  private void writeHeader() {
    if (ListUtils.isNullOrEmpty(headers)) {
      return;
    }

    StringBuilder outputStr = new StringBuilder();
    for (String field : headers) {
      outputStr.append(field).append(outputDelimiter);
    }
    outputStr = new StringBuilder(StringUtils.removeLastChar(outputStr.toString()));
    try {
      write(outputStr.toString());
    } catch (IOException e) {
      log.error("Failed to write headers", e);
    }
  }

  // ================ Getter & Setter ===================================== //

  public String getOutputDelimiter() {
    return outputDelimiter;
  }

  public CSVWriter setOutputDelimiter(String outputDelimiter) {
    this.outputDelimiter = outputDelimiter;
    return this;
  }

  public String getQuoteChar() {
    return quoteChar;
  }

  public CSVWriter setQuoteChar(String quoteChar) {
    this.quoteChar = quoteChar;
    return (CSVWriter) this;
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
