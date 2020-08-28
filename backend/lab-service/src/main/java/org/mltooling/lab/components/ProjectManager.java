package org.mltooling.lab.components;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.mltooling.core.lab.model.LabProject;
import org.mltooling.core.lab.model.LabUser;
import org.mltooling.core.lab.model.LabProjectConfig;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.authorization.AuthorizationManager;
import org.mltooling.lab.authorization.ProjectAuthorizer;
import org.mltooling.lab.services.AbstractServiceManager;
import org.bson.Document;
import org.pac4j.mongo.profile.MongoProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.mltooling.lab.authorization.AuthorizationManager.USERS_COLLECTION_NAME;


public class ProjectManager {

    // ================ Constants =========================================== //
    private static final Logger log = LoggerFactory.getLogger(ProjectManager.class);

    private static final String PROJECT_NAME_VALIDATION_REGEX = "^[a-zA-Z0-9][a-zA-Z0-9\\-\\s]{1,33}[a-zA-Z0-9]$"; // min 3 chars max 35 chars
    // bucket restrictions: https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-s3-bucket-naming-requirements.html

    private final String PROJECTS_COLLECTION = "projects";

    private Set<String> BLACKLISTED_PROJECTS = new HashSet<>(Arrays.asList("local", "lokal"));


    private enum PROJECTS_COLLECTION_PROPERTIES {
        ID("id"), // lowercased name
        NAME("name"),
        CREATOR("creator"),
        VISIBILITY("visibility"),
        DESCRIPTION("description"),
        CREATED_AT("createdAt");

        public String value;

        PROJECTS_COLLECTION_PROPERTIES(String value) {
            this.value = value;
        }
    }


    // ================ Members ============================================= //
    private MongoDbManager mongoDbManager;
    private AbstractServiceManager serviceManager;

