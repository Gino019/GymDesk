# Manual de Usuario y Guía de Operación — GymDesk

**Versión:** 1.5 Commercial Edition  
**Entorno:** Desktop (Java 17, Swing, SQLite JDBC)  
**Destinatarios:** Dueños de gimnasio, administradores y personal de recepción.

---

## 1. Introducción
**GymDesk** es una solución de escritorio diseñada específicamente para la gestión operativa y administrativa de gimnasios comunitarios, de barrio y centros de entrenamiento independientes. Su arquitectura offline permite operar con rapidez y confiabilidad sin requerir conexión permanente a Internet.

---

## 2. Requisitos del Sistema
- **Sistema Operativo:** Windows 10 o Windows 11 (64 bits).
- **Entorno de Ejecución:** Java 17 LTS o superior.
- **Espacio en Disco:** 100 MB mínimo (incluye motor de base de datos y respaldos).

---

## 3. Inicio Rápido

### Ejecución Directa
Hacer doble clic sobre el archivo:
```text
iniciar_gymdesk.bat
```
O desde la consola de comandos:
```powershell
java -jar GymDeskDemo\target\GymDesk.jar
```

---

## 4. Credenciales Iniciales y Roles

GymDesk implementa seguridad con derivación criptográfica **PBKDF2-HmacSHA256 con sal (salt)**. Al iniciar el programa por primera vez se crean dos usuarios predeterminados:

| Usuario | Contraseña Inicial | Rol | Permisos |
|---|---|---|---|
| `admin` | `admin123` | **ADMINISTRADOR** | Acceso total: socios, membresías, cobros, anulaciones, reportes, usuarios, respaldos y configuración. |
| `recepcion` | `recepcion123` | **RECEPCIONISTA** | Registro de socios, renovación, cobros, promociones, asistencia, consulta de rutinas e impresión de recibos. |

> [!IMPORTANT]
> Se recomienda cambiar la contraseña del usuario `admin` inmediatamente después del primer inicio desde el módulo **Usuarios y Permisos**.

---

## 5. Módulos del Sistema

### 5.1 Panel Principal (Dashboard)
- **Tarjetas KPI en tiempo real:**
  - Socios registrados en el sistema.
  - Asistencias registradas hoy.
  - Ingresos netos del mes en curso.
  - Socios con membresía por vencer en los próximos 7 días.
- **Barra de Acceso Rápido:** Botones de un clic para registrar socios, cobros, marcar asistencia o asignar rutinas.
- **Actividad en Vivo:**
  - Lista de los últimos cobros emitidos con estado contable.
  - Lista de asistencias recientes del día de hoy con indicación de hora y plan.

### 5.2 Módulo de Socios y Ficha 360°
- **Alta y Edición:** Registro con documento (DNI/CE/Pasaporte), nombre completo, teléfono y correo electrónico.
- **Baja Lógica:** Nunca se eliminan socios físicamente si cuentan con registros contables o de asistencia; pasan a estado `Inactivo`.
- **Ficha 360° del Socio:** Al seleccionar un socio y presionar **"👁 Ficha 360° del socio"**, se despliega una ventana unificada con cinco pestañas:
  1. *Datos Generales:* Información de contacto y vigencia.
  2. *Membresías:* Historial cronológico de todos los contratos y renovaciones.
  3. *Cobros:* Todos los recibos y pagos efectuados.
  4. *Asistencias:* Historial de ingresos al local.
  5. *Rutinas:* Planes de entrenamiento asignados.

### 5.3 Membresías y Renovación Inteligente
- **Cálculo de Renovación:**
  - *Socio Vigente (Renovación Anticipada):* La nueva membresía inicia automáticamente el día siguiente a la fecha de vencimiento actual (`fecha_fin + 1 día`). El socio nunca pierde los días que ya pagó por adelantado.
  - *Socio Vencido o Nuevo:* La membresía inicia a partir del día del cobro.
- **Configuración de Planes:** Nombre, duración en días, precio oficial y descripción (exclusivo para Administradores).

### 5.4 Cobros y Caja (Modelo Mixto de Ofertas)
- **Modelo Mixto de Promociones:** El sistema analiza las promociones vigentes compatibles con el plan seleccionado (descuentos porcentuales o fijos). El recepcionista decide si aplica una de las ofertas disponibles o cobra la tarifa regular, visualizando el desglose y monto final en tiempo real.
- **Auditoría de Anulación:** Un cobro nunca se borra físicamente. La anulación requiere rol de Administrador y motivo obligatorio justificado, marcando el registro como `Anulado` y restándolo de los ingresos netos.

### 5.5 Rutinas y Catálogo de Ejercicios
- **Catálogo de Ejercicios:** Banco de ejercicios clasificados por grupo muscular (Pecho, Espalda, Piernas, Hombros, Brazos, Core, Cardio).
- **Constructor de Rutinas:** Permite confeccionar un entrenamiento asignando series, repeticiones, tiempo de descanso en segundos y notas técnicas por ejercicio.

### 5.6 Control de Asistencia
- **Marcación Rápida:** Búsqueda ágil por número de documento o nombre.
- **Detección Automática de Vencimiento:** Si el socio tiene la membresía vencida o se encuentra inactivo, el sistema emite una alerta visual destacada en pantalla solicitando la regularización inmediata en caja.

### 5.7 Comprobantes Internos (Normativa Perú)
- **Emisión de Recibo en PDF:** Formato ticket administrativo A6 descargable con logo comercial, detalle del cobro, desglose de promociones y datos del gimnasio.
- **Aviso Legal Tributario Obligatorio:** En cumplimiento de la normativa peruana, cada recibo emitido incluye de manera visible la leyenda:
  > *"Comprobante de control administrativo interno. No válido para efectos tributarios SUNAT."*

### 5.8 Reportes Financieros
- Filtro por rango de fechas personalizable.
- Cálculo de Ingresos Netos, Total de Descuentos Otorgados, Cobros Vigentes, Cobros Anulados y Asistencias del período.
- **Exportación Oficial A4 a PDF:** Genera un consolidado ejecutivo listo para imprimir o enviar al contador.

### 5.9 Usuarios y Permisos (RBAC)
- Creación de cuentas para el personal con roles definidos.
- Activación y desactivación de usuarios.
- Restablecimiento seguro de contraseñas.
- Protección del sistema contra la desactivación o degradación del único administrador activo.

### 5.10 Registro de Auditoría
- Bitácora inmutable en base de datos de todas las operaciones sensibles:
  - Inicios y cierres de sesión.
  - Cobros y anulaciones con motivo.
  - Creación y renovación de socios.
  - Cambios de configuración del gimnasio.
  - Creación y restauración de copias de seguridad.

### 5.11 Copias de Seguridad (Respaldos) y Restauración
- **Snapshot Online:** Gracias al comando nativo `VACUUM INTO`, se pueden generar copias exactas y consistentes de la base de datos `gymdesk.db` sin interrumpir la operación ni cerrar el programa.
- **Restauración Segura:** Valida la integridad física del archivo `.db` antes de restaurarlo y crea automáticamente una copia de resguardo de emergencia previa (`.pre_restore.bak`).

---

## 6. Mantenimiento y Buenas Prácticas
1. **Copias Externas:** Se recomienda realizar una copia de seguridad al menos una vez por semana hacia una memoria USB o disco externo mediante el módulo **Respaldos**.
2. **Personalización Comercial:** En el módulo **Configuración**, configure el nombre de su gimnasio, RUC o documento comercial, dirección, teléfono y moneda local antes de comenzar a emitir recibos.

