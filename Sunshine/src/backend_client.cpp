/**
 * @file src/backend_client.cpp
 * @brief Definitions for the backend client module (Phase 2: V4 ID鐩磋繛涓叉祦鏂规)
 *
 * This module handles Sunshine's communication with the backend server:
 * - Device registration (with Sunshine-generated device_id)
 * - Device ID persistence (to sunshine_server.ini)
 * - Heartbeat keepalive (every 20 seconds)
 * - PIN polling (every 5 seconds)
 *
 * Configuration file: <sunshine.exe鎵€鍦ㄧ洰褰?/config/sunshine_server.ini
 */
#define BOOST_BIND_GLOBAL_PLACEHOLDERS

#include <atomic>
#include <chrono>
#include <cstdlib>
#include <fstream>
#include <sstream>
#include <thread>
#include <mutex>
#include <unordered_map>
#include <random>
#include <iomanip>

// local includes
#include "backend_client.h"
#include "logging.h"
#include "nvhttp.h"

#ifdef _WIN32
#include <WinSock2.h>
#include <WS2tcpip.h>
#include <windows.h>
#else
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#endif

namespace backend {

  static std::atomic<bool> g_running {false};
  static std::atomic<bool> g_connected {false};
  static std::string g_device_id;
  static std::string g_device_password;
  static std::string g_current_pin;
  static std::mutex g_mutex;

  // =========================================================================
  // Config file path & parsing
  // =========================================================================

  // Get the config directory: <sunshine.exe鎵€鍦ㄧ洰褰?/config
  static std::string get_config_dir() {
    std::string config_dir;
#ifdef _WIN32
    char exe_path[MAX_PATH] = {0};
    if (GetModuleFileNameA(nullptr, exe_path, MAX_PATH) != 0) {
      std::string path(exe_path);
      size_t pos = path.find_last_of("\\/");
      if (pos != std::string::npos) {
        config_dir = path.substr(0, pos) + "\\config";
      }
    }
#else
    char exe_path[PATH_MAX] = {0};
    ssize_t len = readlink("/proc/self/exe", exe_path, sizeof(exe_path) - 1);
    if (len > 0) {
      exe_path[len] = '\0';
      std::string path(exe_path);
      size_t pos = path.find_last_of("/");
      if (pos != std::string::npos) {
        config_dir = path.substr(0, pos) + "/config";
      }
    }
#endif
    return config_dir;
  }

  static std::string get_config_file_path() {
    return get_config_dir() + "/sunshine_server.ini";
  }

  // Parse the config file and return key-value map
  static std::unordered_map<std::string, std::string> parseConfigFile(const std::string &path) {
    std::unordered_map<std::string, std::string> vars;
    std::ifstream file(path);
    if (!file.is_open()) {
      return vars;
    }

    std::string line;
    while (std::getline(file, line)) {
      // Skip comments and empty lines
      if (line.empty() || line[0] == '#') continue;

      auto pos = line.find('=');
      if (pos != std::string::npos) {
        std::string key = line.substr(0, pos);
        std::string value = line.substr(pos + 1);

        // Trim whitespace
        auto trim = [](std::string &s) {
          size_t start = s.find_first_not_of(" \t");
          size_t end = s.find_last_not_of(" \t\r\n");
          if (start == std::string::npos) { s.clear(); return; }
          s = s.substr(start, end - start + 1);
        };
        trim(key);
        trim(value);

        if (!key.empty()) {
          vars[key] = value;
        }
      }
    }
    file.close();
    return vars;
  }

  // Read a string config value
  static std::string get_config_string(const std::string &key, const std::string &fallback = "") {
    static std::unordered_map<std::string, std::string> cache;
    static bool loaded = false;
    static std::mutex cache_mutex;

    std::lock_guard<std::mutex> lock(cache_mutex);
    if (!loaded) {
      std::string path = get_config_file_path();
      if (!path.empty()) {
        cache = parseConfigFile(path);
      }
      loaded = true;
    }

    auto it = cache.find(key);
    if (it != cache.end() && !it->second.empty()) {
      return it->second;
    }
    return fallback;
  }

  // =========================================================================
  // Config access helpers
  // =========================================================================

