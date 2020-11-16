package org.mltooling.lab.authorization;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authz.UnauthorizedException;
import org.bson.Document;
import org.glassfish.jersey.server.ResourceConfig;
import org.mltooling.core.api.utils.ApiUtils;
import org.mltooling.core.lab.LabAuthApi;
import org.mltooling.core.lab.model.LabFile;
import org.mltooling.core.lab.model.LabProject;
import org.mltooling.core.lab.model.LabUser;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.LabConfig;
import org.mltooling.lab.api.LabAuthApiHandler;
import org.mltooling.lab.components.MongoDbManager;
import org.pac4j.core.config.Config;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.credentials.authenticator.LocalCachingAuthenticator;
import org.pac4j.core.credentials.password.ShiroPasswordEncoder;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.JavaSerializationHelper;
import org.pac4j.http.client.direct.CookieClient;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.jax.rs.features.JaxRsConfigProvider;
import org.pac4j.jax.rs.features.JaxRsContextFactoryProvider;
import org.pac4j.jax.rs.features.Pac4JSecurityFeature;
import org.pac4j.jax.rs.jersey.features.Pac4JValueFactoryProvider;
import org.pac4j.jax.rs.pac4j.JaxRsAjaxRequestResolver;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.jwt.profile.JwtGenerator;
import org.pac4j.mongo.profile.MongoProfile;
import org.pac4j.mongo.profile.service.MongoProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains all methods relevant to handle the authorization profiles and the authorization tokens.
 * Should not contain any deeper app-logic.
 *
 * <p>user = user-id or user-name , userName = user-name only, userId = user-id only
 */
public class AuthorizationManager {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(AuthorizationManager.class);

  public static final String DEFAULT_JWT_SECRET = "please-change-please-change-please-change";

  public static final String COOKIE_ACCESS_TOKEN = "lab_access_token";
  public static final String USER_NAME_ATTRIBUTE = "username";
  public static final String REFRESH_TOKEN_ATTRIBUTE = "refreshToken";

  public static final int JWT_EXPIRATION_TIME_DAY_IN_SECONDS = 24 * 60 * 60;
  public static final int JWT_EXPIRATION_TIME_WEEK_IN_SECONDS =
      JWT_EXPIRATION_TIME_DAY_IN_SECONDS * 7;
  public static final int JWT_API_TOKEN_EXPIRATION_TIME_YEAR_IN_SECONDS =
      JWT_EXPIRATION_TIME_WEEK_IN_SECONDS * 500; // 500 weeks -> 10 years

  public static final String AUTHORIZER_ADMIN = "ADMIN";
  public static final String AUTHORIZER_PROJECT = "PROJECT";
  public static final String AUTHORIZER_USER = "USER";
  public static final String AUTHORIZER_IS_AUTHENTICATED = "customIsAuthenticated";

  public static final String PAC4J_CLIENT_DIRECT_BASIC_AUTH = "DirectBasicAuthClient";
  public static final String PAC4J_CLIENT_COOKIE = "CookieClient";
  public static final String PAC4J_CLIENT_HEADER = "HeaderClient";

  @Deprecated
  private static final String PROJECT_USER_PREFIX_COMPAT =
      "project-admin-"; // only for compatibility reasons

  private static final String PROJECT_USER_PREFIX = "pa-";

  private static final String TECHNICAL_ADMIN_USER = "technical-admin";

  public static final String DEFAULT_ADMIN_USER = "admin";
  private static final String DEFAULT_ADMIN_PASSWORD = "admin";

  public static final String USERS_COLLECTION_NAME = "users";

  private static final String USER_ID_PROPERTY = "id";

  // allow users to use lab-user-<USER-ID> as project to store data
  // TODO use shorter prefix
  public static final String USER_PROJECT_PREFIX = "lab-user-";

  // min chars 2 , max chars 40 - added min max length for project and user name validation (to
  // conform with Minio)
  private static final String USER_NAME_VALIDATION_REGEX =
      "^[a-zA-Z0-9][a-zA-Z0-9\\-]{1,38}[a-zA-Z0-9]$"; // TODO do not allow spaces for now

