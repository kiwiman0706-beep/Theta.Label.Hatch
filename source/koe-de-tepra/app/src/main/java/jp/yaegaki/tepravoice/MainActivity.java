package jp.yaegaki.tepravoice;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.kingjim.tepraprint.sdk.TepraPrint;
import jp.co.kingjim.tepraprint.sdk.TepraPrintCallback;
import jp.co.kingjim.tepraprint.sdk.TepraPrintDiscoverPrinter;
import jp.co.kingjim.tepraprint.sdk.TepraPrintParameterKey;
import jp.co.kingjim.tepraprint.sdk.TepraPrintPrintSpeed;
import jp.co.kingjim.tepraprint.sdk.TepraPrintPrintingPhase;
import jp.co.kingjim.tepraprint.sdk.TepraPrintStatusError;
import jp.co.kingjim.tepraprint.sdk.TepraPrintTapeCut;
import jp.co.kingjim.tepraprint.sdk.TepraPrintTapeWidth;

public class MainActivity extends Activity {
    private static final int REQUEST_SPEECH = 1001;
    private static final int REQUEST_PRINTER = 1002;
    private static final int REQUEST_WIFI_PERMISSION = 1003;

    private static final String PREFS = "tepra_voice_label";
    private static final String PREF_HISTORY = "print_history";
    private static final int MAX_HISTORY = 8;
    private static final int MAX_COPIES = 99;

    private static final int COLOR_BG = Color.rgb(245, 248, 247);
    private static final int COLOR_CARD = Color.WHITE;
    private static final int COLOR_BRAND = Color.rgb(8, 127, 115);
    private static final int COLOR_BRAND_DARK = Color.rgb(7, 94, 87);
    private static final int COLOR_BRAND_LIGHT = Color.rgb(221, 245, 240);
    private static final int COLOR_INK = Color.rgb(23, 35, 45);
    private static final int COLOR_MUTED = Color.rgb(91, 107, 117);
    private static final int COLOR_BORDER = Color.rgb(215, 226, 223);
    private static final int COLOR_WARNING = Color.rgb(180, 104, 0);
    private static final int COLOR_WARNING_BG = Color.rgb(255, 241, 216);

    private static final String[] PRINTER_KEYS = {
            TepraPrintDiscoverPrinter.PRINTER_INFO_NAME,
            TepraPrintDiscoverPrinter.PRINTER_INFO_PRODUCT,
            TepraPrintDiscoverPrinter.PRINTER_INFO_USBMDL,
            TepraPrintDiscoverPrinter.PRINTER_INFO_HOST,
            TepraPrintDiscoverPrinter.PRINTER_INFO_PORT,
            TepraPrintDiscoverPrinter.PRINTER_INFO_TYPE,
            TepraPrintDiscoverPrinter.PRINTER_INFO_DOMAIN,
            TepraPrintDiscoverPrinter.PRINTER_INFO_SERIAL_NUMBER,
            TepraPrintDiscoverPrinter.PRINTER_INFO_DEVICE_CLASS,
            TepraPrintDiscoverPrinter.PRINTER_INFO_DEVICE_STATUS
    };

    private final ExecutorService printerExecutor = Executors.newSingleThreadExecutor();

    private ScrollView scrollView;
    private EditText labelInput;
    private TextView printerNameView;
    private TextView printerDetailView;
    private TextView copiesView;
    private TextView tapeWidthView;
    private TextView statusView;
    private ProgressBar progressBar;
    private Button printerButton;
    private Button printButton;
    private Button minusButton;
    private Button plusButton;
    private LinearLayout historyContainer;
    private LabelPreviewView previewView;

    private TepraPrint tepraPrint;
    private Map<String, String> printerInfo;
    private Bitmap activeLabelBitmap;
    private volatile boolean printing;
    private int copies = 1;
    private int detectedTapeWidthMm;
    private String pendingPrintedText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(COLOR_BG);
        getWindow().setNavigationBarColor(COLOR_BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        tepraPrint = new TepraPrint(this);
        tepraPrint.setCallback(new PrinterCallback());
        printerInfo = loadPrinterInformation();

        setContentView(buildContentView());
        updatePrinterCard();
        updateCopies();
        updatePreviewAndAction();
        renderHistory();

        if (hasPrinter()) {
            refreshPrinterStatus();
        } else {
            setStatus("最初にSR5900Pを登録してください", false, false);
        }
    }

