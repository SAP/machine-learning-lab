import jwt from "js/jwt.js";

function guardServiceAccess(r) {
  if (!isServiceIsInProject(r)) {
    r.return(401, "Requested service is not in specified project!");
  }

  let apiToken;
  if (r.headersIn["Authorization"]) {
    apiToken = modifyToken(r.headersIn["Authorization"]);
  } else {
    apiToken = modifyToken(r.variables.cookie_ct_token);
  }
  const sessionToken = modifyToken(r.variables.cookie_ct_session_token);
  if (!apiToken && !sessionToken) r.return(403, "No auth cookie set");

  const permission = r.variables.permission;
  let message = "";
  if (sessionToken) {
    try {
      verifySessionToken(r, sessionToken, permission);
      r.internalRedirect("@service");
      return;
    } catch (e) {
      r.error(`ERROR2: ${e}`)
      message = e.message;
    }
  }

  if (apiToken) {
    setNewSessionTokenCookie(r, apiToken, permission);
  } else {
    // Return unauthorized as no token could redirect to the service
    r.return(403, message);
  }
}

function isServiceIsInProject(r) {
  const project_id = r.variables.project_id;
  const service_id = r.variables.service_id;
  const extract_project_id_regex = /-p-(?<project_id>[a-zA-Z0-9\-]+)-s-/g;
  const result = extract_project_id_regex.exec(service_id);
  return (
    result &&
    "project_id" in result.groups &&
    result.groups["project_id"] == project_id
  );
}

function modifyToken(token) {
  if (!token) return "";
  return token.replace("Bearer", "").trim();
}


function verifySessionToken(r, sessionToken, permission) {
    let jwtPayload;
    try {
      jwtPayload = jwt.verify(sessionToken, process.env.JWT_TOKEN_SECRET);
    } catch (e) {
      r.error(`ERROR1: ${e}`)
      throw new Error("JWT token not valid.");
    }
    const containsPermission =
      jwtPayload["scope"].indexOf(permission) > -1 ||
      jwtPayload["scope"].indexOf("admin") > -1;
    if (!containsPermission) {
      throw new Error("JWT token does not contain correct permissions.");
    }
}

async function setNewSessionTokenCookie(r, apiToken, permission) {
  try {
    const token = await requestSessionToken(apiToken, permission);
    // Set token as cookie in response:
    const servicePath = permission.substr(0, permission.lastIndexOf("#"));
    const validInSeconds =
      parseInt(process.env.JWT_TOKEN_EXPIRY_MINUTES, 10) * 60 || 900; // Default is 15 minutes
    r.headersOut["Set-Cookie"] = [
      `ct_session_token=${token}; HttpOnly; Path=/${servicePath}; Max-Age=${
        validInSeconds - 5 // Let cookie expire a bit earlier than token
      }`,
      `ct_session_token=${token}; HttpOnly; Path=/${servicePath}b; Max-Age=${
        validInSeconds - 5
      }`,
    ];
    r.internalRedirect("@service");
  } catch (e) {
    if (e.message) {
      r.return(403, message);
    } else {
      r.return(403, "Unkonwn error while requesting session token!");
    }
  }
}

async function requestSessionToken(apiToken, permission) {
  const response = await ngx.fetch(
    `http://127.0.0.1:8090/auth/tokens?token_type=session-token&scope=${encodeURIComponent(
      permission
    )}`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${apiToken}` },
    }
  );
  if (response.status == 200) {
    // The response body contains the JWT session token
    return await response.json();
  } else if (response.status >= 400 && response.status < 500) {
    const error = await response.json();
    throw new Error(
      `Could not create session token! ${response.status} ${error.message}`
    );
  } else {
    throw new Error(
      `Could not create session token! ${response.status} Internal error!`
    );
  }
}


export default { guardServiceAccess };
