package com.example.instagram.Filter

import android.content.Context
import android.widget.Filter
import android.widget.Toast
import com.example.instagram.Adapters.SearchAdapter
import com.example.instagram.Models.PostModel
import java.util.Locale

class FilterHome(
    val adapter: SearchAdapter,
    val filterProducts: ArrayList<PostModel>,
    val context: Context
) : Filter() {

    override fun performFiltering(searchingText: CharSequence?): FilterResults {
        val filterResults = FilterResults()
        val filterProductList = ArrayList<PostModel>()

        if (!searchingText.isNullOrEmpty()) {
            val query = searchingText.toString().trim().uppercase(Locale.getDefault()).split(" ")

            for (post in filterProducts) {
                if (query.any { search ->
                        post.userName?.uppercase(Locale.getDefault())?.contains(search) == true
                    }) {
                    filterProductList.add(post)
                }
            }

            // ✅ Agar filtered list empty hai, toh "No Results" ka Toast dikhao
            if (filterProductList.isEmpty()) {
                Toast.makeText(context, "No matching results", Toast.LENGTH_SHORT).show()
            } else {
                // ✅ Sirf match hone wale users ka Toast dikhana hai
                filterProductList.forEach {
                    Toast.makeText(context, "Matched: ${it.userName}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            filterProductList.addAll(filterProducts) // Agar input empty ho toh pura list dikhao
        }

        filterResults.apply {
            count = filterProductList.size
            values = filterProductList
        }
        return filterResults
    }

    override fun publishResults(p0: CharSequence?, results: FilterResults?) {
        adapter.differ.submitList((results?.values as? ArrayList<PostModel>)?.toList())
        adapter.notifyDataSetChanged() // ✅ Ensure UI updates properly
    }
}

