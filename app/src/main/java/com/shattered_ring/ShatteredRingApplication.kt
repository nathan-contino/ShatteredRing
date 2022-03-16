package com.shattered_ring

import android.app.Application
import com.shattered_ring.data.Game
import com.shattered_ring.data.Location
import com.shattered_ring.data.NPC
import com.shattered_ring.data.Quest
import io.realm.Realm
import io.realm.RealmConfiguration

const val TAG = "SHATTERED_RING"

class ShatteredRingApplication: Application() {
    lateinit var realm: Realm;
    override fun onCreate() {
        super.onCreate()
        realm = Realm.open(RealmConfiguration.with(setOf(Game::class, Quest::class, Location::class, NPC::class)))
    }
}

fun Application.getRealm(): Realm = (this as ShatteredRingApplication).realm
