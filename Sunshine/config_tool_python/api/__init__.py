"""
API module for Sunshine Config Tool.

This module handles API calls to the backend server.
"""

import json
import ssl
import urllib.request
import urllib.error
from typing import Optional, Dict, Any


# Error code translation map
ERROR_TRANSLATIONS = {
    # Common errors
    "Common.InvalidParam": "参数不合法",
    "Common.InternalError": "服务器内部错误",

    # User errors
    "User.Exists": "用户已存在",
    "User.BuiltIn": "该用户为内置用户，不允许注册",
    "User.NotExist": "用户不存在",
    "User.NoPermission": "用户无权操作此设备",
    "User.PasswordIsWrong": "密码错误",
    "User.Disable": "用户已被禁用",
    "User.CreateFailed": "用户创建失败",

    # Device errors
    "Device.NotExist": "设备不存在",
    "Device.NotOnline": "设备不在线",
    "Device.CreateFailed": "设备创建失败",

    # Bind errors
    "Bind.CreateFailed": "绑定关系创建失败",
    "Bind.UpdateFailed": "绑定关系更新失败",
}


def translate_error(msg: str) -> str:
    """Translate error code to user-friendly Chinese message."""
    return ERROR_TRANSLATIONS.get(msg, msg)


class ApiClient:
    """API client for communicating with the backend server."""

    def __init__(self, base_url: str, verify_ssl: bool = False):
        """
        Initialize the API client.

        Args:
            base_url: The base URL of the backend server (e.g., http://localhost:8080)
            verify_ssl: Whether to verify SSL certificates (default: False for development)
        """
        self.base_url = base_url.rstrip('/')
        self.verify_ssl = verify_ssl

        # Create SSL context that skips certificate verification
        if not verify_ssl:
            self.ssl_context = ssl.create_default_context()
            self.ssl_context.check_hostname = False
            self.ssl_context.verify_mode = ssl.CERT_NONE
        else:
            self.ssl_context = None

    def _make_request(self, method: str, endpoint: str, data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """
        Make an HTTP request to the backend server.

        Args:
            method: HTTP method (GET, POST, etc.)
            endpoint: API endpoint path
            data: Optional data to send in the request body

        Returns:
            Dictionary containing the response data

        Raises:
            Exception: If the request fails
        """
        url = f"{self.base_url}{endpoint}"

        headers = {
            'Content-Type': 'application/json'
        }

        json_data = json.dumps(data).encode('utf-8') if data else None

        req = urllib.request.Request(url, data=json_data, headers=headers, method=method)

        try:
            # Use SSL context if provided (for skipping certificate verification)
            if self.ssl_context:
                with urllib.request.urlopen(req, timeout=30, context=self.ssl_context) as response:
                    response_data = response.read().decode('utf-8')
                    return json.loads(response_data)
            else:
                with urllib.request.urlopen(req, timeout=30) as response:
                    response_data = response.read().decode('utf-8')
                    return json.loads(response_data)
        except urllib.error.HTTPError as e:
            error_body = e.read().decode('utf-8')
            try:
                error_data = json.loads(error_body)
                msg = error_data.get('msg', error_data.get('message', 'Unknown error'))
                raise Exception(msg)
            except:
                raise Exception(f"HTTP Error {e.code}: {error_body}")
        except urllib.error.URLError as e:
            raise Exception(f"Connection failed: {e.reason}")
        except Exception as e:
            raise Exception(f"Request failed: {str(e)}")

    def sunshine_login(self, username: str, password: str) -> Dict[str, Any]:
        """
        Sunshine user login.

        Args:
            username: Username
            password: Password

        Returns:
            Dictionary containing user info and tokens
        """
        data = {
            'username': username,
            'password': password
        }
        return self._make_request('POST', '/api/stream/v1/sunshine/login', data)

    def sunshine_register(self, username: str, password: str) -> Dict[str, Any]:
        """
        Sunshine user registration.

        Args:
            username: Username
            password: Password

        Returns:
            Dictionary containing the created user info
        """
        data = {
            'username': username,
            'password': password
        }
        return self._make_request('POST', '/api/stream/v1/sunshine/register', data)

    def get_device_status(self, device_id: str) -> Dict[str, Any]:
        """
        Get device status by device ID.

        Args:
            device_id: The 8-digit device ID

        Returns:
            Dictionary containing device info (ip, pin, status, etc.)
        """
        return self._make_request('GET', f'/api/stream/v1/devices/{device_id}/status')

    def register_and_bind(self, username: str, password: str, device_id: str) -> Dict[str, Any]:
        """
        Register a new user and bind to a device.

        Args:
            username: Username for the new user
            password: Password for the new user
            device_id: The 8-digit device ID to bind

        Returns:
            Dictionary containing user and device info
        """
        data = {
            'username': username,
            'password': password,
            'deviceId': device_id
        }
        return self._make_request('POST', '/api/stream/v1/register_and_bind', data)

    def bind_existing_user(self, username: str, password: str, device_id: str) -> Dict[str, Any]:
        """
        Bind an existing user to a device.

        This method validates user credentials and then binds the user to the specified device
        using the dedicated bind_user endpoint.

        Args:
            username: Existing username
            password: User password
            device_id: The 8-digit device ID to bind

        Returns:
            Dictionary containing user and device info (includes 'userExisted' field)
        """
        data = {
            'username': username,
            'password': password,
            'deviceId': device_id
        }
        return self._make_request('POST', '/api/stream/v1/bind_user', data)

    def change_password_by_device(self, username: str, new_password: str, device_id: str) -> Dict[str, Any]:
        """
        Change user password by device binding validation (no auth required).

        Args:
            username: Username whose password to change
            new_password: New password (at least 4 characters)
            device_id: Device ID to validate binding relationship

        Returns:
            Dictionary with code=0 on success, code=2 on failure
        """
        data = {
            'username': username,
            'newpassword': new_password,
            'device_id': device_id
        }
        return self._make_request('POST', '/api/stream/v1/change_password_by_device', data)


# Default API client instance (will be configured in main.py)
_api_client: Optional[ApiClient] = None


def get_api_client() -> Optional[ApiClient]:
    """Get the current API client instance."""
    return _api_client


def set_api_client(client: ApiClient) -> None:
    """Set the API client instance."""
    global _api_client
    _api_client = client

