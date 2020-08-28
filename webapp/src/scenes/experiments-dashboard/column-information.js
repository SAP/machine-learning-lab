export const COLUMNS = {
    OVERVIEW: ["name", "project", "operator", "scriptName", "scriptType", "startedAt", "finishedAt", "updatedAt", "duration", "status", "command", "clientVersion","result"],
    DATE: ["startedAt", "finishedAt", "updatedAt"],
    INTEGER: ["result"],
    DURATION: ["duration"],
    HIDDEN: ["project", "command", "key", "updatedAt", "scriptName", "scriptType"],
    DEFAULTHIDDEN: ["project", "command", "key", "updatedAt", "scriptName", "scriptType"],
    INITIALLYHIDDEN: ["project", "command", "scriptName", "scriptType", "dependencies", "key", "clientVersion"],
    LARGE:["name", "key", "os", "cpu", "artifacts", "experimentDir", "input", "output"],
    BLACKLIST: ['dependencies'],
    OUTPUTFILES: ["input", "output", "sourceCode", "sourceScript", "stdout", "tensorboardLogs"]
}

export const NAMETITLE = {
    OVERVIEW: { field: "overview", title: "Overview" }, 
    METRIC: { field: "metric", title: "Metric" }, 
    VALUE: { field: "value", title: "Value" },
    LIBRARY: { field: "library", title: "Library" },
    SPEC:{ field: "spec", title: "Spec" }, 
    INFO: {field: "info", title: "Info" }, 
    PARAMETERS: { field: "parameters", title: "Parameters" },
    TYPE: { field: "type", title: "Type" }, 
}

export const FILTER = {
    DATE: ["month", "contains", "startsWith", "endsWith"],
    METRIC: ["greaterThanOrEqual", "greaterThan", "lessThan", "lessThanOrEqual", "equal", "notEqual"]
    
}
