export function displayNameToId(id) {
  // Converts the display name to secret_id. Secred_id is used as a key in the db.
  id.toLowerCase();
  return id.replace(' ', '-');
}
