package com.learningwithav.ninjarunner

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.learningwithav.ninjarunner.domain.Game
import com.learningwithav.ninjarunner.domain.GameSettings
import com.learningwithav.ninjarunner.domain.GameStatus
import com.learningwithav.ninjarunner.domain.MoveDirection
import com.learningwithav.ninjarunner.domain.Weapon
import com.learningwithav.ninjarunner.domain.audio.AudioPlayer
import com.learningwithav.ninjarunner.domain.levels
import com.learningwithav.ninjarunner.domain.target.EasyTarget
import com.learningwithav.ninjarunner.domain.target.MediumTarget
import com.learningwithav.ninjarunner.domain.target.StrongTarget
import com.learningwithav.ninjarunner.domain.target.Target
import com.learningwithav.ninjarunner.util.detectMoveGesture
import com.stevdza_san.sprite.component.drawSpriteView
import com.stevdza_san.sprite.domain.SpriteFlip
import com.stevdza_san.sprite.domain.SpriteSheet
import com.stevdza_san.sprite.domain.SpriteSpec
import com.stevdza_san.sprite.domain.rememberSpriteState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ninjarunner.composeapp.generated.resources.Res
import ninjarunner.composeapp.generated.resources.background
import ninjarunner.composeapp.generated.resources.background_dark
import ninjarunner.composeapp.generated.resources.light_icon
import ninjarunner.composeapp.generated.resources.night_icon
import ninjarunner.composeapp.generated.resources.run_ninja
import ninjarunner.composeapp.generated.resources.standing_ninja
import ninjarunner.composeapp.generated.resources.weapon
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.math.sqrt

const val NINJA_FRAME_WIDTH = 253
const val NINJA_FRAME_HEIGHT = 303
const val WEAPON_SPAWN_RATE = 150L
const val WEAPON_SIZE = 32f
const val TARGET_SPAWN_RATE = 1500L
const val TARGET_SIZE = 40f

