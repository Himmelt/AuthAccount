# AuthAccount
AuthAccount

### 运行机制

```markdown
连接 : 检查用户名合法性 & 在线状态
登陆 : User -> NBT (-> DB)
验证 : (DB ->) NBT -> User
注册 : (DB ->) NBT -> User

启用插件时 ( ALL -> 缓存)
登陆 (NBT  -> 缓存)
登出 (缓存 ->  NBT)
禁用插件时 ( 缓存 -> ALL)
```

本地注册 数据库注册
本地注册 数据库未注册
本地未注册 数据库注册
本地未注册 数据库未注册
