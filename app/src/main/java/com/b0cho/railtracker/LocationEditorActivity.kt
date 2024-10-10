package com.b0cho.railtracker

import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationEditorActivity : AppCompatActivity() {
    companion object {
        // KEYS FOR INTENTS
        const val LAUNCH_LOCATION_PICKER: String = "BOOLEAN_LAUNCH_LOCATION_PICKER"
    }

    private val mLocationEditorVM: LocationEditorVM by viewModels()
    private lateinit var mLocationPickerLauncher: ActivityResultLauncher<LocationPickerDTO?>
    private lateinit var mLocationName: String
    private var mLocationPosition: GeoPoint? = null
    private var mPendingSaveDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_editor)
        setSupportActionBar(findViewById(R.id.locationEditorToolbar))

        if (savedInstanceState == null) {
            mLocationEditorVM.apply {
                launchLocationPicker = intent.getBooleanExtra(LAUNCH_LOCATION_PICKER, false)
                closeByLocationPicker = launchLocationPicker
                mapViewDTO = intent.getParcelableExtra(
                    OSMMapViewActivity.MAPVIEW_STATE,
                    OSMMapViewActivity.MapViewDTO::class.java
                )
            }
        }

        val exitDialogCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                with(AlertDialog.Builder(this@LocationEditorActivity)) {
                    setMessage(
                        """
                    |Changes have not been saved!
                    |Are you sure to exit?
                """.trimMargin()
                    )
                    setPositiveButton("Yes") { _, _ -> finish() }
                    setNegativeButton("Cancel") { _, _ -> }
                    create()
                }.show()
            }
        }.also { onBackPressedDispatcher.addCallback(this, it) }

        with(mLocationEditorVM) {
            with(findViewById<EditText>(R.id.nameEditText)) {
                locationName.observe(this@LocationEditorActivity) {
                    if (it != this.text.toString()) {
                        this.setText(it)
                    }
                }
                doAfterTextChanged { setLocationName(text.toString()) }
            }

            with(findViewById<EditText>(R.id.notesEditText)) {
                doAfterTextChanged { setLocationNotes(text.toString()) }
            }

            with(findViewById<TextView>(R.id.coordinatesTextView)) {
                locationPosition.observe(this@LocationEditorActivity) {
                    this.text = it.toString()
                }

                // launch LocationPicker on click on coordination text
                this.setOnClickListener {
                    with(mapViewDTO) {
                        mLocationPickerLauncher.launch(
                            LocationPickerDTO(
                                mLocationPosition ?: this?.center ?: GeoPoint(21.0, 37.0),
                                15.0,
                                this?.showCurrentPosition ?: false,
                                this?.selectedTileSourceKey ?: 0,
                                this?.selectedOverlaysKeys ?: listOf(0),
                                this?.showMyLocationsOverlay ?: false,
                                mLocationPosition,
                                mLocationName
                            )
                        )
                    }
                }
            }

            with(findViewById<Button>(R.id.saveButton)) {
                isLocationSaveable.observe(this@LocationEditorActivity) { isEnabled = it }
                setOnClickListener {
                    viewModelScope.launch {
                        val insertJob = launch {
                            try {
                                mLocationEditorVM.insertLocationIntoDB()
                                Toast.makeText(applicationContext, "$mLocationName saved successfully!", Toast.LENGTH_SHORT).show()
                                finish()
                            } catch (e: Exception) {
                                when(e) {
                                    is SQLiteException,
                                    is IllegalStateException -> {
                                        Toast.makeText(applicationContext, "Save failed!", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> throw e
                                }
                            }
                        }
                        // additional job to launch a terminate dialog if inserting lasts more than 1s
                        launch {
                            delay(1000L)
                            mLocationEditorVM.setPendingSave(insertJob)
                        }
                    }
                }
            }

            locationName.observe(this@LocationEditorActivity) { mLocationName = it }
            locationPosition.observe(this@LocationEditorActivity) { mLocationPosition = it }
            isLocationSaved.observe(this@LocationEditorActivity) { exitDialogCallback.isEnabled = !it }
            pendingSavingJob.observe(this@LocationEditorActivity) {
                it?.let {
                    it.invokeOnCompletion {
                        mPendingSaveDialog?.dismiss()
                    }
                    if(it.isActive) {
                        mPendingSaveDialog =
                            AlertDialog.Builder(this@LocationEditorActivity)
                                .setMessage("Saving...")
                                .setNegativeButton("Cancel") { _, _ -> it.cancel() }
                                .setCancelable(false)
                                .show()
                    }
                }
            }
        }

        mLocationPickerLauncher =
            registerForActivityResult(LocationPickerActivity.ResultContract()) {
                if (it != null) {
                    // save obtained value
                    mLocationEditorVM.setLocationPosition(it)
                } else {
                    // close editor activity, if LocationPicker did not obtained value on start
                    if (mLocationPosition == null && mLocationEditorVM.closeByLocationPicker) {
                        finish()
                    }
                }
            }
    }

    override fun onStart() {
        super.onStart()

        if (mLocationEditorVM.launchLocationPicker) {
            mLocationEditorVM.launchLocationPicker = false
            mLocationPickerLauncher.launch(LocationPickerDTO(mLocationEditorVM.mapViewDTO!!))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return false
    }

    override fun onStop() {
        super.onStop()
        // closing and destroying dialog due to unintended attaching on exited activity
        mPendingSaveDialog?.dismiss()
        mPendingSaveDialog = null
    }
}

@HiltViewModel
private class LocationEditorVM @Inject constructor(
    private val pinLocationsDao: MyLocationDao,
) : ViewModel() {
    var launchLocationPicker = false
    var closeByLocationPicker = false
    var mapViewDTO: OSMMapViewActivity.MapViewDTO? = null

    private val mPosition: MutableLiveData<GeoPoint?> = MutableLiveData(null)
    val locationPosition: LiveData<GeoPoint?>
        get() = mPosition
    fun setLocationPosition(value: GeoPoint) {
        mPosition.value = value
        validateLocationData()
    }

    private val mName: MutableLiveData<String> = MutableLiveData("")
    val locationName: LiveData<String>
        get() = mName
    fun setLocationName(value: String) {
        mName.value = value
        validateLocationData()
    }

    private val mNotes: MutableLiveData<String> = MutableLiveData("")
    fun setLocationNotes(value: String) {
        mNotes.value = value
        validateLocationData()
    }

    private val mIsSaveable: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLocationSaveable: LiveData<Boolean>
        get() = mIsSaveable

    private val mIsLocationSaved: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLocationSaved: LiveData<Boolean>
        get() = mIsLocationSaved

    private val mSavingJob: MutableLiveData<Job?> = MutableLiveData(null)
    val pendingSavingJob: LiveData<Job?>
        get() = mSavingJob

    private fun validateLocationData() {
        mIsSaveable.value = !mName.value.isNullOrEmpty() && mPosition.value != null
    }

    suspend fun insertLocationIntoDB() = withContext(Dispatchers.IO) {
        pinLocationsDao.insert(
                MyLocation(
                    name = mName.value!!,
                    position = mPosition.value!!,
                    notes = mNotes.value,
                )
            ).also {
                if(it == 0L) {
                    throw SQLiteException("INSERT of Location FAILED")
                }
            }
    }

    fun setPendingSave(insertJob: Job?) {
        mSavingJob.value = insertJob
    }
}