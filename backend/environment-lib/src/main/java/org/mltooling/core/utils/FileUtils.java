package org.mltooling.core.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.activation.MimetypesFileTypeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileUtils {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

  // Initialized with default list from here:
  // http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types  -> META-INF/mime.types
  private static final MimetypesFileTypeMap MIME_TYPE_MAPPER = new MimetypesFileTypeMap();

  // ================ Members ============================================= //
  static {
    MIME_TYPE_MAPPER.addMimeTypes("application/x-ipynb+json ipynb");
    // Unified model extensions: pyp (Python predictor), model, um (Unified Model)
    // python pickle
    // tensorflow, keras, ... models
  }

  // ================ Constructors & Main ================================= //
  private FileUtils() {}

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public static String getContentType(String fileName) {
    // Try different versions to get content type:
    // http://www.rgagnon.com/javadetails/java-0487.html
    // https://dzone.com/articles/determining-file-types-java
    // e.g. use Files.probeContentType(file.toPath()); if filename == path
    return MIME_TYPE_MAPPER.getContentType(fileName);
  }

  public static synchronized void deleteFile(String filePath) {
    if (StringUtils.isNullOrEmpty(filePath)) {
      log.info("File path is null or empty");
      return;
    }

    File file = new File(filePath);

    if (!file.exists()) {
      log.info(filePath + " does not exist.");
      return;
    }

    if (file.delete()) {
      // logger.info(file.getName() + " is deleted!");
      return;
    } else {
      log.warn(filePath + " failed to be deleted!");
    }
  }

  /** Determine whether a file is a ZIP File. */
  public static boolean isZipFile(File file) {
    if (file.isDirectory()) {
      return false;
    }
    if (!file.canRead()) {
      log.warn("Cannot read file " + file.getAbsolutePath());
      return false;
    }
    if (file.length() < 4) {
      return false;
    }
    try {

      DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
      int test = in.readInt();
      in.close();
      return test == 0x504b0304;
    } catch (IOException e) {
      log.warn("Cannot read file " + file.getAbsolutePath(), e);
      return false;
    }
  }

  public static String readFile(File file) throws IOException {
    return readFile(file.getPath());
  }

  public static String readFile(Path filePath) throws IOException {
    return readFile(filePath.toAbsolutePath().toString());
  }

  public static String readFile(String filePath) throws IOException {
    return readFile(filePath, "UTF-8");
  }

  public static String readFile(String filePath, String charsetName) throws IOException {
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      return null;
    }
    byte[] encoded = Files.readAllBytes(path);
    return new String(encoded, charsetName);
  }

  public static List<String> readLinesToList(File file) {
    List<String> lines = new ArrayList<String>();

    try (BufferedReader br =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(file), StringUtils.UTF_8_CHARSET))) {

      String line;
      while ((line = br.readLine()) != null) {
        if (!StringUtils.isNullOrEmpty(line)) {
          line = line.trim();
          if (!StringUtils.isNullOrEmpty(line)) {
            lines.add(line);
          }
        }
      }
      br.close();

      return lines;
    } catch (IOException e) {
      log.info("error while reading file " + e.getMessage());
    }
    return lines;
  }

  public static List<String> readLinesToList(String filePath) {
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      log.warn("File not found!");
      return new ArrayList<String>();
    }

    return readLinesToList(new File(filePath));
  }

  public static Properties initPropertiesFile(String filePath) {
    try {
      Properties properties = new Properties();
      properties.load(Files.newInputStream(Paths.get(filePath)));
      return properties;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  public static String fileToString(File file) throws IOException {
    try (BufferedReader reader = new BufferedReader(
      new InputStreamReader(new FileInputStream(file), StringUtils.UTF_8_CHARSET))) {
      String line;
      StringBuilder stringBuilder = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line);
        stringBuilder.append("\n");
      }

      return stringBuilder.toString();
    }
  }

  public static synchronized void appendToCsv(
      String filePath, List<String> cells, String outputDelimiter) {
    String fileContent = "";
    for (String cell : cells) {
      if (cell == null) {
        cell = "";
      }
      fileContent += cell + outputDelimiter;
    }
    fileContent = StringUtils.removeLastChar(fileContent);

    appendToCsv(filePath, fileContent);
  }

  public static synchronized void appendToCsv(String filePath, String fileContent) {
    if (StringUtils.isNullOrEmpty(fileContent)) {
      log.warn("file content is null or empty");
      return;
    }

    File file = new File(filePath);
    if (!file.exists()) {

      file = createFile(filePath);
      if (file == null) {
        return;
      }

      try {
        // Write first bytes to indicate UTF-8
        OutputStream os = new FileOutputStream(filePath);
        os.write(239);
        os.write(187);
        os.write(191);

        FileOutputStream fileStream = new FileOutputStream(file, true);
        OutputStreamWriter fw = new OutputStreamWriter(fileStream, StringUtils.UTF_8_CHARSET);
        fw.write(fileContent); // appends the string to the file
        fw.close();

      } catch (IOException ex1) {
        log.info("error writing file " + file.getName());
      }
    } else {
      try {
        FileOutputStream fileStream = new FileOutputStream(file, true);
        OutputStreamWriter fw = new OutputStreamWriter(fileStream, StringUtils.UTF_8_CHARSET);
        // FileWriter fw = new FileWriter(file, true);
        fw.write(
            System.getProperty("line.separator") + fileContent); // appends the string to the file
        fw.close();
      } catch (IOException e) {
        log.info("error initializing file writer " + e.getMessage());
      }
    }
  }

  public static void appendStrToFile(String filePath, String fileContent) {
    if (StringUtils.isNullOrEmpty(fileContent)) {
      log.warn("file content is null or empty");
      return;
    }

    File file = new File(filePath);
    if (!file.exists()) {
      writeStrToFile(filePath, fileContent);
    } else {
      try {
        FileOutputStream fileStream = new FileOutputStream(file, true);
        OutputStreamWriter fw = new OutputStreamWriter(fileStream, "UTF-8");
        // FileWriter fw = new FileWriter(file, true);
        fw.write(
            System.getProperty("line.separator") + fileContent); // appends the string to the file
        fw.close();
      } catch (IOException e) {
        log.info("error initializing file writer " + e.getMessage());
      }
    }
  }

  public static File getResourceFile(String resourcePath) {
    return getResourceFile(resourcePath, false);
  }

  public static File getResourceFile(String resourcePath, boolean keepFilename) {
    InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
    if (inputStream != null) {
      if (keepFilename) {
        return stream2File(inputStream, new File(resourcePath).getName());
      } else {
        return stream2File(inputStream);
      }
    }
    return null;
  }

  public static synchronized File stream2File(InputStream in, String tempFilename) {
    try {
      final File tempFile = File.createTempFile(tempFilename, ".tmp");
      tempFile.deleteOnExit();
      try (FileOutputStream out = new FileOutputStream(tempFile)) {
        copyStream(in, out);
      }
      return tempFile;
    } catch (IOException e) {
      log.warn("Failed to convert InputStream to temp file " + e.getMessage());
    }
    return null;
  }

  public static synchronized File stream2File(InputStream in) {
    return stream2File(in, String.valueOf(in.hashCode()));
  }

  public static synchronized void writeStrToFile(String filePath, String fileContent) {
    File file = createFile(filePath);
    if (file == null || fileContent == null) {
      log.warn("File is null " + file.getName());
      return;
    }

    Writer writer = null;

    try {
      writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
      writer.write(fileContent);

    } catch (IOException ex1) {
      log.info("error writing file " + file.getName());
    } finally {
      try {
        if (writer != null) {
          writer.close();
        }
      } catch (Exception ex1) {
        log.info("error closing writer: " + ex1.getMessage());
      }
    }
  }

  public static synchronized void writeStringsToFile(
      String filePath, Collection<String> fileContent) {
    File file = createFile(filePath);
    if (file == null || fileContent == null) {
      log.warn("File is null " + file.getName());
      return;
    }

    Writer writer = null;
    try {
      writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));

      for (String str : fileContent) {
        writer.write(str);
        writer.write("\n");
      }
      writer.flush();
    } catch (IOException ex1) {
      log.info("error writing file " + file.getName());
    } finally {
      try {
        if (writer != null) {
          writer.close();
        }
      } catch (Exception ex1) {
        log.info("error closing writer: " + ex1.getMessage());
      }
    }
  }

  public static String getFileExtension(String fileName) {
    if (StringUtils.isNullOrEmpty(fileName)) {
      return null;
    }

    String extension = "";

    int i = fileName.lastIndexOf('.');
    if (i > 0) {
      extension = fileName.substring(i + 1);
    }
    return extension;
  }

  public static File copyFile(String sourceFilePath, String targetFilePath, boolean isOverriding) {
    InputStream is;
    try {
      is = new FileInputStream(sourceFilePath);
    } catch (FileNotFoundException e) {
      log.warn("Could not find file to move");
      return null;
    }

    return copyFile(is, targetFilePath, isOverriding);
  }

  /** target path also contains filename */
  public static synchronized File copyFile(
      InputStream is, String targetFilePath, boolean isOverriding) {
    File file = new File(targetFilePath);
    file.getParentFile().mkdirs();

    if (file.exists() && isOverriding == false) {
      return null;
    }

    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        log.warn("Could not create file in target dir : " + targetFilePath);
        return null;
      }
    }

    OutputStream os;
    try {
      os = new FileOutputStream(file.getAbsolutePath(), false);

      try {
        copyStream(is, os);
      } catch (IOException e) {
        log.warn("Could not copy stream");
        return null;
      }
    } catch (FileNotFoundException e) {
      log.warn("Could not find directory to move to: " + file.getAbsolutePath());
      return null;
    }

    try {
      is.close();
      os.close();
    } catch (IOException e) {
      log.warn("Could not close streams");
      return null;
    }

    return file;
  }

  public static void shuffleFile(String filePath) {
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      log.warn("File not found!");
      return;
    }

    List<String> lines = new ArrayList<String>();

    try (BufferedReader br =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(filePath), StringUtils.UTF_8_CHARSET))) {

      String line;
      while ((line = br.readLine()) != null) {
        if (!StringUtils.isNullOrEmpty(line)) {
          line = line.trim();
          if (!StringUtils.isNullOrEmpty(line)) {
            lines.add(line);
          }
        }
      }
      br.close();

      Collections.shuffle(lines);

      if (lines.size() < 1) {
        return;
      }

      deleteFile(filePath);

      File file = new File(filePath);
      if (!file.exists()) {
        writeStrToFile(filePath, lines.get(0));
      }

      FileWriter fw = new FileWriter(file, true);

      for (int i = 1; i < lines.size(); i++) {
        fw.write(System.getProperty("line.separator") + lines.get(i));
      }
      fw.close();

    } catch (IOException e) {
      log.info("error while writing file " + e.getMessage());
    }
  }

  public static Set<String> mergeCsvFilesWithoutDuplicateLines(File... files) {
    Set<String> lines = new LinkedHashSet<>();
    for (File f : files) {
      lines.addAll(readLinesToList(f));
    }

    return lines;
  }

  public static void copyStream(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[1024]; // Adjust if you want
    int bytesRead;
    while ((bytesRead = input.read(buffer)) != -1) {
      output.write(buffer, 0, bytesRead);
    }
  }

  public static int countLines(File inputFile) throws IOException {
    InputStream is = new BufferedInputStream(new FileInputStream(inputFile));
    try {
      byte[] c = new byte[1024];
      int count = 0;
      int readChars = 0;
      boolean empty = true;
      while ((readChars = is.read(c)) != -1) {
        empty = false;
        for (int i = 0; i < readChars; ++i) {
          if (c[i] == '\n') {
            ++count;
          }
        }
      }
      return (count == 0 && !empty) ? 1 : count;
    } finally {
      is.close();
    }
  }

  public static synchronized File createOrCleanFile(String filePath) {
    if (StringUtils.isNullOrEmpty(filePath)) {
      log.warn("File path is null or empty");
      return null;
    }

    File file = new File(filePath);
    file.getParentFile().mkdirs();

    Writer writer = null;

    try {
      writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
      writer.write("");

    } catch (IOException ex1) {
      log.info("error writing file " + file.getName());
    } finally {
      try {
        if (writer != null) {
          writer.close();
        }
      } catch (Exception ex1) {
        log.info("error closing writer: " + ex1.getMessage());
      }
    }
    return file;
  }

  /* needs an external library
  public static String checkEncoding(String fileName) {
      byte[] buf = new byte[4096];
      FileInputStream fileInputStream;
      try {
          fileInputStream = new FileInputStream(fileName);

          // (1)
          UniversalDetector detector = new UniversalDetector(null);

          // (2)
          int nread;
          while ((nread = fileInputStream.read(buf)) > 0 && !detector.isDone()) {
              detector.handleData(buf, 0, nread);
          }
          // (3)
          detector.dataEnd();

          // (4)
          String encoding = detector.getDetectedCharset();

          // (5)
          detector.reset();
          fileInputStream.close();
          return encoding;
      } catch (IOException e) {
          e.printStackTrace();
      }
      return null;
  }*/

  public static String transformEncoding(
      String text, String sourceEncoding, String targetEncoding) {
    try {
      byte[] textBytes = text.getBytes(sourceEncoding);
      return new String(textBytes, targetEncoding);
    } catch (UnsupportedEncodingException e) {
      return "";
    }
  }

  // ================ Private Methods ===================================== //

  private static synchronized File createFile(String filePath) {
    if (StringUtils.isNullOrEmpty(filePath)) {
      log.warn("File path is null or empty");
      return null;
    }

    File file = new File(filePath);
    file.getParentFile().mkdirs();

    return file;
  }
  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
