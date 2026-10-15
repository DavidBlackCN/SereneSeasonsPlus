# 26.3 迁移记录

Migration Result: PARTIAL — 双加载器源码与构建已适配，运行验收等待单独迁移 GabouLibs。

基线：工作区 `1.21.11`，Git 提交 `018d49e`。基线目录保持未修改。目标为复制后的 `26.3`；迁移期间使用文件级差异审查，随后按用户要求接回基线 Git 历史并建立 `26.3-All` 分支。开发运行目录、日志和本机 IDE 启动配置不纳入迁移提交。

## 构建环境

| 项目 | 目标 |
| --- | --- |
| Minecraft / Java | 26.3 / 25（验证 JDK 25.0.3） |
| Gradle Wrapper | 9.6.0 |
| Fabric Loom | `net.fabricmc.fabric-loom` 1.17.21 |
| 命名体系 | Minecraft 非混淆官方名称，无额外 mappings 配置 |
| Fabric Loader / API | 0.19.5 / 0.163.0+26.3 |
| NeoForge / NeoGradle / NeoForm | 26.3.0.76 / 7.1.39 / 26.3-1 |
| Better Days / WhiteNoise | 4.1.1.7 / 2.2.1 |

依赖版本与 API 以已解析的目标版本依赖、源码及字节码为依据。构建配置固定版本，不使用动态版本。

## 修改范围与决策

- 根及三个模块的 Gradle 配置、Wrapper、加载器元数据：切换 26.3 工具链和依赖，公共源码打入各加载器产物；源码 JAR 同样包含公共源码。
- 公共 GUI、两端配置界面和客户端入口：适配 `GuiGraphicsExtractor`、界面状态提取和 `Minecraft.gui`。
- 公共积雪处理：适配 `ChunkPos` record、随机数访问与世界规则接口。
- Fabric 入口：适配 level tick 和 chunk load 事件签名。
- NeoForge 入口：适配 `BreakBlockEvent` 和 `ModConfig.Type.LOCAL`，显式保留 `sereneseasonsplus-common.toml` 文件名与原有本地配置语义。
- `ServerLevelMixin`：修正随机位置方法描述符，按实际字节码将 `BlockState.is` 的参数描述符改为 `Object`。
- `ChunkSerializerSnowMixin`、`ISnowChunkData`、`ChunkSnowData`：将积雪 NBT 附于对应区块快照，并在读取实际区块时恢复；保留原 NBT 键和值格式，避免使用全局坐标缓存关联存档数据。
- NeoForge Better Days 兼容：由晨间通知记录目标时间，再通过 `SleepFinishedTimeEvent` 的时钟调整接口应用；保留现有配置开关和目标起床时间算法。
- 标签目录改为 `tags/block`，草方块资源引用使用 `minecraft:short_grass`。
- `MigrationChecks`：无额外测试框架，通过各模块 `checkMigration` 执行并接入 `check`，失败会使 `build` 失败。

## 验证结果

在目标目录、JDK 25.0.3 下执行：

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat :Fabric:runServer :NeoForge:runServer --continue --console=plain
```

- `build`：PASS，最终运行 30 秒，48 个任务；Fabric、NeoForge 二进制及源码 JAR 均生成。
- Common、Fabric、NeoForge `checkMigration`：PASS。覆盖积雪 NBT 往返、快照隔离、旧数据缺省字段、Mixin 目标方法、静态/实例语义、调用点及 slice 内 ordinal。NeoForge 额外检查 Better Days 目标。
- 产物检查：PASS。JSON/TOML 可解析、无重复 ZIP 项、包含公共源码/类、新标签路径正确、没有打包旧版 GabouLibs 类。
- 两种 `runServer`：FAIL，均在必需依赖解析阶段报告缺少 `gaboulibs`；未进入世界加载或实际 Mixin 应用阶段。NeoForge 退出后的扫描取消异常发生于依赖检查失败之后。
- 客户端 GUI、实际 Mixin 局部变量捕获、积雪/融雪存档重载、Better Days 睡眠流程：NOT RUN，等待适配后的 GabouLibs。
- 日志与文件差异清单位于 `build/reports/migration/`。基线 Git 状态复核为空。

## GabouLibs 后续接入

按用户决定，本轮不迁移 GabouLibs。现有 Mojang 命名版本仅作为 `compileOnly` API；没有替身实现，没有取消必需依赖，也不会加载旧 Fabric intermediary JAR。

适配后可通过 `-Pgaboulibs_fabric_jar=<绝对路径>` 或 `-Pgaboulibs_neoforge_jar=<绝对路径>` 提供对应开发运行依赖。这些属性目前只注入运行类路径；若新库改变 API，还需同步更新三个模块的编译依赖并重新构建。不能用旧库替代运行验收。

## 自检

Code Review: PASS WITH RISKS（限本轮源码与构建阶段）；完整运行与发布验收未完成。

- 已审查全部本轮源码、构建、元数据及资源改动，保持客户端入口隔离及原存储字段格式；未修改其他版本目录。
- 审查中修复测试任务误编主源码、实际 Mixin 调用描述符、NeoForge 事件/配置 API 和 Fabric 源码 JAR 内容。
- 发布阻塞：缺少目标版 GabouLibs。此项已由用户明确安排后续迁移。
- 剩余风险：静态字节码检查不能替代 Mixin 实际注入、局部变量捕获和整合包冲突验证；Snow Real Magic、Project Atmosphere 可选集成尚未获得目标版运行验证；NeoForge 原有性能检测仍有弃用警告。
- `UPDATE_NOTES.md` 仅记录已经验证的构建目标变更，不声明完整运行兼容性或发布就绪。