@Composable
fun MainScreen() {

    val scope = rememberCoroutineScope()
    val audio = koinInject<AudioPlayer>()
    var game by remember { mutableStateOf(Game()) }
    val weapons = remember { mutableStateListOf<Weapon>() }
    val targets = remember { mutableStateListOf<Target>() }
    var moveDirection by remember { mutableStateOf(MoveDirection.None) }
    var screenHeight by remember { mutableStateOf(0) }
    var screenWidth by remember { mutableStateOf(0) }
    var isDarkMode by remember { mutableStateOf(false) }

    LaunchedEffect(game.score) {
        levels
            .filter { it.first.score == game.score }
            .takeIf { it.isNotEmpty() }
            ?.forEach { (_, nextLevel) ->
                game = game.copy(
                    setting = GameSettings(
                        ninjaSpeed = game.setting.ninjaSpeed + nextLevel.ninjaSpeed,
                        weaponSpeed = game.setting.weaponSpeed + nextLevel.weaponSpeed,
                        targetSpeed = game.setting.targetSpeed + nextLevel.targetSpeed,
                    )
                )
            }
    }

    val runningNinja = rememberSpriteState(
        totalFrames = 9,
        framesPerRow = 3
    )
    val standingNinja = rememberSpriteState(
        totalFrames = 1,
        framesPerRow = 1
    )
    val currentRunningFrame by runningNinja.currentFrame.collectAsState()
    val currentStandingFrame by standingNinja.currentFrame.collectAsState()
    val isRunning by runningNinja.isRunning.collectAsState()
    val runningNinjaSpec = remember {
        SpriteSpec(
            screenWidth = screenWidth.toFloat(),
            default = SpriteSheet(
                frameHeight = NINJA_FRAME_HEIGHT,
                frameWidth = NINJA_FRAME_WIDTH,
                image = Res.drawable.run_ninja
            )
        )
    }
    val standingNinjaSpec = remember {
        SpriteSpec(
            screenWidth = screenWidth.toFloat(),
            default = SpriteSheet(
                frameHeight = NINJA_FRAME_HEIGHT,
                frameWidth = NINJA_FRAME_WIDTH,
                image = Res.drawable.standing_ninja
            )
        )
    }
    val runningImage = runningNinjaSpec.imageBitmap
    val standingImage = standingNinjaSpec.imageBitmap
    val weaponImage = imageResource(Res.drawable.weapon)

    val ninjaOffsetX = remember(key1 = screenWidth) {
        Animatable(initialValue = ((screenWidth.toFloat()) / 2) - (NINJA_FRAME_WIDTH / 2))
    }

//    LaunchedEffect(Unit) {
//        game = game.copy(status = GameStatus.Started)
//    }

    LaunchedEffect(isRunning, game.status) {
        while (isRunning && game.status == GameStatus.Started) {
            delay(WEAPON_SPAWN_RATE)
            weapons.add(
                Weapon(
                    x = ninjaOffsetX.value + (NINJA_FRAME_WIDTH / 2),
                    y = screenHeight - (NINJA_FRAME_HEIGHT.toFloat() * 2),
                    radius = WEAPON_SIZE,
                    shootingSpeed = -game.setting.weaponSpeed
                )
            )
        }
    }

    LaunchedEffect(game.status) {
        while (game.status == GameStatus.Started) {
            delay(TARGET_SPAWN_RATE)
            val randomX = (0..screenWidth).random()
            val isEven = (randomX % 2 == 0)
            if(isEven) {
                targets.add(
                    MediumTarget(
                        x = randomX.toFloat(),
                        y = Animatable(0f),
                        radius = TARGET_SIZE,
                        fallingSpeed = game.setting.targetSpeed
                    )
                )
            } else if (randomX > screenWidth * 0.75) {
                targets.add(
                    StrongTarget(
                        x = randomX.toFloat(),
                        y = Animatable(0f),
                        radius = TARGET_SIZE,
                        fallingSpeed = game.setting.targetSpeed * 0.25f
                    )
                )
            } else {
                targets.add(
                    EasyTarget(
                        x = randomX.toFloat(),
                        y = Animatable(0f),
                        radius = TARGET_SIZE,
                        fallingSpeed = game.setting.targetSpeed
                    )
                )
            }
        }
    }

    LaunchedEffect(game.status) {
        while (game.status == GameStatus.Started) {
            withFrameMillis {
                targets.forEach { target ->
                    scope.launch(Dispatchers.Main) {
                        target.y.animateTo(
                            targetValue = target.y.value + target.fallingSpeed
                        )
                    }
                }
                weapons.forEach { weapon ->
                    weapon.y += weapon.shootingSpeed
                }

                //Check for collision
                val weaponIterator = weapons.iterator()
                while (weaponIterator.hasNext()) {
                    val weapon = weaponIterator.next()
                    val targetIterator = targets.listIterator()
                    while (targetIterator.hasNext()) {
                        val target = targetIterator.next()
                        if (isCollision(weapon, target)) {
                            audio.playSound(index = 0)
                            if(target is StrongTarget) {
                                if(target.lives > 0) {
                                    targetIterator.set(
                                        element = target.copy(
                                            radius = target.radius + 10,
                                            lives = target.lives - 1
                                        )
                                    )
                                    weaponIterator.remove()
                                } else {
                                    weaponIterator.remove()
                                    targetIterator.remove()
                                    game = game.copy(score =  game.score + 5)
                                }
                            } else if (target is MediumTarget) {
                                if(target.lives > 0) {
                                    targetIterator.set(
                                        element = target.copy(
                                            radius = target.radius + 10,
                                            lives = target.lives - 1
                                        )
                                    )
                                    weaponIterator.remove()
                                } else {
                                    weaponIterator.remove()
                                    targetIterator.remove()
                                    game = game.copy(score =  game.score + 5)
                                }
                            } else if(target is EasyTarget) {
                                    weaponIterator.remove()
                                    targetIterator.remove()
                                    game = game.copy(score =  game.score + 5)
                            }
                            break
                        }
                    }
                }

                //Check if game over
                val offScreenTarget = targets.firstOrNull {
                    it.y.value > screenHeight
                }
                if(offScreenTarget != null) {
                    game = game.copy(status = GameStatus.Over)
                    runningNinja.stop()
                    weapons.removeAll { true }
                    targets.removeAll { true }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .onGloballyPositioned {
                screenHeight = it.size.height
                screenWidth = it.size.width
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    detectMoveGesture(
                        gameStatus = game.status,
                        onLeft = {
                            moveDirection = MoveDirection.Left
                            runningNinja.start()
                            scope.launch(Dispatchers.Main) {
                                while (isRunning) {
                                    ninjaOffsetX.animateTo(
                                        targetValue = if((ninjaOffsetX.value - game.setting.ninjaSpeed) >= 0 - (NINJA_FRAME_WIDTH / 2))
                                            ninjaOffsetX.value - game.setting.ninjaSpeed else ninjaOffsetX.value,
                                        animationSpec = tween(durationMillis = 30)
                                    )
                                }
                            }
                        },
                        onRight = {
                            moveDirection = MoveDirection.Right
                            runningNinja.start()
                            scope.launch(Dispatchers.Main) {
                                while (isRunning) {
                                    ninjaOffsetX.animateTo(
                                        targetValue = if((ninjaOffsetX.value + game.setting.ninjaSpeed + NINJA_FRAME_WIDTH) <= screenWidth + (NINJA_FRAME_WIDTH / 2))
                                            ninjaOffsetX.value + game.setting.ninjaSpeed else ninjaOffsetX.value,
                                        animationSpec = tween(durationMillis = 30)
                                    )
                                }
                            }
                        },
                        onFingerLifted = {
                            moveDirection = MoveDirection.None
                            runningNinja.stop()
                        }
                    )
                }
            }
    ) {
        Image(
            painter = painterResource(if (isDarkMode) Res.drawable.background_dark else Res.drawable.background),
            modifier = Modifier.fillMaxSize(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            targets.forEach { target ->
                drawCircle(
                   color = target.color,
                    radius = target.radius,
                    center = _root_ide_package_.androidx.compose.ui.geometry.Offset(
                        x = target.x,
                        y = target.y.value
                    )
                )
            }

            weapons.forEach {
                val offset = IntOffset(it.x.toInt(), it.y.toInt())

                // First, draw blurred glow multiple times around the image
                if (isDarkMode) {
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            // Skip the center (that’s for the real image)
                            if (dx != 0 || dy != 0) {
                                drawImage(
                                    image = weaponImage,
                                    dstOffset = offset + IntOffset(dx * 2, dy * 2),
                                    colorFilter = ColorFilter.tint(Color.Yellow.copy(alpha = 0.6f)) // glow color
                                )
                            }
                        }
                    }
                }
                drawImage(
                    image = weaponImage,
                    dstOffset = offset
                )
            }
            drawSpriteView(
                spriteState = if(isRunning) runningNinja else standingNinja,
                spriteSpec = if(isRunning) runningNinjaSpec else standingNinjaSpec,
                currentFrame = if(isRunning) currentRunningFrame else currentStandingFrame,
                image = if(isRunning) runningImage else standingImage,
                spriteFlip = if (moveDirection == MoveDirection.Left) SpriteFlip.Horizontal else null,
                offset = IntOffset(
                    x = ninjaOffsetX.value.toInt(),
                    y = screenHeight - NINJA_FRAME_HEIGHT - (NINJA_FRAME_HEIGHT / 2)
                )
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 34.dp, vertical = 34.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Level: ${levels.firstOrNull { it.first.score >= game.score}?.first?.name ?: "MAX"}",
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Score: ${game.score}",
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Image(modifier = Modifier.clickable {
            isDarkMode = !isDarkMode
        },
            painter = painterResource(if(isDarkMode) Res.drawable.night_icon else Res.drawable.light_icon),
            contentDescription = null)
    }

    if(game.status == GameStatus.Idle) {
        Column(
            modifier = Modifier
                .clickable(enabled = false) {  }
                .background(color = Color.Black.copy(alpha = 0.7f))
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ready?",
                fontSize = MaterialTheme.typography.displayMedium.fontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    game = game.copy(status = GameStatus.Started)
                }
            ) {
                Text(text = "Start")
            }
        }
    }

    if(game.status == GameStatus.Over) {
        Column(
            modifier = Modifier
                .clickable(enabled = false) {  }
                .background(color = Color.Black.copy(alpha = 0.7f))
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Game Over!",
                fontSize = MaterialTheme.typography.displayMedium.fontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Your score: ${game.score}",
                fontSize = MaterialTheme.typography.displayMedium.fontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    game = game.copy(
                        status = GameStatus.Started,
                        score = 0,
                        setting = GameSettings()
                        )
                }
            ) {
                Text(text = "Play again")
            }
        }
    }
}

fun isCollision(weapon: Weapon, target: Target): Boolean {
    val dx = weapon.x - target.x
    val dy = weapon.y - target.y.value
    val distance = sqrt(dx * dx + dy * dy)
    return distance < (weapon.radius + target.radius)
}