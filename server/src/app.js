import Fastify from 'fastify';
import multipart from '@fastify/multipart';
import rateLimit from '@fastify/rate-limit';
import {z} from 'zod';
import {randomUUID} from 'node:crypto';
import {ServiceError} from './groq.js';
import {userPath,requireOwned,publicProject,publicSession,reserveSession,refundSession,claimAnswer} from './store.js';
import {extractPdf} from './pdf.js';
import {createRewardTicket,verifyReward} from './rewards.js';

const uuid=z.string().uuid();
const startSchema=z.object({projectId:uuid,requestId:uuid,level:z.enum(['BTS','Licence','Master','Autre']).default('BTS'),minutes:z.union([z.literal(5),z.literal(10),z.literal(15)]).default(10),style:z.enum(['Bienveillant','Exigeant']).default('Bienveillant'),questionCount:z.number().int().min(2).max(8).default(6),rewarded:z.boolean().default(false)});
const answerSchema=z.object({requestId:uuid,questionIndex:z.number().int().min(0).max(7),text:z.string().trim().min(3).max(8000),durationMs:z.number().int().min(0).max(900000)});
export async function buildApp({store,groq,verifyToken,verifyAppCheck,ready=true,dailyLimit=3,adUnit='',logger=false,pdfExtractor=extractPdf,firebasePublicConfig=null}) {
  const app=Fastify({logger,bodyLimit:150000,requestTimeout:120000,trustProxy:false});
  await app.register(multipart,{limits:{fileSize:10*1024*1024,files:1,fields:4,parts:5}});
  await app.register(rateLimit,{global:true,max:60,timeWindow:'1 minute'});
  app.setErrorHandler((error,req,reply)=>{
    if(error instanceof z.ZodError)return reply.code(400).send({error:'Vérifiez les informations envoyées.',code:'VALIDATION_ERROR'});
    const status=error.statusCode>=400&&error.statusCode<600?error.statusCode:500;
    if(status===500) req.log.error({code:error.code||'INTERNAL_ERROR'},'Request failed');
    return reply.code(status).send({error:status===500?'Une erreur serveur est survenue. Réessayez.':error.message,code:error.code||'REQUEST_FAILED'});
  });
  app.addHook('onSend',async(req,reply,payload)=>{reply.header('Cache-Control','no-store').header('X-Content-Type-Options','nosniff');return payload;});
  app.get('/config',async()=>({firebase:firebasePublicConfig}));
  app.get('/health',async()=>({status:'ok',service:'parole-api',version:'1.0.0',configured:ready}));
  app.get('/ready',async(req,reply)=>reply.code(ready?200:503).send({ready}));
  app.get('/privacy',async(req,reply)=>reply.type('text/plain; charset=utf-8').send('Parole traite vos rapports, réponses et enregistrements pour préparer vos soutenances. Les fichiers PDF et audio sont traités temporairement par le serveur. Le texte, les séances et les bilans sont conservés dans votre compte Firebase. Les extraits nécessaires sont transmis à Groq. Les publicités utilisent Google AdMob selon vos choix de confidentialité. Vous pouvez supprimer vos projets et votre compte dans les réglages. Les résultats sont des conseils pédagogiques indicatifs.'));
  app.get('/admob/ssv',async(req)=>{if(!ready)throw new ServiceError(503,'Service non configuré.');return verifyReward(req.raw.url,{store,adUnit});});
  app.register(async api=>{
    api.addHook('preHandler',async(req)=>{
      if(!ready)throw new ServiceError(503,'Le serveur attend sa configuration Firebase et Groq.','NOT_CONFIGURED');
      const match=/^Bearer (.+)$/.exec(req.headers.authorization||'');
      if(!match)throw new ServiceError(401,'Connectez-vous pour continuer.','UNAUTHENTICATED');
      try { req.uid=await verifyToken(match[1]); if(!req.uid||typeof req.uid!=='string')throw new Error(); }
      catch {throw new ServiceError(401,'Votre session a expiré. Reconnectez-vous.','UNAUTHENTICATED');}
      if(verifyAppCheck)try{await verifyAppCheck(req.headers['x-firebase-appcheck']);}catch{throw new ServiceError(403,'Appareil non reconnu.','APP_CHECK_FAILED');}
    });
    api.get('/account',async req=>{const wallet=await store.get(userPath(req.uid,'private/wallet'));const day=new Date().toISOString().slice(0,10);return {freeRemaining:Math.max(0,dailyLimit-(wallet?.day===day?wallet.used:0)),credits:wallet?.credits||0,rewardsEnabled:!!adUnit};});
    api.get('/projects',async req=>({projects:(await store.list(userPath(req.uid,'projects'))).map(publicProject).sort((a,b)=>b.createdAt.localeCompare(a.createdAt))}));
    api.post('/projects',async req=>{
      const projects=await store.list(userPath(req.uid,'projects'));
      if(projects.length>=50)throw new ServiceError(409,'Supprimez un ancien projet avant d’en ajouter un.','PROJECT_LIMIT');
      let title,pages;
      if(req.isMultipart()) {
        const part=await req.file();if(!part)throw new ServiceError(400,'Choisissez un rapport PDF.');
        const bytes=await part.toBuffer();pages=await pdfExtractor(bytes);title=part.filename.replace(/\.pdf$/i,'').slice(0,100)||'Ma soutenance';
      } else {
        const body=z.object({title:z.string().trim().min(2).max(100),text:z.string().trim().min(80).max(100000)}).parse(req.body);title=body.title;pages=[{number:1,text:body.text}];
      }
      const project={id:randomUUID(),title,pages,pageCount:pages.length,createdAt:new Date().toISOString()};await store.set(userPath(req.uid,`projects/${project.id}`),project);return publicProject(project);
    });
    api.delete('/projects/:id',async req=>{
      const id=uuid.parse(req.params.id);await requireOwned(store,req.uid,'projects',id);
      const sessions=await store.list(userPath(req.uid,'sessions'));
      if(sessions.some(s=>s.projectId===id&&s.busyUntil>Date.now()))throw new ServiceError(409,'Attendez la fin de l’analyse avant de supprimer ce projet.','BUSY');
      for(const session of sessions.filter(s=>s.projectId===id))await store.delete(userPath(req.uid,`sessions/${session.id}`));
      await store.delete(userPath(req.uid,`projects/${id}`));return {deleted:true};
    });
    api.get('/sessions',async req=>({sessions:(await store.list(userPath(req.uid,'sessions'))).filter(s=>s.status!=='preparing').map(publicSession).sort((a,b)=>b.createdAt.localeCompare(a.createdAt))}));
    api.get('/sessions/:id',async req=>publicSession(await requireOwned(store,req.uid,'sessions',uuid.parse(req.params.id))));
    api.post('/sessions',async(req,reply)=>{
      const request=startSchema.parse(req.body),project=await requireOwned(store,req.uid,'projects',request.projectId);
      const reserved=await reserveSession(store,req.uid,request,dailyLimit);
      if(!reserved.created){if(reserved.session.status==='preparing')return reply.code(202).send(publicSession(reserved.session));return publicSession(reserved.session);}
      try {
        const currentQuestion=await groq.first(project,request);const session={...reserved.session,currentQuestion,status:'active',busyUntil:0};
        await store.set(userPath(req.uid,`sessions/${session.id}`),session);return publicSession(session);
      }catch(e){await refundSession(store,req.uid,reserved.session);throw e;}
    });
    api.post('/sessions/:id/answers',async req=>{
      const id=uuid.parse(req.params.id),request=answerSchema.parse(req.body),claimed=await claimAnswer(store,req.uid,id,request);
      if(claimed.duplicate)return publicSession(claimed.session);
      try {
        const project=await requireOwned(store,req.uid,'projects',claimed.session.projectId);
        const last=claimed.session.answers.length+1>=claimed.session.questionCount;
        const review=await groq.review(project,claimed.session,request.text,last);
        return await store.atomic(async tx=>{
          const session=await requireOwned(tx,req.uid,'sessions',id);
          if(session.lease!==claimed.lease)throw new ServiceError(409,'La séance a changé. Actualisez-la.','LEASE_LOST');
          const {nextQuestion,...feedback}=review;
          session.answers.push({requestId:request.requestId,question:session.currentQuestion,text:request.text,durationMs:request.durationMs,feedback,answeredAt:new Date().toISOString()});
          session.currentQuestion=nextQuestion;session.status=last?'completed':'active';session.busyUntil=0;delete session.lease;
          if(last)session.completedAt=new Date().toISOString();
          await tx.set(userPath(req.uid,`sessions/${id}`),session);return publicSession(session);
        });
      } catch(e) {
        await store.atomic(async tx=>{const s=await tx.get(userPath(req.uid,`sessions/${id}`));if(s?.lease===claimed.lease){s.busyUntil=0;delete s.lease;await tx.set(userPath(req.uid,`sessions/${id}`),s);}});throw e;
      }
    });
    api.post('/transcriptions', {config:{rateLimit:{max:12,timeWindow:'1 minute'}}},async req=>{
      const part=await req.file();if(!part)throw new ServiceError(400,'Enregistrement manquant.');
      const allowed=['audio/mp4','audio/m4a','audio/x-m4a','audio/wav','audio/mpeg','audio/aac'];
      if(!allowed.includes(part.mimetype))throw new ServiceError(415,'Format audio non pris en charge.');
      const bytes=await part.toBuffer();if(bytes.length<100)throw new ServiceError(400,'L’enregistrement est trop court.');return groq.transcribe(bytes,part.mimetype,'answer.m4a');
    });
    api.post('/rewards/ticket', {config:{rateLimit:{max:6,timeWindow:'1 minute'}}},async req=>{if(!adUnit)throw new ServiceError(503,'Les récompenses ne sont pas encore activées.','REWARDS_DISABLED');return createRewardTicket(store,req.uid);});
    api.delete('/account',async req=>{
      const sessions=await store.list(userPath(req.uid,'sessions'));
      if(sessions.some(s=>s.busyUntil>Date.now()))throw new ServiceError(409,'Attendez la fin de l’analyse avant de supprimer le compte.','BUSY');
      for(const collection of ['sessions','projects','private'])for(const item of await store.list(userPath(req.uid,collection)))await store.delete(userPath(req.uid,`${collection}/${item.id}`));
      return {deleted:true};
    });
  },{prefix:'/v1'});
  return app;
}
