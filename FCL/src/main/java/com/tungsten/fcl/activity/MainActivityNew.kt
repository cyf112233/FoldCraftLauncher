package com.tungsten.fcl.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar
import com.mio.manager.RendererManager
import com.mio.ui.dialog.RendererSelectDialog
import com.mio.util.AnimUtil
import com.mio.util.GuideUtil
import com.mio.util.ImageUtil
import com.tungsten.fcl.R
import com.tungsten.fcl.databinding.ActivityMainNewBinding
import com.tungsten.fcl.game.JarExecutorHelper
import com.tungsten.fcl.game.TexturesLoader
import com.tungsten.fcl.setting.Accounts
import com.tungsten.fcl.setting.ConfigHolder
import com.tungsten.fcl.setting.Controllers
import com.tungsten.fcl.setting.Profile
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.ui.UIManager
import com.tungsten.fcl.ui.version.Versions
import com.tungsten.fcl.upgrade.UpdateChecker
import com.tungsten.fcl.util.AndroidUtils
import com.tungsten.fcl.util.FXUtils
import com.tungsten.fcl.util.WeakListenerHolder
import com.tungsten.fclauncher.FCLConfig
import com.tungsten.fclauncher.bridge.FCLBridge
import com.tungsten.fclauncher.plugins.DriverPlugin
import com.tungsten.fclauncher.plugins.RendererPlugin
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.auth.Account
import com.tungsten.fclcore.auth.authlibinjector.AuthlibInjectorAccount
import com.tungsten.fclcore.auth.authlibinjector.AuthlibInjectorServer
import com.tungsten.fclcore.auth.yggdrasil.TextureModel
import com.tungsten.fclcore.download.LibraryAnalyzer
import com.tungsten.fclcore.download.LibraryAnalyzer.LibraryType
import com.tungsten.fclcore.fakefx.beans.binding.Bindings
import com.tungsten.fclcore.fakefx.beans.property.IntegerProperty
import com.tungsten.fclcore.fakefx.beans.property.IntegerPropertyBase
import com.tungsten.fclcore.fakefx.beans.property.ObjectProperty
import com.tungsten.fclcore.fakefx.beans.property.SimpleObjectProperty
import com.tungsten.fclcore.fakefx.beans.value.ObservableValue
import com.tungsten.fclcore.mod.RemoteMod
import com.tungsten.fclcore.mod.RemoteMod.IMod
import com.tungsten.fclcore.mod.RemoteModRepository
import com.tungsten.fclcore.util.Logging.LOG
import com.tungsten.fclcore.util.fakefx.BindingMapping
import com.tungsten.fcllibrary.component.FCLActivity
import com.tungsten.fcllibrary.component.dialog.EditDialog
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog
import com.tungsten.fcllibrary.component.theme.ThemeEngine
import com.tungsten.fcllibrary.component.view.FCLNavButton
import com.tungsten.fcllibrary.component.view.FCLUILayout
import com.tungsten.fcllibrary.util.ConvertUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.logging.Level
import java.util.stream.Stream
import kotlin.system.exitProcess

class MainActivityNew : FCLActivity() {
    companion object {
        private lateinit var instance: WeakReference<MainActivityNew>

        @JvmStatic
        fun getInstance(): MainActivityNew {
            return instance.get()!!
        }
    }

    lateinit var binding: ActivityMainNewBinding
    private var _uiManager: UIManager? = null
    lateinit var uiManager: UIManager
    private lateinit var currentAccount: ObjectProperty<Account?>
    private val holder = WeakListenerHolder()
    private lateinit var profile: Profile
    private lateinit var theme: IntegerProperty
    var isVersionLoading = false
    private lateinit var permissionResultLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = WeakReference(this)
        binding = ActivityMainNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化动态背景
        initDynamicBackground()
        
        // 初始化主题
        initTheme()
        
        // 初始化配置
        initConfig()
        
        // 初始化UI
        initUI()
        
        // 初始化权限
        initPermissions()
        
        // 设置监听器
        setupListeners()
        
        // 初始化数据绑定
        setupDataBinding()
        
