package com.example.snaptrail.ui.gallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient

class AutoChallengeViewModel:ViewModel() {
    var displayList = mutableListOf<String>()
    var placesList = mutableListOf<Place>()
    val successfulPlaces = mutableSetOf<Int>()
    lateinit var placesClient: PlacesClient

    val imageHintMap = mutableMapOf<String,Pair<String?, Uri?>>()
    val latLngHintmap = mutableMapOf<String,LatLng>()
    fun storeHintAndImage(placeId:String,hint:String?,imageUri: Uri){
        imageHintMap[placeId] = Pair(hint,imageUri)
    }

    fun getHintAndImage(placeId:String): Pair<String?, Uri?>? {
        return imageHintMap[placeId]
    }

    fun storeLatLng(placeId: String, latLng: LatLng){
        latLngHintmap[placeId] = latLng
    }

    fun getLatLng(placeId: String, latLng: LatLng):LatLng?{
        return latLngHintmap[placeId]
    }
}