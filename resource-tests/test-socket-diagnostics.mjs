import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve(path.dirname(new URL(import.meta.url).pathname),'..');
const socket=fs.readFileSync(path.join(root,'src/br/davi/narutoair/compat/BrowserSocket.as'),'utf8');
const resources=fs.readFileSync(path.join(root,'src/br/davi/narutoair/compat/BrowserResources.as'),'utf8');
const main=fs.readFileSync(path.join(root,'src/NarutoAir.as'),'utf8');
for(const token of [
  'socketWrites=', 'socketBytesEnviados=', 'socketFlushes=', 'socketBytesTransportados=',
  'socketBytesRecebidos=', 'socketReads=', 'ultimoWrite=', 'ultimoRead=', 'socketEstado=',
  'TX FIRST', 'OUTPUT PROGRESS', 'READ FIRST', 'FNV32-'
]) if(!socket.includes(token)) throw new Error('socket diagnostic missing '+token);
for(const token of ['pendingResource=', 'pendingMs=', 'pendentes=', 'RESOURCE '])
  if(!resources.includes(token)) throw new Error('resource pending diagnostic missing '+token);
if(main.includes('new Timer(15000)') || main.includes('bridge.report(param1)'))
  throw new Error('release build must not run continuous diagnostic traffic');
if(socket.includes('payload="') || socket.includes('conteudo="'))
  throw new Error('diagnostics must not serialize raw socket payloads');
console.log('PASS: socket handshake state remains available internally without raw payloads; release build disables recurring diagnostics.');
