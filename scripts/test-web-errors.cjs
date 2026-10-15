const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const Module=require('node:module');
const ts=require('../apps/web/node_modules/typescript');
const filename=path.resolve(__dirname,'../apps/web/lib/hms-api.ts');
const compiled=ts.transpileModule(fs.readFileSync(filename,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
const loaded=new Module(filename,module);loaded.filename=filename;loaded.paths=module.paths;const originalRequire=loaded.require.bind(loaded);loaded.require=name=>{if(name!=="./navigation")return originalRequire(name);const navFile=path.resolve(__dirname,"../apps/web/lib/navigation.ts");const nav=new Module(navFile,module);nav._compile(ts.transpileModule(fs.readFileSync(navFile,"utf8"),{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,navFile);return nav.exports;};loaded._compile(compiled,filename);
const {api,HmsApiError}=loaded.exports;

test('validation preserves code, fields and reference in existing page error messages',async t=>{
  t.mock.method(globalThis,'fetch',async()=>Response.json({code:'VALIDATION_FAILED',message:'Please correct the highlighted fields.',requestId:'field-ref',fieldErrors:[{field:'checkIn',message:'Check-in date is required.'}]},{status:400}));
  await assert.rejects(api('reservations','POST',{}),error=>error instanceof HmsApiError&&error.code==='VALIDATION_FAILED'&&error.requestId==='field-ref'&&error.fieldErrors[0].field==='checkIn'&&error.message.includes('Check-in date is required.'));
});
test('technical failures suppress upstream internals and retain the support reference',async t=>{
  t.mock.method(globalThis,'fetch',async()=>Response.json({code:'INTERNAL_ERROR',message:'SQL private table password=secret',fieldErrors:[{field:'sql',message:'secret'}],requestId:'support-ref'},{status:500}));
  await assert.rejects(api('reservations'),error=>error.status===500&&error.code==='INTERNAL_ERROR'&&error.message.includes('system problem')&&error.message.includes('support-ref')&&!error.message.includes('secret')&&error.fieldErrors.length===0);
});
test('business conflicts retain their actionable message',async t=>{
  t.mock.method(globalThis,'fetch',async()=>Response.json({code:'ROOM_UNAVAILABLE',message:'This room is no longer available for the selected dates.',requestId:'conflict-ref'},{status:409}));
  await assert.rejects(api('reservations'),error=>error.code==='ROOM_UNAVAILABLE'&&error.message.includes('no longer available')&&error.requestId==='conflict-ref');
});
test('401 follows the existing sign-in redirect while preserving metadata',async t=>{
  let destination;
  const old=globalThis.window;globalThis.window={location:{pathname:"/reservations/new",replace:value=>{destination=value;}}};
  t.after(()=>{globalThis.window=old;});
  t.mock.method(globalThis,'fetch',async()=>Response.json({code:'UNAUTHORIZED',requestId:'auth-ref'},{status:401}));
  await assert.rejects(api('reservations'),error=>error.status===401&&error.requestId==='auth-ref');
  assert.equal(destination,'/hms/login?next=%2Freservations%2Fnew&reason=expired');
});
test('request correlation and successful payloads remain intact',async t=>{
  let options;
  t.mock.method(globalThis,'fetch',async(_url,value)=>{options=value;return Response.json({id:'reservation-1'});});
  assert.deepEqual(await api('reservations','POST',{adults:1}),{id:'reservation-1'});
  assert.match(options.headers['X-Request-ID'],/^[0-9a-f-]{36}$/);
  assert.equal(options.body,'{"adults":1}');
});
