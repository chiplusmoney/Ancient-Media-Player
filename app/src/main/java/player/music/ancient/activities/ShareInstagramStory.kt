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
package player.music.ancient.activities

import android.content.ContentValues
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import androidx.core.os.BundleCompat
import androidx.core.view.drawToBitmap
import player.music.ancient.util.ColorUtil
import player.music.ancient.util.MaterialValueHelper
import player.music.ancient.activities.base.AbsThemeActivity
import player.music.ancient.databinding.ActivityShareInstagramBinding
import player.music.ancient.extensions.accentColor
import player.music.ancient.extensions.setStatusBarColor
import player.music.ancient.glide.AncientGlideExtension
import player.music.ancient.glide.AncientGlideExtension.asBitmapPalette
import player.music.ancient.glide.AncientGlideExtension.songCoverOptions
import player.music.ancient.glide.AncientMusicColoredTarget
import player.music.ancient.model.Song
import player.music.ancient.util.Share
import player.music.ancient.util.color.MediaNotificationProcessor
import com.bumptech.glide.Glide

/**
 * Created by hemanths on 2020-02-02.
 */

class ShareInstagramStory : AbsThemeActivity() {

    private lateinit var binding: ActivityShareInstagramBinding

    companion object {
        const val EXTRA_SONG = "extra_song"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareInstagramBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setStatusBarColor(Color.TRANSPARENT)

        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
        setSupportActionBar(binding.toolbar)

        val song = intent.extras?.let { BundleCompat.getParcelable(it, EXTRA_SONG, Song::class.java) }
        song?.let { songFinal ->
            Glide.with(this)
                .asBitmapPalette()
                .songCoverOptions(songFinal)
                .load(AncientGlideExtension.getSongModel(songFinal))
                .into(object : AncientMusicColoredTarget(binding.image) {
                    override fun onColorReady(colors: MediaNotificationProcessor) {
                        setColors(colors.backgroundColor)
                    }
                })

            binding.shareTitle.text = songFinal.title
            binding.shareText.text = songFinal.artistName
            binding.shareButton.setOnClickListener {
                val bitmap = binding.mainContent.drawToBitmap(Bitmap.Config.ARGB_8888)
                val uri = saveBitmapToMediaStore(bitmap, "Design")
                uri?.let { imageUri ->
                    Share.shareStoryToSocial(
                        this@ShareInstagramStory,
                        imageUri
                    )
                }
            }
        }
        binding.shareButton.setTextColor(
            MaterialValueHelper.getPrimaryTextColor(
                this,
                ColorUtil.isColorLight(accentColor())
            )
        )
        binding.shareButton.backgroundTintList =
            ColorStateList.valueOf(accentColor())
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap, displayName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let { imageUri ->
            contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }
        return uri
    }

    private fun setColors(color: Int) {
        binding.mainContent.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(color, Color.BLACK)
            )
    }
}
