const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const Module=require('node:module');
const ts=require('../apps/web/node_modules/typescript');
function load(file, overrides={}) {
 const filename=path.resolve(__dirname,'../apps/web',file);
 const mod=new Module(filename,module);mod.filename=filename;mod.paths=Module._nodeModulePaths(path.dirname(filename));
 const original=mod.require.bind(mod);
 mod.require=name=>name in overrides?overrides[name]:name.startsWith('@/lib/')?load(name.replace('@/','')+'.ts'):name==='./navigation'?load('lib/navigation.ts'):original(name);
 mod._compile(ts.transpileModule(fs.readFileSync(filename,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,esModuleInterop:true,jsx:ts.JsxEmit.ReactJSX}}).outputText,filename);
 return mod.exports;
}
const {safeDestination,loginDestination,authRedirect}=load('lib/navigation.ts');
test('platform entry uses fresh server privileges and never redirects owners to HMS',async t=>{
 let session=null,role='SUPER_ADMIN';
 const Page=load('app/platform/page.tsx',{'@/lib/auth':{auth:async()=>session},'@/components/WorkspaceAccessState':()=>null,'next/navigation':{redirect:url=>{throw new Error(url)}}}).default;
 await assert.rejects(Page,/admin\/login/);
 session={accessToken:'test',user:{role:'SUPER_ADMIN'}};
 t.mock.method(globalThis,'fetch',async()=>Response.json({role}));
 await assert.rejects(Page,/^Error: \/platform\/organizations$/);
 role='OWNER';assert.equal((await Page()).props.state,'denied');
});
test('workspace and nested hotel paths survive login and expiration',()=>{
 for(const destination of ['/dashboard','/commerce','/platform','/reservations/new','/folios/123-abc','/security']){
  assert.equal(safeDestination(destination),destination);
  const query=new URL(loginDestination(destination,true),'http://localhost').searchParams;
  assert.equal(query.get('next'),destination);assert.equal(query.get('reason'),'expired');
 }
});
test('untrusted destinations cannot become external URLs or authentication loops',()=>{
 for(const value of [undefined,null,[],{},'', 'https://evil.test','//evil.test','/\\evil.test','/%2f%2fevil.test','/%252f%252fevil.test','/commerce%3fnext=https://evil.test','/commerce/../platform','/commerce\n','/login','/api/auth/signout','/commerce?accessToken=secret','/commerce#x',' /commerce','/commerce//evil','javascript:alert(1)']){
  assert.equal(safeDestination(value),'/dashboard',String(value));
 }
});
test('NextAuth callback URLs are restricted to canonical internal workspaces',()=>{
 const base='https://platform.test';
 assert.equal(authRedirect('/commerce',base),base+'/commerce');
 assert.equal(authRedirect(base+'/commerce',base),base+'/commerce');
 for(const value of ['https://evil.test','//evil.test','https://platform.test.evil.test/commerce',base+'/%2f%2fevil.test',base+'/commerce?accessToken=secret','/login'])assert.equal(authRedirect(value,base),base+'/dashboard');
 assert.equal(authRedirect(base+'/',base),base);
 assert.equal(authRedirect('/',base),base);
});
test('shared credentials accept commerce-only identities without granting hotel scope',async t=>{
 let config;
 load('lib/auth.ts',{'next-auth':value=>{config=value;return {};},'next-auth/providers/credentials':value=>value});
 const replies=[{accessToken:'test-only',refreshToken:'refresh-only'},{id:'user-1',email:'user@example.test',role:null,memberships:[]}];
 t.mock.method(globalThis,'fetch',async()=>Response.json(replies.shift()));
 const user=await config.providers[0].authorize({email:'user@example.test',password:'test-only'});
 assert.equal(user.id,'user-1');assert.equal(user.hotelId,null);assert.equal(user.branchId,null);assert.equal(user.role,null);
});
test('failed login and invalid backend profiles remain rejected',async t=>{
 let config;load('lib/auth.ts',{'next-auth':value=>{config=value;return {};},'next-auth/providers/credentials':value=>value});
 t.mock.method(globalThis,'fetch',async()=>Response.json({}, {status:401}));
 assert.equal(await config.providers[0].authorize({}),null);
 t.mock.method(globalThis,'fetch',async()=>Response.json({accessToken:'test-only'}));
 assert.equal(await config.providers[0].authorize({}),null);
});
test('refresh clears removed hotel scope and keeps tokens out of the public profile',async t=>{
 let config;load('lib/auth.ts',{'next-auth':value=>{config=value;return {};},'next-auth/providers/credentials':value=>value});
 const replies=[{accessToken:'new-test-token',refreshToken:'new-refresh',accessTokenExpiresAt:'2099-01-01T00:00:00Z'},{id:'user-1',email:'user@example.test',role:null}];
 t.mock.method(globalThis,'fetch',async()=>Response.json(replies.shift()));
 const token=await config.callbacks.jwt({token:{refreshToken:'old-refresh',accessTokenExpiresAt:'2000-01-01T00:00:00Z',role:'OWNER',hotelId:'old-hotel',branchId:'old-branch'}});
 assert.equal(token.role,null);assert.equal(token.hotelId,null);assert.equal(token.branchId,null);
 const session=await config.callbacks.session({session:{user:{}},token});
 assert.equal(session.user.refreshToken,undefined);assert.equal(session.user.accessToken,undefined);assert.equal(session.accessToken,'new-test-token');
});
test('proxy redirects before rendering and overwrites spoofed destination headers',()=>{
 const {proxy,config}=load('proxy.ts',{
  '@/lib/auth':{auth:callback=>callback},
  'next/server':{NextResponse:{redirect:url=>({redirect:String(url)}),next:options=>options}},
 });
 const request={auth:null,nextUrl:{pathname:'/reservations/new'},url:'http://localhost/reservations/new',headers:new Headers({'x-workspace-path':'/commerce'})};
 assert.equal(proxy(request).redirect,'http://localhost/hms/login?next=%2Freservations%2Fnew');
 request.auth={accessToken:'test-only'};
 assert.equal(proxy(request).request.headers.get('x-workspace-path'),'/reservations/new');
 assert.ok(config.matcher.includes('/commerce/:path*'));
 assert.ok(!config.matcher.includes('/login/:path*'));
});
test('commerce entry maps backend states without leaking backend errors or creating businesses',async t=>{
 const Page=load('app/commerce/page.tsx',{
  '@/lib/auth':{auth:async()=>({accessToken:'test-only',user:{id:'test-user'}})},
  '@/components/commerce/CommercePanel':()=>null,
  '@/components/WorkspaceAccessState':()=>null,
  'next/navigation':{redirect:url=>{throw new Error(url)}},
 }).default;
 for(const [status,body,state] of [
  [404,{code:'COMMERCE_DISABLED'},'disabled'],[403,{message:'secret'},'denied'],
  [500,{message:'SQL secret'},'unavailable'],[200,[],'unassigned'],[200,{},'unavailable'],
 ]){
  t.mock.method(globalThis,'fetch',async(_url,options)=>{assert.equal(options.method,undefined);return Response.json(body,{status});});
  const result=await Page();assert.deepEqual(result.props,{state});
 }
 t.mock.method(globalThis,'fetch',async()=>Response.json({},{status:401}));
 await assert.rejects(Page(),{message:'/commerce/login?next=%2Fcommerce&reason=expired'});
 const assigned=[{id:'business-1',name:'Assigned business',status:'ACTIVE'}];
 t.mock.method(globalThis,'fetch',async()=>Response.json(assigned));
 assert.deepEqual((await Page()).props.initialBusinesses,assigned);
});

