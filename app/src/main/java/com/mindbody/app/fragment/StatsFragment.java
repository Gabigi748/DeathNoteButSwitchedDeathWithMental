package com.mindbody.app.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.mindbody.app.R;
import com.mindbody.app.network.ApiService;
import com.mindbody.app.network.RetrofitClient;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsFragment extends Fragment {

    private LineChart chartMood;
    private PieChart chartSymptoms;
    private TextView tvCorrelation;
    private MaterialButton btn7Days, btn30Days, btnExportPdf;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chartMood = view.findViewById(R.id.chart_mood);
        chartSymptoms = view.findViewById(R.id.chart_symptoms);
        tvCorrelation = view.findViewById(R.id.tv_correlation);
        btn7Days = view.findViewById(R.id.btn_7days);
        btn30Days = view.findViewById(R.id.btn_30days);
        btnExportPdf = view.findViewById(R.id.btn_export_pdf);

        apiService = RetrofitClient.getInstance(requireContext()).getApiService();

        setupCharts();
        loadMoodData(7);
        loadSymptomData();
        loadCorrelation();

        btn7Days.setOnClickListener(v -> loadMoodData(7));
        btn30Days.setOnClickListener(v -> loadMoodData(30));
        btnExportPdf.setOnClickListener(v -> exportPdf());
    }

    private void setupCharts() {
        // Line chart setup
        chartMood.getDescription().setEnabled(false);
        chartMood.setTouchEnabled(true);
        chartMood.setDragEnabled(true);
        chartMood.setScaleEnabled(false);
        chartMood.getAxisRight().setEnabled(false);
        chartMood.getAxisLeft().setAxisMinimum(0f);
        chartMood.getAxisLeft().setAxisMaximum(5f);
        chartMood.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartMood.setNoDataText("載入中...");

        // Pie chart setup
        chartSymptoms.getDescription().setEnabled(false);
        chartSymptoms.setUsePercentValues(true);
        chartSymptoms.setEntryLabelTextSize(12f);
        chartSymptoms.setNoDataText("載入中...");
    }

    private void loadMoodData(int days) {
        apiService.getMoodStats(days).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object dataObj = response.body().get("data");
                    List<Map<String, Object>> data = (dataObj instanceof List) ? (List<Map<String, Object>>) dataObj : new ArrayList<>();
                    List<Entry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();

                    for (int i = 0; i < data.size(); i++) {
                        Map<String, Object> item = data.get(i);
                        Object scoreObj = item.get("mood_score");
                        float score = 0;
                        if (scoreObj instanceof Double) {
                            score = ((Double) scoreObj).floatValue();
                        }
                        entries.add(new Entry(i, score));

                        Object dateObj = item.get("date");
                        if (dateObj != null) {
                            String dateStr = dateObj.toString();
                            // Show only MM-DD
                            if (dateStr.length() >= 10) {
                                labels.add(dateStr.substring(5, 10));
                            } else {
                                labels.add(dateStr);
                            }
                        }
                    }

                    if (entries.isEmpty()) {
                        chartMood.setNoDataText("尚無資料");
                        chartMood.invalidate();
                        return;
                    }

                    LineDataSet dataSet = new LineDataSet(entries, "情緒分數");
                    dataSet.setColor(Color.parseColor("#4CAF50"));
                    dataSet.setCircleColor(Color.parseColor("#4CAF50"));
                    dataSet.setLineWidth(2f);
                    dataSet.setCircleRadius(4f);
                    dataSet.setDrawValues(false);
                    dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                    LineData lineData = new LineData(dataSet);
                    chartMood.setData(lineData);
                    chartMood.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                    chartMood.getXAxis().setGranularity(1f);
                    chartMood.animateX(500);
                    chartMood.invalidate();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                chartMood.setNoDataText("載入失敗");
                chartMood.invalidate();
            }
        });
    }

    private void loadSymptomData() {
        apiService.getSymptomStats(30).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object dataObj = response.body().get("data");
                    List<Map<String, Object>> data = (dataObj instanceof List) ? (List<Map<String, Object>>) dataObj : new ArrayList<>();
                    List<PieEntry> entries = new ArrayList<>();

                    for (Map<String, Object> item : data) {
                        String name = (String) item.get("symptom_name");
                        Object countObj = item.get("count");
                        float count = 0;
                        if (countObj instanceof Double) {
                            count = ((Double) countObj).floatValue();
                        }
                        if (name != null && count > 0) {
                            entries.add(new PieEntry(count, name));
                        }
                    }

                    if (entries.isEmpty()) {
                        chartSymptoms.setNoDataText("尚無資料");
                        chartSymptoms.invalidate();
                        return;
                    }

                    PieDataSet dataSet = new PieDataSet(entries, "");
                    dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                    dataSet.setValueTextSize(12f);
                    dataSet.setValueTextColor(Color.WHITE);

                    PieData pieData = new PieData(dataSet);
                    chartSymptoms.setData(pieData);
                    chartSymptoms.animateY(500);
                    chartSymptoms.invalidate();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                chartSymptoms.setNoDataText("載入失敗");
                chartSymptoms.invalidate();
            }
        });
    }

    private void loadCorrelation() {
        apiService.getCorrelation().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    tvCorrelation.setText("尚無足夠資料進行關聯分析");
                    return;
                }
                Map<String, Object> body = response.body();

                // Summary
                int totalDays = 0, lowMoodDays = 0;
                Object sumObj = body.get("summary");
                if (sumObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> summary = (Map<String, Object>) sumObj;
                    Object total = summary.get("total_checkin_days");
                    Object low = summary.get("low_mood_days");
                    if (total instanceof Double) totalDays = ((Double) total).intValue();
                    if (low instanceof Double) lowMoodDays = ((Double) low).intValue();
                }

                StringBuilder sb = new StringBuilder();
                sb.append("📊 累計打卡 ").append(totalDays).append(" 天");
                if (lowMoodDays > 0) {
                    sb.append("（其中 ").append(lowMoodDays).append(" 天情緒低落）");
                }
                sb.append("\n\n");

                if (totalDays < 3) {
                    sb.append("💡 持續打卡幾天後，這裡會出現更個人化的身心關聯洞察。");
                    tvCorrelation.setText(sb.toString());
                    return;
                }

                // Low mood symptoms
                Object lowObj = body.get("low_mood_symptoms");
                if (lowObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> lowSymptoms = (List<Map<String, Object>>) lowObj;
                    if (!lowSymptoms.isEmpty()) {
                        sb.append("😔 情緒低落時最常見的症狀：\n");
                        int shown = 0;
                        for (Map<String, Object> s : lowSymptoms) {
                            if (shown >= 3) break;
                            Object name = s.get("symptom_name");
                            Object pct = s.get("percentage");
                            if (name != null) {
                                sb.append("  • ").append(name);
                                if (pct instanceof Double) {
                                    sb.append("（").append(((Double) pct).intValue()).append("%）");
                                }
                                sb.append("\n");
                                shown++;
                            }
                        }
                        sb.append("\n");
                    }
                }

                // High mood symptoms
                Object highObj = body.get("high_mood_symptoms");
                if (highObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> highSymptoms = (List<Map<String, Object>>) highObj;
                    if (!highSymptoms.isEmpty()) {
                        sb.append("😊 情緒愉悅時記錄的症狀：\n");
                        int shown = 0;
                        for (Map<String, Object> s : highSymptoms) {
                            if (shown >= 3) break;
                            Object name = s.get("symptom_name");
                            Object cnt = s.get("count");
                            if (name != null) {
                                sb.append("  • ").append(name);
                                if (cnt instanceof Double) {
                                    sb.append("（").append(((Double) cnt).intValue()).append(" 次）");
                                }
                                sb.append("\n");
                                shown++;
                            }
                        }
                    }
                }

                if (sb.toString().endsWith("\n\n")) {
                    sb.append("尚未累積足夠的高低情緒症狀資料，繼續打卡會有更多洞察。");
                }

                tvCorrelation.setText(sb.toString());
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                tvCorrelation.setText("載入失敗");
            }
        });
    }

    private void exportPdf() {
        if (getActivity() == null) return;

        try {
            PdfDocument pdf = new PdfDocument();
            int pageWidth = 595;   // A4 in points
            int pageHeight = 842;
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint titlePaint = new Paint();
            titlePaint.setColor(android.graphics.Color.parseColor("#4CAF50"));
            titlePaint.setTextSize(24f);
            titlePaint.setFakeBoldText(true);

            Paint headerPaint = new Paint();
            headerPaint.setColor(android.graphics.Color.parseColor("#333333"));
            headerPaint.setTextSize(16f);
            headerPaint.setFakeBoldText(true);

            Paint textPaint = new Paint();
            textPaint.setColor(android.graphics.Color.BLACK);
            textPaint.setTextSize(13f);

            Paint smallPaint = new Paint();
            smallPaint.setColor(android.graphics.Color.GRAY);
            smallPaint.setTextSize(11f);

            int x = 40;
            int y = 60;
            canvas.drawText("身心健康報表", x, y, titlePaint);
            y += 25;
            String today = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            canvas.drawText("產出時間：" + today, x, y, smallPaint);
            y += 35;

            // 心情趨勢圖
            canvas.drawText("情緒趨勢", x, y, headerPaint);
            y += 10;
            try {
                Bitmap moodBmp = chartMood.getChartBitmap();
                if (moodBmp != null) {
                    int targetW = pageWidth - 80;
                    int targetH = (int) ((float) moodBmp.getHeight() / moodBmp.getWidth() * targetW);
                    if (targetH > 250) targetH = 250;
                    Bitmap scaled = Bitmap.createScaledBitmap(moodBmp, targetW, targetH, true);
                    canvas.drawBitmap(scaled, x, y, null);
                    y += targetH + 20;
                }
            } catch (Exception ignored) {}

            // 症狀分佈圖
            canvas.drawText("症狀分佈", x, y, headerPaint);
            y += 10;
            try {
                Bitmap symBmp = chartSymptoms.getChartBitmap();
                if (symBmp != null) {
                    int targetW = pageWidth - 80;
                    int targetH = (int) ((float) symBmp.getHeight() / symBmp.getWidth() * targetW);
                    if (targetH > 250) targetH = 250;
                    Bitmap scaled = Bitmap.createScaledBitmap(symBmp, targetW, targetH, true);
                    canvas.drawBitmap(scaled, x, y, null);
                    y += targetH + 20;
                }
            } catch (Exception ignored) {}

            // 關聯分析
            canvas.drawText("身心關聯分析", x, y, headerPaint);
            y += 22;
            String correlation = tvCorrelation.getText().toString();
            for (String line : correlation.split("\\n")) {
                if (y > pageHeight - 50) break;
                canvas.drawText(line, x, y, textPaint);
                y += 18;
            }

            pdf.finishPage(page);

            // 寫入下載資料夾
            String fileName = "MindBody_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
            String savedPath;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = requireContext().getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (uri == null) {
                    Toast.makeText(requireContext(), "建立檔案失敗", Toast.LENGTH_SHORT).show();
                    pdf.close();
                    return;
                }
                OutputStream out = requireContext().getContentResolver().openOutputStream(uri);
                if (out != null) {
                    pdf.writeTo(out);
                    out.close();
                }
                savedPath = "下載/" + fileName;
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                pdf.writeTo(fos);
                fos.close();
                savedPath = file.getAbsolutePath();
            }

            pdf.close();
            Toast.makeText(requireContext(), "PDF 已儲存到：" + savedPath, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "匯出失敗：" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
