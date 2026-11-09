-- TurnoYa: datos de ubicacion y contacto del negocio (mapa interactivo, RF02/RF11)
-- Ejecutar una sola vez sobre una base creada con la version anterior de turnoya_schema.sql
USE turnoya_db;

ALTER TABLE negocio
    ADD COLUMN descripcion varchar(500) NULL AFTER horario,
    ADD COLUMN direccion varchar(255) NULL AFTER descripcion,
    ADD COLUMN telefono varchar(30) NULL AFTER direccion,
    ADD COLUMN latitud decimal(10,7) NULL AFTER telefono,
    ADD COLUMN longitud decimal(10,7) NULL AFTER latitud;

CREATE INDEX idx_negocio_estado ON negocio (estado);
CREATE INDEX idx_turno_cola ON turno (id_negocio, estado, fecha_solicitud);
CREATE INDEX idx_turno_cliente ON turno (id_cliente, fecha_solicitud);
CREATE INDEX idx_notificacion_usuario ON notificacion (id_usuario, fecha_envio);
