"""
User registration tab with modern styling.

This module handles the User Registration tab UI for registering and binding users.
"""

import tkinter as tk
from tkinter import messagebox
from typing import Optional, TYPE_CHECKING

if TYPE_CHECKING:
    from api import ApiClient

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from api import translate_error
from ui.theme import (
    PRIMARY, PRIMARY_HOVER, PRIMARY_LIGHT, SUCCESS, SUCCESS_LIGHT,
    DANGER, DANGER_LIGHT,
    DARK_BG, DARK_SECONDARY, DARK_CARD, DARK_BORDER, DARK_TEXT, DARK_TEXT_SECONDARY,
    LIGHT_BG, LIGHT_SECONDARY, LIGHT_BORDER, LIGHT_TEXT, LIGHT_TEXT_SECONDARY,
    FONT_FAMILY, FONT_SIZE_XS, FONT_SIZE_SM, FONT_SIZE_MD, FONT_SIZE_LG, FONT_SIZE_XL,
    RADIUS_SM, RADIUS_MD, RADIUS_LG, PADDING_SM, PADDING_MD, PADDING_LG, PADDING_XL,
    MARGIN_SM, MARGIN_MD, MARGIN_LG, STATUS_ONLINE, STATUS_OFFLINE, STATUS_PENDING
)


class ModernEntry(tk.Frame):
    """Custom modern styled entry field."""

    def __init__(self, parent, placeholder="", width=30, show=None, **kwargs):
        super().__init__(parent, bg=LIGHT_SECONDARY, **kwargs)
        self.placeholder = placeholder
        self.show_char = show
        self.has_placeholder = True

        self.config(highlightbackground=LIGHT_BORDER, highlightthickness=1, bd=0)

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


class ModernTabWidget(tk.Frame):
    """Custom tab widget for switching between content areas."""

    def __init__(self, parent, tabs: list, command=None, **kwargs):
        super().__init__(parent, bg=LIGHT_BG, **kwargs)
        self.tabs = tabs
        self.command = command
        self.current_index = 0
        self.tab_buttons = []

        self._build_tabs()

    def _build_tabs(self):
        # Tab container frame
        self.tab_container = tk.Frame(self, bg=LIGHT_BORDER)
        self.tab_container.pack(fill="x", pady=(0, 0))

        for i, tab_name in enumerate(self.tabs):
            btn = tk.Frame(
                self.tab_container,
                bg=LIGHT_SECONDARY if i == 0 else LIGHT_BG,
                cursor="hand2",
                height=40
            )

            label = tk.Label(
                btn,
                text=tab_name,
                font=(FONT_FAMILY, FONT_SIZE_MD),
                bg=LIGHT_SECONDARY if i == 0 else LIGHT_BG,
                fg=LIGHT_TEXT if i == 0 else LIGHT_TEXT_SECONDARY,
                padx=20, pady=10
            )
            label.pack(side="left")

            btn.tab_index = i
            btn.label = label

            btn.bind("<Button-1>", lambda e, idx=i: self._on_tab_clicked(idx))
            label.bind("<Button-1>", lambda e, idx=i: self._on_tab_clicked(idx))

            btn.pack(side="left", padx=(0, 2))
            self.tab_buttons.append(btn)

        # Content frame
        self.content_frame = tk.Frame(self, bg=LIGHT_BG)
        self.content_frame.pack(fill="both", expand=True, pady=(0, 0))

    def _on_tab_clicked(self, index: int):
        if index == self.current_index:
            return

        # Update button styles
        for i, btn in enumerate(self.tab_buttons):
            if i == index:
                btn.config(bg=LIGHT_SECONDARY)
                btn.label.config(bg=LIGHT_SECONDARY, fg=LIGHT_TEXT)
            else:
                btn.config(bg=LIGHT_BG)
                btn.label.config(bg=LIGHT_BG, fg=LIGHT_TEXT_SECONDARY)

        self.current_index = index

        if self.command:
            self.command(index)

    def get_content_frame(self) -> tk.Frame:
        return self.content_frame

    def set_content(self, index: int, frame: tk.Frame):
        """Set content for a specific tab index."""
        if hasattr(self, '_tab_contents') is False:
            self._tab_contents = {}
        self._tab_contents[index] = frame


