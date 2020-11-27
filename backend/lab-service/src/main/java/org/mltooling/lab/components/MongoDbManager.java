package org.mltooling.lab.components;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

public class MongoDbManager {

  // ================ Constants =========================================== //
  private static final int MONGO_TIMEOUT = 12000;

  public static final String MAIN_MONGO_DB = "lab";

  private static final String DEFAULT_AUTHENTICATION_DATABASE = "admin";

  // ================ Members ============================================= //
  private MongoClient mongoClient;

  // ================ Constructors & Main ================================= //
  public MongoDbManager(
      String mongoHost, int mongoPort, boolean sslEnabled, String mongoUser, String mongoPassword) {
    MongoCredential mongoCredential =
        MongoCredential.createCredential(
            mongoUser, DEFAULT_AUTHENTICATION_DATABASE, mongoPassword.toCharArray());
    mongoClient =
        new MongoClient(
            new ServerAddress(mongoHost, mongoPort),
            mongoCredential,
            new MongoClientOptions.Builder()
                .heartbeatConnectTimeout(MONGO_TIMEOUT)
                .heartbeatSocketTimeout(MONGO_TIMEOUT)
                .connectTimeout(MONGO_TIMEOUT)
                .maxConnectionIdleTime(MONGO_TIMEOUT)
                .sslEnabled(sslEnabled)
                .sslInvalidHostNameAllowed(true)
                // .maxConnectionLifeTime(MONGO_TIMEOUT)
                // .socketTimeout(MONGO_TIMEOUT)
                .build());
  }
  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  /**
   * Get the MongoClient initialized for the whole application. On first call, the MongoClient
   * instance is initialized using the Lab's configuration to get the Mongo connection information.
   *
   * @return the default-initialized MongoClient
   */
  public MongoClient getClient() {
    return mongoClient;
  }

  /**
   * Get the Mongo DB where most Lab relevant collections are stored.
   *
   * @return MongoDatabase object pointing to the Lab's main db.
   */
  public MongoDatabase getLabMongoDb() {
    // tested performance: should be fine to always query the database
    return getClient().getDatabase(MAIN_MONGO_DB);
  }

  public boolean isMongoAvailable() {
    if (getClient() == null) {
      return false;
    }

    try {
      getClient().getAddress();
    } catch (Exception e) {
      return false;
    }

    return true;
  }
  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
