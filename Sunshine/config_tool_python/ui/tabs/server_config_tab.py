"""
Server configuration tab with modern card-based layout.

This module handles the Server Config tab UI for configuring backend connection
settings, which are saved to sunshine_server.ini.
"""

import tkinter as tk
from tkinter import ttk, messagebox
from typing import TYPE_CHECKING, Callable
import subprocess
import re

if TYPE_CHECKING:
    from config.sunshine_conf import SunshineConf
    from config.sunshine_server_ini import SunshineServerIni

# Theme imports
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from ui.theme import (
    PRIMARY, PRIMARY_HOVER, PRIMARY_LIGHT,
    DARK_BG, DARK_SECONDARY, DARK_CARD, DARK_BORDER, DARK_TEXT, DARK_TEXT_SECONDARY,
    LIGHT_BG, LIGHT_SECONDARY, LIGHT_BORDER, LIGHT_TEXT, LIGHT_TEXT_SECONDARY,
    FONT_FAMILY, FONT_SIZE_XS, FONT_SIZE_SM, FONT_SIZE_MD, FONT_SIZE_LG, FONT_SIZE_XL,
    RADIUS_SM, RADIUS_MD, RADIUS_LG, PADDING_SM, PADDING_MD, PADDING_LG, PADDING_XL,
    MARGIN_SM, MARGIN_MD, MARGIN_LG
)


class ModernEntry(tk.Frame):
    """Custom modern styled entry field."""

    def __init__(self, parent, placeholder="", width=30, show=None, **kwargs):
        super().__init__(parent, bg=LIGHT_SECONDARY, **kwargs)
        self.placeholder = placeholder
        self.show_char = show
        self.has_placeholder = True

        # Border frame
        self.config(highlightbackground=LIGHT_BORDER, highlightthickness=1, bd=0)

        # Entry
        self.entry = tk.Entry(
            self,
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_SECONDARY,
            fg=LIGHT_TEXT,
            insertbackground=PRIMARY,
            relief="flat",
            bd=0,
            width=width,
            show=show
        )
        self.entry.pack(fill="x", padx=12, pady=10)

        # Placeholder
        self.entry.insert(0, placeholder)
        self.entry.config(fg=LIGHT_TEXT_SECONDARY)

        self.entry.bind("<FocusIn>", self._on_focus_in)
        self.entry.bind("<FocusOut>", self._on_focus_out)

    def _on_focus_in(self, event):
        if self.has_placeholder:
            self.entry.delete(0, tk.END)
            self.entry.config(fg=LIGHT_TEXT)
            if self.show_char:
                self.entry.config(show=self.show_char)
            self.has_placeholder = False
        self.config(highlightbackground=PRIMARY, highlightthickness=2)

    def _on_focus_out(self, event):
        if self.entry.get() == "":
            self.entry.insert(0, self.placeholder)
            self.entry.config(fg=LIGHT_TEXT_SECONDARY)
            if self.show_char:
                self.entry.config(show="")
            self.has_placeholder = True
        self.config(highlightbackground=LIGHT_BORDER, highlightthickness=1)

    def get(self):
        value = self.entry.get()
        return "" if value == self.placeholder or self.has_placeholder else value

    def delete(self, start, end):
        self.entry.delete(start, end)

    def insert(self, index, value):
        self.entry.delete(0, tk.END)
        self.entry.insert(0, value)
        self.entry.config(fg=LIGHT_TEXT)
        if self.show_char:
            self.entry.config(show=self.show_char)
        self.has_placeholder = False

    def config(self, **kwargs):
        super().config(**kwargs)


