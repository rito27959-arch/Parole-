import { randomUUID } from 'node:crypto';
import { ServiceError } from './groq.js';

export class MemoryStore {
  constructor() { this.docs = new Map(); this.tail = Promise.resolve(); }
  async get(path) { return structuredClone(this.docs.get(path) ?? null); }
  async set(path, data) { this.docs.set(path, structuredClone(data)); }
  async delete(path) { this.docs.delete(path); }
  async list(prefix) { return [...this.docs].filter(([k])=>k.startsWith(prefix+'/') && !k.slice(prefix.length+1).includes('/')).map(([id,v])=>({id:id.split('/').at(-1),...structuredClone(v)})); }
  async atomic(callback) { let release; const before = this.tail; this.tail = new Promise(r=>release=r); await before; const snapshot = structuredClone(this.docs); try { return await callback(this); } catch(e) { this.docs=snapshot; throw e; } finally { release(); } }
}
export class FirestoreStore {
  constructor(db, transaction = null) { this.db=db; this.transaction=transaction; }
  async get(path) { const ref=this.db.doc(path); const doc=await (this.transaction ? this.transaction.get(ref) : ref.get()); return doc.exists ? doc.data() : null; }
  async set(path,data) { const ref=this.db.doc(path); if(this.transaction) this.transaction.set(ref,data); else await ref.set(data); }
  async delete(path) { const ref=this.db.doc(path); if(this.transaction) this.transaction.delete(ref); else await ref.delete(); }
  async list(path) { const snap=await this.db.collection(path).get(); return snap.docs.map(doc=>({id:doc.id,...doc.data()})); }
  async atomic(callback) { return this.db.runTransaction(tx=>callback(new FirestoreStore(this.db,tx))); }
}
export function userPath(uid, suffix) { return `users/${uid}/${suffix}`; }
export async function requireOwned(store, uid, collection, id) {
  const value=await store.get(userPath(uid,`${collection}/${id}`));
  if (!value) throw new ServiceError(404, 'Élément introuvable.', 'NOT_FOUND');
  return value;
}
export async function reserveSession(store, uid, request, dailyLimit) {
  return store.atomic(async tx=>{
    const path=userPath(uid,`sessions/${request.requestId}`), existing=await tx.get(path);
    if(existing) return { session:existing, created:false };
    const day=new Date().toISOString().slice(0,10), walletPath=userPath(uid,'private/wallet');
    const wallet=await tx.get(walletPath) || {day,used:0,credits:0};
    if(wallet.day!==day) {wallet.day=day;wallet.used=0;}
    if(request.rewarded) {
      if(wallet.credits<2) throw new ServiceError(402,'Pas encore de crédit récompensé disponible.','NO_CREDITS');
      wallet.credits-=2;
    } else {
      if(wallet.used>=dailyLimit) throw new ServiceError(429,'Vos séances gratuites du jour sont utilisées. Revenez demain ou débloquez une courte séance.','DAILY_LIMIT');
      wallet.used++;
    }
    const session={id:request.requestId,projectId:request.projectId,level:request.level,minutes:request.minutes,style:request.style,questionCount:request.rewarded?2:request.questionCount,rewarded:request.rewarded,status:'preparing',answers:[],currentQuestion:null,createdAt:new Date().toISOString(),busyUntil:Date.now()+90000};
    await tx.set(walletPath,wallet); await tx.set(path,session); return {session,created:true};
  });
}
export async function refundSession(store,uid,session) {
  await store.atomic(async tx=>{
    const path=userPath(uid,`sessions/${session.id}`), current=await tx.get(path);
    const walletPath=userPath(uid,'private/wallet'), wallet=await tx.get(walletPath);
    if(current?.status!=='preparing') return;
    if(wallet) {
      if(session.rewarded) wallet.credits+=2;
      else if(wallet.day===session.createdAt.slice(0,10)) wallet.used=Math.max(0,wallet.used-1);
      await tx.set(walletPath,wallet);
    }
    await tx.delete(path);
  });
}
export async function claimAnswer(store,uid,id,request) {
  return store.atomic(async tx=>{
    const path=userPath(uid,`sessions/${id}`), session=await requireOwned(tx,uid,'sessions',id);
    if(session.answers.some(a=>a.requestId===request.requestId)) return {session,duplicate:true};
    if(session.status!=='active') throw new ServiceError(409,'Cette séance ne peut pas recevoir de réponse.','SESSION_STATE');
    if(session.answers.length!==request.questionIndex) throw new ServiceError(409,'Cette question a déjà reçu une réponse. Actualisez la séance.','STALE_QUESTION');
    if(session.busyUntil>Date.now()) throw new ServiceError(409,'Le jury analyse déjà une réponse.','BUSY');
    const lease=randomUUID(); session.lease=lease;session.busyUntil=Date.now()+90000;await tx.set(path,session);return {session,lease,duplicate:false};
  });
}
export function publicProject(project) { const {pages,...metadata}=project; return metadata; }
export function publicSession(session) { const {lease,busyUntil,...rest}=session; return rest; }
