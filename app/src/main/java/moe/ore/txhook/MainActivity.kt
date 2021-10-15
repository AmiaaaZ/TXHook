package moe.ore.txhook

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.transition.Slide
import android.view.Gravity
import android.view.KeyEvent
import android.view.View.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager.widget.ViewPager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.guidview.GuideCaseView
import moe.ore.txhook.app.SHARED_IS_FIRST
import moe.ore.txhook.app.TXApp
import moe.ore.txhook.databinding.ActivityMainBinding
import moe.ore.txhook.more.*
import moe.ore.txhook.ui.list.CatchingBaseAdapter
import moe.ore.txhook.ui.main.SectionsPagerAdapter

import moe.ore.txhook.datas.ProtocolDatas
import java.util.concurrent.atomic.AtomicBoolean
import android.view.animation.Animation

import android.view.animation.TranslateAnimation
import com.xuexiang.xui.widget.searchview.MaterialSearchView
import moe.ore.txhook.helper.ThreadManager
import kotlin.concurrent.thread


class MainActivity : BaseActivity() {
    // private var isCatching: Boolean = false
    private val isChanging = AtomicBoolean(false)
    private var adapter: CatchingBaseAdapter? = null

    private lateinit var binding: ActivityMainBinding

    private var isExit = 0
    private val exitHandler: Handler by lazy {
        object : Handler(mainLooper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                isExit--
            }
        }
    }

    private var onSearch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter

        val tabs: TabLayout = binding.tabs

        TXApp.catchingList.addAll(ProtocolDatas.getServices().let {
            if (it.size >= config.maxPacketSize + 10) it.slice(0 .. config.maxPacketSize) else it
        })

        tabs.setupWithViewPager(viewPager)
        val fab: FloatingActionButton = binding.fab

        fab.setOnClickListener { (it as FloatingActionButton).also { view ->
            changeContent(true)
            /*
            if (isCatching) {
                isCatching = false
                val colorStateList = ContextCompat.getColorStateList(
                    applicationContext, R.color.tx_nocatching
                )
                view.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
                view.backgroundTintList = colorStateList
                view.setImageResource(R.drawable.icon_nocatch)

            } else {
                isCatching = true
                val colorStateList = ContextCompat.getColorStateList(
                    applicationContext, R.color.tx_catching
                )
                view.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
                view.backgroundTintList = colorStateList
                view.setImageResource(R.drawable.icon_catch)

            } */
        } }

        val deleteAllButton = binding.deleteAll
        val searchButton = binding.searchView
        viewPager.addOnPageChangeListener(object :ViewPager.OnPageChangeListener {
            private var mHiddenAction: TranslateAnimation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f
            )
            private var mShowAction: TranslateAnimation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f, Animation.RELATIVE_TO_SELF, -0.0f
            )

            init {
                mShowAction.repeatMode = Animation.REVERSE
                mShowAction.duration = 500
                mHiddenAction.duration = 500
            }

            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        if (config.changeViewRefresh) changeContent() // 不点击自动触发
                        fab.show()
                        if (deleteAllButton.visibility == INVISIBLE) {
                            deleteAllButton.clearAnimation()
                            deleteAllButton.startAnimation(mShowAction)
                            deleteAllButton.visibility = VISIBLE
                        }

                        searchButton.visibility = VISIBLE

                    }
                    1 -> {

                        searchButton.visibility = GONE
                        TXApp.getCatchingSearchBar().closeSearch()

                        if (deleteAllButton.visibility == INVISIBLE) {
                            deleteAllButton.clearAnimation()
                            deleteAllButton.startAnimation(mShowAction)
                            deleteAllButton.visibility = VISIBLE
                        }
                        fab.hide()
                    }
                    else -> {
                        searchButton.visibility = GONE
                        TXApp.getCatchingSearchBar().closeSearch()

                        if (deleteAllButton.visibility == VISIBLE) {
                            deleteAllButton.clearAnimation()
                            deleteAllButton.startAnimation(mHiddenAction)
                            deleteAllButton.visibility = INVISIBLE
                        }
                        fab.hide()
                    }

                }
            }
        })

        deleteAllButton.setOnClickListener {
            when (viewPager.currentItem) {
                0 -> {
                    ProtocolDatas.clearService()
                    toast.show("抓包数据清理成功")
                    changeContent()
                }
                1 -> {
                    ProtocolDatas.emptyKeyList()
                    toast.show("KEYS清理成功")
                }
            }

        }

        searchButton.setOnClickListener {
            if (TXApp.catchingList.isNotEmpty()) {
                onSearch = if (onSearch) {
                    searchButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_search_off_24))
                    false
                } else {
                    searchButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_search_24))
                    true
                }

                TXApp.getCatchingSearchBar().also {
                    it.visibility = if(onSearch) {
                        it.showSearch(true)
                        VISIBLE
                    } else {
                        it.closeSearch()
                        GONE
                    }
                }.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener {
                    override fun onSearchViewShown() {
                        onSearch = true
                        searchButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_search_24))
                    }

                    override fun onSearchViewClosed() {
                        onSearch = false
                        searchButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_search_off_24))
                    }
                })
            } else toast.show("当前无数据无法使用过滤功能")
        }

        inputActivity()
    }

    fun changeContent(isClick: Boolean = false) {
        if (!isChanging.get()) {
            isChanging.set(true)

            val catchingList = TXApp.getCatchingList()
            adapter = catchingList.adapter as CatchingBaseAdapter?
            ThreadManager.getInstance(0).addTask {
                val services = ProtocolDatas.getServices()
                runOnUiThread {
                    if (services.isNotEmpty()) {
                        toast.show("刷新成功：${services.size}")
                        adapter?.setItemFirst(services)
                        adapter?.notifyDataSetChanged()
                        TXApp.catching.multipleStatusView.showContent()
                    } else {
                        TXApp.catching.multipleStatusView.showEmpty()
                    }
                }
            }

            isChanging.set(false)
        } else {
            if (isClick) CookieBars.cookieBar(this, "提示一下", "正在刷新数据了哦~", "OK") {}
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun inputActivity() = requestPermission {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // 权限全部申请成功才会执行这里的代码
        if (!config.isFirst) { // 是否是首次运行
            MaterialDialog.Builder(this)
                .autoDismiss(false)
                .iconRes(R.drawable.icon_warning)
                .title("首次使用警告")
                .content("本软件仅提供学习与交流使用，禁止适用于违法范围，如果产生违法行为，与软件作者无关。\n数据目录：/sdcard/TXHook\n请输入：我承诺不将软件应用于违法领域")
                .input("请输入口令", "", false, (MaterialDialog.InputCallback { dialog: MaterialDialog?, input: CharSequence ->
                    if (input.toString() == "我承诺不将软件应用于违法领域") {
                        config.isFirst = true
                        config.apply()

                        dialog?.dismiss()
                        init(true)
                    } else {
                        toast.show("口令错误")
                    } // 请输入您的QQ以继续使用
                }))
                .inputRange(14, 14)
                .positiveText("确定")
                .cancelable(false) // 禁止取消
                .show()
        } else {
            init(false)
        }
    }

    private fun init(isFirst: Boolean) {
        if (isFirst) {
            GuideCaseView.Builder(this)
                .focusOn(binding.fab)
                .focusCircleRadiusFactor(1.5)
                .title("点击这里刷获取数据哦")
                .focusBorderColor(Color.GREEN)
                .titleStyle(0, Gravity.CENTER)
                .fitWindowsAuto()
                .build()
                .show()
            // 引导使用功能
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            isExit++
            exit()
            return false
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun exit() {
        if (isExit < 2) {
            toast("再按一次退出应用")
            exitHandler.sendEmptyMessageDelayed(0, 2000)
        } else {
            ActivityCollector.finishAll()
            super.onBackPressed()
        }
    }

    override fun requiredPermission(): Array<String> = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    )
    override fun needInitTheme(): Boolean = false

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}

