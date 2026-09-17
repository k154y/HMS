import NextAuth from "next-auth";
import Credentials from "next-auth/providers/credentials";
const apiBase=process.env.HMS_API_URL??"http://localhost:8081/api/v1";
// Requests from one rendered page may refresh concurrently. Share the rotating token result.
function validProfile(profile:Record<string,unknown>){return profile.role==="SUPER_ADMIN"||Boolean(profile.role&&profile.hotelId&&profile.branchId)}
const refreshing=new Map<string,Promise<Record<string,unknown>>>();
async function refresh(refreshToken:string){
 const existing=refreshing.get(refreshToken);if(existing)return existing;
 const pending=(async()=>{const response=await fetch(`${apiBase}/auth/refresh`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({refreshToken}),signal:AbortSignal.timeout(15000)});if(!response.ok)throw new Error("Refresh failed");const tokens=await response.json();const profile=await fetch(`${apiBase}/auth/me`,{headers:{Authorization:`Bearer ${tokens.accessToken}`},cache:"no-store"});if(!profile.ok)throw new Error("Profile unavailable");const data=await profile.json();if(!validProfile(data))throw new Error("No active hotel access");return {...data,...tokens} as Record<string,unknown>})();
 refreshing.set(refreshToken,pending);pending.finally(()=>setTimeout(()=>refreshing.delete(refreshToken),10000)).catch(()=>{});return pending;
}
export const {handlers,signIn,signOut,auth}=NextAuth({
session:{
  strategy:"jwt",
  maxAge:30*24*60*60,
},pages:{signIn:"/login"},
 providers:[Credentials({name:"Spring API",credentials:{email:{},password:{}},async authorize(credentials){
  const response=await fetch(`${apiBase}/auth/login`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({email:credentials?.email,password:credentials?.password}),signal:AbortSignal.timeout(15000)});
  if(!response.ok)return null;const tokens=await response.json();
  const responseProfile=await fetch(`${apiBase}/auth/me`,{headers:{Authorization:`Bearer ${tokens.accessToken}`},cache:"no-store"});if(!responseProfile.ok)return null;
  const profile=await responseProfile.json();if(!validProfile(profile))return null;return {...profile,...tokens};
 }})],
 callbacks:{
  async jwt({token,user}){if(user){Object.assign(token,user);return token}if(Date.now()<Date.parse(String(token.accessTokenExpiresAt))-30000)return token;if(typeof token.refreshToken!=="string")return token;try{return {...token,...await refresh(token.refreshToken)}}catch{return {...token,accessToken:undefined,refreshToken:undefined}}},
  async session({session,token}){const {refreshToken,accessTokenExpiresAt,accessToken,...profile}=token;Object.assign(session.user,profile);session.accessToken=typeof accessToken==="string"?accessToken:undefined;return session}
 }
});

