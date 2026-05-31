package com.mindbody.app.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
                if (response.isSuccessful() && response.body() != null) {
                    Object summary = response.body().get("summary");
                    if (summary != null) {
                        tvCorrelation.setText(summary.toString());
                    } else {
                        tvCorrelation.setText("尚無足夠資料進行關聯分析");
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                tvCorrelation.setText("載入失敗");
            }
        });
    }

    private void exportPdf() {
        if (getActivity() == null) return;

        WebView webView = new WebView(requireContext());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                createPdf(view);
            }
        });

        // Build HTML report
        String html = "<html><head><meta charset='utf-8'><style>"
                + "body{font-family:sans-serif;padding:20px;}"
                + "h1{color:#4CAF50;}"
                + "h2{color:#333;margin-top:20px;}"
                + "</style></head><body>"
                + "<h1>身心健康報表</h1>"
                + "<h2>身心關聯分析</h2>"
                + "<p>" + tvCorrelation.getText().toString() + "</p>"
                + "</body></html>";

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private void createPdf(WebView webView) {
        if (getActivity() == null) return;

        PrintManager printManager = (PrintManager) requireActivity().getSystemService(android.content.Context.PRINT_SERVICE);
        if (printManager != null) {
            String jobName = "身心健康報表";
            PrintAttributes attributes = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .build();
            printManager.print(jobName, webView.createPrintDocumentAdapter(jobName), attributes);
        } else {
            Toast.makeText(requireContext(), "無法匯出 PDF", Toast.LENGTH_SHORT).show();
        }
    }
}
