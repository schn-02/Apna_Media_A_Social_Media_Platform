package com.example.instagram.Fragments

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.recyclerview.widget.GridLayoutManager
import com.example.instagram.Adapters.GalleryAdapter
import com.example.instagram.MainActivity
import com.example.instagram.Models.Users
import com.example.instagram.Post.Post
import com.example.instagram.R
import com.example.instagram.databinding.FragmentPostBinding


class Post : Fragment() {

    private lateinit var _binding: FragmentPostBinding
    private val binding get() = _binding!!

    private val READ_REQUEST_CODE = 1
    private var currentOrder = 1

    private val selectedImagesOrder = mutableMapOf<Uri, Int>()
    val imageUris = mutableListOf<Users>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostBinding.inflate(inflater, container, false)

        binding.nextPostt.setOnClickListener {
            if (selectedImagesOrder.isNotEmpty()) {
                navigateToPost()
            } else {
                Toast.makeText(requireContext(), "Please Select some images.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.selectedImageView.setImageResource(R.drawable.photos)

        binding.cross.setOnClickListener {
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (requireContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_REQUEST_CODE)
            } else {
                loadImages()
            }
        }

        return binding.root
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImages()
            } else {
                Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadImages() {
        val contentResolver: ContentResolver = requireContext().contentResolver
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)

        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")
        if (cursor != null && cursor.count > 0) {
            cursor.use {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    imageUris.add(Users(imageUri = imageUri))
                }
            }

            if (imageUris.isNotEmpty() && currentOrder < imageUris.size) {
                binding.selectedImageView.setImageURI(imageUris[currentOrder].imageUri)
            } else {
                Toast.makeText(requireContext(), "No images found or invalid index", Toast.LENGTH_SHORT).show()
            }

            setupRecyclerView(imageUris)
        } else {
            Toast.makeText(requireContext(), "No images found or cursor error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView(imageUris: List<Users>) {
        if (imageUris.isNotEmpty()) {
            val adapter = GalleryAdapter(requireContext(), imageUris, { selectedUri ->
                val index = imageUris.indexOfFirst { it.imageUri == selectedUri }

                if (index != -1) {
                    binding.selectedImageView.setImageURI(imageUris[index].imageUri)
                }

                if (selectedImagesOrder.containsKey(selectedUri)) {
                    selectedImagesOrder.remove(selectedUri)
                    refreshOrder()
                    Toast.makeText(requireContext(), "image Count: ${selectedImagesOrder.count()}", Toast.LENGTH_SHORT).show()
                } else {
                    selectedImagesOrder[selectedUri] = currentOrder++
                }

                binding.galleryRecyclerView.adapter?.notifyDataSetChanged()
            }, selectedImagesOrder)

            binding.galleryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
            binding.galleryRecyclerView.setHasFixedSize(true)
            binding.galleryRecyclerView.adapter = adapter
        } else {
            Toast.makeText(requireContext(), "No images available to display", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToPost() {
        val selectedImageList = selectedImagesOrder.keys.map { uri ->
            Users(uri)
        }

        val intent = Intent(requireContext(), Post::class.java)
        intent.putParcelableArrayListExtra("selected_images", ArrayList(selectedImageList))
        startActivity(intent)
    }

    private fun refreshOrder() {
        currentOrder = 1
        selectedImagesOrder.keys.forEach { key ->
            selectedImagesOrder[key] = currentOrder++
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){

            startActivity(Intent(requireContext() , MainActivity::class.java))
            requireActivity().finish()
        }
    }
}

