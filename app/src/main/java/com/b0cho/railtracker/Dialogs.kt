package com.b0cho.railtracker

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * A simple wrapping class for AlertDialog with two buttons used to confirm or abort user's action.
 */
open class ConfirmDialog : DialogFragment() {
    interface Listener {
        /**
         * Listener being invoked, when "Confirm" button is clicked.
         * @param dialogTag tag of dialog that invoked confirm.
         * @param relatedObject object, to which dialog is related.
         */
        fun onDialogConfirm(dialogTag: String?, relatedObject: Parcelable?)
    }
    private var mTag: String? = null
    companion object {
        /**
         * Creates instance of ConfirmDialog fragment.
         * @param tag can be used to distinguish created dialogs,
         * @param title title shown on dialog window,
         * @param message message shown on dialog window,
         * @param isCancelable if true, dialog is cancellable.
         * @param relatedObject object to which dialog is related. Can be retrieved in invoked listeners.
         * @return new instance of ConfirmDialog
         */
        fun newInstance(
            tag: String? = null,
            title: String?,
            message: String,
            confirmButtonText: String,
            cancelButtonText: String,
            isCancelable: Boolean = true,
            relatedObject: Parcelable? = null,
        ): ConfirmDialog {
            return ConfirmDialog().apply {
                mTag = tag
                setCancelable(isCancelable)
                arguments = Bundle().apply {
                    putString("tag", tag)
                    putString("title", title)
                    putString("message", message)
                    putString("confirmButtonText", confirmButtonText)
                    putString("cancelButtonText", cancelButtonText)
                    putBoolean("cancelable", isCancelable)
                    putParcelable("relatedObject", relatedObject)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireActivity())
            .setTitle(arguments?.getString("title", "") ?: "")
            .setMessage(arguments?.getString("message", "") ?: "")
            .setPositiveButton(arguments?.getString("confirmButtonText", "Confirm")) { _, _ ->
                (requireActivity() as Listener).onDialogConfirm(
                    arguments?.getString("tag"),
                    arguments?.getParcelable("relatedObject", Parcelable::class.java)
                )
            }
            .setNegativeButton(arguments?.getString("cancelButtonText", "Cancel")) { _, _ ->
                dismiss()
            }
            .setCancelable(arguments?.getBoolean("cancelable")?: false)
            .create()
    }
}