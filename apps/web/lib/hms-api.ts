export type Page<T> = { content:T[]; totalElements:number; totalPages:number };
export async function api<T>(path:string,method="GET",body?:unknown):Promise<T>{
 const response=await fetch(`/api/${path}`,{method,headers:{"Content-Type":"application/json"},body:body===undefined?undefined:JSON.stringify(body),cache:"no-store"});
 if(response.status===401){window.location.replace("/login?reason=expired");throw new Error("Session expired. Please sign in again.");}
 if(response.status===204)return undefined as T;
 const data=await response.json();
 if(!response.ok)throw new Error(data.message??data.error??`Request failed (${response.status})`);
 return data;
}
export async function all<T>(path:string):Promise<T[]>{
 const data=await api<T[]|Page<T>>(path+(path.includes("?")?"&":"?")+"size=100");
 if(Array.isArray(data))return data;
 const result=[...data.content];
 for(let page=1;page<data.totalPages;page++){const next=await api<Page<T>>(path+(path.includes("?")?"&":"?")+`size=100&page=${page}`);result.push(...next.content);}
 return result;
}
export const number=(value:unknown)=>Number(value??0);
