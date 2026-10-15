const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const Module=require('node:module');
const ts=require('../apps/web/node_modules/typescript');
function load(file,overrides={}){
 const filename=path.resolve(__dirname,'../apps/web/components/commerce',file);
 const mod=new Module(filename,module);mod.filename=filename;mod.paths=Module._nodeModulePaths(path.dirname(filename));
 const original=mod.require.bind(mod);mod.require=name=>name in overrides?overrides[name]:original(name);
 mod._compile(ts.transpileModule(fs.readFileSync(filename,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,jsx:ts.JsxEmit.ReactJSX,esModuleInterop:true}}).outputText,filename);return mod.exports;
}
const {catalogPrice,changeItem,quantityIssue}=load('trading-pricing.ts');
const product={id:'p',name:'Fanta',sku:'F',base_unit:'bottle',retail_price:1200,purchase_cost:400,fractional_units:false};
const bottle={unit_name:'bottle',units_per_package:'1'},crate={unit_name:'crate',units_per_package:'24',retail_package_price:'28800',wholesale_package_price:'12000',purchase_package_price:'10000'};
test('base and package prices use the chosen policy, independently from buying prices',()=>{
 assert.equal(catalogPrice(product,bottle,'SALE'),1200);
 assert.equal(catalogPrice(product,crate,'SALE'),12000);
 assert.equal(catalogPrice(product,crate,'PURCHASE'),10000);
 assert.equal(catalogPrice(product,{...crate,wholesale_package_price:null},'SALE'),null);
 assert.equal(catalogPrice({...product,retail_price:undefined},bottle,'SALE'),null);
});
test('unit change preserves quantity but discards the previous units price override',()=>{
 const item={productId:'p',unit:'bottle',quantity:'0.5',unitPrice:'1500',discount:'',discountMode:'AMOUNT',reason:''};
 const next=changeItem(item,'unit','crate');assert.equal(next.quantity,'0.5');assert.equal(next.unitPrice,'');assert.equal(item.unit,'bottle');
 assert.equal(quantityIssue(product,crate,next.quantity,24),'');
 assert.match(quantityIssue(product,bottle,'0.5',24),/whole stock/);
 assert.match(quantityIssue(product,crate,'2',24),/Not enough stock/);
});
function visit(node,predicate){
 if(!node||typeof node!=='object')return null;if(predicate(node))return node;
 for(const child of [node.props?.children].flat(Infinity)){const match=visit(child,predicate);if(match)return match;}return null;
}
// Dependency-free lifecycle tests exercise the component handlers. Native browser
// top-layer/inert behavior still needs a real-browser acceptance check.
function modalHarness(){
 const savedDocument=global.document;let focused=null,opened=0,closed=0,removed=0,closeRequests=0,cursor=0;
 const hooks=[],effects=[];
 const trigger={focus:()=>focused=trigger};
 const first={getClientRects:()=>[{}],focus:()=>focused=first},last={getClientRects:()=>[{}],focus:()=>focused=last};
 const native={showModal:()=>opened++,close:()=>closed++,focus:()=>focused=native,querySelectorAll:()=>[first,last]};
 const body={style:{overflow:'auto'},appendChild:node=>node.parentElement=body};
 const doc={body,documentElement:{style:{overflow:'scroll'}},createElement:()=>({dataset:{},remove:()=>removed++})};
 Object.defineProperty(doc,'activeElement',{get:()=>focused});global.document=doc;
 const react={
  useState(initial){const i=cursor++;hooks[i]??={value:initial};return [hooks[i].value,v=>hooks[i].value=v]},
  useRef(initial){const i=cursor++;hooks[i]??={current:initial};return hooks[i]},
  useId(){cursor++;return 'review-heading'},
  useEffect(fn,deps){const i=cursor++,old=hooks[i];if(!old||deps.some((d,j)=>d!==old.deps[j])){effects.push(()=>{old?.cleanup?.();hooks[i]={deps,cleanup:fn()}})}}
 };
 const Component=load('CommerceReviewDialog.tsx',{react,'react-dom':{createPortal:(child,host)=>({child,host})}}).default;
 const returnFocus={current:trigger};
 function render(busy=false){cursor=0;const tree=Component({title:'Review purchase invoice',busy,returnFocus,onClose:()=>closeRequests++,children:'Cart'});if(tree)tree.child.props.ref.current=native;while(effects.length)effects.shift()();return tree;}
 render();let tree=render();
 return {tree,render,doc,body,native,trigger,first,last,get focused(){return focused},get opened(){return opened},get closeRequests(){return closeRequests},cleanup(){hooks.forEach(h=>h?.cleanup?.());global.document=savedDocument;return {closed,removed}}};
}
test('review is portaled into body and opens in native top layer above sticky headers',()=>{
 const h=modalHarness();try{
  assert.equal(h.tree.host.parentElement,h.body);assert.equal(h.opened,1);
  assert.equal(h.tree.child.type,'dialog');assert.equal(h.tree.child.props.role,'dialog');assert.equal(h.tree.child.props['aria-modal'],'true');
  assert.equal(h.doc.body.style.overflow,'hidden');assert.equal(h.doc.documentElement.style.overflow,'hidden');
  const css=visit(h.tree.child,n=>n.type==='style').props.children;assert.match(css,/::backdrop.*position:fixed;inset:0/);assert.match(css,/2147483647/);
 }finally{h.cleanup()}
});
test('Tab wraps both ways, Escape is blocked while posting and closing restores review focus',()=>{
 const h=modalHarness();try{
  let prevented=0;const event=shiftKey=>({key:'Tab',shiftKey,currentTarget:h.native,preventDefault:()=>prevented++});
  h.last.focus();h.tree.child.props.onKeyDown(event(false));assert.equal(h.focused,h.first);
  h.first.focus();h.tree.child.props.onKeyDown(event(true));assert.equal(h.focused,h.last);assert.equal(prevented,2);
  const busy=h.render(true);busy.child.props.onCancel({preventDefault(){}});assert.equal(h.closeRequests,0);
  const idle=h.render(false);idle.child.props.onCancel({preventDefault(){}});assert.equal(h.closeRequests,1);
  visit(idle.child,n=>n.props?.['aria-label']==='Close review').props.onClick();assert.equal(h.closeRequests,2);
 }finally{h.cleanup()}
 assert.equal(h.focused,h.trigger);assert.equal(h.body.style.overflow,'auto');
});
test('integration keeps review separate from cart edits and posting, and exposes units in compact rows',()=>{
 const source=fs.readFileSync(path.resolve(__dirname,'../apps/web/components/commerce/CommerceTrading.tsx'),'utf8');
 assert.match(source,/onClose=\{\(\)=>setPreview\(null\)\}/);
 assert.match(source,/useEffect\(\(\)=>\{setPreview\(null\)\},\[items,/);
 const table=source.slice(source.indexOf('<tbody>'),source.indexOf('</tbody>'));
 assert.match(table,/Selling unit/);assert.match(table,/unitsFor\(item.productId\)\.map/);
 assert.doesNotMatch(source,/Selling price type|setWholesale/);
 assert.match(source,/if\(pending\|\|busy\|\|posting.current\)return/);
 assert.match(source,/execute\(pending\)/);
});
test('sales and purchase invoices exclude the removed disclaimer',()=>{
 const Invoice=load('CommerceInvoice.tsx').default;
 for(const kind of ['SALE','PURCHASE']){const document=Invoice({detail:{kind,lines:[]}});assert.equal(visit(document,n=>n.type==='footer'),null);}
});
