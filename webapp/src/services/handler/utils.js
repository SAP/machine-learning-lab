/**
 * Get the username from the jwtToken and the information whether it is an admin
 * @param {string} jwtToken JWT Token with structure X.Y.Z, whereas Y is the payload with all relevent information (X and Z are JWT header & signature)
 * @returns {object} {username: string, isAdmin: boolean}
 */
const parseJwtToken = function (jwtToken) {
  const JWT_FIELD_PERMISSIONS = '$int_perms';
  const PERMISSION_ADMIN = 'admin';

  // decode the middle part (payload) of the JWT base64 string to a normal string and parse it as JSON.
  let payload = JSON.parse(atob(jwtToken.split('.')[1]));
  let username = payload.username;
  let isAdmin = payload[JWT_FIELD_PERMISSIONS].indexOf(PERMISSION_ADMIN) > -1;

  return { username: username, isAdmin: isAdmin };
};

export { parseJwtToken };
