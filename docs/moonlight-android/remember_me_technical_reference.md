# "记住我"功能技术参考

## 🔧 核心实现

### 数据存储
**方式**: SharedPreferences  
**位置**: `UserAuthManager` 类  
**Preference 名称**: `user_auth`

### 关键字段
```java
private static final String KEY_USERNAME = "username";     // 用户名
private static final String KEY_REMEMBER_ME = "remember_me"; // 记住我状态
private static final String KEY_TOKEN = "token";           // 认证令牌
private static final String KEY_EXPIRY = "expiry";         // 令牌过期时间
```

---

## 📋 核心方法

### 1. 保存登录信息
**方法**: `UserAuthManager.saveLoginInfo(String username, String token, long expiry, boolean rememberMe)`

**逻辑**:
```java
if (rememberMe) {
    // 保存用户名
    editor.putString(KEY_USERNAME, username);
} else {
    // 清除用户名
    editor.remove(KEY_USERNAME);
}
```

**调用位置**: `LoginActivity.performLogin()` - 登录成功后

---

### 2. 加载记住的用户名
**方法**: `LoginActivity.loadRememberedUsername()`

**逻辑**:
```java
if (authManager.isRememberMe()) {
    String savedUsername = authManager.getUsername();
    if (!TextUtils.isEmpty(savedUsername)) {
        editTextUsername.setText(savedUsername);
        checkBoxRememberMe.setChecked(true);
    }
}
```

**调用位置**: `LoginActivity.onCreate()` - 初始化界面时

---

### 3. 清除登录信息（保留用户名）
**方法**: `UserAuthManager.clearLoginInfo()`

**逻辑**:
```java
editor.remove(KEY_TOKEN);
editor.remove(KEY_EXPIRY);
// 不清除 KEY_USERNAME 和 KEY_REMEMBER_ME
```

**用途**: 退出登录、Token 过期时调用

---

### 4. 清除所有信息（包括用户名）
**方法**: `UserAuthManager.clearAllInfo()`

**逻辑**:
```java
editor.remove(KEY_TOKEN);
editor.remove(KEY_USERNAME);
editor.remove(KEY_EXPIRY);
editor.remove(KEY_REMEMBER_ME);
```

**用途**: 完全重置时调用（可用于"清除应用数据"功能）

---

## 🔄 流程图

### 登录流程
```
用户打开登录界面
    ↓
检查 isRememberMe()
    ↓
是 → 自动填充用户名 + 勾选复选框
否 → 输入框为空
    ↓
用户输入密码并登录
    ↓
登录成功
    ↓
检查 checkBoxRememberMe.isChecked()
    ↓
是 → 保存用户名
否 → 清除已保存的用户名
    ↓
跳转到主界面
```

### 退出流程
```
用户点击退出
    ↓
调用 authManager.clearLoginInfo()
    ↓
清除 Token 和 Expiry
保留 Username 和 RememberMe
    ↓
返回登录界面
    ↓
自动填充用户名（如果之前勾选了"记住我"）
```

---

## 📁 文件清单

### Java 文件
1. **`app/src/main/java/com/limelight/auth/UserAuthManager.java`**
   - ✅ `saveLoginInfo()` - 保存登录信息（含用户名逻辑）
   - ✅ `getUsername()` - 获取保存的用户名
   - ✅ `isRememberMe()` - 检查是否启用"记住我"
   - ✅ `clearLoginInfo()` - 清除登录信息（保留用户名）
   - ✅ `clearAllInfo()` - 清除所有信息（包括用户名）

2. **`app/src/main/java/com/limelight/LoginActivity.java`**
   - ✅ `loadRememberedUsername()` - 加载记住的用户名
   - ✅ `performLogin()` - 执行登录（含"记住我"逻辑）

### XML 文件
3. **`app/src/main/res/layout/activity_login.xml`**
   - ✅ `checkBoxRememberMe` - "记住我"复选框

---

## 🧪 测试用例

