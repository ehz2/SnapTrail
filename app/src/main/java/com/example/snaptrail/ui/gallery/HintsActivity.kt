package com.example.snaptrail.ui.gallery

import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.snaptrail.R

class HintsActivity : AppCompatActivity() {

    private lateinit var autoChallengeViewModel: AutoChallengeViewModel

    private val TAG = "xd:"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hints)

        autoChallengeViewModel = ViewModelProvider(this).get(AutoChallengeViewModel::class.java)
        val extras = intent.extras
        val position = extras?.getInt("position")!!
        Log.e(TAG,"Position is ${position}")
        var placesList = autoChallengeViewModel.placesList

        Log.e(TAG,"Place name is ${placesList.size}")

    }
}