package com.Venta.PuntoVenta.service;

import com.Venta.PuntoVenta.repository.GraficasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Servicio de analíticas / gráficas.
 *
 * Convierte los Object[] que devuelven las native queries del
 * GraficasRepository en Maps / Lists simples que el controlador
 * serializará a JSON para Chart.js.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraficasService {

    private final GraficasRepository graficasRepository;

    // ── Etiquetas localizadas ─────────────────────────────────────────
    private static final String[] DIAS_ES  =
            {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
    private static final String[] MESES_ES =
            {"Ene","Feb","Mar","Abr","May","Jun",
             "Jul","Ago","Sep","Oct","Nov","Dic"};

    // ══════════════════════════════════════════════════════════════════
    // 1. Ventas por mes
    // ══════════════════════════════════════════════════════════════════
    public Map<String, Object> getVentasPorMes() {
        List<Object[]> rows = graficasRepository.ventasPorMes();

        List<String>     labels = new ArrayList<>();
        List<BigDecimal> totales = new ArrayList<>();

        for (Object[] r : rows) {
            // r[0] = YYYY-MM (para ordenar), r[1] = Mon (etiqueta), r[2] = total
            labels.add(traducirMesAbrev((String) r[1]));
            totales.add(toBigDecimal(r[2]));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels",  labels);
        out.put("totales", totales);
        return out;
    }

    // ══════════════════════════════════════════════════════════════════
    // 2. Heatmap ventas días × horas
    // ══════════════════════════════════════════════════════════════════
    public Map<String, Object> getHeatmap() {
        List<Object[]> rows = graficasRepository.heatmapVentasDiasHoras();

        // Matriz 7 días × 24 horas inicializada a 0
        double[][] matriz = new double[7][24];
        double maxVal = 0;

        for (Object[] r : rows) {
            int dia  = toInt(r[0]);
            int hora = toInt(r[1]);
            double total = toBigDecimal(r[2]).doubleValue();
            if (dia >= 0 && dia < 7 && hora >= 0 && hora < 24) {
                matriz[dia][hora] = total;
                if (total > maxVal) maxVal = total;
            }
        }

        // Construir lista de puntos {x, y, v} para Chart.js matrix plugin
        List<Map<String, Object>> data = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            for (int h = 0; h < 24; h++) {
                Map<String, Object> punto = new LinkedHashMap<>();
                punto.put("x", h);              // hora → eje X
                punto.put("y", DIAS_ES[d]);     // día  → eje Y
                punto.put("v", matriz[d][h]);   // valor
                data.add(punto);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("data",   data);
        out.put("maxVal", maxVal);
        out.put("dias",   Arrays.asList(DIAS_ES));
        return out;
    }

    // ══════════════════════════════════════════════════════════════════
    // 3. Top 10 productos
    // ══════════════════════════════════════════════════════════════════
    public Map<String, Object> getTopProductos() {
        List<Object[]> rows = graficasRepository.topProductos();

        List<String>     nombres    = new ArrayList<>();
        List<Integer>    cantidades = new ArrayList<>();
        List<BigDecimal> ingresos   = new ArrayList<>();
        List<String>     categorias = new ArrayList<>();

        for (Object[] r : rows) {
            nombres.add((String) r[0]);
            categorias.add((String) r[1]);
            cantidades.add(toInt(r[2]));
            ingresos.add(toBigDecimal(r[3]));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels",     nombres);
        out.put("cantidades", cantidades);
        out.put("ingresos",   ingresos);
        out.put("categorias", categorias);
        return out;
    }

    // ══════════════════════════════════════════════════════════════════
    // 4. Ventas por categoría (donut)
    // ══════════════════════════════════════════════════════════════════
    public Map<String, Object> getVentasPorCategoria() {
        List<Object[]> rows = graficasRepository.ventasPorCategoria();

        List<String>     labels      = new ArrayList<>();
        List<BigDecimal> totales     = new ArrayList<>();
        List<Double>     porcentajes = new ArrayList<>();

        for (Object[] r : rows) {
            labels.add((String) r[0]);
            totales.add(toBigDecimal(r[1]));
            porcentajes.add(r[2] == null ? 0.0 : ((Number) r[2]).doubleValue());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels",      labels);
        out.put("totales",     totales);
        out.put("porcentajes", porcentajes);
        return out;
    }

    // ══════════════════════════════════════════════════════════════════
    // 5. Ticket promedio por día
    // ══════════════════════════════════════════════════════════════════
    public Map<String, Object> getTicketPromedioPorDia() {
        List<Object[]> rows = graficasRepository.ticketPromedioPorDia();

        List<String>     labels   = new ArrayList<>();
        List<BigDecimal> tickets  = new ArrayList<>();
        List<Long>       pedidos  = new ArrayList<>();

        for (Object[] r : rows) {
            labels.add(r[0].toString());   // fecha YYYY-MM-DD
            tickets.add(toBigDecimal(r[1]));
            pedidos.add(toLong(r[2]));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels",  labels);
        out.put("tickets", tickets);
        out.put("pedidos", pedidos);
        return out;
    }

    // ══════════════════════════════════════════════════════════════════
    // 6. Pedidos por estado hoy (donut)
    // ══════════════════════════════════════════════════════════════════
    public Map<String, Object> getPedidosPorEstadoHoy() {
        List<Object[]> rows = graficasRepository.pedidosPorEstadoHoy();

        List<String> labels     = new ArrayList<>();
        List<Long>   cantidades = new ArrayList<>();

        for (Object[] r : rows) {
            labels.add(traducirEstado((String) r[0]));
            cantidades.add(toLong(r[1]));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels",     labels);
        out.put("cantidades", cantidades);
        return out;
    }

    // ══════════════════════════════════════════════════════════════════
    // 7. Comparativa semana actual vs anterior
    // ══════════════════════════════════════════════════════════════════
    public Map<String, Object> getComparativaSemanas() {
        List<Object[]> rows = graficasRepository.comparativaSemanas();

        // inicializar con ceros para todos los días 0-6
        double[] actual   = new double[7];
        double[] anterior = new double[7];

        for (Object[] r : rows) {
            int dia = toInt(r[0]);
            if (dia >= 0 && dia < 7) {
                actual[dia]   = toBigDecimal(r[1]).doubleValue();
                anterior[dia] = toBigDecimal(r[2]).doubleValue();
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels",   Arrays.asList(DIAS_ES));
        out.put("actual",   toList(actual));
        out.put("anterior", toList(anterior));
        return out;
    }

    // ══════════════════════════════════════════════════════════════════
    // 8. KPIs mes actual vs anterior
    // ══════════════════════════════════════════════════════════════════
    public Map<String, Object> getKpis() {
        List<Object[]> rows = graficasRepository.kpisMesActualVsAnterior();

        Map<String, Object> actual   = new LinkedHashMap<>();
        Map<String, Object> anterior = new LinkedHashMap<>();

        for (Object[] r : rows) {
            String periodo = (String) r[0];
            Map<String, Object> dest = "actual".equals(periodo) ? actual : anterior;
            dest.put("totalVentas",     toBigDecimal(r[1]));
            dest.put("numPedidos",      toLong(r[2]));
            dest.put("ticketPromedio",  toBigDecimal(r[3]));
        }

        // Calcular variaciones porcentuales
        double varVentas  = calcVariacion(actual, anterior, "totalVentas");
        double varPedidos = calcVariacion(actual, anterior, "numPedidos");
        double varTicket  = calcVariacion(actual, anterior, "ticketPromedio");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("actual",    actual);
        out.put("anterior",  anterior);
        out.put("varVentas",  varVentas);
        out.put("varPedidos", varPedidos);
        out.put("varTicket",  varTicket);
        return out;
    }

    // ══════════════════════════════════════════════════════════════════
    // Helpers privados
    // ══════════════════════════════════════════════════════════════════

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    private int toInt(Object o) {
        if (o == null) return 0;
        return ((Number) o).intValue();
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        return ((Number) o).longValue();
    }

    private List<Double> toList(double[] arr) {
        List<Double> list = new ArrayList<>();
        for (double v : arr) list.add(v);
        return list;
    }

    private double calcVariacion(Map<String, Object> actual,
                                  Map<String, Object> anterior,
                                  String key) {
        if (anterior.isEmpty()) return 0.0;
        double ant = ((Number) anterior.getOrDefault(key, 0)).doubleValue();
        double act = ((Number) actual.getOrDefault(key, 0)).doubleValue();
        if (ant == 0) return act > 0 ? 100.0 : 0.0;
        return Math.round(((act - ant) / ant) * 1000.0) / 10.0; // 1 decimal
    }

    private String traducirEstado(String estado) {
        return switch (estado == null ? "" : estado) {
            case "ABIERTO"   -> "Abierto";
            case "EN_COCINA" -> "En cocina";
            case "SERVIDO"   -> "Servido";
            case "COBRADO"   -> "Cobrado";
            case "ANULADO"   -> "Anulado";
            default          -> estado;
        };
    }

    private String traducirMesAbrev(String mesEn) {
        // PostgreSQL TO_CHAR 'Mon' devuelve en inglés
        return switch (mesEn == null ? "" : mesEn.trim()) {
            case "Jan" -> "Ene"; case "Feb" -> "Feb"; case "Mar" -> "Mar";
            case "Apr" -> "Abr"; case "May" -> "May"; case "Jun" -> "Jun";
            case "Jul" -> "Jul"; case "Aug" -> "Ago"; case "Sep" -> "Sep";
            case "Oct" -> "Oct"; case "Nov" -> "Nov"; case "Dec" -> "Dic";
            default    -> mesEn;
        };
    }
}