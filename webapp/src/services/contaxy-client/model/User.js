/**
 * Contaxy API
 * Functionality to create and manage projects, services, jobs, and files.
 *
 * The version of the OpenAPI document: 0.0.5
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 *
 */

import ApiClient from "../ApiClient";

/**
 * The User model module.
 * @module model/User
 * @version 0.0.5
 */
class User {
  /**
   * Constructs a new <code>User</code>.
   * @alias module:model/User
   */
  constructor() {
    User.initialize(this);
  }

  /**
   * Initializes the fields of this object.
   * This method is used by the constructors of any subclasses, in order to implement multiple inheritance (mix-ins).
   * Only for internal use.
   */
  static initialize(obj) {}

  /**
   * Constructs a <code>User</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:model/User} obj Optional instance to populate.
   * @return {module:model/User} The populated <code>User</code> instance.
   */
  static constructFromObject(data, obj) {
    if (data) {
      obj = obj || new User();

      if (data.hasOwnProperty("username")) {
        obj["username"] = ApiClient.convertToType(data["username"], "String");
      }
      if (data.hasOwnProperty("email")) {
        obj["email"] = ApiClient.convertToType(data["email"], "String");
      }
      if (data.hasOwnProperty("disabled")) {
        obj["disabled"] = ApiClient.convertToType(data["disabled"], "Boolean");
      }
      if (data.hasOwnProperty("id")) {
        obj["id"] = ApiClient.convertToType(data["id"], "String");
      }
      if (data.hasOwnProperty("technical_user")) {
        obj["technical_user"] = ApiClient.convertToType(
          data["technical_user"],
          "Boolean"
        );
      }
      if (data.hasOwnProperty("created_at")) {
        obj["created_at"] = ApiClient.convertToType(data["created_at"], "Date");
      }
    }
    return obj;
  }
}

/**
 * A unique username on the system.
 * @member {String} username
 */
User.prototype["username"] = undefined;

/**
 * User email address.
 * @member {String} email
 */
User.prototype["email"] = undefined;

/**
 * Indicates that user is disabled. Disabling a user will prevent any access to user-accesible resources.
 * @member {Boolean} disabled
 * @default false
 */
User.prototype["disabled"] = false;

/**
 * Unique ID of the user.
 * @member {String} id
 */
User.prototype["id"] = undefined;

/**
 * Indicates if the user is a technical user created by the system.
 * @member {Boolean} technical_user
 * @default false
 */
User.prototype["technical_user"] = false;

/**
 * Timestamp of the user creation. Assigned by the server and read-only.
 * @member {Date} created_at
 */
User.prototype["created_at"] = undefined;

export default User;