test('product destinations reject cross-product and external return URLs',()=>{
 const {applicationDestination,loginDestination,authRedirect}=load('lib/navigation.ts');
 assert.equal(applicationDestination('HMS','/commerce'),'/dashboard');
 assert.equal(applicationDestination('COMMERCE','/dashboard'),'/commerce');
 assert.equal(applicationDestination('PLATFORM_ADMIN','/pos'),'/platform/organizations');
 assert.equal(applicationDestination('COMMERCE','https://evil.test'),'/commerce');
 assert.equal(applicationDestination('COMMERCE','/commerce/login'),'/commerce');
 for(const [path,route] of [['/dashboard','/hms/login'],['/commerce','/commerce/login'],['/platform','/admin/login']]){
  assert.ok(loginDestination(path).startsWith(route+'?next='));
  assert.equal(authRedirect(route,'https://kbp.test'),'https://kbp.test'+route);
 }
});
test('fresh product checks isolate HMS, Commerce and admin while supporting multiproduct users',async t=>{
 const {productAccess}=load('lib/product-access.ts');
 let profile={role:'OWNER',hotelId:'hotel',branchId:'branch'},businesses=[];
 t.mock.method(globalThis,'fetch',async url=>Response.json(url.endsWith('/auth/me')?profile:businesses));
 assert.equal((await productAccess('token','HMS')).state,'allowed');
 assert.equal((await productAccess('token','COMMERCE')).state,'denied');
 assert.equal((await productAccess('token','PLATFORM_ADMIN')).state,'denied');
 businesses=[{id:'shop'}];assert.equal((await productAccess('token','COMMERCE')).state,'allowed');
 assert.equal((await productAccess('token','HMS')).state,'allowed');
 profile={role:null};assert.equal((await productAccess('token','HMS')).state,'denied');
 assert.equal((await productAccess('token','COMMERCE')).state,'allowed');
 profile={role:'SUPER_ADMIN'};assert.equal((await productAccess('token','PLATFORM_ADMIN')).state,'allowed');
 assert.equal((await productAccess('token','HMS')).state,'denied');
});
test('temporary passwords reach security without bypassing the operational gate',async t=>{
 const {productAccess}=load('lib/product-access.ts');let calls=0;
 t.mock.method(globalThis,'fetch',async()=>{calls++;return Response.json({mustChangePassword:true,commerceBusinesses:[{id:'shop'}]})});
 const result=await productAccess('token','COMMERCE');assert.equal(result.state,'allowed');assert.equal(result.profile.mustChangePassword,true);assert.equal(calls,1);
});
test('failed backend access checks fail closed',async t=>{
 const {productAccess}=load('lib/product-access.ts');
 for(const [status,state] of [[401,'expired'],[403,'denied'],[500,'unavailable']]){
  t.mock.method(globalThis,'fetch',async()=>Response.json({},{status}));assert.equal((await productAccess('token','HMS')).state,state);
 }
});
test('three public login entries reuse the shared server entry and legacy login returns to landing',async()=>{
 const Entry=()=>null;
 for(const [path,application] of [['hms','HMS'],['commerce','COMMERCE'],['admin','PLATFORM_ADMIN']]){
  const Page=load(`app/${path}/login/page.tsx`,{'@/components/ProductLoginEntry':Entry}).default;
  const result=await Page({searchParams:Promise.resolve({next:'/commerce'})});assert.equal(result.type,Entry);assert.equal(result.props.application,application);
 }
 const Legacy=load('app/(auth)/login/page.tsx',{'next/navigation':{redirect:url=>{throw new Error(url)}}}).default;
 assert.throws(Legacy,{message:'/'});
});
test('commerce login is public and does not loop through the protected proxy',()=>{
 const {proxy}=load('proxy.ts',{'@/lib/auth':{auth:cb=>cb},'next/server':{NextResponse:{next:()=>({allowed:true}),redirect:()=>{throw Error('Unexpected redirect')}}}});
 assert.deepEqual(proxy({auth:null,nextUrl:{pathname:'/commerce/login'}}),{allowed:true});
});
test('landing links only to the two product login routes',()=>{
 const source=fs.readFileSync(path.resolve(__dirname,'../apps/web/components/PlatformLanding.tsx'),'utf8');
 assert.match(source,/href="\/hms\/login"/);assert.match(source,/href="\/commerce\/login"/);
 assert.doesNotMatch(source,/href="\/admin|href="\/login"|Choose workspace/);
});

test('existing sessions stay in the requested product and unauthorized users get an access message',async()=>{
 let state='allowed',mustChangePassword=false;
 const Shared=()=>null,Denied=()=>null;
 const Entry=load('components/ProductLoginEntry.tsx',{
  '@/lib/auth':{auth:async()=>({accessToken:'token',user:{}})},
  '@/lib/product-access':{productAccess:async()=>({state,profile:{mustChangePassword}})},
  './SharedLogin':Shared,'./WorkspaceAccessState':Denied,
  'next/navigation':{redirect:url=>{throw new Error(url)}},
 }).default;
 for(const [application,destination] of [['HMS','/dashboard'],['COMMERCE','/commerce'],['PLATFORM_ADMIN','/platform/organizations']])await assert.rejects(Entry({application}),{message:destination});
 state='denied';assert.equal((await Entry({application:'COMMERCE'})).type,Denied);
 state='expired';assert.equal((await Entry({application:'COMMERCE'})).type,Shared);
 state='allowed';mustChangePassword=true;await assert.rejects(Entry({application:'COMMERCE'}),{message:'/security?next=%2Fcommerce'});
});
test('recovery pages carry only validated application context',async()=>{
 for(const mode of ['forgot','reset']){
  const Form=()=>null;
  const Page=load(`app/(auth)/${mode}-password/page.tsx`,{'@/components/operations/PasswordForm':{PasswordForm:Form}}).default;
  for(const application of ['HMS','COMMERCE','PLATFORM_ADMIN']){
   const tree=await Page({searchParams:Promise.resolve({app:application})});assert.equal(tree.props.children.props.children.props.application,application);
  }
 }
});