        // 播放启动动画
        playStartupAnimation()
    }

    private fun initDynamicBackground() {
        // 启动动态背景动画
        binding.dynamicBackground.startAnimation()
        
        // 根据主题设置背景颜色
        binding.dynamicBackground.setTheme(ThemeEngine.getInstance().getTheme())
    }

    private fun initTheme() {
        theme = object : IntegerPropertyBase() {
            override fun getBean(): Any = this@MainActivityNew
            override fun getName(): String = "theme"
        }
        
        // 主题切换按钮
        binding.themeToggle.setOnClickListener {
            val currentTheme = ThemeEngine.getInstance().getTheme()
            val newTheme = if (currentTheme.isDarkTheme()) {
                ThemeEngine.getInstance().getLightTheme()
            } else {
                ThemeEngine.getInstance().getDarkTheme()
            }
            ThemeEngine.getInstance().setTheme(newTheme)
            updateThemeUI()
        }
    }

    private fun initConfig() {
        if (!ConfigHolder.isInit()) {
            try {
                ConfigHolder.init()
            } catch (e: IOException) {
                LOG.log(Level.WARNING, e.message)
            }
        }
    }

    private fun initUI() {
        // 初始化UI管理器
        uiManager = UIManager(this, binding.uiContentLayout)
        _uiManager = uiManager
        
        // 设置默认返回事件
        uiManager.registerDefaultBackEvent {
            if (uiManager.currentUI === uiManager.mainUI) {
                val i = Intent(Intent.ACTION_MAIN)
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i.addCategory(Intent.CATEGORY_HOME)
                startActivity(i)
                exitProcess(0)
            } else {
                binding.navHome.isSelected = true
            }
        }
        
        // 初始化UI
        uiManager.init {
            setupNavigationButtons()
            setupBackendSelector()
            setupAccountCard()
            setupLaunchButton()
            setupQuickActions()
            
            // 默认选中主页
            binding.navHome.isSelected = true
            uiManager.switchUI(uiManager.mainUI)
            
            // 检查更新
            UpdateChecker.getInstance().checkAuto(this).start()
        }
    }

    private fun setupNavigationButtons() {
        // 设置导航按钮点击事件
        binding.navHome.setOnClickListener { onNavButtonClick(binding.navHome, "home") }
        binding.navManage.setOnClickListener { onNavButtonClick(binding.navManage, "manage") }
        binding.navDownload.setOnClickListener { onNavButtonClick(binding.navDownload, "download") }
        binding.navController.setOnClickListener { onNavButtonClick(binding.navController, "controller") }
        binding.navSettings.setOnClickListener { onNavButtonClick(binding.navSettings, "settings") }
        binding.navBack.setOnClickListener { onBackPressed() }
        
        // 长按事件
        binding.navHome.setOnLongClickListener {
            shareLog()
            true
        }
        
        binding.navBack.setOnLongClickListener {
            startActivity(Intent(this, ShellActivity::class.java))
            true
        }
    }

    private fun onNavButtonClick(button: FCLNavButton, page: String) {
        // 重置所有按钮状态
        resetNavButtons()
        
        // 设置当前按钮为选中状态
        button.isSelected = true
        
        // 播放选择动画
        val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.nav_button_scale)
        button.startAnimation(scaleAnimation)
        
        // 切换页面
        when (page) {
            "home" -> {
                updateTitle(getString(R.string.app_name))
                uiManager.switchUI(uiManager.mainUI)
            }
            "manage" -> {
                val version = Profiles.getSelectedProfile().selectedVersion
                if (version == null) {
                    updateTitle(getString(R.string.version))
                    uiManager.switchUI(uiManager.versionUI)
                } else {
                    updateTitle(getString(R.string.manage))
                    uiManager.manageUI.setVersion(version, Profiles.getSelectedProfile())
                    uiManager.switchUI(uiManager.manageUI)
                }
            }
            "download" -> {
                updateTitle(getString(R.string.download))
                uiManager.switchUI(uiManager.downloadUI)
            }
            "controller" -> {
                updateTitle(getString(R.string.controller))
                uiManager.switchUI(uiManager.controllerUI)
            }
            "settings" -> {
                updateTitle(getString(R.string.setting))
                uiManager.switchUI(uiManager.settingUI)
            }
        }
    }

    private fun resetNavButtons() {
        binding.navHome.isSelected = false
        binding.navManage.isSelected = false
        binding.navDownload.isSelected = false
        binding.navController.isSelected = false
        binding.navSettings.isSelected = false
    }

    private fun setupBackendSelector() {
        // 设置后端选择器
        val isBoatBackend = getSharedPreferences("launcher", MODE_PRIVATE).getBoolean("backend", false)
        binding.backendToggleGroup.check(
            if (isBoatBackend) R.id.backend_boat else R.id.backend_pojav
        )
        
        binding.backendToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isBoat = checkedId == R.id.backend_boat
                getSharedPreferences("launcher", MODE_PRIVATE).edit {
                    putBoolean("backend", isBoat)
                }
                
                // 显示切换提示
                Snackbar.make(
                    binding.root,
                    if (isBoat) "已切换到Boat后端" else "已切换到Pojav后端",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupAccountCard() {
        // 账户卡片点击事件
        binding.accountCard.setOnClickListener {
            if (uiManager.currentUI !== uiManager.accountUI) {
                resetNavButtons()
                updateTitle(getString(R.string.account))
                uiManager.switchUI(uiManager.accountUI)
            }
        }
    }

    private fun setupLaunchButton() {
        // 启动按钮点击事件
        binding.launchButton.setOnClickListener {
            launchGame()
        }
        
        // 启动按钮长按事件
        binding.launchButton.setOnLongClickListener {
            RendererSelectDialog(this, false) {
                launchGame()
            }.show()
            true
        }
    }

    private fun setupQuickActions() {
        // 版本管理按钮
        binding.versionManageButton.setOnClickListener {
            if (uiManager.currentUI !== uiManager.versionUI) {
                resetNavButtons()
                updateTitle(getString(R.string.version))
                uiManager.switchUI(uiManager.versionUI)
            }
        }
        
        // JAR执行按钮
        binding.jarExecuteButton.setOnClickListener {
            binding.jarExecuteButton.isSelected = false
            JarExecutorHelper.start(this, this)
        }
        
        // JAR执行按钮长按事件
        binding.jarExecuteButton.setOnLongClickListener {
            EditDialog(this) {
                JarExecutorHelper.exec(
                    this@MainActivityNew,
                    null,
                    JarExecutorHelper.getJava(null),
                    it
                )
            }.apply {
                setTitle(R.string.jar_execute_custom_args)
                binding.editText.hint = "-jar xxx"
                binding.editText.setLines(1)
                binding.editText.maxLines = 1
            }.show()
            true
        }
    }

    private fun launchGame() {
        if (!Controllers.isInitialized()) {
            updateTitle(getString(R.string.message_loading_controllers))
            val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.button_shake)
            binding.launchButton.startAnimation(shakeAnimation)
            return
        }
        
        FCLBridge.BACKEND_IS_BOAT = binding.backendToggleGroup.checkedButtonId == R.id.backend_boat
        val selectedProfile = Profiles.getSelectedProfile()
        var renderer = selectedProfile.getVersionSetting(selectedProfile.selectedVersion).renderer
        RendererManager.getRenderer(selectedProfile.selectedVersion)
        DriverPlugin.selected = DriverPlugin.driverList.find {
            it.driver == selectedProfile.getVersionSetting(selectedProfile.selectedVersion).driver
        } ?: DriverPlugin.driverList[0]
        Versions.launch(this, selectedProfile)
    }

    private fun setupDataBinding() {
        // 设置账户显示
        setupAccountDisplay()
        
        // 设置版本显示
        setupVersionDisplay()
    }

    private fun setupAccountDisplay() {
        binding.apply {
            currentAccount = object : SimpleObjectProperty<Account?>() {
                override fun invalidated() {
                    val account = get()
                    if (account == null) {
                        accountName.text = getString(R.string.account_state_no_account)
                        accountStatus.text = getString(R.string.account_state_add)
                        accountAvatar.setImageDrawable(
                            AppCompatResources.getDrawable(
                                this@MainActivityNew,
                                R.drawable.default_avatar
                            )
                        )
                    } else {
                        accountName.text = account.username
                        accountStatus.text = account.character
                        
                        // 加载账户头像
                        lifecycleScope.launch(Dispatchers.IO) {
                            val avatar = TexturesLoader.toAvatar(
                                TexturesLoader.getDefaultSkin(TextureModel.ALEX).image,
                                ConvertUtils.dip2px(this@MainActivityNew, 64f)
                            )
                            withContext(Dispatchers.Main) {
                                accountAvatar.setImageBitmap(avatar)
                            }
                        }
                    }
                }
            }
            currentAccount.bind(Accounts.selectedAccountProperty())
        }
    }

    private fun setupVersionDisplay() {
        binding.apply {
            val selectedProfile = Profiles.getSelectedProfile()
            val selectedVersion = selectedProfile.selectedVersion
            
            if (selectedVersion == null) {
                versionName.text = getString(R.string.no_version_selected)
            } else {
                versionName.text = selectedVersion
            }
        }
    }

    private fun updateTitle(title: String) {
        // 使用动画更新标题
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        
        binding.appTitle.startAnimation(fadeOut)
        fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                binding.appTitle.text = title
                binding.appTitle.startAnimation(fadeIn)
            }
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
    }

    private fun updateThemeUI() {
        // 更新主题相关的UI元素
        binding.dynamicBackground.setTheme(ThemeEngine.getInstance().getTheme())
        
        // 更新网络状态图标
        updateNetworkStatus()
    }

    private fun updateNetworkStatus() {
        // 检查网络状态并更新图标
        val isOnline = AndroidUtils.isNetworkAvailable(this)
        binding.networkStatus.setImageResource(
            if (isOnline) R.drawable.ic_network_online else R.drawable.ic_network_offline
        )
        binding.networkStatus.setColorFilter(
            ContextCompat.getColor(
                this,
                if (isOnline) R.color.online_green else R.color.offline_red
            )
        )
    }

    private fun playStartupAnimation() {
        // 播放启动动画
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_slow)
        binding.topStatusCard.startAnimation(fadeInAnimation)
        
        val slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        binding.leftNavCard.startAnimation(slideInLeft)
        
        val slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        binding.rightPanelCard.startAnimation(slideInRight)
        
        val scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in)
        binding.contentCard.startAnimation(scaleIn)
    }

    private fun initPermissions() {
        permissionResultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Snackbar.make(binding.root, "权限已授予", Snackbar.LENGTH_SHORT).show()
            }
        }
        
        // 检查通知权限
        if (!checkNotificationPermission() && getSharedPreferences(
                "launcher",
                MODE_PRIVATE
            ).getBoolean("check_notification_permission", true)
        ) {
            getSharedPreferences("launcher", MODE_PRIVATE).edit {
                putBoolean("check_notification_permission", false)
            }
            FCLAlertDialog.Builder(this)
                .setMessage(getString(R.string.notification_permission))
                .setPositiveButton {
                    requestNotificationPermission()
                }
                .setNegativeButton {}
                .create()
                .show()
        }
    }

    private fun setupListeners() {
        // 设置各种监听器
        holder.add(Accounts.selectedAccountProperty().addListener { _, _, _ ->
            setupAccountDisplay()
        })
        
        holder.add(Profiles.selectedProfileProperty().addListener { _, _, _ ->
            setupVersionDisplay()
        })
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        }
    }

    private fun shareLog() {
        // 分享日志功能
        try {
            val logFile = File(FCLPath.FILES_DIR, "latest.log")
            if (logFile.exists()) {
                val uri = FXUtils.getFileUri(this, logFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "FCL Log")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "分享日志"))
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "分享日志失败: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (event?.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            _uiManager?.onBackPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        _uiManager?.onPause()
    }

    override fun onResume() {
        super.onResume()
        _uiManager?.onResume()
        updateNetworkStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        holder.unbind()
    }
} 