class ModernButton(tk.Canvas):
    """Custom rounded button widget."""

    def __init__(self, parent, text, command, width=120, height=40,
                 bg_color=PRIMARY, hover_color=PRIMARY_HOVER, text_color="white",
                 **kwargs):
        super().__init__(parent, width=width, height=height,
                        bg=parent.cget('bg') if hasattr(parent, 'cget') else LIGHT_BG,
                        highlightthickness=0, **kwargs)
        self.command = command
        self.text = text
        self.bg_color = bg_color
        self.hover_color = hover_color
        self.text_color = text_color
        self.current_color = bg_color
        self.disabled = False
        self.disabled_color = "#adb5bd"

        self._draw()

        self.bind("<Enter>", self._on_enter)
        self.bind("<Leave>", self._on_leave)
        self.bind("<Button-1>", self._on_click)
        self.bind("<ButtonRelease-1>", self._on_release)

    def _draw(self):
        self.delete("all")
        w, h = self.winfo_width(), self.winfo_height()
        if w < 2 or h < 2:
            w, h = 120, 40

        color = self.disabled_color if self.disabled else self.current_color
        r = 8

        # Rounded rectangle
        self.create_rounded_rect(0, 0, w, h, r, fill=color, outline="")

        # Text
        self.create_text(w // 2, h // 2, text=self.text,
                        fill=self.text_color, font=(FONT_FAMILY, FONT_SIZE_MD, "bold"),
                        anchor="center")

    def create_rounded_rect(self, x1, y1, x2, y2, radius, **kwargs):
        r = min(radius, (x2 - x1) // 2, (y2 - y1) // 2)
        points = [x1 + r, y1, x2 - r, y1, x2, y1 + r, x2, y2 - r,
                  x2 - r, y2, x1 + r, y2, x1, y2 - r, x1, y1 + r]
        return self.create_polygon(points, smooth=True, **kwargs)

    def _on_enter(self, event):
        if not self.disabled:
            self.current_color = self.hover_color
            self._draw()

    def _on_leave(self, event):
        self.current_color = self.bg_color
        self._draw()

    def _on_click(self, event):
        if not self.disabled:
            self.current_color = self.hover_color
            self._draw()

    def _on_release(self, event):
        if not self.disabled and self.command:
            self.command()
        self.current_color = self.hover_color if self.hovered else self.bg_color
        self._draw()

    def config(self, **kwargs):
        if 'state' in kwargs:
            self.disabled = kwargs['state'] == 'disabled'
        self._draw()

    def update_idletasks(self):
        super().update_idletasks()
        self._draw()


class Card(tk.Frame):
    """Modern card container with shadow effect."""

    def __init__(self, parent, title="", **kwargs):
        bg = kwargs.pop('bg', LIGHT_SECONDARY)
        super().__init__(parent, bg=bg, **kwargs)

        # Card header
        if title:
            self.header = tk.Frame(self, bg=LIGHT_SECONDARY)
            self.header.pack(fill="x", padx=PADDING_LG, pady=(PADDING_LG, 0))

            self.title_label = tk.Label(
                self.header,
                text=title,
                font=(FONT_FAMILY, FONT_SIZE_LG, "bold"),
                bg=LIGHT_SECONDARY,
                fg=LIGHT_TEXT
            )
            self.title_label.pack(side="left")

            # Accent line
            self.accent = tk.Frame(self.header, bg=PRIMARY, height=3)
            self.accent.pack(side="bottom", fill="x", pady=(PADDING_SM, 0))

        # Card content
        self.content = tk.Frame(self, bg=LIGHT_SECONDARY)
        self.content.pack(fill="both", expand=True, padx=PADDING_LG, pady=PADDING_LG)


class Checkbox(tk.Frame):
    """Custom checkbox widget."""

    def __init__(self, parent, text, command=None, initial_state=False):
        super().__init__(parent, bg=parent.cget('bg') if hasattr(parent, 'cget') else LIGHT_SECONDARY)

        self.var = tk.BooleanVar(value=initial_state)
        self.command = command

        self.canvas = tk.Canvas(self, width=20, height=20,
                               bg=self.cget('bg'), highlightthickness=0)
        self.canvas.pack(side="left")

        self.text_label = tk.Label(
            self, text=text,
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=self.cget('bg'), fg=LIGHT_TEXT, cursor="hand2"
        )
        self.text_label.pack(side="left", padx=(8, 0))

        self._draw_checkbox()

        self.canvas.bind("<Button-1>", self._toggle)
        self.text_label.bind("<Button-1>", self._toggle)

    def _draw_checkbox(self):
        self.canvas.delete("all")
        checked = self.var.get()

        if checked:
            # Draw checked box with primary color
            self.canvas.create_rectangle(2, 2, 18, 18, fill=PRIMARY, outline=PRIMARY, width=0)
            # Draw checkmark
            self.canvas.create_text(10, 10, text="✓", fill="white",
                                  font=(FONT_FAMILY, 11, "bold"), anchor="center")
        else:
            # Draw unchecked box
            self.canvas.create_rectangle(2, 2, 18, 18, fill=LIGHT_SECONDARY, outline=LIGHT_BORDER)

    def _toggle(self, event):
        self.var.set(not self.var.get())
        self._draw_checkbox()
        if self.command:
            self.command()

    def get(self):
        return self.var.get()

    def set(self, value):
        self.var.set(value)
        self._draw_checkbox()
        if self.command:
            self.command()


class ServerConfigTab:
    """Server configuration tab for backend connection settings."""

    def __init__(self, conf: 'SunshineConf', server_ini: 'SunshineServerIni'):
        self.conf = conf
        self.server_ini = server_ini
        self.save_callback: Callable[[], None] = None

        # UI elements
        self.frame = None
        self.server_ip_entry = None
        self.server_port_entry = None
        self.lan_fixed_pin_checkbox = None
        self.lan_password_entry = None
        self.lan_password_container = None
        self.network_canvas = None
        self.network_frame = None
        self.network_scrollbar = None
        self.network_info_labels = []
        self.refresh_network_btn = None

    def set_save_callback(self, callback: Callable[[], None]) -> None:
        self.save_callback = callback

    def build(self, parent) -> tk.Frame:
        self.frame = tk.Frame(parent, bg=LIGHT_BG)

        # Scrollable canvas
        container = tk.Frame(self.frame, bg=LIGHT_BG)
        container.pack(fill="both", expand=True)

        scrollbar = tk.Scrollbar(container, orient="vertical", width=8)
        scrollbar.pack(side="right", fill="y")

        canvas = tk.Canvas(container, bg=LIGHT_BG, highlightthickness=0,
                          yscrollcommand=scrollbar.set)
        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.config(command=canvas.yview)

        scroll_frame = tk.Frame(canvas, bg=LIGHT_BG)
        canvas_window = canvas.create_window((0, 0), window=scroll_frame, anchor="nw")

        def on_frame_configure(event):
            canvas.configure(scrollregion=canvas.bbox("all"))

        scroll_frame.bind("<Configure>", on_frame_configure)
        canvas.bind("<Configure>", lambda e: canvas.itemconfig(canvas_window, width=e.width))

        # === BACKEND CONNECTION CARD ===
        connection_card = Card(scroll_frame, title="后端连接")
        connection_card.pack(fill="x", padx=MARGIN_SM, pady=(0, MARGIN_MD))

        # Server IP
        ip_row = tk.Frame(connection_card.content, bg=LIGHT_SECONDARY)
        ip_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            ip_row,
            text="服务器地址",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT,
            width=15, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.server_ip_entry = ModernEntry(ip_row, placeholder="如: 192.168.1.100", width=25)
        self.server_ip_entry.pack(side="left", fill="x", expand=True)

        # Server Port
        port_row = tk.Frame(connection_card.content, bg=LIGHT_SECONDARY)
        port_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            port_row,
            text="服务器端口",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT,
            width=15, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.server_port_entry = ModernEntry(port_row, placeholder="默认: 12345", width=25)
        self.server_port_entry.pack(side="left", fill="x", expand=True)

        # Connection hint
        hint_label = tk.Label(
            connection_card.content,
            text="💡 配置 Sunshine 管理后台的连接信息，用于设备注册和状态同步",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT_SECONDARY, anchor="w"
        )
        hint_label.pack(fill="x", pady=(MARGIN_SM, 0))

        # === LAN MODE CARD ===
        lan_card = Card(scroll_frame, title="局域网直连模式")
        lan_card.pack(fill="x", padx=MARGIN_SM, pady=(0, MARGIN_MD))

        # LAN checkbox
        checkbox_row = tk.Frame(lan_card.content, bg=LIGHT_SECONDARY)
        checkbox_row.pack(fill="x", pady=(0, MARGIN_MD))

        self.lan_fixed_pin_checkbox = Checkbox(
            checkbox_row,
            text="启用局域网直连 (无需管理后台)",
            command=self._on_lan_mode_toggled
        )
        self.lan_fixed_pin_checkbox.pack(side="left")

        self.refresh_network_btn = ModernButton(
            checkbox_row,
            text="🔄 获取本机网络信息",
            command=self._on_refresh_network_clicked
        )
        self.refresh_network_btn.pack(side="left", padx=(MARGIN_MD, 0))

        # LAN password container (hidden by default)
        self.lan_password_container = tk.Frame(lan_card.content, bg=LIGHT_SECONDARY)

        password_row = tk.Frame(self.lan_password_container, bg=LIGHT_SECONDARY)
        password_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            password_row,
            text="连接密码",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT,
            width=15, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.lan_password_entry = ModernEntry(password_row, placeholder="设置连接密码", width=25, show="●")
        self.lan_password_entry.pack(side="left", fill="x", expand=True)

        # Network info section
        network_container = tk.Frame(self.lan_password_container, bg=LIGHT_SECONDARY)

        self.network_canvas = tk.Canvas(
            network_container,
            height=120,
            bg=LIGHT_BG,
            highlightthickness=1,
            highlightbackground=LIGHT_BORDER
        )
        self.network_scrollbar = ttk.Scrollbar(
            network_container,
            orient="vertical",
            command=self.network_canvas.yview
        )
        self.network_canvas.configure(yscrollcommand=self.network_scrollbar.set)

        self.network_frame = tk.Frame(self.network_canvas, bg=LIGHT_BG)
        self.network_canvas_window = self.network_canvas.create_window(
            (0, 0), window=self.network_frame, anchor="nw"
        )

        def _on_canvas_configure(event):
            self.network_canvas.configure(scrollregion=self.network_canvas.bbox("all"))

        def _on_network_frame_configure(event):
            self.network_canvas.configure(scrollregion=self.network_canvas.bbox("all"))

        self.network_frame.bind("<Configure>", _on_network_frame_configure)
        self.network_canvas.bind("<Configure>", _on_canvas_configure)

        self.network_canvas.pack(side="left", fill="both", expand=True, pady=(MARGIN_SM, 0), padx=(0, 5))
        self.network_scrollbar.pack(side="left", fill="y")

        network_container.pack(fill="x", pady=(MARGIN_SM, 0))

        self.network_info_labels = []

        # === ACTION BUTTONS ===
        button_card = tk.Frame(scroll_frame, bg=LIGHT_BG)
        button_card.pack(fill="x", padx=MARGIN_SM, pady=(MARGIN_LG, 0))

        self.save_btn = ModernButton(
            button_card,
            text="💾 保存配置",
            command=self._on_save_clicked,
            width=150, height=44
        )
        self.save_btn.pack(side="left")

        # Info text
        info_label = tk.Label(
            button_card,
            text="配置将保存到 sunshine_server.ini",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_BG, fg=LIGHT_TEXT_SECONDARY
        )
        info_label.pack(side="left", padx=(MARGIN_MD, 0), anchor="w", fill="x")

        return self.frame

    def _on_lan_mode_toggled(self) -> None:
        if self.lan_password_container:
            if self.lan_fixed_pin_checkbox.get():
                self.lan_password_container.pack(fill="x", pady=(MARGIN_MD, 0))
                self.refresh_network_btn.pack(side="left", padx=(MARGIN_MD, 0))
            else:
                self.lan_password_container.pack_forget()
                self.refresh_network_btn.pack_forget()

    def _on_refresh_network_clicked(self) -> None:
        self.refresh_network_btn.text = "获取中..."
        self.refresh_network_btn.disabled = True
        self.refresh_network_btn._draw()
        self.frame.update_idletasks()
        self._update_network_info()
        self.refresh_network_btn.text = "🔄 获取本机网络信息"
        self.refresh_network_btn.disabled = False
        self.refresh_network_btn._draw()

    def _get_active_network_adapters(self) -> list:
        result = []
        try:
            proc = subprocess.run(
                ['ipconfig'],
                capture_output=True,
                text=True,
                encoding='gbk',
                errors='ignore',
                timeout=10
            )

            for line in proc.stdout.splitlines():
                match = re.search(r'IPv4.*?:\s*(\d+\.\d+\.\d+\.\d+)', line)
                if match:
                    ip = match.group(1)
                    if not ip.startswith('127.'):
                        result.append(ip)

        except Exception:
            pass

        return result

    def _is_relevant_adapter(self, adapter_name: str) -> bool:
        if not adapter_name:
            return False
        upper = adapter_name.upper()
        return any(t in upper for t in ['ETHERNET', 'VETHERNET', 'WIRELESS', 'WLAN', '以太网', '无线'])

    def _update_network_info(self) -> None:
        for label in self.network_info_labels:
            label.destroy()
        self.network_info_labels.clear()

        adapters = self._get_active_network_adapters()

        if not adapters:
            label = tk.Label(
                self.network_frame,
                text="未检测到活动的网络适配器",
                font=(FONT_FAMILY, FONT_SIZE_MD),
                bg=LIGHT_BG, fg=LIGHT_TEXT_SECONDARY
            )
            label.pack(anchor="w", padx=10, pady=10)
            self.network_info_labels.append(label)
            self._update_canvas_scrollregion()
            return

        header_label = tk.Label(
            self.network_frame,
            text="🌐 本机网络信息",
            font=(FONT_FAMILY, FONT_SIZE_MD, "bold"),
            bg=LIGHT_BG, fg=LIGHT_TEXT
        )
        header_label.pack(anchor="w", padx=10, pady=(10, 5))
        self.network_info_labels.append(header_label)

        for ip in adapters:
            label = tk.Label(
                self.network_frame,
                text=f"  📶 {ip}",
                font=(FONT_FAMILY, FONT_SIZE_MD),
                bg=LIGHT_BG, fg=LIGHT_TEXT
            )
            label.pack(anchor="w", padx=10, pady=2)
            self.network_info_labels.append(label)

        self._update_canvas_scrollregion()

    def _update_canvas_scrollregion(self) -> None:
        if hasattr(self, 'network_canvas'):
            self.network_canvas.update_idletasks()
            self.network_canvas.configure(scrollregion=self.network_canvas.bbox("all"))

    def _on_save_clicked(self) -> None:
        if self.lan_fixed_pin_checkbox and self.lan_fixed_pin_checkbox.get():
            password = self.lan_password_entry.get().strip() if self.lan_password_entry else ""
            if not password:
                messagebox.showerror("错误", "局域网密码不能为空")
                return

        if self.save_callback:
            self.save_callback()
        else:
            messagebox.showwarning("警告", "保存函数未配置")

    def update_config(self, conf: 'SunshineConf', server_ini: 'SunshineServerIni') -> None:
        self.conf = conf
        self.server_ini = server_ini

        def get_value(value):
            return value if value is not None else ""

        def get_int(value, default=12345):
            try:
                return int(value) if value else default
            except (ValueError, TypeError):
                return default

        def get_bool(value, default=False):
            return bool(value) if value is not None else default

        if self.server_ip_entry:
            self.server_ip_entry.entry.delete(0, tk.END)
            self.server_ip_entry.entry.insert(0, get_value(server_ini.server_ip))
            self.server_ip_entry.entry.config(fg=LIGHT_TEXT)
            self.server_ip_entry.has_placeholder = False

        if self.server_port_entry:
            self.server_port_entry.entry.delete(0, tk.END)
            self.server_port_entry.entry.insert(0, str(get_int(server_ini.server_port)))
            self.server_port_entry.entry.config(fg=LIGHT_TEXT)
            self.server_port_entry.has_placeholder = False

        if self.lan_fixed_pin_checkbox:
            self.lan_fixed_pin_checkbox.set(get_bool(server_ini.lan_fixed_pin, default=False))

        if self.lan_password_entry:
            self.lan_password_entry.entry.delete(0, tk.END)
            self.lan_password_entry.entry.insert(0, get_value(server_ini.lan_password))
            self.lan_password_entry.entry.config(fg=LIGHT_TEXT)
            self.lan_password_entry.has_placeholder = False

    def update_from_ui(self) -> None:
        if self.server_ip_entry:
            self.server_ini.server_ip = self.server_ip_entry.get().strip()

        if self.server_port_entry:
            try:
                self.server_ini.server_port = int(self.server_port_entry.get().strip())
            except ValueError:
                self.server_ini.server_port = 12345

        if self.lan_fixed_pin_checkbox:
            self.server_ini.lan_fixed_pin = self.lan_fixed_pin_checkbox.get()

        if self.lan_password_entry:
            self.server_ini.lan_password = self.lan_password_entry.get().strip()
