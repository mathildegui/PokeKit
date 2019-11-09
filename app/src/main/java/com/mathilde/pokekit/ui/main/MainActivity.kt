package com.mathilde.pokekit.ui.main

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel //FirebaseCloudModelSource
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel //FirebaseLocalModelSource
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.custom.*
import com.google.firebase.storage.FirebaseStorage
import com.mathilde.pokekit.HandleFileUpload
import com.mathilde.pokekit.adapter.PokemonAdapter
import com.mathilde.pokekit.model.Pokemon
import com.mathilde.pokekit.ui.camera.BaseCameraActivity
import com.otaliastudios.cameraview.CameraListener
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.pokemon_sheet.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


//A list of all the pokemons
val pokeArray: Array<String> = arrayOf("abra", "aerodactyl", "alakazam", "arbok", "arcanine", "articuno", "beedrill", "bellsprout",
        "blastoise", "bulbasaur", "butterfree", "caterpie", "chansey", "charizard", "charmander", "charmeleon", "clefable", "clefairy", "cloyster", "cubone", "dewgong",
        "diglett", "ditto", "dodrio", "doduo", "dragonair", "dragonite", "dratini", "drowzee", "dugtrio", "eevee", "ekans", "electabuzz",
        "electrode", "exeggcute", "exeggutor", "farfetchd", "fearow", "flareon", "gastly", "gengar", "geodude", "gloom",
        "golbat", "goldeen", "golduck", "golem", "graveler", "grimer", "growlithe", "gyarados", "haunter", "hitmonchan",
        "hitmonlee", "horsea", "hypno", "ivysaur", "jigglypuff", "jolteon", "jynx", "kabuto",
        "kabutops", "kadabra", "kakuna", "kangaskhan", "kingler", "koffing", "krabby", "lapras", "lickitung", "machamp",
        "machoke", "machop", "magikarp", "magmar", "magnemite", "magneton", "mankey", "marowak", "meowth", "metapod",
        "mew", "mewtwo", "moltres", "mrmime", "muk", "nidoking", "nidoqueen", "nidorina", "nidorino", "ninetales",
        "oddish", "omanyte", "omastar", "onix", "paras", "parasect", "persian", "pidgeot", "pidgeotto", "pidgey",
        "pikachu", "pinsir", "poliwag", "poliwhirl", "poliwrath", "ponyta", "porygon", "primeape", "psyduck", "raichu",
        "rapidash", "raticate", "rattata", "rhydon", "rhyhorn", "sandshrew", "sandslash", "scyther", "seadra",
        "seaking", "seel", "shellder", "slowbro", "slowpoke", "snorlax", "spearow", "squirtle", "starmie", "staryu",
        "tangela", "tauros", "tentacool", "tentacruel", "vaporeon", "venomoth", "venonat", "venusaur", "victreebel",
        "vileplume", "voltorb", "vulpix", "wartortle", "weedle", "weepinbell", "weezing", "wigglytuff", "zapdos", "zubat")

