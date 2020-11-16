package org.mltooling.core.env.handler;

import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.mltooling.core.lab.model.LabFileDataType;

public class FileHandlerUtils {

  // ================ Constants =========================================== //

  public static final String FILE_VERSION_SUFFIX = ".v";
  public static final Pattern FILE_VERSION_SUFFIX_PATTERN =
      Pattern.compile(FILE_VERSION_SUFFIX + "(\\d+)$");

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  /**
   * If the fileKey does not contain a version, the version '1' is returned
   *
   * @param fileKey which may contain a version information
   * @return the version within the fileKey. If not existing, 1
   */
  public static Integer getVersionFromKey(String fileKey) {
    Matcher matcher = FILE_VERSION_SUFFIX_PATTERN.matcher(fileKey);
    if (matcher.find()) {
      return Integer.valueOf(matcher.group(1));
    }
    return 1;
  }

  /**
   * Return the full qualified name (fileKey) based on the fileName and the dataType.
   *
   * @param fileName name of the file. If null, all files of dataType are returned
   * @param dataType folder to be searched for
   * @return the full qualified name (fileKey)
   * @throws Exception if data type is {@link LabFileDataType#UNKNOWN}
   */
  public static String resolveKey(@Nullable String fileName, LabFileDataType dataType) {
    if (dataType.isUnknown()) {
      return null;
    }

    if (fileName == null) {
      fileName = "";
    }

    return dataType.getDefaultFolder() + "/" + fileName;
  }

  /**
   * Returns the pure S3 filename without the version name
   *
   * @param fileKey may or may not contain the version
   * @return the fileKey without version suffix
   */
  public static String getFileNameFromKey(String fileKey, boolean removeVersion) {

    if (removeVersion) {
      return removeVersionFromKey(Paths.get(fileKey).getFileName().toString());
    }
    return Paths.get(fileKey).getFileName().toString();
  }

  public static String getFolderPathFromKey(String fileKey) {
    return Paths.get(fileKey).getParent().toString();
  }

  /**
   * Return type information that is part of the fileKey (e.g. myFile.v3.zip) -> zip
   *
   * @param fileKey
   * @return type, e.g. 'zip' or 'txt'
   */
  public static String getFileTypeFromKey(String fileKey) {
    FileHandlerUtils.getVersionFromKey(fileKey);
    fileKey = removeVersionFromKey(fileKey);

    if (!fileKey.contains(".") || fileKey.endsWith(".")) {
      return "-";
    }

    return fileKey.substring(fileKey.lastIndexOf(".") + 1);
  }

  public static String removeVersionFromKey(String fileKey) {
    return fileKey.replaceAll(FileHandlerUtils.FILE_VERSION_SUFFIX_PATTERN.pattern(), "");
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
