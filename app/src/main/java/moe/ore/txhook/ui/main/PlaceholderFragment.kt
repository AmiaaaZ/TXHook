package moe.ore.txhook.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xuexiang.xui.XUI
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog.SingleButtonCallback
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView.ACCESSORY_TYPE_CHEVRON
import com.xuexiang.xui.widget.grouplist.XUIGroupListView
import kotlinx.io.core.discardExact
import kotlinx.io.core.readBytes
import moe.ore.txhook.JsonViewActivity
import moe.ore.txhook.PacketInfoActivity
import moe.ore.txhook.app.TXApp
import moe.ore.txhook.catching.PacketService
import moe.ore.txhook.databinding.FragmentCatchBinding
import moe.ore.txhook.databinding.FragmentDataBinding
import moe.ore.txhook.databinding.FragmentSettingBinding
import moe.ore.txhook.datas.PacketInfoData
import moe.ore.txhook.datas.ProtocolDatas
import moe.ore.txhook.helper.toByteReadPacket
import moe.ore.txhook.helper.toHexString
import moe.ore.txhook.more.CookieBars
import moe.ore.txhook.more.config
import moe.ore.txhook.more.copyText
import moe.ore.txhook.more.toast
import moe.ore.txhook.ui.list.CatchingBaseAdapter
import com.xuexiang.xui.widget.grouplist.XUICommonListItemView.ACCESSORY_TYPE_SWITCH
import moe.ore.txhook.ByteCheckActivity
import moe.ore.txhook.R
import moe.ore.txhook.databinding.FragmentToolsBinding

