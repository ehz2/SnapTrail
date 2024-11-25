package com.example.snaptrail.ui.home.create.locations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.snaptrail.databinding.ItemLocationBinding

class LocationsAdapter(
    private val locations: List<LocationData>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<LocationsAdapter.ViewHolder>() {

    class ViewHolder(private val binding: ItemLocationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(location: LocationData, onDeleteClick: () -> Unit) {
            binding.textLocationName.text = location.name
            binding.textLocationAddress.text = location.address
            binding.buttonDelete.setOnClickListener { onDeleteClick() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(locations[position]) { onDeleteClick(position) }
    }

    override fun getItemCount() = locations.size
}