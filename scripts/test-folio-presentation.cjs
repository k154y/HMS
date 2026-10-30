const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),Module=require('node:module');
const ts=require('../apps/web/node_modules/typescript');
function load(file,overrides={}){
 const filename=path.resolve(__dirname,'../apps/web/components/operations',file),mod=new Module(filename,module);
 mod.filename=filename;mod.paths=Module._nodeModulePaths(path.dirname(filename));const original=mod.require.bind(mod);
 mod.require=name=>name in overrides?overrides[name]:original(name);
 mod._compile(ts.transpileModule(fs.readFileSync(filename,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,jsx:ts.JsxEmit.ReactJSX,esModuleInterop:true}}).outputText,filename);return mod.exports;
}
const helpers=load('folio-presentation.ts');
const rows=[
 {id:'one',customer_id:'a',customer_name:'Guest',currency:'RWF',balance:100,status:'OPEN'},
 {id:'two',customer_id:'a',customer_name:'Guest',currency:'USD',balance:-20,status:'OPEN'},
 {id:'three',customer_id:'b',customer_name:'Guest',currency:'RWF',balance:50,status:'CLOSED'},
 {id:'four',customer_id:'a',customer_name:'Guest',currency:'RWF',balance:-10,status:'OPEN'}
];
test('same names do not merge different customer accounts',()=>{
 const groups=helpers.groupFolios(rows);assert.equal(groups.length,2);assert.equal(groups[0].bills.length,3);assert.equal(groups[1].bills[0].id,'three');
});
test('currency summaries never mix currencies and retain credits',()=>{
 assert.deepEqual(helpers.currencyBalances(rows),{RWF:140,USD:-20});
 assert.deepEqual(helpers.currencyBalances(rows.filter(r=>r.customer_id==='a')),{RWF:90,USD:-20});
});
const entries=[
 {id:'e1',kind:'ACCOMMODATION',amount:100,memo:'Room 101',sourceId:'room-charge'},
 {id:'e2',kind:'PAYMENT',amount:-80,memo:'Bank payment',sourceId:'receipt-42'},
 {id:'e3',kind:'REVERSAL',amount:-20,memo:'Correction',actorId:'staff-1'}
];
test('transaction search and type filters retain original signs and records',()=>{
 assert.deepEqual(helpers.filterActivity(entries,'RECEIPT-42','PAYMENT'),[entries[1]]);
 assert.deepEqual(helpers.filterActivity(entries,'','REVERSAL'),[entries[2]]);
 assert.deepEqual(helpers.filterActivity(entries,'room','PAYMENT'),[]);
 assert.equal(entries[1].amount,-80);
});
test('folio cards link to real records and identify the current folio',()=>{
 const React=require('../apps/web/node_modules/react'),{renderToStaticMarkup}=require('../apps/web/node_modules/react-dom/server');
 const {FolioCards}=load('FolioCards.tsx',{'./folio-presentation':helpers,'@/components/LocaleProvider':{useLocale:()=>({t:key=>key})},'next/link':{__esModule:true,default:({children,...props})=>React.createElement('a',props,children)}});
 const html=renderToStaticMarkup(React.createElement(FolioCards,{rows,currentId:'two'}));
 assert.match(html,/href="\/folios\/two" aria-current="page"/);assert.match(html,/-20.00/);assert.match(html,/USD/);assert.doesNotMatch(html,/undefined/);
});
test('transaction workspace preserves payment amounts, status and detailed references',()=>{
 const React=require('../apps/web/node_modules/react'),{renderToStaticMarkup}=require('../apps/web/node_modules/react-dom/server');
 const Workspace=load('FolioWorkspace.tsx',{'./folio-presentation':helpers,'@/components/LocaleProvider':{useLocale:()=>({t:key=>key})},'./CustomerFolioHistory':{CustomerFolioHistory:()=>null},'./ui':{inputStyle:'input'}}).default;
 const html=renderToStaticMarkup(React.createElement(Workspace,{folio:{id:'folio-id',customerId:'a',currency:'RWF',status:'OPEN',balance:20},customer:{id:'a',name:'Example Company',kind:'COMPANY'},entries,totals:{charges:100,payments:80}}));
 assert.match(html,/Billed to/);assert.match(html,/COMPANY/);assert.match(html,/PARTIAL/);
 assert.match(html,/-80.00/);assert.match(html,/receipt-42/);assert.match(html,/staff-1/);assert.match(html,/Related folios/);
});