  static std::string get_backend_host() {
    return get_config_string("server_ip", "127.0.0.1");
  }

  static int get_backend_port() {
    std::string val = get_config_string("server_port", "12345");
    try {
      return std::stoi(val);
    } catch (...) {
      return 12345;
    }
  }

  static std::string get_device_id_from_config() {
    return get_config_string("device_id", "");
  }

  static std::string get_device_password_from_config() {
    return get_config_string("device_password", "");
  }

  // =========================================================================
  // Device ID generation
  // =========================================================================

  // Generate a random 8-digit device_id (00000001 ~ 99999999)
  static std::string generate_device_id() {
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(1, 99999999);
    int id = dis(gen);
    std::ostringstream oss;
    oss << std::setw(8) << std::setfill('0') << id;
    return oss.str();
  }

  // Generate a random 6-character device_password (alphanumeric)
  static std::string generate_device_password() {
    const char charset[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, sizeof(charset) - 2); // -2 to exclude null terminator
    std::string password;
    for (int i = 0; i < 6; ++i) {
      password += charset[dis(gen)];
    }
    return password;
  }

  // =========================================================================
  // TCP communication helpers
  // =========================================================================

  static std::string cleanMessage(const std::string &msg) {
    std::string cleaned;
    for (char c : msg) {
      if (c != '\0' && c != '\n' && c != '\r') {
        cleaned += c;
      }
    }
    return cleaned;
  }

  static std::string sendTcpMessage(const std::string &target) {
    std::string cleanedTarget = cleanMessage(target);
    if (cleanedTarget.empty()) {
      return "";
    }

    std::string authPrefix = "SUNSHINE_API_KEY:" + std::string(nvhttp::DEVICE_API_KEY) + ":";
    cleanedTarget = authPrefix + cleanedTarget;

    std::string host = get_backend_host();
    int port = get_backend_port();

    if (host.empty()) {
      BOOST_LOG(warning) << "backend_client: backend_host is not configured";
      return "";
    }

    std::string response;

#ifdef _WIN32
    WSADATA wsaData;
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
      BOOST_LOG(error) << "backend_client: WSAStartup failed";
      return "";
    }

    SOCKET sockfd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (sockfd == INVALID_SOCKET) {
      BOOST_LOG(error) << "backend_client: socket creation failed: " << WSAGetLastError();
      WSACleanup();
      return "";
    }

    struct sockaddr_in serverAddr {};
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons((u_short)port);
    if (inet_pton(AF_INET, host.c_str(), &serverAddr.sin_addr) != 1) {
      BOOST_LOG(error) << "backend_client: invalid backend IP: " << host;
      closesocket(sockfd);
      WSACleanup();
      return "";
    }

    if (connect(sockfd, (struct sockaddr *)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR) {
      BOOST_LOG(warning) << "backend_client: connect to " << host << ":" << port << " failed: " << WSAGetLastError();
      closesocket(sockfd);
      WSACleanup();
      return "";
    }

    int sent = send(sockfd, cleanedTarget.c_str(), (int)cleanedTarget.length(), 0);
    if (sent == SOCKET_ERROR) {
      BOOST_LOG(error) << "backend_client: send failed: " << WSAGetLastError();
      closesocket(sockfd);
      WSACleanup();
      return "";
    }

    char buffer[1024] = {0};
    int received = recv(sockfd, buffer, sizeof(buffer) - 1, 0);
    if (received > 0) {
      response = std::string(buffer, received);
    }

    closesocket(sockfd);
    WSACleanup();
#else
    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
      BOOST_LOG(error) << "backend_client: socket creation failed";
      return "";
    }

    struct sockaddr_in serverAddr {};
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons((uint16_t)port);
    if (inet_pton(AF_INET, host.c_str(), &serverAddr.sin_addr) != 1) {
      BOOST_LOG(error) << "backend_client: invalid backend IP: " << host;
      close(sockfd);
      return "";
    }

    if (connect(sockfd, (struct sockaddr *)&serverAddr, sizeof(serverAddr)) < 0) {
      BOOST_LOG(warning) << "backend_client: connect to " << host << ":" << port << " failed";
      close(sockfd);
      return "";
    }

