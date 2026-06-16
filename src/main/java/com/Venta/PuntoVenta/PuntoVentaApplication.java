package com.Venta.PuntoVenta;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PuntoVentaApplication {

	public static void main(String[] args) {
		// Forzar JVM a usar zona de Perú
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
		SpringApplication.run(PuntoVentaApplication.class, args);
	}

}
