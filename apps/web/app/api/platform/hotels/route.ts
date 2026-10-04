import {proxyError,proxyResponse,referenceId} from "@/lib/api-proxy-errors";
import { auth } from "@/lib/auth";
import { NextRequest, NextResponse } from "next/server";
async function forward(req: NextRequest) {
 const requestId=referenceId(req.headers);
 const session = await auth();
 if (!session) return proxyError(401,requestId);
 if (session.user.role !== "SUPER_ADMIN") return proxyError(403,requestId);
 const base=process.env.HMS_API_URL ?? "http://localhost:8081/api/v1";
 try { const response=await fetch(`${base}/platform/hotels`,{method:req.method,headers:{"X-Request-ID":requestId,Authorization:`Bearer ${session.accessToken}`,"Content-Type":"application/json"},body:req.method==='POST'?await req.text():undefined,cache:"no-store",signal:AbortSignal.timeout(30000)});
 return proxyResponse(response,requestId);
 } catch {return proxyError(502,requestId);}
}
export const GET=forward;
export const POST=forward;
