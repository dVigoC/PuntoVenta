# PUNTO DE VENTA - Sistema de Gestión

Proyecto integral de gestión diseñado con **Spring Boot** (BACKEND Y FRONTEND) y **Supabase** (Base de datos).

---

## ⚙️ Configuración del Backend

Para ejecutar el proyecto, debes crear el archivo `backend/src/main/resources/application.properties` en tu entorno local. Copia la siguiente estructura y completa con tus credenciales:

```properties
# ─────────────────────────────────────────
#  APLICACIÓN
# ─────────────────────────────────────────
spring.application.name=pos-restaurante
server.port=8080

# ─────────────────────────────────────────
#  BASE DE DATOS — SUPABASE (PostgreSQL)
# ─────────────────────────────────────────
spring.datasource.url=jdbc:postgresql://<TU_HOST_DE_SUPABASE>:5432/postgres
spring.datasource.username=postgres.<TU_USERNAME_DB>
spring.datasource.password=<TU_PASSWORD_DB>
spring.datasource.driver-class-name=org.postgresql.Driver

# ─────────────────────────────────────────
#  JPA / HIBERNATE
# ─────────────────────────────────────────
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
# Optimizaciones de Hibernate
spring.jpa.properties.hibernate.jdbc.fetch_size=100
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=600000

# ─────────────────────────────────────────
#  CACHÉ
# ─────────────────────────────────────────
spring.cache.type=simple
spring.cache.cache-names=productos,usuarios,categorias,roles,statsProductos,statsUsuarios
spring.cache.simple.expiration.productos=60000
spring.cache.simple.expiration.usuarios=60000
spring.cache.simple.expiration.categorias=300000
spring.cache.simple.expiration.roles=300000
spring.cache.simple.expiration.statsProductos=30000
spring.cache.simple.expiration.statsUsuarios=30000

# ─────────────────────────────────────────
#  THYMELEAF
# ─────────────────────────────────────────
spring.thymeleaf.cache=false
spring.thymeleaf.encoding=UTF-8
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html

# ─────────────────────────────────────────
#  JWT
# ─────────────────────────────────────────
app.jwt.secret=<TU_CLAVE_SECRETA_JWT>
app.jwt.expiration=28800000

# ─────────────────────────────────────────
#  SUBIDA DE ARCHIVOS
# ─────────────────────────────────────────
spring.servlet.multipart.max-file-size=2MB
spring.servlet.multipart.max-request-size=2MB

# ─────────────────────────────────────────
#  SUPABASE STORAGE
# ─────────────────────────────────────────
supabase.url=<TU_URL_DE_SUPABASE>
supabase.key=<TU_SUPABASE_SERVICE_ROLE_KEY>
supabase.bucket=logos-empresa

# ─────────────────────────────────────────
#  ZONA HORARIA — PERÚ
# ─────────────────────────────────────────
spring.jpa.properties.hibernate.jdbc.time_zone=America/Lima
server.tomcat.uri-encoding=UTF-8
spring.jpa.properties.hibernate.timezone.default_storage=NORMALIZE