    // ================ Constructors & Main ================================= //
    public ProjectManager(MongoDbManager mongoDbManager, AbstractServiceManager serviceManager) {
        this.mongoDbManager = mongoDbManager;
        this.serviceManager = serviceManager;
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    /**
     * Create a project
     */
    public void createProject(LabProjectConfig projectConfig) throws Exception {
        if (!isValidProjectName(projectConfig.getName())) {
            throw new Exception("The project name is not valid. It may only contain letters, digits, spaces, and the minus-character. "
                                        + "The name also has to be between 3 to 35 characters.");
        }

        String projectId = processNameToId(projectConfig.getName());

        if (projectConfig.getName().startsWith(AuthorizationManager.USER_PROJECT_PREFIX)) {
            throw new Exception("Project name should not start with " + AuthorizationManager.USER_PROJECT_PREFIX);
        }

        if (projectExists(projectId)) {
            throw new Exception("Project " + projectConfig.getName() + " already exists, please try another name.");
        }

        mongoDbManager.getLabMongoDb()
                      .getCollection(PROJECTS_COLLECTION)
                      .insertOne(new Document(PROJECTS_COLLECTION_PROPERTIES.ID.value, projectId)
                                         .append(PROJECTS_COLLECTION_PROPERTIES.NAME.value, projectConfig.getName())
                                         .append(PROJECTS_COLLECTION_PROPERTIES.VISIBILITY.value, projectConfig.getVisibility().getName())
                                         .append(PROJECTS_COLLECTION_PROPERTIES.CREATOR.value, projectConfig.getCreator())
                                         .append(PROJECTS_COLLECTION_PROPERTIES.CREATED_AT.value, new Date())
                                         .append(PROJECTS_COLLECTION_PROPERTIES.DESCRIPTION.value, projectConfig.getDescription()));

        serviceManager.createProjectResources(projectId);
    }

    /**
     * Delete a project
     */
    public void deleteProject(String project) throws Exception {
        project = resolveProjectName(project);

        Document document = getProjectsCollection()
                .find(new Document(PROJECTS_COLLECTION_PROPERTIES.ID.value, project))
                .first();
        if (document != null) {
            getProjectsCollection().deleteOne(document);
            serviceManager.deleteProjectResources(project);
            // delete file storage bucket
            ComponentManager.INSTANCE.getFileManager().deleteBucket(project);
            // delete experiment collection
            ComponentManager.INSTANCE.getExperimentsManager().deleteExperimentCollection(project);
        } else {
            log.warn("Failed to delete project " + project);
        }
    }

    /**
     * Get all available projects
     */
    public List<LabProject> getProjects() {
        List<LabProject> projects = new ArrayList<>();

        FindIterable<Document> dbProjects = getProjectsCollection().find();
        for (Document projectDoc : dbProjects) {
            projects.add(transformMongoDocument(projectDoc));
        }

        return projects;
    }

    /**
     * Get one project by name
     */
    public LabProject getProject(String project) throws Exception {
        project = resolveProjectName(project);

        Document document = getProjectsCollection()
                .find(new Document(PROJECTS_COLLECTION_PROPERTIES.ID.value, project))
                .first();
        LabProject labProject = null;
        if (document != null) {
            labProject = transformMongoDocument(document);
        }

        return labProject;
    }

    /**
     * Check if project name is valid and available for project creation
     */
    public boolean isProjectAvailable(String project) throws Exception {

        if (!isValidProjectName(project)) {
            throw new Exception("The project name is not valid. It may only contain letters, digits, spaces, and the minus-character. "
                                        + "The name also has to be between 3 to 35 characters.");
        }

        project = processNameToId(project);

        if (project.startsWith(AuthorizationManager.USER_PROJECT_PREFIX)) {
            throw new Exception("The project name is not valid. " + AuthorizationManager.USER_PROJECT_PREFIX + " prefix is reserved for special user projects.");
        }

        if (BLACKLISTED_PROJECTS.contains(project)) {
            throw new Exception("Project name " + project + " is not allowed.");
        }

        if (projectExists(project)) {
            throw new Exception("Project " + project + " already exists.");
        }

        return true;
    }

    /**
     * Get all members belonging to {@code project}
     * //TODO: The implementation might be slow, due to iterating over all users, deserializing, and then returning them. This is because currently the project a user belongs to is only stored in the serialized profie
     *
     * @param project for which the members should be fetched
     * @return a list of all members. LabUser will only contain information which every user is allowed to see
     * @throws Exception
     */
    public List<String> getProjectMembers(String project) throws Exception {
        List<String> members = new ArrayList<>();
        project = resolveProjectName(project);

        FindIterable<Document> profileIterator = mongoDbManager.getLabMongoDb().getCollection(USERS_COLLECTION_NAME).find();
        for (Document document : profileIterator) {
            MongoProfile profile = AuthorizationManager.transformSerializedProfile(document);
            Set<String> profilePermissions = profile.getPermissions();
            if (ProjectAuthorizer.isProjectInPermissions(profilePermissions, project)) {
                LabUser labUser = AuthorizationManager.transformProfile(profile);
                if (!AuthorizationManager.isTechnicalUser(labUser.getName())) {
                    // only add not technical users
                    members.add(labUser.getId());
                }
            }
        }

        return members;
    }

    public String resolveProjectName(String project, boolean allowUserProjects) throws Exception {
        if (!isValidProjectName(project)) {
            throw new Exception("The project name is not valid. It may only contain letters, digits, spaces, and the minus-character.");
        }

        project = processNameToId(project);

        if (allowUserProjects && project.startsWith(AuthorizationManager.USER_PROJECT_PREFIX)) {
            // special case
            return project;
        }

        if (project.startsWith(AuthorizationManager.USER_PROJECT_PREFIX)) {
            throw new Exception(AuthorizationManager.USER_PROJECT_PREFIX + " prefix is reserved for special user projects.");
        }

        if (BLACKLISTED_PROJECTS.contains(project)) {
            throw new Exception("Project name " + project + " is not allowed.");
        }

        if (!projectExists(project)) {
            throw new Exception("Project " + project + " does not exist.");
        }

        return project;
    }

    public String resolveProjectName(String project) throws Exception {
        return resolveProjectName(project, false);
    }

    // ================ Private Methods ===================================== //
    private LabProject transformMongoDocument(Document projectDoc) {
        return new LabProject().setId(projectDoc.getString(PROJECTS_COLLECTION_PROPERTIES.ID.value))
                               .setName(projectDoc.getString(PROJECTS_COLLECTION_PROPERTIES.NAME.value))
                               .setVisibility(projectDoc.getString(PROJECTS_COLLECTION_PROPERTIES.VISIBILITY.value))
                               .setCreatedAt(projectDoc.getDate(PROJECTS_COLLECTION_PROPERTIES.CREATED_AT.value))
                               .setCreator(projectDoc.getString(PROJECTS_COLLECTION_PROPERTIES.CREATOR.value))
                               .setDescription(projectDoc.getString(PROJECTS_COLLECTION_PROPERTIES.DESCRIPTION.value));

    }

    public static String processNameToId(String projectName) {
        return projectName.replace(" ", "-").toLowerCase().trim();
    }

    private boolean isValidProjectName(String projectName) {
        return !StringUtils.isNullOrEmpty(projectName) && projectName.matches(PROJECT_NAME_VALIDATION_REGEX);
    }

    private MongoCollection<Document> getProjectsCollection() {
        return mongoDbManager.getLabMongoDb()
                             .getCollection(PROJECTS_COLLECTION);
    }

    private boolean projectExists(String project) {
        try {
            return getProjectsCollection().countDocuments(new Document(PROJECTS_COLLECTION_PROPERTIES.ID.value, project)) > 0;
        } catch (Exception e) {
            log.warn("Bad project name.", e);
            return false;
        }
    }
    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
