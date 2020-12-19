package com.icov.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.icov.app.R
import com.icov.app.fragments.SignInFragment
import com.icov.app.fragments.SignUpFragment

class RegisterActivity : AppCompatActivity() {

    private lateinit var registerViewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        initializeVariables()
        setupTheme()
    }

    private fun initializeVariables() {
        registerViewPager = findViewById(R.id.register_view_pager)
        tabLayout = findViewById(R.id.register_tab_layout)
    }

    private fun setupTheme() {
        tabLayout.addTab(tabLayout.newTab(), 0)
        tabLayout.addTab(tabLayout.newTab(), 1)
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val adapter = RegisterAdapter(supportFragmentManager, lifecycle)
        registerViewPager.adapter = adapter

        val tabConfigurationStrategy =
            TabLayoutMediator.TabConfigurationStrategy { tab: TabLayout.Tab, position: Int ->
                when (position) {
                    0 -> tab.text = getString(R.string.sign_in_text)
                    1 -> tab.text = getString(R.string.sign_up_text)
                }
            }

        TabLayoutMediator(tabLayout, registerViewPager, tabConfigurationStrategy).attach()
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