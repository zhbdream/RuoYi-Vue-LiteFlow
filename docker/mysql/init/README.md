# MySQL 初始化（Docker）

`docker compose` 已将仓库根目录 [`sql/ry-vue.sql`](../../sql/ry-vue.sql) 挂载为 `01-ry-vue.sql`，与本目录下 `00-use-database.sql` 一起在 **首次** 创建数据卷时自动执行。

无需再手动 `cp`。

## 注意

- 仅在 MySQL 数据卷为空时执行初始化；若改了 SQL 需重建：`docker compose down -v` 后再 `up`
- 本地非 Docker 开发仍按根目录 [README](../../README.md) 手动导入 `sql/ry-vue.sql`
