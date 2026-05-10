package com.example.instagram.Fragments

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.instagram.Adapters.GalleryAdapter
import com.example.instagram.MainActivity
import com.example.instagram.Models.Users
import com.example.instagram.R
import com.example.instagram.databinding.FragmentPostBinding
import com.example.instagram.Post.Post as CreatePostActivity

class Post : Fragment() {

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    private val READ_REQUEST_CODE = 1
    private var currentOrder = 1

    private val selectedImagesOrder = mutableMapOf<Uri, Int>()
    private val imageUris = mutableListOf<Users>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPostBinding.inflate(inflater, container, false)

        setupClicks()
        binding.selectedImageView.setImageResource(R.drawable.photos)

        requestGalleryPermissionOrLoad()

        return binding.root
    }

    private fun setupClicks() {
        binding.nextPostt.setOnClickListener {
            if (selectedImagesOrder.isNotEmpty()) {
                navigateToPost()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please select at least one image.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.cross.setOnClickListener {
            goBackToMain()
        }
    }

    private fun requestGalleryPermissionOrLoad() {
        val permissions = when {
            Build.VERSION.SDK_INT >= 34 -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            else -> {
                emptyArray()
            }
        }

        if (permissions.isEmpty()) {
            loadImages()
            return
        }

        val isAnyPermissionGranted = permissions.any { permission ->
            requireContext().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }

        if (isAnyPermissionGranted) {
            loadImages()
        } else {
            requestPermissions(permissions, READ_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == READ_REQUEST_CODE) {
            val isGranted = grantResults.any { result ->
                result == PackageManager.PERMISSION_GRANTED
            }

            if (isGranted) {
                loadImages()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Gallery permission denied. Please allow gallery access.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadImages() {
        imageUris.clear()

        val contentResolver: ContentResolver = requireContext().contentResolver
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = contentResolver.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)

                val imageUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                imageUris.add(Users(imageUri = imageUri))
            }
        }

        if (imageUris.isNotEmpty()) {
            binding.selectedImageView.setImageURI(imageUris[0].imageUri)
            setupRecyclerView(imageUris)
        } else {
            Toast.makeText(
                requireContext(),
                "No images found in gallery.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupRecyclerView(imageUris: List<Users>) {
        val adapter = GalleryAdapter(
            requireContext(),
            imageUris,
            { selectedUri ->

                val index = imageUris.indexOfFirst { user ->
                    user.imageUri == selectedUri
                }

                if (index != -1) {
                    binding.selectedImageView.setImageURI(imageUris[index].imageUri)
                }

                if (selectedImagesOrder.containsKey(selectedUri)) {
                    selectedImagesOrder.remove(selectedUri)
                    refreshOrder()

                    Toast.makeText(
                        requireContext(),
                        "Image Count: ${selectedImagesOrder.count()}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    selectedImagesOrder[selectedUri] = currentOrder++
                }

                binding.galleryRecyclerView.adapter?.notifyDataSetChanged()
            },
            selectedImagesOrder
        )

        binding.galleryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.galleryRecyclerView.setHasFixedSize(true)
        binding.galleryRecyclerView.adapter = adapter
    }

    private fun navigateToPost() {
        val selectedImageList = selectedImagesOrder.keys.map { uri ->
            Users(imageUri = uri)
        }

        val intent = Intent(requireContext(), CreatePostActivity::class.java)
        intent.putParcelableArrayListExtra(
            "selected_images",
            ArrayList(selectedImageList)
        )

        startActivity(intent)
    }

    private fun refreshOrder() {
        currentOrder = 1

        selectedImagesOrder.keys.forEach { key ->
            selectedImagesOrder[key] = currentOrder++
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            goBackToMain()
        }
    }

    private fun goBackToMain() {
        startActivity(Intent(requireContext(), MainActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}