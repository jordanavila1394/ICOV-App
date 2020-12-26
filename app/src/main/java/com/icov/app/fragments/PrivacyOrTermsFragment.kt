package com.icov.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.icov.app.R
import com.icov.app.databinding.FragmentPrivacyOrTermsBinding

class PrivacyOrTermsFragment : Fragment() {

    private var _binding: FragmentPrivacyOrTermsBinding? = null
    private val binding get() = _binding!!

    private val assertLoc = "file:///android_asset/"
    private val privacyPolicyFileName = "PrivacyPolicy.html"
    private val termsAndConditionsFileName = "TermsAndConditions.html"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrivacyOrTermsBinding.inflate(inflater, container, false)
        setupTheme()
        return binding.root
    }

    private fun setupTheme() {
        binding.webView.settings.javaScriptEnabled = true

        if (requireArguments().getString("type") == "Privacy") {
            (activity as AppCompatActivity?)!!.supportActionBar?.title =
                getString(R.string.privacy_policy_title)
            binding.privacyImage.visibility = View.VISIBLE
            binding.webView.loadUrl(assertLoc + privacyPolicyFileName)

        } else {
            (activity as AppCompatActivity?)!!.supportActionBar?.title =
                getString(R.string.terms_and_conditions_title)
            binding.privacyImage.visibility = View.GONE
            binding.webView.loadUrl(assertLoc + termsAndConditionsFileName)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}