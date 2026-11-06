# VerticalCut

Editor de video vertical (9:16) para Android, escrito en Kotlin con Jetpack Compose y Media3.

## Qué hace
- Importa hasta 10 videos de la galería (sin pedir permisos de almacenamiento).
- Línea de tiempo con miniaturas: tocar un clip lo selecciona y mueve el cabezal.
- Dividir, recortar, reordenar y eliminar clips.
- Texto sobre el video, música de fondo con volumen, 6 filtros de color.
- Vista previa en tiempo real y exportación a MP4 1080×1920 (H.264 + AAC) en `Movies/VerticalCut`.

## Compilar el APK
1. Instala Android Studio (Ladybug o más reciente) con el SDK 35.
2. Abre la carpeta del proyecto (`File › Open`). Gradle descargará las dependencias la primera vez.
3. `Build › Build Bundle(s) / APK(s) › Build APK(s)`.
4. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

Desde terminal:
```
./gradlew assembleDebug
```

## Requisitos del dispositivo
Android 8.0 (API 26) o superior. La vista previa de filtros usa efectos de GPU; si el dispositivo no los soporta, los filtros igual se aplican al exportar.

## Estructura
```
app/src/main/java/com/verticalcut/app/
  MainActivity.kt          navegación Inicio ↔ Editor
  edit/Project.kt          modelo: clips, texto, música, filtros
  edit/EditorViewModel.kt  estado y operaciones de edición
  edit/Filters.kt          filtros como efectos de Media3
  edit/Exporter.kt         exportación con Media3 Transformer
  ui/HomeScreen.kt         pantalla inicial
  ui/EditorScreen.kt       vista previa, línea de tiempo, herramientas
  ui/Theme.kt              colores de la app
```
