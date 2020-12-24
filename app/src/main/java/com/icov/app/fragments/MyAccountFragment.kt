package com.icov.app.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.icov.app.R
import com.icov.app.activities.RegisterActivity
import com.icov.app.config.AppConfig
import com.icov.app.database.UserMongoDb
import com.icov.app.databinding.FragmentMyAccountBinding
import com.icov.app.utils.CommonFunctions
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration

class MyAccountFragment : Fragment() {

    private var _binding: FragmentMyAccountBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController
    private lateinit var app: App
    private lateinit var loadingDialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyAccountBinding.inflate(inflater, container, false)
        initializeVariables()
        setupClickListeners()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        setupTheme()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeVariables() {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        loadingDialog =
            CommonFunctions.createDialog(requireContext(), R.layout.loading_progress_dialog, false)
    }

    private fun setupTheme() {
        navController = Navigation.findNavController(binding.root)
        binding.include.username.text = UserMongoDb.fullName
        binding.include.userEmail.text = UserMongoDb.email
    }

    private fun setupClickListeners() {
        binding.editProfileInfo.setOnClickListener {
            navController.navigate(R.id.action_nav_my_account_to_updateUserInfoFragment)
        }
        binding.logOutBtn.setOnClickListener {
            logOut()
        }
    }

    private fun logOut() {
        loadingDialog.show()

        app.currentUser()?.logOutAsync() { result ->
            if (result.isSuccess) {
                loadingDialog.dismiss()
                CommonFunctions.startIntent(requireActivity(), RegisterActivity::class.java, true)
            } else {
                loadingDialog.dismiss()
                Toast.makeText(
                    requireContext(),
                    "Failed to log out, error: ${result.error}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}