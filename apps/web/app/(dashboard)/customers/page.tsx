"use client";
import {AccountHistory} from "@/components/operations/AccountHistory";
import {MasterData} from "@/components/operations/MasterData";
import {customerFields} from "@/components/operations/fields";
export default function Customers(){return <MasterData title="Customers" resource="customers" fields={customerFields} columns={["code","name","kind","email","phone","active"]} extra={r=><AccountHistory key={r.id} record={r}/>}/>}

