package com.Venta.PuntoVenta.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita el scheduler de Spring para procesar NO_SHOW automáticamente.
 * ReservaService#procesarNoShow() se ejecuta cada 60 segundos.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}