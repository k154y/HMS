import "next-auth";
declare module "next-auth" { interface User { role?: string; hotelId?: string; branchId?: string; hotelName?: string; subscriptionStatus?: string; trialEndDate?: string; accessToken?: string; } interface Session { user: User; accessToken?: string; } }
declare module "next-auth/jwt" { interface JWT { role?: string; hotelId?: string; branchId?: string; hotelName?: string; subscriptionStatus?: string; trialEndDate?: string; accessToken?: string; } }
