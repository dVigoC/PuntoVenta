package com.Venta.PuntoVenta.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucket;

    // Sube el archivo y retorna la URL pública
    public String subirLogo(MultipartFile archivo) throws Exception {

        // Nombre único para evitar colisiones
        String nombreArchivo = "logo-empresa-" + System.currentTimeMillis()
            + getExtension(archivo.getOriginalFilename());

        String uploadUrl = supabaseUrl
            + "/storage/v1/object/"
            + bucket + "/"
            + nombreArchivo;

        WebClient client = WebClient.builder()
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
            .defaultHeader("x-upsert", "true") // sobreescribe si existe
            .build();

        client.post()
            .uri(uploadUrl)
            .contentType(MediaType.parseMediaType(
                archivo.getContentType() != null
                    ? archivo.getContentType()
                    : "image/png"))
            .bodyValue(archivo.getBytes())
            .retrieve()
            .toBodilessEntity()
            .block();

        // URL pública del archivo
        return supabaseUrl
            + "/storage/v1/object/public/"
            + bucket + "/"
            + nombreArchivo;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".png";
        return filename.substring(filename.lastIndexOf("."));
    }
}