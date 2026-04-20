package com.betnow.app.ui.support

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.betnow.app.R
import com.betnow.app.network.models.SupportMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SupportAdapter : ListAdapter<SupportMessage, RecyclerView.ViewHolder>(Diff) {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_ADMIN = 1
    }

    object Diff : DiffUtil.ItemCallback<SupportMessage>() {
        override fun areItemsTheSame(old: SupportMessage, new: SupportMessage) = old.id == new.id
        override fun areContentsTheSame(old: SupportMessage, new: SupportMessage) = old == new
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).from == "user") TYPE_USER else TYPE_ADMIN

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserVH(inflater.inflate(R.layout.item_support_message_user, parent, false))
        } else {
            AdminVH(inflater.inflate(R.layout.item_support_message_admin, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val m = getItem(position)
        when (holder) {
            is UserVH -> holder.bind(m)
            is AdminVH -> holder.bind(m)
        }
    }

    class UserVH(v: View) : RecyclerView.ViewHolder(v) {
        private val text: TextView = v.findViewById(R.id.message_text)
        private val time: TextView = v.findViewById(R.id.time_text)
        fun bind(m: SupportMessage) {
            text.text = m.text
            time.text = formatTime(m.ts)
        }
    }

    class AdminVH(v: View) : RecyclerView.ViewHolder(v) {
        private val text: TextView = v.findViewById(R.id.message_text)
        private val time: TextView = v.findViewById(R.id.time_text)
        fun bind(m: SupportMessage) {
            text.text = m.text
            time.text = formatTime(m.ts)
        }
    }
}

private val isoParser: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US)
    .apply { timeZone = TimeZone.getTimeZone("UTC") }
private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun formatTime(iso: String): String {
    return try {
        val date: Date = isoParser.parse(iso) ?: return iso
        timeFormatter.format(date)
    } catch (_: Exception) {
        iso
    }
}
