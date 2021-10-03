package com.hossein.runningapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.hossein.runningapp.R
import com.hossein.runningapp.databinding.FragmentRunBinding
import com.hossein.runningapp.databinding.FragmentStaticticsBinding

class StatisticsFragment :Fragment() {

    private val viewModel: ViewModel by viewModels()
    private var _binding: FragmentStaticticsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaticticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}