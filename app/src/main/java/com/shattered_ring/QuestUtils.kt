package com.shattered_ring

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.shattered_ring.data.Game
import com.shattered_ring.data.NPC
import com.shattered_ring.data.Quest
import com.shattered_ring.ui.theme.SpoOoOooooOoky
import io.realm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuestUtils {
    lateinit var resetList: () -> Unit
    lateinit var repository: Realm
    lateinit var lifecycleScope: LifecycleCoroutineScope
    lateinit var game: Game
    lateinit var questsState: SnapshotStateList<Quest>

    fun refresh() {
        resetList() // reset in the caller on update
        // reset locally on update
        lifecycleScope.launch(Dispatchers.IO) {
            // write a noop to force a refresh
            repository.writeBlocking {
                findLatest(game)?.name = game.name
            }
        }

        game = repository.query<Game>("_id == ${game._id}").find().first()
        questsState.clear()
        questsState.addAll(game.quests)
    }



    @SuppressLint("UnrememberedMutableState")
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun QuestsDialog(npc: NPC?, game: Game, questsState: SnapshotStateList<Quest>, questsDialog: MutableState<Boolean>, repository: Realm, lifecycleScope: LifecycleCoroutineScope, resetList: () -> Unit) {
        this.resetList = resetList
        this.repository = repository
        this.game = game
        this.lifecycleScope = lifecycleScope
        this.questsState = questsState
        val createDialog =  remember { mutableStateOf(false) }
        Dialog(
            onDismissRequest = {
                questsDialog.value = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            content = {
                Card {
                    ConstraintLayout {
                        val (quests, button) = createRefs()
                        if (createDialog.value) {
                            QuestEdit(null, npc, createDialog)
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .constrainAs(quests) {
                                    top.linkTo(parent.top)
                                    centerHorizontallyTo(parent)
                                },
                            contentPadding = PaddingValues(
                                bottom = 60.dp
                            )
                        ) {
                            items(questsState) { quest ->
                                QuestSummary(quest, npc)
                            }
                        }
                        Button(
                            onClick = {
                                createDialog.value = true
                            },
                            content = {
                                Text(
                                    "Add Quest" + if (npc != null) " to Character" else ""
                                )
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .constrainAs(button) {
                                    bottom.linkTo(parent.bottom)
                                    centerHorizontallyTo(parent)
                                },
                        )
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun QuestEdit(quest: Quest?, npc: NPC?, dialogToggle: MutableState<Boolean>) {
        val enteredIsComplete = remember { mutableStateOf(quest?.isComplete ?: false) }
        val enteredName = remember { mutableStateOf(quest?.name ?: "") }
        val enteredNotes = remember { mutableStateOf(quest?.notes ?: "") }
        val reuseDialog =  remember { mutableStateOf(false) }
        var reuseQuest: Quest? = null
        var reused: Boolean? = null

        Dialog(
            onDismissRequest = {
                dialogToggle.value = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            content = {
                Surface(
                    modifier = Modifier
                        .padding(
                            PaddingValues(
                                vertical = 4.dp
                            )
                        )
                        .fillMaxSize()
                ) {
                    ConstraintLayout(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(PaddingValues(16.dp))
                    ) {
                        val (nameWidget, isCompleteWidget, notesWidget, saveButton, deleteButton) = createRefs()

                        TextField(
                            value = enteredName.value,
                            onValueChange = {
                                enteredName.value = it
                            },
                            label = { Text("Name") },
                            modifier = Modifier
                                .constrainAs(nameWidget) {
                                    top.linkTo(parent.top)
                                }
                                .fillMaxWidth()
                        )
                        ConstraintLayout(
                            modifier = Modifier
                                .constrainAs(isCompleteWidget) {
                                    top.linkTo(nameWidget.bottom, margin = 4.dp)
                                }
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            val (label, checkbox) = createRefs()
                            Text(text = "Completed", modifier = Modifier
                                .constrainAs(label) {
                                    start.linkTo(parent.start, margin = 16.dp)
                                }
                                .padding(top = 5.dp))
                            Checkbox(
                                checked = enteredIsComplete.value,
                                onCheckedChange = { it -> enteredIsComplete.value = it },
                                modifier = Modifier.constrainAs(checkbox) {
                                    end.linkTo(parent.end, margin = 16.dp)
                                }
                            )
                        }
                        TextField(
                            value = enteredNotes.value,
                            onValueChange = {
                                enteredNotes.value = it
                            },
                            label = { Text("Notes") },
                            modifier = Modifier
                                .constrainAs(notesWidget) {
                                    top.linkTo(isCompleteWidget.bottom, margin = 16.dp)
                                }
                                .height(200.dp)
                                .fillMaxWidth()
                        )
                        if (reuseDialog.value) {
                            AlertDialog(
                                onDismissRequest = {
                                    reuseDialog.value = false
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        enteredIsComplete.value = reuseQuest?.isComplete!!
                                        // concatenate notes so we don't lose them
                                        enteredNotes.value = reuseQuest?.notes + "\n" + enteredNotes.value
                                        // no need to copy over name -- that's what we used to find this
                                        reused = true
                                        reuseDialog.value = false
                                    })
                                    { Text(text = "OK") }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        reused = false
                                        reuseDialog.value = false
                                    })
                                    { Text(text = "Cancel") }
                                },
                                title = { Text(text = "Reuse Quest") },
                                text = { Text(text = "There's already a quest with this name. Would you like to reference the existing quest instead of creating a new one?") }
                            )
                        }
                        Button(
                            onClick = {
                                if (quest != null) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        repository.writeBlocking {
                                            // if we're updating an existing quest, perform the update
                                            val localQuest = findLatest(quest)!!
                                            localQuest.name = enteredName.value
                                            localQuest.isComplete = enteredIsComplete.value
                                            localQuest.notes = enteredNotes.value.trim()
                                        }
                                        // update the list of quests with the latest data and exit dialog
                                        resetList()
                                        dialogToggle.value = false
                                    }
                                } else {
                                    var didNotFindExistingQuest = false
                                    // if we haven't yet gone through the reuse flow...
                                    if (reused == null) {
                                        // see if there's a quest with the same name already
                                        val lookup: RealmResults<Quest> =
                                            repository.query<Quest>("name == '${enteredName.value}'").find()
                                        if (lookup.size != 0) {
                                            Log.v(TAG, "Asking to reuse a quest")
                                            reuseQuest = lookup[0]
                                            reuseDialog.value = true
                                        } else {
                                            didNotFindExistingQuest = true
                                        }
                                        // since we're launching another dialog, leave this dialog up for another flow
                                    }
                                    else if (reused == true) {
                                        Log.v(TAG, "User reused quest")
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            repository.writeBlocking {
                                                val localGame = findLatest(game)!!
                                                val localQuest =
                                                    findLatest(reuseQuest!!)!!

                                                // update the location to the values displayed on the page
                                                localQuest.notes = enteredNotes.value.trim()
                                                localQuest.isComplete = enteredIsComplete.value

                                                // TODO: Find way to add quests to NPCs that are still being created.
                                                // add the location to the NPC and Game, if it isn't already
                                                if (npc != null && npc.isManaged()) {
                                                    val localNPC = findLatest(npc)!!
                                                    val matchedNPCQuests = localNPC.quests.filter { quest -> quest._id == localQuest._id}
                                                    if (matchedNPCQuests.isEmpty()) {
                                                        localNPC.quests.add(localQuest)
                                                    }
                                                }

                                                val matchedGameQuests = localGame.quests.filter {quest -> quest._id == localQuest._id}
                                                if (matchedGameQuests.isEmpty()) {
                                                    localGame.quests.add(localQuest)
                                                }
                                            }

                                            // update the list of quests with the latest data and exit dialog
                                            refresh()
                                            dialogToggle.value = false
                                        }
                                    }
                                    if (didNotFindExistingQuest) {
                                        Log.v(TAG, "User created a new quest")
                                        // add a new quest if no reuse
                                        // on a coroutine -- so write blocking, then refresh list
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            repository.writeBlocking {
                                                // fetch latest npc and game data
                                                val localGame = findLatest(game)!!

                                                val newQuest = copyToRealm(Quest().apply {
                                                    name = enteredName.value
                                                    isComplete = enteredIsComplete.value
                                                    notes = enteredNotes.value.trim()
                                                })
                                                if (npc != null && npc.isManaged()) {
                                                    val localNPC = findLatest(npc)
                                                    localNPC?.quests?.add(newQuest)
                                                }
                                                localGame.quests.add(newQuest)

                                            }

                                            Log.v(TAG, "Created a new quest. Resetting NPC quest list:")
                                            // update the list of quests with the latest data and exit dialog
                                            refresh()
                                            dialogToggle.value = false
                                        }
                                    }
                                }
                            },
                            content = {
                                Text(
                                    if (reused == true) "Update Existing Quest" else if (quest == null) "Create Quest" else "Update Quest"
                                )
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .constrainAs(saveButton) {
                                    bottom.linkTo(parent.bottom)
                                    end.linkTo(parent.end)
                                },
                        )
                        if (quest != null) {
                            Button(
                                onClick = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        // on a coroutine -- so write blocking, then refresh list
                                        repository.writeBlocking {
                                            var ret = findLatest(quest).also {
                                                delete(it!!)
                                            }
                                        }

                                        // update the list of quests with the latest data
                                        refresh()
                                        dialogToggle.value = false
                                    }
                                },
                                content = {
                                    Text(
                                        "Delete Quest"
                                    )
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    backgroundColor = SpoOoOooooOoky,
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier
                                    .height(50.dp)
                                    .constrainAs(deleteButton) {
                                        start.linkTo(parent.start)
                                        bottom.linkTo(parent.bottom)
                                    },
                            )
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun QuestSummary(quest: Quest, npc: NPC?) {
        val openDialog = remember { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .padding(
                    PaddingValues(
                        vertical = 10.dp,
                        horizontal = 10.dp
                    )
                )
                .height(80.dp)
                .fillMaxSize()
                .clickable { openDialog.value = true }
        ) {
            ConstraintLayout {
                val (name, complete, notes) = createRefs()
                Text(text=quest.name, fontSize = 20.sp, modifier=Modifier.padding(top=10.dp, start=10.dp)
                    .constrainAs(name) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                    } )
                if (quest.isComplete) {
                    Text(text="Complete", fontSize = 14.sp, modifier=Modifier.padding(start=20.dp)
                        .constrainAs(complete) {
                            start.linkTo(parent.start)
                            top.linkTo(name.bottom)
                        } )
                } else {
                    Text(text="Not Complete", fontSize = 14.sp, modifier=Modifier.padding(start=20.dp)
                        .constrainAs(complete) {
                            start.linkTo(parent.start)
                            top.linkTo(name.bottom)
                        } )
                }
                Text(text=quest.notes, fontSize = 14.sp, modifier=Modifier.padding(start=20.dp)
                    .constrainAs(notes) {
                        start.linkTo(parent.start)
                        top.linkTo(complete.bottom)
                    } )
            }
            if (openDialog.value) {
                QuestEdit(quest, npc, openDialog)
            }
        }
    }
}