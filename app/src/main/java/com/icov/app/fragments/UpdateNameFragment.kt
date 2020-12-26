package com.icov.app.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.icov.app.R
import com.icov.app.config.AppConfig
import com.icov.app.models.UserMongoDb
import com.icov.app.databinding.FragmentUpdateNameBinding
import com.icov.app.utils.CommonFunctions
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import org.bson.Document

class UpdateNameFragment : Fragment() {

    private var _binding: FragmentUpdateNameBinding? = null
    private val binding get() = _binding!!

    private lateinit var app: App
    private lateinit var loadingDialog: Dialog
    private lateinit var passwordConfirmationDialog: Dialog
    private lateinit var passwordText: TextInputLayout
    private lateinit var doneBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateNameBinding.inflate(inflater, container, false)
        initializeVariables()
        setupTheme()
        setupClickListeners()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeVariables() {
        app = App(AppConfiguration.Builder(AppConfig.REALM_APP_ID).build())
        loadingDialog =
            CommonFunctions.createDialog(requireContext(), R.layout.loading_progress_dialog, R.drawable.progress_circle,false)
        passwordConfirmationDialog = CommonFunctions.createDialog(
            requireContext(),
            R.layout.password_confirmation_dialog,
            R.drawable.slider_background,
            true
        )
        passwordText = passwordConfirmationDialog.findViewById(R.id.password)
        doneBtn = passwordConfirmationDialog.findViewById(R.id.done_btn)
    }

    private fun setupTheme() {
        val textWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkInputs()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
        binding.firstName.editText?.addTextChangedListener(textWatcher)
        binding.surname.editText?.addTextChangedListener(textWatcher)

        binding.firstName.editText?.setText(UserMongoDb.firstName)
        binding.surname.editText?.setText(UserMongoDb.surname)
    }

    private fun setupClickListeners() {
        binding.update.setOnClickListener {
            checkFullName()
        }
    }

    private fun checkInputs() {
        if (binding.firstName.editText?.text!!.isNotEmpty() &&
            binding.surname.editText?.text!!.isNotEmpty()
        ) {
            binding.update.isEnabled = true
            binding.update.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            return
        }
        binding.update.isEnabled = false
        binding.update.setTextColor(Color.argb(50, 255, 255, 255))
    }

    private fun checkFullName() {
        val customErrorIcon = ContextCompat.getDrawable(requireContext(), R.drawable.error_icon)
        customErrorIcon!!.setBounds(
            -16,
            0,
            customErrorIcon.intrinsicWidth - 16,
            customErrorIcon.intrinsicHeight
        )

        if (binding.firstName.editText?.text.toString() != UserMongoDb.firstName) {
            binding.firstName.isErrorEnabled = false
            binding.surname.isErrorEnabled = false

            if (binding.surname.editText?.text.toString() != UserMongoDb.surname) {
                binding.surname.isErrorEnabled = false
                checkPassword(updateFirstName = true, updateSurname = true, customErrorIcon)
            } else {
                checkPassword(updateFirstName = true, updateSurname = false, customErrorIcon)
            }

        } else {
            if (binding.surname.editText?.text.toString() != UserMongoDb.surname) {
                binding.firstName.isErrorEnabled = false
                binding.surname.isErrorEnabled = false
                checkPassword(updateFirstName = false, updateSurname = true, customErrorIcon)

            } else {
                binding.firstName.isErrorEnabled = true
                binding.firstName.error = getString(R.string.name_same)
                binding.firstName.errorIconDrawable = customErrorIcon

                binding.surname.isErrorEnabled = true
                binding.surname.error = getString(R.string.surname_same)
                binding.surname.errorIconDrawable = customErrorIcon
            }
        }

    }

    private fun checkPassword(
        updateFirstName: Boolean,
        updateSurname: Boolean,
        customErrorIcon: Drawable
    ) {
        passwordConfirmationDialog.show()

        doneBtn.setOnClickListener {
            if (passwordText.editText?.text!!.isNotEmpty()) {
                passwordText.isErrorEnabled = false

                if (passwordText.editText?.length()!! >= 8) {
                    passwordText.isErrorEnabled = false

                    if (passwordText.editText?.text.toString() == UserMongoDb.password) {
                        passwordText.isErrorEnabled = false

                        passwordConfirmationDialog.dismiss()
                        loadingDialog.show()

                        updateDatabase(updateFirstName, updateSurname)

                    } else {
                        passwordText.isErrorEnabled = true
                        passwordText.error = getString(R.string.incorrect_pass)
                        passwordText.errorIconDrawable = customErrorIcon
                    }
                } else {
                    passwordText.isErrorEnabled = true
                    passwordText.error = getString(R.string.pass_text_8_chars)
                    passwordText.errorIconDrawable = customErrorIcon
                }
            } else {
                passwordText.isErrorEnabled = true
                passwordText.error = getString(R.string.pass_cant_empty)
                passwordText.errorIconDrawable = customErrorIcon
            }
        }
    }

    private fun updateDatabase(updateFirstName: Boolean, updateSurname: Boolean) {
        val firstNameValue = binding.firstName.editText?.text.toString()
        val surnameValue = binding.surname.editText?.text.toString()

        val user = app.currentUser()!!
        val mongoClient = user.getMongoClient("mongodb-atlas")
        val mongoCollection = mongoClient.getDatabase("icovDB").getCollection("users")
        val queryFilter = Document("user_id", user.id)

        val updateDocument = Document("user_id", user.id)
            .append("name", firstNameValue)
            .append("surname", surnameValue)
            .append("email", UserMongoDb.email)
            .append("password", UserMongoDb.password)

        mongoCollection.findOneAndUpdate(queryFilter, updateDocument).getAsync { result ->
            if (result.isSuccess) {

                if (updateFirstName) {
                    if (updateSurname) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.name_both_updated),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.name_updated),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.surname_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                UserMongoDb.firstName = firstNameValue
                UserMongoDb.surname = surnameValue
                UserMongoDb.fullName = "$firstNameValue $surnameValue"

                loadingDialog.dismiss()
                requireActivity().finish()

            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to update ${result.error}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

}