import { auth } from "@/lib/auth";
import { NextRequest } from "next/server";
import { proxyError, proxyResponse, referenceId } from "@/lib/api-proxy-errors";

async function forward(request: NextRequest) {
 const id=referenceId(request.headers);
 const session=await auth();
 if(!session?.accessToken)return proxyError(401,id);
 if(session.user.role!=="SUPER_ADMIN")return proxyError(403,id);
 if(request.method==="POST"){
  const origin=request.headers.get("origin");
  const host=request.headers.get("host");
  let safe=false;
  try{safe=!!origin&&new URL(origin).host===host;}catch{safe=false;}
  if(!safe)return proxyError(403,id);
  if(!(request.headers.get("content-type")??"").toLowerCase().startsWith("application/json"))return proxyError(415,id);
 }
 try{
  const response=await fetch(`${process.env.HMS_API_URL??"http://localhost:8081/api/v1"}/platform/organizations/modules${request.nextUrl.search}`,{
   method:request.method,cache:"no-store",
   headers:{"Authorization":`Bearer ${session.accessToken}`,"X-Request-ID":id,
    ...(request.method==="POST"?{"Content-Type":"application/json"}:{})},
   body:request.method==="POST"?await request.text():undefined,
   signal:AbortSignal.timeout(30000)
  });
  return proxyResponse(response,id);
 }catch{return proxyError(502,id);}
}
export const GET=forward;
export const POST=forward;

