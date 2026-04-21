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
import java.util.Locale
import java.util.TimeZone

class SupportAdapter : ListAdapter<SupportMessage, SupportAdapter.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).from == "user") R.layout.item_support_message_user
        else R.layout.item_support_message_admin

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.message_text)
        private val time: TextView = view.findViewById(R.id.time_text)
        fun bind(m: SupportMessage) {
            text.text = m.text
            time.text = formatTime(m.ts)
        }
    }

    object Diff : DiffUtil.ItemCallback<SupportMessage>() {
        override fun areItemsTheSame(old: SupportMessage, new: SupportMessage) = old.id == new.id
        override fun areContentsTheSame(old: SupportMessage, new: SupportMessage) = old == new
    }
}

private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US)
    .apply { timeZone = TimeZone.getTimeZone("UTC") }
private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun formatTime(iso: String): String = try {
    isoParser.parse(iso)?.let(timeFormatter::format) ?: iso
} catch (_: Exception) {
    iso
}
