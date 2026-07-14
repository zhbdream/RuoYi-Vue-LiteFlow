# MySQL 初始化（Docker）

请将仓库根目录的全量脚本复制到本目录（或挂载），例如：

```bash
cp ../../sql/ry-vue.sql ./01-ry-vue.sql
```

容器首次启动时，`docker-entrypoint-initdb.d` 会按文件名顺序执行。

本地开发仍推荐按根目录 [README](../../README.md) 手动导入 `sql/ry-vue.sql`。
