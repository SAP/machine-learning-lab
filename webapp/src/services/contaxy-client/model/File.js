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
 * The File model module.
 * @module model/File
 * @version 0.0.5
 */
class File {
  /**
   * Constructs a new <code>File</code>.
   * @alias module:model/File
   * @param key {String} The (virtual) path of the file. This path might not correspond to the actual path on the file storage.
   */
  constructor(key) {
    File.initialize(this, key);
  }

  /**
   * Initializes the fields of this object.
   * This method is used by the constructors of any subclasses, in order to implement multiple inheritance (mix-ins).
   * Only for internal use.
   */
  static initialize(obj, key) {
    obj["key"] = key;
  }

  /**
   * Constructs a <code>File</code> from a plain JavaScript object, optionally creating a new instance.
   * Copies all relevant properties from <code>data</code> to <code>obj</code> if supplied or a new instance if not.
   * @param {Object} data The plain JavaScript object bearing properties of interest.
   * @param {module:model/File} obj Optional instance to populate.
   * @return {module:model/File} The populated <code>File</code> instance.
   */
  static constructFromObject(data, obj) {
    if (data) {
      obj = obj || new File();

      if (data.hasOwnProperty("key")) {
        obj["key"] = ApiClient.convertToType(data["key"], "String");
      }
      if (data.hasOwnProperty("content_type")) {
        obj["content_type"] = ApiClient.convertToType(
          data["content_type"],
          "String"
        );
      }
      if (data.hasOwnProperty("external_id")) {
        obj["external_id"] = ApiClient.convertToType(
          data["external_id"],
          "String"
        );
      }
      if (data.hasOwnProperty("id")) {
        obj["id"] = ApiClient.convertToType(data["id"], "String");
      }
      if (data.hasOwnProperty("name")) {
        obj["name"] = ApiClient.convertToType(data["name"], "String");
      }
      if (data.hasOwnProperty("created_at")) {
        obj["created_at"] = ApiClient.convertToType(data["created_at"], "Date");
      }
      if (data.hasOwnProperty("created_by")) {
        obj["created_by"] = ApiClient.convertToType(
          data["created_by"],
          "String"
        );
      }
      if (data.hasOwnProperty("updated_at")) {
        obj["updated_at"] = ApiClient.convertToType(data["updated_at"], "Date");
      }
      if (data.hasOwnProperty("updated_by")) {
        obj["updated_by"] = ApiClient.convertToType(
          data["updated_by"],
          "String"
        );
      }
      if (data.hasOwnProperty("display_name")) {
        obj["display_name"] = ApiClient.convertToType(
          data["display_name"],
          "String"
        );
      }
      if (data.hasOwnProperty("description")) {
        obj["description"] = ApiClient.convertToType(
          data["description"],
          "String"
        );
      }
      if (data.hasOwnProperty("icon")) {
        obj["icon"] = ApiClient.convertToType(data["icon"], "String");
      }
      if (data.hasOwnProperty("metadata")) {
        obj["metadata"] = ApiClient.convertToType(data["metadata"], {
          String: "String",
        });
      }
      if (data.hasOwnProperty("disabled")) {
        obj["disabled"] = ApiClient.convertToType(data["disabled"], "Boolean");
      }
      if (data.hasOwnProperty("file_extension")) {
        obj["file_extension"] = ApiClient.convertToType(
          data["file_extension"],
          "String"
        );
      }
      if (data.hasOwnProperty("file_size")) {
        obj["file_size"] = ApiClient.convertToType(data["file_size"], "Number");
      }
      if (data.hasOwnProperty("version")) {
        obj["version"] = ApiClient.convertToType(data["version"], "String");
      }
      if (data.hasOwnProperty("available_versions")) {
        obj["available_versions"] = ApiClient.convertToType(
          data["available_versions"],
          ["String"]
        );
      }
      if (data.hasOwnProperty("latest_version")) {
        obj["latest_version"] = ApiClient.convertToType(
          data["latest_version"],
          "Boolean"
        );
      }
      if (data.hasOwnProperty("md5_hash")) {
        obj["md5_hash"] = ApiClient.convertToType(data["md5_hash"], "String");
      }
      if (data.hasOwnProperty("etag")) {
        obj["etag"] = ApiClient.convertToType(data["etag"], "String");
      }
      if (data.hasOwnProperty("extension_id")) {
        obj["extension_id"] = ApiClient.convertToType(
          data["extension_id"],
          "String"
        );
      }
    }
    return obj;
  }
}

