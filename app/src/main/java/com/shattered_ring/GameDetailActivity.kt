package com.shattered_ring

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.lifecycleScope
import com.shattered_ring.data.Game
import com.shattered_ring.data.Location
import com.shattered_ring.data.Quest
import com.shattered_ring.ui.theme.*
import io.realm.Realm
import io.realm.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameDetailActivity : ComponentActivity() {
    private lateinit var repository: Realm
    private lateinit var gameState: Game
    private lateinit var gameID: String
    private lateinit var openDialog: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = this.application.getRealm()

        val extras = intent.extras
        if (extras != null) {
            gameID = extras.getString(Game::_id.name)!!
            Log.v(TAG, "Game id: $gameID")
            setUpGame()
        }

        setContent {
            Content()
        }
    }

    private fun setUpGame() {
        gameState = repository.query<Game>("_id == $gameID").find().first()
    }

    private fun goToNPCs() {
        lifecycleScope.launch {
            startActivity(
                Intent(this@GameDetailActivity, NPCActivity::class.java)
                    .apply {
                        putExtra(Game::_id.name, gameState._id.toString())
                    }
            )
        }
    }

    private fun deleteGame() {
        openDialog.value = true
    }

    @Composable
    fun DeleteDialog() {
        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
            },
            confirmButton = {
                TextButton(onClick = {
                    lifecycleScope.launch(Dispatchers.IO) {
                        repository.write {
                            this.findLatest(gameState)?.also { game ->
                                // delete all related objects, since they're orphaned without a game
                                this.delete(game.npcs)
                                this.delete(game.locations)
                                this.delete(game.quests)
                                this.delete(game)
                            }
                        }
                    }
                    openDialog.value = false
                    // close activity
                    finish()
                })
                { Text(text = "OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    openDialog.value = false
                })
                { Text(text = "Cancel") }
            },
            title = { Text(text = "Delete Game") },
            text = { Text(text = "Are you sure you want to delete this entire game?") }
        )
    }

    private fun showMessage(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@GameDetailActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("UnrememberedMutableState")
    @Composable
    @Preview
    fun Content() {
        val questsDialog = remember { mutableStateOf(false) }
        val locationsDialog =  remember { mutableStateOf(false) }
        val locationsState = mutableStateListOf<Location>()
        locationsState.addAll(gameState.locations)
        val questsState = mutableStateListOf<Quest>()
        questsState.addAll(gameState.quests)

        ShatteredRingTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                ConstraintLayout {
                    val (delete, npcs, quests, locations, title) = createRefs()
                    if (locationsDialog.value) {
                        LocationUtils().LocationsDialog(null, gameState, locationsState, locationsDialog, repository, lifecycleScope, this@GameDetailActivity::setUpGame)
                    }
                    if (questsDialog.value) {
                        QuestUtils().QuestsDialog(null, gameState, questsState, questsDialog, repository, lifecycleScope, this@GameDetailActivity::setUpGame)
                    }
                    Text(
                        text = gameState.name,
                        fontFamily = heroFontFamily,
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center,
                        color = ShatteredRingYellow,
                        modifier = Modifier.constrainAs(title) {
                            top.linkTo(parent.top)
                            centerHorizontallyTo(parent)
                            height = Dimension.value(100.dp)

                        }
                            .background(ShatteredRingOtherGreen)
                            .fillMaxWidth()
                    )
                    Button(
                        onClick = this@GameDetailActivity::goToNPCs,
                        modifier = Modifier
                            .constrainAs(npcs) {
                                top.linkTo(title.bottom, margin = 8.dp)
                            }.fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp)
                    ) {
                        ConstraintLayout {
                            val (title, met) = createRefs()
                            Text(text = "NPCs",
                                fontSize = 22.sp,
                                color = ShatteredRingYellow,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.constrainAs(title) {
                                    top.linkTo(parent.top, margin = 10.dp)
                                    start.linkTo(parent.start)
                                }.fillMaxWidth()
                            )
                            Text(
                                text = "Met: ${gameState.npcs.size}",
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp,
                                modifier = Modifier.constrainAs(met) {
                                    top.linkTo(title.bottom, margin = 20.dp)
                                }.fillMaxWidth(),
                            )
                        }
                    }
                    Button(
                        onClick = { locationsDialog.value = true },
                        modifier = Modifier
                            .constrainAs(locations) {
                                top.linkTo(quests.bottom, margin = 16.dp)
                            }.fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp)
                    ) {
                        ConstraintLayout {
                            val (title, discovered, cleared) = createRefs()
                            Text(text = "Locations",
                                fontSize = 22.sp,
                                textAlign = TextAlign.Left,
                                color = ShatteredRingYellow,
                                modifier = Modifier.constrainAs(title) {
                                    top.linkTo(parent.top, margin = 10.dp)
                                    start.linkTo(parent.start)
                                }.fillMaxWidth()
                            )
                            Text(
                                text = "Discovered: ${gameState.locations.size}",
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp,
                                modifier = Modifier.constrainAs(discovered) {
                                    top.linkTo(title.bottom, margin = 20.dp)
                                }.fillMaxWidth(),
                            )
                            Text(
                                text = "Cleared: ${gameState.locations.filterNot { location -> location.isCleared }.size}",
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp,
                                modifier = Modifier.constrainAs(cleared) {
                                    top.linkTo(discovered.bottom, margin = 20.dp)
                                }.fillMaxWidth(),
                            )
                        }
                    }
                    Button(
                        onClick = { questsDialog.value = true },
                        modifier = Modifier
                            .constrainAs(quests) {
                                top.linkTo(npcs.bottom, margin = 16.dp)
                            }.fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp)
                    ) {
                        ConstraintLayout {
                            val (title, accepted, completed) = createRefs()
                            Text(text = "Quests",
                                fontSize = 22.sp,
                                textAlign = TextAlign.Left,
                                color = ShatteredRingYellow,
                                modifier = Modifier.constrainAs(title) {
                                    top.linkTo(parent.top, margin = 10.dp)
                                    start.linkTo(parent.start)
                                }.fillMaxWidth()
                            )
                            Text(
                                text = "Accepted: ${gameState.quests.size}",
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp,
                                modifier = Modifier.constrainAs(accepted) {
                                    top.linkTo(title.bottom, margin = 20.dp)
                                }.fillMaxWidth()
                            )
                            Text(
                                text = "Completed: ${gameState.quests.filterNot { quest -> quest.isComplete }.size}",
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp,
                                modifier = Modifier.constrainAs(completed) {
                                    top.linkTo(accepted.bottom, margin = 20.dp)
                                }.fillMaxWidth()
                            )
                        }
                    }
                    Button(
                        onClick = this@GameDetailActivity::deleteGame,
                        colors = ButtonDefaults.textButtonColors(
                            backgroundColor = SpoOoOooooOoky,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .constrainAs(delete) {
                                bottom.linkTo(parent.bottom, margin = 16.dp)
                            }.fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp)
                    ) {
                        Text(text = "Delete Game")
                    }
                    openDialog = remember { mutableStateOf(false) }
                    if (openDialog.value) {
                        DeleteDialog()
                    }
                }
            }
        }
    }
}
