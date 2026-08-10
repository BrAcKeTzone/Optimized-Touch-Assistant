package com.bracketzone.touchassistant.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bracketzone.touchassistant.R

class AppPickerActivity : Activity() {

    companion object {
        const val EXTRA_SELECTED_PACKAGE = "extra_selected_package"
        const val EXTRA_RESULT_PACKAGE = "extra_result_package"
    }

    private var currentSelectedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_picker)

        currentSelectedPackage = intent.getStringExtra(EXTRA_SELECTED_PACKAGE)

        findViewById<TextView>(R.id.pickerTitle).text = "Select Application"
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        val pm = packageManager
        val installedApps = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        val recyclerView = findViewById<RecyclerView>(R.id.actionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AppsAdapter(installedApps)
    }

    private inner class AppsAdapter(
        private val appsList: List<ApplicationInfo>
    ) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_action_picker, parent, false)
            return AppViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            holder.bind(appsList[position])
        }

        override fun getItemCount(): Int = appsList.size

        inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.actionIcon)
            private val title: TextView = itemView.findViewById(R.id.actionName)
            private val desc: TextView = itemView.findViewById(R.id.actionDescription)
            private val checkmark: ImageView = itemView.findViewById(R.id.selectedCheckmark)

            fun bind(appInfo: ApplicationInfo) {
                val pm = packageManager
                title.text = pm.getApplicationLabel(appInfo)
                desc.text = appInfo.packageName
                icon.setImageDrawable(pm.getApplicationIcon(appInfo))

                val isSelected = (appInfo.packageName == currentSelectedPackage)
                checkmark.visibility = if (isSelected) View.VISIBLE else View.GONE

                if (isSelected) {
                    itemView.setBackgroundColor(Color.parseColor("#EBF5FF"))
                } else {
                    itemView.setBackgroundColor(Color.WHITE)
                }

                itemView.setOnClickListener {
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_RESULT_PACKAGE, appInfo.packageName)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    }
}