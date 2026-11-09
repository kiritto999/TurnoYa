-- Borra los datos creados por scripts/smoke_test.py (usuarios @smoke-turnoya.com y sus negocios)
USE turnoya_db;
CREATE TEMPORARY TABLE smoke_users AS SELECT id_usuario FROM usuario WHERE email LIKE '%@smoke-turnoya.com';
CREATE TEMPORARY TABLE smoke_biz AS SELECT id_negocio FROM negocio WHERE id_dueno IN (SELECT id_usuario FROM smoke_users);
DELETE FROM notificacion WHERE id_usuario IN (SELECT id_usuario FROM smoke_users) OR id_negocio IN (SELECT id_negocio FROM smoke_biz);
DELETE FROM turno WHERE id_cliente IN (SELECT id_usuario FROM smoke_users) OR id_negocio IN (SELECT id_negocio FROM smoke_biz);
DELETE FROM codigo_qr WHERE id_negocio IN (SELECT id_negocio FROM smoke_biz);
DELETE FROM empleado_negocio WHERE id_usuario IN (SELECT id_usuario FROM smoke_users) OR id_negocio IN (SELECT id_negocio FROM smoke_biz);
DELETE FROM catalogo_item WHERE id_negocio IN (SELECT id_negocio FROM smoke_biz);
DELETE FROM sector WHERE id_negocio IN (SELECT id_negocio FROM smoke_biz);
DELETE FROM negocio WHERE id_negocio IN (SELECT id_negocio FROM smoke_biz);
DELETE FROM usuario WHERE id_usuario IN (SELECT id_usuario FROM smoke_users);