class ModernButton(tk.Canvas):
    """Custom rounded button widget."""

    def __init__(self, parent, text, command, width=140, height=44,
                 bg_color=PRIMARY, hover_color=PRIMARY_HOVER, text_color="white",
                 **kwargs):
        super().__init__(parent, width=width, height=height,
                        bg=parent.cget('bg') if hasattr(parent, 'cget') else LIGHT_BG,
                        highlightthickness=0, **kwargs)
        self.command = command
        self.bg_color = bg_color
        self.hover_color = hover_color
        self.text_color = text_color
        self.current_color = bg_color
        self.disabled = False
        self.disabled_color = "#adb5bd"
        self.text = text

        self._draw()

        self.bind("<Enter>", self._on_enter)
        self.bind("<Leave>", self._on_leave)
        self.bind("<Button-1>", self._on_click)
        self.bind("<ButtonRelease-1>", self._on_release)

    def _draw(self):
        self.delete("all")
        w, h = self.winfo_width(), self.winfo_height()
        if w < 2 or h < 2:
            w, h = 140, 44

        color = self.disabled_color if self.disabled else self.current_color
        r = 8

        self.create_rounded_rect(0, 0, w, h, r, fill=color, outline="")
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
        self.current_color = self.hover_color
        self._draw()

    def config(self, **kwargs):
        if 'state' in kwargs:
            self.disabled = kwargs['state'] == 'disabled'
        self._draw()

    def update_idletasks(self):
        super().update_idletasks()
        self._draw()


class Card(tk.Frame):
    """Modern card container."""

    def __init__(self, parent, title="", **kwargs):
        bg = kwargs.pop('bg', LIGHT_SECONDARY)
        super().__init__(parent, bg=bg, **kwargs)

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

            self.accent = tk.Frame(self.header, bg=PRIMARY, height=3)
            self.accent.pack(side="bottom", fill="x", pady=(PADDING_SM, 0))

        self.content = tk.Frame(self, bg=LIGHT_SECONDARY)
        self.content.pack(fill="both", expand=True, padx=PADDING_LG, pady=PADDING_LG)


