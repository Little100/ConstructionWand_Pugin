# 🏗️ Construction Wand - 建筑之杖

<p align="center">
  此文档由ai生成(没有进行审查)
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.16--1.21.4-green" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Folia-Supported-blue" alt="Folia Support">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

一个 Minecraft 服务器插件，灵感来自 Forge 模组 [Construction Wand](https://www.curseforge.com/minecraft/mc-mods/construction-wand)。让玩家能够快速延伸放置方块，大大提高建筑效率！

## ✨ 特性

- 🔨 **五种建筑之杖** - 石制、铁制、钻石、下界合金、无限
- 🎯 **智能延伸放置** - 右键点击方块表面，自动延伸放置相同方块
- 👁️ **实时预览** - 粒子效果显示即将放置的方块位置
- 🎨 **自定义预览** - 可调整预览颜色和模式（完整/角落/外包框）
- ⚡ **附魔系统** - 建筑扩展附魔，增加最大放置方块数
- 🌍 **多语言支持** - 简体中文、繁体中文、英文、文言文
- 🛡️ **领地兼容** - 支持 WorldGuard、Residence、GriefPrevention 等
- 🚀 **Folia 支持** - 完美兼容 Folia 服务端
- 📦 **资源包支持** - 自定义手杖材质（支持 1.14-1.21.4+）

## 📋 手杖类型

| 手杖 | 最大方块数 | 耐久度 | 基础材料 |
|------|-----------|--------|----------|
| 石制建筑之杖 | 9 | 131 | 石镐 |
| 铁制建筑之杖 | 27 | 250 | 铁镐 |
| 钻石建筑之杖 | 128 | 1561 | 钻石镐 |
| 下界合金建筑之杖 | 256 | 2031 | 下界合金镐 |
| 无限建筑之杖 | 1024 | ∞ | 下界之星 |

## 🔧 安装

1. 下载最新版本的插件 JAR 文件
2. 将 JAR 文件放入服务器的 `plugins` 文件夹
3. 重启服务器
4. （可选）安装资源包以获得自定义材质

### 资源包安装

将 `CWResourcepack` 文件夹打包为 ZIP 文件，然后：
- **单人/客户端**：放入 `.minecraft/resourcepacks` 文件夹
- **服务器**：上传到网络并在 `server.properties` 中设置 `resource-pack` URL

## 📖 使用方法

### 基本使用

1. 合成或通过命令获取建筑之杖
2. 手持建筑之杖
3. 确保背包中有要放置的方块
4. 右键点击一个方块的表面
5. 手杖会自动延伸放置相同的方块

### 合成配方

所有手杖的合成配方格式相同：

```
  空       空     [材料]
  空      [木棍]   空
  [木棍]  空       空
```

| 手杖 | 材料 | 核心 |
|------|------|------|
| 石制 | 圆石 | 石镐 |
| 铁制 | 铁锭 | 铁镐 |
| 钻石 | 钻石 | 钻石镐 |
| 下界合金 | 下界合金锭 | 下界合金镐 |
| 无限 | 下界之星 | 下界之星 |

## 💻 命令

主命令：`/constructionwand` (别名: `/cw`, `/wand`)

| 命令 | 描述 | 权限 |
|------|------|------|
| `/cw help` | 显示帮助信息 | 无 |
| `/cw list` | 列出所有手杖类型 | 无 |
| `/cw preview` | 切换预览模式 | `constructionwand.preview` |
| `/cw gui` | 打开设置GUI | `constructionwand.settings` |
| `/cw give <类型> [玩家]` | 给予手杖 | `constructionwand.give` |
| `/cw itemgui` | 打开物品获取GUI | `constructionwand.itemgui` |
| `/cw enchant <附魔> <等级>` | 给手杖附魔 | `constructionwand.enchant` |
| `/cw lang <语言>` | 切换语言 | `constructionwand.lang` |
| `/cw reload` | 重载配置 | `constructionwand.reload` |
| `/cw nbtdebug` | 调试NBT信息 | `constructionwand.nbtdebug` |

## 🔐 权限

### 玩家权限（默认开启）

| 权限 | 描述 |
|------|------|
| `constructionwand.use` | 允许使用建筑之杖 |
| `constructionwand.use.stone` | 允许使用石制建筑之杖 |
| `constructionwand.use.iron` | 允许使用铁制建筑之杖 |
| `constructionwand.use.diamond` | 允许使用钻石建筑之杖 |
| `constructionwand.use.netherite` | 允许使用下界合金建筑之杖 |
| `constructionwand.use.infinity` | 允许使用无限建筑之杖 |
| `constructionwand.preview` | 允许切换预览模式 |
| `constructionwand.settings` | 允许打开设置GUI |

### 管理员权限（默认OP）

| 权限 | 描述 |
|------|------|
| `constructionwand.give` | 允许给予手杖 |
| `constructionwand.itemgui` | 允许打开物品获取GUI |
| `constructionwand.enchant` | 允许使用附魔命令 |
| `constructionwand.lang` | 允许切换服务器语言 |
| `constructionwand.reload` | 允许重载配置 |
| `constructionwand.nbtdebug` | 允许使用NBT调试 |
| `constructionwand.bypass` | 绕过领地保护检查 |

### 权限组

| 权限组 | 描述 |
|--------|------|
| `constructionwand.player` | 包含所有玩家权限 |
| `constructionwand.admin` | 包含所有权限 |

## ⚔️ 附魔系统

### 建筑扩展 (Building Extension)

增加手杖的最大放置方块数。

| 等级 | 加成 |
|------|------|
| I | +15% |
| II | +30% |
| III | +50% |
| IV | +75% |
| V | +100% |

**获取方式：**
- 使用 `/cw enchant building_extension <等级>` 命令
- 在附魔台附魔建筑之杖（随机1-5级）

## ⚙️ 配置文件

### config.yml

```yaml
# 语言设置
language: "zh_CN"  # 可选: zh_CN, zh_TW, en_US, lzh

# 预览设置
preview:
  # 粒子颜色
  particle-color: "RED"  # RED, GREEN, BLUE, YELLOW, ORANGE, PURPLE, WHITE, AQUA, LIME 或 #RRGGBB
  # 预览模式
  mode: "full"  # full(完整), corners(角落), outline(外包框)

# 领地保护设置
protection:
  use-event-check: true
  use-worldguard: true

# 手杖设置（可自定义每种手杖的属性）
wands:
  stone:
    enabled: true
    max-blocks: 9
    durability: 131
  # ... 其他手杖配置
```

### 附魔配置 (enchant/building_extension.yml)

```yaml
enabled: true
max-level: 10
levels:
  1: 0.15
  2: 0.30
  3: 0.50
  4: 0.75
  5: 1.00
  # ... 更高等级
default-increment: 0.15
```

## 🌐 多语言支持

支持的语言：
- `zh_CN` - 简体中文
- `zh_TW` - 繁體中文
- `en_US` - English
- `lzh` - 文言文

使用 `/cw lang <语言代码>` 切换语言。

## 🔌 兼容性

### 服务端
- Spigot 1.16+
- Paper 1.16+
- Folia（完全支持）

### 领地插件
- WorldGuard
- Residence
- GriefPrevention
- Towny
- Lands
- PlotSquared

### Minecraft 版本
- 1.16.x - 1.21.4+
- 自动适配 CustomModelData 格式（旧版整数/新版字符串）

## 📁 项目结构

```
ConstructionWand/
├── src/main/java/org/little100/constructionWand/
│   ├── ConstructionWand.java      # 主插件类
│   ├── action/                    # 方块放置逻辑
│   ├── command/                   # 命令处理
│   ├── enchant/                   # 附魔系统
│   ├── gui/                       # GUI界面
│   ├── i18n/                      # 国际化
│   ├── listener/                  # 事件监听
│   ├── preview/                   # 预览系统
│   ├── protection/                # 领地保护
│   ├── recipe/                    # 合成配方
│   ├── utils/                     # 工具类
│   └── wand/                      # 手杖管理
├── src/main/resources/
│   ├── config.yml                 # 配置文件
│   ├── plugin.yml                 # 插件信息
│   ├── enchant/                   # 附魔配置
│   └── lang/                      # 语言文件
└── CWResourcepack/                # 资源包
```

## 🛠️ 构建

```bash
# 克隆仓库
git clone https://github.com/your-repo/ConstructionWand.git

# 进入目录
cd ConstructionWand

# 构建
./gradlew build

# 输出文件位于 build/libs/
```

## 📝 更新日志

### v1.0.0
- 初始版本发布
- 五种建筑之杖
- 粒子预览系统
- 多语言支持
- 附魔系统
- 领地插件兼容
- Folia 支持

## 📄 许可证

本项目采用 GPLv3 许可证 - 详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- 灵感来源：[Construction Wand (Forge Mod)](https://www.curseforge.com/minecraft/mc-mods/construction-wand)
- 感谢所有贡献者和测试者！

## 🐛 问题反馈

如果你发现了 bug 或有功能建议，请在 GitHub Issues 中提交。

---

<p align="center">
  Made with ❤️ by Little_100
</p>