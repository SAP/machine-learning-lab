# lab_api.swagger_client.AdministrationApi

All URIs are relative to *https://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**check_workspace**](AdministrationApi.md#check_workspace) | **GET** /api/admin/workspace/check | Checks whether a workspace container for the passed id already exists. If not, a new one is created &amp; started.
[**get_events**](AdministrationApi.md#get_events) | **GET** /api/admin/events | Returns events filtered by a specified event type (admin-only).
[**get_lab_info**](AdministrationApi.md#get_lab_info) | **GET** /api/admin/info | Returns information about this Lab instance.
[**get_statistics**](AdministrationApi.md#get_statistics) | **GET** /api/admin/statistics | Returns statistics about this Lab instance (admin-only).
[**reset_all_workspaces**](AdministrationApi.md#reset_all_workspaces) | **PUT** /api/admin/workspace/reset-all | Resets all workspaces. Use with caution (admin-only).
[**reset_workspace**](AdministrationApi.md#reset_workspace) | **GET** /api/admin/workspace/reset | Resets a workspace. Removes the container (keeps all persisted data) and starts a new one.
[**shutdown_disk_exceeding_containers**](AdministrationApi.md#shutdown_disk_exceeding_containers) | **PUT** /api/admin/workspace/shutdown-disk-exceeding | Remove all workspaces that exceed the disk storage limit (docker-local mode only).
[**shutdown_unused_workspaces**](AdministrationApi.md#shutdown_unused_workspaces) | **PUT** /api/admin/workspace/shutdown-unused | Shutdown all unused workspaces - 15 days without activity (admin-only).


# **check_workspace**
> StatusMessageFormat check_workspace(id=id, authorization=authorization)

Checks whether a workspace container for the passed id already exists. If not, a new one is created & started.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AdministrationApi()
id = 'id_example' # str |  (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Checks whether a workspace container for the passed id already exists. If not, a new one is created & started.
    api_response = api_instance.check_workspace(id=id, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AdministrationApi->check_workspace: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **str**|  | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StatusMessageFormat**](StatusMessageFormat.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_events**
> ListOfLabEventsResponse get_events(event=event, authorization=authorization)

Returns events filtered by a specified event type (admin-only).



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AdministrationApi()
event = 'event_example' # str | Event Type. If not provided, all events will be returned. (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Returns events filtered by a specified event type (admin-only).
    api_response = api_instance.get_events(event=event, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AdministrationApi->get_events: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **event** | **str**| Event Type. If not provided, all events will be returned. | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**ListOfLabEventsResponse**](ListOfLabEventsResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_lab_info**
> LabInfoResponse get_lab_info(authorization=authorization)

Returns information about this Lab instance.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AdministrationApi()
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Returns information about this Lab instance.
    api_response = api_instance.get_lab_info(authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AdministrationApi->get_lab_info: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabInfoResponse**](LabInfoResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_statistics**
> LabStatisticsResponse get_statistics(authorization=authorization)

Returns statistics about this Lab instance (admin-only).



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AdministrationApi()
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Returns statistics about this Lab instance (admin-only).
    api_response = api_instance.get_statistics(authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AdministrationApi->get_statistics: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabStatisticsResponse**](LabStatisticsResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **reset_all_workspaces**
> StatusMessageFormat reset_all_workspaces(authorization=authorization)

Resets all workspaces. Use with caution (admin-only).



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AdministrationApi()
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Resets all workspaces. Use with caution (admin-only).
    api_response = api_instance.reset_all_workspaces(authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AdministrationApi->reset_all_workspaces: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StatusMessageFormat**](StatusMessageFormat.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **reset_workspace**
> LabServiceResponse reset_workspace(id=id, authorization=authorization)

Resets a workspace. Removes the container (keeps all persisted data) and starts a new one.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AdministrationApi()
id = 'id_example' # str |  (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Resets a workspace. Removes the container (keeps all persisted data) and starts a new one.
    api_response = api_instance.reset_workspace(id=id, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AdministrationApi->reset_workspace: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **str**|  | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabServiceResponse**](LabServiceResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **shutdown_disk_exceeding_containers**
> ListOfStringsResponse shutdown_disk_exceeding_containers(dryrun, authorization=authorization)

Remove all workspaces that exceed the disk storage limit (docker-local mode only).



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AdministrationApi()
dryrun = true # bool | If 'true', it will only return candidates for removal. (default to true)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Remove all workspaces that exceed the disk storage limit (docker-local mode only).
    api_response = api_instance.shutdown_disk_exceeding_containers(dryrun, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AdministrationApi->shutdown_disk_exceeding_containers: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **dryrun** | **bool**| If &#39;true&#39;, it will only return candidates for removal. | [default to true]
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**ListOfStringsResponse**](ListOfStringsResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **shutdown_unused_workspaces**
> ListOfLabUsers shutdown_unused_workspaces(dryrun, threshold=threshold, body=body, authorization=authorization)

Shutdown all unused workspaces - 15 days without activity (admin-only).



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AdministrationApi()
dryrun = true # bool | If 'true', it will only return candidates for shutdown. (default to true)
threshold = 14 # int | Number of inactive days to consider workspace unused. (optional) (default to 14)
body = [lab_api.swagger_client.list[str]()] # list[str] | IDs to include as inactive users. (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Shutdown all unused workspaces - 15 days without activity (admin-only).
    api_response = api_instance.shutdown_unused_workspaces(dryrun, threshold=threshold, body=body, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AdministrationApi->shutdown_unused_workspaces: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **dryrun** | **bool**| If &#39;true&#39;, it will only return candidates for shutdown. | [default to true]
 **threshold** | **int**| Number of inactive days to consider workspace unused. | [optional] [default to 14]
 **body** | **list[str]**| IDs to include as inactive users. | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**ListOfLabUsers**](ListOfLabUsers.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

