package com.example.snaptrail.ui.home.game

import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// CURRENTLY NOT BEING USED, KEEPING IT HERE BECAUSE IT MIGHT BE USED LATER
object GameData {
    private val _gameModel = MutableLiveData<GameModel?>()
    val gameModel: MutableLiveData<GameModel?> get() = _gameModel

    fun observeGame(gameId: String) {
        Firebase.firestore.collection("games")
            .document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val model = snapshot.toObject(GameModel::class.java)
                    _gameModel.postValue(model)
                }
            }
    }
}
