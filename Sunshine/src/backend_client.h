/**
 * @file src/backend_client.h
 * @brief Declarations for the backend client module (Phase 2: V4 ID鐩磋繛涓叉祦鏂规)
 *
 * This module handles Sunshine's communication with the backend server:
 * - Device registration (with Sunshine-generated device_id)
 * - Device ID persistence (to sunshine_server.ini)
 * - Heartbeat keepalive (every 20 seconds)
 * - PIN polling (every 5 seconds)
 *
 * Configuration file: <sunshine.exe鎵€鍦ㄧ洰褰?/config/sunshine_server.ini
 */
#pragma once

#include <string>
#include <atomic>

namespace backend {

  struct status_t {
    std::string device_id;
    std::string device_password;
    std::string current_pin;
    bool connected;
  };

  void start();
  void stop();

  status_t get_status();
}
