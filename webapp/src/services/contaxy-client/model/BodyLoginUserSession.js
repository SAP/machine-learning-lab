/**
 * Contaxy API
 * Functionality to create and manage projects, services, jobs, and files.
 *
 * The version of the OpenAPI document: 0.0.11
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 *
 */

import ApiClient from '../ApiClient';

/**
 * The BodyLoginUserSession model module.
 * @module model/BodyLoginUserSession
 * @version 0.0.11
 */
class BodyLoginUserSession {
  /**
   * Constructs a new <code>BodyLoginUserSession</code>.
   * @alias module:model/BodyLoginUserSession
   * @param username {String} The user’s username or email used for login.
   * @param password {String} The user’s password.
   */
  constructor(username, password) {
    BodyLoginUserSession.initialize(this, username, password);
  }

  /**
   * Initializes the fields of this object.
   * This method is used by the constructors of any subclasses, in order to implement multiple inheritance (mix-ins).
   * Only for internal use.
   */
  static initialize(obj, username, password) {
    obj['username'] = username;
    obj['password'] = password;
  }

  /**
   * Constructs a <code>BodyLoginUserSession</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:model/BodyLoginUserSession} obj Optional instance to populate.
   * @return {module:model/BodyLoginUserSession} The populated <code>BodyLoginUserSession</code> instance.
   */
  static constructFromObject(data, obj) {
    if (data) {
      obj = obj || new BodyLoginUserSession();

      if (data.hasOwnProperty('username')) {
        obj['username'] = ApiClient.convertToType(data['username'], 'String');
      }
      if (data.hasOwnProperty('password')) {
        obj['password'] = ApiClient.convertToType(data['password'], 'String');
      }
    }
    return obj;
  }
}

/**
 * The user’s username or email used for login.
 * @member {String} username
 */
BodyLoginUserSession.prototype['username'] = undefined;

/**
 * The user’s password.
 * @member {String} password
 */
BodyLoginUserSession.prototype['password'] = undefined;

export default BodyLoginUserSession;
