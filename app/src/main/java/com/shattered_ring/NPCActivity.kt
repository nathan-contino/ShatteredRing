package com.shattered_ring

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.lifecycleScope
import com.shattered_ring.data.Game
import com.shattered_ring.data.Location
import com.shattered_ring.data.NPC
import com.shattered_ring.data.Quest
import com.shattered_ring.ui.theme.*
import io.realm.Realm
import io.realm.RealmResults
import io.realm.query
import io.realm.realmListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NPCActivity : ComponentActivity() {
    private lateinit var repository: Realm
    private lateinit var game: Game
    private val npcsState = mutableStateListOf<NPC>()
    private lateinit var createDialog: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = this.application.getRealm()

        val extras = intent.extras
        if (extras != null) {
            val gameId = extras.getString(Game::_id.name)
            Log.v(TAG, "Game id: $gameId")
            game = repository.query<Game>("_id == $gameId").find().first()
        }

        setUpNPCs()

        setContent {
            Content()
        }
    }

    private fun setUpNPCs() {
        lifecycleScope.launch {
            // write a noop to force a refresh
            repository.writeBlocking {
                findLatest(game)?.name = game.name
            }
            game = repository.query<Game>("_id == ${game._id}").find().first()
            npcsState.clear()
            npcsState.addAll(game.npcs)
        }
    }

    private fun selectNPC(npc: NPC) {
        showMessage(npc.name)
    }

    private fun deleteNPC(npc: NPC) {
        showMessage(npc.name)
    }

    private fun onFabClick() {
        createDialog.value = true
    }

    private fun showMessage(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@NPCActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("UnrememberedMutableState")
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun NPCDialog(npc: NPC?, updateDialog: MutableState<Boolean>?) {
        val questsDialog = remember { mutableStateOf(false) }
        val locationsDialog =  remember { mutableStateOf(false) }
        val tempNPCNotReference = NPC()
        val tempNPC by remember { mutableStateOf(tempNPCNotReference) }
        Dialog(
            onDismissRequest = {
                if (npc != null) {
                    updateDialog!!.value = false
                } else {
                    createDialog.value = false
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            content = {
                Card(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ConstraintLayout {
                        val (nameWidget, createButton, checkbox, checkboxLabel, notesWidget, deleteButton, locationsButton, questsButton) = createRefs()
                        var enteredName: String by remember { mutableStateOf(npc?.name ?: "") }
                        var enteredIsMerchant: Boolean by remember { mutableStateOf(npc?.isMerchant ?: false) }
                        var enteredNotes: String by remember { mutableStateOf(npc?.notes ?: "") }
                        val locationsState = mutableStateListOf<Location>()
                        locationsState.addAll(npc?.locations ?: realmListOf())
                        val questsState = mutableStateListOf<Quest>()
                        questsState.addAll(npc?.quests ?: realmListOf())

                        if (locationsDialog.value) {
                            LocationUtils().LocationsDialog(npc ?: tempNPC, game, locationsState, locationsDialog, repository, lifecycleScope, this@NPCActivity::setUpNPCs)
                        }
                        if (questsDialog.value) {
                            QuestUtils().QuestsDialog(npc ?: tempNPC, game, questsState, questsDialog, repository, lifecycleScope, this@NPCActivity::setUpNPCs)
                        }
                        TextField(
                            value = enteredName,
                            onValueChange = { enteredName = it },
                            label = { Text("Name") },
                            modifier = Modifier
                                .constrainAs(nameWidget) {
                                    top.linkTo(parent.top, margin = 16.dp)
                                    centerHorizontallyTo(parent)
                                }
                                .padding(top = 16.dp, bottom = 16.dp)
                                .fillMaxWidth()
                        )
                        Text(text = "Is Merchant: ", modifier = Modifier
                            .constrainAs(checkboxLabel) {
                                start.linkTo(parent.start, margin = 16.dp)
                            }
                            .padding(top = 130.dp))
                        Checkbox(
                            checked = enteredIsMerchant,
                            onCheckedChange = { it -> enteredIsMerchant = it },
                            modifier = Modifier
                                .constrainAs(checkbox) {
                                    end.linkTo(parent.end, margin = 16.dp)
                                }
                                .padding(top = 120.dp))
                        TextField(
                            value = enteredNotes,
                            onValueChange = {
                                enteredNotes = it
                            },
                            label = { Text("Notes") },
                            modifier = Modifier
                                .constrainAs(notesWidget) {
                                    top.linkTo(parent.top, margin = 16.dp)
                                    centerHorizontallyTo(parent)
                                }
                                .padding(top = 170.dp, bottom = 16.dp)
                                .height(200.dp)
                                .fillMaxWidth()
                        )
                        if (npc != null) {
                            Button(
                                onClick = {
                                    locationsDialog.value = true
                                },
                                content = {
                                    Text("Locations")
                                },
                                modifier = Modifier
                                    .constrainAs(locationsButton) {
                                        top.linkTo(notesWidget.bottom, margin = 16.dp)
                                        centerHorizontallyTo(parent)
                                    }
                                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                    .fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    questsDialog.value = true
                                },
                                content = {
                                    Text("Quests")
                                },
                                modifier = Modifier
                                    .constrainAs(questsButton) {
                                        top.linkTo(locationsButton.bottom, margin = 16.dp)
                                        centerHorizontallyTo(parent)
                                    }
                                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                                    .fillMaxWidth()
                            )
                        }
                        Button(
                            onClick = {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    // on a coroutine -- so write blocking, then refresh game list
                                    repository.writeBlocking {
                                        var latestGame = findLatest(game)
                                        if (npc != null) {
                                            var localNPC = findLatest(npc)!!
                                            localNPC.name = enteredName
                                            localNPC.isMerchant = enteredIsMerchant
                                            localNPC.notes = enteredNotes
                                        } else {
                                            latestGame?.npcs?.add(copyToRealm(NPC().apply {
                                                name = enteredName
                                                isMerchant = enteredIsMerchant
                                                notes = enteredNotes
                                            }))
                                        }
                                    }

                                    // refresh the game list after creating new game
                                    setUpNPCs()
                                }
                                if (npc != null) {
                                    updateDialog!!.value = false
                                } else {
                                    createDialog.value = false
                                }
                            },
                            content = {
                                Text(
                                    if(npc != null) "Update NPC" else "Create NPC"
                                )
                            },
                            modifier = Modifier
                                .constrainAs(createButton) {
                                    bottom.linkTo(parent.bottom, margin = 16.dp)
                                    end.linkTo(parent.end, margin = 16.dp)
                                }
                                .padding(top = 16.dp, bottom = 16.dp)
                        )
                        if (npc != null) {
                            Button(
                                onClick = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        // on a coroutine -- so write blocking, then refresh game list
                                        repository.writeBlocking {
                                            // note: findLatest on its own in a write transaction
                                            // throws an error because Kotlin tries to be clever
                                            // and return one-liners as values without a return
                                            val npc = findLatest(npc)?.also {
                                                delete(it)
                                            }
                                        }

                                        // refresh the game list after deleting the NPC
                                        setUpNPCs()

                                        if (npc != null) {
                                            updateDialog!!.value = false
                                        } else {
                                            createDialog.value = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    backgroundColor = SpoOoOooooOoky,
                                    contentColor = Color.White,
                                ),
                                content = {
                                    Text(
                                        "Delete NPC"
                                    )
                                },
                                modifier = Modifier
                                    .constrainAs(deleteButton) {
                                        bottom.linkTo(parent.bottom, margin = 16.dp)
                                        start.linkTo(parent.start, margin = 16.dp)
                                    }
                                    .padding(top = 16.dp, bottom = 16.dp)
                            )
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun RowScope.Cell(
        text: String,
        weight: Float,
        modifier: Modifier = Modifier
    ) {
        Text(
            text = text,
            modifier = modifier.apply { this.weight(weight) }
        )
    }

    @Composable
    fun Content() {
        createDialog = remember { mutableStateOf(false) }
        ShatteredRingTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                    val (npcs, title, fab) = createRefs()

                    Text(
                        text = "Non Player Characters",
                        fontFamily = heroFontFamily,
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center,
                        color = ShatteredRingYellow,
                        modifier = Modifier
                            .constrainAs(title) {
                                top.linkTo(parent.top)
                                centerHorizontallyTo(parent)
                                height = Dimension.value(60.dp)

                            }
                            .background(ShatteredRingOtherGreen)
                            .fillMaxWidth()
                    )
                    NPCsList(
                        npcs = this@NPCActivity.npcsState,
                        onNPCSelected = { npc ->
                            selectNPC(npc)
                        },
                        modifier = Modifier
                            .constrainAs(npcs) {
                                top.linkTo(title.bottom)
                            }
                            .fillMaxWidth()
                    )
                    if (createDialog.value) {
                        NPCDialog(null, null)
                    }
                    FloatingActionButton(
                        onClick = this@NPCActivity::onFabClick,
                        backgroundColor = ShatteredRingGreen,
                        contentColor = ShatteredRingYellow,
                        modifier = Modifier.constrainAs(fab) {
                            end.linkTo(parent.end, margin = 16.dp)
                            bottom.linkTo(parent.bottom, margin = 16.dp)
                        }
                    ){
                        Icon(Icons.Filled.Add,"Create a new NPC.")
                    }
                }
            }
        }
    }

    @Composable
    fun NPCRow(npc: NPC,
               onClick: (NPC) -> Unit) {
        var updateDialog: MutableState<Boolean> = remember { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .padding(
                    PaddingValues(
                        vertical = 4.dp
                    )
                )
                .fillMaxWidth()
                .clickable {
                    updateDialog.value = true
                }
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .padding(PaddingValues(16.dp))
                    .height(50.dp)
            ) {
                val (name, merchant, locations, quests) = createRefs()

                Text(
                    text = npc.name,
                    fontSize = 17.sp
                )
                Text(
                    text = if (npc.isMerchant) "Is Merchant" else "Not a Merchant",
                    modifier = Modifier
                        .constrainAs(merchant) {
                            start.linkTo(parent.start, margin = 4.dp)
                            bottom.linkTo(parent.bottom, margin = 4.dp)
                        }
                        .padding(top = 20.dp)
                )
                Text(
                    text = "Locations: ${npc.locations.size}",
                    modifier = Modifier.constrainAs(locations) {
                        end.linkTo(parent.end)
                        top.linkTo(parent.top, margin = 4.dp)
                    }
                )
                Text(
                    text = "Quests: ${npc.quests.size}",
                    modifier = Modifier.constrainAs(quests) {
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom, margin = 4.dp)
                    }
                )
            }
        }

        if (updateDialog.value) {
            NPCDialog(npc, updateDialog)
        }
    }

    @Composable
    fun NPCsList(npcs: List<NPC>,
                 onNPCSelected: (NPC) -> Unit,
                 modifier: Modifier = Modifier) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 120.dp
            )
        ) {
            items(npcs) { npc ->
                NPCRow(npc, onNPCSelected)
            }
        }
    }
}
