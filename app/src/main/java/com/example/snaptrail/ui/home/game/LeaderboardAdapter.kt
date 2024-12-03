package com.example.snaptrail.ui.home.game

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.snaptrail.R

class LeaderboardAdapter(
    context: Context,
    private val players: List<PlayerLeaderboardItem>
) : ArrayAdapter<PlayerLeaderboardItem>(context, 0, players) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_leaderboard, parent, false)

        val playerName = view.findViewById<TextView>(R.id.tvPlayerName)
        val playerPoints = view.findViewById<TextView>(R.id.tvPlayerPoints)

        val player = players[position]
        playerName.text = player.name
        playerPoints.text = "${player.points} pts"

        return view
    }
}