export type Product={id:string;name:string;sku:string;barcode?:string;base_unit?:string;active?:boolean;fractional_units?:boolean;retail_price?:number|string;wholesale_price?:number|string;purchase_cost?:number|string};
export type ProductUnit={unit_name:string;units_per_package:string;purchase_package_price?:string|null;retail_package_price?:string|null;wholesale_package_price?:string|null};
export type Item={productId:string;unit:string;quantity:string;unitPrice:string;discount:string;discountMode:"AMOUNT"|"PERCENT";reason:string};

// Catalog display is in the business base currency. Backend preview remains
// authoritative for branch prices, exchange rates, taxes and permissions.
export function catalogPrice(product:Product,unit:ProductUnit,kind:string):number|null{
 const value=kind==="PURCHASE"?unit.purchase_package_price??(product.purchase_cost==null?null:Number(product.purchase_cost)*Number(unit.units_per_package)):
  unit.unit_name===product.base_unit?unit.retail_package_price??product.retail_price:unit.wholesale_package_price;
 if(value==null||String(value).trim()==="")return null;
 const amount=Number(value);return Number.isFinite(amount)&&(kind==="PURCHASE"?amount>=0:amount>0)?amount:null;
}
export function changeItem(item:Item,key:keyof Item,value:string):Item{
 // An override entered for a bottle must never follow a switch to a crate.
 return key==="unit"?{...item,unit:value,unitPrice:""}:{...item,[key]:value};
}
export function quantityIssue(product:Product,unit:ProductUnit,quantity:string,available?:number):string{
 const count=Number(quantity),base=count*Number(unit.units_per_package);
 if(!Number.isFinite(count)||count<=0)return "Enter a positive quantity.";
 if(product.fractional_units===false&&Math.abs(base-Math.round(base))>0.0000001)return "Choose a quantity that contains whole stock units.";
 if(available!==undefined&&base>available+0.0000001)return "Not enough stock at this location. Reduce the quantity or choose another location.";
 return "";
}
