import { auth } from "@/lib/auth";
import { redirect } from "next/navigation";
import {headers} from "next/headers";
import {loginDestination} from "@/lib/navigation";
import WorkspaceAccessState from "@/components/WorkspaceAccessState";
import { SessionProvider } from "next-auth/react";
import { SessionGuard } from "@/components/SessionGuard";

export default async function CommerceLayout({ children }: { children: React.ReactNode }) {
  const session = await auth();
  if (!session?.accessToken) redirect(loginDestination((await headers()).get("x-workspace-path")??"/commerce"));
  return <SessionProvider session={session}><SessionGuard />{children}</SessionProvider>;
}
