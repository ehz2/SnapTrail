package com.example.snaptrail.ui.gallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.example.snaptrail.BuildConfig
import com.example.snaptrail.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import java.util.Arrays


class AutoChallengeFragment : Fragment() {

    private lateinit var placesClient: PlacesClient
    private lateinit var listPlaces: List<Place>
    private  var displayList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("xd:Hello from challenge")

        val apiKey = BuildConfig.MAPS_API_KEY
        println("xd:api key is ${apiKey}")
        // Log an error if apiKey is not set.
        if (apiKey.isEmpty() || apiKey == "DEFAULT_API_KEY") {
            Log.e("Places test", "No api key")
            requireActivity().finish()
            return
        }

        //Initialize the SDK
        Places.initializeWithNewPlacesApiEnabled(requireContext(),apiKey)
        // Create a new Places API client
        placesClient = Places.createClient(requireContext())

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_auto_challenge, container, false)
        //Get the user selected difficulty
        val difficulty = arguments?.getString("DifficultyKey")
        val placesListView = view.findViewById<ListView>(R.id.placesListView)
        //Response list
        var placeFields: List<Place.Field> = Arrays.asList(Place.Field.ID,Place.Field.NAME,Place.Field.ADDRESS)
        //Radius
        val center = LatLng(49.2253767, -123.0605233)
        val circle = CircularBounds.newInstance(center,  1000.0)

        // Define a list of types to include.
        val includedTypes: List<String> = mutableListOf("park","restaurant","bar","night_club")
        val excludedTypes: List<String> = mutableListOf("american_restaurant")

        val searchNearbyRequest = SearchNearbyRequest.builder(circle,placeFields).
        setIncludedTypes(includedTypes).setExcludedTypes(excludedTypes).
        setMaxResultCount(5).build()

        placesClient.searchNearby(searchNearbyRequest).addOnSuccessListener { response->
            listPlaces = response.places
            for (place in listPlaces) {
                val placeInfo = "${place.name} - ${place.address}"
                displayList.add(placeInfo)
            }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayList)
            placesListView.adapter = adapter
        }.addOnFailureListener {
            println("xd:It failed")
        }


        return view
    }
}