import auth from "js/auth.js";
import jwt from "js/jwt.js";

function checkWorkspace(r) {
  var token = auth.getToken(r);
  var jwtPayload = jwt.verify(token, process.env.JWT_TOKEN_SECRET);

  // TODO: reject API tokens

  var isAllowedToAccess =
    jwtPayload.username.toLowerCase() === r.variables.id.toLowerCase() ||
    jwtPayload["$int_perms"].indexOf("admin") > -1;

  if (!isAllowedToAccess) {
    r.return(403, "Not authorized to access the workspace");
  }

  // use the username of the JWT token to route to the correct container. No one can temper it as then the JWT token is invalid
  if (!r.variables.id) {
    r.variables.id = jwtPayload.username.toLowerCase();
  }

  r.subrequest("/ping-workspace/" + r.variables.id).then((pingReply) => {
    if (pingReply.status !== 200) {
      r.subrequest("{CONTAXY_BASE_URL}/api/admin/workspace/check", {
        args: "id=" + r.variables.id,
      }).then((checkReply) => {
        if (checkReply.status !== 200) {
          r.log(500, "Workspace check failed for " + r.variables.id);
          r.internalRedirect("{CONTAXY_BASE_URL}/5xx.html");
        } else {
          r.internalRedirect("@workspace");
        }
      });
    } else {
      r.internalRedirect("@workspace");
    }
  });
}

export default { checkWorkspace };
