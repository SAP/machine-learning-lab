package org.mltooling.core.env.handler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import javax.annotation.Nullable;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.env.Environment;
import org.mltooling.core.lab.LabApiClient;
import org.mltooling.core.lab.model.LabFile;
import org.mltooling.core.lab.model.LabFileDataType;
import org.mltooling.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

public class FileHandler {
  // ================ Constants =========================================== //

  private Logger log = LoggerFactory.getLogger(FileHandler.class);

  // ================ Members ============================================= //

  private Environment env;

  // ================ Constructors & Main ================================= //

  public FileHandler(Environment env) {
    this.env = env;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  public File getFile(String fileName, LabFileDataType dataType) {
    String key = dataType.getDefaultFolder() + "/" + fileName;
    return getRemoteFile(key, null);
  }

  public File getFile(String key) {
    return getRemoteFile(key, null);
  }

  public File getFolder(String key) {
    if (StringUtils.isNullOrEmpty(key)) {
      log.warn("The key should not be null or empty!");
      return null;
    }

    Path unpackPath = env.getProjectFolder().resolve(key);
    // change folder name by removing all extensions
    unpackPath = unpackPath.resolveSibling(unpackPath.getFileName().toString().split("\\.")[0]);
    return getRemoteFile(key, unpackPath);
  }

  public String uploadFile(
      InputStream fileStream,
      String fileName,
      LabFileDataType dataType,
      @Nullable Boolean versioning,
      @Nullable Map<String, String> metadata) {
    SingleValueFormat<String> response = null;
    try {
      response =
          getLabApiClient()
              .uploadFile(
                  env.getProjectName(), fileStream, fileName, dataType, versioning, metadata);
    } catch (Exception e) {
      log.warn("Failed to upload remote file.", e);
      return null;
    }
    try {
      fileStream.close();
    } catch (IOException ignored) {
    }

    if (response != null
        && response.getStatus() == 200
        && !StringUtils.isNullOrEmpty(response.getData())) {
      log.info("File " + fileName + " uploaded successful. Key: " + response.getData());
      return response.getData();
    }
    return null;
  }

  public String uploadFile(Path filePath, LabFileDataType dataType) {
    return uploadFile(filePath, filePath.getFileName().toString(), dataType, true, null);
  }

  public String uploadFile(
      Path filePath,
      String fileName,
      LabFileDataType dataType,
      @Nullable Boolean versioning,
      @Nullable Map<String, String> metadata) {
    try {
      return uploadFile(
          new FileInputStream(filePath.toFile()), fileName, dataType, versioning, metadata);
    } catch (FileNotFoundException e) {
      log.error(String.format("Could not read file %s", filePath.toString()));
      return null;
    }
  }

  public String uploadFolder(Path folderPath, LabFileDataType dataType) {
    if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
      log.error("Directory does not exist: " + folderPath.toAbsolutePath().toString());
      return null;
    }

    Path zipPath = folderPath.resolveSibling(folderPath.getFileName() + ".zip");
    ZipUtil.pack(folderPath.toFile(), zipPath.toFile());

    return uploadFile(zipPath, zipPath.getFileName().toString(), dataType, null, null);
  }

  public List<LabFile> listRemoteFiles() {
    return listRemoteFiles(null, null);
  }

  public List<LabFile> listRemoteFiles(LabFileDataType labFileDataType) {
    return listRemoteFiles(labFileDataType, null, null);
  }

  public List<LabFile> listRemoteFiles(
      @Nullable String prefix, @Nullable Boolean aggregateVersion) {
    return listRemoteFiles(null, prefix, aggregateVersion);
  }

  public List<LabFile> listRemoteFiles(
      @Nullable LabFileDataType labFileDataType,
      @Nullable String prefix,
      @Nullable Boolean aggregateVersion) {
    try {
      return getLabApiClient()
          .getFiles(env.getProjectName(), labFileDataType, prefix, aggregateVersion)
          .getData();
    } catch (Exception e) {
      log.warn("Failed to get remote files.", e);
      return new ArrayList<>();
    }
  }

  public void deleteRemoteFile(String fileKey, @Nullable Integer keepLatestVersion) {
    try {
      getLabApiClient().deleteFile(env.getProjectName(), fileKey, keepLatestVersion);
    } catch (Exception e) {
      log.warn("Failed to delete remote file.", e);
    }
  }

  // ================ Private Methods ===================================== //

