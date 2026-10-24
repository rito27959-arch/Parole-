import {createVerify,randomUUID} from 'node:crypto';
import {ServiceError} from './groq.js';
import {userPath} from './store.js';
let cachedKeys=null, keysExpires=0;
async function googleKey(id) {
  if(!cachedKeys || Date.now()>keysExpires) {
    const r=await fetch('https://www.gstatic.com/admob/reward/verifier-keys.json',{signal:AbortSignal.timeout(10000)});
    if(!r.ok)throw new ServiceError(503,'Vérification publicitaire indisponible.','SSV_UNAVAILABLE');
    cachedKeys=(await r.json()).keys;keysExpires=Date.now()+3600000;
  }
  return cachedKeys.find(k=>String(k.keyId)===id)?.pem;
}
export async function createRewardTicket(store,uid) {
  const id=randomUUID(); await store.set(`rewardTickets/${id}`,{uid,createdAt:Date.now(),used:false});return {id,userId:uid};
}
export async function verifyReward(rawUrl,{store,adUnit,getKey=googleKey,now=Date.now()}) {
  const rawQuery=rawUrl.split('?')[1]||'', marker=rawQuery.indexOf('&signature=');
  if(marker<0)throw new ServiceError(400,'Signature manquante.','SSV_INVALID');
  const params=new URLSearchParams(rawQuery);
  for(const key of ['signature','key_id','transaction_id','timestamp','user_id','custom_data','ad_unit'])if(params.getAll(key).length!==1)throw new ServiceError(400,'Paramètre publicitaire invalide.','SSV_INVALID');
  const pem=await getKey(params.get('key_id'));
  if(!pem || !createVerify('SHA256').update(rawQuery.slice(0,marker)).verify(pem,Buffer.from(params.get('signature'),'base64url')))throw new ServiceError(403,'Signature incorrecte.','SSV_INVALID');
  if(!adUnit || params.get('ad_unit')!==adUnit)throw new ServiceError(403,'Bloc publicitaire non autorisé.','SSV_INVALID');
  const timestamp=Number(params.get('timestamp'));
  if(!Number.isFinite(timestamp)||Math.abs(now-timestamp)>86400000)throw new ServiceError(400,'Notification expirée.','SSV_EXPIRED');
  const uid=params.get('user_id'),ticket=params.get('custom_data'),tid=params.get('transaction_id');
  if(!/^[A-Za-z0-9_-]{1,128}$/.test(uid)||!/^[0-9a-f-]{36}$/i.test(ticket)||!/^[A-Za-z0-9_-]{1,200}$/.test(tid))throw new ServiceError(400,'Récompense invalide.','SSV_INVALID');
  return store.atomic(async tx=>{
    const event=await tx.get(`rewardEvents/${tid}`), intent=await tx.get(`rewardTickets/${ticket}`);
    const path=userPath(uid,'private/wallet'), wallet=await tx.get(path)||{day:new Date(now).toISOString().slice(0,10),used:0,credits:0};
    if(event)return {credited:false};
    if(!intent||intent.uid!==uid||intent.used||now-intent.createdAt>86400000)throw new ServiceError(403,'Récompense non attendue.','SSV_INVALID');
    wallet.credits+=2;await tx.set(path,wallet);await tx.set(`rewardEvents/${tid}`,{uid,ticket,at:now});await tx.set(`rewardTickets/${ticket}`,{...intent,used:true});return {credited:true};
  });
}
