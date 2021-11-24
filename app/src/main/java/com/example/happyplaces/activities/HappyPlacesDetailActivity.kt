package com.example.happyplaces.activities

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.net.toUri
import com.example.happyplaces.R
import com.example.happyplaces.models.HappyPlaceModel
import kotlinx.android.synthetic.main.activity_add_happy_place.*

import kotlinx.android.synthetic.main.activity_happy_places_detail.*
import kotlinx.android.synthetic.main.activity_happy_places_detail.iv_place_image
import kotlinx.android.synthetic.main.item_happy_place.*

class HappyPlacesDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_happy_places_detail)

        setSupportActionBar(toolbar_happy_place_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar_happy_place_detail.setNavigationOnClickListener {
            onBackPressed()
        }

        var happyPlaceDetailModel: HappyPlaceModel? = null

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            // get the Serializable data model class with the details in it
            happyPlaceDetailModel =
                intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        if (happyPlaceDetailModel != null) {

            supportActionBar!!.title = happyPlaceDetailModel.title


            iv_place_image.setImageURI(Uri.parse(happyPlaceDetailModel.image))
            tv_description.text = happyPlaceDetailModel.description
            tv_location.text = happyPlaceDetailModel.location
        }

    }
}