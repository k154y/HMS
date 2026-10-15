import {Application} from "./navigation";
type Profile=Record<string,unknown>;
export type ProductAccess={state:"allowed"|"denied"|"unavailable"|"expired";profile?:Profile};
export async function productAccess(token:string,application:Application):Promise<ProductAccess>{
 const base=process.env.HMS_API_URL??"http://localhost:8081/api/v1";
 const options={headers:{Authorization:`Bearer ${token}`},cache:"no-store" as const,signal:AbortSignal.timeout(15000)};
 try{
  const response=await fetch(`${base}/auth/me`,options);
  if(response.status===401)return {state:"expired"};
  if(!response.ok)return {state:response.status===403?"denied":"unavailable"};
  const profile=await response.json() as Profile;
  if(application==="PLATFORM_ADMIN")return {state:profile.role==="SUPER_ADMIN"?"allowed":"denied",profile};
  if(application==="HMS")return {state:profile.role&&profile.role!=="SUPER_ADMIN"&&profile.hotelId&&profile.branchId?"allowed":"denied",profile};
  if(profile.mustChangePassword===true)return {state:Array.isArray(profile.commerceBusinesses)&&profile.commerceBusinesses.length>0?"allowed":"denied",profile};
  // This endpoint checks current active membership, entitlements and availability.
  const businesses=await fetch(`${base}/commerce/businesses`,{...options,signal:AbortSignal.timeout(15000)});
  if(businesses.status===401)return {state:"expired"};
  if(!businesses.ok)return {state:[403,404].includes(businesses.status)?"denied":"unavailable",profile};
  const rows=await businesses.json();
  return {state:Array.isArray(rows)?rows.length?"allowed":"denied":"unavailable",profile};
 }catch{return {state:"unavailable"};}
}
