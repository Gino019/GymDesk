# GymDesk Demo — Sistema de gestión de gimnasio

Aplicación de escritorio Java Swing (JFrame), Java 17 y SQLite. Proyecto demostrativo para importar en IntelliJ IDEA, Eclipse o NetBeans como proyecto Maven.

## Requisitos
- JDK 17 o superior.
- Maven 3.8+ (el IDE puede importar y resolver el `pom.xml` automáticamente).
- Conexión a Internet la primera vez para descargar SQLite JDBC y plugins Maven.

## Ejecutar
1. Descomprime el proyecto.
2. Abre la carpeta `GymDeskDemo` desde tu IDE como proyecto Maven.
3. Espera a que Maven descargue las dependencias.
4. Ejecuta `com.gymdesk.app.GymDeskApp` o desde una terminal ejecuta `mvn clean compile exec:java`.
5. La base de datos `gymdesk.db` se crea en el directorio de trabajo al iniciar.

## Módulos incluidos
- Panel principal con indicadores e ingresos del mes.
- Socios: alta, edición, eliminación y consulta.
- Membresías: consulta de planes y alta de nuevos planes.
- Cobros: registro de pagos, método, concepto y número interno de comprobante.
- Rutinas: asignación de rutinas por socio.
- Asistencia: registro de ingresos con fecha y hora.
- Facturación: consulta de comprobantes internos de demostración.
- Reportes: indicadores básicos.

## Arquitectura y buenas prácticas
- `domain`: modelos del dominio (`Member`, `Plan`, `Payment`).
- `repository`: contrato `GymRepository` e implementación SQLite `SqliteGymRepository`.
- `service`: reglas y validaciones de negocio en `GymService`.
- `ui`: interfaz Swing; no contiene consultas SQL.
- `app`: punto de entrada.

Se aplica inversión de dependencias entre servicio y repositorio, responsabilidad única por capa, encapsulación, validación en servicios y consultas parametrizadas (`PreparedStatement`).

## Datos de demostración
Al primer inicio se crean tres planes y tres socios ficticios. Los registros que agregues se guardan en SQLite y permanecen al cerrar la aplicación. Para reiniciar la demo, cierra la aplicación y elimina `gymdesk.db`; se volverá a crear con los datos de ejemplo.

## Alcance importante
Esta versión es una demo funcional inicial, no un producto listo para producción. La sección de facturación genera identificadores y notas internas; **no emite comprobantes electrónicos ni está integrada con SUNAT**. Tampoco incluye autenticación, roles, copias de seguridad ni control avanzado de renovaciones. Esos módulos pueden incorporarse en siguientes iteraciones.
