package com.example.instagram.Filter

import android.content.Context
import android.widget.Filter
import android.widget.Toast
import com.example.instagram.Adapters.ChatFragmentAdapter
import com.example.instagram.Models.PostModel
import java.util.Locale

class FilterChat(
     val adapter: ChatFragmentAdapter,
    val  original: ArrayList<PostModel>,
    val  context: Context ): Filter()
{
    override fun performFiltering(searchingText: CharSequence?): FilterResults {
        val filterResults = FilterResults()
        val filterProductList = ArrayList<PostModel>()

        if (!searchingText.isNullOrEmpty()) {
            val query = searchingText.toString().trim().uppercase(Locale.getDefault()).split(" ")

            for (post in original) {
                if (query.any { search ->
                        post.userName?.uppercase(Locale.getDefault())?.contains(search) == true
                    }) {
                    filterProductList.add(post)
                }
            }

            // ✅ Agar filtered list empty hai, toh "No Results" ka Toast dikhao
            if (filterProductList.isEmpty()) {
                Toast.makeText(context, "No matching results", Toast.LENGTH_SHORT).show()
            }
        } else {
            filterProductList.addAll(original) // Agar input empty ho toh pura list dikhao
        }

        filterResults.apply {
            count = filterProductList.size
            values = filterProductList
        }
        return filterResults

    }

    override fun publishResults(p0: CharSequence?, results: FilterResults?) {
        adapter.differ.submitList((results?.values as? ArrayList<PostModel>)?.toList())
        adapter.notifyDataSetChanged() // ✅ Ensure UI updates properly    }
}
}