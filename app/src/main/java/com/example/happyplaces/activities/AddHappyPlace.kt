package com.example.happyplaces.activities

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.happyplaces.R
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.models.HappyPlaceModel
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_add_happy_place.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlace : AppCompatActivity() , View.OnClickListener {


    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var galleryImageResultLauncher: ActivityResultLauncher<Intent>
    private var saveImageToInternalStorage: Uri? = null

    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    private var mHappyPlaceDetails: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_place)

        setSupportActionBar(toolbar_add_place)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar_add_place.setNavigationOnClickListener {
            onBackPressed()
        }

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mHappyPlaceDetails =
                intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                updateDateInView()

            }
        updateDateInView()

        if (mHappyPlaceDetails != null) {
            supportActionBar?.title = "Edit Happy Place"

            et_title.setText(mHappyPlaceDetails!!.title)
            et_description.setText(mHappyPlaceDetails!!.description)
            et_date.setText(mHappyPlaceDetails!!.date)
            et_location.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)

            iv_place_image.setImageURI(saveImageToInternalStorage)

            btn_save.text = "UPDATE"
        }

        et_date.setOnClickListener(this@AddHappyPlace)
        tv_add_image.setOnClickListener(this@AddHappyPlace)
        btn_save.setOnClickListener(this)
        registerOnActivityForResult()

    }


    override fun onClick(v: View?) {
        when (v!!.id) {

            R.id.et_date -> {
                DatePickerDialog(
                    this@AddHappyPlace,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image ->{
                val pictureDialog=AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems= arrayOf("Select picture from gallery","Capture picture from camera")
                pictureDialog.setItems(pictureDialogItems){
                    dialog,which->
                    when(which){
                        0-> choosePictureFromGallery()
                        1-> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()}
            R.id.btn_save ->{
                when{
                    et_title.text.isNullOrEmpty()->{
                        Toast.makeText(this@AddHappyPlace,"Pls enter title",Toast.LENGTH_SHORT).show()
                    }
                    et_description.text.isNullOrEmpty()->{
                        Toast.makeText(this@AddHappyPlace,"Pls enter description",Toast.LENGTH_SHORT).show()
                    }
                    et_location.text.isNullOrEmpty()->{
                        Toast.makeText(this@AddHappyPlace,"Pls enter location",Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage==null ->{
                        Toast.makeText(this@AddHappyPlace,"Pls select an image",Toast.LENGTH_SHORT).show()
                    }else->{
                        val happyPlaceModel = HappyPlaceModel(
                            if(mHappyPlaceDetails==null)0 else mHappyPlaceDetails!!.id,
                            et_title.text.toString(),
                            saveImageToInternalStorage.toString(),
                            et_description.text.toString(),
                            et_date.text.toString(),
                            et_location.text.toString(),
                            mLatitude,
                            mLongitude
                        )
                        val dbHandler = DatabaseHandler(this)
                        if(mHappyPlaceDetails == null){
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                            if(addHappyPlace > 0){
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }else{
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                            if(updateHappyPlace > 0){
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
            }

        }
    }

    var resultLauncherCamera = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data

            val thumbNail : Bitmap = data!!.extras?.get("data") as Bitmap

            saveImageToInternalStorage = saveImageToInternalStorage(thumbNail)
            Log.e("Saved Image","Path:: $saveImageToInternalStorage")
            iv_place_image.setImageBitmap(thumbNail)
        }
    }

    private fun takePhotoFromCamera() {
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
        ).withListener(object: MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    resultLauncherCamera.launch(cameraIntent)
                }
            }
            override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken) {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }


    private fun registerOnActivityForResult(){
        //returns: the launcher that can be used to start the activity or dispose of the prepared call.
        galleryImageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                if(data!=null){
                    val contentUri=data.data
                    try{
                        val selectedImageBitmap:Bitmap=MediaStore.Images.Media.getBitmap(this.contentResolver,contentUri)
                        iv_place_image.setImageBitmap(selectedImageBitmap)

                        saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("Saved Image","Path:: $saveImageToInternalStorage")
                        //iv_place_image.setImageURI(contentUri)
                    }
                    catch (e:IOException){
                        e.printStackTrace()
                        Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

    }


    private fun choosePictureFromGallery(){
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ).withListener(object:MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if(report!!.areAllPermissionsGranted()){
                    val galleryIntent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryImageResultLauncher.launch(galleryIntent)  //start activity to get image from gallery
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun updateDateInView() {
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        et_date.setText(sdf.format(cal.time).toString())
    }

    private fun saveImageToInternalStorage(bitmap:Bitmap):Uri{
        val wrapper= ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_PRIVATE)
        file= File(file,"${UUID.randomUUID()}.jpg")
        try{
            val stream :OutputStream= FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e:IOException){
            e.printStackTrace()
        }

        return Uri.parse(file.absolutePath)
    }

    companion object {
        private const val GALLERY = 1
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
    }
}