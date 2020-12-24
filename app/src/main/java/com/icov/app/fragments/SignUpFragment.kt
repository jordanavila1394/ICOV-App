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
import com.icov.app.config.AppConfig
import com.icov.app.databinding.FragmentSignUpBinding
import com.icov.app.utils.CommonFunctions
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import org.bson.Document

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private lateinit var app: App

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
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
        val textWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkInputs()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
        binding.signUpFirstName.editText?.addTextChangedListener(textWatcher)
        binding.signUpSurname.editText?.addTextChangedListener(textWatcher)
        binding.signUpEmail.editText?.addTextChangedListener(textWatcher)
        binding.signUpPassword.editText?.addTextChangedListener(textWatcher)
        binding.signUpConfirmPassword.editText?.addTextChangedListener(textWatcher)
    }

    private fun setupClickListeners() {
        binding.signUpBtn.setOnClickListener {
            checkEmailAndPassword()
        }
    }

    private fun checkInputs() {
        if (binding.signUpFirstName.editText?.text!!.isNotEmpty() &&
            binding.signUpSurname.editText?.text!!.isNotEmpty() &&
            binding.signUpEmail.editText?.text!!.isNotEmpty() &&
            binding.signUpPassword.editText?.text!!.isNotEmpty() &&
            binding.signUpPassword.editText?.length()!! >= 8 &&
            binding.signUpConfirmPassword.editText?.text!!.isNotEmpty()
        ) {
            binding.signUpBtn.isEnabled = true
            binding.signUpBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            return
        }
        binding.signUpBtn.isEnabled = false
        binding.signUpBtn.setTextColor(Color.argb(50, 255, 255, 255))
    }

    private fun checkEmailAndPassword() {
        val customErrorIcon = ContextCompat.getDrawable(requireContext(), R.drawable.error_icon)
        customErrorIcon!!.setBounds(
            -16,
            0,
            customErrorIcon.intrinsicWidth - 16,
            customErrorIcon.intrinsicHeight
        )

        if (Patterns.EMAIL_ADDRESS.matcher(binding.signUpEmail.editText?.text.toString())
                .matches()
        ) {
            binding.signUpEmail.isErrorEnabled = false

            if (binding.signUpPassword.editText?.text.toString() == binding.signUpConfirmPassword.editText?.text.toString()) {
                binding.signUpConfirmPassword.isErrorEnabled = false
                binding.signUpProgressText.visibility = View.VISIBLE
                binding.signUpProgressText.text = getString(R.string.registering_text)
                binding.signUpProgressBar.visibility = View.VISIBLE
                binding.signUpBtn.isEnabled = false
                binding.signUpBtn.setTextColor(Color.argb(50, 255, 255, 255))

                signUp()

            } else {
                binding.signUpConfirmPassword.isErrorEnabled = true
                binding.signUpConfirmPassword.error = getString(R.string.password_doesnt_match)
                binding.signUpConfirmPassword.errorIconDrawable = customErrorIcon
            }
        } else {
            binding.signUpEmail.isErrorEnabled = true
            binding.signUpEmail.error = getString(R.string.invalid_email_address)
            binding.signUpEmail.errorIconDrawable = customErrorIcon
        }
    }

    private fun signUp() {
        val emailValue = binding.signUpEmail.editText?.text.toString()
        val passwordValue = binding.signUpPassword.editText?.text.toString()

        app.emailPassword
            .registerUserAsync(emailValue, passwordValue) { result ->
                if (result.isSuccess) {
                    binding.signUpProgressText.text =
                        getString(R.string.successfully_registered_text)
                    val emailPasswordCredentials: Credentials =
                        Credentials.emailPassword(emailValue, passwordValue)

                    app
                        .loginAsync(emailPasswordCredentials) { loginResult ->
                            if (loginResult.isSuccess) {
                                binding.signUpProgressText.text =
                                    getString(R.string.successfully_logged_in_text)
                                setDataOnDB(emailValue, passwordValue)

                            } else {
                                binding.signUpProgressText.visibility = View.INVISIBLE
                                binding.signUpProgressText.text =
                                    getString(R.string.registering_text)
                                binding.signUpProgressBar.visibility = View.INVISIBLE
                                binding.signUpBtn.isEnabled = true
                                binding.signUpBtn.setTextColor(resources.getColor(R.color.white))
                                Toast.makeText(
                                    requireContext(),
                                    result.error.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    binding.signUpProgressText.visibility = View.INVISIBLE
                    binding.signUpProgressText.text = getString(R.string.registering_text)
                    binding.signUpProgressBar.visibility = View.INVISIBLE
                    binding.signUpBtn.isEnabled = true
                    binding.signUpBtn.setTextColor(resources.getColor(R.color.white))
                    Toast.makeText(
                        requireContext(),
                        result.error.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun setDataOnDB(emailValue: String, passwordValue: String) {
        val user = app.currentUser()!!
        val mongoClient = user.getMongoClient("mongodb-atlas")
        val mongoCollection = mongoClient.getDatabase("icovDB").getCollection("users")

        val firstNameValue = binding.signUpFirstName.editText?.text.toString()
        val surnameValue = binding.signUpSurname.editText?.text.toString()

        val document = Document("user_id", user.id)
            .append("name", firstNameValue)
            .append("surname", surnameValue)
            .append("email", emailValue)
            .append("password", passwordValue)

        mongoCollection
            .insertOne(document)
            .getAsync { result ->
                if (result.isSuccess) {
                    CommonFunctions.startIntent(
                        requireActivity(),
                        MainActivity::class.java,
                        true
                    )
                } else {
                    binding.signUpProgressText.visibility = View.INVISIBLE
                    binding.signUpProgressText.text = getString(R.string.registering_text)
                    binding.signUpProgressBar.visibility = View.INVISIBLE
                    binding.signUpBtn.isEnabled = true
                    binding.signUpBtn.setTextColor(resources.getColor(R.color.white))
                    Toast.makeText(
                        requireContext(),
                        result.error.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

}