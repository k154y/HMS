const fs=require('fs');const ts=require('../apps/web/node_modules/typescript');const vm=require('vm');const assert=require('node:assert/strict');
const source=fs.readFileSync('apps/web/lib/report-export.ts','utf8');const compiled=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;const reportExports={};vm.runInNewContext(compiled,{exports:reportExports,Blob:global.Blob});
assert.equal(reportExports.csvCell('=HYPERLINK("https://example.test")'),'"\'=HYPERLINK(""https://example.test"")"');
assert.equal(reportExports.csvCell('  +SUM(1,2)'),'"\'  +SUM(1,2)"');
assert.equal(reportExports.csvCell(-25),'"-25"');
assert.equal(reportExports.csvCell('Client, "A"'),'"Client, ""A"""');
const csv=reportExports.reportCsv([{title:'Customer balances',rows:[{customer:'École, Kigali',balance:25}]}],x=>x);
assert.ok(csv.startsWith('\uFEFF'));assert.ok(csv.includes('"École, Kigali","25"'));assert.ok(csv.includes('\r\n'));
console.log('PASS: CSV escaping, spreadsheet formula protection, Unicode and numeric values');

