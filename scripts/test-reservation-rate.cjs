const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),Module=require('node:module');
const ts=require('../apps/web/node_modules/typescript');
function load(file,overrides={}){
 const filename=path.resolve(__dirname,'../apps/web',file),mod=new Module(filename,module);
 mod.filename=filename;mod.paths=Module._nodeModulePaths(path.dirname(filename));
 const original=mod.require.bind(mod);mod.require=name=>name in overrides?overrides[name]:original(name);
 mod._compile(ts.transpileModule(fs.readFileSync(filename,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,jsx:ts.JsxEmit.ReactJSX,esModuleInterop:true}}).outputText,filename);
 return mod.exports;
}
const pricing=load('components/operations/reservation-rate.ts');
test('optional decimal rate preserves exact input and rejects invalid amounts',()=>{
 assert.equal(pricing.effectiveReservationRate('','100000'),'100000');
 assert.equal(pricing.effectiveReservationRate('90000.1234','100000'),'90000.1234');
 for(const rate of ['','0','90000.1234','999999999999999.9999'])assert.equal(pricing.validReservationRate(rate),true);
 for(const rate of ['-1','-0.00001','NaN','Infinity','1.00001','1000000000000000'])assert.equal(pricing.validReservationRate(rate),false);
});
function nodes(tree){
 if(!tree||typeof tree!=='object')return [];
 return [tree,...[tree.props?.children].flat(Infinity).flatMap(nodes)];
}
function text(tree){if(tree==null||typeof tree==='boolean')return '';if(typeof tree==='string'||typeof tree==='number')return String(tree);return [tree?.props?.children].flat(Infinity).map(text).join(' ')}
function render(rate='',allowed=true,fail=false){
 let index=0,focus=0,opened=0,closed=0;global.document={body:{style:{overflow:'auto'}}};const effects=[],changes=[],calls=[];
 const values=['2027-01-01','2027-01-03',[{id:'r',code:'101',nightlyRate:'100000',adults:2,children:1}],
 [{id:'c',name:'Guest'}],'c',{r:{adults:1,children:0,rate}},'','',false,true,null,'2027-01-01/2027-01-03','RWF'];
 const refs=[{current:{isConnected:true,focus:()=>focus++}},{current:{focus:()=>focus++,showModal:()=>opened++,close:()=>closed++}},{current:null},{current:false}];let ref=0;
 const react={...require('../apps/web/node_modules/react'),useState:()=>{const i=index++;return [values[i],value=>changes.push([i,value])]},useRef:()=>refs[ref++],useEffect:f=>effects.push(f)};
 const Page=load('app/(dashboard)/reservations/new/page.tsx',{
 'react':react,'next/link':{default:'a'},'next-auth/react':{useSession:()=>({data:{user:{permissions:allowed?['ROOM_RATE_MANAGE']:[]}}})},
 '@/components/LocaleProvider':{useLocale:()=>({t:key=>key})},
 '@/components/operations/ui':{Panel:'section',Field:'label',inputStyle:'input',buttonStyle:'button'},
 '@/components/operations/reservation-rate':pricing,
 '@/lib/hms-api':{api:async(...args)=>{calls.push(args);if(fail)throw new Error('Room no longer available');return {reference:'RSV',folioId:'f'}},all:async()=>[]}
 }).default;
 return {tree:Page(),effects,changes,calls,get focus(){return focus},get opened(){return opened},get closed(){return closed}};
}
test('preview opens as a native modal with effective rate and restores focus',()=>{
 const h=render('90000');const list=nodes(h.tree);
 const panel=list.find(n=>n.props?.id==='reservation-preview');
 const trigger=list.find(n=>n.props?.['aria-controls']==='reservation-preview');
 assert.ok(list.indexOf(panel)<list.indexOf(trigger));
 assert.match(text(panel),/90000\s+RWF/);assert.match(text(h.tree),/Configured rate.*100000/);
 assert.equal(panel.props.tabIndex,-1);assert.equal(trigger.props['aria-expanded'],true);
 assert.equal(panel.type,'dialog');assert.equal(panel.props['aria-modal'],'true');assert.match(panel.props.className,/max-h-\[85dvh\].*overflow-y-auto/);
 const cleanup=h.effects[1]();assert.equal(h.focus,1);assert.equal(h.opened,1);assert.equal(document.body.style.overflow,'hidden');
 let prevented=false;panel.props.onCancel({preventDefault:()=>prevented=true});cleanup();assert.equal(h.closed,1);assert.equal(document.body.style.overflow,'auto');
 assert.equal(prevented,true);assert.equal(h.focus,2);assert.ok(h.changes.some(([i,v])=>i===9&&v===false));
});
test('blank rate keeps legacy payload and configured preview',async()=>{
 const h=render();assert.match(text(nodes(h.tree).find(n=>n.props?.id==='reservation-preview')),/100000\s+RWF/);
 await nodes(h.tree).find(n=>n.type==='button'&&text(n)==='Confirm reservation').props.onClick();
 assert.equal(Object.hasOwn(h.calls[0][2].rooms[0],'rate'),false);
});
test('custom rate is submitted as an exact decimal string',async()=>{
 const h=render('90000.1234');
 await nodes(h.tree).find(n=>n.type==='button'&&text(n)==='Confirm reservation').props.onClick();
 assert.equal(h.calls[0][2].rooms[0].rate,'90000.1234');
});
test('users without rate permission submit no override',async()=>{
 const h=render('90000',false);
 assert.equal(nodes(h.tree).some(n=>n.props?.label==='Custom room rate (optional)'),false);
 await nodes(h.tree).find(n=>n.type==='button'&&text(n)==='Confirm reservation').props.onClick();
 assert.equal(Object.hasOwn(h.calls[0][2].rooms[0],'rate'),false);
});



test('successful confirmation closes preview and sets confirmation message',async()=>{
 const h=render();
 const confirm=nodes(h.tree).find(n=>n.type==='button'&&text(n)==='Confirm reservation');
 await Promise.all([confirm.props.onClick(),confirm.props.onClick()]);
 assert.equal(h.calls.length,1);
 assert.ok(h.changes.some(([i,v])=>i===9&&v===false));
 assert.ok(h.changes.some(([i,v])=>i===10&&v?.reference==='RSV'));
});
test('failed save keeps preview open and displays the error',async()=>{
 const h=render('',true,true);
 await nodes(h.tree).find(n=>n.type==='button'&&text(n)==='Confirm reservation').props.onClick();
 assert.ok(h.changes.some(([i,v])=>i===7&&v==='Room no longer available'));
 assert.equal(h.changes.some(([i,v])=>i===9&&v===false),false);
 assert.equal(h.changes.some(([i,v])=>i===10&&v),false);
});
