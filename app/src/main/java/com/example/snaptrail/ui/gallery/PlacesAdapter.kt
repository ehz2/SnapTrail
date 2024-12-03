package com.example.snaptrail.ui.gallery

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.example.snaptrail.R
import com.google.android.libraries.places.api.model.Place

class PlacesAdapter(
    private val context: Context,
    private val places: List<Place>
) : ArrayAdapter<Place>(context, 0, places) {

    private val successfulPositions = mutableSetOf<Int>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_place, parent, false)

        val place = getItem(position)
        val nameTextView = view.findViewById<TextView>(R.id.place_name)
        val addressTextView = view.findViewById<TextView>(R.id.place_address)

        // Highlight successful places with a light green background
        if (successfulPositions.contains(position)) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.light_green_success))
            nameTextView.text = place?.name
            addressTextView.text = place?.address
        } else {
            view.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            nameTextView.text = "Guess Location #${position + 1}"
            addressTextView.text = ""
        }

        return view
    }

    // Method to mark a place as successfully found
    fun setSuccessfulPlace(position: Int) {
        successfulPositions.add(position)
        notifyDataSetChanged() // Update the list
    }
}