class UserRegistrationTab:
    """User registration tab with API integration."""

    def __init__(self, api_client: Optional['ApiClient'] = None, device_id: Optional[str] = None):
        self.api_client = api_client
        self.device_id = device_id
        self.server_ini = None
        self.save_callback = None
        self.user_updated_callback = None

        self.frame = None
        self.tab_widget = None
        self.status_icon = None
        self.bound_user_label = None
        self.device_id_label = None
        self.status_badge = None

        # Register new user tab elements
        self.reg_username_entry = None
        self.reg_password_entry = None
        self.reg_confirm_password_entry = None
        self.register_btn = None

        # Bind existing user tab elements
        self.bind_username_entry = None
        self.bind_password_entry = None
        self.bind_btn = None

        # Change password tab elements
        self.change_username_label = None
        self.change_new_password_entry = None
        self.change_confirm_password_entry = None
        self.change_password_btn = None

        # Content frames for tabs
        self.register_content_frame = None
        self.bind_content_frame = None
        self.change_password_content_frame = None
        self.current_tab = 0

    def set_api_client(self, api_client: 'ApiClient') -> None:
        self.api_client = api_client

    def set_device_id(self, device_id: str) -> None:
        self.device_id = device_id
        if self.device_id_label:
            if device_id:
                self.device_id_label.config(
                    text=f"  📱 {device_id}  ",
                    fg=PRIMARY
                )
            else:
                self.device_id_label.config(
                    text="  ⏳ 等待分配  ",
                    fg=LIGHT_TEXT_SECONDARY
                )

    def set_server_ini(self, server_ini, save_callback) -> None:
        self.server_ini = server_ini
        self.save_callback = save_callback
        if server_ini and server_ini.login_user:
            self.set_bound_user(server_ini.login_user)

    def set_user_updated_callback(self, callback) -> None:
        """Set callback to be called when user is bound successfully."""
        self.user_updated_callback = callback

    def _build_register_content(self, parent) -> tk.Frame:
        """Build content for 'Register New User' tab."""
        frame = tk.Frame(parent, bg=LIGHT_BG)

        # Username
        username_row = tk.Frame(frame, bg=LIGHT_BG)
        username_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            username_row,
            text="用户名",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_BG, fg=LIGHT_TEXT,
            width=12, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.reg_username_entry = ModernEntry(username_row, placeholder="输入用户名 (至少3个字符)", width=20)
        self.reg_username_entry.pack(side="left", fill="x", expand=True)

        # Password
        pwd_row = tk.Frame(frame, bg=LIGHT_BG)
        pwd_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            pwd_row,
            text="密码",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_BG, fg=LIGHT_TEXT,
            width=12, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.reg_password_entry = ModernEntry(pwd_row, width=20, show="●")
        self.reg_password_entry.pack(side="left", fill="x", expand=True)

        # Confirm Password
        confirm_row = tk.Frame(frame, bg=LIGHT_BG)
        confirm_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            confirm_row,
            text="确认密码",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_BG, fg=LIGHT_TEXT,
            width=12, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.reg_confirm_password_entry = ModernEntry(confirm_row, width=20, show="●")
        self.reg_confirm_password_entry.pack(side="left", fill="x", expand=True)
        self.reg_confirm_password_entry.entry.bind("<Return>", lambda e: self._on_register_clicked())

        # Hint
        hint_label = tk.Label(
            frame,
            text="💡 注册后将自动绑定此设备到您的账户",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_BG, fg=LIGHT_TEXT_SECONDARY
        )
        hint_label.pack(fill="x")

        # Button
        btn_frame = tk.Frame(frame, bg=LIGHT_BG)
        btn_frame.pack(fill="x", pady=(MARGIN_LG, 0))

        self.register_btn = ModernButton(
            btn_frame,
            text="✅ 注册并绑定",
            command=self._on_register_clicked,
            width=160, height=44
        )
        self.register_btn.pack(side="left")

        return frame

    def _build_bind_content(self, parent) -> tk.Frame:
        """Build content for 'Bind Existing User' tab."""
        frame = tk.Frame(parent, bg=LIGHT_BG)

        # Username
        username_row = tk.Frame(frame, bg=LIGHT_BG)
        username_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            username_row,
            text="用户名",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_BG, fg=LIGHT_TEXT,
            width=12, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.bind_username_entry = ModernEntry(username_row, placeholder="输入已注册的用户名", width=20)
        self.bind_username_entry.pack(side="left", fill="x", expand=True)

        # Password
        pwd_row = tk.Frame(frame, bg=LIGHT_BG)
        pwd_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            pwd_row,
            text="密码",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_BG, fg=LIGHT_TEXT,
            width=12, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.bind_password_entry = ModernEntry(pwd_row, width=20, show="●")
        self.bind_password_entry.pack(side="left", fill="x", expand=True)
        self.bind_password_entry.entry.bind("<Return>", lambda e: self._on_bind_clicked())

        # Hint
        hint_label = tk.Label(
            frame,
            text="💡 使用已注册的账号绑定此设备",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_BG, fg=LIGHT_TEXT_SECONDARY
        )
        hint_label.pack(fill="x")

        # Button
        btn_frame = tk.Frame(frame, bg=LIGHT_BG)
        btn_frame.pack(fill="x", pady=(MARGIN_LG, 0))

        self.bind_btn = ModernButton(
            btn_frame,
            text="🔗 绑定已有用户",
            command=self._on_bind_clicked,
            width=160, height=44
        )
        self.bind_btn.pack(side="left")

        return frame

    def _build_change_password_content(self, parent) -> tk.Frame:
        """Build content for 'Change Password' tab."""
        frame = tk.Frame(parent, bg=LIGHT_BG)

        # Username display (read-only)
        username_row = tk.Frame(frame, bg=LIGHT_BG)
        username_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            username_row,
            text="当前账号",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_BG, fg=LIGHT_TEXT,
            width=12, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.change_username_label = tk.Label(
            username_row,
            text="未绑定用户",
            font=(FONT_FAMILY, FONT_SIZE_MD, "bold"),
            bg=LIGHT_BG, fg=LIGHT_TEXT_SECONDARY,
            anchor="w"
        )
        self.change_username_label.pack(side="left", fill="x", expand=True)

        # New Password
        pwd_row = tk.Frame(frame, bg=LIGHT_BG)
        pwd_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            pwd_row,
            text="新密码",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_BG, fg=LIGHT_TEXT,
            width=12, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.change_new_password_entry = ModernEntry(pwd_row, width=20, show="●")
        self.change_new_password_entry.pack(side="left", fill="x", expand=True)

        # Confirm Password
        confirm_row = tk.Frame(frame, bg=LIGHT_BG)
        confirm_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            confirm_row,
            text="确认密码",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_BG, fg=LIGHT_TEXT,
            width=12, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.change_confirm_password_entry = ModernEntry(confirm_row, width=20, show="●")
        self.change_confirm_password_entry.pack(side="left", fill="x", expand=True)
        self.change_confirm_password_entry.entry.bind("<Return>", lambda e: self._on_change_password_clicked())

        # Hint
        hint_label = tk.Label(
            frame,
            text="💡 新密码至少需要4个字符",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_BG, fg=LIGHT_TEXT_SECONDARY
        )
        hint_label.pack(fill="x")

        # Button
        btn_frame = tk.Frame(frame, bg=LIGHT_BG)
        btn_frame.pack(fill="x", pady=(MARGIN_LG, 0))

        self.change_password_btn = ModernButton(
            btn_frame,
            text="🔑 确认修改",
            command=self._on_change_password_clicked,
            width=160, height=44
        )
        self.change_password_btn.pack(side="left")

        return frame

    def _on_tab_changed(self, index: int):
        """Handle tab switching."""
        self.current_tab = index
        if self.register_content_frame:
            if index == 0:
                self.register_content_frame.pack(fill="both", expand=True)
                self.bind_content_frame.pack_forget()
                self.change_password_content_frame.pack_forget()
            elif index == 1:
                self.register_content_frame.pack_forget()
                self.bind_content_frame.pack(fill="both", expand=True)
                self.change_password_content_frame.pack_forget()
            elif index == 2:
                self.register_content_frame.pack_forget()
                self.bind_content_frame.pack_forget()
                self.change_password_content_frame.pack(fill="both", expand=True)

    def build(self, parent) -> tk.Frame:
        self.frame = tk.Frame(parent, bg=LIGHT_BG)

        # Scrollable container
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

        # === DEVICE INFO CARD ===
        device_card = Card(scroll_frame, title="设备信息")
        device_card.pack(fill="x", padx=MARGIN_SM, pady=(0, MARGIN_MD))

        # Device info row
        device_row = tk.Frame(device_card.content, bg=LIGHT_SECONDARY)
        device_row.pack(fill="x")

        # Device icon
        icon_label = tk.Label(
            device_row,
            text="📱",
            font=("Segoe UI Emoji", 28),
            bg=LIGHT_SECONDARY
        )
        icon_label.pack(side="left", padx=(0, MARGIN_MD))

        # Device info
        info_frame = tk.Frame(device_row, bg=LIGHT_SECONDARY)
        info_frame.pack(side="left", fill="both", expand=True)

        tk.Label(
            info_frame,
            text="设备 ID",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT_SECONDARY
        ).pack(anchor="w")

        self.device_id_label = tk.Label(
            info_frame,
            text=f"  📱 {self.device_id if self.device_id else '等待分配'}  ",
            font=(FONT_FAMILY, FONT_SIZE_LG, "bold"),
            bg=LIGHT_SECONDARY,
            fg=PRIMARY if self.device_id else LIGHT_TEXT_SECONDARY
        )
        self.device_id_label.pack(anchor="w", pady=(2, 0))

        # === BINDING STATUS CARD ===
        status_card = Card(scroll_frame, title="绑定状态")
        status_card.pack(fill="x", padx=MARGIN_SM, pady=(0, MARGIN_MD))

        # Status display
        status_row = tk.Frame(status_card.content, bg=LIGHT_SECONDARY)
        status_row.pack(fill="x")

        # Status icon
        self.status_icon = tk.Label(
            status_row,
            text="❌",
            font=("Segoe UI Emoji", 24),
            bg=LIGHT_SECONDARY
        )
        self.status_icon.pack(side="left", padx=(0, MARGIN_MD))

        # Status info
        status_info = tk.Frame(status_row, bg=LIGHT_SECONDARY)
        status_info.pack(side="left", fill="both", expand=True)

        tk.Label(
            status_info,
            text="绑定状态",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT_SECONDARY
        ).pack(anchor="w")

        self.bound_user_label = tk.Label(
            status_info,
            text="未绑定用户",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT
        )
        self.bound_user_label.pack(anchor="w", pady=(2, 0))

        # Status badge
        self.status_badge = tk.Label(
            status_row,
            text="  未绑定  ",
            font=(FONT_FAMILY, FONT_SIZE_SM, "bold"),
            bg=STATUS_OFFLINE,
            fg="white",
            padx=12, pady=4
        )
        self.status_badge.pack(side="right")

        # Hint text
        hint_label = tk.Label(
            status_card.content,
            text="💡 绑定用户后将自动使用此账号登录管理后台",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT_SECONDARY
        )
        hint_label.pack(fill="x", pady=(MARGIN_SM, 0))

        # === TAB WIDGET ===
        self.tab_widget = ModernTabWidget(
            scroll_frame,
            tabs=["注册新用户", "绑定已有用户", "修改密码"],
            command=self._on_tab_changed
        )
        self.tab_widget.pack(fill="x", padx=MARGIN_SM, pady=(MARGIN_MD, 0))

        tab_content_frame = self.tab_widget.get_content_frame()

        # Build tab contents
        self.register_content_frame = self._build_register_content(tab_content_frame)
        self.register_content_frame.pack(fill="both", expand=True)

        self.bind_content_frame = self._build_bind_content(tab_content_frame)
        self.bind_content_frame.pack_forget()

        self.change_password_content_frame = self._build_change_password_content(tab_content_frame)
        self.change_password_content_frame.pack_forget()

        return self.frame

    def _on_register_clicked(self) -> None:
        if not self.api_client:
            messagebox.showerror("错误", "API客户端未配置")
            return

        if not self.device_id:
            messagebox.showerror("错误", "设备ID不可用")
            return

        username = self.reg_username_entry.get().strip()
        password = self.reg_password_entry.get()
        confirm_password = self.reg_confirm_password_entry.get()

        if not username:
            messagebox.showerror("错误", "用户名不能为空")
            return

        if len(username) < 3:
            messagebox.showerror("错误", "用户名至少需要3个字符")
            return

        if len(password) < 4:
            messagebox.showerror("错误", "密码至少需要4个字符")
            return

        if password != confirm_password:
            messagebox.showerror("错误", "两次输入的密码不一致")
            return

        # Disable button during registration
        original_text = self.register_btn.text
        self.register_btn.text = "⏳ 注册中..."
        self.register_btn.config(state='disabled')

        try:
            result = self.api_client.register_and_bind(username, password, self.device_id)
            if result.get('code') == 0:
                data = result.get('data', {})
                bound_username = data.get('userName', username)

                if self.server_ini and self.save_callback:
                    self.server_ini.login_user = bound_username
                    self.save_callback()

                messagebox.showinfo(
                    "成功",
                    f"✅ 用户注册并绑定成功!\n\n"
                    f"用户名: {bound_username}\n"
                    f"设备: {data.get('deviceName', '未知')}"
                )
                self.set_bound_user(bound_username)

                # 通知主窗口更新右上角状态
                if self.user_updated_callback:
                    self.user_updated_callback(bound_username)

                self._clear_register_inputs()
            else:
                messagebox.showerror("错误", translate_error(result.get('msg', '注册失败')))
        except Exception as e:
            messagebox.showerror("错误", str(e))
        finally:
            self.register_btn.text = original_text
            self.register_btn.config(state='normal')
            self.register_btn._draw()

    def _on_bind_clicked(self) -> None:
        if not self.api_client:
            messagebox.showerror("错误", "API客户端未配置")
            return

        if not self.device_id:
            messagebox.showerror("错误", "设备ID不可用")
            return

        username = self.bind_username_entry.get().strip()
        password = self.bind_password_entry.get()

        if not username:
            messagebox.showerror("错误", "用户名不能为空")
            return

        if not password:
            messagebox.showerror("错误", "密码不能为空")
            return

        # Disable button during binding
        original_text = self.bind_btn.text
        self.bind_btn.text = "⏳ 验证中..."
        self.bind_btn.config(state='disabled')

        try:
            result = self.api_client.bind_existing_user(username, password, self.device_id)
            if result.get('code') == 0:
                data = result.get('data', {})
                bound_username = data.get('userName', username)
                user_existed = data.get('userExisted', True)

                if self.server_ini and self.save_callback:
                    self.server_ini.login_user = bound_username
                    self.save_callback()

                messagebox.showinfo(
                    "成功",
                    f"✅ 用户绑定成功!\n\n"
                    f"用户名: {bound_username}\n"
                    f"设备: {data.get('deviceName', '未知')}"
                )
                self.set_bound_user(bound_username)

                # 通知主窗口更新右上角状态
                if self.user_updated_callback:
                    self.user_updated_callback(bound_username)

                self._clear_bind_inputs()
            else:
                messagebox.showerror("错误", translate_error(result.get('msg', '绑定失败')))
        except Exception as e:
            messagebox.showerror("错误", str(e))
        finally:
            self.bind_btn.text = original_text
            self.bind_btn.config(state='normal')
            self.bind_btn._draw()

    def _clear_register_inputs(self):
        """Clear register form inputs."""
        if self.reg_username_entry:
            self.reg_username_entry.entry.delete(0, tk.END)
        if self.reg_password_entry:
            self.reg_password_entry.entry.delete(0, tk.END)
        if self.reg_confirm_password_entry:
            self.reg_confirm_password_entry.entry.delete(0, tk.END)

    def _clear_bind_inputs(self):
        """Clear bind form inputs."""
        if self.bind_username_entry:
            self.bind_username_entry.entry.delete(0, tk.END)
        if self.bind_password_entry:
            self.bind_password_entry.entry.delete(0, tk.END)

    def _on_change_password_clicked(self) -> None:
        if not self.api_client:
            messagebox.showerror("错误", "API客户端未配置")
            return

        if not self.device_id:
            messagebox.showerror("错误", "设备ID不可用")
            return

        bound_username = self._get_bound_username()
        if not bound_username:
            messagebox.showerror("错误", "未绑定用户，无法修改密码")
            return

        new_password = self.change_new_password_entry.get()
        confirm_password = self.change_confirm_password_entry.get()

        if len(new_password) < 4:
            messagebox.showerror("错误", "新密码至少需要4个字符")
            return

        if new_password != confirm_password:
            messagebox.showerror("错误", "两次输入的密码不一致")
            return

        # Disable button during operation
        original_text = self.change_password_btn.text
        self.change_password_btn.text = "⏳ 修改中..."
        self.change_password_btn.config(state='disabled')

        try:
            result = self.api_client.change_password_by_device(bound_username, new_password, self.device_id)
            if result.get('code') == 0:
                messagebox.showinfo("成功", "✅ 密码修改成功！")
                self._clear_change_password_inputs()
            else:
                messagebox.showerror("错误", translate_error(result.get('msg', '修改密码失败')))
        except Exception as e:
            messagebox.showerror("错误", str(e))
        finally:
            self.change_password_btn.text = original_text
            self.change_password_btn.config(state='normal')
            self.change_password_btn._draw()

    def _clear_change_password_inputs(self):
        """Clear change password form inputs."""
        if self.change_new_password_entry:
            self.change_new_password_entry.entry.delete(0, tk.END)
        if self.change_confirm_password_entry:
            self.change_confirm_password_entry.entry.delete(0, tk.END)

    def _get_bound_username(self) -> str:
        """Get the currently bound username from the status label."""
        if self.bound_user_label:
            text = self.bound_user_label.cget("text")
            if text.startswith("已绑定: "):
                return text.replace("已绑定: ", "")
        return ""

    def set_bound_user(self, username: str) -> None:
        if self.bound_user_label:
            if username:
                self.bound_user_label.config(text=f"已绑定: {username}", fg=SUCCESS)
                self.status_icon.config(text="✅")
                self.status_badge.config(text=f"  {username}  ", bg=STATUS_ONLINE)
            else:
                self.bound_user_label.config(text="未绑定用户", fg=LIGHT_TEXT)
                self.status_icon.config(text="❌")
                self.status_badge.config(text="  未绑定  ", bg=STATUS_OFFLINE)

        # Update change password tab username display
        if self.change_username_label:
            if username:
                self.change_username_label.config(text=username, fg=LIGHT_TEXT)
            else:
                self.change_username_label.config(text="未绑定用户", fg=LIGHT_TEXT_SECONDARY)

        # Enable/disable change password button based on binding status
        if self.change_password_btn:
            if username:
                self.change_password_btn.config(state='normal')
            else:
                self.change_password_btn.config(state='disabled')
