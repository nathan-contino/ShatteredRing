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
import com.shattered_ring.data.Location
import com.shattered_ring.data.NPC
import com.shattered_ring.data.Quest
import com.shattered_ring.ui.theme.SpoOoOooooOoky
import io.realm.Realm
import io.realm.RealmResults
import io.realm.isManaged
import io.realm.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationUtils {
    lateinit var resetList: () -> Unit
    lateinit var repository: Realm
    lateinit var lifecycleScope: LifecycleCoroutineScope
    lateinit var game: Game
    lateinit var locationsState: SnapshotStateList<Location>

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
        locationsState.clear()
        locationsState.addAll(game.locations)
    }

    @SuppressLint("UnrememberedMutableState")
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun LocationsDialog(npc: NPC?, game: Game, locationsState: SnapshotStateList<Location>, locationDialog: MutableState<Boolean>, repository: Realm, lifecycleScope: LifecycleCoroutineScope, resetList: () -> Unit) {
        this.resetList = resetList
        this.repository = repository
        this.game = game
        this.lifecycleScope = lifecycleScope
        this.locationsState = locationsState

        val createDialog =  remember { mutableStateOf(false) }
        Dialog(
            onDismissRequest = {
                locationDialog.value = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            content = {
                Card {
                    ConstraintLayout {
                        val (locations, button) = createRefs()
                        if (createDialog.value) {
                            LocationEdit(null, npc, createDialog)
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .constrainAs(locations) {
                                    top.linkTo(parent.top)
                                    centerHorizontallyTo(parent)
                                },
                            contentPadding = PaddingValues(
                                bottom = 60.dp
                            )
                        ) {
                            items(locationsState) { location ->
                                LocationSummary(location, npc)
                            }
                        }
                        Button(
                            onClick = {
                                createDialog.value = true
                            },
                            content = {
                                Text(
                                    "Add Location" + if (npc != null) " to Character" else ""
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
    fun LocationEdit(location: Location?, npc: NPC?, dialogToggle: MutableState<Boolean>) {
        val enteredCleared = remember { mutableStateOf(location?.isCleared ?: false) }
        val enteredName = remember { mutableStateOf(location?.name ?: "") }
        val enteredHasSmithAnvil = remember { mutableStateOf(location?.hasSmithAnvil ?: false) }
        val enteredHasMerchant = remember { mutableStateOf(location?.hasMerchant ?: false) }
        val enteredNotes = remember { mutableStateOf(location?.notes ?: "") }
        val reuseDialog =  remember { mutableStateOf(false) }
        var reuseLocation: Location? = null
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
                        val (nameWidget, clearedWidget, hasSmithAnvilWidget, hasMerchantWidget, notesWidget, saveButton, deleteButton) = createRefs()

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
                                .constrainAs(clearedWidget) {
                                    top.linkTo(nameWidget.bottom, margin = 4.dp)
                                }
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            val (label, checkbox) = createRefs()
                            Text(text = "Cleared", modifier = Modifier
                                .constrainAs(label) {
                                    start.linkTo(parent.start, margin = 16.dp)
                                }
                                .padding(top = 5.dp))
                            Checkbox(
                                checked = enteredCleared.value,
                                onCheckedChange = { it -> enteredCleared.value = it },
                                modifier = Modifier.constrainAs(checkbox) {
                                    end.linkTo(parent.end, margin = 16.dp)
                                }
                            )

                        }
                        ConstraintLayout(
                            modifier = Modifier
                                .constrainAs(hasSmithAnvilWidget) {
                                    top.linkTo(clearedWidget.bottom, margin = 4.dp)
                                }
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            val (label, checkbox) = createRefs()
                            Text(
                                text = "Has Smith Anvil",
                                modifier = Modifier
                                    .constrainAs(label) {
                                        start.linkTo(parent.start, margin = 16.dp)
                                    }
                                    .padding(top = 5.dp)
                            )
                            Checkbox(
                                checked = enteredHasSmithAnvil.value,
                                onCheckedChange = { it -> enteredHasSmithAnvil.value = it },
                                modifier = Modifier.constrainAs(checkbox) {
                                    end.linkTo(parent.end, margin = 16.dp)
                                }
                            )
                        }
                        ConstraintLayout(
                            modifier = Modifier
                                .constrainAs(hasMerchantWidget) {
                                    top.linkTo(hasSmithAnvilWidget.bottom, margin = 4.dp)
                                }
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            val (label, checkbox) = createRefs()
                            Text(
                                text = "Has Merchant: ",
                                modifier = Modifier
                                    .constrainAs(label) {
                                        start.linkTo(parent.start, margin = 16.dp)
                                    }
                                    .padding(top = 5.dp)
                            )
                            Checkbox(
                                checked = enteredHasMerchant.value,
                                onCheckedChange = { it -> enteredHasMerchant.value = it },
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
                                    top.linkTo(hasMerchantWidget.bottom, margin = 16.dp)
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
                                        enteredCleared.value = reuseLocation?.isCleared!!
                                        enteredHasMerchant.value = reuseLocation?.hasMerchant!!
                                        enteredHasSmithAnvil.value = reuseLocation?.hasSmithAnvil!!
                                        // concatenate notes so we don't lose them
                                        enteredNotes.value = reuseLocation?.notes + "\n" + enteredNotes.value
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
                                title = { Text(text = "Reuse Location") },
                                text = { Text(text = "There's already a location with this name. Would you like to reference the existing location instead of creating a new one?") }
                            )
                        }
                        Button(
                            onClick = {
                                if (location != null) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        repository.writeBlocking {
                                            // if we're updating an existing location, perform the update
                                            val localLocation = findLatest(location)!!
                                            localLocation.name = enteredName.value
                                            localLocation.isCleared = enteredCleared.value
                                            localLocation.hasSmithAnvil =
                                                enteredHasSmithAnvil.value
                                            localLocation.hasMerchant = enteredHasMerchant.value
                                            localLocation.notes = enteredNotes.value.trim()
                                        }
                                        // update the list of locations with the latest data and exit dialog
                                        refresh()
                                        dialogToggle.value = false
                                    }
                                } else {
                                    var didNotFindExistingLocation = false
                                    // if we haven't yet gone through the reuse flow...
                                    if (reused == null) {
                                        // see if there's a location with the same name already
                                        val lookup: RealmResults<Location> =
                                            repository.query<Location>("name == '${enteredName.value}'").find()
                                        if (lookup.size != 0) {
                                            Log.v(TAG, "Asking to reuse a location")
                                            reuseLocation = lookup[0]
                                            reuseDialog.value = true
                                        } else {
                                            didNotFindExistingLocation = true
                                        }
                                        // since we're launching another dialog, leave this dialog up for another flow
                                    }
                                    else if (reused == true) {
                                        Log.v(TAG, "User reused location")
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            repository.writeBlocking {
                                                val localGame = findLatest(game)!!
                                                val localLocation =
                                                    findLatest(reuseLocation!!)!!

                                                // update the location to the values displayed on the page
                                                localLocation.hasMerchant = enteredHasMerchant.value
                                                localLocation.hasSmithAnvil = enteredHasSmithAnvil.value
                                                localLocation.notes = enteredNotes.value.trim()
                                                localLocation.isCleared = enteredCleared.value

                                                // add the location to the NPC and Game, if it isn't already
                                                if (npc != null && npc.isManaged()) {
                                                    val localNPC = findLatest(npc)!!
                                                    val matchedNPCLocations = localNPC.locations.filter { location -> location._id == localLocation._id}
                                                    if (matchedNPCLocations.isEmpty()) {
                                                        localNPC.locations.add(localLocation)
                                                    }
                                                }

                                                val matchedGameLocations = localGame.locations.filter {location -> location._id == localLocation._id}
                                                if (matchedGameLocations.isEmpty()) {
                                                    localGame.locations.add(localLocation)
                                                }
                                            }

                                            // update the list of locations with the latest data and exit dialog
                                            refresh()
                                            dialogToggle.value = false
                                        }
                                    }
                                    if (didNotFindExistingLocation) {
                                        Log.v(TAG, "User created a new location")
                                        // add a new location if no reuse
                                        // on a coroutine -- so write blocking, then refresh list
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            repository.writeBlocking {
                                                // fetch latest npc and game data
                                                val localGame = findLatest(game)!!

                                                val newLocation = copyToRealm(Location().apply {
                                                    name = enteredName.value
                                                    isCleared = enteredCleared.value
                                                    hasSmithAnvil = enteredHasSmithAnvil.value
                                                    hasMerchant = enteredHasMerchant.value
                                                    notes = enteredNotes.value.trim()
                                                })
                                                // TODO: Find a way to add locations to NPCs later
                                                if (npc != null && npc.isManaged()) {
                                                    val localNPC = findLatest(npc)!!
                                                    localNPC.locations.add(newLocation)
                                                }
                                                localGame.locations.add(newLocation)
                                            }

                                            Log.v(TAG, "Created a new location. Resetting NPC location list:")
                                            // update the list of locations with the latest data and exit dialog
                                            refresh()
                                            dialogToggle.value = false
                                        }
                                    }
                                }
                            },
                            content = {
                                Text(
                                    if (reused == true) "Update Existing Location" else if (location == null) "Create Location" else "Update Location"
                                )
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .constrainAs(saveButton) {
                                    bottom.linkTo(parent.bottom)
                                    end.linkTo(parent.end)
                                },
                        )
                        if (location != null) {
                            Button(
                                onClick = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        // on a coroutine -- so write blocking, then refresh list
                                        repository.writeBlocking {
                                            var ret = findLatest(location).also {
                                                delete(it!!)
                                            }
                                        }

                                        // update the list of locations with the latest data
                                        refresh()
                                        dialogToggle.value = false
                                    }
                                },
                                content = {
                                    Text(
                                        "Delete Location"
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
    fun LocationSummary(location: Location, npc: NPC?) {
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
                val (title, cleared, smith, merchant, notes) = createRefs()
                var lastBeforeNotes = cleared
                Text(text=location.name, fontSize = 20.sp, modifier=Modifier.padding(top=10.dp, start=10.dp)
                    .constrainAs(title) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                    } )
                if (location.isCleared) {
                    Text(text="Cleared", fontSize = 14.sp, modifier=Modifier.padding(start=20.dp)
                        .constrainAs(cleared) {
                            start.linkTo(parent.start)
                            top.linkTo(title.bottom)
                        } )
                } else {
                    Text(text="Not Cleared", fontSize = 14.sp, modifier=Modifier.padding(start=20.dp)
                        .constrainAs(cleared) {
                            start.linkTo(parent.start)
                            top.linkTo(title.bottom)
                        } )
                }
                if (location.hasSmithAnvil) {
                    lastBeforeNotes = smith
                    Text(text="Has Smith", fontSize = 14.sp, modifier=Modifier.padding(start=20.dp)
                        .constrainAs(smith) {
                            start.linkTo(parent.start)
                            top.linkTo(cleared.bottom)
                        })
                }
                if (location.hasMerchant) {
                    lastBeforeNotes = merchant
                    Text(text="Has Merchant", fontSize = 14.sp, modifier=Modifier.padding(start=20.dp)
                        .constrainAs(merchant) {
                            start.linkTo(parent.start)
                            top.linkTo(smith.bottom)
                        } )
                }
                Text(text=location.notes, fontSize = 14.sp, modifier=Modifier.padding(start=20.dp)
                    .constrainAs(notes) {
                        top.linkTo(lastBeforeNotes.bottom)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    } )
            }
            if (openDialog.value) {
                LocationEdit(location, npc, openDialog)
            }
        }
    }
}