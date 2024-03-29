package com.whocooler.app.UserProfile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.whocooler.app.Common.Models.User
import com.whocooler.app.R
import com.squareup.picasso.Picasso
import com.whocooler.app.Common.Helpers.FileUtils
import com.whocooler.app.Common.Helpers.PendingDownloadContent
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class UserProfileActivity: AppCompatActivity(), UserProfileContracts.PresenterViewContract {

    lateinit var interactor: UserProfileContracts.ViewInteractorContract
    lateinit var router: UserProfileContracts.RouterInterface
    private var user: User? = null

    companion object {
        private val IMAGE_PICK_CODE = 1000;
        private val PERMISSION_CODE = 1001;
    }

    private fun setupModule() {
        var activity = this
        var interactor = UserProfileInteractor()
        var presenter = UserProfilePresenter()
        var router = UserProfileRouter()

        activity.interactor = interactor
        activity.router = router
        interactor.presenter = presenter
        presenter.activity = activity
        router.activity = activity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setupModule()
        setContentView(R.layout.activity_user_profile)

        setupOnClickListeners()
        interactor.getProfile()
        setupActionBar()
    }

    private fun setupOnClickListeners() {
        val changeName = findViewById<TextView>(R.id.profile_edit_name)
        changeName.setOnClickListener {
            showEditNameAlert()
        }

        val changeAvatar = findViewById<TextView>(R.id.profile_change_avatar)
        changeAvatar.setOnClickListener {
            tryToGetImageFromUser()
        }
    }

    private fun tryToGetImageFromUser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                requestPermissions(permissions, PERMISSION_CODE);
            } else {
                pickImageFromGallery();
            }
        } else {
            //system OS is < Marshmallow
            pickImageFromGallery();
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                updateUserImage(data)
            }
        }
    }

    @Throws(IOException::class)
    private fun updateUserImage(data: Intent?) {
        val clipData = data?.clipData
        val path: Any? = if (clipData != null) {
            val paths = ArrayList<Any>()
            for (i in 0 until clipData.itemCount) {
                val path = FileUtils.getPath(this, clipData.getItemAt(i).uri)
                if (path != null) {
                    paths.add(path)
                }
            }
            paths.firstOrNull()
        } else {
            FileUtils.getPath(this, data?.data)
        }

        val file = when (path) {
            is PendingDownloadContent -> {
                val fileName = UUID.randomUUID().toString() + path.fileName
                FileUtils.copyUriToFile(this, path.uri, fileName)
            }
            is String -> {
                File(path)
            }
            else -> null
        } ?: return

        val uri = Uri.fromFile(file)
        val imageStream = contentResolver.openInputStream(uri)
        var imageBitmap = BitmapFactory.decodeStream(imageStream)

        imageBitmap = modifyOrientation(imageBitmap, file.absolutePath)

        val byteStream = ByteArrayOutputStream()
        val compressedBitmap = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.width / 4, imageBitmap.height / 4, false)

        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteStream)

        interactor.updateUserAvatar(byteStream.toByteArray())
    }

    private fun showEditNameAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.profile_change_name)

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(user?.name)
        builder.setView(input)

        builder.setPositiveButton(R.string.ok, DialogInterface.OnClickListener { dialog, which ->
            interactor.updateUserName(input.text.toString())
        })

        builder.setNegativeButton(R.string.cancel, DialogInterface.OnClickListener { dialog, which ->
            dialog.cancel()
        })

        val dialog = builder.create()

        dialog.show()
    }

    override fun displayProfile(user: User) {
        this.user = user

        val userAvatarView = findViewById<ImageView>(R.id.rounded_user_image)
        val namePlaceholder = findViewById<TextView>(R.id.profile_name_placeholder)
        val userName = findViewById<TextView>(R.id.profile_name_txt)
        val feedback = findViewById<TextView>(R.id.profile_feedback)
        val feedbackImg = findViewById<ImageView>(R.id.profile_feedback_image)
        val logoutBtn = findViewById<TextView>(R.id.profile_logout_btn)
        val logoutImg = findViewById<ImageView>(R.id.profile_logout_image)
        val privacyPolicyTextView = findViewById<TextView>(R.id.profile_privacy_policy)
        val privacyPolicyImg = findViewById<ImageView>(R.id.profile_privacy_image)

        Picasso.get().load(user.avatar).into(userAvatarView)
        namePlaceholder.text = getString(R.string.profile_name)
        userName.text = user.name
        logoutBtn.text = getString(R.string.profile_logout)
        feedback.text = getString(R.string.profile_feedback)
        privacyPolicyTextView.text = getString(R.string.privacy_policy)

        feedback.setOnClickListener {
            navigateToFeedback()
        }

        feedbackImg.setOnClickListener {
            navigateToFeedback()
        }

        logoutImg.setOnClickListener {
            showLogoutAlertDialog()
        }

        logoutBtn.setOnClickListener {
            showLogoutAlertDialog()
        }

        privacyPolicyTextView.setOnClickListener {
            openUrl("https://www.iubenda.com/privacy-policy/66454455", this)
        }

        privacyPolicyImg.setOnClickListener {
            openUrl("https://www.iubenda.com/privacy-policy/66454455", this)
        }
    }

    private fun navigateToFeedback() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:whocoolerfeedback@gmail.com?subject=WhoCooler Support")
        }
        startActivity(Intent.createChooser(emailIntent, "Send feedback"))
    }

    private fun showLogoutAlertDialog() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(getString(R.string.profile_logout_alert))
        builder.setPositiveButton(getString(R.string.yes)) { dialog, which ->
            interactor.logout(this)
        }
        builder.setNegativeButton(getString(R.string.no)) {_,_ ->}

        builder.create().show()
    }

    private fun openUrl(url: String, context: Context) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    override fun navigateToDebateList() {
        router?.navigateToDebateList()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImageFromGallery()
                } else {
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun showErrorToast(message: String) {
        Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
    }

    // Prepares nav bar
    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.profile_toolbar))
        supportActionBar?.setTitle("")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // Handles navigation back
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun modifyOrientation(bitmap: Bitmap, image_absolute_path: String): Bitmap {
        val ei = ExifInterface(image_absolute_path)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipImage(bitmap, true, false)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipImage(bitmap, false, true)
            else -> bitmap
        }
    }

    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun flipImage(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.preScale((if (horizontal) -1 else 1).toFloat(), (if (vertical) -1 else 1).toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun getRealPathFromURI(contentURI: Uri): String? {
        val result: String?
        val cursor: Cursor? = contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.path
        } else {
            cursor.moveToFirst()
            val idx: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }

}