/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.example.cameraxbasic.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import java.io.File
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import com.android.example.cameraxbasic.BuildConfig
import com.android.example.cameraxbasic.utils.padWithDisplayCutout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.android.example.cameraxbasic.utils.showImmersive
import com.android.example.cameraxbasic.R
import java.util.Locale

val EXTENSION_WHITELIST = arrayOf("JPG")

/** Fragment used to present the user with a gallery of photos taken */
class GalleryFragment internal constructor() : Fragment() {

    /** AndroidX navigation arguments */
    private val args: GalleryFragmentArgs by navArgs()

    private lateinit var mediaList: MutableList<File>
    private lateinit var sugarResult: TextView
    private lateinit var fileName:TextView

    /** Adapter class used to present a fragment containing one photo or video as a page */
    inner class MediaPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount():Int= mediaList.size
        override fun getItem(position: Int): Fragment = PhotoFragment.create(mediaList[position])
        override fun getItemPosition(obj: Any): Int = POSITION_NONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        retainInstance = true

        // Get root directory of media from navigation arguments
        val rootDirectory = File(args.rootDirectory)

        // Walk through all files in the root directory
        // We reverse the order of the list to present the last photos first
        mediaList = rootDirectory.listFiles { file ->
            EXTENSION_WHITELIST.contains(file.extension.toUpperCase(Locale.ROOT))
        }?.sortedDescending()?.toMutableList() ?: mutableListOf()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_gallery, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Checking media files list
        if (mediaList.isEmpty()) {
            view.findViewById<ImageButton>(R.id.delete_button).isEnabled = false
            view.findViewById<ImageButton>(R.id.share_button).isEnabled = false
        }
        view.findViewById<TextView>(R.id.photoNum).text= String.format("1/%d",mediaList.size)

        sugarResult =view.findViewById<TextView>(R.id.sugarValue)
        fileName = view.findViewById(R.id.fileName)
        // Populate the ViewPager and implement a cache of two media items
        val mediaViewPager = view.findViewById<ViewPager>(R.id.photo_view_pager).apply {
            offscreenPageLimit = 1
            adapter = MediaPagerAdapter(childFragmentManager)
            addOnPageChangeListener(object: ViewPager.OnPageChangeListener{
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
//                    TODO("Not yet implemented")
                }

                override fun onPageSelected(position: Int) {
                    view.findViewById<TextView>(R.id.photoNum).text= String.format("%d/%d",position+1,mediaList.size)
                    object :Thread() {
                        override fun run() {
                            super.run()
                            val imageByte = mediaList[position].readBytes()
                            val bitMap = BitmapFactory.decodeByteArray(imageByte,0,imageByte.size)
                            convertGreyImg(bitMap)
                            fileName.post{
                                fileName.text = mediaList[position].name
                            }



                        }
                    }.start()
                }

                override fun onPageScrollStateChanged(state: Int) {
//                    TODO("Not yet implemented")
                }
            })

        }
        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            view.findViewById<ConstraintLayout>(R.id.cutout_safe_area).padWithDisplayCutout()
        }


        firstCoverGrey()

        view.findViewById<Button>(R.id.greyButton).setOnClickListener{
          println(mediaViewPager.currentItem)
        }
        // Handle back button press
        view.findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp()
        }

        // Handle share button press
        view.findViewById<ImageButton>(R.id.share_button).setOnClickListener {

            mediaList.getOrNull(mediaViewPager.currentItem)?.let { mediaFile ->

                // Create a sharing intent
                val intent = Intent().apply {
                    // Infer media type from file extension
                    val mediaType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(mediaFile.extension)
                    // Get URI from our FileProvider implementation
                    val uri = FileProvider.getUriForFile(
                            view.context, BuildConfig.APPLICATION_ID + ".provider", mediaFile)
                    // Set the appropriate intent extra, type, action and flags
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = mediaType
                    action = Intent.ACTION_SEND
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                // Launch the intent letting the user choose which app to share with
                startActivity(Intent.createChooser(intent, getString(R.string.share_hint)))
            }
        }

        // Handle delete button press
        view.findViewById<ImageButton>(R.id.delete_button).setOnClickListener {

            mediaList.getOrNull(mediaViewPager.currentItem)?.let { mediaFile ->

                AlertDialog.Builder(view.context, android.R.style.Theme_Material_Dialog)
                        .setTitle(getString(R.string.delete_title))
                        .setMessage(getString(R.string.delete_dialog))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { _, _ ->

                            // Delete current photo
                            mediaFile.delete()

                            // Send relevant broadcast to notify other apps of deletion
                            MediaScannerConnection.scanFile(
                                    view.context, arrayOf(mediaFile.absolutePath), null, null)

                            // Notify our view pager
                            mediaList.removeAt(mediaViewPager.currentItem)
                            mediaViewPager.adapter?.notifyDataSetChanged()

                            // If all photos have been deleted, return to camera
                            if (mediaList.isEmpty()) {
                                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp()
                            }

                        }

                        .setNegativeButton(android.R.string.no, null)
                        .create().showImmersive()
            }
        }
    }

    private fun convertGreyImg(bitMap:Bitmap){
        val width = bitMap.width
        val height = bitMap.height
        println("image:width "+width+" height "+height)

        var areaNum = 0

        val pixels:IntArray = IntArray(width*height)
        bitMap.getPixels(pixels,0,width,0,0,width,height)

        val alpha = 0xFF shl 24
        for (indexH in 0 until height){
            for (indexW in 0 until width){
                var grey = pixels[width*indexH+indexW]
                val red = (grey and 0x00ff0000) shr 16
                val green = (grey and 0x0000ff00) shr 8
                val blue = (grey and 0x000000ff)

                grey = (red.toFloat()*0.3+green.toFloat()*0.59+blue.toFloat()*0.11).toInt()
                if (grey>10){
                    areaNum++
                }

                grey = alpha or (grey shl 16) or (grey shl 8) or grey
                pixels[width*indexH+indexW] = grey

            }
        }

        sugarResult.post {
            sugarResult.text= String.format("分辨率：%d * %d\r\n光斑面积：%d",width,height,areaNum)
        }
    }
    private fun firstCoverGrey(){
        val imageByte = mediaList[0].readBytes()
        val bitMap = BitmapFactory.decodeByteArray(imageByte,0,imageByte.size)
        convertGreyImg(bitMap)
        fileName.text = mediaList[0].name

    }


}
