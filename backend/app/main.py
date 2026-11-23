import os
import httpx
from fastapi import FastAPI, HTTPException, Header
from fastapi.responses import HTMLResponse
from pydantic import BaseModel, Field

app = FastAPI(title="León AI Backend", docs_url=None, redoc_url=None)

@app.get("/", response_class=HTMLResponse)
def home():
    return "<!doctype html><html lang=\"es\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>León AI</title><style>body{font:16px system-ui;background:#18142b;color:white;max-width:700px;margin:auto;padding:16px}input,button{font:inherit;padding:12px;border-radius:10px}input{background:#292340;color:white;border:1px solid #7566a4}button{background:#8565dc;color:white;border:0}#messages{min-height:40vh;white-space:pre-wrap}p{padding:10px;background:#292340;border-radius:10px}form{display:flex;gap:6px}form input{flex:1;min-width:0}</style><h2>🦁 León AI</h2><p>Prueba de conexión. Introduce tu token LEON_APP_TOKEN; no se guarda en el navegador.</p><input id=\"token\" type=\"password\" placeholder=\"Token de acceso\" aria-label=\"Token de acceso\"><div id=\"messages\"></div><form id=\"form\"><input id=\"msg\" placeholder=\"Escribe a León…\" required maxlength=\"4000\"><button id=\"send\">Enviar</button></form><script>const m=document.getElementById('messages'),f=document.getElementById('form'),s=document.getElementById('send');function add(x){let p=document.createElement('p');p.textContent=x;m.append(p)}f.onsubmit=async e=>{e.preventDefault();let q=document.getElementById('msg').value,t=document.getElementById('token').value;if(!t){add('Introduce el token de acceso.');return}add('Tú: '+q);s.disabled=true;try{let r=await fetch('/chat',{method:'POST',headers:{'Content-Type':'application/json','Authorization':'Bearer '+t},body:JSON.stringify({message:q})});let raw=await r.text(),d;try{d=JSON.parse(raw)}catch(_){add('El servidor respondió con estado '+r.status+' pero no devolvió JSON.');return}add(r.ok?'León: '+(d.reply||'Sin texto de respuesta'):'Error '+r.status+': '+(d.detail||'Sin respuesta'))}catch(e){add('Fallo de red al enviar el mensaje. Comprueba la conexión e inténtalo de nuevo.')}finally{s.disabled=false}};</script>"

class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=4000)

@app.get('/health')
def health():
    return {'status': 'ok', 'service': 'leon-ai'}

@app.post('/chat')
async def chat(request: ChatRequest, authorization: str | None = Header(default=None)):
    token = os.getenv('LEON_APP_TOKEN')
    if not token or authorization != f'Bearer {token}':
        raise HTTPException(status_code=401, detail='Unauthorized')
    api_key = os.getenv('OPENAI_API_KEY')
    if not api_key:
        raise HTTPException(status_code=503, detail='AI provider not configured')
    async with httpx.AsyncClient(timeout=45) as client:
        try:
            response = await client.post('https://api.openai.com/v1/responses', headers={'Authorization': f'Bearer {api_key}'}, json={'model': os.getenv('OPENAI_MODEL', 'gpt-4.1-mini'), 'instructions': 'Eres León AI, un asistente útil en español. No afirmes controlar el dispositivo ni navegar por internet si no tienes herramientas conectadas.', 'input': request.message})
            response.raise_for_status()
            data = response.json()
        except httpx.TimeoutException:
            raise HTTPException(status_code=504, detail='OpenAI tardó demasiado en responder')
        except httpx.HTTPStatusError as exc:
            status = exc.response.status_code
            raise HTTPException(status_code=502, detail=f'OpenAI rechazó la solicitud (estado {status}). Comprueba la clave API, el crédito y el modelo.')
        except httpx.HTTPError:
            raise HTTPException(status_code=502, detail='No se pudo conectar con OpenAI')
    parts = [part.get('text', '') for item in data.get('output', []) if item.get('type') == 'message' for part in item.get('content', []) if part.get('type') == 'output_text']
    return {'reply': '\n'.join(parts)}
