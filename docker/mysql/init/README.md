-- 按顺序导入（容器首次启动时由 docker-entrypoint-initdb.d 执行）
-- 1. ry_20260417.sql
-- 2. quartz.sql
-- 3. liteflow.sql
-- 4. liteflow_phase2*.sql / liteflow_phase3*.sql

-- 请将上述 SQL 文件复制到本目录，或手动挂载后执行。
-- 本地开发仍推荐按 README 手动导入。
