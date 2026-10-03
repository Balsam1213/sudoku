# 数独（Sudoku）

一款纯离线的安卓数独游戏，使用 Kotlin + Jetpack Compose（Material 3）从零实现，无需任何网络权限，无需登录。

## 功能

- **六档难度**：初学者 / 简单 / 中级 / 困难 / 专家 / 极端（挖空数 25/35/45/54/58/60+）
- **题目质量保证**：自研位掩码 + MRV 回溯求解器，挖洞时每移除一个数字都验证解仍然唯一——每道题必有且仅有一个解
- **完整玩法**：铅笔笔记（冲突候选自动拒绝）、撤销/重做、擦除、提示、冲突即时标红、行列宫与同数字高亮
- **闪电模式**：锁定一个数字依次填入所有格子，填满自动切换下一个；新局默认开启
- **每日挑战**：按近期游玩难度自适应出题，同一天全网同题（日期种子生成），当月全勤获得该月专属奖杯
- **成就系统**：72 个成就、13 个分组（每难度里程碑、铅笔/提示/累计天数/每日挑战等），累计型成就带进度条，对局中解锁以非阻塞横幅 + 音效提示
- **战绩统计**：各难度最佳/平均用时与胜率、当前与最长连续天数、累计完成天数、最近 50 局记录
- **分享**：对局中分享题目图片，结算页分享完成盘面与成绩
- **其他**：浅色/深色/跟随系统主题、可限制错误 3 次判负、音效与震动反馈、对局自动存档随时继续

## 技术要点

- Kotlin + Jetpack Compose（BOM）/ Material 3，单 Activity，MVVM
- Room（战绩 / 成就 / 每日挑战 / 月度奖杯，含 v1→v2 迁移）+ DataStore（设置与对局存档）
- 数独生成与求解引擎为纯 Kotlin 实现，含唯一解单元测试
- R8 代码压缩与资源收缩，release APK 约 1.4 MB
- 最低支持 Android 8.0（API 26）

## 构建与运行

```bash
./gradlew installDebug        # 构建并安装调试包（需已连接设备/模拟器）
./gradlew testDebugUnitTest   # 运行单元测试（生成器唯一解保证等）
./gradlew assembleRelease     # 构建 release 包
```

要求：JDK 17+，Android SDK（`local.properties` 中配置 `sdk.dir`，此文件不入库，首次克隆后需自行创建）。

## 关于签名

`keystore/` 与 `keystore.properties`（release 签名密钥）**有意不入库**。克隆后若需构建 release，请自建密钥并在项目根目录创建 `keystore.properties`：

```properties
storeFile=keystore/你的密钥.jks
storePassword=你的密码
keyAlias=你的别名
keyPassword=你的密码
```

未提供密钥时 release 构建会生成未签名包，debug 构建不受影响。

## 目录结构

```
app/src/main/java/com/balsam/sudoku/
├── game/          # 数独引擎（生成器、求解器、规则判定）
├── data/          # Room 数据库与 DataStore 仓库
├── achievements/  # 成就定义与判定引擎
├── ui/
│   ├── menu/      # 主菜单（每日挑战、难度选择）
│   ├── play/      # 对局界面（棋盘、键盘、闪电模式）
│   ├── stats/     # 战绩统计
│   ├── achievements/ # 成就页
│   ├── settings/  # 设置
│   ├── common/    # 分享图片、音效、格式化工具
│   └── theme/     # 主题
└── MainActivity.kt
```
