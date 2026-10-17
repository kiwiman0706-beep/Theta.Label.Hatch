package jp.yaegaki.tepravoice;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.kingjim.tepraprint.sdk.TepraPrintDiscoverConnectionType;
import jp.co.kingjim.tepraprint.sdk.TepraPrintDiscoverPrinter;
import jp.co.kingjim.tepraprint.sdk.TepraPrintDiscoverPrinterCallback;

public class PrinterSearchActivity extends Activity implements TepraPrintDiscoverPrinterCallback {
    private static final int COLOR_BG = Color.rgb(245, 248, 247);
    private static final int COLOR_BRAND = Color.rgb(8, 127, 115);
    private static final int COLOR_INK = Color.rgb(23, 35, 45);
    private static final int COLOR_MUTED = Color.rgb(91, 107, 117);
    private static final int COLOR_BORDER = Color.rgb(215, 226, 223);

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

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Map<String, String>> printers = new ArrayList<>();
    private final List<String> rows = new ArrayList<>();

    private ArrayAdapter<String> adapter;
    private TepraPrintDiscoverPrinter discoverer;
    private TextView statusView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(COLOR_BG);
        getWindow().setNavigationBarColor(COLOR_BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildContentView());
        startSearch();
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));
        root.setBackgroundColor(COLOR_BG);
        final int left = dp(20), top = dp(18), right = dp(20), bottom = dp(24);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(left, top + bars.top, right, bottom + bars.bottom);
            return insets;
        });

        TextView title = text("SR5900Pを探す", 27, COLOR_INK, true);
        root.addView(title);

        TextView guide = text(
                "スマートフォンとSR5900Pを同じWi-Fiに接続してください。見つかったプリンターをタップすると登録できます。",
                14, COLOR_MUTED, false);
        guide.setLineSpacing(0, 1.2f);
        root.addView(guide, marginTop(7));

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(dp(14), dp(12), dp(14), dp(12));
        searchRow.setBackground(roundRect(Color.WHITE, 14, 1, COLOR_BORDER));

        progressBar = new ProgressBar(this);
        searchRow.addView(progressBar, new LinearLayout.LayoutParams(dp(27), dp(27)));
        statusView = text("検索しています…", 14, COLOR_BRAND, true);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        statusParams.setMargins(dp(10), 0, 0, 0);
        searchRow.addView(statusView, statusParams);
        root.addView(searchRow, marginTop(18));

        ListView listView = new ListView(this);
        listView.setDividerHeight(dp(8));
        listView.setBackgroundColor(Color.TRANSPARENT);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rows) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(COLOR_INK);
                view.setTextSize(16);
                view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                view.setGravity(Gravity.CENTER_VERTICAL);
                view.setPadding(dp(16), dp(12), dp(16), dp(12));
                view.setBackground(roundRect(Color.WHITE, 14, 1, COLOR_BORDER));
                return view;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> selectPrinter(position));
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        listParams.setMargins(0, dp(14), 0, dp(14));
        root.addView(listView, listParams);

        Button retry = new Button(this);
        retry.setAllCaps(false);
        retry.setText("もう一度探す");
        retry.setTextColor(Color.WHITE);
        retry.setTextSize(16);
        retry.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        retry.setBackground(roundRect(COLOR_BRAND, 14, 0, Color.TRANSPARENT));
        retry.setOnClickListener(view -> startSearch());
        root.addView(retry, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        TextView hint = text(
                "見つからない場合：SR5900Pの電源、無線LANランプ、スマートフォン側のWi-Fi接続を確認してください。ゲストWi-Fiでは機器同士が見えないことがあります。",
                12, COLOR_MUTED, false);
        hint.setLineSpacing(0, 1.15f);
        root.addView(hint, marginTop(12));
        return root;
    }

    private void startSearch() {
        stopSearch();
        printers.clear();
        rows.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.VISIBLE);
        statusView.setText("検索しています…");
        statusView.setTextColor(COLOR_BRAND);

        try {
            List<String> serviceTypes = Arrays.asList("_pdl-datastream._tcp.local.");
            List<String> modelNames = Arrays.asList("(KING JIM TEPRA PRO SR5900P)");
            EnumSet<TepraPrintDiscoverConnectionType> connections =
                    EnumSet.of(TepraPrintDiscoverConnectionType.ConnectionTypeNetwork);
            discoverer = new TepraPrintDiscoverPrinter(serviceTypes, modelNames, connections);
            discoverer.setCallback(this);
            discoverer.enableMulticastOnWifi(this);
            discoverer.startDiscover(this);
        } catch (Exception error) {
            progressBar.setVisibility(View.GONE);
            statusView.setText("検索を開始できませんでした。Wi-Fi接続を確認してください。");
            statusView.setTextColor(Color.rgb(180, 104, 0));
            return;
        }

        handler.postDelayed(() -> {
            if (printers.isEmpty() && discoverer != null) {
                progressBar.setVisibility(View.GONE);
                statusView.setText("見つかりませんでした。接続を確認して、もう一度お試しください。");
                statusView.setTextColor(Color.rgb(180, 104, 0));
            }
        }, 14000);
    }

    @Override
    public void onFindPrinter(TepraPrintDiscoverPrinter source, Map<String, String> printer) {
        HashMap<String, String> found = new HashMap<>();
        for (String key : PRINTER_KEYS) found.put(key, value(printer.get(key)));
        String host = found.get(TepraPrintDiscoverPrinter.PRINTER_INFO_HOST);
        String name = found.get(TepraPrintDiscoverPrinter.PRINTER_INFO_NAME);
        String identifier = host + "|" + name;

        handler.post(() -> {
            for (Map<String, String> existing : printers) {
                String existingId = value(existing.get(TepraPrintDiscoverPrinter.PRINTER_INFO_HOST))
                        + "|" + value(existing.get(TepraPrintDiscoverPrinter.PRINTER_INFO_NAME));
                if (identifier.equals(existingId)) return;
            }
            printers.add(found);
            String displayName = TextUtils.isEmpty(name) ? "SR5900P" : name;
            rows.add(TextUtils.isEmpty(host) ? displayName : displayName + "\n" + host);
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
            statusView.setText("見つかりました。タップして登録してください。");
            statusView.setTextColor(COLOR_BRAND);
        });
    }

    @Override
    public void onRemovePrinter(TepraPrintDiscoverPrinter source, Map<String, String> printer) {
        String host = value(printer.get(TepraPrintDiscoverPrinter.PRINTER_INFO_HOST));
        String name = value(printer.get(TepraPrintDiscoverPrinter.PRINTER_INFO_NAME));
        handler.post(() -> {
            for (int i = printers.size() - 1; i >= 0; i--) {
                Map<String, String> existing = printers.get(i);
                if (host.equals(value(existing.get(TepraPrintDiscoverPrinter.PRINTER_INFO_HOST)))
                        && name.equals(value(existing.get(TepraPrintDiscoverPrinter.PRINTER_INFO_NAME)))) {
                    printers.remove(i);
                    rows.remove(i);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void selectPrinter(int position) {
        if (position < 0 || position >= printers.size()) return;
        Intent result = new Intent();
        Map<String, String> selected = printers.get(position);
        for (String key : PRINTER_KEYS) result.putExtra(key, value(selected.get(key)));
        setResult(RESULT_OK, result);
        finish();
    }

    private void stopSearch() {
        handler.removeCallbacksAndMessages(null);
        if (discoverer != null) {
            try {
                discoverer.stopDiscover();
                discoverer.disableMulticastOnWifi();
            } catch (Exception ignored) { }
            discoverer = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopSearch();
        super.onDestroy();
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams marginTop(int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
