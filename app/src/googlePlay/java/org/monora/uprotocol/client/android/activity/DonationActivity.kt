package org.monora.uprotocol.client.android.activity

import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.fragment.external.DonationListFragment

/**
 * created by: veli
 * date: 7/12/18 10:32 PM
 */
class DonationActivity : Activity() {
    private val animation: Animation by lazy {
        AlphaAnimation(0.0f, 1.0f).also {
            it.duration = 3000
            it.repeatCount = Animation.INFINITE
            it.repeatMode = Animation.REVERSE

            it.setAnimationListener(object : Animation.AnimationListener {
                var animPoint = 0

                var textPoint = 0

                override fun onAnimationStart(animation: Animation) {
                    animPoint = 0
                    textPoint = 0
                    developerText.text = texts[textPoint]

                    changeSpeed()
                }

                override fun onAnimationEnd(animation: Animation) {

                }

                override fun onAnimationRepeat(animation: Animation) {
                    animPoint++

                    if (animPoint % 2 == 0) {
                        animPoint = 0
                        textPoint = if (textPoint + 1 >= texts.size) 0 else textPoint + 1
                        developerText.text = texts[textPoint]

                        changeSpeed()
                    }
                }

                fun changeSpeed() {
                    animation.duration = (30 * developerText.text.length).toLong()
                }
            })
        }
    }

    private val developerText: TextView by lazy {
        findViewById(R.id.developerText)
    }

    private val texts: Array<String> by lazy {
        getString(R.string.text_prefaceDonation).split(";").toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donation)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val contributorsListFragment = supportFragmentManager.findFragmentById(
            R.id.activity_about_contributors_fragment
        ) as DonationListFragment?

        contributorsListFragment?.listView?.isNestedScrollingEnabled = false
    }

    override fun onResume() {
        super.onResume()
        developerText.animation = animation
    }

    override fun onPause() {
        super.onPause()
        developerText.animation = null
    }
}