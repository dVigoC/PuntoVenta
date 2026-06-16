package com.Venta.PuntoVenta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.Venta.PuntoVenta.model.Pedido;

import java.util.List;

public interface GraficasRepository extends JpaRepository<Pedido, Long> {

    // ══════════════════════════════════════════════════════════════════
    // 1. VENTAS TOTALES POR MES (últimos 12 meses)
    //    Columnas: [0]=mes_label (Ene, Feb …), [1]=total NUMERIC
    // ══════════════════════════════════════════════════════════════════
    @Query(value = """
            SELECT TO_CHAR(p.creado_en AT TIME ZONE 'America/Lima', 'YYYY-MM') AS mes,
                   TO_CHAR(p.creado_en AT TIME ZONE 'America/Lima', 'Mon')     AS mes_label,
                   COALESCE(SUM(p.total), 0)                                   AS total
            FROM   pedidos p
            WHERE  p.estado = 'COBRADO'
              AND  p.creado_en >= (NOW() AT TIME ZONE 'America/Lima' - INTERVAL '11 months')
                                   ::DATE ::TIMESTAMPTZ
            GROUP BY mes, mes_label
            ORDER BY mes ASC
            """, nativeQuery = true)
    List<Object[]> ventasPorMes();

    // ══════════════════════════════════════════════════════════════════
    // 2. HEATMAP: ventas por DÍA DE SEMANA × HORA DEL DÍA (últimos 90 días)
    //    Columnas: [0]=dia_semana INT(0=Dom…6=Sáb), [1]=hora INT(0-23),
    //              [2]=total NUMERIC
    // ══════════════════════════════════════════════════════════════════
    @Query(value = """
            SELECT EXTRACT(DOW  FROM p.creado_en AT TIME ZONE 'America/Lima')::INT AS dia_semana,
                   EXTRACT(HOUR FROM p.creado_en AT TIME ZONE 'America/Lima')::INT AS hora,
                   COALESCE(SUM(p.total), 0)                                        AS total
            FROM   pedidos p
            WHERE  p.estado = 'COBRADO'
              AND  p.creado_en >= NOW() - INTERVAL '90 days'
            GROUP BY dia_semana, hora
            ORDER BY dia_semana, hora
            """, nativeQuery = true)
    List<Object[]> heatmapVentasDiasHoras();

