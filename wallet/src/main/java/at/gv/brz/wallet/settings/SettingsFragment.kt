package at.gv.brz.wallet.settings

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import at.gv.brz.common.BuildConfig
import at.gv.brz.common.R
import at.gv.brz.common.html.BuildInfo
import at.gv.brz.common.util.AssetUtil
import at.gv.brz.common.util.setSecureFlagToBlockScreenshots
import at.gv.brz.eval.CovidCertificateSdk
import at.gv.brz.wallet.data.WalletSecureStorage
import at.gv.brz.wallet.databinding.FragmentSettingsBinding
import at.gv.brz.wallet.faq.WalletFaqFragment
import at.gv.brz.wallet.html.HtmlFragment
import at.gv.brz.wallet.util.DebugLogFragment
import at.gv.brz.wallet.util.DebugLogUtil

class SettingsFragment: Fragment() {

    companion object {
        fun newInstance(): SettingsFragment = SettingsFragment()
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.settingsToolbar.setNavigationOnClickListener { v: View? ->
            parentFragmentManager.popBackStack()
        }
        binding.settingsItemImprint.setOnClickListener {
            val buildInfo =
                BuildInfo(
                    getString(at.gv.brz.wallet.R.string.wallet_onboarding_app_title),
                    at.gv.brz.wallet.BuildConfig.VERSION_NAME,
                    at.gv.brz.wallet.BuildConfig.BUILD_TIME,
                    at.gv.brz.wallet.BuildConfig.FLAVOR,
                    getString(at.gv.brz.wallet.R.string.wallet_terms_privacy_link)
                )
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(at.gv.brz.wallet.R.anim.slide_enter, at.gv.brz.wallet.R.anim.slide_exit, at.gv.brz.wallet.R.anim.slide_pop_enter, at.gv.brz.wallet.R.anim.slide_pop_exit)
                .replace(
                    at.gv.brz.wallet.R.id.fragment_container, HtmlFragment.newInstance(
                        at.gv.brz.wallet.R.string.impressum_title,
                        buildInfo,
                        AssetUtil.getImpressumBaseUrl(it.context),
                        AssetUtil.getImpressumHtml(it.context, buildInfo),
                        at.gv.brz.wallet.R.id.fragment_container,
                        R.string.impressum_title_loaded
                    )
                )
                .addToBackStack(HtmlFragment::class.java.canonicalName)
                .commit()
        }
        binding.settingsItemLicenses.setOnClickListener {
            val buildInfo =
                BuildInfo(
                    getString(at.gv.brz.wallet.R.string.wallet_onboarding_app_title),
                    at.gv.brz.wallet.BuildConfig.VERSION_NAME,
                    at.gv.brz.wallet.BuildConfig.BUILD_TIME,
                    at.gv.brz.wallet.BuildConfig.FLAVOR,
                    getString(at.gv.brz.wallet.R.string.wallet_terms_privacy_link)
                )
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(at.gv.brz.wallet.R.anim.slide_enter, at.gv.brz.wallet.R.anim.slide_exit, at.gv.brz.wallet.R.anim.slide_pop_enter, at.gv.brz.wallet.R.anim.slide_pop_exit)
                .replace(
                    at.gv.brz.wallet.R.id.fragment_container, HtmlFragment.newInstance(
                        at.gv.brz.wallet.R.string.licenses_title,
                        buildInfo,
                        AssetUtil.getImpressumBaseUrl(it.context),
                        AssetUtil.getLicenseHtml(it.context, buildInfo),
                        at.gv.brz.wallet.R.id.fragment_container,
                        R.string.licenses_title_loaded
                    )
                )
                .addToBackStack(HtmlFragment::class.java.canonicalName)
                .commit()
        }
        binding.settingsItemFaq.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(at.gv.brz.wallet.R.anim.slide_enter, at.gv.brz.wallet.R.anim.slide_exit, at.gv.brz.wallet.R.anim.slide_pop_enter, at.gv.brz.wallet.R.anim.slide_pop_exit)
                .replace(at.gv.brz.wallet.R.id.fragment_container, WalletFaqFragment.newInstance())
                .addToBackStack(WalletFaqFragment::class.java.canonicalName)
                .commit()
        }
        binding.settingsItemCampaignNotifications.setOnClickListener {
            val am = activity?.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
            if (am?.isEnabled == true) {
                toggleCampaignNotifications()
                setContentDescriptionForCampaignNotifications()
            }
        }
        binding.settingsItemCampaignNotificationsSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            val secureStorage = WalletSecureStorage.getInstance(requireContext())
            secureStorage.setHasOptedOutOfNonImportantCampaigns(!isChecked)
            setContentDescriptionForCampaignNotifications()
        }
        val secureStorage = WalletSecureStorage.getInstance(requireContext())
        binding.settingsItemCampaignNotificationsSwitch.isChecked = secureStorage.getHasOptedOutOfNonImportantCampaigns() == false
        binding.settingsItemUpdateData.setOnClickListener {
            updateData()
        }
        if (at.gv.brz.wallet.BuildConfig.FLAVOR == "abn" || at.gv.brz.wallet.BuildConfig.FLAVOR == "prodtest") {
            binding.settingsItemLog.isVisible = true
            binding.settingsItemLog.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(at.gv.brz.wallet.R.anim.slide_enter, at.gv.brz.wallet.R.anim.slide_exit, at.gv.brz.wallet.R.anim.slide_pop_enter, at.gv.brz.wallet.R.anim.slide_pop_exit)
                    .replace(at.gv.brz.wallet.R.id.fragment_container, DebugLogFragment.newInstance())
                    .addToBackStack(DebugLogFragment::class.java.canonicalName)
                    .commit()
            }
        }

        setContentDescriptionForCampaignNotifications()
        view.announceForAccessibility(getString(R.string.settings_title_loaded))
    }

    private fun setContentDescriptionForCampaignNotifications() {
        val secureStorage = WalletSecureStorage.getInstance(requireContext())
        if (secureStorage.getHasOptedOutOfNonImportantCampaigns()) {
            binding.settingsItemCampaignNotifications.contentDescription = listOf(getString(R.string.accessibility_settings_row_campaign_notifications_toggle_inactive), getString(R.string.settings_row_campaign_notifications_message), getString(R.string.accessibility_change_campaign_notifications_toggle)).joinToString(", ")
        } else {
            binding.settingsItemCampaignNotifications.contentDescription = listOf(getString(R.string.accessibility_settings_row_campaign_notifications_toggle_active), getString(R.string.settings_row_campaign_notifications_message), getString(R.string.accessibility_change_campaign_notifications_toggle)).joinToString(", ")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun toggleCampaignNotifications() {
        val secureStorage = WalletSecureStorage.getInstance(requireContext())
        secureStorage.setHasOptedOutOfNonImportantCampaigns(!secureStorage.getHasOptedOutOfNonImportantCampaigns())
        binding.settingsItemCampaignNotificationsSwitch.isChecked = secureStorage.getHasOptedOutOfNonImportantCampaigns() == false
    }

    private fun updateData() {
        val alertDialog = getAlertDialog(requireContext(), R.layout.dialog_progress, true)
        alertDialog.show()
        alertDialog.findViewById<TextView>(R.id.text_progress_bar).setText(R.string.business_rule_update_progress)
        CovidCertificateSdk.getCertificateVerificationController().refreshTrustList(lifecycleScope, true, onCompletionCallback = {
            alertDialog.dismiss()
            val message = if (it.failed) R.string.business_rule_update_failed_message else R.string.business_rule_update_success_message
            androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CovidCertificate_AlertDialogStyle)
                .setMessage(message)
                .setPositiveButton(R.string.business_rule_update_ok_button) { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .create()
                .apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
                .show()
        })
    }

    fun getAlertDialog(
        context: Context,
        layout: Int,
        setCancellationOnTouchOutside: Boolean
    ): AlertDialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val customLayout: View =
            layoutInflater.inflate(layout, null)
        builder.setView(customLayout)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(setCancellationOnTouchOutside)
        return dialog
    }
}