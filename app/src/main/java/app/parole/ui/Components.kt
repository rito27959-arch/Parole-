@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package app.parole.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlin.math.*

@Composable fun PrimaryButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,icon:ImageVector?=null){
    Button(onClick=onClick,enabled=enabled,modifier=modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(19.dp),contentPadding=PaddingValues(horizontal=18.dp,vertical=16.dp),colors=ButtonDefaults.buttonColors(containerColor=ParoleBlue,contentColor=Color.White)){
        if(icon!=null){Icon(icon,null,Modifier.size(20.dp));Spacer(Modifier.width(9.dp))};Text(text,style=MaterialTheme.typography.labelLarge)
    }
}
@Composable fun SoftCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Column(modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),content=content)}
@Composable fun SectionLabel(text:String,action:String?=null,onAction:()->Unit={}){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(text,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));if(action!=null)TextButton(onClick=onAction){Text(action)}}}
@Composable fun BackTitle(title:String,onBack:()->Unit,subtitle:String?=null){Column{TextButton(onClick=onBack,contentPadding=PaddingValues(0.dp)){Icon(Icons.AutoMirrored.Rounded.ArrowBack,null,Modifier.size(20.dp));Spacer(Modifier.width(5.dp));Text("Retour")};Text(title,style=MaterialTheme.typography.headlineLarge);if(subtitle!=null){Spacer(Modifier.height(6.dp));Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable fun IconTile(icon:ImageVector,modifier:Modifier=Modifier){Box(modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Icon(icon,null,Modifier.size(23.dp),tint=MaterialTheme.colorScheme.primary)}}
@Composable fun EmptyState(title:String,description:String,icon:ImageVector=Icons.Rounded.GraphicEq,action:String?=null,onAction:()->Unit={}){SoftCard(Modifier.fillMaxWidth()){
    IconTile(icon);Spacer(Modifier.height(16.dp));Text(title,style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(8.dp));Text(description,color=MaterialTheme.colorScheme.onSurfaceVariant);if(action!=null){Spacer(Modifier.height(20.dp));PrimaryButton(action,onAction)}
}}
@Composable fun ChoiceRow(label:String,values:List<String>,selected:String,onSelect:(String)->Unit){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text(label,style=MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
        values.forEach{value->FilterChip(selected=value==selected,onClick={onSelect(value)},label={Text(value)},modifier=Modifier.heightIn(min=48.dp),shape=RoundedCornerShape(14.dp))}
    }
}}
@Composable fun Waveform(amplitude:Float,active:Boolean,modifier:Modifier=Modifier){
    val transition=rememberInfiniteTransition(label="voice")
    val phase by transition.animateFloat(0f,(2*PI).toFloat(),infiniteRepeatable(tween(2200,easing=LinearEasing)),label="phase")
    val volume by animateFloatAsState(if(active)amplitude.coerceIn(.04f,1f)else .02f,tween(150),label="volume")
    Canvas(modifier.fillMaxWidth().height(156.dp).semantics{contentDescription=if(active)"Microphone actif"else"Microphone au repos"}){
        val middle=size.height/2
        repeat(8){layer->
            val path=Path();val frequency=2.1+layer*.12;val scale=(.25+volume*.75)*(1-layer*.07)
            for(x in 0..180){val position=x/180f;val envelope=sin(position*PI).pow(2);val y=middle+(sin(position*PI*2*frequency+if(active)phase else 0f)*envelope*size.height*.35*scale).toFloat();if(x==0)path.moveTo(0f,y)else path.lineTo(position*size.width,y)}
            drawPath(path,Color(0xFF89BDFF).copy(alpha=.9f-layer*.085f),style=Stroke(width=(1.1f+layer*.12f)*density))
        }
        drawLine(Color(0xFF4589FF).copy(alpha=.15f),Offset(0f,middle),Offset(size.width,middle),1.dp.toPx())
    }
}
@Composable fun Pill(text:String,icon:ImageVector?=null){Row(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){if(icon!=null){Icon(icon,null,Modifier.size(16.dp));Spacer(Modifier.width(7.dp))};Text(text,style=MaterialTheme.typography.bodySmall)}}
@Composable fun SettingRow(title:String,subtitle:String?=null,icon:ImageVector?=null,onClick:(()->Unit)?=null,trailing:@Composable()->Unit={}){Row(Modifier.fillMaxWidth().then(if(onClick!=null)Modifier.clickable(onClick=onClick)else Modifier).heightIn(min=64.dp).padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically){if(icon!=null){Icon(icon,null,tint=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.width(14.dp))};Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.bodyLarge);if(subtitle!=null)Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};trailing()}}
@Composable fun TinyDivider(){HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))}
