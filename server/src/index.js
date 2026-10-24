import {initializeApp,cert,applicationDefault,getApps} from 'firebase-admin/app';
import {getAuth} from 'firebase-admin/auth';
import {getFirestore} from 'firebase-admin/firestore';
import {getAppCheck} from 'firebase-admin/app-check';
import {buildApp} from './app.js';
import {FirestoreStore,MemoryStore} from './store.js';
import {GroqClient} from './groq.js';

const projectId=process.env.FIREBASE_PROJECT_ID;
const ready=Boolean(projectId&&process.env.GROQ_API_KEY);
if(projectId&&!getApps().length)initializeApp({projectId,credential:process.env.FIREBASE_SERVICE_ACCOUNT_JSON?cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON)):applicationDefault()});
const app=await buildApp({
  ready,firebasePublicConfig:process.env.FIREBASE_WEB_API_KEY&&process.env.FIREBASE_ANDROID_APP_ID?{apiKey:process.env.FIREBASE_WEB_API_KEY,applicationId:process.env.FIREBASE_ANDROID_APP_ID,projectId}:null,store:projectId?new FirestoreStore(getFirestore()):new MemoryStore(),groq:new GroqClient(),
  verifyToken:async token=>(await getAuth().verifyIdToken(token,true)).uid,
  verifyAppCheck:process.env.REQUIRE_APP_CHECK==='true'?async token=>getAppCheck().verifyToken(token):null,
  dailyLimit:Number(process.env.FREE_DAILY_SESSIONS||3),adUnit:process.env.ADMOB_REWARDED_AD_UNIT||'',
  logger:{level:'info',redact:['req.headers.authorization','req.headers.cookie','req.body','res.body']}
});
for(const signal of ['SIGTERM','SIGINT'])process.on(signal,async()=>{await app.close();process.exit(0);});
await app.listen({port:Number(process.env.PORT||8080),host:'0.0.0.0'});
