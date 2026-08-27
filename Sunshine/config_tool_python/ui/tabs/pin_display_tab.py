"""
Device display tab with modern styling.

This module handles the Device Display tab UI for showing device ID
and configuring device password.
"""

import tkinter as tk
from tkinter import messagebox
from typing import TYPE_CHECKING, Callable, Optional

if TYPE_CHECKING:
    from config.sunshine_server_ini import SunshineServerIni

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from ui.theme import (
    PRIMARY, PRIMARY_HOVER, PRIMARY_LIGHT, SUCCESS, SUCCESS_LIGHT,
    DARK_BG, DARK_SECONDARY, DARK_CARD, DARK_BORDER, DARK_TEXT, DARK_TEXT_SECONDARY,
    LIGHT_BG, LIGHT_SECONDARY, LIGHT_BORDER, LIGHT_TEXT, LIGHT_TEXT_SECONDARY,
    FONT_FAMILY, FONT_SIZE_XS, FONT_SIZE_SM, FONT_SIZE_MD, FONT_SIZE_LG, FONT_SIZE_XL, FONT_SIZE_XXL,
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


class ModernButton(tk.Canvas):
    """Custom rounded button widget."""

    def __init__(self, parent, text, command, width=120, height=40,
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
            w, h = 120, 40

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


class PinDisplayTab:
    """Device display tab showing device ID and password settings."""

    def __init__(self):
        self.server_ini: Optional['SunshineServerIni'] = None
        self.save_callback: Optional[Callable[[], None]] = None

        self.frame = None
        self.device_id_label = None
        self.current_password_entry = None
        self.new_password_entry = None
        self.confirm_password_entry = None
        self.show_password_btn = None
        self.show_password_state = False

    def set_save_callback(self, callback: Callable[[], None]) -> None:
        self.save_callback = callback

    def set_server_ini(self, server_ini: 'SunshineServerIni') -> None:
        self.server_ini = server_ini
        self._update_password_display()

    def _update_password_display(self) -> None:
        if self.current_password_entry and self.server_ini:
            self.current_password_entry.entry.delete(0, tk.END)
            password = self.server_ini.device_password or ""
            self.current_password_entry.entry.insert(0, password)
            self.current_password_entry.entry.config(fg=LIGHT_TEXT)
            self.current_password_entry.has_placeholder = False

    def _toggle_show_password(self) -> None:
        self.show_password_state = not self.show_password_state

        if self.show_password_state:
            self.current_password_entry.entry.config(show="")
            self.show_password_btn.itemconfig("text", text="🔒 隐藏")
        else:
            self.current_password_entry.entry.config(show="●")
            self.show_password_btn.itemconfig("text", text="👁 显示")

    def _on_save_clicked(self) -> None:
        if not self.server_ini:
            messagebox.showwarning("警告", "配置未初始化")
            return

        new_password = self.new_password_entry.get().strip() if self.new_password_entry else ""
        confirm_password = self.confirm_password_entry.get().strip() if self.confirm_password_entry else ""

        if new_password or confirm_password:
            if new_password != confirm_password:
                messagebox.showerror("错误", "新密码与确认密码不一致")
                return

        if new_password:
            self.server_ini.device_password = new_password
        elif confirm_password == "" and new_password == "":
            pass

        if self.save_callback:
            self.save_callback()
            messagebox.showinfo("提示", "密码已保存，需重启 Sunshine 服务使配置生效")
        else:
            messagebox.showwarning("警告", "保存函数未配置")

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

        # Device ID display
        device_id_frame = tk.Frame(device_card.content, bg=LIGHT_SECONDARY)
        device_id_frame.pack(fill="x", pady=(0, MARGIN_MD))

        # Icon
        icon_label = tk.Label(
            device_id_frame,
            text="📱",
            font=("Segoe UI Emoji", 32),
            bg=LIGHT_SECONDARY
        )
        icon_label.pack(side="left", padx=(0, MARGIN_MD))

        # Info
        info_frame = tk.Frame(device_id_frame, bg=LIGHT_SECONDARY)
        info_frame.pack(side="left", fill="both", expand=True)

        tk.Label(
            info_frame,
            text="设备 ID",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT_SECONDARY
        ).pack(anchor="w")

        self.device_id_label = tk.Label(
            info_frame,
            text="--",
            font=(FONT_FAMILY, FONT_SIZE_XXL, "bold"),
            bg=LIGHT_SECONDARY, fg=PRIMARY
        )
        self.device_id_label.pack(anchor="w", pady=(2, 0))

        # Status badge
        status_frame = tk.Frame(device_id_frame, bg=LIGHT_SECONDARY)
        status_frame.pack(side="right", padx=(MARGIN_MD, 0))

        self.status_badge = tk.Label(
            status_frame,
            text="  等待注册  ",
            font=(FONT_FAMILY, FONT_SIZE_SM, "bold"),
            bg=STATUS_PENDING,
            fg="white",
            bd=0,
            padx=12, pady=4
        )
        self.status_badge.pack(anchor="e")

        # Hint text
        hint_label = tk.Label(
            device_card.content,
            text="💡 设备 ID 用于在管理后台注册此设备",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT_SECONDARY
        )
        hint_label.pack(fill="x")

        # === PASSWORD CARD ===
        password_card = Card(scroll_frame, title="设备密码设置")
        password_card.pack(fill="x", padx=MARGIN_SM, pady=(0, MARGIN_MD))

        # Current password
        current_pw_row = tk.Frame(password_card.content, bg=LIGHT_SECONDARY)
        current_pw_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            current_pw_row,
            text="当前密码",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT,
            width=12, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.current_password_entry = ModernEntry(current_pw_row, placeholder="输入当前密码", width=20, show="●")
        self.current_password_entry.pack(side="left", fill="x", expand=True, padx=(0, MARGIN_SM))

        self.show_password_btn = ModernButton(
            current_pw_row,
            text="👁 显示",
            command=self._toggle_show_password,
            width=100, height=36,
            bg_color=LIGHT_BG, hover_color=LIGHT_BORDER, text_color=LIGHT_TEXT
        )
        self.show_password_btn.pack(side="left")

        # New password
        new_pw_row = tk.Frame(password_card.content, bg=LIGHT_SECONDARY)
        new_pw_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            new_pw_row,
            text="新密码",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT,
            width=12, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.new_password_entry = ModernEntry(new_pw_row, placeholder="留空则保持不变", width=20, show="●")
        self.new_password_entry.pack(side="left", fill="x", expand=True)

        # Confirm password
        confirm_pw_row = tk.Frame(password_card.content, bg=LIGHT_SECONDARY)
        confirm_pw_row.pack(fill="x", pady=(0, MARGIN_MD))

        tk.Label(
            confirm_pw_row,
            text="确认密码",
            font=(FONT_FAMILY, FONT_SIZE_MD),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT,
            width=12, anchor="w"
        ).pack(side="left", padx=(0, MARGIN_MD))

        self.confirm_password_entry = ModernEntry(confirm_pw_row, placeholder="再次输入新密码", width=20, show="●")
        self.confirm_password_entry.pack(side="left", fill="x", expand=True)

        # Hint
        hint_label2 = tk.Label(
            password_card.content,
            text="💡 修改密码后需重启 Sunshine 服务使配置生效",
            font=(FONT_FAMILY, FONT_SIZE_SM),
            bg=LIGHT_SECONDARY, fg=LIGHT_TEXT_SECONDARY
        )
        hint_label2.pack(fill="x")

        # === ACTION BUTTONS ===
        button_frame = tk.Frame(scroll_frame, bg=LIGHT_BG)
        button_frame.pack(fill="x", padx=MARGIN_SM, pady=(MARGIN_LG, 0))

        self.save_btn = ModernButton(
            button_frame,
            text="💾 保存密码",
            command=self._on_save_clicked,
            width=150, height=44
        )
        self.save_btn.pack(side="left")

        return self.frame

    def set_device_id(self, device_id: str) -> None:
        if self.device_id_label:
            if device_id:
                self.device_id_label.config(text=device_id, fg=PRIMARY)
                self.status_badge.config(text="  已注册  ", bg=STATUS_ONLINE)
            else:
                self.device_id_label.config(text="--", fg=LIGHT_TEXT_SECONDARY)
                self.status_badge.config(text="  等待注册  ", bg=STATUS_PENDING)
