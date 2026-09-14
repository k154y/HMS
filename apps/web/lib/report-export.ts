export type ReportRow=Record<string,unknown>;
export type ReportSection={title:string;rows:ReportRow[]};
// Quoting alone does not stop spreadsheet formula execution in user-entered names.
export function csvCell(value:unknown):string {
 const raw=value==null?"":String(value);
 const safe=typeof value==="number"?raw:(/^[\s]*[=+@-]/.test(raw)||/^[\t\r\n]/.test(raw)?"'"+raw:raw);
 return '"'+safe.replaceAll('"','""')+'"';
}
export function reportCsv(sections:ReportSection[],label:(key:string)=>string):string {
 return '\uFEFF'+sections.map(section=>{const columns=Array.from(new Set(section.rows.flatMap(row=>Object.keys(row))));return [csvCell(section.title),columns.map(k=>csvCell(label(k))).join(','),...section.rows.map(row=>columns.map(k=>csvCell(row[k])).join(','))].join('\r\n')}).join('\r\n\r\n');
}
export function downloadReport(sections:ReportSection[],filename:string,label:(key:string)=>string){
 const url=URL.createObjectURL(new Blob([reportCsv(sections,label)],{type:'text/csv;charset=utf-8;'}));
 const link=document.createElement('a');link.href=url;link.download=filename;document.body.appendChild(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);
}
