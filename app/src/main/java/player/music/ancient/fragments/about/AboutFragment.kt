/*
 * Copyright (c) 2020 Future Eternal.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package player.music.ancient.fragments.about

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import player.music.ancient.App
import player.music.ancient.Constants
import player.music.ancient.R
import player.music.ancient.adapter.ContributorAdapter
import player.music.ancient.databinding.FragmentAboutBinding
import player.music.ancient.extensions.openUrl
import player.music.ancient.fragments.LibraryViewModel
import player.music.ancient.util.NavigationUtil
import dev.chrisbanes.insetter.applyInsetter
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AboutFragment : Fragment(R.layout.fragment_about), View.OnClickListener {
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    private val libraryViewModel by activityViewModel<LibraryViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAboutBinding.bind(view)
        binding.aboutContent.cardOther.version.setSummary(getAppVersion())
        setUpView()
        loadContributors()

        binding.aboutContent.root.applyInsetter {
            type(navigationBars = true) {
                padding(vertical = true)
            }
        }
    }

    private fun setUpView() {
        binding.aboutContent.cardAncientInfo.appGithub.setOnClickListener {
            openUrl(Constants.GITHUB_PROJECT)
        }
        binding.aboutContent.cardAncientInfo.faqLink.setOnClickListener {
            openUrl(Constants.FAQ_LINK)
        }
        binding.aboutContent.cardAncientInfo.appRate.setOnClickListener {
            openUrl(Constants.RATE_ON_GOOGLE_PLAY)
        }
        binding.aboutContent.cardAncientInfo.appTranslation.setOnClickListener {
            openUrl(Constants.TRANSLATE)
        }
        binding.aboutContent.cardAncientInfo.appShare.setOnClickListener {
            shareApp()
        }
        binding.aboutContent.cardAncientInfo.donateLink.setOnClickListener {
            NavigationUtil.goToSupportDevelopment(requireActivity())
        }
        binding.aboutContent.cardAncientInfo.bugReportLink.setOnClickListener {
            NavigationUtil.bugReport(requireActivity())
        }

        binding.aboutContent.cardSocial.telegramLink.setOnClickListener {
            openUrl(Constants.APP_TELEGRAM_LINK)
        }
        binding.aboutContent.cardSocial.twitterLink.setOnClickListener {
            openUrl(Constants.APP_TWITTER_LINK)
        }
        binding.aboutContent.cardSocial.pinterestLink.setOnClickListener {
            openUrl(Constants.PINTEREST)
        }
        binding.aboutContent.cardSocial.websiteLink.setOnClickListener {
            openUrl(Constants.WEBSITE)
        }

        binding.aboutContent.cardOther.changelog.setOnClickListener {
            NavigationUtil.gotoWhatNews(requireActivity())
        }
        binding.aboutContent.cardOther.openSource.setOnClickListener {
            NavigationUtil.goToOpenSource(requireActivity())
        }
    }

    override fun onClick(view: View) {
    }

    private fun getAppVersion(): String {
        return try {
            val isPro = if (App.isProVersion()) "Pro" else "Free"
            val packageInfo =
                requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            "${packageInfo.versionName} $isPro"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "0.0.0"
        }
    }

    private fun shareApp() {
        ShareCompat.IntentBuilder(requireActivity()).setType("text/plain")
            .setChooserTitle(R.string.share_app)
            .setText(String.format(getString(R.string.app_share), requireActivity().packageName))
            .startChooser()
    }

    private fun loadContributors() {
        val contributorAdapter = ContributorAdapter(emptyList())
        binding.aboutContent.cardCredit.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = DefaultItemAnimator()
            adapter = contributorAdapter
        }
        libraryViewModel.fetchContributors().observe(viewLifecycleOwner) { contributors ->
            contributorAdapter.swapData(contributors)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