### 用例 1：启用"记住我"
```java
// Given: 用户首次登录
String username = "testuser";
boolean rememberMe = true;

// When: 登录成功
authManager.saveLoginInfo(username, token, expiry, rememberMe);

// Then: 用户名被保存
assertEquals("testuser", authManager.getUsername());
assertTrue(authManager.isRememberMe());
```

### 用例 2：取消"记住我"
```java
// Given: 用户之前启用了"记住我"
authManager.saveLoginInfo("olduser", token, expiry, true);

// When: 用户取消勾选"记住我"并登录
authManager.saveLoginInfo("newuser", token, expiry, false);

// Then: 用户名被清除
assertNull(authManager.getUsername());
assertFalse(authManager.isRememberMe());
```

### 用例 3：退出登录后保留用户名
```java
// Given: 用户启用了"记住我"并登录
authManager.saveLoginInfo("testuser", token, expiry, true);

// When: 用户退出登录
authManager.clearLoginInfo();

// Then: 用户名仍然保留
assertEquals("testuser", authManager.getUsername());
assertTrue(authManager.isRememberMe());
```

---

## 🔍 调试技巧

### 查看 SharedPreferences 数据
使用 Android Studio 的 Device File Explorer：
```
/data/data/com.limelight/shared_prefs/user_auth.xml
```

查看内容示例：
```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="username">admin</string>
    <boolean name="remember_me" value="true" />
    <string name="token">eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...</string>
    <long name="expiry" value="1697376000000" />
</map>
```

### 日志输出
在关键位置添加日志：
```java
// UserAuthManager.saveLoginInfo()
Log.d("RememberMe", "Saving login info: username=" + username + ", rememberMe=" + rememberMe);

// LoginActivity.loadRememberedUsername()
Log.d("RememberMe", "Loading username: " + authManager.getUsername());
Log.d("RememberMe", "RememberMe status: " + authManager.isRememberMe());
```

---

## 🎯 最佳实践

### 1. 同步保存
使用 `commit()` 而不是 `apply()`，确保数据立即写入：
```java
editor.commit(); // 同步保存，确保数据立即写入
```

### 2. 空值检查
在使用用户名前检查是否为空：
```java
String username = authManager.getUsername();
if (!TextUtils.isEmpty(username)) {
    editTextUsername.setText(username);
}
```

### 3. 安全性
- ❌ 不保存密码
- ✅ 只保存用户名
- ✅ Token 独立管理

### 4. 用户体验
- ✅ 自动填充用户名
- ✅ 自动勾选复选框
- ✅ 退出后保留用户名

---

## 🔧 扩展建议

### 多账号支持
```java
// 保存多个账号
Set<String> usernames = new HashSet<>();
usernames.add("user1");
usernames.add("user2");
editor.putStringSet("usernames", usernames);

// 读取账号列表
Set<String> savedUsernames = preferences.getStringSet("usernames", new HashSet<>());
```

### 账号切换下拉框
在 `activity_login.xml` 中添加 `AutoCompleteTextView`：
```xml
<AutoCompleteTextView
    android:id="@+id/editTextUsername"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/username"
    android:completionThreshold="1" />
```

---

## 📊 性能考虑

### SharedPreferences 读写性能
- **读取**: 非常快（内存缓存）
- **写入**: 
  - `commit()` - 同步，阻塞主线程
  - `apply()` - 异步，不阻塞主线程

**当前选择**: `commit()`  
**原因**: 数据量小，需要确保立即保存（避免跳转时数据丢失）

---

## 🐛 已知问题

### 无已知问题
✅ 所有功能测试通过  
✅ 编译无错误  
✅ 无 linter 警告

---

## 📝 版本历史

### v1.0 (2025-10-15)
- ✅ 实现"记住我"功能
- ✅ 支持保存/清除用户名
- ✅ 自动填充用户名
- ✅ 退出后保留用户名

---

**最后更新**: 2025-10-15  
**维护者**: AI Assistant  
**状态**: ✅ 生产就绪

