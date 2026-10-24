package app.parole
import android.app.Application
import app.parole.data.ParoleRepository
class ParoleApplication:Application(){val repository by lazy{ParoleRepository(this)}}
