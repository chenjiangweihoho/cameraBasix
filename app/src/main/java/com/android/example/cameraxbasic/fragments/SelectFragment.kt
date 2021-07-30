package com.android.example.cameraxbasic.fragments

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentResolverCompat.query
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.android.example.cameraxbasic.R
import com.bumptech.glide.Glide
import java.io.File
import java.time.temporal.ValueRange
import java.util.jar.Manifest

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val CHOOSE_PHOTO = 111
/**
 * A simple [Fragment] subclass.
 * Use the [SelectFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SelectFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var realPathFromUri:String? = null
    private var imageUri:Uri? = null
    private var resolver:ContentResolver? =null

    private var pictureShow:ImageView?=null
    private var appleType:TextView? = null
    private lateinit var showMessage:TextView
    private lateinit var sugarValue:TextView

    private var threadGrey:Int = 10
    private var c1:Double = -0.134
    private var c2:Double = 26.47
    private var heightStart:Int = 1500
    private var widthStart:Int = 1500
    private var areaNum:Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resolver = requireContext().contentResolver
        pictureShow = view.findViewById(R.id.picture)
        appleType = view.findViewById(R.id.appleType)
        showMessage = view.findViewById(R.id.showMessage)
        sugarValue = view.findViewById(R.id.sugarValue)
        view.findViewById<ImageButton>(R.id.tackPicture).setOnClickListener{
            Navigation.findNavController(requireActivity(),R.id.fragment_container).navigate(
                SelectFragmentDirections.actionSelectFragmentToCameraFragment()
            )
        }
        view.findViewById<ImageButton>(R.id.selectPicture).setOnClickListener{
            if (ContextCompat.checkSelfPermission(requireContext(),android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(requireContext() as Activity, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }else{
                openAlumb()
            }
        }
        view.findViewById<Button>(R.id.bingtangxin).setOnClickListener{
            c1 = -0.134
            c2 = 26.47
            threadGrey = 10
            appleType?.text = "苹果类型：冰糖心"
            if (areaNum>0){
                sugarValue.text = String.format("苹果糖度：%.3f",(c1*areaNum+c2))
            }

        }
        view.findViewById<Button>(R.id.hongfushi).setOnClickListener {
            c1 = -0.15
            c2 = 28.5
            threadGrey = 12
            appleType?.text = "苹果类型：红富士"
            if (areaNum>0){
                sugarValue.text = String.format("苹果糖度：%.3f",(c1*areaNum+c2))
            }
        }
        view.findViewById<Button>(R.id.huangjinshuai).setOnClickListener {
            c1 = -0.18
            c2 = 24.5
            threadGrey = 12
            appleType?.text = "苹果类型：黄金帅"
            if (areaNum>0){
                sugarValue.text = String.format("苹果糖度：%.3f",(c1*areaNum+c2))
            }
        }
//        view.findViewById<TextView>(R.id.tip).text ="请选择照片或者拍照"
        view.findViewById<TextView>(R.id.showMessage)?.text = String.format("Tip:请选择一张照片或者拍照")
        val bitMap = BitmapFactory.decodeResource(resources,R.drawable.apple)
        pictureShow?.setImageBitmap(bitMap)
    }
     fun openAlumb(){
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent,CHOOSE_PHOTO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){
            CHOOSE_PHOTO -> if (resultCode == Activity.RESULT_OK){
                if (data != null) {
                    handleImageOnKitKat(data)
                }
            }
            else ->{

            }
        }
    }
    /**
     * 适配api19及以上,根据uri获取图片的绝对路径
     *
     * @param context 上下文对象
     * @param uri     图片的Uri
     * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
     */
     private fun handleImageOnKitKat(data: Intent){
        var imagePath: String? = null
        val uri = data.data
        if (DocumentsContract.isDocumentUri(context,uri)){
            val docId = DocumentsContract.getDocumentId(uri)
            if ("com.android.providers.media.documents" == uri!!.authority){
                val id = docId.split(":".toRegex()).toTypedArray()[1]
                val selection = MediaStore.Images.Media._ID + "=" + id
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection)
            }else if ("com.android.providers.downloads.documents" == uri.authority){
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),java.lang.Long.valueOf(docId))
                imagePath = getImagePath(contentUri,null)
            }else if("content".equals(uri!!.scheme,ignoreCase = true)){
                imagePath = getImagePath(uri,null)
            }else if("file".equals(uri.scheme,ignoreCase = true)){
                imagePath = uri.path
            }
            println(imagePath)
            displayImage(imagePath)
        }
     }
    private fun getImagePath(uri: Uri?,selection:String?): String? {
        var path: String? = null
        var cursor = resolver?.query(uri!!,null,selection,null,null)
        if (cursor != null){
            if (cursor.moveToFirst()){
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path
    }
    private fun displayImage(imagePath:String?){
        if (imagePath != null){
            val bitmap = BitmapFactory.decodeFile(imagePath)
            pictureShow?.setImageBitmap(bitmap)
            view?.findViewById<TextView>(R.id.pictureName)?.text = File(imagePath).name
            convertGreyImg(bitmap)
//            val resource = imagePath?.let { File(it) }?:R.drawable.ic_photo
//            context?.let { Glide.with(it).load(resource).into(requireView().findViewById(R.id.picture)) }

        }
    }

    private fun convertGreyImg(bitMap: Bitmap){
        val width = bitMap.width
        val height = bitMap.height

        println("image:width "+width+" height "+height)

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
                if (grey>threadGrey){
                    areaNum++
                }

                grey = alpha or (grey shl 16) or (grey shl 8) or grey
                pixels[width*indexH+indexW] = grey

            }
        }

        showMessage.text = String.format("分辨率：%d * %d   光斑面积：%d",width,height,areaNum)
        sugarValue.text = String.format("苹果糖度：%.3f",(c1*areaNum+c2))
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SelectFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SelectFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}