  private Path loadLocalFile(String key) {
    Path localFile = env.getProjectFolder().resolve(key);

    // TODO Always load latest version also locally?
    if (Files.exists(localFile)) {
      return localFile;
    }

    File[] fileVersions =
        localFile
            .getParent()
            .toFile()
            .listFiles(
                new FilenameFilter() {

                  public boolean accept(File dir, String name) {
                    return name.startsWith(localFile.getFileName().toString());
                  }
                });

    if (fileVersions == null || fileVersions.length == 0) {
      return localFile;
    }

    HashMap<Integer, String> versionToFile = new HashMap<>();

    for (File fileVersion : fileVersions) {
      try {
        String fileName = fileVersion.getName();
        Integer version = FileHandlerUtils.getVersionFromKey(fileName);
        versionToFile.put(version, fileName);
      } catch (Exception e1) {
        // do nothing
      }
    }

    if (versionToFile.keySet().size() == 0) {
      return localFile;
    }

    List<Integer> sortedVersions = new ArrayList<Integer>(versionToFile.keySet());
    sortedVersions.sort(Collections.reverseOrder());

    String latestFileName = versionToFile.get(sortedVersions.get(0));
    log.info("Loading latest version (" + sortedVersions.get(0) + ") for " + key + " from local.");
    return localFile.getParent().resolve(latestFileName);
  }

  private Path unpackFile(File packageFile, Path unpackPath, boolean removeIfExists) {
    if (!org.mltooling.core.utils.FileUtils.isZipFile(packageFile)) {
      log.warn(packageFile.getAbsolutePath() + " is not a zip file.");
      return packageFile.toPath();
    }

    if (Files.exists(unpackPath) && Files.isDirectory(unpackPath)) {
      log.info("Unpacked directory already exist.");
      if (!removeIfExists) {
        return unpackPath;
      }

      log.info("Removing existing unpacked dir: " + unpackPath.toAbsolutePath());
      try {
        FileUtils.deleteDirectory(unpackPath.toFile());
      } catch (IOException e) {
        log.warn("Failed to delete directory: " + unpackPath.toAbsolutePath(), e);
      }
    }
    log.info("Unpacking file to " + unpackPath.toFile());
    ZipUtil.unpack(packageFile, unpackPath.toFile());
    return unpackPath;
  }

  private Path downloadFile(String key) {
    LabFile labFile = null;
    try {
      labFile = getLabApiClient().downloadFile(env.getProjectName(), key).getData();
    } catch (Exception e) {
      log.warn("Failed to download remote file.", e);
      return null;
    }
    // if labFile.getFileStream is unequal null, the file on the remote server is newer with regards
    // to the passsed criteria
    if (labFile != null && labFile.getFileStream() != null) {
      Path localFile = env.getProjectFolder().resolve(labFile.getKey());
      localFile.toFile().getParentFile().mkdirs();

      try {

        InputStream progressInputStream =
            ProgressBar.wrap(
                labFile.getFileStream(),
                new ProgressBarBuilder()
                    .showSpeed()
                    .setUnit("MB", 1048576L)
                    .setInitialMax(labFile.getSize())
                    .setTaskName("Downloading " + key + ": ")
                    .setUpdateIntervalMillis(5000)
                    .setStyle(ProgressBarStyle.UNICODE_BLOCK)
                    .setPrintStream(System.out));

        Files.copy(
            progressInputStream, localFile.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);

        try {
          progressInputStream.close();
        } catch (Exception e) {
          // do nothing
        }

        try {
          labFile.getFileStream().close();
        } catch (Exception e) {
          // do nothing
        }

        return localFile;
      } catch (IOException e) {
        log.error("Could not write remote file to path: " + localFile.toString(), e);
      }
    }
    return null;
  }

  private File getRemoteFile(String key, @Nullable Path unpackPath) {
    Path localFile = loadLocalFile(key);

    boolean fileUpdated = false;

    long localFileSize = localFile.toFile().length();
    Integer localVersion = FileHandlerUtils.getVersionFromKey(localFile.getFileName().toString());

    LabFile labFile = null;
    try {
      labFile = getLabApiClient().getFileInfo(env.getProjectName(), key).getData();
    } catch (Exception e) {
      log.info("Cannot request file info from remote.");
    }

    if (labFile != null) {
      Integer remoteVersion = labFile.getVersion();

      long remoteFileSize = labFile.getSize();

      if (!Files.exists(localFile)) {
        log.info("File does not exist locally. Downloading from remote.");
        localFile = downloadFile(key);
        fileUpdated = true;
      } else if (remoteVersion > localVersion) {
        log.info(
            "Updating file from remote because local file has a lower version compared to remote"
                + " file.");
        localFile = downloadFile(key);
        fileUpdated = true;
      } else if (remoteVersion.equals(localVersion) && localFileSize != remoteFileSize) {
        log.info(
            "Updating file from remote because local file has another size compared to remote"
                + " file.");
        localFile = downloadFile(key);
        fileUpdated = true;
      }
    }

    if (localFile == null || !Files.exists(localFile)) {
      log.warn("File doesn't exist locally or on remote");
      return null; // TODO return null?
    }

    if (unpackPath != null) {
      // only update if it not exists or zip was updated
      return unpackFile(localFile.toFile(), unpackPath, fileUpdated).toFile();
    }

    return localFile.toFile();
  }

  private LabApiClient getLabApiClient() throws Exception {
    if (!env.isConnected()) {
      throw new Exception(
          "The environment is not connected to an Lab Instance. Is the Environment config"
              + " correct?");
    }

    return this.env.getLabApiHandler();
  }

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
