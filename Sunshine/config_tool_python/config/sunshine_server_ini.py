"""
sunshine_server.ini parser and manager.

This module handles reading and writing the Sunshine server configuration file.
Format: key = value
"""

import os
from typing import Optional


class SunshineServerIni:
    """Represents the sunshine_server.ini configuration."""

    def __init__(self):
        # Management system
        self.server_ip: Optional[str] = None
        self.server_port: Optional[int] = 12345
        self.device_id: Optional[str] = None

        # Tailscale settings
        self.is_tailscale: Optional[bool] = None
        self.tailscale_authkey: Optional[str] = None
        self.headscale_url: Optional[str] = None

        # LAN mode settings
        self.lan_fixed_pin: Optional[bool] = False
        self.lan_password: Optional[str] = None

        # Device password
        self.device_password: Optional[str] = None

        # Current logged in user
        self.login_user: Optional[str] = None

        # File path (stored but not saved to file)
        self.file_path: Optional[str] = None

    @classmethod
    def load(cls, file_path: str) -> 'SunshineServerIni':
        """Load configuration from a file."""
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"Configuration file not found: {file_path}")

        config = cls()
        config.file_path = file_path

        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith('#'):
                        continue

                    if '=' not in line:
                        continue

                    parts = line.split('=', 1)
                    if len(parts) != 2:
                        continue

                    key = parts[0].strip()
                    value = parts[1].strip()

                    cls._apply_config(config, key, value)
        except Exception as e:
            raise IOError(f"Failed to read configuration file: {e}")

        return config

        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith('#'):
                        continue

                    if '=' not in line:
                        continue

                    parts = line.split('=', 1)
                    if len(parts) != 2:
                        continue

                    key = parts[0].strip()
                    value = parts[1].strip()

                    cls._apply_config(config, key, value)
        except Exception:
            pass

        return config

    @staticmethod
    def _apply_config(config: 'SunshineServerIni', key: str, value: str) -> None:
        """Apply a key-value pair to the config object."""
        if key == "server_ip":
            config.server_ip = value
        elif key == "server_port":
            try:
                config.server_port = int(value) if value else 12345
            except ValueError:
                config.server_port = 12345
        elif key == "device_id":
            config.device_id = value
        elif key == "isTailscale":
            config.is_tailscale = value.lower() == "true"
        elif key == "tailscale_authkey":
            config.tailscale_authkey = value
        elif key == "headscale_url":
            config.headscale_url = value
        elif key == "lan_fixed_pin":
            config.lan_fixed_pin = value.lower() == "true"
        elif key == "lan_password":
            config.lan_password = value if value else None
        elif key == "device_password":
            config.device_password = value if value else None
        elif key == "login_user":
            config.login_user = value

    @staticmethod
    def _bool_to_string(value: bool) -> str:
        """Convert boolean to string."""
        return "true" if value else "false"

    def save(self, file_path: Optional[str] = None) -> bool:
        """Save configuration to a file."""
        path = file_path or self.file_path
        if not path:
            return False

        try:
            # Ensure directory exists
            os.makedirs(os.path.dirname(path), exist_ok=True)

            with open(path, 'w', encoding='utf-8') as f:
                f.write("# Sunshine Server Configuration\n")
                f.write("# Server IP address for management system communication\n")
                f.write(f"server_ip = {self.server_ip or ''}\n")
                f.write(f"server_port = {self.server_port or 12345}\n")
                f.write(f"device_id = {self.device_id or ''}\n")
                f.write("\n")

                f.write("# Enable Tailscale IP usage\n")
                f.write(f"isTailscale = {self._bool_to_string(self.is_tailscale)}\n")
                f.write("\n")

                f.write("# Tailscale auto-login configuration\n")
                f.write(f"tailscale_authkey = {self.tailscale_authkey or ''}\n")
                f.write(f"headscale_url = {self.headscale_url or ''}\n")
                f.write("\n")

                f.write("# LAN Mode Settings\n")
                f.write("# Enable to allow clients on the same network to connect with fixed PIN\n")
                f.write(f"lan_fixed_pin = {self._bool_to_string(self.lan_fixed_pin)}\n")
                f.write("# Password for LAN mode authentication (required when LAN mode is enabled)\n")
                f.write(f"lan_password = {self.lan_password or ''}\n")
                f.write("\n")

                f.write("# Device Password\n")
                f.write(f"device_password = {self.device_password or ''}\n")
                f.write("\n")

                f.write("# Current logged in user\n")
                f.write(f"login_user = {self.login_user or ''}\n")

            return True
        except Exception:
            return False

    def update_from(self, other: 'SunshineServerIni') -> None:
        """Update config from another SunshineServerIni instance."""
        self.server_ip = other.server_ip
        self.server_port = other.server_port
        self.device_id = other.device_id
        self.is_tailscale = other.is_tailscale
        self.tailscale_authkey = other.tailscale_authkey
        self.headscale_url = other.headscale_url
        self.lan_fixed_pin = other.lan_fixed_pin
        self.lan_password = other.lan_password
        self.device_password = other.device_password
        self.login_user = other.login_user
        if other.file_path:
            self.file_path = other.file_path

