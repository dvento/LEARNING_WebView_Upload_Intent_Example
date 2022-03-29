package com.learning.webviewuploadintentexample

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.learning.webviewuploadintentexample.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    // LOAD ANY URL, I'VE TRIED THIS ONE BECAUSE IS WAS THE FIRST ONE ON SEARCH' RESULTS AND IT WORKS ON THE WEBVIEW. I'M NOT AFFILIATED WITH THEM
    private val URL: String = "https://imgbb.com/"

    // file upload
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private var imageUri: Uri? = null
    lateinit var currentPhotoPath: String
    // imagechooser intent code
    private val UPLOAD_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        // set the view
        val view = binding.root
        setContentView(view)

        webView = binding.webview

        setUpWebview()

    }

    private fun setUpWebview() {

        webView.webViewClient = WebViewClient()

        val webViewSettings = webView.settings
        webViewSettings.javaScriptEnabled = true
        webViewSettings.domStorageEnabled = true
        webViewSettings.allowFileAccess = true
        webViewSettings.allowContentAccess = true
        webViewSettings.setSupportZoom(true)

        webView.loadUrl(URL)

        webView.webChromeClient = MyWebChromeClient()

    }

    inner class MyWebChromeClient() : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?, // TODO: make it backward compatible
            fileChooserParams: FileChooserParams?
        ): Boolean {
            uploadCallback = filePathCallback
            createChooserIntent()
            return true
        }
    }

    private fun createChooserIntent() {
        var photoFile: File? = null
        val authorities: String = applicationContext.packageName + ".fileprovider"

        try {
            photoFile = createImageFile()
            imageUri = FileProvider.getUriForFile(applicationContext.applicationContext,authorities,
                photoFile!!
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // camera intent
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // camera intent includes handling the output file
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri)
        // gallery intent
        val photoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        // picker intent, includes gallery intent
        val chooserIntent = Intent.createChooser(photoIntent, "File chooser")
        // we include the camera intent in the picker intent
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(captureIntent))
        // launch the intent
        resultLauncher.launch(chooserIntent)
    }

    // new activityResult handling
    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (uploadCallback != null) {
                // process image upload to the webview
                processImageUpload(result.data)
            } else {
                Toast.makeText(this, "An error occurred while uploading the file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // creates the file in order to upload the camera photo to the webview
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}", // prefix
            ".jpg", // suffix
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun processImageUpload(data: Intent?) {
        if (data != null) {
            val results: Array<Uri>
            val uriData: Uri? = data.data

            if (uriData != null) {
                arrayOf(uriData).also { results = it }
                // pass the data to the webview
                uploadCallback!!.onReceiveValue(results)
            } else {
                uploadCallback!!.onReceiveValue(null)
            }
        } else {
            if (imageUri != null) {
                uploadCallback!!.onReceiveValue(arrayOf(imageUri!!))
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

}