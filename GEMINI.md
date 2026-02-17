# GEMINI.md - Alien Client 项目上下文

## 1. 项目概览
**项目名称:** Alien
**项目类型:** Minecraft Fabric Mod (Utility/Cheat Client)
**目标版本:** Minecraft 1.21.1
**开发语言:** Java 21
**核心框架:** Fabric Loader, Fabric API, Mixin

Alien 是一个功能丰富的 Minecraft 辅助客户端，旨在通过模块化系统（Modules）和各种管理器（Managers）增强玩家体验。

## 2. 核心架构

### A. 启动与核心 (EntryPoint)
*   **主入口:** `dev.luminous.Alien` (实现 `ModInitializer`)。
*   **事件总线:** 使用自定义的 `EventBus` 处理系统事件。
*   **配置管理:** `ConfigManager` 处理模块和设置的持久化，配置文件存储在 `.minecraft/alien/` 目录下。

### B. 模块系统 (Module System)
所有功能都封装在 `Module` 的子类中，分为以下类别：
*   **Combat:** 战斗辅助功能。
*   **Movement:** 移动增强。
*   **Player:** 玩家相关辅助。
*   **Render:** 视觉效果与渲染增强。
*   **Misc:** 杂项功能。
*   **Exploit:** 漏洞利用相关。
*   **Client:** 客户端自身设置。

### C. 管理器 (Managers)
项目包含多个管理器类来处理特定逻辑：
*   `ModuleManager`: 管理所有功能模块。
*   `CommandManager`: 处理控制台命令。
*   `RotationManager`: 接管玩家视角旋转逻辑。
*   `HoleManager`: 用于寻找安全位置。
*   `FriendManager`: 管理好友列表。
*   `ShaderManager` / `BlurManager`: 处理后期渲染效果。

## 3. 技术栈与依赖
*   **构建工具:** Gradle (Fabric Loom 1.14+)
*   **Mixin:** 用于修改 Minecraft 核心代码。
*   **重要依赖:**
    *   `Baritone`: 自动路径搜索与导航。
    *   `Sodium`: 性能与渲染优化。
    *   `Satin`: 高性能着色器 API。
    *   `IAS (In-Game Account Switcher)`: 游戏内快速切换账号。

## 4. 构建与运行
*   **构建项目:** 执行 `./gradlew build` 生成 mod jar 文件。
*   **开发调试:** 执行 `./gradlew runClient` 启动带 Mod 的游戏实例。
*   **环境准备:** 确保安装了 Java 21。项目使用 Yarn 映射进行反混淆开发。

## 5. 开发约定
*   **模块开发:** 新功能应继承 `dev.luminous.mod.modules.Module` 并放置在 `dev.luminous.mod.modules.impl` 的相应分类下。
*   **字节码注入:** Mixin 类位于 `dev.luminous.asm.mixins`。通过 `dev.luminous.asm.accessors` 访问受限的 Minecraft 成员。
*   **配置文件:** 运行时数据存放在运行目录下的 `alien` 文件夹中。
*   **多语言支持:** 模块支持通过 `setChinese(String)` 设置中文显示名称。
