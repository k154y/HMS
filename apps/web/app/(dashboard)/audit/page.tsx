import {ReadRecords} from "@/components/operations/ReadRecords";
export default function Audit(){return <ReadRecords title="Audit Trail" resource="audit-events" columns={{occurredAt:"Date",action:"Action",entityType:"Entity",actorUserId:"User",requestId:"Request",reason:"Reason",approvalStatus:"Approval status",device:"Device"}}/>}

