"""
sunshine.conf parser and manager.

This module handles reading and writing the main Sunshine configuration file.
Format: key = value
"""

import os
from typing import List, Optional


class SunshineConf:
    """Represents the sunshine.conf configuration."""

    def __init__(self):
        # Server settings
        self.address: Optional[str] = None
        self.port: Optional[str] = None
        self.origin_web_ui_allowed: Optional[str] = None

        # Video settings
        self.encoder: Optional[str] = None
        self.resolutions: Optional[List[str]] = None
        self.fps: Optional[List[str]] = None
        self.capture: Optional[str] = None

        # Audio settings
        self.audio_sink: Optional[str] = None

        # General settings
        self.sunshine_name: Optional[str] = None
        self.min_log_level: Optional[str] = None
        self.external_ip: Optional[str] = None
        self.keybindings: Optional[str] = None
        self.enable_web_ui: Optional[str] = None

        # File path (stored but not saved to file)
        self.file_path: Optional[str] = None

    @classmethod
    def load(cls, file_path: str) -> 'SunshineConf':
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
    def _apply_config(config: 'SunshineConf', key: str, value: str) -> None:
        """Apply a key-value pair to the config object."""
        if key == "address":
            config.address = value
        elif key == "port":
            config.port = value
        elif key == "origin_web_ui_allowed":
            config.origin_web_ui_allowed = value
        elif key == "encoder":
            config.encoder = value
        elif key == "resolutions":
            config.resolutions = value.split(',') if value else []
        elif key == "fps":
            config.fps = value.split(',') if value else []
        elif key == "capture":
            config.capture = value
        elif key == "audio_sink":
            config.audio_sink = value
        elif key == "sunshine_name":
            config.sunshine_name = value
        elif key == "min_log_level":
            config.min_log_level = value
        elif key == "external_ip":
            config.external_ip = value
        elif key == "keybindings":
            config.keybindings = value
        elif key == "enable_web_ui":
            config.enable_web_ui = value

    @staticmethod
    def _parse_list(value: str) -> List[str]:
        """Parse a comma-separated list."""
        if not value:
            return []
        parts = value.split(',')
        return [p.strip() for p in parts if p.strip()]

    @staticmethod
    def _join_list(items: List[str]) -> str:
        """Join a list to a comma-separated string."""
        return ','.join(items)

    def save(self, file_path: Optional[str] = None) -> bool:
        """Save configuration to a file."""
        path = file_path or self.file_path
        if not path:
            return False

        try:
            # Ensure directory exists
            os.makedirs(os.path.dirname(path), exist_ok=True)

            with open(path, 'w', encoding='utf-8') as f:
                f.write("# Sunshine Configuration File\n")
                f.write("# This is a default configuration file for Sunshine\n")
                f.write("\n")

                f.write("# Server Settings\n")
                f.write(f"address = {self.address}\n")
                f.write(f"port = {self.port}\n")
                f.write(f"origin_web_ui_allowed = {self.origin_web_ui_allowed}\n")
                f.write("\n")

                f.write("# Video Settings\n")
                f.write("# Video encoder (nvenc, amdvce, software)\n")
                f.write(f"encoder = {self.encoder}\n")
                f.write("\n")

                f.write("# Audio Settings\n")
                f.write("# Audio sink (leave blank to auto-detect)\n")
                f.write(f"audio_sink = {self.audio_sink}\n")
                f.write("\n")

                f.write("# Sunshine Name\n")
                f.write("# The name that will appear in Moonlight\n")
                f.write(f"sunshine_name = {self.sunshine_name}\n")
                f.write("\n")

                f.write("# Log Level (verbose, debug, info, warning, error, fatal)\n")
                f.write(f"min_log_level = {self.min_log_level}\n")
                f.write("\n")

                f.write("# Keybindings\n")
                f.write("# Map keyboard shortcuts\n")
                f.write("# keybindings = 0x10=0xA2 (Example: Shift -> Left Alt)\n")
                f.write(f"keybindings = {self.keybindings}\n")
                f.write("\n")

                f.write("# Resolutions\n")
                f.write("# Supported resolutions for clients\n")
                f.write(f"resolutions = {self._join_list(self.resolutions)}\n")
                f.write("\n")

                f.write("# FPS\n")
                f.write("# Supported frame rates\n")
                f.write(f"fps = {self._join_list(self.fps)}\n")
                f.write("\n")

                f.write("# Capture Settings\n")
                f.write("# Screen capture method (dddupe, wgc, ddxgi)\n")
                f.write(f"capture = {self.capture}\n")
                f.write("\n")

                f.write("# External IP\n")
                f.write("# Leave blank for auto-detection\n")
                f.write(f"external_ip = {self.external_ip}\n")
                f.write("\n")

                f.write("# Web UI Settings\n")
                f.write("# Enable or disable the Web UI (configuration interface)\n")
                f.write("# Set to false to completely disable Web UI access\n")
                f.write(f"enable_web_ui = {self.enable_web_ui}\n")

            return True
        except Exception:
            return False

    def update_from(self, other: 'SunshineConf') -> None:
        """Update config from another SunshineConf instance."""
        self.address = other.address
        self.port = other.port
        self.origin_web_ui_allowed = other.origin_web_ui_allowed
        self.encoder = other.encoder
        self.resolutions = other.resolutions.copy()
        self.fps = other.fps.copy()
        self.capture = other.capture
        self.audio_sink = other.audio_sink
        self.sunshine_name = other.sunshine_name
        self.min_log_level = other.min_log_level
        self.external_ip = other.external_ip
        self.keybindings = other.keybindings
        self.enable_web_ui = other.enable_web_ui
        if other.file_path:
            self.file_path = other.file_path

