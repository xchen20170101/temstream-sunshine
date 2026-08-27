"""
Theme configuration for Sunshine Config Tool.

Defines colors, fonts, and styles for a modern dark sidebar + light content layout.
"""

# =============================================================================
# Color Palette - Modern Teal/Green Theme
# =============================================================================

# Primary colors
PRIMARY = "#20c997"           # Main teal accent
PRIMARY_HOVER = "#1ba87c"     # Darker teal for hover
PRIMARY_LIGHT = "#e8f8f4"     # Light teal for backgrounds
SUCCESS = "#198754"           # Green for success states
SUCCESS_LIGHT = "#d1e7dd"     # Light green background
WARNING = "#ffc107"           # Amber warning
DANGER = "#dc3545"            # Red for errors
DANGER_LIGHT = "#f8d7da"     # Light red background
INFO = "#0dcaf0"              # Cyan info

# Dark theme colors (for sidebar)
DARK_BG = "#1a1d23"           # Very dark blue-gray
DARK_SECONDARY = "#212529"    # Secondary dark
DARK_CARD = "#2d3239"         # Card background
DARK_BORDER = "#3d444d"       # Border color
DARK_TEXT = "#ffffff"         # White text
DARK_TEXT_SECONDARY = "#a1a1a1"  # Muted text

# Light theme colors (for content area)
LIGHT_BG = "#f8f9fa"          # Very light gray
LIGHT_SECONDARY = "#ffffff"   # White cards
LIGHT_BORDER = "#dee2e6"      # Light border
LIGHT_TEXT = "#212529"        # Dark text
LIGHT_TEXT_SECONDARY = "#6c757d"  # Muted gray text

# Sidebar specific
SIDEBAR_WIDTH = 200
SIDEBAR_ICON_SIZE = 24
SIDEBAR_ITEM_HEIGHT = 48
SIDEBAR_ACTIVE_BG = "#2d3239"

# =============================================================================
# Typography
# =============================================================================

FONT_FAMILY = "SimSun"          # Classic Chinese serif font

# Font sizes
FONT_SIZE_XS = 9
FONT_SIZE_SM = 10
FONT_SIZE_MD = 11
FONT_SIZE_LG = 13
FONT_SIZE_XL = 16
FONT_SIZE_XXL = 20

# Font weights
FONT_NORMAL = "normal"
FONT_BOLD = "bold"

# =============================================================================
# Spacing & Sizing
# =============================================================================

# Padding
PADDING_XS = 4
PADDING_SM = 8
PADDING_MD = 12
PADDING_LG = 16
PADDING_XL = 24

# Margins
MARGIN_SM = 8
MARGIN_MD = 16
MARGIN_LG = 24

# Border radius
RADIUS_SM = 4
RADIUS_MD = 8
RADIUS_LG = 12
RADIUS_XL = 16

# Button sizes
BUTTON_HEIGHT = 36
BUTTON_HEIGHT_SM = 28
BUTTON_HEIGHT_LG = 44

# Input sizes
INPUT_HEIGHT = 36

# =============================================================================
# Shadow definitions (for canvas-based shadows)
# =============================================================================

SHADOW_COLOR = "#000000"
SHADOW_ALPHA = 0.15
SHADOW_OFFSET = 2
SHADOW_BLUR = 8

# =============================================================================
# Animation durations (ms)
# =============================================================================

ANIM_FAST = 100
ANIM_NORMAL = 200
ANIM_SLOW = 300

# =============================================================================
# Status colors
# =============================================================================

STATUS_ONLINE = SUCCESS
STATUS_OFFLINE = DANGER
STATUS_PENDING = WARNING

# =============================================================================
# Helper functions
# =============================================================================

def hex_to_rgb(hex_color: str) -> tuple:
    """Convert hex color to RGB tuple."""
    hex_color = hex_color.lstrip('#')
    return tuple(int(hex_color[i:i+2], 16) for i in (0, 2, 4))

def rgb_to_hex(r: int, g: int, b: int) -> str:
    """Convert RGB tuple to hex color."""
    return f"#{r:02x}{g:02x}{b:02x}"

def adjust_brightness(hex_color: str, factor: float) -> str:
    """Adjust color brightness by factor (0-2)."""
    r, g, b = hex_to_rgb(hex_color)
    r = max(0, min(255, int(r * factor)))
    g = max(0, min(255, int(g * factor)))
    b = max(0, min(255, int(b * factor)))
    return rgb_to_hex(r, g, b)
