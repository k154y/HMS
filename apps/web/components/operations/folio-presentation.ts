export type FolioListItem={id:string;customer_id:string;customer_name:string;currency:string;status:string;balance:number;created_at?:string};
export type FolioActivity={id:string;kind:string;amount:number;memo:string;sourceId?:string;actorId?:string;createdAt?:string};
export function folioMoney(value:number){return Number(value??0).toLocaleString(undefined,{minimumFractionDigits:2,maximumFractionDigits:4})}
export function folioInitials(name:string){return name.trim().split(/\s+/).slice(0,2).map(s=>s[0]??"").join("").toUpperCase()||"?"}
export function groupFolios(rows:FolioListItem[]){
 return Object.values(rows.reduce<Record<string,{id:string;name:string;bills:FolioListItem[]}>>((groups,row)=>{
  groups[row.customer_id]??={id:row.customer_id,name:row.customer_name,bills:[]};
  groups[row.customer_id].bills.push(row);return groups;
 },{}));
}
export function currencyBalances(rows:FolioListItem[]){
 return rows.reduce<Record<string,number>>((totals,row)=>{totals[row.currency]=(totals[row.currency]??0)+Number(row.balance);return totals},{});
}
export function filterActivity(entries:FolioActivity[],query:string,kind:string){
 const search=query.trim().toLowerCase();
 return entries.filter(e=>(kind==="ALL"||e.kind===kind)&&[e.memo,e.kind,e.id,e.sourceId??""].some(v=>v.toLowerCase().includes(search)));
}
