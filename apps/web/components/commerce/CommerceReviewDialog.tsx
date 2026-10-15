"use client";
import {useLocale} from "@/components/LocaleProvider";

import {useEffect,useId,useRef,useState,ReactNode,RefObject} from "react";
import {createPortal} from "react-dom";

export default function CommerceReviewDialog({title,busy,onClose,returnFocus,children}:{title:string;busy:boolean;onClose:()=>void;returnFocus:RefObject<HTMLButtonElement|null>;children:ReactNode}){
 const {t}=useLocale();
 const [host,setHost]=useState<HTMLElement|null>(null);
 const dialog=useRef<HTMLDialogElement>(null),heading=useId();
 useEffect(()=>{const node=document.createElement("div");node.dataset.commerceModal="true";document.body.appendChild(node);setHost(node);return()=>node.remove()},[]);
 useEffect(()=>{
  const node=dialog.current;if(!host||!node)return;
  const bodyOverflow=document.body.style.overflow,rootOverflow=document.documentElement.style.overflow;
  const trigger=returnFocus.current;
  document.body.style.overflow="hidden";document.documentElement.style.overflow="hidden";
  // showModal enters the browser top layer and makes everything outside this
  // dialog inert, including sticky headers in unrelated stacking contexts.
  node.showModal();node.focus();
  return()=>{node.close();document.body.style.overflow=bodyOverflow;document.documentElement.style.overflow=rootOverflow;trigger?.focus({preventScroll:true})};
 },[host,returnFocus]);
 if(!host)return null;
 return createPortal(<dialog ref={dialog} role="dialog" aria-modal="true" aria-labelledby={heading} tabIndex={-1}
  className="commerce-review-dialog fixed inset-0 m-auto max-h-[92dvh] w-[calc(100%-1.5rem)] max-w-4xl overflow-hidden rounded-xl border-0 bg-white p-0 text-slate-900 shadow-2xl"
  onCancel={event=>{event.preventDefault();if(!busy)onClose()}}
  onKeyDown={event=>{
   if(event.key!=="Tab")return;
   const controls=Array.from(event.currentTarget.querySelectorAll<HTMLElement>('button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),summary,a[href],[tabindex="0"]')).filter(el=>el.getClientRects().length>0);
   const first=controls[0],last=controls.at(-1),active=document.activeElement;
   if(!first){event.preventDefault();event.currentTarget.focus();return;}
   if(event.shiftKey&&(active===first||active===event.currentTarget)){event.preventDefault();last?.focus()}
   else if(!event.shiftKey&&(active===last||active===event.currentTarget)){event.preventDefault();first.focus()}
  }}>
  <div className="flex max-h-[92dvh] flex-col"><header className="flex shrink-0 items-center justify-between gap-3 border-b p-4"><div><h2 id={heading} className="font-semibold">{title}</h2><p className="text-xs text-slate-500">{t("Review the details before confirming.")}</p></div><button type="button" disabled={busy} onClick={onClose} aria-label="Close review" className="rounded-lg border px-3 py-2 text-sm disabled:opacity-50">{t("Close")}</button></header><div className="min-h-0 overflow-y-auto overscroll-contain p-4">{children}</div></div>
  <style>{`.commerce-review-dialog {z-index:2147483647} .commerce-review-dialog::backdrop {position:fixed;inset:0;background:rgb(2 6 23 / 65%);touch-action:none}`}</style>
 </dialog>,host);
}
