const fs=require('fs');const path=require('path');const ts=require('../apps/web/node_modules/typescript');const vm=require('vm');
const file=path.join(__dirname,'../apps/web/lib/i18n.ts');const compiled=ts.transpileModule(fs.readFileSync(file,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText;
const context={exports:{}};vm.runInNewContext(compiled,context);const {messages,locales}=context.exports;const keys=new Set();const untranslated=[];
function strings(node){if(ts.isStringLiteral(node))keys.add(node.text);else if(ts.isConditionalExpression(node)){strings(node.whenTrue);strings(node.whenFalse)}}
function visit(node){if(ts.isCallExpression(node)&&ts.isIdentifier(node.expression)&&node.expression.text==='t'&&node.arguments.length)strings(node.arguments[0]);if(ts.isJsxAttribute(node)&&['label','title'].includes(node.name.text)&&node.initializer&&ts.isStringLiteral(node.initializer))keys.add(node.initializer.text);if(ts.isPropertyAssignment(node)&&['label','title'].includes(node.name.text)&&ts.isStringLiteral(node.initializer))keys.add(node.initializer.text);if(ts.isJsxText(node)){const s=node.text.trim();if(/[A-Za-z]{3}/.test(s)&&!s.startsWith('HotelPro')&&!['English','Français','Kinyarwanda'].includes(s))untranslated.push(s)}ts.forEachChild(node,visit)}
function walk(dir){for(const f of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,f.name);if(f.isDirectory()){if(!['generated','api'].includes(f.name))walk(p)}else if(/\.tsx$/.test(p))visit(ts.createSourceFile(p,fs.readFileSync(p,'utf8'),ts.ScriptTarget.Latest,true,ts.ScriptKind.TSX))}}
walk(path.join(__dirname,'../apps/web/app'));walk(path.join(__dirname,'../apps/web/components'));
const missing=[...keys].filter(k=>locales.some(l=>!messages[l][k]));
for(const l of locales)for(const k of Object.keys(messages.en))if(!messages[l][k])missing.push(`${l}:${k}`);
if(missing.length||untranslated.length){console.error({missing:[...new Set(missing)],untranslated:[...new Set(untranslated)]});process.exitCode=1}else console.log(`PASS: ${Object.keys(messages.en).length} keys in all three languages; ${keys.size} literal UI keys checked.`);

