package com.hiennv.flutter_callkit_incoming

import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.*
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import com.hiennv.flutter_callkit_incoming.widgets.RippleRelativeLayout
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.OkHttpClient
import kotlin.math.abs


class CallkitIncomingActivity : Activity() {

    companion object {
        private var timer: CountDownTimer? = null
        const val ACTION_ENDED_CALL_INCOMING =
            "com.hiennv.flutter_callkit_incoming.ACTION_ENDED_CALL_INCOMING"

        const val ACTION_START_CALL_DURATION =
            "com.hiennv.flutter_callkit_incoming.ACTION_START_CALL_DURATION"

        fun getIntent(context: Context, data: Bundle) =
            Intent(CallkitConstants.ACTION_CALL_INCOMING).apply {
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_INCOMING}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

        fun getIntentEnded(context: Context, isAccepted: Boolean): Intent {
            val intent = Intent("${context.packageName}.${ACTION_ENDED_CALL_INCOMING}")
            intent.putExtra("ACCEPTED", isAccepted)
            return intent
        }
    }

    inner class EndedCallkitIncomingBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!isFinishing) {
                val isAccepted = intent.getBooleanExtra("ACCEPTED", false)
                if (isAccepted) {
//                    finishDelayed()
                } else {
                    finishTask()
                }
            }
        }
    }

    inner class StartCallDurationBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            tvDuration.base = SystemClock.elapsedRealtime()
            tvDuration.stop()
            tvDuration.start()
        }
    }

    private var endedCallkitIncomingBroadcastReceiver = EndedCallkitIncomingBroadcastReceiver()
    private var startCallDurationBroadcastReceiver = StartCallDurationBroadcastReceiver()


    private lateinit var ivBackground: ImageView
    private lateinit var llBackgroundAnimation: RippleRelativeLayout
    private lateinit var llDeclineAnimation: RippleRelativeLayout

    private lateinit var tvNameCaller: TextView
    private lateinit var tvDuration: Chronometer
    private lateinit var tvNumber: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var ivAvatar: CircleImageView

    private lateinit var llAction: LinearLayout
    private lateinit var ivAcceptCall: ImageView
    private lateinit var tvAccept: TextView

    private lateinit var ivDeclineCall: ImageView
    private lateinit var tvDecline: TextView

    private lateinit var loAccept: LinearLayout
    private lateinit var loDecline: LinearLayout
    private lateinit var atSpace: Space

    private lateinit var llActionCtl: LinearLayout

    private lateinit var llMic: LinearLayout
    private lateinit var ivMic: ImageView

    private lateinit var llSpeaker: LinearLayout
    private lateinit var ivSpeaker: ImageView

    private lateinit var llVideo: LinearLayout
    private lateinit var ivVideo: ImageView

    private lateinit var llUChat: LinearLayout
    private lateinit var ivUChat: ImageView

    private var isMute: Boolean = false
    private var isSpeaker: Boolean = false
    private var isVideo: Boolean = false

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = if (!Utils.isTablet(this@CallkitIncomingActivity)) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }
        transparentStatusAndNavigation()
        setContentView(R.layout.activity_callkit_incoming)
        initView(intent)
        incomingData(intent)
        registerReceiver(
            endedCallkitIncomingBroadcastReceiver,
            IntentFilter("${packageName}.${ACTION_ENDED_CALL_INCOMING}")
        )
        registerReceiver(
            startCallDurationBroadcastReceiver,
            IntentFilter("${packageName}.${ACTION_START_CALL_DURATION}")
        )
    }

    private fun wakeLockRequest(duration: Long) {

        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Callkit:PowerManager"
        )
        wakeLock.acquire(duration)
    }

    private fun transparentStatusAndNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setWindowFlag(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, true
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setWindowFlag(
                (WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION), false
            )
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
    }

    private fun setWindowFlag(bits: Int, on: Boolean) {
        val win: Window = window
        val winParams: WindowManager.LayoutParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }


    private fun incomingData(intent: Intent) {
        val data = intent.extras?.getBundle(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA)
        if (data == null) finish()

        tvNameCaller.text = data?.getString(CallkitConstants.EXTRA_CALLKIT_NAME_CALLER, "")
        tvNumber.text = data?.getString(CallkitConstants.EXTRA_CALLKIT_HANDLE, "")

//        val isShowLogo = data?.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_SHOW_LOGO, false)
//        ivLogo.visibility = if (isShowLogo == true) View.VISIBLE else View.INVISIBLE

        val avatarUrl = data?.getString(CallkitConstants.EXTRA_CALLKIT_AVATAR, "")
        if (avatarUrl != null && avatarUrl.isNotEmpty()) {
            ivAvatar.visibility = View.VISIBLE
            val headers =
                data.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
            getPicassoInstance(this@CallkitIncomingActivity, headers)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(ivAvatar)
        }

        val callType = data?.getInt(CallkitConstants.EXTRA_CALLKIT_TYPE, 0) ?: 0
        if (callType > 0) {
            ivAcceptCall.setImageResource(R.drawable.ic_video)
        }
        val duration = data?.getLong(CallkitConstants.EXTRA_CALLKIT_DURATION, 30L) ?: 30L
        wakeLockRequest(duration)
//        if (duration > 0L) {
//            timer = object: CountDownTimer(duration, 1000) {
//                override fun onTick(millisUntilFinished: Long) {}
//
//                override fun onFinish() {
//                    finishTask()
//                }
//            }
//            timer?.start()
//        }

//        finishTimeout(data, duration)

        val textAccept = data?.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_ACCEPT, "")
        tvAccept.text =
            if (TextUtils.isEmpty(textAccept)) getString(R.string.text_accept) else textAccept
        val textDecline = data?.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_DECLINE, "")
        tvDecline.text =
            if (TextUtils.isEmpty(textDecline)) getString(R.string.text_decline) else textDecline

        val backgroundColor =
            data?.getString(CallkitConstants.EXTRA_CALLKIT_BACKGROUND_COLOR, "#0955fa")
        try {
            ivBackground.setBackgroundColor(Color.parseColor(backgroundColor))
        } catch (error: Exception) {
        }
        var backgroundUrl = data?.getString(CallkitConstants.EXTRA_CALLKIT_AVATAR, "")
        if (backgroundUrl != null && backgroundUrl.isNotEmpty()) {
            val headers =
                data?.getSerializable(CallkitConstants.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
            getPicassoInstance(this@CallkitIncomingActivity, headers)
                .load(backgroundUrl)
                .placeholder(R.drawable.transparent)
                .error(R.drawable.transparent)
                .into(ivBackground)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ivBackground.setRenderEffect(
                RenderEffect.createBlurEffect(
                    20.0f, 20.0f, Shader.TileMode.CLAMP
                )
            )
        }
    }

    private fun finishTimeout(data: Bundle?, duration: Long) {
        val currentSystemTime = System.currentTimeMillis()
        val timeStartCall =
            data?.getLong(CallkitNotificationManager.EXTRA_TIME_START_CALL, currentSystemTime)
                ?: currentSystemTime

        val timeOut = duration - abs(currentSystemTime - timeStartCall)
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                finishTask()
            }
        }, timeOut)
    }

    private fun initView(intent: Intent) {
        ivBackground = findViewById(R.id.ivBackground)
        llBackgroundAnimation = findViewById(R.id.llBackgroundAnimation)
        llBackgroundAnimation.layoutParams.height =
            Utils.getScreenWidth() + Utils.getStatusBarHeight(this@CallkitIncomingActivity)
        llBackgroundAnimation.startRippleAnimation()

        llDeclineAnimation = findViewById(R.id.llDeclineAnimation)
        llDeclineAnimation.startRippleAnimation()


        tvNameCaller = findViewById(R.id.tvNameCaller)
        tvNumber = findViewById(R.id.tvNumber)
        ivLogo = findViewById(R.id.ivLogo)
        ivAvatar = findViewById(R.id.ivAvatar)
        tvDuration = findViewById(R.id.tvDuration)
        llAction = findViewById(R.id.llAction)
        loAccept = findViewById(R.id.loAccept)
        loDecline = findViewById(R.id.loDecline)
        atSpace = findViewById(R.id.atSpace)

        val params = llAction.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(0, 0, 0, Utils.getNavigationBarHeight(this@CallkitIncomingActivity))
        llAction.layoutParams = params

        ivAcceptCall = findViewById(R.id.ivAcceptCall)
        tvAccept = findViewById(R.id.tvAccept)
        ivDeclineCall = findViewById(R.id.ivDeclineCall)
        tvDecline = findViewById(R.id.tvDecline)
        tvDuration.visibility = View.INVISIBLE
        animateAcceptCall()

        ivAcceptCall.setOnClickListener {
            onAcceptClick()
        }
        ivDeclineCall.setOnClickListener {
            onDeclineClick()
        }

        llActionCtl = findViewById(R.id.llActionCtl)
        llMic = findViewById(R.id.llMic)
        llSpeaker = findViewById(R.id.llSpeaker)
        llVideo = findViewById(R.id.llVideo)
        llUChat = findViewById(R.id.llUChat)

        ivMic = findViewById(R.id.ivMic)
        ivSpeaker = findViewById(R.id.ivSpeaker)
        ivVideo = findViewById(R.id.ivVideo)
        ivUChat = findViewById(R.id.ivUChat)

        ivMic.setImageResource(R.drawable.ic_mic_on)
        ivSpeaker.setImageResource(R.drawable.ic_speaker_volume_down)
        ivVideo.setImageResource(R.drawable.ic_video_off)
        ivUChat.setImageResource(R.drawable.ic_logo_without_text)

        ivMic.setOnClickListener {
            onMuteMicClick()
        }
        ivSpeaker.setOnClickListener {
            onSpeakerClick()
        }
        ivVideo.setOnClickListener {
            onVideoClick()
        }
        ivUChat.setOnClickListener {
            onUChatClick()
        }

        val data = intent.extras?.getBundle(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA)
        val isAccepted = data?.getBoolean("isAccept") ?: false
        if (isAccepted) {
            initialAcceptCallUI()
        }
    }

    private fun animateAcceptCall() {
        val shakeAnimation =
            AnimationUtils.loadAnimation(this@CallkitIncomingActivity, R.anim.shake_anim)
        ivAcceptCall.animation = shakeAnimation
    }

    private fun initialAcceptCallUI() {
        timer?.cancel()
        tvDecline.visibility = View.GONE
        loAccept.visibility = View.GONE
        loDecline.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        atSpace.visibility = View.GONE
        llActionCtl.visibility = View.VISIBLE
        tvDuration.visibility = View.VISIBLE
        tvDuration.base = SystemClock.elapsedRealtime()
        tvDuration.stop()
        llBackgroundAnimation.stopRippleAnimation()
        llDeclineAnimation.stopRippleAnimation()
    }

    private fun onAcceptClick() {
        initialAcceptCallUI()
        val data = intent.extras?.getBundle(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA)
        data?.putBoolean("fullScreen", true)
        val acceptIntent =
            TransparentActivity.getIntent(this, CallkitConstants.ACTION_CALL_ACCEPT, data)
        startActivity(acceptIntent)
//        dismissKeyguard()
//        finish()
    }

    private fun dismissKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }

    fun onDeclineClick() {
        val data = intent.extras?.getBundle(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA)
        val intentSelector: Intent? = if (loAccept.visibility == View.GONE) {
            CallkitIncomingBroadcastReceiver.getIntentEnded(this@CallkitIncomingActivity, data)
        } else {
            CallkitIncomingBroadcastReceiver.getIntentDecline(this@CallkitIncomingActivity, data)
        }
        sendBroadcast(intentSelector)
        finishTask()
    }

    private fun onMuteMicClick() {
        isMute = !isMute
        if (isMute) {
            ivMic.setImageResource(R.drawable.ic_mic_off_red)
            ivMic.setBackgroundResource(R.drawable.bg_action_circle_active)
        } else {
            ivMic.setImageResource(R.drawable.ic_mic_on);
            ivMic.setBackgroundResource(R.drawable.bg_action_circle)
        }
        FlutterCallkitIncomingPlugin.getInstance().sendEventCustom(mapOf("isMuted" to isMute))
    }

    private fun onSpeakerClick() {
        isSpeaker = !isSpeaker
        if (isSpeaker) {
            ivSpeaker.setImageResource(R.drawable.ic_speaker_volume_up)
            ivSpeaker.setBackgroundResource(R.drawable.bg_action_circle_active)
            val padding = resources.getDimensionPixelOffset(R.dimen.call_action_v1)
            ivSpeaker.setPadding(padding, padding, padding, padding)
        } else {
            ivSpeaker.setImageResource(R.drawable.ic_speaker_volume_down)
            ivSpeaker.setBackgroundResource(R.drawable.bg_action_circle)
            val padding = resources.getDimensionPixelOffset(R.dimen.call_action_v2)
            ivSpeaker.setPadding(padding, padding, padding, padding)
        }
        FlutterCallkitIncomingPlugin.getInstance().sendEventCustom(mapOf("isSpeaker" to isSpeaker))
    }

    private fun onVideoClick() {
        isVideo = !isVideo
        if (isSpeaker) {
            ivVideo.setImageResource(R.drawable.ic_video_on)
            ivVideo.setBackgroundResource(R.drawable.bg_action_circle_active)
        } else {
            ivVideo.setImageResource(R.drawable.ic_video_off)
            ivVideo.setBackgroundResource(R.drawable.bg_action_circle)
        }
        val data = intent.extras?.getBundle(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA)
        FlutterCallkitIncomingPlugin.getInstance().sendEventCustom(mapOf("isVideo" to isVideo))

        val acceptIntent: Intent? = AppUtils.getAppIntent(this@CallkitIncomingActivity, data = data)
        startActivity(acceptIntent)
        dismissKeyguard()
        finish()
    }

    private fun onUChatClick() {
        val data = intent.extras?.getBundle(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA)
        val acceptIntent: Intent? = AppUtils.getAppIntent(this@CallkitIncomingActivity, data = data)
        startActivity(acceptIntent)
        dismissKeyguard()
        finish()
    }

    private fun finishDelayed() {
        Handler(Looper.getMainLooper()).postDelayed({
            finishTask()
        }, 1000)
    }

    private fun finishTask() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    private fun getPicassoInstance(context: Context, headers: HashMap<String, Any?>): Picasso {
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor { chain ->
                val newRequestBuilder: okhttp3.Request.Builder = chain.request().newBuilder()
                for ((key, value) in headers) {
                    newRequestBuilder.addHeader(key, value.toString())
                }
                chain.proceed(newRequestBuilder.build())
            }
            .build()
        return Picasso.Builder(context)
            .downloader(OkHttp3Downloader(client))
            .build()
    }

    override fun onDestroy() {
        unregisterReceiver(endedCallkitIncomingBroadcastReceiver)
        unregisterReceiver(startCallDurationBroadcastReceiver)
        super.onDestroy()
    }

    override fun onBackPressed() {}
}
