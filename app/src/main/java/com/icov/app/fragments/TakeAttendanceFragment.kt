package com.icov.app.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.icov.app.R
import com.icov.app.database.UserMongoDb

class TakeAttendanceFragment: Fragment() {
    private val TAG = "ATTENDANCE"

    private lateinit var fullNameText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_attendance, container, false)
        initializeVariables(view)
        return view
    }

    override fun onStart() {
        super.onStart()
        setupTheme()
    }


    private fun initializeVariables(view: View) {
        fullNameText = view.findViewById(R.id.username)

    }

    private fun setupTheme() {
        fullNameText.text = UserMongoDb.fullName

    }

    private fun setupClickListeners() {

    }
}