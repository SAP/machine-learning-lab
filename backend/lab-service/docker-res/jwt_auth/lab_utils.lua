local M = {}

--- Callback function executed by nginx-jwt.auth
-- @param field_value The first argument
-- @param[opt=false] field_value_2 The second argument
-- @return true if either field_value or field_value_2 match the value of the specified JWT field (specified by the nginx-jwt.auth logic)
function M.table_values_comparison_callback(field_value, field_value_2)
    field_value_2 = field_value_2 or false
    -- the values of the json field (array) are checked for the value 'field_value' or 'field_value2'. (the callback will be called by the nginx-jwt.auth function)
    return function(jwt_field)
        for _, value in pairs(jwt_field) do
            if (value == field_value or value == field_value_2) then return true end
        end
    end
end

function M.fieldContains(field_value)
    return function(jwt_field)
        for _, value in pairs(jwt_field) do
            if (value == field_value) then return true end
        end
    end
end

function M.isAdmin()
    return M.fieldContains("admin")
end

function M.isProjectMember(project_name)
    return M.table_values_comparison_callback("project-" .. project_name)
end

function M.isProjectMemberOrAdmin(project_name)
    return (M.table_values_comparison_callback("project-" .. project_name, "admin"))
end

function M.isResourceOwner(resource_name)
    return M.table_values_comparison_callback("owner-" .. resource_name)
end

return M