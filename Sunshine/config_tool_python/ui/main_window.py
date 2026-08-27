"""
Main window module with modern sidebar navigation.

Features:
- Dark sidebar with icon navigation
- Light content area
- Smooth transitions between tabs
"""

import tkinter as tk
from tkinter import messagebox
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from ui.theme import (
    PRIMARY, PRIMARY_HOVER, PRIMARY_LIGHT,
    DARK_BG, DARK_SECONDARY, DARK_CARD, DARK_BORDER, DARK_TEXT, DARK_TEXT_SECONDARY,
    LIGHT_BG, LIGHT_SECONDARY, LIGHT_BORDER, LIGHT_TEXT, LIGHT_TEXT_SECONDARY,
    SIDEBAR_WIDTH, SIDEBAR_ACTIVE_BG, FONT_FAMILY, FONT_SIZE_SM, FONT_SIZE_MD, FONT_SIZE_LG, FONT_SIZE_XL, FONT_SIZE_XXL,
    RADIUS_MD, RADIUS_LG, PADDING_MD, PADDING_LG, PADDING_XL,
    MARGIN_SM, MARGIN_MD, MARGIN_LG, ANIM_FAST, STATUS_ONLINE, STATUS_OFFLINE, STATUS_PENDING
)
from config.sunshine_conf import SunshineConf
from config.sunshine_server_ini import SunshineServerIni
from ui.tabs.server_config_tab import ServerConfigTab
from ui.tabs.pin_display_tab import PinDisplayTab
from ui.tabs.user_registration_tab import UserRegistrationTab


