import { auth } from "@/lib/auth";
import { NextRequest, NextResponse } from "next/server";
async function forward(req: NextRequest) {
 const session = await auth();
 if (!session) return NextResponse.json({message:"Sign in required"},{status:401});
 if (session.user.role !== "SUPER_ADMIN") return NextResponse.json({message:"Access denied"},{status:403});
 const base=process.env.HMS_API_URL ?? "http://localhost:8081/api/v1";
 const response=await fetch(`${base}/platform/hotels`,{method:req.method,headers:{Authorization:`Bearer ${session.accessToken}`,"Content-Type":"application/json"},body:req.method==='POST'?await req.text():undefined,cache:"no-store"});
 return new NextResponse(await response.text(),{status:response.status,headers:{"Content-Type":"application/json"}});
}
export const GET=forward;
export const POST=forward;
