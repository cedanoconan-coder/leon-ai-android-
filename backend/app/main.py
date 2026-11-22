import os
import httpx
from fastapi import FastAPI, HTTPException, Header
from pydantic import BaseModel, Field

app = FastAPI(title="León AI Backend", docs_url=None, redoc_url=None)

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
        except httpx.HTTPError:
            raise HTTPException(status_code=502, detail='AI provider unavailable')
    parts = [part.get('text', '') for item in data.get('output', []) if item.get('type') == 'message' for part in item.get('content', []) if part.get('type') == 'output_text']
    return {'reply': '\n'.join(parts)}
