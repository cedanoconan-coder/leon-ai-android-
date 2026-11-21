# León AI para Android 🦁

MVP Android independiente de KLK Manin. Permite comandos por texto y voz: `inicio`, `atrás`, `ajustes`.

## Compilación
Java 17, Android SDK 35, Gradle 8.9. Ejecutar `gradle :app:assembleDebug`. GitHub Actions intenta compilar y adjuntar `leon-ai-debug-apk`.

## Privacidad
El micrófono se solicita al usar voz. Accesibilidad se habilita manualmente y solo ejecuta Inicio/Atrás; no lee contenido de pantalla. El reconocimiento de voz puede depender del proveedor del teléfono. No hay IA remota ni memoria todavía.

## Próximas fases
Confirmaciones de acciones sensibles, comprensión de intenciones, visión con consentimiento y memoria local cifrada.
