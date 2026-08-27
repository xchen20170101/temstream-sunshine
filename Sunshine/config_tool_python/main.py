#!/usr/bin/env python3
"""
Sunshine Config Tool - Python Edition

A configuration management tool for Sunshine streaming server,
built with Python and Tkinter.

Features:
- Edit sunshine.conf and sunshine_server.ini configuration files
- Backend server connection and PIN display
- User registration and device binding

Usage:
    python main.py

Requirements:
    - Python 3.7+
    - Tkinter (included with standard Python installation)
"""

import tkinter as tk
from tkinter import messagebox
import sys
import os

# Add the current directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from ui.main_window import MainWindow
from config.sunshine_conf import SunshineConf
from config.sunshine_server_ini import SunshineServerIni
from api import ApiClient, set_api_client


def main():
    """Main entry point."""
    # Get current working directory (where exe/script is run from)
    current_dir = os.getcwd()

    # Config directory (当前目录下的config文件夹)
    config_dir = os.path.join(current_dir, "config")

    # Default config file paths
    conf_path = os.path.join(config_dir, "sunshine.conf")
    ini_path = os.path.join(config_dir, "sunshine_server.ini")

    # Check if config files exist
    missing_files = []
    if not os.path.exists(conf_path):
        missing_files.append(f"config/sunshine.conf")
    if not os.path.exists(ini_path):
        missing_files.append(f"config/sunshine_server.ini")

    # Create API client - read from sunshine_server.ini
    api_url = None
    error_msg = None

    if not os.path.exists(ini_path):
        # If ini doesn't exist, use defaults
        api_url = "http://localhost:8090"
    else:
        try:
            server_ini = SunshineServerIni.load(ini_path)

            # Build backend URL from server_ip (HTTP API port is fixed at 8090)
            server_ip = server_ini.server_ip if server_ini.server_ip else "localhost"

            # HTTP API URL - port 8090 is hardcoded
            api_url = f"http://{server_ip}:8090/"

        except Exception as e:
            # Use default if loading fails
            api_url = "http://localhost:8090/"

    # Show warning if files were missing
    if missing_files:
        warning_msg = f"配置文件未找到:\n\n{chr(10).join(missing_files)}\n\n配置界面将使用默认值加载。"
        # Defer the message box to after mainloop starts
        root_temp = tk.Tk()
        root_temp.withdraw()
        root_temp.after(100, lambda: messagebox.showwarning("警告", warning_msg))
        root_temp.after(100, root_temp.destroy)

    # Create API client
    api_client = ApiClient(api_url)

    # Set API client globally
    set_api_client(api_client)

    # Create root window
    root = tk.Tk()

    # Show main window
    window = MainWindow(root)

    # Pass API client to main window
    window.set_api_client(api_client)

    # Set file paths for saving
    window.conf_file_path = conf_path
    window.server_ini_path = ini_path

    # Try to load configuration files
    try:
        conf = SunshineConf.load(conf_path)
    except (FileNotFoundError, IOError):
        conf = SunshineConf()
        conf.resolutions = []
        conf.fps = []

    try:
        server_ini = SunshineServerIni.load(ini_path)
    except (FileNotFoundError, IOError):
        server_ini = SunshineServerIni()

    # Set configurations
    window.set_config(conf, server_ini)

    # Center window on screen
    root.update_idletasks()
    screen_width = root.winfo_screenwidth()
    screen_height = root.winfo_screenheight()
    window_width = root.winfo_width()
    window_height = root.winfo_height()
    x = (screen_width - window_width) // 2
    y = (screen_height - window_height) // 2
    root.geometry(f"+{x}+{y}")

    # Start main loop
    root.mainloop()


if __name__ == "__main__":
    main()