/**
 * The (virtual) path of the file. This path might not correspond to the actual path on the file storage.
 * @member {String} key
 */
File.prototype["key"] = undefined;

/**
 * A standard MIME type describing the format of the contents. If an file is stored without a Content-Type, it is served as application/octet-stream.
 * @member {String} content_type
 */
File.prototype["content_type"] = undefined;

/**
 * The ID (or access instruction) of the file on the file storage provider.
 * @member {String} external_id
 */
File.prototype["external_id"] = undefined;

/**
 * Resource ID. Identifies a resource in a given context and time, for example, in combination with its type. Used in API operations and/or configuration files.
 * @member {String} id
 */
File.prototype["id"] = undefined;

/**
 * Resource Name. A relative URI-path that uniquely identifies a resource within the system. Assigned by the server and read-only.
 * @member {String} name
 */
File.prototype["name"] = undefined;

/**
 * Timestamp of the resource creation. Assigned by the server and read-only.
 * @member {Date} created_at
 */
File.prototype["created_at"] = undefined;

/**
 * Resource name of the entity responsible for the creation of this resource. Assigned by the server and read-only.
 * @member {String} created_by
 */
File.prototype["created_by"] = undefined;

/**
 * Timestamp of the last resource modification. Is updated when create/patch/delete operation is performed. Assigned by the server and read-only.
 * @member {Date} updated_at
 */
File.prototype["updated_at"] = undefined;

/**
 * Resource name of the entity responsible for the last modification of this resource. Assigned by the server and read-only.
 * @member {String} updated_by
 */
File.prototype["updated_by"] = undefined;

/**
 * A user-defined human-readable name of the resource. The name can be up to 128 characters long and can consist of any UTF-8 character.
 * @member {String} display_name
 */
File.prototype["display_name"] = undefined;

/**
 * A user-defined short description about the resource. Can consist of any UTF-8 character.
 * @member {String} description
 */
File.prototype["description"] = undefined;

/**
 * Identifier or image URL used for displaying this resource.
 * @member {String} icon
 */
File.prototype["icon"] = undefined;

/**
 * A collection of arbitrary key-value pairs associated with this resource that does not need predefined structure. Enable third-party integrations to decorate objects with additional metadata for their own use.
 * @member {Object.<String, String>} metadata
 */
File.prototype["metadata"] = undefined;

/**
 * Allows to disable a resource without requiring deletion. A disabled resource is not shown and not accessible.
 * @member {Boolean} disabled
 * @default false
 */
File.prototype["disabled"] = false;

/**
 * The full file extension extracted from the key field. May contain multiple concatenated extensions, such as `tar.gz`.
 * @member {String} file_extension
 */
File.prototype["file_extension"] = undefined;

/**
 * The file size in bytes.
 * @member {Number} file_size
 */
File.prototype["file_size"] = undefined;

/**
 * Version tag of this file. The version order might not be inferable from the version tag.
 * @member {String} version
 */
File.prototype["version"] = undefined;

/**
 * All version tags available for the given file.
 * @member {Array.<String>} available_versions
 */
File.prototype["available_versions"] = undefined;

/**
 * Indicates if this is the latest available version of the file.
 * @member {Boolean} latest_version
 */
File.prototype["latest_version"] = undefined;

/**
 * The base64-encoded 128-bit MD5 digest of the file. This can be used for checking the file integrity.
 * @member {String} md5_hash
 */
File.prototype["md5_hash"] = undefined;

/**
 * The etag of the file (mainly used by S3 storage). An entity-tag is an opaque validator for differentiating between multiple representations of the same resource
 * @member {String} etag
 */
File.prototype["etag"] = undefined;

/**
 * The extension ID in case the file is provided via an extension.
 * @member {String} extension_id
 */
File.prototype["extension_id"] = undefined;

export default File;
