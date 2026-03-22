# PTT Broadcast - D21对讲机PTT键广播模块

## 功能说明
拦截D21对讲机的PTT按键（BTN_9），发送Android系统广播：
- 按下PTT键 → 发送 `android.intent.action.PTT.down`
- 松开PTT键 → 发送 `android.intent.action.PTT.up`

这样所有监听这些广播的对讲软件（如HyTalk、海能达APP等）就能自动支持PTT发射键了。

## 测试结果
✅ 已验证：Android可以直接发送 `android.intent.action.PTT.down` 和 `android.intent.action.PTT.up` 广播，无需Root或系统签名！

## PTT按键信息
- 设备节点：`/dev/input/event1` (mtk-kpd)
- 按键键值：`BTN_9` (scancode 0x109)

---

## 🚀 最简单的安装方法：使用GitHub自动编译

### 步骤1：创建GitHub仓库

1. 访问 https://github.com 并登录
2. 点击右上角 **"+"** → **"New repository"**
3. 仓库名称填 `PTTBroadcast`
4. 选择 **Public**（公开仓库才能用GitHub Actions免费编译）
5. 点击 **"Create repository"**

### 步骤2：上传代码

在仓库页面点击 **"uploading an existing file"**，上传以下文件/文件夹：

```
PTTBroadcast/
├── apk/
│   ├── AndroidManifest.xml
│   └── com/pttbroadcast/
│       ├── BootReceiver.java
│       └── PTTService.java
├── github/
│   └── workflow.yml  ← 上传到 .github/workflows/ 目录
└── README.md
```

**注意**：workflow.yml 需要放到 `.github/workflows/` 目录下！

### 步骤3：下载APK

1. 等待几秒钟，GitHub会自动开始编译
2. 点击仓库的 **"Actions"** 标签查看编译进度
3. 编译完成后，点击绿色的编译任务
4. 在 **Artifacts** 部分下载 `PTTBroadcast` 文件
5. 解压得到 `app-debug.apk`

### 步骤4：安装APK到D21

```bash
# 通过ADB安装
E:\backup\无线电\和对讲\读取广播值\adb\adb install app-debug.apk
```

---

## 📱 使用方法

### 1. 授予权限
安装后打开APP，授予以下权限：
- **自启动权限**：允许开机自动运行
- **后台运行权限**：允许在后台持续运行

### 2. 启动服务
打开APP后，服务会自动启动。

### 3. 测试
打开HyTalk，按下PTT键，应该能正常发射了！

### 手动测试广播
```bash
E:\backup\无线电\和对讲\读取广播值\adb\adb shell am broadcast -a android.intent.action.PTT.down
E:\backup\无线电\和对讲\读取广播值\adb\adb shell am broadcast -a android.intent.action.PTT.up
```

---

## ⚠️ 注意事项

1. **对讲软件兼容性**：此方案的前提是对讲软件（如HyTalk）监听 `android.intent.action.PTT.down` 广播。如果软件不支持，则此方案无效。
2. **后台运行**：需要授予APP后台运行权限，否则息屏后可能停止工作。
3. **设备节点权限**：如果服务无法读取 `/dev/input/event1`，需要修改权限：
   ```bash
   adb shell chmod 666 /dev/input/event1
   ```

---

## 🛠️ 故障排查

### 问题：GitHub编译失败
**解决**：检查文件路径是否正确，确保workflow.yml在 `.github/workflows/` 目录下

### 问题：APK安装后闪退
**解决**：
```bash
# 查看错误日志
adb logcat | grep PTTBroadcast
```

### 问题：PTT键无响应
**解决**：
```bash
# 检查事件设备
adb shell getevent -l /dev/input/event1
# 按PTT键，确认有BTN_9事件输出
```

---

## 📁 文件结构

```
PTTBroadcast/
├── apk/                              # Android应用源码
│   ├── AndroidManifest.xml           # 应用配置
│   └── com/pttbroadcast/
│       ├── BootReceiver.java        # 开机自启接收器
│       └── PTTService.java          # PTT监听服务
├── .github/
│   └── workflows/
│       └── workflow.yml              # GitHub自动编译配置
└── README.md
```

---

## 原理说明

```
┌─────────────────┐
│  按下PTT键       │
│  (BTN_9 DOWN)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  PTTService     │  读取 /dev/input/event1
│  (后台服务)      │  监听input_event
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  sendBroadcast  │  发送系统广播
│  "PTT.down"     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  HyTalk等APP    │  接收广播
│  触发PTT发射     │  开始讲话
└─────────────────┘
```
