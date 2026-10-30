# Nexo Launcher

Launcher minimalista para Android al estilo Niagara, con más opciones en la pantalla principal.

## Funciones

- **Lista de apps por texto**, sin iconos por defecto, con buscador.
- **Cajón por categorías** (Comunicación, Bancos, Inteligencia artificial, Notas y guardados, Juegos…): arriba aparecen las 10 últimas apps usadas y debajo las categorías, plegadas hasta que las tocas. Las apps de mensajería (WhatsApp, Telegram, Signal…) van siempre en Comunicación. Mantén pulsada una app → *Cambiar categoría…* para corregirla. En Ajustes se puede volver a la lista alfabética con barra A–Z.
- **Iconos opcionales** por separado en inicio y en el cajón: sin iconos, color, grises o silueta monocroma.
- **Widgets**: cualquier widget de tus apps, más reloj, fecha, batería y próxima alarma integrados.
- **Siluetas de teléfono, cámara y mensajes** (negras y semitransparentes) siempre al pie de la pantalla de inicio (se usan tus apps predeterminadas).
- **Carpetas** que se despliegan en la propia lista de inicio.
- **Apps ocultas**, que se pueden volver a mostrar desde Ajustes.
- **Personalización**: color del texto, color de acento, fuente (incluye nueve tipografías finas: Inter, Lato, Manrope, Nunito Sans, Work Sans, Montserrat, Raleway, Jost y Quicksand), tamaño, alineación y oscurecido del fondo.
- **Segunda pantalla de widgets** (desliza a la izquierda en inicio): coloca los widgets donde quieras y cámbiales el tamaño libremente (mantén pulsado el fondo → *Mover y cambiar tamaño de widgets*).
- Gestos: deslizar arriba abre las apps, deslizar abajo abre las notificaciones, mantener pulsado el fondo abre el menú.

## Instalar

1. Descarga `NexoLauncher.apk` (sección *Releases* del repositorio) en el móvil.
2. Ábrelo y permite instalar apps de origen desconocido si te lo pide.
3. Pulsa el botón Inicio y elige **Nexo Launcher** como predeterminado.

## Compilar

Cada `push` a GitHub compila el APK con GitHub Actions. En local: abre la carpeta en Android Studio y ejecuta `gradle :app:assembleRelease`.

Requisitos: Android 8.0 o superior.