/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment(private val sectionNumber: Int) : Fragment() {
    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // println("unknown: $sectionNumber")
        when(sectionNumber) {
            1 -> {
                val binding = FragmentCatchBinding.inflate(inflater, container, false).also {
                    TXApp.catching = it
                }
                val statusView = binding.multipleStatusView
                statusView.setOnRetryClickListener {
                    CookieBars.cookieBar(activity, "提示一下", "请点击右下角按钮刷新获取数据哦~", "明白了") {
                        toast.show("去吧，皮卡丘~")
                    }
                }
                val listView = TXApp.getCatchingList()
                listView.adapter = CatchingBaseAdapter(TXApp.catchingList)

                listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                    val service = parent.getItemAtPosition(position) as PacketService
                    val startIntent = Intent(XUI.getContext(), PacketInfoActivity::class.java)
                    startIntent.putExtra("data", PacketInfoData().apply {
                        if (service.from) {
                            val from = service.toFromService()
                            fromSource = from.fromSource
                            uin = from.uin
                            seq = from.seq
                            cmd = from.cmd
                            bufferSize = from.buffer.size
                            buffer = from.buffer
                            time = from.time
                            sessionSize = from.sessionId.size
                            sessionId = from.sessionId

                        } else {
                            val to = service.toToService()
                            fromSource = to.fromSource
                            uin = to.uin
                            seq = to.seq
                            cmd = to.cmd
                            bufferSize = to.buffer.size
                            buffer = to.buffer
                            time = to.time
                            sessionSize = to.sessionId.size
                            sessionId = to.sessionId

                            packetType = to.packetType
                            encodeType = to.encodeType
                        }
                    })
                    startActivity(startIntent)

                }
                if (TXApp.catchingList.isEmpty()) statusView.showEmpty() else statusView.showContent()

                return binding.root
            }
            2 -> {
                val binding = FragmentDataBinding.inflate(inflater, container, false)

                val copyListener = View.OnClickListener { (it as XUICommonListItemView).let { itemView ->
                    context?.copyText(itemView.detailText.toString())
                } }

                val groupListView = binding.groupListView

                val appIdItem = groupListView.createItemView("AppId")
                appIdItem.detailText = ProtocolDatas.getAppId().toString()

                val maxPackageSizeItem = groupListView.createItemView("MaxPackageSize")
                maxPackageSizeItem.detailText = ProtocolDatas.getMaxPackageSize().toString()

                val publicKeyItem = groupListView.createItemView("PublicKey")
                publicKeyItem.detailText = "点击查看详细"

                val shareKeyItem = groupListView.createItemView("ShareKey")
                shareKeyItem.detailText = "点击查看详细"

                val guidItem = groupListView.createItemView("Guid")
                guidItem.detailText = ProtocolDatas.getGUID().toHexString()

                val ksidItem = groupListView.createItemView("Ksid")
                ksidItem.detailText = ProtocolDatas.getKsid().toHexString()

                val qimeiItem = groupListView.createItemView("QImei")
                qimeiItem.detailText = ProtocolDatas.getQIMEI().toHexString()

                XUIGroupListView.newSection(context)
                    .setTitle("基础信息")
                    .addItemView(appIdItem, copyListener)
                    .addItemView(maxPackageSizeItem, copyListener)
                    .addItemView(publicKeyItem) {
                        val list = ProtocolDatas.getKeyList()
                        if (list.publicKeyList.isNotEmpty()) {
                            val arr = list.publicKeyList.map { it.toHexString() }
                            MaterialDialog.Builder(requireContext())
                                .title("密钥列表（点击复制）")
                                .items(
                                    arr.map { it.let { if (it.length > 24) it.substring(0, 24) + "..." else it } }
                                )
                                .itemsCallback { dialog: MaterialDialog, _: View?, position: Int, _: CharSequence? ->
                                    dialog.dismiss()
                                    context?.copyText(arr[position])
                                }
                                .show()
                        } else {
                            toast.show("啥数据也没有嗷~~")
                        }
                    }
                    .addItemView(shareKeyItem) {
                        val list = ProtocolDatas.getKeyList()
                        if (list.shareKeyList.isNotEmpty()) {
                            val arr = list.shareKeyList.map { it.toHexString() }
                            MaterialDialog.Builder(requireContext())
                                .title("密钥列表（点击复制）")
                                .items(
                                    arr.map { it.let { if (it.length > 24) it.substring(0, 24) + "..." else it } }
                                )
                                .itemsCallback { dialog: MaterialDialog, _: View?, position: Int, _: CharSequence? ->
                                    dialog.dismiss()
                                    context?.copyText(arr[position])
                                }
                                .show()
                        } else {
                            toast.show("空空如也~~")
                        }
                    }
                    .addItemView(guidItem, copyListener)
                    .addItemView(ksidItem, copyListener)
                    .addItemView(qimeiItem, copyListener)
                    .addTo(groupListView)

                val lastUin = String(ProtocolDatas.getId("last_uin"))
                val reader = ProtocolDatas.getId("$lastUin-AccountKey").toByteReadPacket()

                val uinItem = groupListView.createItemView("Uin")
                uinItem.detailText = lastUin

                if (reader.hasBytes(18)) {
                    reader.discardExact(reader.readShort().toInt())

                    val a1Item = groupListView.createItemView("A1")
                    a1Item.detailText = reader.readBytes(reader.readShort().toInt()).toHexString()

                    val a2Item = groupListView.createItemView("A2")
                    a2Item.detailText = reader.readBytes(reader.readShort().toInt()).toHexString()

                    val a3Item = groupListView.createItemView("A3")
                    a3Item.detailText = reader.readBytes(reader.readShort().toInt()).toHexString()

                    val d1Item = groupListView.createItemView("D1")
                    d1Item.detailText = reader.readBytes(reader.readShort().toInt()).toHexString()

                    val d2Item = groupListView.createItemView("D2")
                    d2Item.detailText = reader.readBytes(reader.readShort().toInt()).toHexString()

                    val s2Item = groupListView.createItemView("S2")
                    s2Item.detailText = reader.readBytes(reader.readShort().toInt()).toHexString()

                    val keyItem = groupListView.createItemView("Key")
                    keyItem.detailText = reader.readBytes(reader.readShort().toInt()).toHexString()

                    val cookieItem = groupListView.createItemView("Cookie")
                    cookieItem.detailText = reader.readBytes(reader.readShort().toInt()).toHexString()

                    XUIGroupListView.newSection(context)
                        .setTitle("最后QQ操作者TOKEN")
                        .addItemView(uinItem, copyListener)
                        .addItemView(a1Item, copyListener)
                        .addItemView(a2Item, copyListener)
                        .addItemView(a3Item, copyListener)
                        .addItemView(d1Item, copyListener)
                        .addItemView(d2Item, copyListener)
                        .addItemView(s2Item, copyListener)
                        .addItemView(keyItem, copyListener)
                        .addItemView(cookieItem, copyListener)
                        .addTo(groupListView)
                }

                val svnItem = groupListView.createItemView("SvnVersion")
                svnItem.detailText = ProtocolDatas.getSVNVersion()

                val releaseTimeItem = groupListView.createItemView("ReleaseTime")
                releaseTimeItem.detailText = ProtocolDatas.getReleaseTime()

                val androidIdItem = groupListView.createItemView("AndroidId")
                androidIdItem.detailText = ProtocolDatas.getAndroidId()

                val macItem = groupListView.createItemView("MacAddress")
                macItem.detailText = ProtocolDatas.getMac()

                val ssidItem = groupListView.createItemView("SSID")
                ssidItem.detailText = ProtocolDatas.getSsid()

                val bssidItem = groupListView.createItemView("BSSID")
                bssidItem.detailText = ProtocolDatas.getBSsid()

                val netTypeItem = groupListView.createItemView("NetType")
                netTypeItem.detailText = ProtocolDatas.getNetType().toString()

                val logDirItem = groupListView.createItemView("LogPath")
                logDirItem.detailText = ProtocolDatas.getWloginLogDir()

                XUIGroupListView.newSection(context)
                    .setTitle("QQ其它参数")
                    .addItemView(svnItem, copyListener)
                    .addItemView(releaseTimeItem, copyListener)
                    .addItemView(androidIdItem, copyListener)
                    .addItemView(macItem, copyListener)
                    .addItemView(ssidItem, copyListener)
                    .addItemView(bssidItem, copyListener)
                    .addItemView(netTypeItem, copyListener)
                    .addItemView(logDirItem, copyListener)
                    .addTo(groupListView)

                return binding.root
            }
            3 -> {
                val binding = FragmentToolsBinding.inflate(inflater, container, false)
                binding.analyseView.setOnClickListener {
                    val intent = Intent(requireContext(), JsonViewActivity::class.java)
                    intent.putExtra("require_input", false)
                    requireContext().startActivity(intent)
                }
                binding.calcView.setOnClickListener {
                    requireContext().startActivity(Intent(requireContext(), ByteCheckActivity::class.java))
                }

                return binding.root
            }
            4 -> {
                val binding = FragmentSettingBinding.inflate(inflater, container, false)

                val group = binding.groupListView

                config.changeViewRefresh

                val maxPacketSizeItem = group.createItemView("最大包展示大小")
                maxPacketSizeItem.detailText = config.maxPacketSize.toString()
                maxPacketSizeItem.accessoryType = ACCESSORY_TYPE_CHEVRON

                val changeViewRefreshItem = group.createItemView("切换界面自动刷新(卡顿)")
                changeViewRefreshItem.accessoryType = ACCESSORY_TYPE_SWITCH
                changeViewRefreshItem.switch.isChecked = config.changeViewRefresh
                changeViewRefreshItem.switch.setOnCheckedChangeListener { _, isChecked ->
                    config.changeViewRefresh = isChecked
                    config.apply()
                    toast.show("修改为：${config.changeViewRefresh}")
                }

                val autoLoginMerge = group.createItemView("自动翻译Sso.LoginMerge")
                autoLoginMerge.accessoryType = ACCESSORY_TYPE_SWITCH
                autoLoginMerge.switch.isChecked = ProtocolDatas.getSetting().autoSsoLoginMerge
                autoLoginMerge.switch.setOnCheckedChangeListener { _, isChecked ->
                    val setting = ProtocolDatas.getSetting()
                    setting.autoSsoLoginMerge = isChecked
                    ProtocolDatas.setSetting(setting)
                }

                XUIGroupListView.newSection(context)
                    .setTitle("基础设置")
                    .addItemView(maxPacketSizeItem) {
                        MaterialDialog.Builder(requireContext())
                            .iconRes(R.drawable.ic_baseline_edit_note_24)
                            .title("请输入修改后的值")
                            .content("取值范围在0~2000之间，切记不要设置的过大！")
                            .inputType(InputType.TYPE_CLASS_NUMBER)
                            .input("请输入一个数字", "", false, (MaterialDialog.InputCallback { _: MaterialDialog?, text: CharSequence ->
                                val num = text.toString().toInt()
                                config.maxPacketSize = num
                                config.apply()
                                maxPacketSizeItem.detailText = text
                                toast.show("修改成功~")
                            }))
                            .inputRange(1, 4)
                            .positiveText("确定")
                            .negativeText("取消")
                            .onPositive((SingleButtonCallback { dialog: MaterialDialog, _: DialogAction? -> dialog.dismiss() }))
                            .cancelable(false)
                            .show()
                    }
                    .addItemView(changeViewRefreshItem, null)
                    .addItemView(autoLoginMerge, null)
                    .addTo(group)

                return binding.root
            }
        }
        return View(context)
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment(sectionNumber).apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
