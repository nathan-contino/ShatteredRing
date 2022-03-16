package com.shattered_ring

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.lifecycleScope
import com.shattered_ring.data.Game
import com.shattered_ring.ui.theme.*
import io.realm.Realm
import io.realm.notifications.InitialResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameListActivity : ComponentActivity() {
    lateinit private var repository: Realm
    private val gamesState = mutableStateListOf<Game>()
    private lateinit var openDialog: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = this.application.getRealm()

        setUpGames()

        setContent {
            Content()
        }
    }

    // when we return to this activity from a game, need to refresh the list of games
    // in case that game was deleted
    override fun onResume() {
        setUpGames()
        super.onResume()
    }

    private fun setUpGames() {
        lifecycleScope.launch {
            repository.query(Game::class).asFlow()
                .collect { results ->
                    when (results) {
                        // print out initial results
                        is InitialResults<Game> -> {
                            gamesState.clear()
                            gamesState.addAll(results.list)
                        } else -> {
                            // do nothing on changes
                        }
                    }
                }
        }
    }

    private fun selectGame(game: Game) {
        lifecycleScope.launch {
            startActivity(
                Intent(this@GameListActivity, GameDetailActivity::class.java)
                    .apply {
                        putExtra(Game::_id.name, game._id.toString())
                    }
                )
            }
    }

    private fun onFabClick() {
        openDialog.value = true
    }

    private fun showMessage(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@GameListActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    fun CreateGameDialog() {
        var enteredName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
            },
            confirmButton = {
                TextButton(onClick = {
                    lifecycleScope.launch(Dispatchers.IO) {
                        // on a coroutine -- so write blocking, then refresh game list
                        repository.writeBlocking {
                            this.copyToRealm(Game().apply {
                                name = enteredName
                                isActive = true
                            })
                        }

                        // refresh the game list after creating new game
                        setUpGames()
                    }
                    // for maximum user responsiveness, close the dialog
                    openDialog.value = false
                })
                { Text(text = "OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    openDialog.value = false
                })
                { Text(text = "Cancel") }
            },
            title = { Text(text = "Create Game") },
            text = { TextField(
                value = enteredName,
                onValueChange = { enteredName = it },
                label = { Text("Enter game name") }
        )}
        )
    }

    @Composable
    @Preview
    fun Content() {
        openDialog = remember { mutableStateOf(false) }
        ShatteredRingTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                ConstraintLayout {
                    val (title, gameList, fab) = createRefs()
                    if (openDialog.value) {
                        CreateGameDialog()
                    }
                    Text(
                        text = "Shattered Ring",
                        fontFamily = heroFontFamily,
                        fontSize = 42.sp,
                        textAlign = TextAlign.Center,
                        color = ShatteredRingYellow,
                        modifier = Modifier.constrainAs(title) {
                            top.linkTo(parent.top)
                            centerHorizontallyTo(parent)
                            height = Dimension.value(100.dp)

                        }.background(ShatteredRingOtherGreen)
                            .fillMaxWidth()
                    )
                    GameList(
                        games = gamesState,
                        onGameSelected = { game ->
                            selectGame(game)
                        },
                        modifier = Modifier.constrainAs(gameList) {
                            top.linkTo(title.bottom, margin = 16.dp)
                            height = Dimension.wrapContent
                            width = Dimension.matchParent
                        }
                    )
                    FloatingActionButton(
                        onClick = this@GameListActivity::onFabClick,
                        backgroundColor = ShatteredRingGreen,
                        contentColor = ShatteredRingYellow,
                        modifier = Modifier.constrainAs(fab) {
                            end.linkTo(parent.end, margin = 16.dp)
                            bottom.linkTo(parent.bottom, margin = 16.dp)
                        }
                    ){
                        Icon(Icons.Filled.Add,"Create a new Game.")
                    }
                }
            }
        }
    }

    @Composable
    fun GameRow(
        game: Game,
        onClick: (Game) -> Unit
    ) {
        Card(
            modifier = Modifier
                .padding(
                    PaddingValues(
                        vertical = 4.dp
                    )
                )
                .fillMaxWidth()
                .clickable { onClick(game) }
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .padding(PaddingValues(16.dp))
                    .height(50.dp)
            ) {
                val (title, playerCount) = createRefs()

                Text(
                    text = game.name,
                    fontSize = 20.sp,
                    modifier = Modifier.constrainAs(title) {
                        start.linkTo(parent.start)
                    }
                )
                Text(
                    text = "${game.npcs.size} NPCs, ${game.quests.size} quests, ${game.locations.size} locations",
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.constrainAs(playerCount) {
                        end.linkTo(parent.end)

                    }.offset(y = 32.dp)
                )
            }
        }

    }

    @Composable
    fun GameList(
        games: List<Game>,
        onGameSelected: (Game) -> Unit,
        modifier: Modifier
    ) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 240.dp
            )
        ) {
            items(games) { game ->
                GameRow(game, onGameSelected)
            }
        }
    }
}