    // ══════════════════════════════════════════════════════════════════
    // 3. TOP 10 PRODUCTOS MÁS VENDIDOS (por cantidad, últimos 30 días)
    //    Columnas: [0]=nombre_producto, [1]=categoria, [2]=cantidad INT,
    //              [3]=total_ingresos NUMERIC
    // ══════════════════════════════════════════════════════════════════
    @Query(value = """
            SELECT pr.nombre                         AS nombre_producto,
                   cat.nombre                        AS categoria,
                   COALESCE(SUM(dp.cantidad), 0)     AS cantidad,
                   COALESCE(SUM(dp.subtotal), 0)     AS total_ingresos
            FROM   detalle_pedido dp
            JOIN   pedidos        p   ON p.id  = dp.pedido_id
            JOIN   productos      pr  ON pr.id = dp.producto_id
            JOIN   categorias_producto cat ON cat.id = pr.categoria_id
            WHERE  p.estado    = 'COBRADO'
              AND  dp.estado_item <> 'ANULADO'
              AND  p.creado_en >= NOW() - INTERVAL '30 days'
            GROUP BY pr.nombre, cat.nombre
            ORDER BY cantidad DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> topProductos();

    // ══════════════════════════════════════════════════════════════════
    // 4. VENTAS POR CATEGORÍA (últimos 30 días)
    //    Columnas: [0]=categoria, [1]=total NUMERIC, [2]=porcentaje NUMERIC
    // ══════════════════════════════════════════════════════════════════
    @Query(value = """
            WITH cat_totales AS (
                SELECT cat.nombre                      AS categoria,
                       COALESCE(SUM(dp.subtotal), 0)  AS total
                FROM   detalle_pedido dp
                JOIN   pedidos        p   ON p.id  = dp.pedido_id
                JOIN   productos      pr  ON pr.id = dp.producto_id
                JOIN   categorias_producto cat ON cat.id = pr.categoria_id
                WHERE  p.estado     = 'COBRADO'
                  AND  dp.estado_item <> 'ANULADO'
                  AND  p.creado_en  >= NOW() - INTERVAL '30 days'
                GROUP BY cat.nombre
            ),
            gran_total AS (SELECT SUM(total) AS gt FROM cat_totales)
            SELECT ct.categoria,
                   ct.total,
                   ROUND(ct.total * 100.0 / NULLIF(gt.gt, 0), 1) AS porcentaje
            FROM   cat_totales ct, gran_total gt
            ORDER BY ct.total DESC
            """, nativeQuery = true)
    List<Object[]> ventasPorCategoria();

    // ══════════════════════════════════════════════════════════════════
    // 5. TICKET PROMEDIO POR DÍA (últimos 30 días)
    //    Columnas: [0]=dia DATE, [1]=ticket_promedio NUMERIC,
    //              [2]=num_pedidos BIGINT
    // ══════════════════════════════════════════════════════════════════
    @Query(value = """
            SELECT (p.creado_en AT TIME ZONE 'America/Lima')::DATE AS dia,
                   ROUND(AVG(p.total), 2)                          AS ticket_promedio,
                   COUNT(*)                                         AS num_pedidos
            FROM   pedidos p
            WHERE  p.estado = 'COBRADO'
              AND  p.creado_en >= NOW() - INTERVAL '30 days'
            GROUP BY dia
            ORDER BY dia ASC
            """, nativeQuery = true)
    List<Object[]> ticketPromedioPorDia();

    // ══════════════════════════════════════════════════════════════════
    // 6. PEDIDOS POR ESTADO HOY (para dona/donut)
    //    Columnas: [0]=estado, [1]=cantidad BIGINT
    // ══════════════════════════════════════════════════════════════════
    @Query(value = """
            SELECT p.estado,
                   COUNT(*) AS cantidad
            FROM   pedidos p
            WHERE  (p.creado_en AT TIME ZONE 'America/Lima')::DATE = CURRENT_DATE
            GROUP BY p.estado
            ORDER BY cantidad DESC
            """, nativeQuery = true)
    List<Object[]> pedidosPorEstadoHoy();

    // ══════════════════════════════════════════════════════════════════
    // 7. COMPARATIVA: semana actual vs semana anterior
    //    Columnas: [0]=dia_semana INT, [1]=total_actual, [2]=total_anterior
    // ══════════════════════════════════════════════════════════════════
    @Query(value = """
            SELECT sub.dia,
                   COALESCE(SUM(CASE WHEN sub.semana = 0 THEN sub.total END), 0) AS total_actual,
                   COALESCE(SUM(CASE WHEN sub.semana = 1 THEN sub.total END), 0) AS total_anterior
            FROM (
                SELECT EXTRACT(DOW FROM p.creado_en AT TIME ZONE 'America/Lima')::INT AS dia,
                       p.total,
                       CASE
                           WHEN p.creado_en >= date_trunc('week', NOW() AT TIME ZONE 'America/Lima')
                                              AT TIME ZONE 'America/Lima'
                           THEN 0
                           ELSE 1
                       END AS semana
                FROM pedidos p
                WHERE p.estado = 'COBRADO'
                  AND p.creado_en >= date_trunc('week', NOW() AT TIME ZONE 'America/Lima')
                                     AT TIME ZONE 'America/Lima' - INTERVAL '7 days'
            ) sub
            GROUP BY sub.dia
            ORDER BY sub.dia
            """, nativeQuery = true)
    List<Object[]> comparativaSemanas();

    // ══════════════════════════════════════════════════════════════════
    // 8. KPIs RÁPIDOS: mes actual vs mes anterior
    //    Columnas: [0]=periodo ('actual'|'anterior'), [1]=total_ventas,
    //              [2]=num_pedidos, [3]=ticket_promedio
    // ══════════════════════════════════════════════════════════════════
    @Query(value = """
            SELECT 'actual'                       AS periodo,
                   COALESCE(SUM(p.total), 0)      AS total_ventas,
                   COUNT(*)                        AS num_pedidos,
                   COALESCE(ROUND(AVG(p.total),2), 0) AS ticket_promedio
            FROM pedidos p
            WHERE p.estado = 'COBRADO'
              AND date_trunc('month', p.creado_en AT TIME ZONE 'America/Lima')
                = date_trunc('month', NOW() AT TIME ZONE 'America/Lima')
            UNION ALL
            SELECT 'anterior',
                   COALESCE(SUM(p.total), 0),
                   COUNT(*),
                   COALESCE(ROUND(AVG(p.total),2), 0)
            FROM pedidos p
            WHERE p.estado = 'COBRADO'
              AND date_trunc('month', p.creado_en AT TIME ZONE 'America/Lima')
                = date_trunc('month', NOW() AT TIME ZONE 'America/Lima') - INTERVAL '1 month'
            """, nativeQuery = true)
    List<Object[]> kpisMesActualVsAnterior();
}