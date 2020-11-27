package org.mltooling.lab.components;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import java.util.*;
import javax.annotation.Nullable;
import org.bson.Document;
import org.mltooling.core.lab.model.LabEvent;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.core.utils.structures.PropertyContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventLogManager {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(EventLogManager.class);

  private final String EVENTS_COLLECTION = "events";

  private enum EVENTS_COLLECTION_PROPERTIES {
    NAME("name"),
    ATTRIBUTES("attributes"),
    CREATED_AT("createdAt");

    public String value;

    EVENTS_COLLECTION_PROPERTIES(String value) {
      this.value = value;
    }
  }

  // ================ Members ============================================= //
  private MongoDbManager mongoDbManager;
  private boolean loggingActivated = true;

  // ================ Constructors & Main ================================= //
  public EventLogManager(MongoDbManager mongoDbManager) {
    this.mongoDbManager = mongoDbManager;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public void logEvent(String name) {
    this.logEvent(name, new HashMap<>());
  }

  public void logEvent(String name, PropertyContainer attributes) {
    this.logEvent(name, attributes.getProperties());
  }

  public void logEvent(String name, Map<String, Object> attributes) {
    if (attributes == null) {
      attributes = new HashMap<>();
    }

    if (loggingActivated) {
      getEventsCollection()
          .insertOne(
              new Document(EVENTS_COLLECTION_PROPERTIES.NAME.value, name)
                  .append(EVENTS_COLLECTION_PROPERTIES.ATTRIBUTES.value, attributes)
                  .append(EVENTS_COLLECTION_PROPERTIES.CREATED_AT.value, new Date()));
    }
  }

  public List<LabEvent> getEvents(@Nullable String eventName) {
    List<LabEvent> events = new ArrayList<>();

    FindIterable<Document> dbEvents;
    if (StringUtils.isNullOrEmpty(eventName)) {
      dbEvents = getEventsCollection().find();
    } else {
      dbEvents =
          getEventsCollection()
              .find(new Document(new Document(EVENTS_COLLECTION_PROPERTIES.NAME.value, eventName)));
    }

    for (Document eventDoc : dbEvents) {
      events.add(transformMongoDocument(eventDoc));
    }

    return events;
  }

  public void setLoggingActivated(boolean loggingActivated) {
    this.loggingActivated = loggingActivated;
  }

  // ================ Private Methods ===================================== //
  private LabEvent transformMongoDocument(Document projectDoc) {
    return new LabEvent()
        .setName(projectDoc.getString(EVENTS_COLLECTION_PROPERTIES.NAME.value))
        .setCreatedAt(projectDoc.getDate(EVENTS_COLLECTION_PROPERTIES.CREATED_AT.value))
        .setAttributes(projectDoc.get(EVENTS_COLLECTION_PROPERTIES.ATTRIBUTES.value, Map.class));
  }

  private MongoCollection<Document> getEventsCollection() {
    return mongoDbManager.getLabMongoDb().getCollection(EVENTS_COLLECTION);
  }
  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
