# 协议（版本 1.0）

## 命名空间与通道
- `nationrealm:handshake`、`nationrealm:data`。

## 帧格式（逐字节）
```
[varint payloadLen][payload]
payload = [varint msgId][header][body][varint extLen][ext]
header  = requestId:u64 | timestampMillis:u64 | nationDataVersion:u64 | errorCode:varint
```
- 整数小端；varint 为无符号 LEB128；字符串 = `varint len + UTF-8`；数组 = `varint count + 元素`。
- 单包上限 1MiB；超阈值（16KiB）可启用压缩。
- 扩展块 `ext`：未知字段以 `varint len + bytes` 结尾，旧版本跳过；非法值必须拒绝。

## 消息 ID（稳定，不得复用）
| ID | 消息 | 说明 |
|----|------|------|
| 0x01 | HANDSHAKE_REQUEST | major/minor/modVersion/serverBuild/flags/compression |
| 0x02 | HANDSHAKE_RESPONSE | accepted/major/minor/build/flags/compression/reason |
| 0x10 | SNAPSHOT_REQUEST | regionX/regionZ/zoom/knownVersion |
| 0x11 | REGION_SNAPSHOT | 区域所有权稠密数组 + 探索位图 |
| 0x12 | TERRITORY_DELTA | 稀疏 (localX,localZ,owner) 三元组 + 未知掩码 |
| 0x13 | NATION_SUMMARY | 国名/缩写/成员数/首都/颜色/版本 |
| 0x14 | BUILDING_STATE | 施工阶段/进度/耐久/版本 |
| 0x15 | ARMY_STATE | 驻地/士气/补给/兵力/版本 |
| 0x16 | NPC_STATE | 含 ai 标记（AI 身份永久可见） |
| 0x20 | OPERATION_REQUEST | 只表达意图；服务端再次验证权限 |
| 0x21 | OPERATION_ACK | ackRequestId/success/detail |
| 0x22 | CHUNK_CLAIM | 单区块占领变更：chunkX/chunkZ/nationId(0=放弃)/regionVersion/claimedAtUtc |
| 0x2F | ERROR | errorCode + 可读消息（不含堆栈） |
| 0x30 | HEARTBEAT | nonce + clientTime |

## 兼容策略
- 握手含主/次版本、模组版本、服务端构建、压缩与功能标志。
- 主版本不同：拒绝启用高级界面并给玩家清晰提示，不静默出错。
- 次版本不同：未知字段跳过、能力协商、默认值兼容。
- 登录 / 切服 / 断线 / 重连都重新建立订阅与必要快照。
- 重复包按 `MessageHeader.requestId` 与 `nationDataVersion` 幂等去重；乱序按版本丢弃旧包。

## 性能约束
- 默认 60 秒增量刷新；客户端补间/动画不依赖高频 tick。
- 实现视口裁剪、优先级队列、背压、批量更新；禁止每 tick 广播全部国家数据。
- 未订阅数据不下发。

## 测试矩阵
旧客户端×新服务端、新客户端×旧服务端；乱序/重复/截断/超大包/未知字段/版本不匹配均有自动测试（`PacketCodecTest`）。