    ssize_t sent = send(sockfd, cleanedTarget.c_str(), cleanedTarget.length(), 0);
    if (sent < 0) {
      BOOST_LOG(error) << "backend_client: send failed";
      close(sockfd);
      return "";
    }

    char buffer[1024] = {0};
    ssize_t received = recv(sockfd, buffer, sizeof(buffer) - 1, 0);
    if (received > 0) {
      response = std::string(buffer, received);
    }

    close(sockfd);
#endif

    return response;
  }

  // =========================================================================
  // Device ID persistence (to sunshine_config.ini)
  // =========================================================================

  static void saveDeviceIdToConfig(const std::string &device_id) {
    std::string config_dir = get_config_dir();
    std::string config_path = get_config_file_path();

    if (config_dir.empty()) {
      BOOST_LOG(warning) << "backend_client: cannot determine config directory";
      return;
    }

    // Ensure directory exists
#ifdef _WIN32
    std::string mk_dir_cmd = "mkdir /Q /S \"" + config_dir + "\"";
#else
    std::string mk_dir_cmd = "mkdir -p \"" + config_dir + "\"";
#endif

    if (system(mk_dir_cmd.c_str()) != 0) {
      // Try creating directory manually
#ifdef _WIN32
      CreateDirectoryA(config_dir.c_str(), nullptr);
#endif
    }

    // Read existing content
    std::string content;
    std::ifstream inFile(config_path);
    if (inFile.is_open()) {
      std::stringstream buf;
      buf << inFile.rdbuf();
      content = buf.str();
      inFile.close();
    }

    // Update or append device_id
    bool found = false;
    std::istringstream iss(content);
    std::string line;
    std::ostringstream out;

    while (std::getline(iss, line)) {
      std::string trimmed = line;
      // Trim leading whitespace
      size_t start = trimmed.find_first_not_of(" \t");
      if (start > 0 && start != std::string::npos) {
        trimmed = trimmed.substr(start);
      }
      // Trim trailing whitespace
      size_t end = trimmed.find_last_not_of(" \t\r\n");
      if (end != std::string::npos && end < trimmed.size() - 1) {
        trimmed = trimmed.substr(0, end + 1);
      }

      if (trimmed.find("device_id") == 0) {
        out << "device_id = " << device_id << "\n";
        found = true;
      } else {
        out << line << "\n";
      }
    }

    if (!found) {
      out << "device_id = " << device_id << "\n";
    }

    std::ofstream outFile(config_path);
    if (outFile.is_open()) {
      outFile << out.str();
      outFile.close();
      BOOST_LOG(info) << "backend_client: saved device_id = " << device_id << " to " << config_path;
    } else {
      BOOST_LOG(error) << "backend_client: failed to write config: " << config_path;
    }
  }

  // Save device_password to config file
  static void saveDevicePasswordToConfig(const std::string &device_password) {
    std::string config_dir = get_config_dir();
    std::string config_path = get_config_file_path();

    if (config_dir.empty()) {
      BOOST_LOG(warning) << "backend_client: cannot determine config directory";
      return;
    }

    // Ensure directory exists
#ifdef _WIN32
    std::string mk_dir_cmd = "mkdir /Q /S \"" + config_dir + "\"";
#else
    std::string mk_dir_cmd = "mkdir -p \"" + config_dir + "\"";
#endif

    if (system(mk_dir_cmd.c_str()) != 0) {
#ifdef _WIN32
      CreateDirectoryA(config_dir.c_str(), nullptr);
#endif
    }

    // Read existing content
    std::string content;
    std::ifstream inFile(config_path);
    if (inFile.is_open()) {
      std::stringstream buf;
      buf << inFile.rdbuf();
      content = buf.str();
      inFile.close();
    }

    // Update or append device_password
    bool found = false;
    std::istringstream iss(content);
    std::string line;
    std::ostringstream out;

    while (std::getline(iss, line)) {
      std::string trimmed = line;
      // Trim leading whitespace
      size_t start = trimmed.find_first_not_of(" \t");
      if (start > 0 && start != std::string::npos) {
        trimmed = trimmed.substr(start);
      }
      // Trim trailing whitespace
      size_t end = trimmed.find_last_not_of(" \t\r\n");
      if (end != std::string::npos && end < trimmed.size() - 1) {
        trimmed = trimmed.substr(0, end + 1);
      }

      if (trimmed.find("device_password") == 0) {
        out << "device_password = " << device_password << "\n";
        found = true;
      } else {
        out << line << "\n";
      }
    }

    if (!found) {
      out << "device_password = " << device_password << "\n";
    }

    std::ofstream outFile(config_path);
    if (outFile.is_open()) {
      outFile << out.str();
      outFile.close();
      BOOST_LOG(info) << "backend_client: saved device_password = " << device_password << " to " << config_path;
    } else {
      BOOST_LOG(error) << "backend_client: failed to write config: " << config_path;
    }
  }

  // =========================================================================
  // Core operations
  // =========================================================================

  static bool doRegister() {
    std::string hostname = nvhttp::getHostname();
    if (hostname.empty() || hostname == "Failed to get hostname.") {
      hostname = "UnknownHost";
    }

    std::string reportedIp = nvhttp::getReportedIp();
    std::string currentDeviceId;
    std::string currentDevicePassword;
    {
      std::lock_guard<std::mutex> lock(g_mutex);
      currentDeviceId = g_device_id;
      currentDevicePassword = g_device_password;
    }
    if (currentDeviceId.empty()) {
      currentDeviceId = get_device_id_from_config();
    }
    if (currentDevicePassword.empty()) {
      currentDevicePassword = get_device_password_from_config();
    }

    // 濡傛灉浠嶇劧涓虹┖锛岀敓鎴愬苟淇濆瓨
    if (currentDeviceId.empty()) {
      currentDeviceId = generate_device_id();
      saveDeviceIdToConfig(currentDeviceId);
      BOOST_LOG(info) << "backend_client: generated new device_id = " << currentDeviceId;
      {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_device_id = currentDeviceId;
      }
    }

    // 濡傛灉浠嶇劧涓虹┖锛岀敓鎴愬苟淇濆瓨 device_password
    if (currentDevicePassword.empty()) {
      currentDevicePassword = generate_device_password();
      saveDevicePasswordToConfig(currentDevicePassword);
      BOOST_LOG(info) << "backend_client: generated new device_password = " << currentDevicePassword;
      {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_device_password = currentDevicePassword;
      }
    }

    std::string message;
    if (!reportedIp.empty()) {
      // 鏂版牸寮? register:<hostname>:<ip>:<deviceId>:<devicePassword>
      message = "register:" + hostname + ":" + reportedIp + ":" + currentDeviceId + ":" + currentDevicePassword;
    } else {
      message = "register:" + hostname + "::" + currentDeviceId + ":" + currentDevicePassword;
    }

    BOOST_LOG(info) << "backend_client: registering with device_id = " << currentDeviceId << ", device_password = " << currentDevicePassword;

    std::string response = sendTcpMessage(message);
    if (response.empty()) {
      BOOST_LOG(warning) << "backend_client: registration failed - no response";
      return false;
    }

    std::string cleaned = cleanMessage(response);
    // 鎴愬姛鍝嶅簲鍙鏌?"OK"
    if (cleaned == "OK") {
      BOOST_LOG(info) << "backend_client: registration succeeded";
      return true;
    }

    // 澶勭悊閿欒鍝嶅簲
    if (cleaned.find("ERROR:") == 0) {
      BOOST_LOG(error) << "backend_client: registration error: " << cleaned;
      return false;
    }

    BOOST_LOG(warning) << "backend_client: registration unexpected response: " << cleaned;
    return false;
  }

  static bool doKeepalive() {
    std::string reportedIp = nvhttp::getReportedIp();

    std::string currentId;
    std::string currentPassword;
    {
      std::lock_guard<std::mutex> lock(g_mutex);
      currentId = g_device_id;
      currentPassword = g_device_password;
    }
    if (currentId.empty()) {
      currentId = get_device_id_from_config();
    }
    if (currentPassword.empty()) {
      currentPassword = get_device_password_from_config();
    }

    std::string message;
    if (!reportedIp.empty()) {
      // 鏂版牸寮? keepalive:<ip>:<deviceId>:<devicePassword>
      message = "keepalive:" + reportedIp + ":" + currentId + ":" + currentPassword;
    } else {
      message = "keepalive::" + currentId + ":" + currentPassword;
    }

    std::string response = sendTcpMessage(message);
    return !response.empty();
  }

  static std::string doPollPin() {
    std::string currentId;
    {
      std::lock_guard<std::mutex> lock(g_mutex);
      currentId = g_device_id;
    }
    if (currentId.empty()) {
      currentId = get_device_id_from_config();
    }

    std::string message = "get pin:" + currentId;
    std::string response = sendTcpMessage(message);

    if (response.empty()) {
      return "";
    }

    std::string cleaned = cleanMessage(response);
    if (cleaned.size() == 4 && std::all_of(cleaned.begin(), cleaned.end(), ::isdigit)) {
      return cleaned;
    }
    return "";
  }

  // =========================================================================
  // Threads
  // =========================================================================

  static void registrationThread() {
    BOOST_LOG(info) << "backend_client: registration thread started";
    bool registered = doRegister();
    {
      std::lock_guard<std::mutex> lock(g_mutex);
      g_connected = registered;
    }
    BOOST_LOG(info) << "backend_client: registration " << (registered ? "succeeded" : "failed");
  }

  static void heartbeatThread() {
    BOOST_LOG(info) << "backend_client: heartbeat thread started";
    while (g_running) {
      std::this_thread::sleep_for(std::chrono::seconds(20));
      if (!g_running) break;

      bool ok = doKeepalive();
      std::lock_guard<std::mutex> lock(g_mutex);
      g_connected = ok;
    }
    BOOST_LOG(info) << "backend_client: heartbeat thread stopped";
  }

  static void pinPollingThread() {
    BOOST_LOG(info) << "backend_client: PIN polling thread started";
    while (g_running) {
      std::this_thread::sleep_for(std::chrono::seconds(5));
      if (!g_running) break;

      std::string pin = doPollPin();
      if (!pin.empty()) {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_current_pin = pin;
      }
    }
    BOOST_LOG(info) << "backend_client: PIN polling thread stopped";
  }

  // =========================================================================
  // Public API
  // =========================================================================

  void start() {
    if (g_running.exchange(true)) {
      BOOST_LOG(warning) << "backend_client: already running";
      return;
    }

    std::string host = get_backend_host();
    if (host.empty()) {
      BOOST_LOG(warning) << "backend_client: backend_host is not configured, skipping backend client";
      g_running = false;
      return;
    }

    // Check or generate device_id at startup
    std::string existingId = get_device_id_from_config();
    if (existingId.empty()) {
      existingId = generate_device_id();
      saveDeviceIdToConfig(existingId);
      BOOST_LOG(info) << "backend_client: generated new device_id = " << existingId;
    } else {
      BOOST_LOG(info) << "backend_client: using existing device_id = " << existingId;
    }

    // Check or generate device_password at startup
    std::string existingPassword = get_device_password_from_config();
    if (existingPassword.empty()) {
      existingPassword = generate_device_password();
      saveDevicePasswordToConfig(existingPassword);
      BOOST_LOG(info) << "backend_client: generated new device_password = " << existingPassword;
    } else {
      BOOST_LOG(info) << "backend_client: using existing device_password = " << existingPassword;
    }

    {
      std::lock_guard<std::mutex> lock(g_mutex);
      g_device_id = existingId;
      g_device_password = existingPassword;
    }

    std::thread(&heartbeatThread).detach();
    std::thread(&pinPollingThread).detach();
    registrationThread();
  }

  void stop() {
    g_running = false;
    std::lock_guard<std::mutex> lock(g_mutex);
    g_device_id.clear();
    g_device_password.clear();
    g_current_pin.clear();
    g_connected = false;
    BOOST_LOG(info) << "backend_client: stopped";
  }

  status_t get_status() {
    status_t status;
    std::lock_guard<std::mutex> lock(g_mutex);
    status.device_id = g_device_id;
    status.device_password = g_device_password;
    status.current_pin = g_current_pin;
    status.connected = g_connected;
    return status;
  }

}  // namespace backend
