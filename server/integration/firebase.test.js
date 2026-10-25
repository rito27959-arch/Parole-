import test from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {initializeApp,deleteApp} from 'firebase-admin/app';
import {getAuth} from 'firebase-admin/auth';
import {getFirestore} from 'firebase-admin/firestore';
import {FirestoreStore} from '../src/store.js';
import {buildApp} from '../src/app.js';

// These tests may only run against local emulators; they never use a real account.
const projectId='demo-parole-ci';
for(const key of ['FIRESTORE_EMULATOR_HOST','FIREBASE_AUTH_EMULATOR_HOST']) {
  assert.match(process.env[key]||'',/^(127\.0\.0\.1|localhost):\d+$/,`${key} must identify a local emulator`);
}
const firebase=initializeApp({projectId}),auth=getAuth(firebase),db=getFirestore(firebase);
const question={question:'Quel problème votre application résout-elle ?',sourcePage:1,focus:'Objectifs'};
async function register(email) {
  const r=await fetch(`http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=emulator-only`,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({email,password:'Test-only-password-123',returnSecureToken:true})});
  assert.equal(r.status,200);return r.json();
}
function documentRequest(uid,path,token,method='GET',body) {
  return fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/v1/projects/${projectId}/databases/(default)/documents/users/${uid}/${path}`,{method,headers:{...(token?{authorization:`Bearer ${token}`}:{ }),'content-type':'application/json'},...(body?{body:JSON.stringify(body)}:{})});
}
test('Firebase Auth + Firestore + security rules + full account lifecycle',async t=>{
  const alice=await register(`alice-${randomUUID()}@example.test`),bob=await register(`bob-${randomUUID()}@example.test`);
  const app=await buildApp({store:new FirestoreStore(db),verifyToken:async token=>{const identity=await auth.verifyIdToken(token,true);return {uid:identity.uid,authTime:identity.auth_time};},deleteIdentity:uid=>auth.deleteUser(uid),groq:{first:async()=>question,review:async(p,s,text,last)=>({strengths:['Objectif expliqué.'],improvements:['Donnez un exemple.'],clarity:4,structure:3,precision:4,evidence:'',nextQuestion:last?null:question})}});
  const headers={authorization:`Bearer ${alice.idToken}`};let project,session;
  try {
    await t.test('real Firebase token authenticates and document persists',async()=>{
      const result=await app.inject({method:'POST',url:'/v1/projects',headers,payload:{title:'Projet de bibliothèque',text:'Notre application de bibliothèque aide les étudiants à chercher les livres disponibles puis à réserver un exemplaire. Les données sont conservées dans une base partagée.'}});
      assert.equal(result.statusCode,200,result.body);project=result.json();assert.ok((await db.doc(`users/${alice.localId}/projects/${project.id}`).get()).exists);
    });
    await t.test('Firestore rules allow own reads and deny cross-user reads and credit writes',async()=>{
      assert.equal((await documentRequest(alice.localId,`projects/${project.id}`,alice.idToken)).status,200);
      assert.equal((await documentRequest(alice.localId,`projects/${project.id}`,bob.idToken)).status,403);
      assert.equal((await documentRequest(alice.localId,`projects/${project.id}`,null)).status,403);
      assert.equal((await documentRequest(alice.localId,'private/wallet',alice.idToken,'PATCH',{fields:{credits:{integerValue:'999'}}})).status,403);
    });
    await t.test('Firestore transactions preserve quota and answer idempotency',async()=>{
      const request={projectId:project.id,requestId:randomUUID(),questionCount:2};
      const start=await app.inject({method:'POST',url:'/v1/sessions',headers,payload:request});assert.equal(start.statusCode,200,start.body);session=start.json();
      assert.equal((await app.inject({method:'POST',url:'/v1/sessions',headers,payload:request})).json().id,session.id);
      assert.equal((await app.inject({url:'/v1/account',headers})).json().freeRemaining,2);
      const answer={requestId:randomUUID(),questionIndex:0,text:'Cette application facilite les réservations de livres par les étudiants.',durationMs:5000};
      const send=()=>app.inject({method:'POST',url:`/v1/sessions/${session.id}/answers`,headers,payload:answer});
      const first=await send();assert.equal(first.statusCode,200,first.body);assert.equal((await send()).json().answers.length,1);
      const last=await app.inject({method:'POST',url:`/v1/sessions/${session.id}/answers`,headers,payload:{...answer,questionIndex:1,requestId:randomUUID()}});assert.equal(last.statusCode,200,last.body);assert.equal(last.json().status,'completed');
      assert.equal((await db.doc(`users/${alice.localId}/sessions/${session.id}`).get()).data().answers.length,2);
    });
    await t.test('account removal deletes Firebase identity, data and invalidates token',async()=>{
      const removed=await app.inject({method:'DELETE',url:'/v1/account',headers});assert.equal(removed.statusCode,200,removed.body);
      assert.equal((await db.collection(`users/${alice.localId}/projects`).get()).size,0);
      assert.equal((await db.collection(`users/${alice.localId}/sessions`).get()).size,0);
      await assert.rejects(()=>auth.getUser(alice.localId),e=>e.code==='auth/user-not-found');
      assert.equal((await app.inject({url:'/v1/account',headers})).statusCode,401);
    });
  } finally {await app.close();await auth.deleteUser(bob.localId);await deleteApp(firebase);}
});
