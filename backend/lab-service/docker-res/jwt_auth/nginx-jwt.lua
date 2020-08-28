local jwt = require "resty.jwt"
local cjson = require "cjson"
local basexx = require "basexx"
local lab_utils = require "lab_utils"
local secret = os.getenv("JWT_SECRET")

assert(secret ~= nil, "Environment variable JWT_SECRET not set")

if os.getenv("JWT_SECRET_IS_BASE64_ENCODED") == 'true' then
    -- convert from URL-safe Base64 to Base64
    local r = #secret % 4
    if r == 2 then
        secret = secret .. "=="
    elseif r == 3 then
        secret = secret .. "="
    end
    secret = string.gsub(secret, "-", "+")
    secret = string.gsub(secret, "_", "/")

    -- convert from Base64 to UTF-8 string
    secret = basexx.from_base64(secret)
end

local M = {}

function M.load_jwt() 
    return jwt:load_jwt(M.get_token(), secret)
end

function M.get_token()
    -- require Authorization request header
    local auth_header = ngx.var.http_Authorization
    local token = ngx.var.arg_lab_token

    local cookie_name = "lab_access_token"
    
    if token ~= nil then
        -- if token is provided as get parameter, set as cookie
        ngx.header["Set-Cookie"] = cookie_name .. "=" .. token .. "; path=/"
        -- TODO set correct path?
        -- remove token from args
        local args = ngx.req.get_uri_args()
        args.arg_lab_token = nil
        ngx.req.set_uri_args(args)
    end

    if token == nil and auth_header ~= nil then
        _, _, token = string.find(auth_header, "Bearer%s+(.+)")
        -- token is nil if the token is not a Bearer token
        -- if token == nil then
        --     -- use auth header as full token
        --    token = auth_header
        -- end
    end

    if token == nil and ngx.var["cookie_" .. cookie_name] ~= nil then
        token = ngx.var["cookie_" .. cookie_name]
    elseif token == nil and ngx.var["cookie_" .. cookie_name] == nil then
        ngx.log(ngx.WARN, "No Authorization information provided")
        ngx.exit(ngx.HTTP_UNAUTHORIZED)
    end

    if token == nil then
        ngx.log(ngx.WARN, "Missing token")
        ngx.exit(ngx.HTTP_UNAUTHORIZED)
    end

    ngx.log(ngx.INFO, "Authorization Token: " .. token)
    
    return token
end

function M.auth(claim_specs)
    local token = M.get_token()

    -- require valid JWT
    local jwt_obj = jwt:verify(secret, token)
    if jwt_obj.verified == false then
        ngx.log(ngx.WARN, "Invalid token: ".. jwt_obj.reason)
        ngx.exit(ngx.HTTP_UNAUTHORIZED)
    end

    ngx.log(ngx.INFO, "JWT: " .. cjson.encode(jwt_obj))

    -- optionally require specific claims
    if claim_specs ~= nil then
        --TODO: test
        -- make sure they passed a Table
        if type(claim_specs) ~= 'table' then
            ngx.log(ngx.STDERR, "Configuration error: claim_specs arg must be a table")
            ngx.exit(ngx.HTTP_INTERNAL_SERVER_ERROR)
        end

        -- process each claim
        local blocking_claim = ""
        for claim, spec in pairs(claim_specs) do
            -- make sure token actually contains the claim
            local claim_value = jwt_obj.payload[claim]
            if claim_value == nil then
                blocking_claim = claim .. " (missing)"
                break
            end

            local spec_actions = {
                -- claim spec is a string (pattern)
                ["string"] = function (pattern, val, jwt_obj)
                    return string.match(val, pattern) ~= nil
                end,

                -- claim spec is a predicate function
                ["function"] = function (func, val, jwt_obj)
                    -- convert truthy to true/false
                    if func(val, jwt_obj) then
                        return true
                    else
                        return false
                    end
                end
            }

            local spec_action = spec_actions[type(spec)]

            -- make sure claim spec is a supported type
            -- TODO: test
            if spec_action == nil then
                ngx.log(ngx.STDERR, "Configuration error: claim_specs arg claim '" .. claim .. "' must be a string or a table")
                ngx.exit(ngx.HTTP_INTERNAL_SERVER_ERROR)
            end

            -- make sure token claim value satisfies the claim spec
            if not spec_action(spec, claim_value, jwt_obj) then
                blocking_claim = claim
                break
            end
        end

        if blocking_claim ~= "" then
            ngx.log(ngx.WARN, "User did not satisfy claim: ".. blocking_claim)
            ngx.exit(ngx.HTTP_UNAUTHORIZED)
        end
    end

    -- write the X-Auth-UserId header
    ngx.header["X-Auth-UserId"] = jwt_obj.payload.sub
end

function M.table_contains(table, item)
    for _, value in pairs(table) do
        if value == item then return true end
    end
    return false
end

M.lab_utils = lab_utils

return M