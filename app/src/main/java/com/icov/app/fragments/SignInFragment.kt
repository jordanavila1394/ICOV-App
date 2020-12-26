package com.icov.app.fragments

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.icov.app.R
import com.icov.app.activities.MainActivity
import com.icov.app.activities.ResetPasswordActivity
import com.icov.app.config.AppConfig
import com.icov.app.databinding.FragmentSignInBinding
import com.icov.app.utils.CommonFunctions
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        setupTheme()
        setupClickListeners()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupTheme() {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())

        binding.signInEmail.translationX = 800f
        binding.signInPassword.translationX = 800f
        binding.forgotPassword.translationX = 800f
        binding.signInBtn.translationX = 800f

        binding.signInEmail.alpha = 0f
        binding.signInPassword.alpha = 0f
        binding.forgotPassword.alpha = 0f
        binding.signInBtn.alpha = 0f

        binding.signInEmail.animate().translationX(0f).alpha(1f).setDuration(800).setStartDelay(300)
            .start()
        binding.signInPassword.animate().translationX(0f).alpha(1f).setDuration(800)
            .setStartDelay(500).start()
        binding.forgotPassword.animate().translationX(0f).alpha(1f).setDuration(800)
            .setStartDelay(500)
            .start()
        binding.signInBtn.animate().translationX(0f).alpha(1f).setDuration(800).setStartDelay(700)
            .start()

        val textWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkInputs()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
        binding.signInEmail.editText?.addTextChangedListener(textWatcher)
        binding.signInPassword.editText?.addTextChangedListener(textWatcher)
    }

    private fun setupClickListeners() {
        binding.forgotPassword.setOnClickListener {
            CommonFunctions.startIntent(requireActivity(), ResetPasswordActivity::class.java, false)
        }
        binding.signInBtn.setOnClickListener {
            checkEmailAndPassword()
        }
    }

    private fun checkInputs() {
        if (binding.signInEmail.editText?.text!!.isNotEmpty() &&
            binding.signInPassword.editText?.text!!.isNotEmpty()
        ) {
            binding.signInBtn.isEnabled = true
            binding.signInBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            return
        }
        binding.signInBtn.isEnabled = false
        binding.signInBtn.setTextColor(Color.argb(50, 255, 255, 255))
    }


    private fun checkEmailAndPassword() {
        val customErrorIcon = ContextCompat.getDrawable(requireContext(), R.drawable.error_icon)
        customErrorIcon!!.setBounds(
            -16,
            0,
            customErrorIcon.intrinsicWidth - 16,
            customErrorIcon.intrinsicHeight
        )

        if (Patterns.EMAIL_ADDRESS.matcher(binding.signInEmail.editText?.text.toString())
                .matches()
        ) {
            binding.signInEmail.isErrorEnabled = false

            if (binding.signInPassword.editText?.length()!! >= 8) {
                binding.signInPassword.isErrorEnabled = false

                binding.signInProgressBar.visibility = View.VISIBLE
                binding.signInBtn.isEnabled = false
                binding.signInBtn.setTextColor(Color.argb(50, 255, 255, 255))

                val emailPasswordCredentials: Credentials = Credentials.emailPassword(
                    binding.signInEmail.editText?.text.toString(),
                    binding.signInPassword.editText?.text.toString()
                )

                app.loginAsync(emailPasswordCredentials) { result ->
                    if (result.isSuccess) {
                        CommonFunctions.startIntent(
                            requireActivity(),
                            MainActivity::class.java,
                            true
                        )
                    } else {
                        binding.signInProgressBar.visibility = View.INVISIBLE
                        binding.signInBtn.isEnabled = true
                        binding.signInBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        Toast.makeText(
                            requireContext(),
                            result.error.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } else {
                binding.signInPassword.isErrorEnabled = true
                binding.signInPassword.error = getString(R.string.incorrect_pass)
                binding.signInPassword.errorIconDrawable = customErrorIcon
            }

        } else {
            binding.signInEmail.isErrorEnabled = true
            binding.signInEmail.error = getString(R.string.invalid_email_address)
            binding.signInEmail.errorIconDrawable = customErrorIcon
        }
    }

}