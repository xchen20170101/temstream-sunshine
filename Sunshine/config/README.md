# Sunshine Configuration Directory

This directory contains configuration files for Sunshine server management and communication.

## Files

### sunshine_server.ini
- **Purpose**: Stores server communication, Tailscale configuration, and auto-login settings
- **Format**: Key=value format with comments (lines starting with # are ignored)
- **Configuration Options**:
  - `server_ip`: IP address of the management server (default: `127.0.0.1`)
  - `isTailscale`: Enable/disable Tailscale IP usage (`true`/`false`, default: `true`)
  - `tailscale_authkey`: Tailscale authentication key for auto-login (leave empty to disable)
  - `headscale_url`: Headscale server URL for auto-login (leave empty to disable)

- **Usage**: Used by Sunshine to:
  - Communicate with the backend management system for PIN code retrieval
  - Report device status (e.g., Tailscale IP updates)
  - Enable centralized device management
  - Automatically login to Tailscale network on startup

- **Example**:
  ```
  # Sunshine Server Configuration
  # Server IP address for management system communication
  server_ip = 192.168.1.100

  # Enable Tailscale IP usage
  isTailscale = true

  # Tailscale auto-login configuration
  tailscale_authkey = tskey-auth-XXXXXXXXXXXXXXXXXXXXXXXXXXXXX
  headscale_url = https://headscale.example.com
  ```

## Migration Notes

- **From `conf_sunshine.ini`**: If you have an existing `conf_sunshine.ini` file in the root directory, it will be automatically migrated to the new format on first run
- **From `sunshine.conf`**: The `isTailscale` configuration has been moved from `sunshine.conf` to this file for better organization
- **Backward Compatibility**: Old configurations are preserved and migrated automatically

## Tailscale Auto-Login

When both `tailscale_authkey` and `headscale_url` are configured, Sunshine will automatically execute the following command on startup:

```bash
tailscale.exe up --login-server "<headscale_url>" --authkey "<tailscale_authkey>" --accept-dns=false
```

This feature allows Sunshine to automatically connect to your Tailscale network without manual intervention.
