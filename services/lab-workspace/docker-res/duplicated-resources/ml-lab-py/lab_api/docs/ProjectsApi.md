# lab_api.swagger_client.ProjectsApi

All URIs are relative to *https://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create_project**](ProjectsApi.md#create_project) | **POST** /api/projects | Create a new project.
[**create_project_token**](ProjectsApi.md#create_project_token) | **GET** /api/projects/{project}/token | Get project token for the specified project.
[**delete_experiment**](ProjectsApi.md#delete_experiment) | **DELETE** /api/projects/{project}/experiments | Deletes an experiment from a specified project.
[**delete_file**](ProjectsApi.md#delete_file) | **DELETE** /api/projects/{project}/files | Deletes a file from a specified project.
[**delete_job**](ProjectsApi.md#delete_job) | **DELETE** /api/projects/{project}/jobs/{job} | Deletes a job from a project.
[**delete_project**](ProjectsApi.md#delete_project) | **DELETE** /api/projects/{project} | Delete a project and all its associated networks, services &amp; data.
[**delete_scheduled_job**](ProjectsApi.md#delete_scheduled_job) | **DELETE** /api/projects/{project}/jobs/scheduled/{job} | Remove a scheduled job.
[**delete_service**](ProjectsApi.md#delete_service) | **DELETE** /api/projects/{project}/services/{service} | Delete a specific project service by name or type.
[**deploy_job**](ProjectsApi.md#deploy_job) | **POST** /api/projects/{project}/jobs | Deploy a job for a specified project based on a provided image.
[**deploy_model**](ProjectsApi.md#deploy_model) | **POST** /api/projects/{project}/files/models/deploy | Deploy a model as a service for a specified project.
[**deploy_service**](ProjectsApi.md#deploy_service) | **POST** /api/projects/{project}/services | Deploy a service for a specified project based on a provided image.
[**download_file**](ProjectsApi.md#download_file) | **GET** /api/projects/{project}/files/download | Download file from remote storage of selected project.
[**get_experiments**](ProjectsApi.md#get_experiments) | **GET** /api/projects/{project}/experiments | Get all experiments of a project with details.
[**get_file_info**](ProjectsApi.md#get_file_info) | **GET** /api/projects/{project}/files/info | Get info about the specified file.
[**get_files**](ProjectsApi.md#get_files) | **GET** /api/projects/{project}/files | Get all files of a project with details and general statistics filtered by data type and/or prefix.
[**get_job**](ProjectsApi.md#get_job) | **GET** /api/projects/{project}/jobs/{job} | Get a specific project job by name or type.
[**get_job_logs**](ProjectsApi.md#get_job_logs) | **GET** /api/projects/{project}/jobs/{job}/logs | Get the logs for a job.
[**get_jobs**](ProjectsApi.md#get_jobs) | **GET** /api/projects/{project}/jobs | Get all jobs of a project with details and general statistics.
[**get_project**](ProjectsApi.md#get_project) | **GET** /api/projects/{project} | Get details for the specified project.
[**get_projects**](ProjectsApi.md#get_projects) | **GET** /api/projects | Get all available projects with details.
[**get_scheduled_jobs**](ProjectsApi.md#get_scheduled_jobs) | **GET** /api/projects/{project}/jobs/scheduled | Get all scheduled jobs of a project.
[**get_service**](ProjectsApi.md#get_service) | **GET** /api/projects/{project}/services/{service} | Get a specific project service by name or type.
[**get_service_logs**](ProjectsApi.md#get_service_logs) | **GET** /api/projects/{project}/services/{service}/logs | Get the logs for a service.
[**get_services**](ProjectsApi.md#get_services) | **GET** /api/projects/{project}/services | Get all services of a project with details and general statistics.
[**is_project_available**](ProjectsApi.md#is_project_available) | **GET** /api/projects/{project}/available | Checks if a project name is available for project creation .
[**sync_experiment**](ProjectsApi.md#sync_experiment) | **POST** /api/projects/{project}/experiments | Sync an experiment to the experiments DB of a project.
[**upload_file**](ProjectsApi.md#upload_file) | **POST** /api/projects/{project}/files/upload | Upload file to remote storage of selected project and returns key.


# **create_project**
> LabProjectResponse create_project(body, authorization=authorization)

Create a new project.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
body = lab_api.swagger_client.LabProjectConfig() # LabProjectConfig | Project Configuration
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Create a new project.
    api_response = api_instance.create_project(body, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->create_project: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**LabProjectConfig**](LabProjectConfig.md)| Project Configuration | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabProjectResponse**](LabProjectResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **create_project_token**
> StringResponse create_project_token(project, authorization=authorization)

Get project token for the specified project.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get project token for the specified project.
    api_response = api_instance.create_project_token(project, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->create_project_token: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StringResponse**](StringResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete_experiment**
> StatusMessageFormat delete_experiment(project, experiment, authorization=authorization)

Deletes an experiment from a specified project.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
experiment = 'experiment_example' # str | Experiment ID
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Deletes an experiment from a specified project.
    api_response = api_instance.delete_experiment(project, experiment, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->delete_experiment: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **experiment** | **str**| Experiment ID | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StatusMessageFormat**](StatusMessageFormat.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete_file**
> StatusMessageFormat delete_file(project, file_key, keep_latest_versions=keep_latest_versions, authorization=authorization)

Deletes a file from a specified project.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
file_key = 'file_key_example' # str | File Key
keep_latest_versions = 56 # int | Keep the n-latest Versions (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Deletes a file from a specified project.
    api_response = api_instance.delete_file(project, file_key, keep_latest_versions=keep_latest_versions, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->delete_file: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **file_key** | **str**| File Key | 
 **keep_latest_versions** | **int**| Keep the n-latest Versions | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StatusMessageFormat**](StatusMessageFormat.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete_job**
> StatusMessageFormat delete_job(project, job, authorization=authorization)

Deletes a job from a project.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
job = 'job_example' # str | Job Name or Id
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Deletes a job from a project.
    api_response = api_instance.delete_job(project, job, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->delete_job: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **job** | **str**| Job Name or Id | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StatusMessageFormat**](StatusMessageFormat.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete_project**
> StatusMessageFormat delete_project(project, authorization=authorization)

Delete a project and all its associated networks, services & data.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Delete a project and all its associated networks, services & data.
    api_response = api_instance.delete_project(project, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->delete_project: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StatusMessageFormat**](StatusMessageFormat.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete_scheduled_job**
> StatusMessageFormat delete_scheduled_job(project, job)

Remove a scheduled job.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
job = 'job_example' # str | Job ID

try:
    # Remove a scheduled job.
    api_response = api_instance.delete_scheduled_job(project, job)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->delete_scheduled_job: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **job** | **str**| Job ID | 

### Return type

[**StatusMessageFormat**](StatusMessageFormat.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete_service**
> StatusMessageFormat delete_service(project, service, authorization=authorization)

Delete a specific project service by name or type.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
service = 'service_example' # str | Service Name or Type
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Delete a specific project service by name or type.
    api_response = api_instance.delete_service(project, service, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->delete_service: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **service** | **str**| Service Name or Type | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StatusMessageFormat**](StatusMessageFormat.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deploy_job**
> LabJobResponse deploy_job(project, image, body=body, schedule=schedule, name=name, authorization=authorization)

Deploy a job for a specified project based on a provided image.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
image = 'image_example' # str | Image Name
body = NULL # object | Job Configuration (optional)
schedule = 'schedule_example' # str | Cron Schedule in UNIX format. If specified, the job is executed repeatedly according to the cron definition. A job cannot run more often than once a minute. (optional)
name = 'name_example' # str | Job Name (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Deploy a job for a specified project based on a provided image.
    api_response = api_instance.deploy_job(project, image, body=body, schedule=schedule, name=name, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->deploy_job: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **image** | **str**| Image Name | 
 **body** | **object**| Job Configuration | [optional] 
 **schedule** | **str**| Cron Schedule in UNIX format. If specified, the job is executed repeatedly according to the cron definition. A job cannot run more often than once a minute. | [optional] 
 **name** | **str**| Job Name | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabJobResponse**](LabJobResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deploy_model**
> LabServiceResponse deploy_model(project, file_key, body=body, name=name, authorization=authorization)

Deploy a model as a service for a specified project.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
file_key = 'file_key_example' # str | Model Key
body = NULL # object | JSON config containing the environment variables to overwrite the default (optional)
name = 'name_example' # str | Service Name (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Deploy a model as a service for a specified project.
    api_response = api_instance.deploy_model(project, file_key, body=body, name=name, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->deploy_model: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **file_key** | **str**| Model Key | 
 **body** | **object**| JSON config containing the environment variables to overwrite the default | [optional] 
 **name** | **str**| Service Name | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabServiceResponse**](LabServiceResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deploy_service**
> LabServiceResponse deploy_service(project, image, body=body, name=name, authorization=authorization)

Deploy a service for a specified project based on a provided image.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
image = 'image_example' # str | Image Name
body = NULL # object | Service Configuration (optional)
name = 'name_example' # str | Service Name (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Deploy a service for a specified project based on a provided image.
    api_response = api_instance.deploy_service(project, image, body=body, name=name, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->deploy_service: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **image** | **str**| Image Name | 
 **body** | **object**| Service Configuration | [optional] 
 **name** | **str**| Service Name | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabServiceResponse**](LabServiceResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **download_file**
> download_file(project, file_key, authorization=authorization)

Download file from remote storage of selected project.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
file_key = 'file_key_example' # str | File Key
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Download file from remote storage of selected project.
    api_instance.download_file(project, file_key, authorization=authorization)
except ApiException as e:
    print("Exception when calling ProjectsApi->download_file: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **file_key** | **str**| File Key | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/octet-stream

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_experiments**
> ListOfLabExperimentsResponse get_experiments(project, authorization=authorization)

Get all experiments of a project with details.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get all experiments of a project with details.
    api_response = api_instance.get_experiments(project, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_experiments: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**ListOfLabExperimentsResponse**](ListOfLabExperimentsResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_file_info**
> LabFileResponse get_file_info(project, file_key, authorization=authorization)

Get info about the specified file.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
file_key = 'file_key_example' # str | File Key
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get info about the specified file.
    api_response = api_instance.get_file_info(project, file_key, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_file_info: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **file_key** | **str**| File Key | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabFileResponse**](LabFileResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_files**
> ListOfLabFilesResponse get_files(project, data_type=data_type, prefix=prefix, aggregate_versions=aggregate_versions, authorization=authorization)

Get all files of a project with details and general statistics filtered by data type and/or prefix.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
data_type = 'data_type_example' # str | Data Type (optional)
prefix = 'prefix_example' # str | File Key Prefix. If data type is provided, will prefix will be applied for datatype, otherwise on full remote storage. (optional)
aggregate_versions = true # bool | Aggregate Versions (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get all files of a project with details and general statistics filtered by data type and/or prefix.
    api_response = api_instance.get_files(project, data_type=data_type, prefix=prefix, aggregate_versions=aggregate_versions, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_files: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **data_type** | **str**| Data Type | [optional] 
 **prefix** | **str**| File Key Prefix. If data type is provided, will prefix will be applied for datatype, otherwise on full remote storage. | [optional] 
 **aggregate_versions** | **bool**| Aggregate Versions | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**ListOfLabFilesResponse**](ListOfLabFilesResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_job**
> LabJobResponse get_job(project, job, authorization=authorization)

Get a specific project job by name or type.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
job = 'job_example' # str | Job Name or Id
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get a specific project job by name or type.
    api_response = api_instance.get_job(project, job, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_job: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **job** | **str**| Job Name or Id | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabJobResponse**](LabJobResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_job_logs**
> StringResponse get_job_logs(project, job, authorization=authorization)

Get the logs for a job.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
job = 'job_example' # str | Job Name or Id
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get the logs for a job.
    api_response = api_instance.get_job_logs(project, job, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_job_logs: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **job** | **str**| Job Name or Id | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StringResponse**](StringResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_jobs**
> ListOfLabJobsResponse get_jobs(project, authorization=authorization)

Get all jobs of a project with details and general statistics.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get all jobs of a project with details and general statistics.
    api_response = api_instance.get_jobs(project, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_jobs: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**ListOfLabJobsResponse**](ListOfLabJobsResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_project**
> LabProjectResponse get_project(project, expand=expand, authorization=authorization)

Get details for the specified project.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
expand = true # bool | Expand Information (files, services, experiments...) (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get details for the specified project.
    api_response = api_instance.get_project(project, expand=expand, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_project: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **expand** | **bool**| Expand Information (files, services, experiments...) | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabProjectResponse**](LabProjectResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_projects**
> ListOfLabProjectsResponse get_projects(authorization=authorization)

Get all available projects with details.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get all available projects with details.
    api_response = api_instance.get_projects(authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_projects: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**ListOfLabProjectsResponse**](ListOfLabProjectsResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_scheduled_jobs**
> ListOfLabScheduledJobsResponse get_scheduled_jobs(project, authorization=authorization)

Get all scheduled jobs of a project.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get all scheduled jobs of a project.
    api_response = api_instance.get_scheduled_jobs(project, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_scheduled_jobs: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**ListOfLabScheduledJobsResponse**](ListOfLabScheduledJobsResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_service**
> LabServiceResponse get_service(project, service, authorization=authorization)

Get a specific project service by name or type.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
service = 'service_example' # str | Service Name or Type
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get a specific project service by name or type.
    api_response = api_instance.get_service(project, service, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_service: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **service** | **str**| Service Name or Type | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabServiceResponse**](LabServiceResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_service_logs**
> StringResponse get_service_logs(project, service, authorization=authorization)

Get the logs for a service.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
service = 'service_example' # str | Service Name or Type
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get the logs for a service.
    api_response = api_instance.get_service_logs(project, service, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_service_logs: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **service** | **str**| Service Name or Type | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StringResponse**](StringResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_services**
> ListOfLabServicesResponse get_services(project, authorization=authorization)

Get all services of a project with details and general statistics.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get all services of a project with details and general statistics.
    api_response = api_instance.get_services(project, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->get_services: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**ListOfLabServicesResponse**](ListOfLabServicesResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **is_project_available**
> StatusMessageFormat is_project_available(project, authorization=authorization)

Checks if a project name is available for project creation .



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Checks if a project name is available for project creation .
    api_response = api_instance.is_project_available(project, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->is_project_available: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StatusMessageFormat**](StatusMessageFormat.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **sync_experiment**
> StringResponse sync_experiment(body, project, authorization=authorization)

Sync an experiment to the experiments DB of a project.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
body = lab_api.swagger_client.LabExperiment() # LabExperiment | Experiment
project = 'project_example' # str | Project Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Sync an experiment to the experiments DB of a project.
    api_response = api_instance.sync_experiment(body, project, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->sync_experiment: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**LabExperiment**](LabExperiment.md)| Experiment | 
 **project** | **str**| Project Name | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StringResponse**](StringResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **upload_file**
> StringResponse upload_file(project, data_type, file, file_name=file_name, versioning=versioning, authorization=authorization)

Upload file to remote storage of selected project and returns key.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.ProjectsApi()
project = 'project_example' # str | Project Name
data_type = 'data_type_example' # str | Data Type of File.
file = '/path/to/file.txt' # file | 
file_name = 'file_name_example' # str | File Name. If not provided, the filename from file metadata will be used. (optional)
versioning = true # bool | Versioning activated (optional) (default to true)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Upload file to remote storage of selected project and returns key.
    api_response = api_instance.upload_file(project, data_type, file, file_name=file_name, versioning=versioning, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ProjectsApi->upload_file: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **project** | **str**| Project Name | 
 **data_type** | **str**| Data Type of File. | 
 **file** | **file**|  | 
 **file_name** | **str**| File Name. If not provided, the filename from file metadata will be used. | [optional] 
 **versioning** | **bool**| Versioning activated | [optional] [default to true]
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StringResponse**](StringResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

