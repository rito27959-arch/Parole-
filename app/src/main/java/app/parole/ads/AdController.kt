package app.parole.ads

import android.app.Activity
import app.parole.BuildConfig
import app.parole.data.RewardTicket
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.*
import com.google.android.ump.*

class AdController(private val activity:Activity) {
    private val consent=UserMessagingPlatform.getConsentInformation(activity)
    private var initialized=false
    private var loading=false
    fun initialize() {
        consent.requestConsentInfoUpdate(activity,ConsentRequestParameters.Builder().build(),{
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity){ initializeWhenAllowed() }
        },{ initializeWhenAllowed() })
        initializeWhenAllowed()
    }
    private fun initializeWhenAllowed(){if(consent.canRequestAds()&&!initialized){initialized=true;MobileAds.initialize(activity)}}
    fun privacy(onMessage:(String)->Unit){UserMessagingPlatform.showPrivacyOptionsForm(activity){error->if(error!=null)onMessage("Les choix publicitaires ne sont pas disponibles pour le moment.")}}
    fun show(ticket:RewardTicket?,onEarned:()->Unit,onMessage:(String)->Unit) {
        if(loading){onMessage("Une annonce est déjà en préparation.");return}
        if(!consent.canRequestAds()){onMessage("Aucune annonce disponible avec vos choix actuels.");return}
        if(!initialized)initializeWhenAllowed()
        val test=BuildConfig.ADMOB_REWARDED_ID.startsWith("ca-app-pub-3940256099942544")
        if(!test&&ticket==null){onMessage("La récompense ne peut pas être préparée. Réessayez.");return}
        loading=true
        RewardedAd.load(activity,BuildConfig.ADMOB_REWARDED_ID,AdRequest.Builder().build(),object:RewardedAdLoadCallback(){
            override fun onAdFailedToLoad(error:LoadAdError){loading=false;onMessage("Aucune publicité disponible pour le moment. Vous pouvez continuer à utiliser Parole.")}
            override fun onAdLoaded(ad:RewardedAd){
                loading=false
                if(ticket!=null)ad.setServerSideVerificationOptions(ServerSideVerificationOptions.Builder().setUserId(ticket.userId).setCustomData(ticket.id).build())
                ad.fullScreenContentCallback=object:FullScreenContentCallback(){override fun onAdFailedToShowFullScreenContent(error:AdError){onMessage("L’annonce n’a pas pu être affichée.")}}
                ad.show(activity){if(test)onMessage("Annonce de test terminée. Les crédits réels seront activés avec AdMob en production.")else onEarned()}
            }
        })
    }
}
