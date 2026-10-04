// Isolated browser acceptance fixture: renders the real calendar/drawer with fake API/session data.
// Run after npm ci in apps/web. Nothing contacts the backend or uses hotel credentials.
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const root=path.resolve(__dirname,'..'),web=path.join(root,'apps/web');
const scratch=path.join(root,'tmp/reservation-calendar-fixture');fs.mkdirSync(scratch,{recursive:true});
const write=(name,text)=>{const file=path.join(scratch,name);fs.writeFileSync(file,text);return file;};
const aliases={
  '@/components/LocaleProvider':write('locale.ts','export const useLocale=()=>({t:(v:string)=>v});'),
  'next-auth/react':write('session.ts','export const useSession=()=>({data:{user:{permissions:[]}}});'),
  'next/link':write('link.tsx','import React from "react";export default function Link(props:any){return <a {...props}/>;}'),
  '@/lib/hms-api':write('api.ts',`
const day=(offset:number)=>new Date(Date.now()+offset*86400000).toISOString().slice(0,10);
const reservation={id:'fixture-reservation',reference:'RSV-CALENDAR',customerId:'fixture-customer',folioId:'fixture-folio',checkIn:day(0),checkOut:day(3),adults:2,children:1,status:'CONFIRMED',roomIds:['fixture-room']};
export async function all<T>(path:string):Promise<T[]> {
  return (path==='rooms'?[{id:'fixture-room',code:'101',active:true,operational:'AVAILABLE',bedDimensions:'Double'},{id:'unavailable-room',code:'102',active:true,operational:'MAINTENANCE',bedDimensions:'Double'}]:path==='reservations'?[reservation]:path==='customers'?[{id:'fixture-customer',name:'Test Guest'}]:[]) as T[];
}
export async function api<T>(path:string):Promise<T> {
  document.getElementById('requests')!.textContent+='GET '+path+'\\n';
  await new Promise(resolve=>setTimeout(resolve,400));
  if(path==='reservations/fixture-reservation') {
    if((document.getElementById('simulate-error') as HTMLInputElement).checked)throw new Error('The requested record could not be found or is no longer available.');
    return {...reservation,reference:'RSV-FRESH-DETAIL',notes:'Fresh reservation response from the detail endpoint.'} as T;
  }
  return {reservationId:reservation.id,folioId:reservation.folioId,currency:'RWF',reservationTotal:120000,advancePaid:20000,remaining:100000,paymentStatus:'PARTIALLY_PREPAID'} as T;
}
`),
  '@':web,
};
const entry=write('entry.tsx',`import React from 'react';import {createRoot} from 'react-dom/client';import Calendar from ${JSON.stringify(path.join(web,'app/(dashboard)/reservations/page.tsx'))};createRoot(document.getElementById('root')!).render(<Calendar/>);`);
const loader=write('loader.cjs',`const ts=require(${JSON.stringify(path.join(web,'node_modules/typescript'))});module.exports=function(source){return ts.transpileModule(source,{compilerOptions:{jsx:ts.JsxEmit.ReactJSX,module:ts.ModuleKind.ESNext,target:ts.ScriptTarget.ES2020}}).outputText;};`);
const bundled=require(path.join(web,'node_modules/next/dist/compiled/webpack/webpack'));bundled.init();
bundled.webpack({mode:'development',entry,devtool:false,output:{path:scratch,filename:'bundle.js'},
  resolve:{extensions:['.tsx','.ts','.js'],modules:[path.join(web,'node_modules'),'node_modules'],alias:aliases},
  module:{rules:[{test:/\.[jt]sx?$/,exclude:/node_modules/,use:loader}]}},(error,stats)=>{
  if(error||stats.hasErrors()){console.error(error||stats.toString({all:false,errors:true}));process.exitCode=1;return;}
  const styles=[];
  function findCss(directory){if(!fs.existsSync(directory))return;for(const entry of fs.readdirSync(directory,{withFileTypes:true})){const file=path.join(directory,entry.name);if(entry.isDirectory())findCss(file);else if(file.endsWith('.css'))styles.push(fs.readFileSync(file,'utf8'));}}
  findCss(path.join(web,'tmp/production-improvements-build/static'));
  const html=`<!doctype html><html><head><meta charset="utf-8"><title>Isolated reservation calendar acceptance</title><style>${styles.join('\n')}</style></head><body style="padding:24px"><aside><strong>Isolated fixture - no hotel data</strong> <label><input id="simulate-error" type="checkbox"> Simulate detail failure</label><details><summary>API request log</summary><pre id="requests"></pre></details></aside><main id="root"></main><script src="/bundle.js"></script></body></html>`;
  const server=http.createServer((request,response)=>{if(request.url==='/bundle.js'){response.setHeader('Content-Type','text/javascript');response.end(fs.readFileSync(path.join(scratch,'bundle.js')));}else if(request.url==='/'){response.setHeader('Content-Type','text/html');response.end(html);}else{response.statusCode=404;response.end();}}).listen(0,'127.0.0.1',()=>console.log('Isolated calendar fixture: http://127.0.0.1:'+server.address().port));
});
