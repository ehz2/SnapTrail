package com.example.snaptrail.ui.home.create

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentCreateTrailBinding

class CreateTrailFragment : Fragment() {

    private var _binding: FragmentCreateTrailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateTrailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfigureLocations.setOnClickListener {
            findNavController().navigate(R.id.action_createTrailFragment_to_configureLocationsFragment)
        }

        binding.btnSaveTrail.setOnClickListener {
            // Implement logic to save trail information here
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}