    private View buildContentView() {
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG);
        scrollView.setClipToPadding(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(14), dp(20), dp(28));
        content.setFocusableInTouchMode(true);
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final int baseLeft = dp(20);
        final int baseTop = dp(14);
        final int baseRight = dp(20);
        final int baseBottom = dp(28);
        scrollView.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets system = insets.getInsets(WindowInsets.Type.systemBars());
            content.setPadding(baseLeft, baseTop + system.top, baseRight, baseBottom + system.bottom);
            return insets;
        });

        content.addView(buildHeader());
        content.addView(buildVoiceCard(), marginTop(18));
        content.addView(buildOptionsCard(), marginTop(14));
        content.addView(buildPrintButton(), marginTop(16));
        content.addView(buildStatusRow(), marginTop(12));
        content.addView(buildHistoryCard(), marginTop(22));
        content.addView(buildAttribution(), marginTop(22));

        return scrollView;
    }

    private View buildHeader() {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("声でテプラ", 28, COLOR_INK, true);
        TextView subtitle = text("話して、確認して、すぐ印刷", 13, COLOR_MUTED, false);
        titleBlock.addView(title);
        titleBlock.addView(subtitle, marginTop(2));
        row.addView(titleBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        printerButton = new Button(this);
        printerButton.setAllCaps(false);
        printerButton.setText("プリンター設定");
        printerButton.setTextSize(13);
        printerButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        printerButton.setTextColor(COLOR_BRAND_DARK);
        printerButton.setPadding(dp(14), 0, dp(14), 0);
        printerButton.setMinHeight(0);
        printerButton.setMinimumHeight(0);
        printerButton.setBackground(roundRect(COLOR_BRAND_LIGHT, 18, 0, Color.TRANSPARENT));
        printerButton.setOnClickListener(view -> startPrinterSearch());
        row.addView(printerButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)));

        block.addView(row);

        LinearLayout printerCard = new LinearLayout(this);
        printerCard.setOrientation(LinearLayout.VERTICAL);
        printerCard.setPadding(dp(14), dp(11), dp(14), dp(11));
        printerCard.setBackground(roundRect(COLOR_CARD, 14, 1, COLOR_BORDER));
        printerNameView = text("SR5900P 未設定", 15, COLOR_INK, true);
        printerDetailView = text("同じWi-Fi上のプリンターを登録します", 12, COLOR_MUTED, false);
        printerCard.addView(printerNameView);
        printerCard.addView(printerDetailView, marginTop(2));
        block.addView(printerCard, marginTop(12));
        return block;
    }

    private View buildVoiceCard() {
        LinearLayout card = card();
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView prompt = text("ラベルにしたい言葉を話してください", 18, COLOR_INK, true);
        prompt.setGravity(Gravity.CENTER);
        card.addView(prompt);

        TextView note = text("例：冬物の布団 ／ りさ 工作用品", 13, COLOR_MUTED, false);
        note.setGravity(Gravity.CENTER);
        card.addView(note, marginTop(5));

        ImageButton mic = new ImageButton(this);
        mic.setImageResource(jp.yaegaki.tepravoice.R.drawable.ic_mic);
        mic.setScaleType(ImageButton.ScaleType.CENTER);
        mic.setBackground(roundRect(COLOR_BRAND, 56, 0, Color.TRANSPARENT));
        mic.setContentDescription("音声入力を開始");
        mic.setOnClickListener(view -> {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startVoiceInput();
        });
        LinearLayout.LayoutParams micParams = new LinearLayout.LayoutParams(dp(104), dp(104));
        micParams.gravity = Gravity.CENTER_HORIZONTAL;
        micParams.setMargins(0, dp(18), 0, 0);
        card.addView(mic, micParams);

        TextView tap = text("タップして話す", 14, COLOR_BRAND_DARK, true);
        tap.setGravity(Gravity.CENTER);
        card.addView(tap, marginTop(8));

        TextView checkLabel = text("認識した内容（ここで修正できます）", 12, COLOR_MUTED, false);
        card.addView(checkLabel, marginTop(18));

        labelInput = new EditText(this);
        labelInput.setTextSize(24);
        labelInput.setTextColor(COLOR_INK);
        labelInput.setHintTextColor(Color.rgb(150, 163, 168));
        labelInput.setHint("ここに印刷内容が表示されます");
        labelInput.setGravity(Gravity.CENTER);
        labelInput.setMinLines(2);
        labelInput.setMaxLines(3);
        labelInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        labelInput.setBackground(roundRect(Color.WHITE, 14, 1, COLOR_BORDER));
        labelInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreviewAndAction();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        card.addView(labelInput, fullWidthHeight(ViewGroup.LayoutParams.WRAP_CONTENT, 10));

        previewView = new LabelPreviewView(this);
        previewView.setBackground(roundRect(Color.rgb(236, 242, 240), 14, 0, Color.TRANSPARENT));
        card.addView(previewView, fullWidthHeight(dp(108), 12));

        tapeWidthView = text("テープ幅は印刷時に自動判定します", 12, COLOR_MUTED, false);
        tapeWidthView.setGravity(Gravity.CENTER);
        card.addView(tapeWidthView, marginTop(7));
        return card;
    }

    private View buildOptionsCard() {
        LinearLayout card = card();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("印刷枚数", 16, COLOR_INK, true));
        labels.addView(text("初期値は1枚です", 12, COLOR_MUTED, false), marginTop(1));
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        minusButton = stepButton("−");
        minusButton.setContentDescription("印刷枚数を減らす");
        minusButton.setOnClickListener(view -> changeCopies(-1));
        row.addView(minusButton, new LinearLayout.LayoutParams(dp(46), dp(46)));

        copiesView = text("1", 23, COLOR_INK, true);
        copiesView.setGravity(Gravity.CENTER);
        row.addView(copiesView, new LinearLayout.LayoutParams(dp(54), dp(46)));

        plusButton = stepButton("＋");
        plusButton.setContentDescription("印刷枚数を増やす");
        plusButton.setOnClickListener(view -> changeCopies(1));
        row.addView(plusButton, new LinearLayout.LayoutParams(dp(46), dp(46)));

        card.addView(row);
        return card;
    }

    private View buildPrintButton() {
        printButton = new Button(this);
        printButton.setAllCaps(false);
        printButton.setTextSize(18);
        printButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        printButton.setTextColor(Color.WHITE);
        printButton.setBackground(roundRect(COLOR_BRAND, 16, 0, Color.TRANSPARENT));
        printButton.setOnClickListener(view -> onPrimaryAction());
        return wrapFixedHeight(printButton, 62);
    }

    private View buildStatusRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), 0, dp(4), 0);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        row.addView(progressBar, new LinearLayout.LayoutParams(dp(24), dp(24)));

        statusView = text("", 13, COLOR_MUTED, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        statusParams.setMargins(dp(8), 0, 0, 0);
        row.addView(statusView, statusParams);
        return row;
    }

    private View buildHistoryCard() {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);

        block.addView(text("最近のラベル", 17, COLOR_INK, true));
        TextView hint = text("タップすると同じ内容をもう一度使えます", 12, COLOR_MUTED, false);
        block.addView(hint, marginTop(2));

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        block.addView(historyContainer, marginTop(9));
        return block;
    }

    private View buildAttribution() {
        TextView attribution = text("Powerted by KING JIM TEPRA-Print SDK", 11, COLOR_MUTED, false);
        SpannableString content = new SpannableString(attribution.getText());
        int start = content.toString().indexOf("KING JIM");
        content.setSpan(new URLSpan("https://www.kingjim.co.jp/download/tepra/sdk/"),
                start, start + "KING JIM".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        attribution.setText(content);
        attribution.setMovementMethod(LinkMovementMethod.getInstance());
        attribution.setGravity(Gravity.CENTER);
        return attribution;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(roundRect(COLOR_CARD, 18, 1, COLOR_BORDER));
        return card;
    }

    private Button stepButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(22);
        button.setTextColor(COLOR_BRAND_DARK);
        button.setPadding(0, 0, 0, 0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setBackground(roundRect(COLOR_BRAND_LIGHT, 13, 0, Color.TRANSPARENT));
        return button;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private View wrapFixedHeight(View view, int heightDp) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp)));
        return wrapper;
    }

    private LinearLayout.LayoutParams marginTop(int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(topDp), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams fullWidthHeight(int height, int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height);
        params.setMargins(0, dp(topDp), 0, 0);
        return params;
    }

    private GradientDrawable roundRect(int fill, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.JAPAN.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "ラベルにする言葉を話してください");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        try {
            setStatus("音声を聞き取っています…", true, false);
            startActivityForResult(intent, REQUEST_SPEECH);
        } catch (Exception error) {
            setStatus("音声入力を起動できません。文字欄へ直接入力してください", false, true);
            labelInput.requestFocus();
            showKeyboard(labelInput);
        }
    }

    private void onPrimaryAction() {
        String text = normalizedInput();
        if (text.isEmpty()) {
            startVoiceInput();
            return;
        }
        if (!hasPrinter()) {
            startPrinterSearch();
            return;
        }
        requestPrint(text, copies);
    }

    private void changeCopies(int delta) {
        copies = Math.max(1, Math.min(MAX_COPIES, copies + delta));
        updateCopies();
        updatePreviewAndAction();
    }

    private void updateCopies() {
        if (copiesView != null) copiesView.setText(String.valueOf(copies));
        if (minusButton != null) minusButton.setEnabled(copies > 1 && !printing);
        if (plusButton != null) plusButton.setEnabled(copies < MAX_COPIES && !printing);
    }

    private void updatePreviewAndAction() {
        String text = normalizedInput();
        if (previewView != null) previewView.setLabel(text, detectedTapeWidthMm);
        if (printButton == null) return;

        boolean hasText = !text.isEmpty();
        printButton.setEnabled(hasText && !printing);
        printButton.setAlpha(hasText && !printing ? 1f : 0.5f);
        if (printing) {
            printButton.setText("印刷中…");
        } else if (!hasPrinter()) {
            printButton.setText("プリンターを設定する");
        } else {
            printButton.setText("この内容で" + copies + "枚印刷");
        }
    }

    private String normalizedInput() {
        if (labelInput == null || labelInput.getText() == null) return "";
        return labelInput.getText().toString()
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
    }

    private void startPrinterSearch() {
        if (printing) return;
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES},
                        REQUEST_WIFI_PERMISSION);
                return;
            }
        } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_WIFI_PERMISSION);
            return;
        }
        startActivityForResult(new Intent(this, PrinterSearchActivity.class), REQUEST_PRINTER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SPEECH) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    String recognized = results.get(0).trim();
                    labelInput.setText(recognized);
                    labelInput.setSelection(labelInput.length());
                    hideKeyboard();
                    setStatus("内容を確認し、よければ下の印刷ボタンを押してください", false, false);
                } else {
                    setStatus("聞き取れませんでした。もう一度お試しください", false, true);
                }
            } else {
                setStatus("音声入力を中止しました", false, false);
            }
            return;
        }

        if (requestCode == REQUEST_PRINTER && resultCode == RESULT_OK && data != null) {
            HashMap<String, String> selected = new HashMap<>();
            for (String key : PRINTER_KEYS) {
                selected.put(key, value(data.getStringExtra(key)));
            }
            printerInfo = selected;
            savePrinterInformation(selected);
            detectedTapeWidthMm = 0;
            updatePrinterCard();
            updatePreviewAndAction();
            setStatus("SR5900Pを登録しました。接続を確認しています…", true, false);
            refreshPrinterStatus();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_WIFI_PERMISSION) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startPrinterSearch();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Wi-Fi機器の検索権限が必要です")
                    .setMessage("同じWi-Fi上のSR5900Pを見つけるために使用します。位置情報そのものは保存しません。")
                    .setPositiveButton("OK", null)
                    .show();
            setStatus("権限がないためSR5900Pを検索できません", false, true);
        }
    }

    private boolean hasPrinter() {
        return printerInfo != null
                && (!TextUtils.isEmpty(printerInfo.get(TepraPrintDiscoverPrinter.PRINTER_INFO_NAME))
                || !TextUtils.isEmpty(printerInfo.get(TepraPrintDiscoverPrinter.PRINTER_INFO_HOST)));
    }

    private void updatePrinterCard() {
        if (printerNameView == null) return;
        if (!hasPrinter()) {
            printerNameView.setText("SR5900P 未設定");
            printerDetailView.setText("同じWi-Fi上のプリンターを登録します");
            printerButton.setText("設定する");
            printerButton.setTextColor(COLOR_WARNING);
            printerButton.setBackground(roundRect(COLOR_WARNING_BG, 18, 0, Color.TRANSPARENT));
            return;
        }
        String name = value(printerInfo.get(TepraPrintDiscoverPrinter.PRINTER_INFO_NAME));
        if (name.isEmpty()) name = "SR5900P";
        String host = value(printerInfo.get(TepraPrintDiscoverPrinter.PRINTER_INFO_HOST));
        printerNameView.setText(name);
        printerDetailView.setText(host.isEmpty() ? "登録済み" : "Wi-Fi  " + host);
        printerButton.setText("変更");
        printerButton.setTextColor(COLOR_BRAND_DARK);
        printerButton.setBackground(roundRect(COLOR_BRAND_LIGHT, 18, 0, Color.TRANSPARENT));
    }

    private void savePrinterInformation(Map<String, String> information) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        for (String key : PRINTER_KEYS) editor.putString("printer_" + key, value(information.get(key)));
        editor.apply();
    }

    private Map<String, String> loadPrinterInformation() {
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        String name = preferences.getString("printer_" + TepraPrintDiscoverPrinter.PRINTER_INFO_NAME, "");
        String host = preferences.getString("printer_" + TepraPrintDiscoverPrinter.PRINTER_INFO_HOST, "");
        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(host)) return null;
        HashMap<String, String> information = new HashMap<>();
        for (String key : PRINTER_KEYS) {
            information.put(key, preferences.getString("printer_" + key, ""));
        }
        return information;
    }

    private void refreshPrinterStatus() {
        if (!hasPrinter() || printing) return;
        setStatus("SR5900Pへ接続しています…", true, false);
        Map<String, String> selected = new HashMap<>(printerInfo);
        printerExecutor.execute(() -> {
            try {
                tepraPrint.setPrinterInformation(selected);
                Map<String, Integer> status = tepraPrint.fetchPrinterStatus();
                int error = status == null || status.isEmpty()
                        ? TepraPrintStatusError.ConnectionFailed
                        : tepraPrint.getDeviceErrorFromStatus(status);
                if (error != TepraPrintStatusError.NoError) {
                    runOnUiThread(() -> setStatus(errorMessage(error), false, true));
                    return;
                }
                int tape = tepraPrint.getTapeWidthFromStatus(status);
                int width = tapeWidthToMillimeters(tape);
                runOnUiThread(() -> {
                    detectedTapeWidthMm = width;
                    updateTapeWidthLabel();
                    updatePreviewAndAction();
                    setStatus(width > 0
                            ? "印刷できます（装着テープ " + width + "mm）"
                            : "接続しました。テープを確認してください", false, width == 0);
                });
            } catch (Exception error) {
                runOnUiThread(() -> setStatus(
                        "SR5900Pへ接続できません。同じWi-Fiか確認してください", false, true));
            }
        });
    }

    private void requestPrint(String text, int requestedCopies) {
        if (printing) return;
        hideKeyboard();
        printing = true;
        pendingPrintedText = text;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setControlsForPrinting(true);
        setStatus("SR5900Pとテープを確認しています…", true, false);

        Map<String, String> selected = new HashMap<>(printerInfo);
        int safeCopies = Math.max(1, Math.min(MAX_COPIES, requestedCopies));
        printerExecutor.execute(() -> {
            try {
                tepraPrint.setPrinterInformation(selected);
                Map<String, Integer> status = tepraPrint.fetchPrinterStatus();
                int error = status == null || status.isEmpty()
                        ? TepraPrintStatusError.ConnectionFailed
                        : tepraPrint.getDeviceErrorFromStatus(status);
                if (error != TepraPrintStatusError.NoError) {
                    finishPrint(false, errorMessage(error), 0);
                    return;
                }

                int tapeWidth = tepraPrint.getTapeWidthFromStatus(status);
                int tapeWidthMm = tapeWidthToMillimeters(tapeWidth);
                if (tapeWidth == TepraPrintTapeWidth.None
                        || tapeWidth == TepraPrintTapeWidth.Unknown
                        || tapeWidthMm == 0) {
                    finishPrint(false, "テープカートリッジを確認してください", 0);
                    return;
                }

                int resolution = tepraPrint.getResolution();
                int printableHeight = tepraPrint.getPrintableSizeFromTape(tapeWidth);
                if (resolution <= 0 || printableHeight <= 0) {
                    finishPrint(false, "印刷サイズを取得できませんでした", tapeWidthMm);
                    return;
                }

                activeLabelBitmap = LabelRenderer.render(
                        pendingPrintedText, printableHeight, resolution, tapeWidthMm);

                Map<String, Object> parameters = new HashMap<>();
                parameters.put(TepraPrintParameterKey.Copies, safeCopies);
                parameters.put(TepraPrintParameterKey.TapeCut, TepraPrintTapeCut.EachLabel);
                parameters.put(TepraPrintParameterKey.HalfCut, tepraPrint.isSupportHalfCut());
                parameters.put(TepraPrintParameterKey.PrintSpeed, TepraPrintPrintSpeed.PrintSpeedHigh);
                parameters.put(TepraPrintParameterKey.Density, 0);
                parameters.put(TepraPrintParameterKey.TapeWidth, tapeWidth);
                parameters.put(TepraPrintParameterKey.PriorityPrintSetting, false);
                parameters.put(TepraPrintParameterKey.HalfCutContinuous, false);

                runOnUiThread(() -> {
                    detectedTapeWidthMm = tapeWidthMm;
                    updateTapeWidthLabel();
                    updatePreviewAndAction();
                    setStatus("印刷データを送信しています…（" + tapeWidthMm + "mm）", true, false);
                });
                tepraPrint.doPrint(activeLabelBitmap, parameters);
            } catch (Exception error) {
                finishPrint(false, "印刷を開始できませんでした：" + value(error.getMessage()), 0);
            }
        });
    }

    private void setControlsForPrinting(boolean active) {
        if (printerButton != null) printerButton.setEnabled(!active);
        if (labelInput != null) labelInput.setEnabled(!active);
        updateCopies();
        updatePreviewAndAction();
    }

    private void updateTapeWidthLabel() {
        if (tapeWidthView == null) return;
        if (detectedTapeWidthMm > 0) {
            tapeWidthView.setText("装着テープ " + detectedTapeWidthMm
                    + "mm に文字サイズと長さを自動調整");
        } else {
            tapeWidthView.setText("テープ幅は印刷時に自動判定します");
        }
    }

    private int tapeWidthToMillimeters(int tapeWidth) {
        if (tapeWidth == TepraPrintTapeWidth.Normal_4mm) return 4;
        if (tapeWidth == TepraPrintTapeWidth.Normal_6mm) return 6;
        if (tapeWidth == TepraPrintTapeWidth.Normal_9mm) return 9;
        if (tapeWidth == TepraPrintTapeWidth.Normal_12mm) return 12;
        if (tapeWidth == TepraPrintTapeWidth.Normal_18mm) return 18;
        if (tapeWidth == TepraPrintTapeWidth.Normal_24mm
                || tapeWidth == TepraPrintTapeWidth.Cable_24mm) return 24;
        if (tapeWidth == TepraPrintTapeWidth.Normal_36mm
                || tapeWidth == TepraPrintTapeWidth.Cable_36mm) return 36;
        return 0;
    }

    private String errorMessage(int error) {
        if (error == TepraPrintStatusError.ConnectionFailed) {
            return "SR5900Pへ接続できません。同じWi-Fiか確認してください";
        }
        if (error == TepraPrintStatusError.NoTapeCartridge) return "テープが入っていません";
        if (error == TepraPrintStatusError.CoverOpen) return "SR5900Pのカバーが開いています";
        if (error == TepraPrintStatusError.TapeEnd) return "テープがなくなりました";
        if (error == TepraPrintStatusError.CutterError) return "カッターエラーです";
        if (error == TepraPrintStatusError.OtherUsing
                || error == TepraPrintStatusError.DeviceUsing) {
            return "SR5900Pはほかの端末で使用中です";
        }
        if (error == TepraPrintStatusError.FirmwareUpdating) {
            return "SR5900Pの更新が終わるまでお待ちください";
        }
        if (error == TepraPrintStatusError.HeadOverheated
                || error == TepraPrintStatusError.TemperatureError) {
            return "SR5900Pの温度が下がるまでお待ちください";
        }
        return "SR5900Pエラー（" + Integer.toHexString(error) + "）";
    }

    private void setStatus(String message, boolean busy, boolean warning) {
        if (statusView == null || progressBar == null) return;
        statusView.setText(message);
        statusView.setTextColor(warning ? COLOR_WARNING : COLOR_MUTED);
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void finishPrint(boolean success, String message, int tapeWidthMm) {
        printing = false;
        runOnUiThread(() -> {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (activeLabelBitmap != null) {
                activeLabelBitmap.recycle();
                activeLabelBitmap = null;
            }
            if (tapeWidthMm > 0) {
                detectedTapeWidthMm = tapeWidthMm;
                updateTapeWidthLabel();
            }
            setControlsForPrinting(false);
            setStatus(message, false, !success);
            if (success) {
                addHistory(pendingPrintedText);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> loadHistory() {
        ArrayList<String> items = new ArrayList<>();
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_HISTORY, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length() && items.size() < MAX_HISTORY; i++) {
                String item = array.optString(i, "").trim();
                if (!item.isEmpty()) items.add(item);
            }
        } catch (Exception ignored) { }
        return items;
    }

    private void addHistory(String item) {
        String normalized = value(item).trim();
        if (normalized.isEmpty()) return;
        List<String> current = loadHistory();
        current.remove(normalized);
        current.add(0, normalized);
        while (current.size() > MAX_HISTORY) current.remove(current.size() - 1);
        JSONArray array = new JSONArray();
        for (String value : current) array.put(value);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_HISTORY, array.toString())
                .apply();
        renderHistory();
    }

    private void renderHistory() {
        if (historyContainer == null) return;
        historyContainer.removeAllViews();
        List<String> history = loadHistory();
        if (history.isEmpty()) {
            TextView empty = text("印刷すると、ここからワンタップで再利用できます", 13, COLOR_MUTED, false);
            empty.setPadding(dp(14), dp(14), dp(14), dp(14));
            empty.setBackground(roundRect(Color.WHITE, 13, 1, COLOR_BORDER));
            historyContainer.addView(empty);
            return;
        }
        for (int i = 0; i < history.size(); i++) {
            String item = history.get(i);
            Button button = new Button(this);
            button.setAllCaps(false);
            button.setText("↺  " + item.replace('\n', ' '));
            button.setTextSize(14);
            button.setTextColor(COLOR_INK);
            button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            button.setPadding(dp(14), dp(8), dp(14), dp(8));
            button.setMinHeight(0);
            button.setMinimumHeight(0);
            button.setBackground(roundRect(Color.WHITE, 13, 1, COLOR_BORDER));
            button.setOnClickListener(view -> {
                labelInput.setText(item);
                labelInput.setSelection(labelInput.length());
                setStatus("内容を確認して印刷してください", false, false);
                scrollView.smoothScrollTo(0, labelInput.getTop());
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
            if (i > 0) params.setMargins(0, dp(7), 0, 0);
            historyContainer.addView(button, params);
        }
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus == null) focus = labelInput;
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null && focus != null) manager.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        if (focus != null) focus.clearFocus();
    }

    private void showKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null) manager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    @Override
    protected void onDestroy() {
        if (tepraPrint != null && printing) tepraPrint.cancelPrint();
        printerExecutor.shutdownNow();
        super.onDestroy();
    }

    private class PrinterCallback implements TepraPrintCallback {
        @Override
        public void onChangePrintOperationPhase(TepraPrint print, int phase) {
            if (phase == TepraPrintPrintingPhase.Prepare) {
                runOnUiThread(() -> setStatus("印刷データを準備しています…", true, false));
            } else if (phase == TepraPrintPrintingPhase.Processing) {
                runOnUiThread(() -> setStatus("SR5900Pへ送信しています…", true, false));
            } else if (phase == TepraPrintPrintingPhase.WaitingForPrint) {
                runOnUiThread(() -> setStatus("SR5900Pで印刷しています…", true, false));
            } else if (phase == TepraPrintPrintingPhase.Complete) {
                finishPrint(true, copies > 1
                        ? copies + "枚の印刷が完了しました"
                        : "印刷が完了しました", detectedTapeWidthMm);
            }
        }

        @Override
        public void onSuspendPrintOperation(TepraPrint print, int errorStatus, int deviceStatus) {
            int error = deviceStatus != 0 ? deviceStatus : errorStatus;
            runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                    .setTitle("印刷を再開しますか？")
                    .setMessage(errorMessage(error))
                    .setPositiveButton("再開", (dialog, which) -> print.resumeOfPrint())
                    .setNegativeButton("中止", (dialog, which) -> {
                        print.cancelPrint();
                        finishPrint(false, "印刷を中止しました", detectedTapeWidthMm);
                    })
                    .setCancelable(false)
                    .show());
        }

        @Override
        public void onAbortPrintOperation(TepraPrint print, int errorStatus, int deviceStatus) {
            int error = deviceStatus != 0 ? deviceStatus : errorStatus;
            finishPrint(false, errorMessage(error), detectedTapeWidthMm);
        }

        @Override public void onChangeTapeFeedOperationPhase(TepraPrint print, int phase) { }
        @Override public void onAbortTapeFeedOperation(TepraPrint print, int errorStatus, int deviceStatus) { }
    }

    static final class LabelRenderer {
        private static final float MAX_LENGTH_MM = 120f;
        private static final float MIN_LENGTH_MM = 28f;

        private LabelRenderer() { }

        static Bitmap render(String source, int height, int resolution, int tapeWidthMm) {
            String[] lines = chooseLines(source, tapeWidthMm);
            int padding = Math.max(5, Math.round(height * 0.09f));
            int maxWidth = millimetersToPixels(MAX_LENGTH_MM, resolution);
            int minWidth = millimetersToPixels(MIN_LENGTH_MM, resolution);
            int textWidthLimit = Math.max(1, maxWidth - padding * 2);

            TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            paint.setColor(Color.BLACK);
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);

            float low = 7f;
            float high = Math.max(8f, height * (lines.length == 1 ? 0.92f : 0.56f));
            for (int i = 0; i < 24; i++) {
                float candidate = (low + high) / 2f;
                paint.setTextSize(candidate);
                Paint.FontMetrics metrics = paint.getFontMetrics();
                float lineHeight = metrics.descent - metrics.ascent;
                float totalHeight = lineHeight * lines.length + lineHeight * 0.05f * (lines.length - 1);
                float widest = widestLine(paint, lines);
                if (totalHeight <= height - padding * 2f && widest <= textWidthLimit) {
                    low = candidate;
                } else {
                    high = candidate;
                }
            }
            paint.setTextSize(low);

            for (int i = 0; i < lines.length; i++) {
                lines[i] = TextUtils.ellipsize(lines[i], paint, textWidthLimit,
                        TextUtils.TruncateAt.END).toString();
            }
            int width = Math.max(minWidth,
                    Math.min(maxWidth, Math.round(widestLine(paint, lines) + padding * 2f)));
            width = Math.max(width, height + padding * 2);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);

            Paint.FontMetrics metrics = paint.getFontMetrics();
            float lineHeight = metrics.descent - metrics.ascent;
            float spacing = lineHeight * 0.05f;
            float totalHeight = lineHeight * lines.length + spacing * (lines.length - 1);
            float baseline = (height - totalHeight) / 2f - metrics.ascent;
            float centerX = width / 2f;
            for (String line : lines) {
                canvas.drawText(line, centerX, baseline, paint);
                baseline += lineHeight + spacing;
            }
            return bitmap;
        }

        static String[] chooseLines(String source, int tapeWidthMm) {
            String text = value(source).replace('\r', '\n').trim();
            if (text.isEmpty()) return new String[]{"ラベル"};

            String[] explicit = text.split("\\n+");
            if (explicit.length >= 2 && tapeWidthMm >= 9) {
                String first = explicit[0].trim();
                StringBuilder second = new StringBuilder();
                for (int i = 1; i < explicit.length; i++) {
                    if (second.length() > 0) second.append(' ');
                    second.append(explicit[i].trim());
                }
                return new String[]{first, second.toString()};
            }
            text = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
            int count = text.codePointCount(0, text.length());
            int threshold = tapeWidthMm >= 18 ? 18 : 14;
            if (tapeWidthMm >= 9 && count > threshold) {
                return splitBalanced(text);
            }
            return new String[]{text};
        }

        private static String[] splitBalanced(String text) {
            int count = text.codePointCount(0, text.length());
            int target = count / 2;
            int best = target;
            int bestDistance = Integer.MAX_VALUE;
            for (int cp = Math.max(1, target - 5); cp <= Math.min(count - 1, target + 5); cp++) {
                int index = text.offsetByCodePoints(0, cp);
                char before = text.charAt(Math.max(0, index - 1));
                char after = text.charAt(index);
                if (isSeparator(before) || isSeparator(after)) {
                    int distance = Math.abs(cp - target);
                    if (distance < bestDistance) {
                        best = cp;
                        bestDistance = distance;
                    }
                }
            }
            int index = text.offsetByCodePoints(0, best);
            String left = trimSeparators(text.substring(0, index));
            String right = trimSeparators(text.substring(index));
            if (left.isEmpty() || right.isEmpty()) {
                left = text.substring(0, index).trim();
                right = text.substring(index).trim();
            }
            return new String[]{left, right};
        }

        private static boolean isSeparator(char value) {
            return Character.isWhitespace(value)
                    || value == '・' || value == '、' || value == '。'
                    || value == '/' || value == '／' || value == '-';
        }

        private static String trimSeparators(String value) {
            return value.replaceAll("^[\\s・、。/／-]+|[\\s・、。/／-]+$", "").trim();
        }

        private static float widestLine(Paint paint, String[] lines) {
            float widest = 0f;
            for (String line : lines) widest = Math.max(widest, paint.measureText(line));
            return widest;
        }

        private static int millimetersToPixels(float millimeters, int resolution) {
            return Math.max(1, Math.round(millimeters / 25.4f * resolution));
        }
    }

    private static final class LabelPreviewView extends View {
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String label = "";
        private int tapeWidthMm;

        LabelPreviewView(Context context) {
            super(context);
            labelPaint.setColor(Color.WHITE);
            labelPaint.setShadowLayer(5f, 0f, 2f, Color.argb(38, 0, 0, 0));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(1.2f);
            borderPaint.setColor(Color.rgb(196, 207, 204));
            textPaint.setColor(Color.BLACK);
            textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            textPaint.setTextAlign(Paint.Align.CENTER);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setContentDescription("印刷プレビュー");
        }

        void setLabel(String value, int widthMm) {
            label = value == null ? "" : value;
            tapeWidthMm = widthMm;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float density = getResources().getDisplayMetrics().density;
            float margin = 14f * density;
            float availableWidth = getWidth() - margin * 2f;
            float tapeHeight = Math.min(getHeight() - margin * 2f,
                    (tapeWidthMm > 0 ? Math.min(36, Math.max(9, tapeWidthMm)) : 18) * 2.2f * density);
            tapeHeight = Math.max(36f * density, tapeHeight);

            int charCount = Math.max(3, label.codePointCount(0, label.length()));
            float desired = Math.min(availableWidth, Math.max(tapeHeight * 2.2f,
                    tapeHeight * (1.5f + Math.min(charCount, 18) * 0.17f)));
            float left = (getWidth() - desired) / 2f;
            float top = (getHeight() - tapeHeight) / 2f;
            RectF tape = new RectF(left, top, left + desired, top + tapeHeight);
            float radius = Math.min(7f * density, tapeHeight / 5f);
            canvas.drawRoundRect(tape, radius, radius, labelPaint);
            canvas.drawRoundRect(tape, radius, radius, borderPaint);

            String shown = label.trim();
            if (shown.isEmpty()) {
                shown = "印刷プレビュー";
                textPaint.setColor(Color.rgb(150, 158, 160));
                textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            } else {
                textPaint.setColor(Color.BLACK);
                textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            }
            String[] lines = LabelRenderer.chooseLines(shown, tapeWidthMm > 0 ? tapeWidthMm : 18);
            float maxTextWidth = desired - 24f * density;
            float maxTextHeight = tapeHeight - 10f * density;
            float low = 8f * density;
            float high = Math.max(low, tapeHeight * (lines.length == 1 ? 0.70f : 0.42f));
            for (int i = 0; i < 18; i++) {
                float size = (low + high) / 2f;
                textPaint.setTextSize(size);
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float total = (fm.descent - fm.ascent) * lines.length;
                if (total <= maxTextHeight && LabelRenderer.widestLine(textPaint, lines) <= maxTextWidth) {
                    low = size;
                } else {
                    high = size;
                }
            }
            textPaint.setTextSize(low);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float lineHeight = fm.descent - fm.ascent;
            float baseline = tape.centerY() - lineHeight * lines.length / 2f - fm.ascent;
            for (String line : lines) {
                String fitted = TextUtils.ellipsize(line, textPaint, maxTextWidth,
                        TextUtils.TruncateAt.END).toString();
                canvas.drawText(fitted, tape.centerX(), baseline, textPaint);
                baseline += lineHeight;
            }
        }
    }
}
