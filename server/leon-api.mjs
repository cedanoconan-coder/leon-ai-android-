// León AI: private API server. Node.js >=20; no dependencies.
import http from 'node:http';
const PORT=Number(process.env.PORT||3000);
const KEY=process.env.OPENAI_API_KEY;
const MODEL=process.env.OPENAI_MODEL||'gpt-4.1-mini';
const ORIGIN=process.env.ALLOWED_ORIGIN||'https://cedanoconan-coder.github.io';
const hits=new Map();
function send(res,status,data){res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Access-Control-Allow-Origin':ORIGIN,'Vary':'Origin','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(JSON.stringify(data))}
http.createServer(async(req,res)=>{
 if(req.method==='OPTIONS'){res.writeHead(204,{'Access-Control-Allow-Origin':ORIGIN,'Access-Control-Allow-Methods':'POST, OPTIONS','Access-Control-Allow-Headers':'Content-Type','Vary':'Origin'});return res.end()}
 if(req.url!=='/api/chat'||req.method!=='POST')return send(res,404,{error:'Ruta no encontrada'});
 if(req.headers.origin!==ORIGIN)return send(res,403,{error:'Origen no autorizado'});
 if(!KEY)return send(res,503,{error:'Servidor sin OPENAI_API_KEY'});
 const ip=req.socket.remoteAddress||'unknown',now=Date.now(),record=hits.get(ip)||{count:0,reset:now+60000};
 if(now>record.reset){record.count=0;record.reset=now+60000}
 if(++record.count>12)return send(res,429,{error:'Demasiadas solicitudes. Intenta en un minuto.'});
 hits.set(ip,record);if(hits.size>10000)hits.clear();
 let raw='';try{for await(const chunk of req){raw+=chunk;if(raw.length>18000)throw Error('Solicitud demasiado larga')}
 const data=JSON.parse(raw),message=data.message;
 if(typeof message!=='string'||!message.trim()||message.length>2000)return send(res,400,{error:'Pregunta inválida'});
 const history=Array.isArray(data.history)?data.history.slice(-8).filter(x=>x&&['user','assistant'].includes(x.role)&&typeof x.content==='string'&&x.content.length<=2000).map(x=>({role:x.role,content:x.content})):[];
 const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),45000);
 let upstream;try{upstream=await fetch('https://api.openai.com/v1/responses',{method:'POST',headers:{Authorization:'Bearer '+KEY,'Content-Type':'application/json'},body:JSON.stringify({model:MODEL,instructions:'Eres León AI, asistente en español dominicano. Sé claro, útil y prudente. Para información reciente usa búsqueda web cuando sea necesaria. No inventes fuentes ni afirmes controlar Android.',input:[...history,{role:'user',content:message}],tools:[{type:'web_search'}],max_output_tokens:700}),signal:controller.signal})}finally{clearTimeout(timeout)}
 if(!upstream.ok)return send(res,502,{error:'El proveedor de IA no respondió correctamente'});
 const result=await upstream.json();
 const outputs=(result.output||[]).filter(x=>x.type==='message').flatMap(x=>x.content||[]);
 const answer=outputs.filter(x=>x.type==='output_text').map(x=>x.text).join('\n')||'No pude elaborar una respuesta.';
 const sources=outputs.flatMap(x=>x.annotations||[]).filter(x=>x.type==='url_citation'&&typeof x.url==='string').map(x=>({title:x.title||'',url:x.url}));
 return send(res,200,{answer,sources:[...new Map(sources.map(x=>[x.url,x])).values()].slice(0,8)});
 }catch(e){return send(res,400,{error:e.name==='AbortError'?'La consulta tardó demasiado':'No pude procesar la consulta'})}
}).listen(PORT,()=>console.log('León API escuchando en puerto '+PORT));
