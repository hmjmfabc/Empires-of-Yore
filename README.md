# 剑客群组服 (Swordsman)

**剑客群组服（Swordsman Group Uniform, SGU）定制 Minecraft 模组**，基于 **NeoForge 21.1.248 / Minecraft 1.21.1**（Java 21）构建。

## 内容

### 剑客群组服玩法
- **宝箱战利品表**：`swordsman:chests/lv1` ~ `lv9`、`lv6_thunder`（红红火火 Lv.6⚡️）与 `cheating_lv1~3`（诈骗箱），共 13 张；前 10 张必定开出 4~6 个名为「剑客群组服」的烤马铃薯，宝箱物品按等级逐级升级（附魔、装备、稀有材料等）
- **进度**：「剑客荣光」标签页——10 个隐藏宝藏进度（对应开启各级宝箱，奖励 100 XP）+ 「术友汇玩家不得不品的一环」「新手期已过！」（与清朝僵尸 `iss_magicfromtheeast:jiangshi` 相关，奖励 390 XP）
- **自定义维度 `swordsman:mining`**：无地表、全地底洞穴世界（0~1024 层，上下基岩封闭，出生点 512 层），深板岩 256 层以下生成，矿石更密集，64 层以下极低概率远古残骸，384~800 层石英矿、128~256 层深层石英矿；怪物生成率更高且全部获得加强（生命/攻击/移速/追踪加成）
- **石英矿石**：`swordsman:overworld_quartz_ore`（主世界 0~63 层）、`swordsman:deepslate_quartz_ore`（主世界 -64~0 层），均与下界石英矿石行为一致（除贴图）

### 原 HCE 服务器科技模组内容（已并入 swordsman 命名空间）
- 能源石、紫水晶货币、升级工具、铝/钛系列：矿石（主世界生成）、锭、粉、生矿
- 钛合金装备：镐/剑/锄/锭/棒/碎片 + 钛合金四件套护甲（猪灵中立）
- 合成配方、村民交易（工具匠）、熔炉燃料（能源石）、自定义创造标签

## 构建

```bash
./gradlew build
# 产物：build/libs/swordsman-<版本>.jar
```

> 版本号规则：`27.<重要更新>.<次要更新>.<Build>`（第一位为年份，首次构建为 27.0.0.1）。

> 国内网络下 Gradle 与依赖下载较慢，`settings.gradle` 与 `build.gradle` 已配置阿里云/腾讯云镜像（官方源兜底）。

## 依赖

- Minecraft 1.21.1（Java 21）
- NeoForge 21.1.248+

## 许可

本项目基于 **GNU GPLv3** 许可发布，详见 [LICENSE](LICENSE)。
