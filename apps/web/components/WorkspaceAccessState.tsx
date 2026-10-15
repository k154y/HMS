"use client";
import {useLocale} from "@/components/LocaleProvider";
import WorkspaceNavigation from "@/components/WorkspaceNavigation";
export type AccessState = "disabled"|"unassigned"|"denied"|"unavailable"|"hotel";
const messages:Record<AccessState,string>={
 disabled:"Commerce is currently disabled. Contact your administrator for availability.",
 unassigned:"No commerce business is assigned to your account. Ask your administrator for access.",
 denied:"You do not have access to this workspace. Contact your administrator.",
 unavailable:"This workspace is temporarily unavailable. Please try again shortly.",
 hotel:"No active hotel workspace is assigned to your account. Ask your administrator for access.",
};
export default function WorkspaceAccessState({state}:{state:AccessState}) {
 const {t}=useLocale();
 return <main className="mx-auto w-full max-w-3xl px-6 py-12"><WorkspaceNavigation controls/><section className="mt-10 rounded-2xl border border-slate-200 bg-white p-8"><h1 className="text-2xl font-semibold">{t("Workspace access")}</h1><p role="status" className="mt-4 text-slate-600">{t(messages[state])}</p></section></main>;
}
