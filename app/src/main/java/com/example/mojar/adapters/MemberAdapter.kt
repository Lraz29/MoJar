package com.example.mojar.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mojar.R
import com.example.mojar.models.User
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import java.util.Locale

/**
 * Adapter for the household members list.
 * handles dynamic user status updates using the TimeAgo library.
 */
class MemberAdapter(private val members: List<User>) :
    RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {


    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvEmail: TextView = view.findViewById(R.id.tvUserEmail)
        // TextView to display the last active status.
        val tvLastActive: TextView = view.findViewById(R.id.tvLastActive)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        // Inflate the item_user_card layout for each member in the list
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_card, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val user = members[position]

        holder.tvName.text = if (user.name.isNotEmpty()) user.name else "Unknown User"
        holder.tvEmail.text = user.email

        /**
         * Dynamic TimeAgo Logic:
         * Converts the Unix timestamp (Long) into a friendly string.
         */
        if (user.lastActive > 0) {
            // Configure TimeAgo to use English messages
            val messages = TimeAgoMessages.Builder()
                .withLocale(Locale.ENGLISH)
                .build()

            // Calculate elapsed time relative to current system time
            val timeAgo = TimeAgo.using(user.lastActive, messages)

            holder.tvLastActive.text = "Last updated: $timeAgo"
            holder.tvLastActive.visibility = View.VISIBLE
        } else {
            // Fallback for new users with no recorded activity yet
            holder.tvLastActive.text = "No recent activity"
            holder.tvLastActive.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = members.size
}