@file:OptIn(ExperimentalStdlibApi::class)

package com.mrboomdev.awery.ui.mobile.screens.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.textview.MaterialTextView
import com.mrboomdev.awery.BuildConfig
import com.mrboomdev.awery.R
import com.mrboomdev.awery.app.App.Companion.getMoshi
import com.mrboomdev.awery.app.App.Companion.i18n
import com.mrboomdev.awery.app.App.Companion.isLandscape
import com.mrboomdev.awery.app.App.Companion.openUrl
import com.mrboomdev.awery.databinding.ScreenAboutBinding
import com.mrboomdev.awery.ui.mobile.screens.settings.AboutActivity.Contributor
import com.mrboomdev.awery.util.extensions.UI_INSETS
import com.mrboomdev.awery.util.extensions.applyInsets
import com.mrboomdev.awery.util.extensions.applyTheme
import com.mrboomdev.awery.util.extensions.bottomMargin
import com.mrboomdev.awery.util.extensions.dpPx
import com.mrboomdev.awery.util.extensions.enableEdgeToEdge
import com.mrboomdev.awery.util.extensions.leftMargin
import com.mrboomdev.awery.util.extensions.resolveAttrColor
import com.mrboomdev.awery.util.extensions.setImageTintAttr
import com.mrboomdev.awery.util.extensions.setMarkwon
import com.mrboomdev.awery.util.extensions.setPadding
import com.mrboomdev.awery.util.extensions.topMargin
import com.mrboomdev.awery.util.io.HttpCacheMode
import com.mrboomdev.awery.util.io.HttpClient.fetch
import com.mrboomdev.awery.util.io.HttpRequest
import com.squareup.moshi.Json
import com.squareup.moshi.adapter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

private const val TAG = "AboutActivity"
private const val URL = "https://api.github.com/repos/MrBoomDeveloper/Awery/contributors?per_page=100&page=0"
private val EXCLUDE_GITHUB_USERS = arrayOf("MrBoomDeveloper", "weblate")

private val LOCAL_DEVS = arrayOf(
	Contributor(
		"MrBoomDev", arrayOf("Main Developer"), "https://github.com/MrBoomDeveloper",
		"https://cdn.discordapp.com/avatars/1034891767822176357/3420c6a4d16fe513a69c85d86cb206c2.png?size=4096"
	),

	Contributor(
		"Ichiro", arrayOf("App Icon"),
		"https://discord.com/channels/@me/1262060731981889536",
		"https://cdn.discordapp.com/avatars/778503249619058689/9d5baf6943f4eafbaf09eb8e9e287f2d.png?size=4096"
	)
)

class AboutActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		setContentView(ScreenAboutBinding.inflate(layoutInflater).apply {
			root.setBackgroundColor(resolveAttrColor(android.R.attr.colorBackground))
			back.setOnClickListener { finish() }

			version.text = arrayOf(
				"${i18n(R.string.version)}: ${BuildConfig.VERSION_NAME}",
				"${i18n(R.string.built_at)}: ${Date(BuildConfig.BUILD_TIME)}"
			).joinToString("\n")

			info.fundMessage.setMarkwon(
				info.fundMessage.text.toString())

			applyInsets(UI_INSETS, { _, insets ->
				if(isLandscape) {
					root.setPadding(insets.left, insets.top, insets.right, insets.bottom)
					window.navigationBarColor = resolveAttrColor(android.R.attr.colorBackground)
				} else {
					root.setPadding(insets.left, 0)
					header.topMargin = insets.top
				}

				true
			})

			val items = LOCAL_DEVS.toMutableList()

			class ViewHolder(val view: ContributorView): RecyclerView.ViewHolder(view)

			info.recycler.adapter = object : RecyclerView.Adapter<ViewHolder>() {
				override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
					ViewHolder(ContributorView(parent.context))

				override fun getItemCount() = items.size

				override fun onBindViewHolder(holder: ViewHolder, position: Int) {
					holder.view.contributor = items[position]
				}
			}

			lifecycleScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, t ->
				Log.e(TAG, "Failed to load contributors list", t)
			}) {
				val receivedItems = HttpRequest(URL).apply {
					cacheMode = HttpCacheMode.CACHE_FIRST
					cacheDuration = 60 * 60 * 24 * 7 /** 7 days **/
				}.fetch().let {
					getMoshi().adapter<List<GitHubContributor>>().fromJson(it.text)!!
				}.filter {
					it.login !in EXCLUDE_GITHUB_USERS
				}.map { it.toContributor() }

				withContext(Dispatchers.Main) {
					items.addAll(receivedItems)
					info.recycler.adapter!!.notifyItemRangeInserted(2, receivedItems.size)
				}
			}
		}.root)
	}

	@SuppressLint("ViewConstructor")
	class ContributorView(context: Context) : LinearLayoutCompat(context) {
		private val name: MaterialTextView
		private val roles: MaterialTextView
		private val icon: AppCompatImageView
		private val linear: LinearLayoutCompat

		var contributor: Contributor? = null
			set(value) {
				field = value

				if(value != null) {
					Glide.with(icon)
						.load(value.avatar)
						.transition(DrawableTransitionOptions.withCrossFade())
						.into(icon)

					name.text = value.name
					roles.text = value.roles.joinToString(", ")
					setOnClickListener { openUrl(context, value.url) }
				}
			}

		init {
			orientation = VERTICAL

			linear = LinearLayoutCompat(context).apply {
				orientation = HORIZONTAL
				setBackgroundResource(R.drawable.ripple_round_you)
				this@ContributorView.addView(this, MATCH_PARENT, WRAP_CONTENT)
				setPadding(dpPx(8f))
				bottomMargin = dpPx(4f)

				isClickable = true
				isFocusable = true
			}

			val iconWrapper = CardView(context).apply {
				radius = dpPx(48f).toFloat()
				linear.addView(this)
			}

			icon = AppCompatImageView(context).apply {
				iconWrapper.addView(this, dpPx(48f), dpPx(48f))
			}

			val info = LinearLayoutCompat(context).apply {
				orientation = VERTICAL
				linear.addView(this)
				leftMargin = dpPx(16f)
			}

			name = MaterialTextView(context).apply {
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
				setTextColor(context.resolveAttrColor(com.google.android.material.R.attr.colorOnBackground))
				info.addView(this)
				bottomMargin = dpPx(4f)
			}

			roles = MaterialTextView(context).apply {
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
				info.addView(this)
			}
		}
	}

	class SocialView @JvmOverloads constructor(
		context: Context,
		attrs: AttributeSet? = null,
		defStyleAttr: Int = 0
	) : LinearLayoutCompat(context, attrs, defStyleAttr) {

		init {
			val linear = LinearLayoutCompat(context).apply {
				gravity = Gravity.CENTER_HORIZONTAL
				orientation = VERTICAL
				isClickable = true
				isFocusable = true
				setBackgroundResource(R.drawable.ripple_round_you)
				this@SocialView.addView(this)
				setPadding(dpPx(12f), dpPx(8f))
			}

			val icon = AppCompatImageView(context).apply {
				linear.addView(this, dpPx(42f), dpPx(42f))
				setImageTintAttr(com.google.android.material.R.attr.colorOnSecondaryContainer)
			}

			val label = MaterialTextView(context).apply {
				linear.addView(this, WRAP_CONTENT, WRAP_CONTENT)
				topMargin = dpPx(4f)
			}

			if(attrs != null) {
				context.obtainStyledAttributes(attrs, R.styleable.SocialView).use { typed ->
					icon.setImageDrawable(typed.getDrawable(R.styleable.SocialView_socialIcon))
					label.text = typed.getString(R.styleable.SocialView_socialName)

					typed.getString(R.styleable.SocialView_socialLink)?.let { url ->
						linear.setOnClickListener { openUrl(context, url) }
					}
				}
			}
		}
	}

	class Contributor(
		val name: String,
		val roles: Array<String>,
		val url: String,
		val avatar: String
	)

	class GitHubContributor(
		val login: String,
		@Json(name = "avatar_url")
		val avatarUrl: String,
		@Json(name = "html_url")
		val htmlUrl: String
	) {
		fun toContributor() = Contributor(
			name = login,
			roles = arrayOf(),
			url = htmlUrl,
			avatar = avatarUrl
		)
	}
}