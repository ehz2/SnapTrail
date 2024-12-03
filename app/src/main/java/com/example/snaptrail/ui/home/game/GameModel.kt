package com.example.snaptrail.ui.home.game

import com.example.snaptrail.ui.home.create.locations.LocationData

data class GameModel(
    var gameId: String = "",
    var gameStatus: String = "CREATED",
    var hostId: String = "",
    var trailName: String = "",
    var maxPlayers: Int? = null,
    var comments: String = "",
    var locations: List<LocationData> = listOf(),
    var players: MutableList<String> = mutableListOf(),
    var playerNames: MutableMap<String, String> = mutableMapOf(),
    var playerProgress: MutableMap<String, Int> = mutableMapOf(),
    var playerPoints: MutableMap<String, Int> = mutableMapOf(),
    var completedPlayers: MutableList<String> = mutableListOf(),
    var isGameEnded: Boolean = false
)
