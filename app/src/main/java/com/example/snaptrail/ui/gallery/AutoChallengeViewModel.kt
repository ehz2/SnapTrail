package com.example.snaptrail.ui.gallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient

class AutoChallengeViewModel:ViewModel() {
    var displayList = mutableListOf<String>()
    var placesList = mutableListOf<Place>()
    lateinit var placesClient: PlacesClient

    val imageHintMap = mutableMapOf<String,Pair<String?, Uri?>>()

    fun storeHintAndImage(placeId:String,hint:String?,imageUri: Uri){
        imageHintMap[placeId] = Pair(hint,imageUri)
    }

    fun getHintAndImage(placeId:String): Pair<String?, Uri?>? {
        return imageHintMap[placeId]
    }
}