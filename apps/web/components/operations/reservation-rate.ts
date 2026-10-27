// Display and input validation only. ReservationService computes and snapshots the authoritative rate.
export function validReservationRate(value:string):boolean {
 return value==="" || /^\d{1,15}(?:\.\d{1,4})?$/.test(value);
}
export function effectiveReservationRate(custom:string,configured:string|number):string {
 return custom===""?String(configured):custom;
}
