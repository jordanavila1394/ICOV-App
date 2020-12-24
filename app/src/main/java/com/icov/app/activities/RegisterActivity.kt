package com.icov.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.icov.app.R
import com.icov.app.databinding.ActivityRegisterBinding
import com.icov.app.fragments.SignInFragment
import com.icov.app.fragments.SignUpFragment

class RegisterActivity : AppCompatActivity() {

    private lateinit var bind: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(bind.root)
        setupTheme()
    }

    private fun setupTheme() {
        bind.registerTabLayout.addTab(bind.registerTabLayout.newTab(), 0)
        bind.registerTabLayout.addTab(bind.registerTabLayout.newTab(), 1)
        bind.registerTabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val adapter = RegisterAdapter(supportFragmentManager, lifecycle)
        bind.registerViewPager.adapter = adapter

        val tabConfigurationStrategy =
            TabLayoutMediator.TabConfigurationStrategy { tab: TabLayout.Tab, position: Int ->
                when (position) {
                    0 -> tab.text = getString(R.string.sign_in_text)
                    1 -> tab.text = getString(R.string.sign_up_text)
                }
            }

        TabLayoutMediator(bind.registerTabLayout, bind.registerViewPager, tabConfigurationStrategy).attach()
    }

    class RegisterAdapter (fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> SignInFragment()
                1 -> SignUpFragment()
                else -> SignInFragment()
            }
        }

    }

}