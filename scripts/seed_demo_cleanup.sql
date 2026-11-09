-- Borra los datos creados por scripts/seed_demo.py (cuentas @turnoya-demo.com y sus negocios)
USE turnoya_db;
CREATE TEMPORARY TABLE demo_users AS SELECT id_usuario FROM usuario WHERE email LIKE '%@turnoya-demo.com';
CREATE TEMPORARY TABLE demo_biz AS SELECT id_negocio FROM negocio WHERE id_dueno IN (SELECT id_usuario FROM demo_users);
DELETE FROM notificacion WHERE id_usuario IN (SELECT id_usuario FROM demo_users) OR id_negocio IN (SELECT id_negocio FROM demo_biz);
DELETE FROM turno WHERE id_cliente IN (SELECT id_usuario FROM demo_users) OR id_negocio IN (SELECT id_negocio FROM demo_biz);
DELETE FROM codigo_qr WHERE id_negocio IN (SELECT id_negocio FROM demo_biz);
DELETE FROM empleado_negocio WHERE id_usuario IN (SELECT id_usuario FROM demo_users) OR id_negocio IN (SELECT id_negocio FROM demo_biz);
DELETE FROM catalogo_item WHERE id_negocio IN (SELECT id_negocio FROM demo_biz);
DELETE FROM sector WHERE id_negocio IN (SELECT id_negocio FROM demo_biz);
DELETE FROM negocio WHERE id_negocio IN (SELECT id_negocio FROM demo_biz);
DELETE FROM usuario WHERE id_usuario IN (SELECT id_usuario FROM demo_users);
