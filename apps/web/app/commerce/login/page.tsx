import ProductLoginEntry from "@/components/ProductLoginEntry";
export default async function Page({searchParams}:{searchParams:Promise<{next?:string}>}){const params=await searchParams;return <ProductLoginEntry application="COMMERCE" next={params.next}/>;}
