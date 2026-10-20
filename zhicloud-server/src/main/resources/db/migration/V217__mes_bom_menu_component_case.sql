-- V217 修正 MES BOM 菜单的组件路径大小写
--
-- 背景：菜单 id=9560（md/bom，MES 物料清单）的 component 写成 'mes/md/bom/index'，
--       而前端实际视图是 views/mes/md/Bom/index.vue。Vite 的 import.meta.glob 键区分大小写，
--       匹配不到时路由拿不到组件，点击该菜单得到空白页。
--
-- 处理：只改这一条菜单的 component 指向，不动菜单结构、权限与排序；重复执行安全。

UPDATE `system_menu`
   SET `component` = 'mes/md/Bom/index'
 WHERE `id` = 9560
   AND `component` = 'mes/md/bom/index';