class MainActivity : BaseCameraActivity(), HandleFileUpload {
    override fun uploadImageToStorage(name: String) {
        //TODO - To change body of created functions use File | Settings | File Templates.

        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        val baos = ByteArrayOutputStream()
        currentBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
        val data = baos.toByteArray()
        if(isNetworkAvailable()) {
            toast("Thanks for feedback")
//            root
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    companion object {
        /** Dimensions of inputs.  */
        const val DIM_IMG_SIZE_X = 224
        const val DIM_IMG_SIZE_Y = 224
        const val DIM_BATCH_SIZE = 1
        const val DIM_PIXEL_SIZE = 3
        const val IMAGE_MEAN = 128
        private const val IMAGE_STD = 128.0f
    }


    private val intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)
    private var imgData: ByteBuffer = ByteBuffer.allocateDirect(
            4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)

    private lateinit var viewModel: MainViewModel
    private lateinit var firebaseInterpreter: FirebaseModelInterpreter

    private lateinit var inputOutputOptions: FirebaseModelInputOutputOptions

    private lateinit var currentBitmap: Bitmap

    private val pokemonList = mutableListOf<Pokemon>()
    private lateinit var itemAdapter: PokemonAdapter

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val rootRef = FirebaseStorage.getInstance().reference.child("pokemon")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayShowTitleEnabled(false)

        imgData.order(ByteOrder.nativeOrder())
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        loadModel()

        rvLabel.layoutManager = LinearLayoutManager(this)
        itemAdapter = PokemonAdapter(pokemonList, this)
        rvLabel.adapter = itemAdapter

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_UPLOAD, getString(R.string.feedback_notification), NotificationManager.IMPORTANCE_MIN)
            notificationManager.createNotificationChannel(channel)
        }*/

        fab.setOnClickListener {
            cameraView.capturePicture()
            itemAdapter.setList(emptyList())
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            cameraView.addCameraListener(object : CameraListener() {
                @RequiresApi(Build.VERSION_CODES.N)
                override fun onPictureTaken(jpeg: ByteArray) {
//                    isRefreshVisible = true
                    convertByteArrayToBitmap(jpeg)
                }
            })
        }
    }

    /*override fun onClick(v: View?) {
        //the if statement is to alternate between the refresh and image capture functionality of FAB
        if (v?.id == R.id.fab) {
//            progressBar.visibility = View.VISIBLE
//            itemAdapter.setList(emptyList())
            cameraView.capturePicture()
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            cameraView.addCameraListener(object : CameraListener() {
                override fun onPictureTaken(jpeg: ByteArray) {
//                    isRefreshVisible = true
                    convertByteArrayToBitmap(jpeg)
                }
            })
        }
    }*/

    /*private fun runModelInference() {
        if (firebaseInterpreter == null) {
            Log.e("TAG", "Image classifier has not been initialized; Skipped.")
            return
        }
        // Create input data.
        val imgData = convertBitmapToByteBuffer(mSelectedImage, mSelectedImage.getWidth(),
                mSelectedImage.getHeight())

        try {
            val inputs = FirebaseModelInputs.Builder().add(imgData).build()
            // Here's where the magic happens!!
            firebaseInterpreter
                    .run(inputs, inputOutputOptions)
                    .addOnFailureListener(OnFailureListener { e ->
                        e.printStackTrace()
//                        showToast("Error running model inference")
                    })
                    .continueWith(
                            object : Continuation<FirebaseModelOutputs, List<String>>() {
                                fun then(task: Task<FirebaseModelOutputs>): List<String> {
                                    val labelProbArray = task.result!!
                                            .getOutput<Array<ByteArray>>(0)
                                    val topLabels = getTopLabels(labelProbArray)
                                    mGraphicOverlay.clear()
                                    val labelGraphic = LabelGraphic(mGraphicOverlay, topLabels)
                                    mGraphicOverlay.add(labelGraphic)
                                    return topLabels
                                }
                            })
        } catch (e: FirebaseMLException) {
            e.printStackTrace()
//            showToast("Error running model inference")
        }

    }*/

    private fun loadModel() {
        var conditionsBuilder = FirebaseModelDownloadConditions.Builder().requireWifi()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Enable advanced conditions on Android Nougat and newer.
            conditionsBuilder = conditionsBuilder.requireCharging().requireDeviceIdle()
        }


