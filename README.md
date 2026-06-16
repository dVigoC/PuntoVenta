⚙️ Configuración del Backend
Para que el proyecto funcione localmente, debes crear el archivo application.properties en la ruta:
backend/src/main/resources/application.properties

Copia y pega la siguiente configuración, reemplazando los campos indicados entre < > con tus credenciales reales:

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

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000

# ─────────────────────────────────────────
#  JWT
# ─────────────────────────────────────────
app.jwt.secret=<TU_CLAVE_SECRETA_JWT_LARGA>
app.jwt.expiration=28800000

# ─────────────────────────────────────────
#  SUPABASE STORAGE
# ─────────────────────────────────────────
supabase.url=<TU_URL_DE_SUPABASE>
supabase.key=<TU_SUPABASE_SERVICE_ROLE_KEY>
supabase.bucket=logos-empresa

# ─────────────────────────────────────────
#  CONFIGURACIONES ADICIONALES
# ─────────────────────────────────────────
spring.jpa.properties.hibernate.jdbc.time_zone=America/Lima
server.tomcat.uri-encoding=UTF-8
