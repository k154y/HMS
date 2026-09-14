"use client";
import {AccountHistory} from "@/components/operations/AccountHistory";
import {MasterData} from "@/components/operations/MasterData";
import {vendorFields} from "@/components/operations/fields";
export default function Vendors(){return <MasterData title="Vendors" resource="vendors" fields={vendorFields} columns={["code","name","phone","paymentTermsDays","active"]} extra={r=><AccountHistory key={r.id} record={r} vendor/>}/>}

