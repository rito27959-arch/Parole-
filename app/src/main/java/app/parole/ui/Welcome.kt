package app.parole.ui

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import app.parole.data.Appearance
import kotlin.math.*

/** Native, offline introduction. No permission, account or network prerequisite. */
@Composable
fun ParoleWelcome(
    appearance: Appearance = Appearance.SYSTEM,
    initialLevel: String = "BTS",
    initialJury: String = "Bienveillant",
    initialPage: Int = 0,
    motionEnabled: Boolean = ValueAnimator.areAnimatorsEnabled(),
    onFinish: (String, String) -> Unit
) {
    var page by rememberSaveable { mutableIntStateOf(initialPage.coerceIn(0, 2)) }
    var level by rememberSaveable { mutableStateOf(initialLevel) }
    var jury by rememberSaveable { mutableStateOf(initialJury) }
    val haptics = LocalHapticFeedback.current
    BackHandler(page > 0) { page-- }
    val finish = { onFinish(level, jury) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val animate = motionEnabled && lifecycle.isAtLeast(Lifecycle.State.RESUMED)

    ParoleTheme(appearance) {
        val c = MaterialTheme.colorScheme
        Surface(color = c.background, modifier = Modifier.fillMaxSize().testTag("welcome_root")) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                val heroHeight = if (LocalDensity.current.fontScale > 1.3f) 150.dp else (maxHeight * .30f).coerceIn(160.dp, 260.dp)
                Column(Modifier.widthIn(max = 640.dp).fillMaxSize().align(Alignment.TopCenter)) {
                    Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GraphicEq, null, Modifier.size(22.dp), tint = c.primary)
                        Text("parole", Modifier.padding(start = 8.dp).weight(1f), fontSize = 23.sp, letterSpacing = (-1).sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = finish, modifier = Modifier.testTag("welcome_skip")) { Text("Passer", color = c.onSurfaceVariant) }
                    }
                    Crossfade(targetState = page, animationSpec = tween(if (motionEnabled) 240 else 0), label = "welcome_page", modifier = Modifier.weight(1f)) { current ->
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp)) {
                            // Leave both jury choices visible on a standard phone without scrolling.
                            VoiceSculpture(current, if (current == 1) heroHeight * .75f else heroHeight, animate)
                            Text("0${current + 1}  /  03   ·   " + listOf("L’ÉLAN", "LE DIALOGUE", "LE PREMIER PAS")[current], color = c.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.3.sp, modifier = Modifier.padding(top = 12.dp, bottom = 14.dp).semantics { contentDescription = "Étape ${current + 1} sur 3" })
                            when (current) {
                                0 -> {
                                    WelcomeHeading("Vos idées méritent", "une voix.")
                                    Text("Du premier mot au jour J. Préparez votre soutenance avec un jury IA qui vous aide à avancer.", style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
                                    Row(Modifier.padding(top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(6.dp).background(c.primary, CircleShape))
                                        Text("Votre rapport. Votre rythme. Votre voix.", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                                    }
                                }
                                1 -> {
                                    WelcomeHeading("Un jury à", "votre écoute.")
                                    Text("Choisissez votre point de départ. Vous pourrez tout ajuster avant chaque séance.", color = c.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp, bottom = 18.dp))
                                    Text("Votre niveau", style = MaterialTheme.typography.labelLarge)
                                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("BTS", "Licence", "Master").forEach { value ->
                                            Surface(color = if (level == value) c.primaryContainer else c.surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).selectable(level == value, role = Role.RadioButton, onClick = { level = value }).testTag("welcome_level_$value")) {
                                                Text(value, Modifier.padding(vertical = 15.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelLarge, color = if (level == value) c.onPrimaryContainer else c.onSurface)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(18.dp))
                                    JuryOption("Bienveillant", "Prendre confiance, un pas à la fois.", Icons.Rounded.FavoriteBorder, jury) { jury = it }
                                    Spacer(Modifier.height(8.dp))
                                    JuryOption("Exigeant", "Approfondir et défendre vos choix.", Icons.Rounded.AutoAwesome, jury) { jury = it }
                                }
                                2 -> {
                                    WelcomeHeading("Le jour J,", "prenez votre place.")
                                    Text("Une préparation simple, de votre sujet à votre prochain progrès.", color = c.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp, bottom = 20.dp))
                                    JourneyRow("1", "Apportez votre sujet", "Un PDF ou du texte, à vous de choisir.")
                                    JourneyRow("2", "Échangez avec le jury", "Parlez ou écrivez, sans vous précipiter.")
                                    JourneyRow("3", "Repartez avec une piste", "Des retours précis pour votre prochain essai.")
                                    Row(Modifier.padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = c.onSurfaceVariant)
                                        Text("Vous gardez la main. L’envoi de vos contenus à l’IA et l’accès au micro vous seront demandés au moment utile.", Modifier.padding(start = 9.dp), color = c.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                        }
                    }
                    Surface(color = c.background) {
                        Column(Modifier.padding(horizontal = 28.dp).padding(top = 8.dp, bottom = 18.dp)) {
                            Row(Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                repeat(3) { dot ->
                                    val width by animateDpAsState(if (dot == page) 28.dp else 6.dp, tween(if (motionEnabled) 220 else 0), label = "progress")
                                    Box(Modifier.padding(horizontal = 4.dp).size(width, 5.dp).clip(CircleShape).background(if (dot == page) c.primary else c.outlineVariant))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (page > 0) OutlinedIconButton(onClick = { page-- }, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(20.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Étape précédente") }
                                Button(onClick = { if (motionEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (page < 2) page++ else finish() }, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("welcome_next"), shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)) {
                                    Text(listOf("Trouver ma voix", "C’est mon rythme", "Entrer dans Parole")[page], Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(21.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun WelcomeHeading(first: String, second: String) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
        Text(first, fontSize = 33.sp, lineHeight = 38.sp, letterSpacing = (-1.1).sp, fontWeight = FontWeight.SemiBold, color = c.onBackground)
        Text(second, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 39.sp, lineHeight = 44.sp, letterSpacing = (-1.2).sp, color = c.primary)
    }
}

@Composable private fun JuryOption(title: String, subtitle: String, icon: ImageVector, selected: String, choose: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    val active = selected == title
    Surface(color = if (active) c.primaryContainer else c.surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, if (active) c.primary.copy(alpha = .35f) else c.outlineVariant), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).selectable(active, role = Role.RadioButton, onClick = { choose(title) }).testTag("welcome_jury_$title")) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(23.dp), tint = if (active) c.onPrimaryContainer else c.onSurfaceVariant)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(title, style = MaterialTheme.typography.labelLarge); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) }
            Icon(if (active) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, Modifier.size(21.dp), tint = if (active) c.primary else c.outline)
        }
    }
}

@Composable private fun JourneyRow(number: String, title: String, detail: String) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.padding(bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(33.dp).background(c.primaryContainer, CircleShape), contentAlignment = Alignment.Center) { Text(number, color = c.onPrimaryContainer, style = MaterialTheme.typography.labelMedium) }
        Column(Modifier.padding(start = 13.dp)) { Text(title, style = MaterialTheme.typography.labelLarge); Text(detail, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) }
    }
}

/** A procedural voice sculpture: precise at every density, with no downloaded assets. */
@Composable private fun VoiceSculpture(page: Int, height: Dp, animate: Boolean) {
    val c = MaterialTheme.colorScheme
    val phase = if (animate) {
        val transition = rememberInfiniteTransition(label = "voice_breath")
        val value by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "voice_phase")
        value
    } else 0f
    val dark = c.background.luminance() < .2f
    val blue = if (dark) Color(0xFF8AA7FF) else ParoleBlue
    val ice = if (dark) Color(0xFFD9E5FF) else Color.White
    Canvas(Modifier.fillMaxWidth().height(height).clearAndSetSemantics {}) {
        val unit = min(size.width, size.height) / 260f
        val center = Offset(size.width / 2, size.height / 2 + 3 * unit)
        val radius = 80 * unit
        val lift = sin(phase) * 3 * unit
        drawCircle(Brush.radialGradient(listOf(blue.copy(alpha = if (dark) .17f else .10f), Color.Transparent), center, radius * 1.65f), radius * 1.65f, center)
        drawOval(c.onBackground.copy(alpha = .05f), Offset(center.x - radius * .65f, center.y + radius * 1.09f), Size(radius * 1.3f, 8 * unit))
        if (page == 2) {
            for (ring in 0..2) drawCircle(blue.copy(alpha = .10f + ring * .025f), radius * (1.13f + ring * .22f), center, style = Stroke(unit))
        }
        val orb = Offset(center.x, center.y + lift)
        drawCircle(Brush.linearGradient(listOf(ice, blue.copy(alpha = .32f), blue.copy(alpha = .83f)), orb - Offset(radius, radius), orb + Offset(radius, radius)), radius, orb)
        drawCircle(Brush.radialGradient(listOf(ice.copy(alpha = .9f), ice.copy(alpha = .02f), Color.Transparent), orb - Offset(radius * .38f, radius * .45f), radius * 1.5f), radius, orb)
        drawCircle(ice.copy(alpha = .7f), radius, orb, style = Stroke(1.2f * unit))
        drawArc(ice.copy(alpha = .9f), 212f, 92f, false, orb - Offset(radius * .91f, radius * .91f), Size(radius * 1.82f, radius * 1.82f), style = Stroke(1.3f * unit, cap = StrokeCap.Round))
        val ink = if (dark) Color(0xFF233F95) else Color(0xFF2146C5)
        repeat(9) { i ->
            val distance = abs(i - 4) / 4f
            val length = (16f + (1 - distance) * 54f + if (animate) sin(phase + i * .65f) * 5f else 0f) * unit
            val x = orb.x + (i - 4) * 12 * unit
            drawLine(ink.copy(alpha = .88f), Offset(x, orb.y - length / 2), Offset(x, orb.y + length / 2), 5.2f * unit, StrokeCap.Round)
        }
        // Detached points connect the voice, the document and the outcome, never a fake score.
        val dots = listOf(Offset(-102f, -48f), Offset(100f, 53f), Offset(87f, -76f))
        dots.forEachIndexed { index, offset ->
            val point = center + offset * unit + Offset(0f, sin(phase + index) * 3 * unit)
            drawCircle(c.surface, (if (index == page) 10f else 4f) * unit, point)
            drawCircle(blue.copy(alpha = .70f), (if (index == page) 5f else 2.5f) * unit, point)
        }
    }
}