class ModernButton(tk.Canvas):
    """Custom rounded button widget."""

    def __init__(self, parent, text, command, width=140, height=40,
                 bg_color=PRIMARY, hover_color=PRIMARY_HOVER, text_color="white",
                 icon=None, **kwargs):
        super().__init__(parent, width=width, height=height,
                        bg=parent.cget('bg'), highlightthickness=0, **kwargs)
        self.command = command
        self.bg_color = bg_color
        self.hover_color = hover_color
        self.text_color = text_color
        self.icon = icon
        self.current_color = bg_color
        self.hovered = False
        self.disabled = False
        self.disabled_color = "#6c757d"

        self._draw()

        self.bind("<Enter>", self._on_enter)
        self.bind("<Leave>", self._on_leave)
        self.bind("<Button-1>", self._on_click)

    def _draw(self):
        """Draw the rounded button."""
        self.delete("all")
        w, h = self.winfo_width(), self.winfo_height()
        if w < 2 or h < 2:
            w, h = 140, 40

        color = self.disabled_color if self.disabled else self.current_color

        # Draw rounded rectangle
        r = 8
        self.create_rounded_rect(0, 0, w, h, r, fill=color, outline="")

        # Draw text
        text_x = w // 2
        text_y = h // 2
        display_text = self.icon + " " + text if self.icon else text
        self.create_text(text_x, text_y, text=display_text,
                        fill=self.text_color, font=(FONT_FAMILY, FONT_SIZE_MD, "bold"),
                        anchor="center")

    def create_rounded_rect(self, x1, y1, x2, y2, radius, **kwargs):
        """Create a rounded rectangle shape."""
        fill = kwargs.get('fill', '')
        points = []
        r = min(radius, (x2 - x1) // 2, (y2 - y1) // 2)

        points.extend([x1 + r, y1, x2 - r, y1])
        points.extend([x2, y1 + r, x2, y2 - r])
        points.extend([x2 - r, y2, x1 + r, y2])
        points.extend([x1, y2 - r, x1, y1 + r])

        return self.create_polygon(points, smooth=True, **kwargs)

    def _on_enter(self, event):
        if not self.disabled:
            self.hovered = True
            self.current_color = self.hover_color
            self._draw()

    def _on_leave(self, event):
        self.hovered = False
        self.current_color = self.bg_color
        self._draw()

    def _on_click(self, event):
        if not self.disabled and self.command:
            self.command()

    def config(self, **kwargs):
        """Update button configuration."""
        if 'state' in kwargs:
            self.disabled = kwargs['state'] == 'disabled'
        self._draw()

    def update_idletasks(self):
        super().update_idletasks()
        self._draw()


class SidebarItem(tk.Frame):
    """Sidebar navigation item."""

    def __init__(self, parent, text, icon, index, on_select, is_active=False):
        super().__init__(parent, bg=DARK_BG, cursor="hand2")
        self.text = text
        self.icon = icon
        self.index = index
        self.on_select = on_select
        self.is_active = is_active

        self.config(height=48)

        # Icon label
        self.icon_label = tk.Label(
            self, text=icon, font=("Segoe UI Emoji", 16),
            bg=DARK_BG, fg=DARK_TEXT if is_active else DARK_TEXT_SECONDARY
        )
        self.icon_label.pack(side="left", padx=(20, 12))

        # Text label
        self.text_label = tk.Label(
            self, text=text, font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=DARK_BG, fg=DARK_TEXT if is_active else DARK_TEXT_SECONDARY
        )
        self.text_label.pack(side="left")

        # Active indicator
        self.indicator = tk.Frame(self, bg=PRIMARY, width=3, height=0)
        self.indicator.place(x=0, y=0, relheight=1)

        self.bind("<Button-1>", self._on_click)
        self.icon_label.bind("<Button-1>", self._on_click)
        self.text_label.bind("<Button-1>", self._on_click)

        self.bind("<Enter>", self._on_enter)
        self.bind("<Leave>", self._on_leave)

        self.update_active_state()

    def _on_click(self, event):
        self.on_select(self.index)

    def _on_enter(self, event):
        if not self.is_active:
            self.config(bg=SIDEBAR_ACTIVE_BG)
            self.icon_label.config(bg=SIDEBAR_ACTIVE_BG)
            self.text_label.config(bg=SIDEBAR_ACTIVE_BG)

    def _on_leave(self, event):
        if not self.is_active:
            self.config(bg=DARK_BG)
            self.icon_label.config(bg=DARK_BG)
            self.text_label.config(bg=DARK_BG)

    def set_active(self, active):
        self.is_active = active
        self.update_active_state()

    def update_active_state(self):
        if self.is_active:
            self.config(bg=SIDEBAR_ACTIVE_BG)
            self.icon_label.config(bg=SIDEBAR_ACTIVE_BG, fg=PRIMARY)
            self.text_label.config(bg=SIDEBAR_ACTIVE_BG, fg=DARK_TEXT)
            self.indicator.config(height=48)
        else:
            self.config(bg=DARK_BG)
            self.icon_label.config(bg=DARK_BG, fg=DARK_TEXT_SECONDARY)
            self.text_label.config(bg=DARK_BG, fg=DARK_TEXT_SECONDARY)
            self.indicator.config(height=0)


class MainWindow:
    """Main application window with modern sidebar navigation."""

    def __init__(self, root: tk.Tk):
        self.root = root
        self.root.title("TemStream 配置工具")
        self.root.geometry("900x750")
        self.root.minsize(800, 650)

        # Configuration objects
        self.conf: 'SunshineConf' = None
        self.server_ini: 'SunshineServerIni' = None
        self.conf_file_path: str = None
        self.server_ini_path: str = None
        self.api_client = None

        # Tabs
        self.server_config_tab: ServerConfigTab = None
        self.pin_display_tab: PinDisplayTab = None
        self.user_registration_tab: UserRegistrationTab = None

        # Navigation state
        self.current_tab = 0
        self.sidebar_items = []

        # Build UI
        self._build_ui()

    def set_api_client(self, api_client) -> None:
        """Set the API client for backend communication."""
        self.api_client = api_client

    def _build_ui(self) -> None:
        """Build the main UI with sidebar navigation."""
        # Configure root window
        self.root.configure(bg=DARK_BG)

        # === SIDEBAR ===
        self.sidebar = tk.Frame(self.root, bg=DARK_BG, width=SIDEBAR_WIDTH)
        self.sidebar.pack(side="left", fill="y")
        self.sidebar.pack_propagate(False)

        # Logo/Title area
        self.logo_frame = tk.Frame(self.sidebar, bg=DARK_BG, height=80)
        self.logo_frame.pack(fill="x", pady=(0, 20))

        # Logo icon
        self.logo_icon = tk.Label(
            self.logo_frame,
            text="☀️",
            font=("Segoe UI Emoji", 28),
            bg=DARK_BG,
            fg=PRIMARY
        )
        self.logo_icon.pack(pady=(20, 5))

        # App title
        self.logo_title = tk.Label(
            self.logo_frame,
            text="TemStream",
            font=(FONT_FAMILY, FONT_SIZE_XL, "bold"),
            bg=DARK_BG,
            fg=DARK_TEXT
        )
        self.logo_title.pack()

        # Navigation items
        nav_items = [
            ("⚙️", "服务器配置"),
            ("📱", "设备信息"),
            ("👤", "用户管理"),
        ]

        nav_frame = tk.Frame(self.sidebar, bg=DARK_BG)
        nav_frame.pack(fill="x", pady=20)

        for i, (icon, text) in enumerate(nav_items):
            item = SidebarItem(
                nav_frame, text, icon, i,
                self._on_nav_select,
                is_active=(i == 0)
            )
            item.pack(fill="x", ipady=4)
            self.sidebar_items.append(item)

        # Version info at bottom
        self.version_label = tk.Label(
            self.sidebar,
            text="v1.0.0",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=DARK_BG,
            fg=DARK_TEXT_SECONDARY
        )
        self.version_label.pack(side="bottom", pady=15)

        # === MAIN CONTENT ===
        self.content_frame = tk.Frame(self.root, bg=LIGHT_BG)
        self.content_frame.pack(side="left", fill="both", expand=True)

        # Content header
        self._build_content_header()

        # Tab containers (stacked, only show active)
        self.tab_container = tk.Frame(self.content_frame, bg=LIGHT_BG)
        self.tab_container.pack(fill="both", expand=True, padx=MARGIN_LG, pady=(0, MARGIN_LG))

        # Initialize tabs
        self._init_tabs()

        # Center window on screen
        self.root.update_idletasks()
        screen_width = self.root.winfo_screenwidth()
        screen_height = self.root.winfo_screenheight()
        window_width = self.root.winfo_width()
        window_height = self.root.winfo_height()
        x = (screen_width - window_width) // 2
        y = (screen_height - window_height) // 2
        self.root.geometry(f"+{x}+{y}")

    def _build_content_header(self) -> None:
        """Build the content area header."""
        header = tk.Frame(self.content_frame, bg=LIGHT_SECONDARY, height=70)
        header.pack(fill="x", padx=MARGIN_LG, pady=(MARGIN_LG, 0))
        header.pack_propagate(False)

        # Left side - Title
        self.header_title = tk.Label(
            header,
            text="服务器配置",
            font=(FONT_FAMILY, FONT_SIZE_XXL, "bold"),
            bg=LIGHT_SECONDARY,
            fg=LIGHT_TEXT
        )
        self.header_title.pack(side="left", padx=20, anchor="w", fill="y")

        # Right side - Status indicator
        self.status_frame = tk.Frame(header, bg=LIGHT_SECONDARY)
        self.status_frame.pack(side="right", padx=20, fill="y")

        self.status_indicator = tk.Label(
            self.status_frame,
            text="●",
            font=(FONT_FAMILY, 14),
            bg=LIGHT_SECONDARY,
            fg=STATUS_OFFLINE
        )
        self.status_indicator.pack(side="left", padx=(0, 5), anchor="e")

        self.status_text = tk.Label(
            self.status_frame,
            text="未连接",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_SECONDARY,
            fg=LIGHT_TEXT_SECONDARY
        )
        self.status_text.pack(side="left", anchor="e")

    def _init_tabs(self) -> None:
        """Initialize all tab containers."""
        self.tab_frames = {}

        # Server Config Tab
        self.server_config_tab = ServerConfigTab(self.conf, self.server_ini)
        self.server_config_tab.set_save_callback(self._save_config)
        self.tab_frames['server'] = self.server_config_tab.build(self.tab_container)
        self.tab_frames['server'].configure(bg=LIGHT_BG)

        # Pin Display Tab
        self.pin_display_tab = PinDisplayTab()
        self.pin_display_tab.set_save_callback(self._save_config)
        self.tab_frames['pin'] = self.pin_display_tab.build(self.tab_container)
        self.tab_frames['pin'].configure(bg=LIGHT_BG)

        # User Registration Tab
        self.user_registration_tab = UserRegistrationTab()
        self.tab_frames['user'] = self.user_registration_tab.build(self.tab_container)
        self.tab_frames['user'].configure(bg=LIGHT_BG)

        # Hide all except first
        for key in ['pin', 'user']:
            self.tab_frames[key].place_forget()
        self.tab_frames['server'].place(relx=0, rely=0, relwidth=1, relheight=1)

    def _on_nav_select(self, index: int) -> None:
        """Handle navigation item selection."""
        if index == self.current_tab:
            return

        # Update navigation state
        for i, item in enumerate(self.sidebar_items):
            item.set_active(i == index)

        # Update header title
        titles = ["服务器配置", "设备信息", "用户管理"]
        self.header_title.config(text=titles[index])

        # Update status based on tab
        if index == 0:
            # Server config - check connection status
            if self.server_ini and self.server_ini.server_ip:
                self._update_connection_status(True)
            else:
                self._update_connection_status(False)
        elif index == 1:
            # Device info - show device status
            if self.server_ini and self.server_ini.device_id:
                self._update_device_status(True)
            else:
                self._update_device_status(False)
        elif index == 2:
            # User management
            if self.server_ini and self.server_ini.login_user:
                self._update_user_status(True, self.server_ini.login_user)
            else:
                self._update_user_status(False)

        # Animate tab transition
        self._animate_tab_change(index)

    def _animate_tab_change(self, new_index: int) -> None:
        """Animate the tab content change."""
        # Map index to key
        keys = ['server', 'pin', 'user']
        new_key = keys[new_index]

        # Hide current tab
        current_keys = ['server', 'pin', 'user']
        current_key = current_keys[self.current_tab]
        self.tab_frames[current_key].place_forget()

        # Show new tab
        self.tab_frames[new_key].place(relx=0, rely=0, relwidth=1, relheight=1)

        self.current_tab = new_index

    def _update_connection_status(self, connected: bool) -> None:
        """Update connection status display."""
        if connected:
            self.status_indicator.config(fg=STATUS_ONLINE)
            self.status_text.config(text="已连接", fg=STATUS_ONLINE)
        else:
            self.status_indicator.config(fg=STATUS_OFFLINE)
            self.status_text.config(text="未连接", fg=STATUS_OFFLINE)

    def _update_device_status(self, has_device: bool) -> None:
        """Update device status display."""
        if has_device:
            self.status_indicator.config(fg=STATUS_ONLINE)
            self.status_text.config(text="设备就绪", fg=STATUS_ONLINE)
        else:
            self.status_indicator.config(fg=STATUS_PENDING)
            self.status_text.config(text="等待注册", fg=STATUS_PENDING)

    def _update_user_status(self, has_user: bool, username: str = None) -> None:
        """Update user status display."""
        if has_user:
            self.status_indicator.config(fg=STATUS_ONLINE)
            self.status_text.config(text=f"已登录: {username}", fg=STATUS_ONLINE)
        else:
            self.status_indicator.config(fg=STATUS_OFFLINE)
            self.status_text.config(text="未登录", fg=STATUS_OFFLINE)

    def _on_user_bound(self, username: str) -> None:
        """Handle user bound event - update header status."""
        self._update_user_status(True, username)

    def set_config(self, conf: 'SunshineConf', server_ini: 'SunshineServerIni') -> None:
        """Set configuration objects and initialize UI."""
        self.conf = conf
        self.server_ini = server_ini

        # Update Server Config Tab
        if self.server_config_tab:
            self.server_config_tab.update_config(conf, server_ini)

        # Pass server_ini to PIN Display Tab
        if self.pin_display_tab:
            self.pin_display_tab.set_server_ini(server_ini)

        # Pass api_client and device_id to User Registration Tab
        if self.user_registration_tab:
            self.user_registration_tab.set_api_client(self.api_client)
            if server_ini and server_ini.device_id:
                self.user_registration_tab.set_device_id(server_ini.device_id)
            self.user_registration_tab.set_server_ini(server_ini, self._save_config)
            self.user_registration_tab.set_user_updated_callback(self._on_user_bound)

        # Update UI with config values
        self._update_ui_from_config()

    def _update_ui_from_config(self) -> None:
        """Update UI with configuration values."""
        if self.server_ini and self.pin_display_tab:
            self.pin_display_tab.set_device_id(self.server_ini.device_id or "")

        # Update connection status
        if self.server_ini and self.server_ini.server_ip:
            self._update_connection_status(True)
        else:
            self._update_connection_status(False)

    def _save_config(self) -> None:
        """Save configuration to files."""
        # Update Server Config Tab from UI
        if self.server_config_tab:
            self.server_config_tab.update_from_ui()

        if self.server_ini_path:
            ini_saved = self.server_ini.save(self.server_ini_path)

            if ini_saved:
                messagebox.showinfo("成功", "配置保存成功！\n\n请重启 Sunshine 服务使配置生效。")
            else:
                messagebox.showerror("错误", "配置保存失败！")
