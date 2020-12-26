package com.icov.app.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.icov.app.R
import com.icov.app.databinding.FragmentUpdateUserInfoBinding

class UpdateUserInfoFragment : Fragment() {

    private var _binding: FragmentUpdateUserInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateUserInfoBinding.inflate(inflater, container, false)
        setupTheme()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupTheme() {
        binding.tabLayout.addTab(binding.tabLayout.newTab(), 0)
        binding.tabLayout.addTab(binding.tabLayout.newTab(), 1)
        binding.tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val adapter =
            UpdateUserInfoAdapter(requireActivity().supportFragmentManager, lifecycle)
        binding.viewPager.adapter = adapter

        val tabConfigurationStrategy =
            TabLayoutMediator.TabConfigurationStrategy { tab: TabLayout.Tab, position: Int ->
                when (position) {
                    0 -> tab.text = getString(R.string.user_info_text)
                    1 -> tab.text = getString(R.string.password_text)
                }
            }

        TabLayoutMediator(binding.tabLayout, binding.viewPager, tabConfigurationStrategy).attach()
    }

    class UpdateUserInfoAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> UpdateNameFragment()
                1 -> UpdatePasswordFragment()
                else -> UpdateNameFragment()
            }

        }

    }


}