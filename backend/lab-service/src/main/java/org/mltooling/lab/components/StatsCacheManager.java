package org.mltooling.lab.components;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.mltooling.core.lab.model.*;
import org.mltooling.core.utils.PerfLogger;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.authorization.AuthorizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsCacheManager {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(EventLogManager.class);

  // ================ Members ============================================= //
  private Map<String, LabProjectsStatistics> projectsStatsCache = new HashMap<>();
  private LabProjectsStatistics labStatsCache;

  private ComponentManager componentManager;
  private long MAX_CACHE_VALIDITY = MILLISECONDS.convert(15, MINUTES);

  // ================ Constructors & Main ================================= //
  public StatsCacheManager(ComponentManager componentManager) {
    this.componentManager = componentManager;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  // get total lab statistics
  public LabProjectsStatistics getStats() {
    if (labStatsCache == null || labStatsCache.getCacheUpdateDate() == null) {
      updateCache();
      return labStatsCache;
    }

    if ((new Date().getTime() - labStatsCache.getCacheUpdateDate().getTime())
        > MAX_CACHE_VALIDITY) {
      // update if cache is older than 15 minutes
      updateCache();
      return labStatsCache;
    }

    if (labStatsCache.getCacheUpdateDuration() == null) {
      // cache updates seems to be still running
      try {
        // Sleep for 3 seconds and then return the current version of the cache
        Thread.sleep(3000);
        return labStatsCache;
      } catch (InterruptedException e) {
        return labStatsCache;
      }
    }

    if (labStatsCache.getCacheUpdateDuration() < 3000) {
      // always update if last caching took less than 3000 milliseconds
      updateCache();
      return labStatsCache;
    }

    return labStatsCache;
  }

  public LabProjectsStatistics getStats(String project) {
    LabProjectsStatistics statistics = projectsStatsCache.get(project);
    if (statistics == null || statistics.getCacheUpdateDate() == null) {
      updateCache(project);
      return projectsStatsCache.get(project);
    }

    if ((new Date().getTime() - statistics.getCacheUpdateDate().getTime()) > MAX_CACHE_VALIDITY) {
      // update if cache is older than 15 minutes
      updateCache(project);
      return projectsStatsCache.get(project);
    }

    if (statistics.getCacheUpdateDuration() == null) {
      // cache updates seems to be still running
      try {
        // Sleep for 0.5 seconds and then return the current version of the cache
        Thread.sleep(500);
        return projectsStatsCache.get(project);
      } catch (InterruptedException e) {
        return projectsStatsCache.get(project);
      }
    }

    if (statistics.getCacheUpdateDuration() < 500) {
      // always update if last caching took less than 500 milliseconds
      updateCache(project);
      return projectsStatsCache.get(project);
    }

    return statistics;
  }

  public void updateCache() {
    PerfLogger fullPerfLogger = new PerfLogger();
    fullPerfLogger.start();

    log.debug("Updating lab cache");
    labStatsCache = new LabProjectsStatistics();
    labStatsCache.setCacheUpdateDate(new Date());

    List<LabProject> projects = componentManager.getProjectManager().getProjects();

    labStatsCache.setProjectsCount(projects.size());
    labStatsCache.setSharedProjectsCount(0);
    labStatsCache.setInactiveProjectsCount(0);
    labStatsCache.setUserCount(componentManager.getAuthManager().getProfileIds().size());
    labStatsCache.setInactiveUserCount(0);
    // labStatsCache.setInactiveUserCount(componentManager.getAuthManager().getInactiveUsers(null).size());

    labStatsCache.setDownloadedFiles(
        componentManager.getEventLogManager().getEvents(LabEvent.DOWNLOADED_FILE).size());

    labStatsCache.setExperimentsCount(0);
    labStatsCache.setServicesCount(0);
    labStatsCache.setJobsCount(0);
    labStatsCache.setModelsCount(0);
    labStatsCache.setModelsTotalSize(0d);
    labStatsCache.setDatasetsCount(0);
    labStatsCache.setDatasetsTotalSize(0d);
    // labStatsCache.setFilesCount(0);
    // labStatsCache.setFilesTotalSize(0d);

    for (LabProject project : projects) {
      try {
        LabProjectsStatistics projectCache = getStats(project.getId());
        project.setMembers(componentManager.getProjectManager().getProjectMembers(project.getId()));

        if (project.getMembers() != null) {
          int realMembers = 0;
          for (String member : project.getMembers()) {
            if (!AuthorizationManager.isTechnicalUser(member)) {
              realMembers++;
            }
          }
          if (realMembers >= 2) {
            labStatsCache.setSharedProjectsCount(labStatsCache.getSharedProjectsCount() + 1);
          }
        }

        if (projectCache.getLastModified() != null) {
          // if older than 14 days = count project as inactive
          long dateThreshold = new Date().getTime() - TimeUnit.DAYS.toMillis(14);
          if (projectCache.getLastModified().getTime() < dateThreshold) {
            labStatsCache.setInactiveProjectsCount(labStatsCache.getInactiveProjectsCount() + 1);
          }
        }

        if (projectCache.getExperimentsCount() != null) {
          labStatsCache.setExperimentsCount(
              labStatsCache.getExperimentsCount() + projectCache.getExperimentsCount());
        }

        if (projectCache.getServicesCount() != null) {
          labStatsCache.setServicesCount(
              labStatsCache.getServicesCount() + projectCache.getServicesCount());
        }

        if (projectCache.getJobsCount() != null) {
          labStatsCache.setJobsCount(labStatsCache.getJobsCount() + projectCache.getJobsCount());
        }

        if (projectCache.getModelsCount() != null) {
          labStatsCache.setModelsCount(
              labStatsCache.getModelsCount() + projectCache.getModelsCount());
        }

        if (projectCache.getModelsTotalSize() != null) {
          labStatsCache.setModelsTotalSize(
              labStatsCache.getModelsTotalSize() + projectCache.getModelsTotalSize());
        }

        if (projectCache.getDatasetsCount() != null) {
          labStatsCache.setDatasetsCount(
              labStatsCache.getDatasetsCount() + projectCache.getDatasetsCount());
        }

        if (projectCache.getDatasetsTotalSize() != null) {
          labStatsCache.setDatasetsTotalSize(
              labStatsCache.getDatasetsTotalSize() + projectCache.getDatasetsTotalSize());
        }

        if (projectCache.getFilesCount() != null) {
          labStatsCache.setFilesCount(labStatsCache.getFilesCount() + projectCache.getFilesCount());
        }

        if (projectCache.getFilesTotalSize() != null) {
          labStatsCache.setFilesTotalSize(
              labStatsCache.getFilesTotalSize() + projectCache.getFilesTotalSize());
        }
      } catch (Exception ex) {
        log.warn(
            "Failed to get statistics for project: " + project + "; Exception: " + ex.getMessage());
      }
    }

    labStatsCache.setCacheUpdateDuration(fullPerfLogger.end());
  }

  public void updateCache(String project) {
    try {
      LabProject labProject = componentManager.getProjectManager().getProject(project);
      Date lastModified = labProject.getCreatedAt();

      PerfLogger fullPerfLogger = new PerfLogger();
      fullPerfLogger.start();

      log.debug("Updating project cache " + project);

      LabProjectsStatistics statistics = new LabProjectsStatistics();
      statistics.setCacheUpdateDate(new Date());

      PerfLogger perfLogger = new PerfLogger();
      perfLogger.start();
      LabFileCollection datasetsCollection =
          componentManager.getFileManager().listRemoteFiles(project, LabFileDataType.DATASET);
      statistics.setDatasetsCount(
          datasetsCollection.getLabFiles().size()); // use count of datasets instead of file count
      statistics.setDatasetsTotalSize((double) datasetsCollection.getTotalSize());

      try {
        statistics.setUserCount(
            componentManager.getProjectManager().getProjectMembers(labProject.getId()).size());
      } catch (Exception ex) {
        // do nothing
      }

      if (datasetsCollection.getLastModified() != null) {
        if (lastModified == null) {
          lastModified = datasetsCollection.getLastModified();
        } else if (lastModified.before(datasetsCollection.getLastModified())) {
          lastModified = datasetsCollection.getLastModified();
        }
      }

      log.debug("Time to load dataset stats: " + perfLogger.end());

      perfLogger.start();
      LabFileCollection modelsCollection =
          componentManager.getFileManager().listRemoteFiles(project, LabFileDataType.MODEL);
      statistics.setModelsCount(
          modelsCollection.getLabFiles().size()); // use count of models instead of file count
      statistics.setModelsTotalSize((double) modelsCollection.getTotalSize());

      if (modelsCollection.getLastModified() != null) {
        if (lastModified == null) {
          lastModified = modelsCollection.getLastModified();
        } else if (lastModified.before(modelsCollection.getLastModified())) {
          lastModified = modelsCollection.getLastModified();
        }
      }

      log.debug("Time to load model stats: " + perfLogger.end());

      try {
        // This might be a bit slow - optimize?
        perfLogger.start();
        statistics.setServicesCount(
            componentManager.getServiceManger().getServices(project).size());
        log.debug("Time to load services stats: " + perfLogger.end());
        perfLogger.start();
        statistics.setJobsCount(componentManager.getServiceManger().getJobs(project).size());
        log.debug("Time to load jobs stats: " + perfLogger.end());

      } catch (Exception e) {
        // DO nothing
        log.warn("Failed to load statistics for docker.", e);
      }

      perfLogger.start();

      List<LabExperiment> experiments =
          componentManager.getExperimentsManager().getExperiments(project);
      statistics.setExperimentsCount(experiments.size());

      Map<String, Object> experimentStats = ExperimentsManager.getExperimentsStats(experiments);
      if (experimentStats != null) {
        Date lastRun = (Date) experimentStats.get(ExperimentsManager.STATS_LAST_RUN);
        if (lastRun != null) {
          if (lastModified == null) {
            lastModified = lastRun;
          } else if (lastModified.before(lastRun)) {
            lastModified = lastRun;
          }
        }
      }

      log.debug("Time to load experiment stats: " + perfLogger.end());

      statistics.setLastModified(lastModified);

      statistics.setCacheUpdateDuration(fullPerfLogger.end());

      projectsStatsCache.put(project, statistics);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
