package com.example.snaptrail.ui.home.game

data class GameModel(
    var gameId: String = "",
    var gameStatus: String = "CREATED",
    var hostId: String = "",
    var players: MutableList<String> = mutableListOf(),
    var playerNames: MutableMap<String, String> = mutableMapOf()
)