  private static final JavaSerializationHelper javaSerializationHelper =
      new JavaSerializationHelper();

  // TODO currently only a temp memory storage - workaround
  public List<String> deactivatedUsers = new ArrayList<>();

  // ================ Members ============================================= //
  private MongoProfileService mongoProfileService;
  private MongoDbManager mongoDbManager;

  // ================ Constructors & Main ================================= //
  public AuthorizationManager(MongoDbManager mongoDbManager) {
    this.mongoDbManager = mongoDbManager;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  public void addProjectPermission(String user, String projectName) throws Exception {
    projectName = ProjectAuthorizer.prefixProjectNameIfNecessary(projectName);
    addPermission(user, projectName);
  }

  public void removeProjectPermission(String user, String projectName) throws Exception {
    MongoProfile mongoProfile = getUser(user);
    Set<String> permissions = mongoProfile.getPermissions();
    projectName = ProjectAuthorizer.prefixProjectNameIfNecessary(projectName);
    permissions.remove(projectName);

    updatePermissions(user, new ArrayList<>(permissions), false);
  }

  public void removeProjectPermissionFromAllUsers(String projectName) throws Exception {
    projectName = ProjectAuthorizer.prefixProjectNameIfNecessary(projectName);
    removePermissionsFromAllProfiles(Collections.singleton(projectName));
  }

  public List<LabProject> filterProjects(Set<String> permissions, Collection<LabProject> projects) {
    List<LabProject> filteredProjects = new ArrayList<>(projects);
    for (LabProject project : projects) {
      if (!ProjectAuthorizer.hasProjectPermission(permissions, project.getId(), null)) {
        filteredProjects.remove(project);
      }
    }

    return filteredProjects;
  }

  /**
   * Creates a user profile and stores it in the database. Based on that profile, all JWT token will
   * be generated.
   *
   * @param userName Used for the fields "id" and "username" in the database
   * @param password Will be used for Basic authentication. Is encrypted before stored.
   * @return the generated profile or null if the profile already existed
   */
  public MongoProfile createUser(String userName, String password) throws Exception {
    return createUser(userName, password, null, null);
  }

  /**
   * Create an admin profile. Therefore, the passed secret must match the server's secret. If the
   * secret matches, the method {@link LabAuthApiHandler#createUser} is called and its value
   * returned, otherwise an Exception is thrown.
   *
   * @param jwtSecret must match the server's secret
   * @return the created profile with admin permission based on return value of {@link
   *     LabAuthApiHandler#createUser}
   * @throws UnauthorizedException if the passed jwtSecret does not match the server's secret
   */
  public MongoProfile createAdminUser(String userName, String password, String jwtSecret)
      throws Exception {
    if (!StringUtils.isNullOrEmpty(jwtSecret) && jwtSecret.equals(LabConfig.JWT_SECRET)) {
      return createUser(
          userName, password, Collections.singleton(CORE_PERMISSIONS.ADMIN.getName()), null);
    }

    throw new UnauthorizedException("Passed jwtSecret does not match the server's secret.");
  }

  /**
   * Same as {@link AuthorizationManager#createUser(String, String, Set, Map)} but with random
   * password. Can be used to create a technical user profile.
   */
  public MongoProfile createUser(
      String userName, @Nullable Set<String> permissions, @Nullable Map<String, Object> attributes)
      throws Exception {
    return createUser(
        userName,
        generateSecureRandomString(),
        permissions,
        attributes); // create the API token with a random password
  }

  /**
   * Adds a permission to a profile
   *
   * @param user of the profile in the database
   * @param permission the permission to be added. Will overwrite an existing permission with same
   *     name.
   */
  public void addPermission(String user, String permission) throws Exception {
    user = resolveUserName(user);

    MongoProfile mongoProfile = mongoProfileService.findById(user);
    mongoProfile.addPermission(permission);
    mongoProfileService.update(mongoProfile, null);
  }

  /**
   * Removes the specified permission(s) from all profiles
   *
   * @param permissions to be removed from all profiles in the database
   */
  public void removePermissionsFromAllProfiles(Set<String> permissions) throws Exception {
    FindIterable<Document> profileIterator =
        getLabMongoDb().getCollection(USERS_COLLECTION_NAME).find();
    for (Document document : profileIterator) {
      String id = document.getString("id");
      MongoProfile mongoProfile = transformSerializedProfile(document);
      Set<String> profilePermissions = mongoProfile.getPermissions();
      for (String permission : permissions) {
        profilePermissions.remove(permission);
      }
      updatePermissions(id, new ArrayList<>(profilePermissions), false);
    }
  }

  /**
   * Overwrites all existing permissions with the passed set of permissions. If needed, first fetch
   * all user permissions, alter the list and send the new list here. If true, the refresh token for
   * the profile can be changed. Doing so will let existing short-term tokens expire without the
   * possibility to refresh them without new loginUser.
   *
   * @param user of the profile to be updated
   * @param permissions set of all permissions the profile will contain
   * @param changeRefreshToken if true, the refresh token in the database will be changed. JWT
   *     tokens issued before the modification cannot be renewed without a new loginUser.
   */
  public void updatePermissions(
      String user, List<String> permissions, @Nullable Boolean changeRefreshToken)
      throws Exception {
    if (changeRefreshToken == null) {
      changeRefreshToken = false;
    }

    user = resolveUserName(user);

    MongoProfile mongoProfile = mongoProfileService.findById(user);

    mongoProfile.setPermissions(new HashSet<>(permissions));

    if (changeRefreshToken) {
      mongoProfile.addAttribute(REFRESH_TOKEN_ATTRIBUTE, generateSecureRandomString());
    }

    mongoProfileService.update(mongoProfile, null);
  }

  /**
   * Update the password of a given user. THe refresh token will be changed so that the user has to
   * re-login the next time.
   *
   * @param user of the profile to be updated
   * @param password new password
   * @param changeRefreshToken if true, the refresh token in the database will be changed. JWT
   *     tokens issued before the modification cannot be renewed without a new loginUser.
   */
  public void updatePassword(String user, String password, @Nullable Boolean changeRefreshToken)
      throws Exception {
    if (changeRefreshToken == null) {
      changeRefreshToken = false;
    }

    user = resolveUserName(user);

    MongoProfile mongoProfile = mongoProfileService.findById(user);

    if (changeRefreshToken) {
      // Update refresh token -> user needs to login again the next time?
      mongoProfile.addAttribute(REFRESH_TOKEN_ATTRIBUTE, generateSecureRandomString());
    }
    // Update profile and password
    mongoProfileService.update(mongoProfile, password);
  }

  /**
   * Creates a JWT token based on the passed profile.
   *
   * @param profile based on which the token will be generated.
   * @return the generated JWT token
   */
  public <T extends CommonProfile> String createAppToken(T profile) {
    return createJwtToken(profile, JWT_EXPIRATION_TIME_WEEK_IN_SECONDS, TOKEN_TYPES.SHORT);
  }

  /**
   * See {@link AuthorizationManager#createApiToken(String user)}.
   *
   * @param user Used to fetch the user profile from the database
   */
  public String createApiToken(String user) throws Exception {
    return createJwtToken(
        getUser(user), JWT_API_TOKEN_EXPIRATION_TIME_YEAR_IN_SECONDS, TOKEN_TYPES.LONG);
  }

  public String createProjectToken(String project) throws Exception {
    MongoProfile projectUser = getUser(PROJECT_USER_PREFIX + project);

    if (projectUser == null) {
      // create technical project user if it does not exist
      projectUser =
          createUser(
              PROJECT_USER_PREFIX + project,
              new HashSet<>(Arrays.asList(ProjectAuthorizer.prefixProjectNameIfNecessary(project))),
              null);
    }

    return createJwtToken(
        projectUser, JWT_API_TOKEN_EXPIRATION_TIME_YEAR_IN_SECONDS, TOKEN_TYPES.LONG);
  }

  public String createAdminToken() throws Exception {
    MongoProfile technicalAdmin = getUser(TECHNICAL_ADMIN_USER);

    if (technicalAdmin == null) {
      // create technical admin user if it does not exist
      technicalAdmin =
          createAdminUser(TECHNICAL_ADMIN_USER, generateSecureRandomString(), LabConfig.JWT_SECRET);
    }

    return createJwtToken(
        technicalAdmin, JWT_API_TOKEN_EXPIRATION_TIME_YEAR_IN_SECONDS, TOKEN_TYPES.LONG);
  }

  /**
   * Creates a JWT API token based on the passed profile.
   *
   * @param profile based on which the token will be generated.
   */
  public <T extends CommonProfile> String createApiToken(T profile) {
    return createJwtToken(profile, JWT_API_TOKEN_EXPIRATION_TIME_YEAR_IN_SECONDS, TOKEN_TYPES.LONG);
  }

  /**
   * If the refreshToken in the profile is valid (incoming refresh token == refresh token stored in
   * db), issue a new JWT token with new expiration time.
   *
   * @param commonProfile passed profile
   * @return new JWT token or null
   */
  public String refreshToken(CommonProfile commonProfile) {
    MongoProfile savedProfile = mongoProfileService.findById(commonProfile.getId());
    if (savedProfile == null) {
      log.info("Profile was not found in database: " + commonProfile.getId());
      return null;
    }

    if (commonProfile
        .getAttribute(REFRESH_TOKEN_ATTRIBUTE)
        .equals(savedProfile.getAttribute(REFRESH_TOKEN_ATTRIBUTE))) {
      return createAppToken(savedProfile);
    }

    log.info(
        "Refresh token of authorized profile ("
            + commonProfile.getId()
            + ") is not equal to saved profile ("
            + savedProfile.getId()
            + ").");
    return null;
  }

  /** @return all users stored in MongoDB */
  public List<MongoProfile> getUsers() {
    List<MongoProfile> users = new ArrayList<>();
    for (String user : getProfileIds()) {
      try {
        users.add(getUser(user));
      } catch (Exception e) {
        log.error("User not found: " + user + ". This should not happen.");
      }
    }
    return users;
  }

  /** @return all profile ids stored in MongoDB */
  public List<String> getProfileIds() {
    List<String> ids = new ArrayList<>();
    FindIterable<Document> userIterator =
        getLabMongoDb().getCollection(USERS_COLLECTION_NAME).find();
    for (Document document : userIterator) {
      ids.add(document.getString("id"));
    }

    return ids;
  }

  public static LabUser transformProfile(CommonProfile commonProfile) {
    LabUser user = new LabUser();
    user.setId(commonProfile.getId())
        .setName(commonProfile.getUsername())
        .setPermissions(commonProfile.getPermissions())
        .setAttributes(commonProfile.getAttributes());
    return user;
  }

  /** @return the profile with the given {@param user} */
  public MongoProfile getUser(String user) throws Exception {
    user = resolveUserName(user);
    return mongoProfileService.findById(user);
  }

  public static boolean isProjectAdmin(String user) {
    try {
      user = resolveUserName(user);
      if (user.startsWith(PROJECT_USER_PREFIX)) {
        return true;
      } else if (user.startsWith(PROJECT_USER_PREFIX_COMPAT)) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isTechnicalUser(String user) {
    try {
      user = resolveUserName(user);
      if (user.equalsIgnoreCase(DEFAULT_ADMIN_USER)) {
        return true;
      } else if (user.equalsIgnoreCase(TECHNICAL_ADMIN_USER)) {
        return true;
      } else if (user.startsWith(PROJECT_USER_PREFIX)) {
        return true;
      } else if (user.startsWith(PROJECT_USER_PREFIX_COMPAT)) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * Removes profile with {@param user} from the database
   *
   * @param user the id of the user profile to be deleted
   */
  public void deleteProfile(String user) throws Exception {
    user = resolveUserName(user);
    mongoProfileService.removeById(user);
  }

  /**
   * Create a Jwt Token based on the given profile and the passed attributes. The token will be
   * signed using the secret stored in {@link LabConfig#JWT_SECRET}.
   *
   * @param profile Based on which the token will be generated
   * @param expirationTimeInSeconds Defines when the token will expire. An expired token will be
   *     automatically rejected upon all authorization checks.
   * @param tokenType Specifies the type of the token. Will be stored within the token itself.
   *     Tokens of type {@link AuthorizationManager.TOKEN_TYPES#LONG} will not contain any
   *     permissions, hence a db lookup has to be done for them.
   * @return the generated Jwt Token
   */
  public <T extends CommonProfile> String createJwtToken(
      T profile, int expirationTimeInSeconds, TOKEN_TYPES tokenType) {
    JwtGenerator<T> generator =
        new JwtGenerator<>(new SecretSignatureConfiguration(LabConfig.JWT_SECRET));
    profile.addAttribute(TOKEN_TYPE, tokenType.getName());

    /* TODO also send permissions in API token, use token type instead to check if further security details are necessary.
    if (tokenType.equals(TOKEN_TYPES.LONG)) {
        // for long-term tokens (for example used by technical users), we make a database lookup to check
        // whether the sufficient permissions exist. Hence, it doesn't have to be in the JWT token itself (could even be confusing).
        // This lookup is made, because it is very difficult for long-term tokens to revoke/update them in case any permissions changed.
        // This update does not change the profile in the database, but is just temporary to create the token
        profile.setPermissions(new HashSet<>());
    }
    */

    Calendar currentTime = Calendar.getInstance();
    currentTime.add(Calendar.SECOND, expirationTimeInSeconds);
    generator.setExpirationTime(currentTime.getTime());
    return generator.generate(profile);
  }

  public String createTechnicalToken(CommonProfile commonProfile) throws Exception {
    MongoProfile profile =
        createUser(
            commonProfile.getId(), commonProfile.getPermissions(), commonProfile.getAttributes());
    return createApiToken(profile);
  }

  public MongoProfile createUser(
      String userName,
      String password,
      @Nullable Set<String> permissions,
      @Nullable Map<String, Object> attributes)
      throws Exception {

    if (!isValidUserName(userName)) {
      throw new Exception(
          "The user name is not valid. It may only contain letters, digits, spaces, and the"
              + " minus-character.");
    }

    String userId = processNameToId(userName);
    if (userExists(userId)) {
      throw new Exception("User " + userId + " already exists");
    }

    MongoProfile mongoProfile = new MongoProfile();
    mongoProfile.setId(userId);
    mongoProfile.addAttribute(USER_NAME_ATTRIBUTE, userName);
    mongoProfile.addAttribute(REFRESH_TOKEN_ATTRIBUTE, generateSecureRandomString());

    if (!ListUtils.isNullOrEmpty(permissions)) {
      mongoProfile.setPermissions(new HashSet<>(permissions));
    }
    if (!ListUtils.isNullOrEmpty(attributes)) {
      mongoProfile.addAttributes(attributes);
    }

    mongoProfileService.create(mongoProfile, password);
    return mongoProfile;
  }

  private boolean userExists(String userId) {
    try {
      return getLabMongoDb()
              .getCollection(USERS_COLLECTION_NAME)
              .countDocuments(new Document(USER_ID_PROPERTY, userId))
          > 0;
    } catch (Exception e) {
      log.warn("Bad user name.", e);
      return false;
    }
  }

  /**
   * Creates the configuration for all Pac4J-related stuff
   *
   * @return the configuration
   */
  public Config getPac4jConfig() {
    mongoProfileService = new MongoProfileService(mongoDbManager.getClient());
    mongoProfileService.setUsersDatabase(MongoDbManager.MAIN_MONGO_DB);
    mongoProfileService.setUsernameAttribute(
        USER_NAME_ATTRIBUTE); // username attribute has to be set as demanded by Pac4J
    mongoProfileService.setPasswordEncoder(
        new ShiroPasswordEncoder(
            new DefaultPasswordService())); // used to encrypt the profile passwords in the database

    // The authClient checks basic auth credentials of the user stored in MongoDB and, if valid,
    // returns the profile
    DirectBasicAuthClient authClient = new DirectBasicAuthClient(mongoProfileService);

    if (LabConfig.JWT_SECRET.equals(DEFAULT_JWT_SECRET)) {
      log.warn(
          "SECURITY RISK: ML Lab was started with the default jwt secret. "
              + "For a secure setup, set the JWT_SECRET environment variable");
    }

    // The cookieClient checks the jwtCookie and, if valid, returns/injects the profile
    Authenticator jwtAuthenticator =
        new LocalCachingAuthenticator<>(
            new JwtAuthenticator(new SecretSignatureConfiguration(LabConfig.JWT_SECRET)),
            100,
            15,
            TimeUnit.MINUTES);
    CookieClient cookieClient = new CookieClient(COOKIE_ACCESS_TOKEN, jwtAuthenticator);
    HeaderClient headerClient = new HeaderClient(ApiUtils.AUTHORIZATION_HEADER, jwtAuthenticator);
    Config config = new Config(authClient, headerClient, cookieClient);

    // Authorizers are used to check a profile for the right permissions.
    // They can be activated for a class / method by using their names.
    // Note: long-term api tokens don't have access to ADMIN annotated methods, as the permission
    // itself is not in the JWT token and must be looked up in the DB, which doesn't happen with the
    // 'RequireAnyPermissionAuthorizer'. If this should change, a custom authorizer must be written.
    config.addAuthorizer(
        AUTHORIZER_ADMIN,
        new AdminAuthorizer()); // new
                                // RequireAnyPermissionAuthorizer(CORE_PERMISSIONS.ADMIN.getName()));
    config.addAuthorizer(AUTHORIZER_PROJECT, new ProjectAuthorizer());
    config.addAuthorizer(AUTHORIZER_USER, new UserAuthorizer());
    config.addAuthorizer(AUTHORIZER_IS_AUTHENTICATED, new CustomIsAuthenticatedAuthorizer());
    config.getClients().setAjaxRequestResolver(new JaxRsAjaxRequestResolver());

    return config;
  }

  public void configureSecuritySettings(ResourceConfig resourceConfig) {
    Config config = getPac4jConfig();

    resourceConfig
        .register(new JaxRsConfigProvider(config))
        .register(new JaxRsContextFactoryProvider())
        .register(new Pac4JSecurityFeature())
        .register(new Pac4JValueFactoryProvider.Binder());

    // create an admin token on startup so an initial user exists.
    // An already existing admin user is deleted first.
    LabAuthApi authorizationApi = new LabAuthApiHandler();

    try {
      String password = DEFAULT_ADMIN_PASSWORD; // RandomStringUtils.randomAlphanumeric(32);
      MongoProfile adminUser = getUser(DEFAULT_ADMIN_USER);
      if (adminUser == null) {
        // only create if admin user does not exist
        adminUser = createAdminUser(DEFAULT_ADMIN_USER, password, LabConfig.JWT_SECRET);
      }
      String token = createAppToken(adminUser);
      log.info(
          "Admin user created with following password "
              + password
              + "\nApp Token (short-term): "
              + token
              + "\nAPI Token (long-term): "
              + authorizationApi.createApiToken(DEFAULT_ADMIN_USER).getData());
    } catch (Exception e) {
      log.error("Failed to create " + DEFAULT_ADMIN_USER + " profile.", e);
    }
  }

  public <T extends CommonProfile> boolean isAdmin(T profile) {
    if (profile == null) {
      return false;
    }

    Set<String> permissions = profile.getPermissions();
    if (ListUtils.isNullOrEmpty(profile.getPermissions())) {
      // TODO permissions empty -> Not requested, really needed
      permissions = getDbPermissions(profile);
      profile.setPermissions(permissions);
    }

    if (!ListUtils.isNullOrEmpty(profile.getPermissions())) {
      return permissions.contains(AuthorizationManager.CORE_PERMISSIONS.ADMIN.getName());
    }

    return false;
  }

  public <U extends CommonProfile> Set<String> getDbPermissions(U profile) {
    MongoProfile mongoProfile = null;
    try {
      mongoProfile = getUser(profile.getId());
    } catch (Exception e) {
      return new HashSet<>();
    }

    if (mongoProfile == null) {
      return new HashSet<>();
    }

    return mongoProfile.getPermissions();
  }

  public static String resolveUserProject(String user) throws Exception {
    return USER_PROJECT_PREFIX + resolveUserName(user);
  }

  /** Resolve username to user id */
  public static String resolveUserName(String user) throws Exception {
    if (!isValidUserName(user)) {
      user = (!StringUtils.isNullOrEmpty(user)) ? user : "<empty>";
      throw new Exception(
          "The user name "
              + user
              + " is not valid. It may only contain letters, digits, spaces, and the"
              + " minus-character.");
    }

    String userId = processNameToId(user);

    /* TODO check if user exists? - maybe performance problems?
    if (!userExists(userName)) {
        throw new Exception("Project " + userName + " does not exist.");
    } */

    return userId;
  }

  public static MongoProfile transformSerializedProfile(Document user) {
    return (MongoProfile)
        javaSerializationHelper.unserializeFromBase64(user.getString("serializedprofile"));
  }

  /** get list of inactive users based on workspace activity */
  public List<LabUser> getInactiveUsers(@Nullable Integer daysThreshold) {

    if (daysThreshold == null) {
      daysThreshold = 14;
    }

    List<LabUser> inactiveUser = new ArrayList<>();
    long dateThreshold = new Date().getTime() - TimeUnit.DAYS.toMillis(daysThreshold);

    for (MongoProfile user : getUsers()) {
      try {
        LabUser labUser = transformProfile(user);
        if (isTechnicalUser(labUser.getName())) {
          // ignore technical users
          continue;
        }

        String userProject = AuthorizationManager.resolveUserProject(labUser.getId());
        LabFile workspaceMetadataFile =
            ComponentManager.INSTANCE
                .getFileManager()
                .getFile("backups/metadata.json", userProject);

        if (workspaceMetadataFile == null || workspaceMetadataFile.getModifiedAt() == null) {
          log.info("User " + user.getId() + " does not have any workspace metadata backup file.");
          if (daysThreshold == 0) {
            // add user if threshold is 0
            inactiveUser.add(labUser);
          }
          continue;
        }

        if (workspaceMetadataFile.getModifiedAt().getTime() < dateThreshold || dateThreshold == 0) {
          labUser.setLastActivity(workspaceMetadataFile.getModifiedAt());
          inactiveUser.add(labUser);
        }
      } catch (Exception ex) {
        log.warn("Cannot check inactivity for " + user.getId() + ": " + ex.getMessage());
      }
    }
    return inactiveUser;
  }

  // ================ Private Methods ===================================== //

  private static boolean isValidUserName(String userName) {
    // if technical user or it matches regex
    return !StringUtils.isNullOrEmpty(userName) && (userName.matches(USER_NAME_VALIDATION_REGEX));
  }

  private static String processNameToId(String userName) {
    return userName.replace(" ", "-").toLowerCase().trim();
  }

  private MongoProfileService getMongoProfileService() {
    return mongoProfileService;
  }

  private MongoDatabase getLabMongoDb() {
    return this.mongoDbManager.getLabMongoDb();
  }

  /** Return a 32 character long SecureRandom string */
  public static String generateSecureRandomString() {
    final char[] allAllowed =
        "abcdefghijklmnopqrstuvwxyzABCDEFGJKLMNPRSTUVWXYZ0123456789".toCharArray();

    // Use cryptographically secure random number generator
    Random random = new SecureRandom();

    StringBuilder password = new StringBuilder();
    final int stringLength = 32;
    for (int i = 0; i < stringLength; i++) {
      password.append(allAllowed[random.nextInt(allAllowed.length)]);
    }

    return password.toString();
  }

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
  // Security Relevant Constants
  public enum CORE_PERMISSIONS {
    ADMIN("admin");

    String name;

    CORE_PERMISSIONS(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

  public static final String TOKEN_TYPE = "tokenType";

  enum TOKEN_TYPES {
    SHORT("shortTerm"),
    LONG("longTerm");

    private String name;

    TOKEN_TYPES(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }
}
