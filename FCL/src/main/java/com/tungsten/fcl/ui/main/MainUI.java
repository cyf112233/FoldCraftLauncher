package com.tungsten.fcl.ui.main;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;

import com.tungsten.fcl.R;
import com.tungsten.fcl.game.TexturesLoader;
import com.tungsten.fcl.setting.Accounts;
import com.tungsten.fcl.util.AndroidUtils;
import com.tungsten.fclcore.auth.Account;
import com.tungsten.fclcore.fakefx.beans.property.ObjectProperty;
import com.tungsten.fclcore.fakefx.beans.property.SimpleObjectProperty;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.util.Logging;
import com.tungsten.fclcore.util.io.HttpRequest;
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog;
import com.tungsten.fcllibrary.component.theme.ThemeEngine;
import com.tungsten.fcllibrary.component.ui.FCLCommonUI;
import com.tungsten.fcllibrary.component.view.FCLButton;
import com.tungsten.fcllibrary.component.view.FCLTextView;
import com.tungsten.fcllibrary.component.view.FCLUILayout;
import com.tungsten.fcllibrary.skin.SkinCanvas;
import com.tungsten.fcllibrary.skin.SkinRenderer;
import com.tungsten.fcllibrary.util.LocaleUtils;

import java.util.logging.Level;

public class MainUI extends FCLCommonUI implements View.OnClickListener {

    public static final String ANNOUNCEMENT_URL = "https://raw.githubusercontent.com/FCL-Team/FCL-Repo/refs/heads/main/res/announcement_v2.txt";
    public static final String ANNOUNCEMENT_URL_CN = "https://gitee.com/fcl-team/FCL-Repo/raw/main/res/announcement_v2.txt";

    private LinearLayoutCompat announcementContainer;
    private LinearLayoutCompat announcementLayout;
    private FCLTextView title;
    private FCLTextView announcementView;
    private FCLTextView date;
    private FCLButton hide;
    private Announcement announcement = null;

    private RelativeLayout skinContainer;
    private SkinCanvas skinCanvas;
    private SkinRenderer renderer;

    private ObjectProperty<Account> currentAccount;

    public MainUI(Context context, FCLUILayout parent, int id) {
        super(context, parent, id);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 绑定新布局
        View topBar = findViewById(R.id.top_bar);
        View logoView = findViewById(R.id.logo_view);
        View btnSettings = findViewById(R.id.btn_settings);
        RelativeLayout skinContainer = findViewById(R.id.skin_container);
        View bottomPanel = findViewById(R.id.bottom_panel);
        View startButton = findViewById(R.id.btn_start);
        View announcementCard = findViewById(R.id.announcement_card);
        TextView announcementTitle = findViewById(R.id.announcement_title);
        TextView announcementContent = findViewById(R.id.announcement_content);
        // 设置logo自绘
        if (logoView instanceof FCLDynamicLogoView) {
            ((FCLDynamicLogoView) logoView).startAnimation();
        }
        // 设置设置按钮自绘
        if (btnSettings instanceof FCLSettingsIconView) {
            ((FCLSettingsIconView) btnSettings).setOnClickListener(v -> {
                // 打开设置界面
                getParent().switchUI(getUIManager().settingUI);
            });
        }
        // 公告内容
        checkAnnouncement((title, content) -> {
            announcementTitle.setText(title);
            announcementContent.setText(content);
        });
        // 启动按钮自绘
        if (startButton instanceof FCLStartButtonView) {
            ((FCLStartButtonView) startButton).setOnClickListener(v -> {
                // 启动游戏
                getUIManager().getActivity().runOnUiThread(() -> {
                    // 这里调用启动逻辑
                });
            });
        }
        // 皮肤展示区
        renderer = new SkinRenderer(getContext());
        skinCanvas = new SkinCanvas(getContext());
        skinCanvas.setRenderer(renderer, 5f);
        skinContainer.removeAllViews();
        skinContainer.addView(skinCanvas);
        skinContainer.setVisibility(View.VISIBLE);
        setupSkinDisplay();
        // 动态背景由Activity注入或在MainActivity中实现
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!ThemeEngine.getInstance().theme.isCloseSkinModel()) {
            if (skinCanvas == null) {
                skinCanvas = new SkinCanvas(getContext());
                skinCanvas.setRenderer(renderer, 5f);
            } else {
                skinCanvas.onResume();
                renderer.updateTexture(renderer.getTexture()[0], renderer.getTexture()[1]);
            }

            skinContainer.addView(skinCanvas);
            skinContainer.setVisibility(View.VISIBLE);
        } else {
            if (skinCanvas != null) skinCanvas.onPause();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (skinCanvas != null) {
            skinCanvas.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isShowing() && skinCanvas != null) {
            skinCanvas.onResume();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (skinCanvas != null) {
            skinCanvas.onPause();
        }
        skinContainer.removeView(skinCanvas);
    }

    @Override
    public Task<?> refresh(Object... param) {
        return Task.runAsync(() -> {

        });
    }

    // 公告内容回调
    private void checkAnnouncement(AnnouncementCallback callback) {
        try {
            String url = LocaleUtils.isChinese(getContext()) ? ANNOUNCEMENT_URL_CN : ANNOUNCEMENT_URL;
            Task.supplyAsync(() -> HttpRequest.HttpGetRequest.GET(url).getJson(Announcement.class))
                    .thenAcceptAsync(Schedulers.androidUIThread(), announcement -> {
                        this.announcement = announcement;
                        if (!announcement.shouldDisplay(getContext()))
                            return;
                        callback.onResult(
                            AndroidUtils.getLocalizedText(getContext(), "announcement", announcement.getDisplayTitle(getContext())),
                            announcement.getDisplayContent(getContext())
                        );
                    }).start();
        } catch (Exception e) {
            e.printStackTrace();
            Logging.LOG.log(Level.WARNING, "Failed to get announcement!", e);
        }
    }
    private interface AnnouncementCallback {
        void onResult(String title, String content);
    }

    private void hideAnnouncement() {
        announcementContainer.setVisibility(View.GONE);
        if (announcement != null) {
            announcement.hide(getContext());
        }
    }

    private void setupSkinDisplay() {
        currentAccount = new SimpleObjectProperty<Account>() {

            @Override
            protected void invalidated() {
                Account account = get();
                renderer.textureProperty().unbind();
                if (account == null) {
                    renderer.updateTexture(BitmapFactory.decodeStream(MainUI.class.getResourceAsStream("/assets/img/alex.png")), null);
                } else {
                    renderer.textureProperty().bind(TexturesLoader.textureBinding(account));
                }
            }
        };
        currentAccount.bind(Accounts.selectedAccountProperty());
    }

    public void refreshSkin(Account account) {
        Schedulers.androidUIThread().execute(() -> {
            if (currentAccount.get() == account) {
                renderer.textureProperty().unbind();
                renderer.textureProperty().bind(TexturesLoader.textureBinding(currentAccount.get()));
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view == hide) {
            if (announcement != null && announcement.isSignificant()) {
                FCLAlertDialog.Builder builder = new FCLAlertDialog.Builder(getContext());
                builder.setAlertLevel(FCLAlertDialog.AlertLevel.ALERT);
                builder.setCancelable(false);
                builder.setMessage(getContext().getString(R.string.announcement_significant));
                builder.setPositiveButton(this::hideAnnouncement);
                builder.setNegativeButton(null);
                builder.create().show();
            } else {
                hideAnnouncement();
            }
        }
    }
}
