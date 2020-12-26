package com.icov.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.icov.app.databinding.FragmentCalendarBinding

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        setupTheme()
        setupClickListeners()
        return binding.root
    }

    private fun setupTheme() {

    }

    private fun setupClickListeners() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}