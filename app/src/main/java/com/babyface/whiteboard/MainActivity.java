package com.babyface.whiteboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private WhiteboardView board;
    private LinearLayout toolbar;
    private Button modeButton;
    private Button widthButton;
    private final Handler handler = new Handler();
    private final Runnable hideToolbarRunnable = new Runnable() {
        @Override public void run() { hideToolbar(); }
    };

    private final float[] widths = {3f, 6f, 12f, 20f};
    private int widthIndex = 1;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();

        FrameLayout root = new FrameLayout(this);
        board = new WhiteboardView(this);
        root.addView(board, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        toolbar = buildToolbar();
        FrameLayout.LayoutParams toolbarParams = new FrameLayout.LayoutParams(
                dp(72), FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.CENTER_VERTICAL);
        toolbarParams.setMargins(0, 0, dp(8), 0);
        root.addView(toolbar, toolbarParams);

        Button showButton = makeButton("☰", "ツール");
        FrameLayout.LayoutParams showParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.END | Gravity.CENTER_VERTICAL);
        showParams.setMargins(0, 0, dp(8), 0);
        showButton.setOnClickListener(v -> showToolbar());
        root.addView(showButton, showParams);

        toolbar.setTag(showButton);
        setContentView(root);
        updateModeButton();
        updateWidthButton();
        showToolbar();
    }

    private LinearLayout buildToolbar() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(4), dp(4), dp(4), dp(4));
        box.setBackgroundColor(Color.argb(235, 245, 245, 245));

        modeButton = makeButton("✎", "ペン / 消しゴム");
        modeButton.setOnClickListener(v -> {
            board.setEraser(!board.isEraser());
            updateModeButton();
            scheduleHide();
        });
        box.addView(modeButton);

        widthButton = makeButton("●", "太さ");
        widthButton.setOnClickListener(v -> {
            widthIndex = (widthIndex + 1) % widths.length;
            board.setPenWidth(widths[widthIndex]);
            updateWidthButton();
            scheduleHide();
        });
        box.addView(widthButton);

        Button undo = makeButton("↶", "元に戻す");
        undo.setOnClickListener(v -> { board.undo(); scheduleHide(); });
        box.addView(undo);

        Button redo = makeButton("↷", "やり直す");
        redo.setOnClickListener(v -> { board.redo(); scheduleHide(); });
        box.addView(redo);

        Button clear = makeButton("⌫", "全消去");
        clear.setOnClickListener(v -> confirmClear());
        box.addView(clear);

        Button hide = makeButton("×", "閉じる");
        hide.setOnClickListener(v -> hideToolbar());
        box.addView(hide);

        return box;
    }

    private Button makeButton(String text, String description) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(22);
        b.setTextColor(Color.rgb(25, 25, 25));
        b.setAllCaps(false);
        b.setContentDescription(description);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp(62), dp(58));
        lp.setMargins(0, dp(2), 0, dp(2));
        b.setLayoutParams(lp);
        return b;
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("すべて消去")
                .setMessage("ホワイトボードをすべて消去しますか？")
                .setNegativeButton("キャンセル", null)
                .setPositiveButton("消去", (dialog, which) -> {
                    board.clearBoard();
                    scheduleHide();
                })
                .show();
    }

    private void updateModeButton() {
        if (modeButton == null) return;
        modeButton.setText(board.isEraser() ? "消" : "✎");
    }

    private void updateWidthButton() {
        if (widthButton == null) return;
        widthButton.setText(widthIndex == 0 ? "•" : widthIndex == 1 ? "●" : widthIndex == 2 ? "⬤" : "●");
    }

    private void showToolbar() {
        if (toolbar == null) return;
        toolbar.setVisibility(View.VISIBLE);
        View showButton = (View) toolbar.getTag();
        if (showButton != null) showButton.setVisibility(View.GONE);
        scheduleHide();
    }

    private void hideToolbar() {
        if (toolbar == null) return;
        toolbar.setVisibility(View.GONE);
        View showButton = (View) toolbar.getTag();
        if (showButton != null) showButton.setVisibility(View.VISIBLE);
    }

    private void scheduleHide() {
        handler.removeCallbacks(hideToolbarRunnable);
        handler.postDelayed(hideToolbarRunnable, 3000);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
