package org.mltooling.lab.components;

import com.google.common.collect.Lists;
import org.mltooling.core.env.handler.FileHandlerUtils;
import org.mltooling.core.lab.LabApi;
import org.mltooling.core.lab.model.LabFile;
import org.mltooling.core.lab.model.LabFileCollection;
import org.mltooling.core.lab.model.LabFileDataType;
import org.mltooling.core.utils.ReflectionUtils;
import org.mltooling.core.utils.StringUtils;
import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;


/**
 * Concept:
 * - fileName: corresponds to the file name only, without directory information fooFile.txt
 * - fileKey | key: corresponds to the full qualified name, fooDirectory/fooFile.txt
 */
public class FileStorageManager {

    // ================ Constants =========================================== //
    private static final Logger log = LoggerFactory.getLogger(FileStorageManager.class);

    private static final String CUSTOM_METADATA_PREFIX = "x-amz-meta-";
    private static final String UNKNOWN_PARAMETER = "-";

    private static final boolean DEFAULT_VERSIONING_ACTIVATED = true;
    private static final boolean VERSION_FIRST_FILE = true;

    // ================ Members ============================================= //

    private MinioClient minioClient;

    // ================ Constructors & Main ================================= //

    public FileStorageManager(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public FileStorageManager(String s3Endpoint, String s3AccessKey, String s3SecretKey, boolean s3Secured) {
        try {
            log.info("Init Minio client with endpoint: " + s3Endpoint + " | accessKey: "
                             + s3AccessKey + " | secretKey: " + s3SecretKey + " | secured: " + s3Secured);
            minioClient = new MinioClient(s3Endpoint,
                                          s3AccessKey,
                                          s3SecretKey,
                                          s3Secured);
            minioClient.ignoreCertCheck();
        } catch (InvalidEndpointException | InvalidPortException | NoSuchAlgorithmException | KeyManagementException e) {
            log.warn("Failed to load minio client, config is missing", e);
        }
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //
    public LabFile getFile(String fileName, LabFileDataType dataType, String bucket) {
        String key = FileHandlerUtils.resolveKey(fileName, dataType);

        if (key == null) {
            log.warn("Failed to resolve key.");
            return null;
        }
        return getFile(key, bucket);
    }

    /**
     * Get file from S3 Storage with given key.
     *
     * @param fileKey key of the file to download (can contain the file version as given by {@link FileHandlerUtils#FILE_VERSION_SUFFIX_PATTERN})
     * @param bucket  the bucket where the fileKey can be found
     * @return the remote file or null if an error occurred
     */
    public LabFile getFile(String fileKey,
                           String bucket) {
        if (!isMinioAvailable()) {
            log.warn("Minio client not available. Cannot load remote files.");
            return null;
        }
        try {
            checkAndCreateBucket(bucket);
            String remoteFileKey = resolveLatestVersionOnRemoteStorage(fileKey, bucket);
            LabFile labFile = getFileInfo(remoteFileKey, bucket);
            InputStream remoteFileInputStream = getFileInputStream(bucket, remoteFileKey);
            labFile.setFileStream(remoteFileInputStream);
            return labFile;
        } catch (NoSuchElementException ex) {
            log.info("Cannot find file on minio for " + fileKey);
            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public LabFile getFileInfo(String fileKey, String bucket) throws Exception {
        try {
            LabFileCollection fileCollection = listRemoteFiles(bucket, fileKey, true);

            if (fileCollection.getLabFiles().size() == 0) {
                throw new NoSuchElementException("File " + fileKey + " could not be found.");
            }

            if (fileCollection.getLabFiles().size() > 1) {
                throw new Exception("Multiple files found for " + fileKey + " as file prefix.");
            }

            return fileCollection.getLabFiles().get(0);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw e;
        }
    }

    public String uploadFile(String fileName, InputStream fileStream, LabFileDataType dataType, String bucket, @Nullable Boolean versioning, @Nullable Map<String, String> metadata) {
        if (dataType == null || dataType.isUnknown()) {
            log.warn("Not able to upload file. Unknown remoteFileType. Available options: " + LabFileDataType.ALLOWABLE_VALUES);
            return null;
        }

        String key = FileHandlerUtils.resolveKey(fileName, dataType);

        if (key == null) {
            log.warn("Failed to resolve key.");
            return null;
        }

        return uploadFile(key, fileStream, bucket, versioning, metadata);
    }

    public String uploadFile(String key, InputStream fileStream, String bucket, @Nullable Boolean versioning, @Nullable Map<String, String> metadata) {
        if (versioning == null) {
            versioning = DEFAULT_VERSIONING_ACTIVATED;
        }

        if (!isMinioAvailable()) {
            log.warn("Minio client not available. Cannot upload file to remote.");
            return null;
        }

        try {
            checkAndCreateBucket(bucket);

            if (versioning && !FileHandlerUtils.FILE_VERSION_SUFFIX_PATTERN.matcher(key).find()) {
                // versioning is activated and key does not contain version number

                //get latest version of version files
                List<LabFile> aggregatedFiles = listRemoteFiles(bucket, key, true).getLabFiles();
                HashMap<String, LabFile> filesByName = new HashMap<>();

                for (LabFile labFile : aggregatedFiles) {
                    filesByName.put(labFile.getName(), labFile);
                }

                LabFile matchingFile = filesByName.get(FileHandlerUtils.getFileNameFromKey(key, true));

                if (matchingFile != null) {
                    // a file for the given key exists
                    Integer currentVersion = matchingFile.getVersion();
                    if (currentVersion == null) {
                        currentVersion = 1;
                    }

                    try {
                        //checking if file with exact key exists on minio
                        getMinioClient().statObject(bucket, key);

                        try {
                            //special case -> check if version v1 also exists
                            getMinioClient().statObject(bucket, key + FileHandlerUtils.FILE_VERSION_SUFFIX + 1);
                        } catch (ErrorResponseException e) {
                            if (e.errorResponse().code().equalsIgnoreCase("NoSuchKey")) {
                                // if not, then the file with the specified key is the only existing file so it should be version 1 instead of 2
                                currentVersion = 0;
                            }
                        }

                        //version object with matching name to current version + 1
                        String renameKey = key + FileHandlerUtils.FILE_VERSION_SUFFIX + String.valueOf(currentVersion + 1);
                        log.debug("Renaming " + key + " to " + renameKey);
                        getMinioClient().copyObject(bucket, key, bucket, renameKey);
                        getMinioClient().removeObject(bucket, key);

                        //version new object to current version + 2
                        key = key + FileHandlerUtils.FILE_VERSION_SUFFIX + String.valueOf(currentVersion + 2);

                    } catch (ErrorResponseException e) {
                        if (e.errorResponse().code().equalsIgnoreCase("NoSuchKey")) {
                            // only versioned files uploaded -> changing key to version + 1
                            key = key + FileHandlerUtils.FILE_VERSION_SUFFIX + String.valueOf(currentVersion + 1);
                        }
                    }
                } else if (VERSION_FIRST_FILE) {
                    // also version first file
                    key = key + FileHandlerUtils.FILE_VERSION_SUFFIX + String.valueOf(1);
                }
            }

            log.info("Uploading file to remote with key " + key + " to bucket " + bucket);

            // prepared headers for metadata
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("Content-Type", "application/octet-stream");

            // Add metadata as headers, to be valid, metadata needs to have the x-amz-meta prefix (S3 conform)
            if (metadata != null) {
                for (String field : metadata.keySet()) {
                    String value = metadata.get(field);
                    if (!field.startsWith(LabApi.FILE_METADATA_PREFIX)) {
                        field = LabApi.FILE_METADATA_PREFIX + field;
                    }
                    headerMap.put(field, value);
                }

                String contentType = metadata.get(LabApi.FILE_METADATA_PREFIX + LabFile.META_CONTENT_TYPE);
                if (!StringUtils.isNullOrEmpty(contentType)) {
                    // if available, use content type from metadata
                    headerMap.put("Content-Type", contentType);
                }
            }

            // call private method with reflection utils (otherwise not possible):
            // TODO change if fix is provided: https://github.com/minio/minio-java/issues/662
            // old upload method: getMinioClient().putObject(bucket, key, fileStream, "application/octet-stream");
            ReflectionUtils.invokeMethod(getMinioClient(), "putObject", bucket, key, (Long) null, new BufferedInputStream(fileStream), headerMap);

            log.info("Successfully uploaded: " + key);
            fileStream.close();
            return key;
        } catch (Exception e) {
            try {
                fileStream.close();
            } catch (IOException ignored) {
            }
            log.warn("Failed to upload file with key " + key + " to bucket " + bucket, e);
            return null;
        }
    }

    public LabFileCollection listRemoteFiles(String bucket) {
        return listRemoteFiles(bucket, "", null);
    }

    public LabFileCollection listRemoteFiles(String bucket, LabFileDataType dataType) {
        return listRemoteFiles(bucket, dataType, null, null);
    }

    public LabFileCollection listRemoteFiles(String bucket, @Nullable LabFileDataType dataType, @Nullable String prefix, @Nullable Boolean aggregateVersions) {
        String combinedPrefix = "";
        if (dataType != null && !dataType.isUnknown()) {
            combinedPrefix += dataType.getDefaultFolder();
        }
        if (!StringUtils.isNullOrEmpty(prefix)) {
            combinedPrefix += prefix;
        }

        return listRemoteFiles(bucket, combinedPrefix, aggregateVersions);
    }

    public LabFileCollection listRemoteFiles(String bucket, @Nullable String prefix, @Nullable Boolean aggregateVersions) {
        if (aggregateVersions == null) {
            aggregateVersions = true;
        }

        if (prefix == null) {
            prefix = "";
        }

        if (!isMinioAvailable()) {
            log.warn("Failed to list remote files in " + bucket + " for " + prefix + ". Minio not available.");
            return new LabFileCollection();
        }

        try {
            checkAndCreateBucket(bucket);

            Iterable<Result<Item>> matchingFiles = getMinioClient().listObjects(bucket, prefix, true);

            if (matchingFiles == null) {
                return new LabFileCollection();
            }

            HashMap<String, LabFile> remoteFiles = new HashMap<>();

            LabFileCollection labFileCollection = new LabFileCollection();

            for (Result<Item> matchingFile : matchingFiles) {
                Item fileObject = matchingFile.get();
                String key = fileObject.objectName();
                ObjectStat fileStats = getMinioClient().statObject(bucket, key);

                HashMap<String, String> customMetadata = new HashMap<>();
                for (String metadataKey : fileStats.httpHeaders().keySet()) {
                    if (metadataKey.toLowerCase().startsWith(CUSTOM_METADATA_PREFIX) && fileStats.httpHeaders().get(metadataKey).size() > 0) {
                        // clean metadata key and take the first value in the metadata map
                        customMetadata.put(metadataKey.toLowerCase().replace(CUSTOM_METADATA_PREFIX, ""), fileStats.httpHeaders().get(metadataKey).get(0));
                    }
                }

                Integer version = FileHandlerUtils.getVersionFromKey(matchingFile.get().objectName());

                LabFile labFile = new LabFile()
                        .setKey(key)
                        .setName(FileHandlerUtils.getFileNameFromKey(key, aggregateVersions)) // filname with version if not aggregated
                        .setModifiedAt(fileObject.lastModified())
                        .setSize(fileObject.objectSize())
                        .setHash(fileStats.etag())
                        .setVersion(version)
                        .setContentType(fileStats.contentType())
                        .setDataType(LabFileDataType.fromKey(key))
                        .setMetadata(customMetadata);

                // Use fileStats.createdTime() ?

                // set modifier if available in custom metadata
                labFile.setModifiedBy(customMetadata.getOrDefault(LabFile.META_MODIFIED_BY, UNKNOWN_PARAMETER));

                // calculate stats
                labFileCollection.add(labFile);

                remoteFiles.put(key, labFile);
            }

            if (!aggregateVersions) {
                return labFileCollection;
            }

            // merge multiple versions of the same file by keeping only the file with the highest version
            HashMap<String, LabFile> aggregatedFiles = new HashMap<>();
            for (LabFile labFile : remoteFiles.values()) {
                if (!aggregatedFiles.containsKey(labFile.getName())) {
                    aggregatedFiles.put(labFile.getName(), labFile);
                } else if (labFile.getVersion() > aggregatedFiles.get(labFile.getName()).getVersion()) {
                    aggregatedFiles.put(labFile.getName(), labFile);
                }
            }
            return labFileCollection.setLabFiles(new ArrayList<>(aggregatedFiles.values())).setAggregatedVersions(true);
        } catch (Exception e) {
            log.warn("Exception for listing remote files in " + bucket + " for " + prefix + ".", e);
            return new LabFileCollection();
        }
    }

    public void deleteFolder(String bucket, String path) {
        LabFileCollection remoteFiles = listRemoteFiles(bucket, path, false);
        if (remoteFiles != null) {
            for (LabFile file : remoteFiles.getLabFiles()) {
                deleteFile(file.getKey());
            }
        }

    }

    public void deleteFile(String key) {
        this.deleteFile(key, null, null);
    }

    /**
     * Delete all versions, but keep the latest n if keepLatestVersions is provided
     */
    public void deleteFile(String key, String bucket, @Nullable Integer keepLatestVersions) {
        if (keepLatestVersions == null) {
            keepLatestVersions = 0;
        }

        HashSet<Integer> keepVersions = new HashSet<>();
        LabFileCollection fileCollection = listRemoteFiles(bucket, key, false);

        if (keepLatestVersions > 0) {
            List<Integer> sortedVersions = new ArrayList<>();
            for (LabFile labFile : fileCollection.getLabFiles()) {
                sortedVersions.add(labFile.getVersion());
            }
            sortedVersions.sort(Collections.reverseOrder());

            for (Integer version : sortedVersions) {
                if (keepVersions.size() < keepLatestVersions) {
                    // start from the latest version and add to the keep list
                    keepVersions.add(version);
                }
            }
        }

        for (LabFile labFile : fileCollection.getLabFiles()) {
            if (keepVersions.contains(labFile.getVersion())) {
                continue;
            }

            try {
                getMinioClient().removeObject(bucket, labFile.getKey());
            } catch (Exception e) {
                log.warn("Exception for removing remote file " + key + " in " + bucket, e);
            }
        }
    }

    /**
     * Use with care, this can not be undone.
     */
    public void deleteBucket(String bucket) {
        try {
            List<Result<Item>> files = Lists.newArrayList(getMinioClient().listObjects(bucket));
            log.info("Deleting all files (" + files.size() + ") of bucket " + bucket);

            for (Result<Item> file : files) {
                getMinioClient().removeObject(bucket, file.get().objectName());
            }

            log.warn("Deleting bucket " + bucket);
            getMinioClient().removeBucket(bucket);
        } catch (Exception e) {
            log.warn("Cannot delete bucket " + bucket, e);
        }
    }

    public MinioClient getMinioClient() {
        return minioClient;
    }

    public boolean isMinioAvailable() {
        return minioClient != null; //TODO: implement Minio connection check?
    }

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //
    private InputStream getFileInputStream(String bucket, String key) {
        try {
            return getMinioClient().getObject(bucket, key);
        } catch (Exception e) {
            log.error(String.format("Could not get stream for bucket '%s' and key '%s'", bucket, key));
        }

        return null;
    }

    /**
     * Return the latest file version on remote storage if {@code key} does not contain the version.
     *
     * @param key    name of the file, either with or without name
     * @param bucket where to look for the key
     * @return the key containing the latest version
     */
    private String resolveLatestVersionOnRemoteStorage(String key, String bucket) {
        if (FileHandlerUtils.FILE_VERSION_SUFFIX_PATTERN.matcher(key).find()) {
            // key contains version suffix, no need to resolve to latest version
            return key;
        }

        try {
            HashMap<Integer, String> versionToFile = new HashMap<>();
            Iterable<Result<Item>> fileVersions = getMinioClient().listObjects(bucket, key + FileHandlerUtils.FILE_VERSION_SUFFIX);
            for (Result<Item> fileVersion : fileVersions) {
                try {
                    String fileKey = fileVersion.get().objectName();
                    Integer version = FileHandlerUtils.getVersionFromKey(fileKey);
                    versionToFile.put(version, fileKey);

                } catch (Exception ignored) {
                }
            }

            if (versionToFile.keySet().size() == 0) {
                // return null
                return key;
            }

            List<Integer> sortedVersions = new ArrayList<>(versionToFile.keySet());
            sortedVersions.sort(Collections.reverseOrder());

            String latestFileKey = versionToFile.get(sortedVersions.get(0));
            log.info("Latest version available on remote for " + key + " is " + sortedVersions.get(0));
            return latestFileKey;

        } catch (XmlPullParserException e1) {
            log.info("Failed to request file versions", e1);
            return key;
        }
    }

    private void checkAndCreateBucket(String bucket) throws Exception {
        if (!getMinioClient().bucketExists(bucket)) {
            log.debug("Bucket " + bucket + " does not exist. Creating missing bucket.");
            getMinioClient().makeBucket(bucket);
        }
    }

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
