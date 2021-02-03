/*
 * Copyright 2021 headuck (https://blog.headuck.com/)
 *
 * This file is part of GoOutWithDuck
 *
 * GoOutWithDuck is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GoOutWithDuck is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GoOutWithDuck. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.headuck.app.gooutwithduck

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.google.android.material.snackbar.Snackbar
import com.headuck.app.gooutwithduck.data.VenueVisitInfo
import com.headuck.app.gooutwithduck.databinding.FragmentScanCodeBinding
import com.headuck.app.gooutwithduck.utilities.LocaleUtil
import com.headuck.app.gooutwithduck.utilities.SNACK_DURATION
import com.headuck.app.gooutwithduck.utilities.navigateUpSafe
import com.headuck.app.gooutwithduck.utilities.setBackPressHandler
import com.headuck.app.gooutwithduck.viewmodels.BottomNavSharedViewModel
import com.headuck.app.gooutwithduck.viewmodels.ScanCodeViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatReader
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.google.zxing.client.android.DecodeFormatManager
import com.google.zxing.client.android.DecodeHintManager
import com.journeyapps.barcodescanner.*
import com.journeyapps.barcodescanner.CameraPreview.StateListener
import com.journeyapps.barcodescanner.camera.CameraSettings
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*

/**
 * Fragment for scan QR code
 */
@AndroidEntryPoint
class ScanCodeFragment : Fragment() {

    private val viewModel: ScanCodeViewModel by viewModels()
    private val bottomNavSharedViewModel: BottomNavSharedViewModel by lazy {
        ViewModelProvider(requireActivity(), defaultViewModelProviderFactory).get(BottomNavSharedViewModel::class.java)
    }

    private lateinit var barcodeView : BarcodeView
    private lateinit var viewFinderView : ViewfinderView

    // Logic
    private var beepManager: BeepManager? = null
    private var cameraPermissionTextView: TextView? = null

    private val stateListener: StateListener = object : StateListener {
        override fun previewSized() {}
        override fun previewStarted() {}
        override fun previewStopped() {}
        override fun cameraError(error: Exception) {}
        override fun cameraClosed() {}
    }

    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            val contents = result.toString()
            if (contents.isEmpty()) {
                return
            }
            beepManager!!.playBeepSoundAndVibrate()

            val scanResult = viewModel.setNewScan(result.text)
            scanResult.observe(this@ScanCodeFragment, {
                it?.onFailure { msg ->
                    Timber.d("Scan failure: $msg")
                    val msg1 = if (msg == ScanCodeViewModel.ALREADY_CHECKED_IN) {
                        getString(R.string.scan_already_checked_in)
                    } else {
                        msg
                    }
                    Snackbar.make(binding.root, msg1, Snackbar.LENGTH_LONG)
                            .setDuration(SNACK_DURATION)
                            .addCallback(
                                object : Snackbar.Callback() {
                                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                        super.onDismissed(transientBottomBar, event)
                                        this@ScanCodeFragment.navigateUpSafe()
                                    }
                                }
                            )
                            .show()
                }
                ?.onSuccess {
                    navigateToScanDonePage(
                            VenueVisitInfo(it, LocaleUtil.getDisplayLang(activity!!))
                    )
                }
            })
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }


    @TargetApi(23)
    private fun initScanWithPermissionCheck() {
        activity?.let {
            val hasCameraPermission = ActivityCompat.checkSelfPermission(it, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (!hasCameraPermission) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_CAMERA_REQUEST)
                showCameraPermissionRequirement(true)
            } else {
                initScan()
            }
        }
    }

    private fun showCameraPermissionRequirement(show: Boolean) {
        // barcodeScannerView!!.visibility = if (show) View.GONE else View.VISIBLE
        barcodeView.visibility = if (show) View.GONE else View.VISIBLE
        viewFinderView.visibility = if (show) View.GONE else View.VISIBLE
        if (show) {
            barcodeView.pause()
        } else {
            barcodeView.resume()
        }
        cameraPermissionTextView!!.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun initScan() {
        activity?.let {

            showCameraPermissionRequirement(false)
            val window: Window = it.window
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val formats: Collection<BarcodeFormat> = listOf(BarcodeFormat.QR_CODE)
            barcodeView.decoderFactory = DefaultDecoderFactory(formats)

            val settings = CameraSettings()
            val reader = MultiFormatReader()

            // Scan the formats the intent requested, and return the result to the calling activity.
            val decodeFormats = DecodeFormatManager.parseDecodeFormats(it.intent)
            val decodeHints = DecodeHintManager.parseDecodeHints(it.intent)

            // Check what type of scan. Default: normal scan
            val scanType = 0
            val characterSet = null

            reader.setHints(decodeHints)

            barcodeView.cameraSettings = settings
            barcodeView.decoderFactory = DefaultDecoderFactory(decodeFormats, decodeHints, characterSet, scanType)
            barcodeView.isUseTextureView = true
            barcodeView.marginFraction = 0.15
            viewFinderView.setCameraPreview(barcodeView)
            //barcodeScannerView!!.initializeFromIntent(it.intent)
            barcodeView.decodeSingle(callback)
            barcodeView.resume()

        }
    }


    override fun onResume() {
        super.onResume()
        activity?.let{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                showCameraPermissionRequirement(true)
            } else {
                // barcodeScannerView!!.setStatusText(null)
                barcodeView.decodeSingle(callback)
                barcodeView.resume()
            }
        }

    }

    override fun onPause() {
        super.onPause()
        barcodeView.pauseAndWait()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CAMERA_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initScan()
            } else {
                // TODO
                activity?.let{
                    Snackbar.make(binding.scannerContainer, R.string.camera_permission_denied_text, Snackbar.LENGTH_LONG)
                            .setDuration(SNACK_DURATION)
                            .show()
                }

            }
        }
    }

    private fun onClick(view: View) {
        if (cameraPermissionTextView!!.visibility == View.VISIBLE) {
            when (view.id) {
                R.id.scanner_container -> initScanWithPermissionCheck()
                else -> {
                }
            }
        }
    }

    private lateinit var binding: FragmentScanCodeBinding;


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScanCodeBinding.inflate(inflater, container, false)
        binding.scannerContainer.setOnClickListener {
            onClick(it)
        }
        binding.scanToolbar.setNavigationOnClickListener { _ ->
            navigateUpSafe()
        }
        setBackPressHandler()
        bottomNavSharedViewModel.setBottomNavHidden(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraPermissionTextView = binding.cameraPermissionText

        // val preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
        // permissionNeededExplanation = findViewById(R.id.activity_scanner_permission_needed_explanation)
        barcodeView = binding.zxingBarcodeSurface;
        viewFinderView = binding.zxingViewfinderView;
        // barcodeScannerView = binding.zxingBarcodeScanner
        barcodeView.addStateListener(stateListener)

        beepManager = BeepManager(this.activity)
        beepManager!!.isVibrateEnabled = true
        //if (!preferences.getBoolean("pref_enable_beep_on_scan", true)) {
        //    beepManager!!.isBeepEnabled = false
        //}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            initScanWithPermissionCheck()
        } else {
            initScan()
        }
    }

    private fun navigateToScanDonePage(venueVisitInfo: VenueVisitInfo) {
        val direction =
                ScanCodeFragmentDirections.actionScanCodeFragmentToScanDoneFragment(venueVisitInfo)
        findNavController().navigate(direction)
    }

    companion object {
        private const val PERMISSION_CAMERA_REQUEST = 0
    }
}