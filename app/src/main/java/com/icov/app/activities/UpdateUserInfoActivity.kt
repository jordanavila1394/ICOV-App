package com.kabbodev.mongodb.activities

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kabbodev.mongodb.R
import com.kabbodev.mongodb.fragments.UpdateNameFragment
import com.kabbodev.mongodb.fragments.UpdatePasswordFragment
import com.kabbodev.mongodb.utils.Functions

class UpdateUserInfoActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var loadingDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_user_info)
        initializeVariables()
        setupTheme()
    }

    private fun initializeVariables() {
        tabLayout = findViewById(R.id.tab_layout)
        viewPager2 = findViewById(R.id.view_pager)
        loadingDialog = Functions.createDialog(this, R.layout.loading_progress_dialog, false)
    }

    private fun setupTheme() {
        tabLayout.addTab(tabLayout.newTab(), 0)
        tabLayout.addTab(tabLayout.newTab(), 1)
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val adapter = UpdateUserInfoAdapter(supportFragmentManager, lifecycle)
        viewPager2.adapter = adapter

        val tabConfigurationStrategy = TabLayoutMediator.TabConfigurationStrategy { tab: TabLayout.Tab, position: Int ->
            when (position) {
                0 -> tab.text = getString(R.string.user_info_text)
                1 -> tab.text = getString(R.string.password_text)
            }
        }

        TabLayoutMediator(tabLayout, viewPager2, tabConfigurationStrategy).attach()
    }

    class UpdateUserInfoAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

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