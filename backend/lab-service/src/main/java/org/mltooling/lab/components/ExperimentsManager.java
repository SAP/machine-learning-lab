package org.mltooling.lab.components;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import java.util.*;
import org.bson.Document;
import org.mltooling.core.env.Environment;
import org.mltooling.core.lab.model.LabExperiment;
import org.mltooling.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExperimentsManager {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(ExperimentsManager.class);

  public static final String EXPERIMENTS_COLLECTION_PREFIX = "experiments-";

  private static final String KEY_PROPERTY = "key";

  public static final String STATS_COMPLETED = "completed";
  public static final String STATS_RUNNING = "running";
  public static final String STATS_FAILED = "failed";
  public static final String STATS_LAST_RUN = "lastRun";

  // ================ Members ============================================= //
  public Environment env;

  private MongoDbManager mongoDbManager;

  // ================ Constructors & Main ================================= //
  public ExperimentsManager(MongoDbManager mongoDbManager) {
    this.mongoDbManager = mongoDbManager;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  public LabExperiment updateExperiment(String project, LabExperiment experiment) throws Exception {
    if (StringUtils.isNullOrEmpty(experiment.getKey())) {
      throw new Exception("Failed to sync experiment. The experiment key is not provided.");
    }

    MongoCollection collection = getExpCollection(project);

    Document keyFilter = new Document(KEY_PROPERTY, experiment.getKey());

    if (experimentExists(project, experiment.getKey())) {
      collection.updateOne(keyFilter, new Document("$set", transformLabExperiment(experiment)));
    } else {
      collection.insertOne(transformLabExperiment(experiment));
    }
    return experiment;
  }

  public List<LabExperiment> getExperiments(String project) {
    if (getExperimentCount(project) == 0) {
      return new ArrayList<>();
    }

    List<LabExperiment> experiments = new ArrayList<>();
    FindIterable<Document> experimentsIter = getExpCollection(project).find();
    for (Document doc : experimentsIter) {
      try {
        experiments.add(transformMongoDocument(doc));
      } catch (Exception e) {
        log.info("Failed to transform experiment", e);
      }
    }

    return experiments;
  }

  public static Map<String, Object> getExperimentsStats(List<LabExperiment> experiments) {
    Integer running = 0;
    Integer completed = 0;
    Integer failed = 0;

    Date lastRun = null;
    for (LabExperiment experiment : experiments) {

      if (lastRun == null) {
        lastRun = experiment.getStartedAt();
      }

      if (experiment.getStartedAt() != null) {
        if (lastRun.before(experiment.getStartedAt())) {
          lastRun = experiment.getStartedAt();
        }
      }

      if (experiment.getUpdatedAt() != null) {
        if (lastRun.before(experiment.getUpdatedAt())) {
          lastRun = experiment.getUpdatedAt();
        }
      }

      if (experiment.getFinishedAt() != null) {
        if (lastRun.before(experiment.getFinishedAt())) {
          lastRun = experiment.getFinishedAt();
        }
      }

      // set experiment to dead if it wasn't update in the last 30 minutes
      long MAX_TIME_WITHOUT_UPDATE = MILLISECONDS.convert(30, MINUTES);
      if (experiment.getStatus() == LabExperiment.State.RUNNING
          && experiment.getUpdatedAt() != null
          && (new Date().getTime() - experiment.getUpdatedAt().getTime())
              >= MAX_TIME_WITHOUT_UPDATE) {
        experiment.setStatus(LabExperiment.State.DEAD);
      }

      if (experiment.getStatus() == LabExperiment.State.RUNNING) {
        running++;
      } else if (experiment.getStatus() == LabExperiment.State.FAILED
          || experiment.getStatus() == LabExperiment.State.DEAD
          || experiment.getStatus() == LabExperiment.State.INTERRUPTED) {
        failed++;
      } else if (experiment.getStatus() == LabExperiment.State.COMPLETED) {
        completed++;
      }
    }

    HashMap<String, Object> statistics = new HashMap<>();
    statistics.put(STATS_COMPLETED, completed);
    statistics.put(STATS_RUNNING, running);
    statistics.put(STATS_FAILED, failed);
    statistics.put(STATS_LAST_RUN, lastRun);

    return statistics;
  }

  /** Use with care, cannot be undone. */
  protected void deleteExperimentCollection(String project) {
    getExpCollection(project).drop();
  }

  public LabExperiment getExperiment(String project, String experimentKey) {
    Document document =
        getExpCollection(project).find(new Document(KEY_PROPERTY, experimentKey)).first();
    LabExperiment experiment = null;
    if (document != null) {
      experiment = transformMongoDocument(document);
    }

    return experiment;
  }

  public boolean experimentExists(String project, String experiment) {
    try {
      return getExpCollection(project).countDocuments(new Document("key", experiment)) > 0;
    } catch (Exception e) {
      log.warn("Bad experiment name.", e);
      return false;
    }
  }

  public int getExperimentCount(String project) {
    try {
      return (int) getExpCollection(project).countDocuments();
    } catch (Exception e) {
      log.warn("Failed to count experiments.", e);
      return 0;
    }
  }

  /** Delete an experiment */
  public void deleteExperiment(String project, String experimentKey) {
    Document document =
        getExpCollection(project).find(new Document(KEY_PROPERTY, experimentKey)).first();

    if (document != null) {
      getExpCollection(project).deleteOne(document);
    } else {
      log.warn("Failed to delete experiment " + project);
    }
  }

  // ================ Private Methods ===================================== //
  private LabExperiment transformMongoDocument(Document doc) {
    return new Gson().fromJson(doc.toJson(), LabExperiment.class);
  }

  private Document transformLabExperiment(LabExperiment experiment) {
    return Document.parse(new Gson().toJson(experiment, LabExperiment.class));
  }

  private MongoCollection<Document> getExpCollection(String project) {
    return mongoDbManager.getLabMongoDb().getCollection(EXPERIMENTS_COLLECTION_PREFIX + project);
  }

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
