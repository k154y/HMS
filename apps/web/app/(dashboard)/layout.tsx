import {SessionGuard} from "@/components/SessionGuard";
import { auth } from "@/lib/auth";
import { SessionProvider } from "next-auth/react";
import { redirect } from "next/navigation";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";

export default async function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await auth();
  if (!session?.accessToken) redirect("/login");
  const user = session.user as any;
  if (user.role === "SUPER_ADMIN") redirect("/platform");

  return (
    <SessionProvider session={session}><SessionGuard/>
      <div className="flex h-screen bg-slate-50 overflow-hidden">
        <Sidebar role={user.role} />
        <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
          <TopBar
            userName={user.name ?? ""}
            userRole={user.role} userRoles={user.roles}
            hotelName={user.hotelName ?? ""}
            subscriptionStatus={user.subscriptionStatus ?? ""}
            trialEndDate={user.trialEndDate ?? ""}
          />
          <main className="flex-1 overflow-y-auto p-5 lg:p-8"><div className="mx-auto w-full max-w-[1440px]">
            {children}
          </div></main>
        </div>
      </div>
    </SessionProvider>
  );
}