//        val conditions = conditionsBuilder.build() as FirebaseModelDownloadConditions
        // Build a FirebaseCloudModelSource object by specifying the name you assigned the model
        // when you uploaded it in the Firebase console.

        val remoteModel = FirebaseCustomRemoteModel.Builder("poke-detector")
                .build()

        val conditions = FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build()
        FirebaseModelManager.getInstance().download(remoteModel, conditions)
                .addOnCompleteListener {
                    // Success.
                }

        /*val cloudSource = FirebaseCloudModelSource.Builder("poke-detector")
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build()
        FirebaseModelManager.getInstance().registerCloudModelSource(cloudSource)*/

        val localModel = FirebaseCustomLocalModel.Builder()
                .setAssetFilePath("pokedex.tflite")
                .build()

        val options = FirebaseModelInterpreterOptions.Builder(localModel).build()

        //Load a local model using the FirebaseLocalModelSource Builder class
       /* val fireBaseLocalModelSource = FirebaseLocalModelSource.Builder("poke-detector")
                .setAssetFilePath("pokedex.tflite") //name of the .tflite model stored in asset folder
                .build()

        //Registering the model loaded above with the ModelManager Singleton
        FirebaseModelManager.getInstance().registerLocalModelSource(fireBaseLocalModelSource)*/

        /*val options =  FirebaseModelOptions.Builder()
                .setCloudModelName("poke-detector")
                .setLocalModelName("poke-detector")
                .build()*/

        firebaseInterpreter =
                FirebaseModelInterpreter.getInstance(options)!!

        inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
                .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 224, 224, 3))
                .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 149))
                .build()




        //Show target if not already shown
//        if(!activity?.defaultSharedPreferences?.contains("TARGET_INTRO")!!) {
//            showTarget()
//        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun convertByteArrayToBitmap(byteArray: ByteArray) {
        //Handle this shit in bg
        doAsync {
            val exifInterface = ExifInterface(ByteArrayInputStream(byteArray))
            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

            //to fix images coming out to be rotated
            //https://github.com/google/cameraview/issues/22#issuecomment-269321811
            val m = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90F)
                ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180F)
                ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270F)
            }
            //Create a new bitmap with fixed rotation
            //Crop a part of image that's inside viewfinder and perform detection on that image
            //https://stackoverflow.com/a/8180576/5471095
            //TODO : Need to find a better way to do this than creating a new bitmap
            val cropX = (bitmap.width * 0.2).toInt()
            val cropY = (bitmap.height * 0.25).toInt()

            currentBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, bitmap.width - 2 * cropX, bitmap.height - 2 * cropY, m, true)
            //free up the original bitmap
            bitmap.recycle()

            //create a scaled bitmap for Tensorflow
            val scaledBitmap = Bitmap.createScaledBitmap(currentBitmap, 224, 224, false)
            uiThread {
                getPokemonFromBitmap(scaledBitmap)
            }
        }
    }

    //Generate input data
    private fun convertBitmapToByteBuffer(btp: Bitmap?) : ByteBuffer {
        //Clear the Bytebuffer for a new image
        imgData.rewind()

        btp?.getPixels(intValues, 0, btp.width, 0, 0, btp.width, btp.height) //??

        // Convert the image to floating point
        var px = 0
        for(i in 0 until DIM_IMG_SIZE_X) {
            for(j in 0 until DIM_IMG_SIZE_Y) {
                val currPixel = intValues[px++]

                imgData.putFloat(((currPixel shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((currPixel shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((currPixel and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
        return imgData
    }

    //Perform detection on the input data
    private fun getPokemonFromBitmap(btp: Bitmap?) {

//        val imgData = convertBitmapToByteBuffer(buffer, width, height);
        val inputs = FirebaseModelInputs.Builder()
                .add(convertBitmapToByteBuffer(btp))
                .build()

        firebaseInterpreter.run(inputs, inputOutputOptions)
                ?.addOnSuccessListener {
                    //we found a pokemon

                    val pokelist = mutableListOf<Pokemon>()

                    /**
                     * Run a foreach loop through the output float array containing the probabilities
                     * corresponding to each label
                     * @see pokeArray to know what labels are supported
                     * it here is the list of probabilities for the detected pokemons, so we can loop through it easily
                     */

                    it.getOutput<Array<FloatArray>>(0)[0].forEachIndexed { i, flt ->
                        //Only consider a pokemon when the accuracy is more than 40%
                        if (flt > .20)
                            pokelist.add(Pokemon(pokeArray[i], flt))

                           /* Snackbar.make(this, "Here's a Snackbar", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null)
                                    .show()*/
                    }

                    itemAdapter.setList(pokelist)
                    sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                    // {TODO} Update UI here by setting pokeList to the adapter
                }
                ?.addOnFailureListener {
                    //something went wrong
                    it.printStackTrace()

                }
    }
}
