package com.octal.android.mediautils

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.octal.android.mediautils.databinding.AleartBottomSheetBinding

class UserAlertBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: AleartBottomSheetBinding
    var title: String = ""
    var desc: String = ""
    var firstButtonText: String = ""
    var firstButtonClick: () -> Unit = {}
    var secondButtonText: String = ""
    var secondButtonClick: () -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = AleartBottomSheetBinding.inflate(inflater, container, false).also {
        binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        with(binding) {
            tvTitle.text = title
            tvDescription.text = desc
            btnFirst.text = firstButtonText
            btnFirst.setOnClickListener {
                firstButtonClick.invoke()
                dismiss()
            }
            btnSecond.isVisible = secondButtonText.isNotEmpty()
            btnSecond.text = secondButtonText
            btnSecond.setOnClickListener {
                secondButtonClick.invoke()
                dismiss()
            }
            ivClose.isVisible = isCancelable
            ivClose.setOnClickListener { dismiss() }
        }
    }
}

fun Fragment.showAlertBottomSheet(
    title: String,
    desc: String,
    firstButtonText: String,
    secondButtonText: String = "",
    isCancelable: Boolean = true,
    firstButtonClick: () -> Unit = {},
    secondButtonClick: () -> Unit = {}
) {
    UserAlertBottomSheet().apply {
        this.title = title
        this.desc = desc
        this.isCancelable = isCancelable
        this.firstButtonText = firstButtonText
        this.secondButtonText = secondButtonText
        this.firstButtonClick = firstButtonClick
        this.secondButtonClick = secondButtonClick
    }.show(childFragmentManager, getString(R.string.alert_screen))
}

fun FragmentActivity.showAlertBottomSheet(
    title: String,
    desc: String,
    firstButtonText: String = "",
    secondButtonText: String = "",
    isCancelable: Boolean = true,
    firstButtonClick: () -> Unit = {},
    secondButtonClick: () -> Unit = {}
) {
    UserAlertBottomSheet().apply {
        this.title = title
        this.desc = desc
        this.isCancelable = isCancelable
        this.firstButtonText = firstButtonText
        this.secondButtonText = secondButtonText
        this.firstButtonClick = firstButtonClick
        this.secondButtonClick = secondButtonClick
    }.show(supportFragmentManager, getString(R.string.alert_screen))
}