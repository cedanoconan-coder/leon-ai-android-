# León AI · inteligencia y búsqueda web (fase inicial)
La web de GitHub Pages es estática: NO coloques una API key en HTML, JavaScript, Android ni en el repositorio.
Esta rama incluye un servidor Node.js privado en `server/leon-api.mjs` que usa OpenAI Responses API y la herramienta `web_search`. El servicio NO está desplegado ni dispone de credenciales; hasta configurarlo, la web sigue en modo demostración.
## Configuración
1. Despliega `server/leon-api.mjs` en un servidor Node.js >=20 con HTTPS y dominio propio.
2. En el entorno privado del servidor define `OPENAI_API_KEY`, `OPENAI_MODEL` (por defecto `gpt-4.1-mini`) y `ALLOWED_ORIGIN=https://cedanoconan-coder.github.io`. El modelo elegido debe admitir `web_search`.
3. Configura autenticación real y límites de gasto antes de abrirlo al público. La restricción CORS y el límite por IP incluidos NO son autenticación y no evitan abusos de clientes externos.
4. En Chrome, en la consola de desarrollador del sitio, ejecuta `localStorage.setItem('leon_api_url','https://TU-SERVIDOR-HTTPS')` y recarga. En una versión posterior se agregará configuración segura desde la interfaz.
5. Prueba preguntas recientes y verifica fuentes; las respuestas pueden contener errores y la búsqueda puede generar costos.
Para volver al modo demo: `localStorage.removeItem('leon_api_url')` y recarga.
No hay despliegue automático, memoria persistente, acceso a datos privados ni control del teléfono en esta fase.
