package com.shattered_ring.data

import io.realm.*
import io.realm.annotations.PrimaryKey
import io.realm.notifications.ResultsChange
import kotlinx.coroutines.flow.Flow
import java.util.*

class Game: RealmObject {
    @PrimaryKey
    var _id: Long = Random().nextLong()
    var name: String = ""
    var isActive: Boolean = false
    var npcs: RealmList<NPC> = realmListOf()
    var locations: RealmList<Location> = realmListOf()
    var quests: RealmList<Quest> = realmListOf()
}

class Location : RealmObject {
    @PrimaryKey
    var _id: Long = Random().nextLong()
    var name: String = ""
    var isCleared: Boolean = false
    var hasSmithAnvil: Boolean = false
    var hasMerchant: Boolean = false
    var notes: String = ""
    // inverse relationship to NPCs
    // inverse relationship to game
}
fun NPCsForLocation(game: Game, location: Location, realm: Realm):
    Flow<ResultsChange<NPC>> {
    return realm.query<NPC>("location._id == ${location._id}").asFlow()
}

class NPC: RealmObject {
    @PrimaryKey
    var _id: Long = Random().nextLong()
    var name: String = ""
    var isMerchant: Boolean = false
    var locations: RealmList<Location> = realmListOf()
    var quests: RealmList<Quest> = realmListOf()
    var notes: String = ""
    //inverse relationship to game
}

class Quest: RealmObject {
    @PrimaryKey
    var _id: Long = Random().nextLong()
    var name: String = ""
    var isComplete: Boolean = false
    var notes: String = ""
    // inverse relationship to NPCs giving quests
    // inverse relationship to game
}
fun NPCsForQuest(game: Game, quest: Quest, realm: Realm):
        Flow<ResultsChange<NPC>> {
    return realm.query<NPC>("quest._id == ${quest._id}").asFlow()
}