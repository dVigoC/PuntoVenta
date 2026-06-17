-- ============================================================
--  SISTEMA POS - RESTAURANTE
--  Versión: 3.0 — Final funcional
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- 1. empresa
-- ============================================================
CREATE TABLE empresa (
    id               BIGSERIAL PRIMARY KEY,
    nombre           VARCHAR(150)  NOT NULL,
    ruc              VARCHAR(20)   UNIQUE,
    direccion        VARCHAR(255),
    telefono         VARCHAR(20),
    email            VARCHAR(100),
    logo_url         TEXT,
    moneda           VARCHAR(10)   NOT NULL DEFAULT 'PEN',
    porcentaje_igv   NUMERIC(5,2)  NOT NULL DEFAULT 18.00,
    pie_factura      VARCHAR(255)  DEFAULT 'Gracias por su visita',
    desarrollador    VARCHAR(150)  DEFAULT 'Desarrollado por TuEmpresa Dev',
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    actualizado_en   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 2. roles
-- ============================================================
CREATE TABLE roles (
    id               BIGSERIAL PRIMARY KEY,
    nombre           VARCHAR(50)   NOT NULL UNIQUE,
    descripcion      VARCHAR(200),
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 3. usuarios
-- ============================================================
CREATE TABLE usuarios (
    id                BIGSERIAL PRIMARY KEY,
    username          VARCHAR(60)   NOT NULL UNIQUE,
    password_hash     VARCHAR(255)  NOT NULL,
    email             VARCHAR(120)  UNIQUE,
    nombre_completo   VARCHAR(150)  NOT NULL,
    rol_id            BIGINT        NOT NULL REFERENCES roles(id),
    activo            BOOLEAN       NOT NULL DEFAULT TRUE,
    ultimo_acceso     TIMESTAMPTZ,
    intentos_fallidos INT           NOT NULL DEFAULT 0,
    bloqueado         BOOLEAN       NOT NULL DEFAULT FALSE,
    creado_en         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    actualizado_en    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_username_len CHECK (char_length(username) >= 4),
    CONSTRAINT chk_email_format CHECK (
        email IS NULL OR
        email ~* '^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$'
    )
);

-- ============================================================
-- 4. empleados
-- ============================================================
CREATE TABLE empleados (
    id               BIGSERIAL PRIMARY KEY,
    usuario_id       BIGINT        REFERENCES usuarios(id) ON DELETE SET NULL,
    dni              VARCHAR(20)   NOT NULL UNIQUE,
    nombres          VARCHAR(100)  NOT NULL,
    apellidos        VARCHAR(100)  NOT NULL,
    telefono         VARCHAR(20),
    direccion        VARCHAR(255),
    email            VARCHAR(120),
    cargo            VARCHAR(80)   NOT NULL,
    fecha_ingreso    DATE          NOT NULL DEFAULT CURRENT_DATE,
    fecha_cese       DATE,
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    actualizado_en   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_dni_len CHECK (char_length(dni) >= 8)
);

-- ============================================================
-- 5. categorias_producto
-- ============================================================
CREATE TABLE categorias_producto (
    id               BIGSERIAL PRIMARY KEY,
    nombre           VARCHAR(100)  NOT NULL UNIQUE,
    descripcion      VARCHAR(255),
    orden_display    INT           NOT NULL DEFAULT 0,
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 6. productos
-- ============================================================
CREATE TABLE productos (
    id               BIGSERIAL PRIMARY KEY,
    categoria_id     BIGINT        NOT NULL REFERENCES categorias_producto(id),
    codigo           VARCHAR(30)   UNIQUE,
    nombre           VARCHAR(150)  NOT NULL,
    descripcion      TEXT,
    precio           NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    disponible       BOOLEAN       NOT NULL DEFAULT TRUE,
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    actualizado_en   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 7. areas
-- ============================================================
CREATE TABLE areas (
    id               BIGSERIAL PRIMARY KEY,
    nombre           VARCHAR(100)  NOT NULL UNIQUE,
    descripcion      VARCHAR(255),
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 8. mesas
-- ============================================================
CREATE TABLE mesas (
    id               BIGSERIAL PRIMARY KEY,
    area_id          BIGINT        NOT NULL REFERENCES areas(id),
    numero           VARCHAR(20)   NOT NULL,
    estado           VARCHAR(20)   NOT NULL DEFAULT 'LIBRE'
                         CHECK (estado IN ('LIBRE','OCUPADA','RESERVADA','INACTIVA')),
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    actualizado_en   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (area_id, numero)
);

-- ============================================================
-- 9. pedidos
--    - Sin empleado_id (el cajero opera el sistema)
--    - Una mesa activa tiene UN solo pedido abierto
--    - Los platos adicionales se agregan al mismo pedido
-- ============================================================
-- ============================================================
-- 9. pedidos (CORREGIDO)
-- ============================================================
CREATE TABLE pedidos (
    id               BIGSERIAL PRIMARY KEY,
    numero_pedido    VARCHAR(30)   NOT NULL UNIQUE,
    mesa_id          BIGINT        NOT NULL REFERENCES mesas(id),
    estado           VARCHAR(20)   NOT NULL DEFAULT 'ABIERTO'
                         CHECK (estado IN (
                             'ABIERTO',
                             'EN_COCINA',
                             'SERVIDO',
                             'COBRADO',
                             'ANULADO'
                         )),
    subtotal         NUMERIC(12,2) NOT NULL DEFAULT 0,
    igv              NUMERIC(12,2) NOT NULL DEFAULT 0,
    descuento        NUMERIC(12,2) NOT NULL DEFAULT 0,
    total            NUMERIC(12,2) NOT NULL DEFAULT 0,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    actualizado_en   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Índice parcial: garantiza que una mesa solo tenga UN pedido activo
-- (ignora los pedidos COBRADOS o ANULADOS)
CREATE UNIQUE INDEX uq_mesa_un_pedido_activo
    ON pedidos (mesa_id)
    WHERE estado NOT IN ('COBRADO', 'ANULADO');

-- ============================================================
-- 10. detalle_pedido
--     comanda_numero: identifica a qué envío a cocina pertenece
--     este ítem (permite saber qué platos son "nuevos")
-- ============================================================
CREATE TABLE detalle_pedido (
    id               BIGSERIAL PRIMARY KEY,
    pedido_id        BIGINT        NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    producto_id      BIGINT        NOT NULL REFERENCES productos(id),
    cantidad         INT           NOT NULL CHECK (cantidad > 0),
    precio_unitario  NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0),
    descuento_item   NUMERIC(10,2) NOT NULL DEFAULT 0,
    subtotal         NUMERIC(12,2) NOT NULL,
    nota_cocina      VARCHAR(255),
    estado_item      VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE'
                         CHECK (estado_item IN (
                             'PENDIENTE',  -- recién agregado, aún no va a cocina
                             'EN_PREP',    -- en preparación
                             'LISTO',      -- listo para servir
                             'SERVIDO',    -- entregado en mesa
                             'ANULADO'
                         )),
    -- Referencia al número de comanda en que fue enviado a cocina
    -- NULL = aún no enviado
    comanda_ref      VARCHAR(30),
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    actualizado_en   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 11. comandas_cocina
--     Cada vez que se envía/imprime a cocina se crea un registro.
--     Puede haber MÚLTIPLES comandas por pedido (pedidos adicionales).
--     Solo contiene los ítems NUEVOS de ese envío.
-- ============================================================
CREATE TABLE comandas_cocina (
    id                BIGSERIAL PRIMARY KEY,
    pedido_id         BIGINT        NOT NULL REFERENCES pedidos(id),
    numero_comanda    VARCHAR(30)   NOT NULL UNIQUE,
    -- nro. de envío: 1ra comanda, 2da comanda, etc.
    nro_envio         INT           NOT NULL DEFAULT 1,
    impresa           BOOLEAN       NOT NULL DEFAULT FALSE,
    impresa_auto      BOOLEAN       NOT NULL DEFAULT FALSE,
    impresora_destino VARCHAR(100),
    estado            VARCHAR(20)   NOT NULL DEFAULT 'ENVIADA'
                          CHECK (estado IN ('ENVIADA','EN_PREP','LISTA','ENTREGADA')),
    creado_en         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    actualizado_en    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 12. detalle_comanda_cocina
--     Qué ítems específicos fueron en cada envío a cocina.
--     Así se puede reimprimir exactamente esa comanda.
-- ============================================================
CREATE TABLE detalle_comanda_cocina (
    id                  BIGSERIAL PRIMARY KEY,
    comanda_id          BIGINT  NOT NULL REFERENCES comandas_cocina(id) ON DELETE CASCADE,
    detalle_pedido_id   BIGINT  NOT NULL REFERENCES detalle_pedido(id),
    cantidad            INT     NOT NULL CHECK (cantidad > 0),
    nota_cocina         VARCHAR(255)
);

-- ============================================================
-- 13. impresoras
-- ============================================================
CREATE TABLE impresoras (
    id               BIGSERIAL PRIMARY KEY,
    nombre           VARCHAR(100)  NOT NULL UNIQUE,
    tipo             VARCHAR(20)   NOT NULL DEFAULT 'TERMICA'
                         CHECK (tipo IN ('TERMICA','LASER','INKJET')),
    destino          VARCHAR(20)   NOT NULL DEFAULT 'COCINA'
                         CHECK (destino IN ('COCINA','CAJA','TODOS')),
    ip_address       VARCHAR(45),
    puerto           INT           DEFAULT 9100,
    auto_imprimir    BOOLEAN       NOT NULL DEFAULT FALSE,
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 14. sesiones_caja
-- ============================================================
CREATE TABLE sesiones_caja (
    id               BIGSERIAL PRIMARY KEY,
    empleado_id      BIGINT        NOT NULL REFERENCES empleados(id),
    fecha_apertura   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    fecha_cierre     TIMESTAMPTZ,
    monto_inicial    NUMERIC(12,2) NOT NULL DEFAULT 0,
    monto_cierre     NUMERIC(12,2),
    total_ventas     NUMERIC(12,2) NOT NULL DEFAULT 0,
    estado           VARCHAR(20)   NOT NULL DEFAULT 'ABIERTA'
                         CHECK (estado IN ('ABIERTA','CERRADA')),
    observaciones    TEXT,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 15. comprobantes (listo para SUNAT — se activa después)
-- ============================================================
CREATE TABLE comprobantes (
    id               BIGSERIAL PRIMARY KEY,
    pedido_id        BIGINT        NOT NULL REFERENCES pedidos(id),
    empleado_id      BIGINT        REFERENCES empleados(id),
    tipo             VARCHAR(20)   NOT NULL DEFAULT 'TICKET'
                         CHECK (tipo IN ('TICKET','BOLETA','FACTURA')),
    serie            VARCHAR(10),
    correlativo      INT,
    fecha_emision    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    subtotal         NUMERIC(12,2) NOT NULL,
    igv              NUMERIC(12,2) NOT NULL,
    descuento        NUMERIC(12,2) NOT NULL DEFAULT 0,
    total            NUMERIC(12,2) NOT NULL,
    monto_pagado     NUMERIC(12,2),
    vuelto           NUMERIC(12,2) DEFAULT 0,
    estado           VARCHAR(20)   NOT NULL DEFAULT 'EMITIDO'
                         CHECK (estado IN ('EMITIDO','ANULADO')),
    pdf_url          TEXT,
    sunat_enviado    BOOLEAN       NOT NULL DEFAULT FALSE,
    sunat_respuesta  JSONB,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 16. auditoria
-- ============================================================
CREATE TABLE auditoria (
    id               BIGSERIAL PRIMARY KEY,
    usuario_id       BIGINT        REFERENCES usuarios(id) ON DELETE SET NULL,
    accion           VARCHAR(100)  NOT NULL,
    tabla_afectada   VARCHAR(80),
    registro_id      BIGINT,
    datos_anteriores JSONB,
    datos_nuevos     JSONB,
    ip_origen        VARCHAR(45),
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- ÍNDICES
-- ============================================================
CREATE INDEX idx_pedidos_estado           ON pedidos(estado);
CREATE INDEX idx_pedidos_mesa             ON pedidos(mesa_id);
CREATE INDEX idx_pedidos_fecha            ON pedidos(creado_en DESC);
CREATE INDEX idx_pedidos_numero           ON pedidos(numero_pedido);
CREATE INDEX idx_detalle_pedido_pedido    ON detalle_pedido(pedido_id);
CREATE INDEX idx_detalle_pedido_producto  ON detalle_pedido(producto_id);
CREATE INDEX idx_detalle_pedido_estado    ON detalle_pedido(estado_item);
CREATE INDEX idx_detalle_pedido_comanda   ON detalle_pedido(comanda_ref);
CREATE INDEX idx_productos_categoria      ON productos(categoria_id);
CREATE INDEX idx_mesas_estado             ON mesas(estado);
CREATE INDEX idx_mesas_area               ON mesas(area_id);
CREATE INDEX idx_comandas_pedido          ON comandas_cocina(pedido_id);
CREATE INDEX idx_det_comanda_comanda      ON detalle_comanda_cocina(comanda_id);
CREATE INDEX idx_comprobantes_pedido      ON comprobantes(pedido_id);
CREATE INDEX idx_auditoria_fecha          ON auditoria(creado_en DESC);

-- ============================================================
-- TRIGGERS: actualizar timestamps
-- ============================================================
CREATE OR REPLACE FUNCTION fn_actualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'empresa','usuarios','empleados','productos',
        'mesas','pedidos','detalle_pedido','comandas_cocina'
    ]
    LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%s_upd
             BEFORE UPDATE ON %s
             FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();', t, t);
    END LOOP;
END;
$$;

-- ============================================================
-- TRIGGER: número de pedido automático  PED-YYYYMMDD-NNNN
-- ============================================================
CREATE OR REPLACE FUNCTION fn_numero_pedido()
RETURNS TRIGGER AS $$
DECLARE v_seq INT;
BEGIN
    SELECT COALESCE(MAX(CAST(SUBSTRING(numero_pedido FROM '[0-9]+$') AS INT)), 0) + 1
    INTO v_seq
    FROM pedidos
    WHERE numero_pedido LIKE 'PED-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-%';

    NEW.numero_pedido :=
        'PED-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' || LPAD(v_seq::TEXT, 4, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_pedidos_numero
BEFORE INSERT ON pedidos
FOR EACH ROW
WHEN (NEW.numero_pedido IS NULL OR NEW.numero_pedido = '')
EXECUTE FUNCTION fn_numero_pedido();

-- ============================================================
-- TRIGGER: número de comanda automático  COM-YYYYMMDD-NNNN
-- ============================================================
CREATE OR REPLACE FUNCTION fn_numero_comanda()
RETURNS TRIGGER AS $$
DECLARE v_seq INT;
BEGIN
    SELECT COALESCE(MAX(CAST(SUBSTRING(numero_comanda FROM '[0-9]+$') AS INT)), 0) + 1
    INTO v_seq
    FROM comandas_cocina
    WHERE numero_comanda LIKE 'COM-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-%';

    NEW.numero_comanda :=
        'COM-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' || LPAD(v_seq::TEXT, 4, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_comanda_numero
BEFORE INSERT ON comandas_cocina
FOR EACH ROW
WHEN (NEW.numero_comanda IS NULL OR NEW.numero_comanda = '')
EXECUTE FUNCTION fn_numero_comanda();

-- ============================================================
-- TRIGGER: recalcular totales del pedido
-- ============================================================
CREATE OR REPLACE FUNCTION fn_recalcular_totales_pedido()
RETURNS TRIGGER AS $$
DECLARE
    v_pid      BIGINT;
    v_subtotal NUMERIC(12,2);
    v_igv      NUMERIC(12,2);
    v_porc_igv NUMERIC(5,2);
BEGIN
    v_pid := COALESCE(NEW.pedido_id, OLD.pedido_id);

    SELECT COALESCE(porcentaje_igv, 18.00)
    INTO v_porc_igv FROM empresa LIMIT 1;

    SELECT COALESCE(SUM(subtotal), 0)
    INTO v_subtotal
    FROM detalle_pedido
    WHERE pedido_id = v_pid
      AND estado_item <> 'ANULADO';

    v_igv := ROUND(v_subtotal * v_porc_igv / 100, 2);

    UPDATE pedidos
    SET subtotal = v_subtotal,
        igv      = v_igv,
        total    = v_subtotal + v_igv
    WHERE id = v_pid;

    -- DELETE: NEW es NULL, hay que retornar OLD
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_recalcular_totales
AFTER INSERT OR UPDATE OR DELETE ON detalle_pedido
FOR EACH ROW EXECUTE FUNCTION fn_recalcular_totales_pedido();

-- ============================================================
-- 1. GESTIONAR ESTADO DE MESA SEGÚN PEDIDOS
-- ============================================================
CREATE OR REPLACE FUNCTION fn_gestionar_estado_mesa()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE mesas
        SET estado = 'OCUPADA', actualizado_en = NOW()
        WHERE id = NEW.mesa_id;

    ELSIF TG_OP = 'UPDATE' AND
          NEW.estado IN ('COBRADO', 'ANULADO') AND
          OLD.estado NOT IN ('COBRADO', 'ANULADO') THEN

        IF EXISTS (
            SELECT 1 FROM reservas
            WHERE mesa_id = NEW.mesa_id
              AND estado = 'ACTIVA'
        ) THEN
            UPDATE mesas
            SET estado = 'RESERVADA', actualizado_en = NOW()
            WHERE id = NEW.mesa_id;
        ELSE
            UPDATE mesas
            SET estado = 'LIBRE', actualizado_en = NOW()
            WHERE id = NEW.mesa_id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_gestionar_mesa ON pedidos;
CREATE TRIGGER trg_gestionar_mesa
AFTER INSERT OR UPDATE OF estado ON pedidos
FOR EACH ROW EXECUTE FUNCTION fn_gestionar_estado_mesa();

-- ============================================================
-- TABLA: reservas
-- ============================================================
CREATE TABLE reservas (
    id                  BIGSERIAL PRIMARY KEY,
    mesa_id             BIGINT        NOT NULL REFERENCES mesas(id),
    nombre_cliente      VARCHAR(150)  NOT NULL,
    telefono_cliente    VARCHAR(9),
    fecha_reserva       TIMESTAMPTZ   NOT NULL,
    tolerancia_minutos  INT           NOT NULL DEFAULT 15 CHECK (tolerancia_minutos > 0),
    personas            INT           NOT NULL DEFAULT 1  CHECK (personas > 0),
    estado              VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE'
                            CHECK (estado IN (
                                'PENDIENTE',   -- registrada, aún no llegó el cliente
                                'CONFIRMADA',  -- confirmada con el cliente
                                'ACTIVA',      -- cliente llegó, mesa ocupada por reserva
                                'COMPLETADA',  -- finalizada normalmente por el admin
                                'CANCELADA',   -- cancelada por el admin antes de llegar
                                'NO_SHOW'      -- cliente no se presentó en el tiempo de tolerancia
                            )),
    observaciones       TEXT,
    creado_en           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- ÍNDICES
-- ============================================================

-- Garantiza que una mesa no tenga dos reservas activas simultáneas
CREATE UNIQUE INDEX uq_mesa_reserva_activa
    ON reservas (mesa_id)
    WHERE estado IN ('PENDIENTE', 'CONFIRMADA', 'ACTIVA');

CREATE INDEX idx_reservas_mesa      ON reservas(mesa_id);
CREATE INDEX idx_reservas_fecha     ON reservas(fecha_reserva);
CREATE INDEX idx_reservas_estado    ON reservas(estado);

-- ============================================================
-- TRIGGER: actualizar timestamp (usa la función ya existente)
-- ============================================================
CREATE TRIGGER trg_reservas_upd
BEFORE UPDATE ON reservas
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

-- ============================================================
-- TRIGGER: gestionar estado de mesa según reservas
-- - INSERT con estado activo      → mesa RESERVADA
-- - UPDATE a ACTIVA               → mesa se mantiene RESERVADA
--   (al crear el pedido el trigger de pedidos la pasa a OCUPADA)
-- - UPDATE a COMPLETADA/CANCELADA → mesa LIBRE (si no hay pedido abierto)
-- - UPDATE a NO_SHOW              → mesa LIBRE (si no hay pedido abierto)
-- ============================================================
-- ============================================================
-- 2. GESTIONAR ESTADO DE MESA SEGÚN RESERVAS
-- ============================================================
CREATE OR REPLACE FUNCTION fn_gestionar_mesa_por_reserva()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NEW.estado IN ('PENDIENTE', 'CONFIRMADA', 'ACTIVA') THEN
        UPDATE mesas
        SET estado = 'RESERVADA', actualizado_en = NOW()
        WHERE id = NEW.mesa_id;

    ELSIF TG_OP = 'UPDATE' THEN

        IF NEW.estado = 'ACTIVA' AND OLD.estado IN ('PENDIENTE', 'CONFIRMADA') THEN
            UPDATE mesas
            SET estado = 'RESERVADA', actualizado_en = NOW()
            WHERE id = NEW.mesa_id;

        ELSIF NEW.estado IN ('COMPLETADA', 'CANCELADA', 'NO_SHOW')
          AND OLD.estado IN ('PENDIENTE', 'CONFIRMADA', 'ACTIVA') THEN

            IF NOT EXISTS (
                SELECT 1 FROM pedidos
                WHERE mesa_id = NEW.mesa_id
                  AND estado NOT IN ('COBRADO', 'ANULADO')
            ) THEN
                UPDATE mesas
                SET estado = 'LIBRE', actualizado_en = NOW()
                WHERE id = NEW.mesa_id;
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_gestionar_mesa_reserva ON reservas;
CREATE TRIGGER trg_gestionar_mesa_reserva
AFTER INSERT OR UPDATE OF estado ON reservas
FOR EACH ROW EXECUTE FUNCTION fn_gestionar_mesa_por_reserva();

-- ============================================================
-- TRIGGER: bloquear pedidos en mesas RESERVADAS
-- Protección a nivel BD: evita crear un pedido en mesa reservada
-- ============================================================
-- ============================================================
-- 3. VALIDAR MESA ANTES DE CREAR PEDIDO
-- ============================================================
CREATE OR REPLACE FUNCTION fn_validar_mesa_disponible()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM reservas
        WHERE mesa_id = NEW.mesa_id
          AND estado IN ('PENDIENTE', 'CONFIRMADA')
    ) THEN
        RAISE EXCEPTION
            'La mesa % tiene una reserva pendiente o confirmada. '
            'Espera a que el cliente llegue.',
            NEW.mesa_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_mesa_en_pedido ON pedidos;
CREATE TRIGGER trg_validar_mesa_en_pedido
BEFORE INSERT ON pedidos
FOR EACH ROW EXECUTE FUNCTION fn_validar_mesa_disponible();

-- ============================================================
-- FUNCIÓN: procesar NO_SHOW automáticamente
-- Llamar cada minuto desde pg_cron o desde el backend
-- ============================================================
CREATE OR REPLACE FUNCTION fn_procesar_no_show()
RETURNS void AS $$
DECLARE
    v_reserva RECORD;
BEGIN
    FOR v_reserva IN
        SELECT r.id, r.mesa_id
        FROM reservas r
        WHERE r.estado IN ('PENDIENTE', 'CONFIRMADA')
          AND r.fecha_reserva + (r.tolerancia_minutos || ' minutes')::INTERVAL < NOW()
    LOOP
        -- Marcar reserva como NO_SHOW
        UPDATE reservas
        SET estado = 'NO_SHOW'
        WHERE id = v_reserva.id;

        -- Liberar mesa solo si no hay pedido abierto
        IF NOT EXISTS (
            SELECT 1 FROM pedidos
            WHERE mesa_id = v_reserva.mesa_id
              AND estado NOT IN ('COBRADO', 'ANULADO')
        ) THEN
            UPDATE mesas SET estado = 'LIBRE'
            WHERE id = v_reserva.mesa_id;
        END IF;

    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- ACTIVAR pg_cron (descomentar si tienes la extensión)
-- ============================================================
-- CREATE EXTENSION IF NOT EXISTS pg_cron;
--
-- SELECT cron.schedule(
--     'procesar_no_show',
--     '* * * * *',
--     'SELECT fn_procesar_no_show()'
-- );

-- Eliminar el trigger con la condición WHEN
DROP TRIGGER IF EXISTS trg_pedidos_numero ON pedidos;

-- Recrear sin la condición WHEN
CREATE TRIGGER trg_pedidos_numero
BEFORE INSERT ON pedidos
FOR EACH ROW
EXECUTE FUNCTION fn_numero_pedido();