import jwt from "js/jwt.js";

function modifyToken(token) {
  if (!token) return "";
  return token.replace("Bearer", "").trim();
}

function verifyAccess(r, apiToken, permission) {
  var validInSeconds = 900; // valid for 15 minutes
  return new Promise((resolve, reject) => {
    ngx
      .fetch(
        `http://localhost:8090/auth/tokens/verify?permission=${encodeURIComponent(
          permission
        )}`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${apiToken}` },
        }
      )
      .then((res) => {
        if (res.status !== 204) {
          reject(false);
          return;
        }

        var generated_session_token = jwt.generate(
          process.env.JWT_TOKEN_SECRET,
          undefined,
          { perms: permission },
          validInSeconds
        );
        r.headersOut[
          "Set-Cookie"
        ] = `ct_session_token=${generated_session_token}; HttpOnly; Path=/; Max-Age=${validInSeconds}`;
        resolve(true);
      })
      .catch((e) => {
        reject(false);
      });
  });
}

function isServiceIsInProject(r) {
  var project_id = r.variables.project_id
  var service_id = r.variables.service_id
  var extract_project_id_regex = /-p-(?<project_id>[a-zA-Z0-9\-]+)-s-/g
  var result = extract_project_id_regex.exec(service_id)
  return result && "project_id" in result.groups
         && result.groups["project_id"] == project_id
}

function guardServiceAccess(r) {
  if(!isServiceIsInProject(r)){
    r.return(401, "Requested service is not in specified project!");
  }
  if (r.headersIn['Authorization']) {
    var apiToken = modifyToken(r.headersIn['Authorization'])
  } else {
    var apiToken = modifyToken(r.variables.cookie_ct_token);
  }
  var sessionToken = modifyToken(r.variables.cookie_ct_session_token);
  var permission = r.variables.permission;
  if (!apiToken && !sessionToken) r.return(403, "No auth cookie set");
  var message = "";
  if (sessionToken) {
    try {
      var jwtPayload = jwt.verify(sessionToken, process.env.JWT_TOKEN_SECRET);

      var containsPermission =
        jwtPayload["perms"].indexOf(permission) > -1 ||
        jwtPayload["perms"].indexOf("admin") > -1;
      if (containsPermission) {
        r.internalRedirect("@service");
        return;
      }

      message = "JWT token does not contain correct permissions.";
    } catch (e) {
      message = "JWT token not valid.";
    }
  }

  if (apiToken) {
    verifyAccess(r, apiToken, permission)
      .then(
        (res) => {
          r.internalRedirect("@service");
        },
        () => r.return(403, "API Token not valid")
      )
      .catch((e) => {
        r.return(403, "API Token not valid");
      });
  } else {
    // Return unauthorized as no token could redirect to the service
    r.return(403, message);
  }
}

export default { guardServiceAccess };
