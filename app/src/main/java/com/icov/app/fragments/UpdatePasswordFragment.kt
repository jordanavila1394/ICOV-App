package com.icov.app.fragments

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.icov.app.R
import com.icov.app.config.AppConfig
import com.icov.app.models.UserMongoDb
import com.icov.app.databinding.FragmentUpdatePasswordBinding
import com.icov.app.utils.CommonFunctions
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import org.bson.Document

class UpdatePasswordFragment : Fragment() {

    private var _binding: FragmentUpdatePasswordBinding? = null
    private val binding get() = _binding!!

    private lateinit var app: App
    private lateinit var loadingDialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdatePasswordBinding.inflate(inflater, container, false)
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
        binding.oldPassword.editText?.addTextChangedListener(textWatcher)
        binding.newPassword.editText?.addTextChangedListener(textWatcher)
        binding.confirmNewPassword.editText?.addTextChangedListener(textWatcher)
    }

    private fun setupClickListeners() {
        binding.updatePasswordBtn.setOnClickListener {
            checkEmailAndPassword()
        }
    }

    private fun checkInputs() {
        if (binding.oldPassword.editText?.text!!.isNotEmpty() &&
            binding.oldPassword.editText?.length()!! >= 8 &&
            binding.newPassword.editText?.text!!.isNotEmpty() &&
            binding.newPassword.editText?.length()!! >= 8 &&
            binding.confirmNewPassword.editText?.text!!.isNotEmpty() &&
            binding.confirmNewPassword.editText?.length()!! >= 8
        ) {
            binding.updatePasswordBtn.isEnabled = true
            binding.updatePasswordBtn.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            return
        }
        binding.updatePasswordBtn.isEnabled = false
        binding.updatePasswordBtn.setTextColor(Color.argb(50, 255, 255, 255))
    }


    private fun checkEmailAndPassword() {
        val customErrorIcon = ContextCompat.getDrawable(requireContext(), R.drawable.error_icon)
        customErrorIcon!!.setBounds(
            -16,
            0,
            customErrorIcon.intrinsicWidth - 16,
            customErrorIcon.intrinsicHeight
        )

        if (binding.newPassword.editText?.text.toString() == binding.confirmNewPassword.editText?.text.toString()) {
            binding.confirmNewPassword.isErrorEnabled = false

            if (binding.oldPassword.editText?.text.toString() != binding.newPassword.editText?.text.toString()) {
                binding.newPassword.isErrorEnabled = false

                if (binding.oldPassword.editText?.text.toString() == UserMongoDb.password) {
                    binding.oldPassword.isErrorEnabled = false
                    loadingDialog.show()

                    val args: List<String> =
                        listOf("oldPass", binding.oldPassword.editText?.text.toString())
                    app.emailPassword.callResetPasswordFunctionAsync(
                        UserMongoDb.email, binding.newPassword.editText?.text.toString(),
                        arrayOf(args)
                    ) { result ->
                        if (result.isSuccess) {
                            updateDatabase()
                        } else {
                            loadingDialog.dismiss()
                        }
                    }

                } else {
                    binding.oldPassword.isErrorEnabled = true
                    binding.oldPassword.error = getString(R.string.incorrect_pass)
                    binding.oldPassword.errorIconDrawable = customErrorIcon
                }
            } else {
                binding.newPassword.isErrorEnabled = true
                binding.newPassword.error = getString(R.string.pass_same)
                binding.newPassword.errorIconDrawable = customErrorIcon
            }
        } else {
            binding.confirmNewPassword.isErrorEnabled = true
            binding.confirmNewPassword.error = getString(R.string.password_doesnt_match)
            binding.confirmNewPassword.errorIconDrawable = customErrorIcon
        }
    }

    private fun updateDatabase() {
        val passwordValue = binding.newPassword.editText?.text.toString()

        val user = app.currentUser()!!
        val mongoClient = user.getMongoClient("mongodb-atlas")
        val mongoCollection = mongoClient.getDatabase("icovDB").getCollection("users")
        val queryFilter = Document("user_id", user.id)

        val updateDocument = Document("user_id", user.id)
            .append("name", UserMongoDb.firstName)
            .append("surname", UserMongoDb.surname)
            .append("email", UserMongoDb.email)
            .append("password", passwordValue)

        mongoCollection.findOneAndUpdate(queryFilter, updateDocument).getAsync { result ->
            if (result.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.pass_updated),
                    Toast.LENGTH_SHORT
                ).show()

                UserMongoDb.password = passwordValue

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