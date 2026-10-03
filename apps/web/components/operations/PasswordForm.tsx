"use client";
import {useLocale} from "@/components/LocaleProvider";
import {FormEvent,useEffect,useState,useRef} from "react";
import {signOut} from "next-auth/react";
import Link from "next/link";
import {api} from "@/lib/hms-api";
import {Field,inputStyle,buttonStyle} from "./ui";

export function PasswordForm({mode}:{mode:"forgot"|"reset"|"change"}) {
  const {t}=useLocale();
  const [email,setEmail]=useState("");const [current,setCurrent]=useState("");
  const [password,setPassword]=useState("");const [confirm,setConfirm]=useState("");
  const [token,setToken]=useState("");const [busy,setBusy]=useState(false);const [message,setMessage]=useState("");const [error,setError]=useState("");
  const tokenRead=useRef(false);
  useEffect(()=>{if(mode==="reset"&&!tokenRead.current) {tokenRead.current=true;setToken(new URLSearchParams(window.location.hash.slice(1)).get("token")??"");window.history.replaceState(null,"",window.location.pathname);}},[mode]);
  async function submit(e:FormEvent){e.preventDefault();setError("");setBusy(true);try {
    if(mode!=="forgot"&&(password!==confirm||Array.from(password).length<15||Array.from(password).length>128))throw new Error("Use 15–128 characters and matching passwords.");
    if(mode==="forgot") {await api("account/forgot-password","POST",{email});setMessage("If an account exists for the supplied information, password reset instructions have been sent.");}
    else if(mode==="reset") {await api("account/reset-password","POST",{token,newPassword:password,confirmPassword:confirm});setToken("");setPassword("");setConfirm("");setMessage("Password reset. You can now sign in.");}
    else {await api("account/password","POST",{currentPassword:current,newPassword:password,confirmPassword:confirm});await signOut({callbackUrl:"/login"});}
  }catch(e){setError(e instanceof Error?e.message:"Unable to complete this request.");}finally{setBusy(false);}}
  return <section className="rounded-xl border bg-white p-6 space-y-4"><h2 className="text-xl font-semibold">{t(mode==="forgot"?"Forgot password":mode==="reset"?"Reset password":"Change password")}</h2>
    {message?<p role="status">{t(message)}</p>:<form onSubmit={submit} className="space-y-4">
      {mode==="forgot"?<Field label="Email"><input className={inputStyle} type="email" autoComplete="email" required maxLength={255} value={email} onChange={e=>setEmail(e.target.value)}/></Field>:<>
        <p className="text-sm text-slate-600">{t("Use 15–128 characters. Changing your password signs out existing sessions when their access tokens expire.")}</p>
        {mode==="change"&&<Field label="Current password"><input className={inputStyle} type="password" autoComplete="current-password" required value={current} onChange={e=>setCurrent(e.target.value)}/></Field>}
        <Field label="New password"><input className={inputStyle} type="password" autoComplete="new-password" required value={password} onChange={e=>setPassword(e.target.value)}/></Field>
        <Field label="Confirm password"><input className={inputStyle} type="password" autoComplete="new-password" required value={confirm} onChange={e=>setConfirm(e.target.value)}/></Field>
      </>}
      {mode==="reset"&&!token&&<p role="alert">{t("Open a valid reset link from your email. If this page was refreshed, reopen the link.")}</p>}
      {error&&<p role="alert" className="text-red-700">{t(error)}</p>}
      <button className={buttonStyle} disabled={busy||(mode==="reset"&&!token)}>{t(busy?"Please wait…":mode==="forgot"?"Send reset instructions":"Save password")}</button>
    </form>}
    {mode!=="change"&&<Link className="block text-blue-700" href="/login">{t("Back to sign in")}</Link>}
  </section>;
}
