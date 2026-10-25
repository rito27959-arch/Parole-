package app.parole.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.parole.data.Appearance

val ParoleBlue=Color(0xFF315DFA)
val Ink=Color(0xFF151B2B)
val Midnight=Color(0xFF0B1123)
private val Light=lightColorScheme(primary=ParoleBlue,onPrimary=Color.White,primaryContainer=Color(0xFFE9EEFF),onPrimaryContainer=Color(0xFF2448C6),background=Color(0xFFF5F7FB),onBackground=Ink,surface=Color.White,onSurface=Ink,surfaceVariant=Color(0xFFEDF0F7),onSurfaceVariant=Color(0xFF59647A),outline=Color(0xFF758198),outlineVariant=Color(0xFFDFE4ED),error=Color(0xFFB32630),errorContainer=Color(0xFFFFEDEA))
private val Dark=darkColorScheme(primary=Color(0xFF93ADFF),onPrimary=Color(0xFF0C225D),primaryContainer=Color(0xFF203566),onPrimaryContainer=Color(0xFFDCE5FF),background=Color(0xFF0B1020),onBackground=Color(0xFFF4F6FF),surface=Color(0xFF151D31),onSurface=Color(0xFFF4F6FF),surfaceVariant=Color(0xFF202B43),onSurfaceVariant=Color(0xFFAEBAD0),outline=Color(0xFF8793AD),outlineVariant=Color(0xFF303C54),error=Color(0xFFFFB4B4),errorContainer=Color(0xFF47252D))
private val Font=FontFamily.SansSerif
private val Type=Typography(
    displaySmall=TextStyle(fontFamily=Font,fontSize=34.sp,lineHeight=39.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1.1).sp),
    headlineLarge=TextStyle(fontFamily=Font,fontSize=30.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold,letterSpacing=(-0.8).sp),
    headlineMedium=TextStyle(fontFamily=Font,fontSize=26.sp,lineHeight=32.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-0.5).sp),
    titleLarge=TextStyle(fontFamily=Font,fontSize=21.sp,lineHeight=27.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-0.4).sp),
    titleMedium=TextStyle(fontFamily=Font,fontSize=17.sp,lineHeight=23.sp,fontWeight=FontWeight.SemiBold),
    bodyLarge=TextStyle(fontFamily=Font,fontSize=16.sp,lineHeight=24.sp),
    bodyMedium=TextStyle(fontFamily=Font,fontSize=14.sp,lineHeight=21.sp),
    bodySmall=TextStyle(fontFamily=Font,fontSize=12.sp,lineHeight=18.sp),
    labelLarge=TextStyle(fontFamily=Font,fontSize=15.sp,lineHeight=21.sp,fontWeight=FontWeight.SemiBold),
    labelMedium=TextStyle(fontFamily=Font,fontSize=12.sp,lineHeight=17.sp,fontWeight=FontWeight.Medium)
)
@Composable fun ParoleTheme(appearance:Appearance=Appearance.SYSTEM,content:@Composable () ->Unit){val dark=appearance==Appearance.DARK||(appearance==Appearance.SYSTEM&&isSystemInDarkTheme());MaterialTheme(colorScheme=if(dark)Dark else Light,typography=Type,content=content)}
