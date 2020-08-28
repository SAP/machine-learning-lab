# lab_api.swagger_client.AuthorizationApi

All URIs are relative to *https://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**add_user_to_project**](AuthorizationApi.md#add_user_to_project) | **GET** /api/auth/users/{user}/projects/{project} | Add a user to a project. Return new token.
[**create_api_token**](AuthorizationApi.md#create_api_token) | **GET** /api/auth/users/{user}/token | Get a long-term API token for given user.
[**create_user**](AuthorizationApi.md#create_user) | **POST** /api/auth/users | Create user profile.
[**deactivate_users**](AuthorizationApi.md#deactivate_users) | **POST** /api/auth/users/deactivate | Deactivate a list of users. This will overwrite all deactivated users (admin only).
[**delete_user**](AuthorizationApi.md#delete_user) | **DELETE** /api/auth/users/{user} | Delete a user (admin only).
[**get_me**](AuthorizationApi.md#get_me) | **GET** /api/auth/users/me | Get the user profile of the current user.
[**get_user**](AuthorizationApi.md#get_user) | **GET** /api/auth/users/{user} | Get the profile a user has access to.
[**get_users**](AuthorizationApi.md#get_users) | **GET** /api/auth/users | Get all profiles stored in the database (admin only).
[**login_user**](AuthorizationApi.md#login_user) | **GET** /api/auth/login | Login with basic auth and get short-term application token (JWT).
[**logout_user**](AuthorizationApi.md#logout_user) | **GET** /api/auth/logout | Log the user out by setting the auth cookie to a time in the past
[**refresh_token**](AuthorizationApi.md#refresh_token) | **GET** /api/auth/refresh | Get a new short-term application token (JWT).
[**remove_user_from_project**](AuthorizationApi.md#remove_user_from_project) | **DELETE** /api/auth/users/{user}/projects/{project} | Remove a user from a project. Return new token.
[**update_permissions**](AuthorizationApi.md#update_permissions) | **POST** /api/auth/users/{user}/permissions | Update permissions of a user (admin only). Return new token.
[**update_user_password**](AuthorizationApi.md#update_user_password) | **GET** /api/auth/users/{user}/password | Update user password. Return new token.


# **add_user_to_project**
> StringResponse add_user_to_project(user, project, authorization=authorization)

Add a user to a project. Return new token.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
user = 'user_example' # str | User Name
project = 'project_example' # str | Project Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Add a user to a project. Return new token.
    api_response = api_instance.add_user_to_project(user, project, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->add_user_to_project: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **str**| User Name | 
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

# **create_api_token**
> StringResponse create_api_token(user, authorization=authorization)

Get a long-term API token for given user.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
user = 'user_example' # str | User Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get a long-term API token for given user.
    api_response = api_instance.create_api_token(user, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->create_api_token: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **str**| User Name | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StringResponse**](StringResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **create_user**
> LabUserResponse create_user(user, password, admin=admin, jwt_secret=jwt_secret, authorization=authorization)

Create user profile.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
user = 'user_example' # str | Id/username of the profile.
password = 'password_example' # str | Password of the profile.
admin = true # bool | Create the user with Admin permissions. (optional)
jwt_secret = 'jwt_secret_example' # str | JWT Secret. If passed and matches the server's secret, the account will be created with admin credentials. (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Create user profile.
    api_response = api_instance.create_user(user, password, admin=admin, jwt_secret=jwt_secret, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->create_user: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **str**| Id/username of the profile. | 
 **password** | **str**| Password of the profile. | 
 **admin** | **bool**| Create the user with Admin permissions. | [optional] 
 **jwt_secret** | **str**| JWT Secret. If passed and matches the server&#39;s secret, the account will be created with admin credentials. | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabUserResponse**](LabUserResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deactivate_users**
> StatusMessageFormat deactivate_users(body, authorization=authorization)

Deactivate a list of users. This will overwrite all deactivated users (admin only).



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
body = [lab_api.swagger_client.list[str]()] # list[str] | List of users to set deactivated.
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Deactivate a list of users. This will overwrite all deactivated users (admin only).
    api_response = api_instance.deactivate_users(body, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->deactivate_users: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | **list[str]**| List of users to set deactivated. | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StatusMessageFormat**](StatusMessageFormat.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete_user**
> StatusMessageFormat delete_user(user, authorization=authorization)

Delete a user (admin only).



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
user = 'user_example' # str | User Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Delete a user (admin only).
    api_response = api_instance.delete_user(user, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->delete_user: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **str**| User Name | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StatusMessageFormat**](StatusMessageFormat.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_me**
> LabUserResponse get_me(authorization=authorization)

Get the user profile of the current user.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get the user profile of the current user.
    api_response = api_instance.get_me(authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->get_me: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabUserResponse**](LabUserResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_user**
> LabUserResponse get_user(user, authorization=authorization)

Get the profile a user has access to.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
user = 'user_example' # str | User Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get the profile a user has access to.
    api_response = api_instance.get_user(user, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->get_user: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **str**| User Name | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**LabUserResponse**](LabUserResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_users**
> ListOfLabUsersResponse get_users(authorization=authorization)

Get all profiles stored in the database (admin only).



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get all profiles stored in the database (admin only).
    api_response = api_instance.get_users(authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->get_users: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**ListOfLabUsersResponse**](ListOfLabUsersResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **login_user**
> StringResponse login_user(authorization=authorization)

Login with basic auth and get short-term application token (JWT).



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Login with basic auth and get short-term application token (JWT).
    api_response = api_instance.login_user(authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->login_user: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StringResponse**](StringResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **logout_user**
> StringResponse logout_user(authorization=authorization)

Log the user out by setting the auth cookie to a time in the past



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Log the user out by setting the auth cookie to a time in the past
    api_response = api_instance.logout_user(authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->logout_user: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StringResponse**](StringResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **refresh_token**
> StringResponse refresh_token(authorization=authorization)

Get a new short-term application token (JWT).



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Get a new short-term application token (JWT).
    api_response = api_instance.refresh_token(authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->refresh_token: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StringResponse**](StringResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: text/plain

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **remove_user_from_project**
> StringResponse remove_user_from_project(user, project, authorization=authorization)

Remove a user from a project. Return new token.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
user = 'user_example' # str | User Name
project = 'project_example' # str | Project Name
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Remove a user from a project. Return new token.
    api_response = api_instance.remove_user_from_project(user, project, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->remove_user_from_project: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **str**| User Name | 
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

# **update_permissions**
> StringResponse update_permissions(user, body, deactivate_token=deactivate_token, authorization=authorization)

Update permissions of a user (admin only). Return new token.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
user = 'user_example' # str | User Name
body = [lab_api.swagger_client.list[str]()] # list[str] | Permission List
deactivate_token = true # bool | If true, the user is forced to re-login. (optional)
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Update permissions of a user (admin only). Return new token.
    api_response = api_instance.update_permissions(user, body, deactivate_token=deactivate_token, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->update_permissions: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **str**| User Name | 
 **body** | **list[str]**| Permission List | 
 **deactivate_token** | **bool**| If true, the user is forced to re-login. | [optional] 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StringResponse**](StringResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **update_user_password**
> StringResponse update_user_password(user, password, authorization=authorization)

Update user password. Return new token.



### Example
```python
from __future__ import print_function
import time
import lab_api.swagger_client
from lab_api.swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = lab_api.swagger_client.AuthorizationApi()
user = 'user_example' # str | User Name
password = 'password_example' # str | New Password
authorization = 'authorization_example' # str | Authorization Token (optional)

try:
    # Update user password. Return new token.
    api_response = api_instance.update_user_password(user, password, authorization=authorization)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling AuthorizationApi->update_user_password: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **str**| User Name | 
 **password** | **str**| New Password | 
 **authorization** | **str**| Authorization Token | [optional] 

### Return type

[**StringResponse**](StringResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

