package com.icov.app.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.icov.app.R

class Functions {

    companion object {

        fun startIntent(
            fromActivity: Activity,
            toActivity: Class<out Activity>,
            toFinish: Boolean
        ) {
            val intent = Intent(fromActivity, toActivity)
            fromActivity.startActivity(intent)
            if (toFinish) {
                fromActivity.finish()
            }
        }


        fun setFragment(
            context: Context,
            parentFrameLayout: FrameLayout,
            fragment: Fragment,
            animOpen: Int,
            animExit: Int
        ) {
            val fragmentTransaction: FragmentTransaction =
                (context as FragmentActivity).supportFragmentManager.beginTransaction()
            fragmentTransaction.setCustomAnimations(animOpen, animExit)
            fragmentTransaction.replace(parentFrameLayout.id, fragment)
            fragmentTransaction.commit()
        }


        fun createDialog(context: Context, layoutResId: Int, cancellable: Boolean): Dialog {
            val dialog = Dialog(context)
            dialog.setContentView(layoutResId)
            dialog.setCancelable(cancellable)
            dialog.window?.setBackgroundDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.slider_background
                )
            )
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            return dialog
        }
    }


}