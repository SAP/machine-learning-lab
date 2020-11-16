package org.mltooling.lab.components;

import static com.cronutils.model.CronType.UNIX;

import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.mongodb.client.result.DeleteResult;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.mltooling.core.lab.LabApi;
import org.mltooling.core.lab.model.LabScheduledJob;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.services.AbstractServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobManager {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(JobManager.class);

  public static final String JOB_ID_FIELD = "_id";
  public static final String JOB_ADDED_AT_MS_FIELD = "addedAt";
  public static final String JOB_LAST_EXECUTION_MS_FIELD = "lastExecutionInMs";
  public static final long CHECK_JOBS_INTERVAL_IN_MS = 60 * 1000; // 1 minute

  public static final String JOBS_COLLECTION = "jobs";

  private static CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(UNIX);

  // ================ Members ============================================= //
  private CronParser parser = new CronParser(cronDefinition);
  private MongoDbManager mongoDbManager;

  // ================ Constructors & Main ================================= //
  public JobManager(MongoDbManager mongoDbManager) {
    this.mongoDbManager = mongoDbManager;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  public void checkJobs() {
    Iterable<Document> jobs = mongoDbManager.getLabMongoDb().getCollection(JOBS_COLLECTION).find();

    for (Document job : jobs) {
      JobDocument jobDocument = JobDocument.fromMongoDocument(job);
      final String schedule = jobDocument.getSchedule();
      if (StringUtils.isNullOrEmpty(jobDocument.getSchedule())
          || jobDocument.getLastExecutionInMs() == null) {
        continue;
      }

      final Object jobId = job.get(JOB_ID_FIELD);
      ExecutionTime executionTime;
      try {
        executionTime = ExecutionTime.forCron(parser.parse(schedule));
      } catch (IllegalArgumentException e) {
        log.error(
            String.format("Error with cron definition (%s) of job with _id %s", schedule, jobId),
            e.getMessage());
        continue;
      }

      ZonedDateTime zonedDateTime =
          ZonedDateTime.ofInstant(
              new DateTime(jobDocument.getLastExecutionInMs()).toDate().toInstant(),
              ZoneOffset.systemDefault());
      Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(zonedDateTime);
      if (!nextExecution.isPresent()) {
        continue;
      }

      if (Duration.between(ZonedDateTime.now(), nextExecution.get()).isNegative()) {
        log.info(String.format("Starting scheduled job %s", jobId.toString()));

        // run job, as nextExecutionTime is due
        try {
          AbstractServiceManager serviceManager = ComponentManager.INSTANCE.getServiceManger();
          serviceManager.deployJob(
              serviceManager
                  .createProjectJob(
                      jobDocument.getProject(), jobDocument.getImage(), jobDocument.getName())
                  .addEnvVariables(jobDocument.getConfig()));

          jobDocument.setLastExecutionInMs(new Date().getTime());
          this.mongoDbManager
              .getLabMongoDb()
              .getCollection(JOBS_COLLECTION)
              .updateOne(
                  new Document(JOB_ID_FIELD, jobId),
                  new Document("$set", jobDocument.toMongoDocument()));
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  public List<LabScheduledJob> getScheduledJobs(String project) {
    List<LabScheduledJob> scheduledJobs = new ArrayList<>();

    for (Document doc : mongoDbManager.getLabMongoDb().getCollection(JOBS_COLLECTION).find()) {
      JobDocument jobDocument = JobDocument.fromMongoDocument(doc);
      if (jobDocument.getProject().equalsIgnoreCase(project)) {
        // only add if it is within the project
        scheduledJobs.add(transformScheduledJob(jobDocument));
      }
    }

    return scheduledJobs;
  }

  public void addScheduledJob(
      String project,
      String image,
      String schedule,
      @Nullable String name,
      @Nullable Map<String, String> config)
      throws Exception {

    JobDocument jobDocument =
        new JobDocument(project, image)
            .setConfig(config)
            .setSchedule(schedule)
            .setName(name)
            .setLastExecutionInMs(new Date().getTime());

    try {
      ExecutionTime.forCron(parser.parse(jobDocument.getSchedule()));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          String.format(
              "Error with cron definition (%s) of job with image %s",
              jobDocument.getSchedule(), jobDocument.getImage()));
    }
    log.info("insert job into Mongo");
    this.mongoDbManager
        .getLabMongoDb()
        .getCollection(JOBS_COLLECTION)
        .insertOne(jobDocument.toMongoDocument());
  }

  public boolean deleteScheduledJob(String project, String jobId) throws Exception {
    LabScheduledJob jobToDelete = getJob(jobId, project);
    if (jobToDelete == null) {
      throw new Exception("Failed to find job " + jobId + " in project " + project);
    }

    DeleteResult result =
        mongoDbManager
            .getLabMongoDb()
            .getCollection(JOBS_COLLECTION)
            .deleteOne(new Document(JOB_ID_FIELD, new ObjectId(jobId)));
    return result.wasAcknowledged();
  }

  public LabScheduledJob getJob(String jobId, @Nullable String project) {
    Document document =
        mongoDbManager
            .getLabMongoDb()
            .getCollection(JOBS_COLLECTION)
            .find(new Document(new Document(JOB_ID_FIELD, new ObjectId(jobId))))
            .first();

    if (document == null) {
      return null;
    }

    JobDocument jobDocument = JobDocument.fromMongoDocument(document);

    if (!StringUtils.isNullOrEmpty(project)
        && !jobDocument.getProject().equalsIgnoreCase(project)) {
      log.warn("Scheduled job " + jobId + " is not part of the project " + project);
      return null;
    }

    return transformScheduledJob(jobDocument);
  }

  public void startJobScheduleMonitor() {
    ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    exec.scheduleAtFixedRate(
        this::checkJobs,
        JobManager.CHECK_JOBS_INTERVAL_IN_MS,
        JobManager.CHECK_JOBS_INTERVAL_IN_MS,
        TimeUnit.MILLISECONDS);
  }

  // ================ Private Methods ===================================== //
  private LabScheduledJob transformScheduledJob(JobDocument jobDocument) {
    LabScheduledJob scheduledJob =
        new LabScheduledJob()
            .setJobName(jobDocument.getName())
            .setAddedAt(new Date(jobDocument.getAddedAt()))
            .setConfiguration(jobDocument.getConfig())
            .setSchedule(jobDocument.getSchedule())
            .setDockerImage(jobDocument.getImage())
            .setId(jobDocument.getId())
            .setLastExecution(
                new Date(jobDocument.getLastExecutionInMs())); // TODO check if correct

    return scheduledJob;
  }

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //

  public static class JobDocument {

    private String _id; // autogenerated by Mongo
    private String project;
    private String image;
    private String name;
    private Map<String, String> config;
    private String schedule;
    private Long addedAt;
    private Long lastExecutionInMs;

    public JobDocument(String project, String image) {
      this.project = project;
      this.image = image;
      this.addedAt = new Date().getTime();
    }

    public String getSchedule() {
      return schedule;
    }

    public JobDocument setSchedule(String schedule) {
      this.schedule = schedule;
      return this;
    }

    public Map<String, String> getConfig() {
      return config;
    }

    public JobDocument setConfig(Map<String, String> config) {
      this.config = config;
      return this;
    }

    public String getProject() {
      return project;
    }

    public JobDocument setProject(String project) {
      this.project = project;
      return this;
    }

    public String getImage() {
      return image;
    }

    public JobDocument setImage(String image) {
      this.image = image;
      return this;
    }

    public Long getLastExecutionInMs() {
      return lastExecutionInMs;
    }

    public JobDocument setLastExecutionInMs(Long lastExecutionInMs) {
      this.lastExecutionInMs = lastExecutionInMs;
      return this;
    }

    public Long getAddedAt() {
      return addedAt;
    }

    public JobDocument setAddedAt(Long addedAt) {
      this.addedAt = addedAt;
      return this;
    }

    /** _id field must be present in JSON of getScheduledJobs endpoint */
    @SuppressWarnings("unused")
    private String getId() {
      return _id;
    }

    private JobDocument setId(String _id) {
      this._id = _id;
      return this;
    }

    @SuppressWarnings("unchecked")
    public static JobDocument fromMongoDocument(Document document) {
      return new JobDocument(
              document.getString(LabApi.PARAM_PROJECT),
              document.getString(LabApi.PARAM_DOCKER_IMAGE))
          .setId(document.get(JOB_ID_FIELD).toString())
          .setName(document.getString(LabApi.PARAM_NAME))
          .setConfig(document.get(LabApi.PARAM_CONFIG, Map.class))
          .setSchedule(document.getString(LabApi.PARAM_SCHEDULE))
          .setLastExecutionInMs(document.getLong(JOB_LAST_EXECUTION_MS_FIELD))
          .setAddedAt(document.getLong(JOB_ADDED_AT_MS_FIELD));
    }

    public Document toMongoDocument() {
      return new Document()
          .append(LabApi.PARAM_PROJECT, project)
          .append(LabApi.PARAM_NAME, name)
          .append(LabApi.PARAM_DOCKER_IMAGE, image)
          .append(LabApi.PARAM_SCHEDULE, schedule)
          .append(LabApi.PARAM_CONFIG, config)
          .append(JOB_LAST_EXECUTION_MS_FIELD, lastExecutionInMs)
          .append(JOB_ADDED_AT_MS_FIELD, addedAt);
    }

    public String getName() {
      return this.name;
    }

    public JobDocument setName(String name) {
      this.name = name;
      return this;
    }
  }
}
