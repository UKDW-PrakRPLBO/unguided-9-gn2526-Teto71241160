package com.ukdw.rplbo.soal_ug_javafx;

import com.ukdw.rplbo.soal_ug_javafx.data.Mahasiswa_table;
import com.ukdw.rplbo.soal_ug_javafx.data.Matakuliah_table;
import com.ukdw.rplbo.soal_ug_javafx.data.Nilai_table;
import com.ukdw.rplbo.soal_ug_javafx.entity.Mahasiswa;
import com.ukdw.rplbo.soal_ug_javafx.entity.Matakuliah;
import com.ukdw.rplbo.soal_ug_javafx.entity.Nilai;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AppController {
    @FXML
    private ComboBox<String> option;
    @FXML
    private TableView<Object> table;
    @FXML
    private TableColumn<Object,String> column1;
    @FXML
    private TableColumn<Object,String> column2;
    @FXML
    private TableColumn<Object,String> column3;

    @FXML
    private BarChart<String, Number> barchart;
    @FXML
    private LineChart<String, Number> linechart;
    @FXML
    private PieChart piechart;


    Mahasiswa_table mhs_table = new Mahasiswa_table();
    Matakuliah_table mtkl_table = new Matakuliah_table();
    Nilai_table nilai_table = new Nilai_table();


    public AppController() throws SQLException {
    }

    @FXML
    public void initialize() throws SQLException {
        ObservableList<String> options = FXCollections.observableArrayList(
                "Mahasiswa",
                "Matakuliah"
        );
        option.setItems(options);
        option.setValue("Mahasiswa");

        option.valueProperty().addListener((observable, oldValue, newValue) -> {
            table.getItems().clear();

            if ("Matakuliah".equals(newValue)) {
                linechart.setVisible(true);
                column1.setText("kode_mk");
                column1.setCellValueFactory(new PropertyValueFactory<>("kode_mk"));
                column2.setText("nama");
                column2.setCellValueFactory(new PropertyValueFactory<>("nama"));


                column3.setText("sks");
                column3.setCellValueFactory(new PropertyValueFactory<>("sks"));

                table.setItems(FXCollections.observableArrayList(mtkl_table.fetch_all_matkul()));
            } else {
                linechart.setVisible(false);
                column1.setText("NIM");
                column1.setCellValueFactory(new PropertyValueFactory<>("NIM"));
                column2.setText("nama");
                column2.setCellValueFactory(new PropertyValueFactory<>("nama"));

                column3.setText(" ");
                column3.setCellValueFactory(null);

                table.setItems(FXCollections.observableArrayList(mhs_table.fetch_all_mahasiswa()));
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {

                if (newSelection instanceof Mahasiswa) {
                    Mahasiswa m = (Mahasiswa) newSelection;
                    System.out.println("Clicked Mahasiswa: " + m.getNama() + " (" + m.getNIM() + ")");

                    // -- chart --
                    update_barchart("nim",m.getNIM());
                    update_piechart("nim",m.getNIM());


                } else if (newSelection instanceof Matakuliah) {
                    Matakuliah m = (Matakuliah) newSelection;
                    System.out.println("Clicked Mahasiswa: " + m.getNama() + " (" + m.getKode_mk() + ")");

                    // -- chart --
                    update_barchart("kode_mk",m.getKode_mk());
                    update_piechart("kode_mk",m.getKode_mk());
                    update_linechart(m.getKode_mk());
                }
            }
        });

        linechart.setVisible(false);
        column1.setText("NIM");
        column1.setCellValueFactory(new PropertyValueFactory<>("NIM"));
        column2.setText("nama");
        column2.setCellValueFactory(new PropertyValueFactory<>("nama"));
        column3.setText(" ");

        ObservableList<Object> data = FXCollections.observableArrayList(mhs_table.fetch_all_mahasiswa());
        table.setItems(data);

    }

    public void update_barchart(String target_col, String val) {
        // TODO: buat barchart menampilkan seberapa banyak nilai A,A-,B+,...
        // method ini dapat di gunakan di 2 situasi yaitu nilai berdasarkan nim mahasiswa dan berdasarkan kode matakuliah
        // ambil data dari attribute nilai_table
        // tips: target_col merujuk pada nama kolom di datbase sedangkan val adalah value yang di cari dari kolom tersebut misal:
        // target_col -> nim, val -> 71200001, maka kita mencari 71200001 di kolom nim
        barchart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Jumlah Nilai");

        java.util.Map<String, Integer> counts = new java.util.HashMap<>();

        try {
            List<Nilai> listNilai = nilai_table.fetch_all_nilai();
            for (Nilai n : listNilai) {
                String compareVal = target_col.equals("nim") ? n.getNIM() : n.getKode_mk();
                if (compareVal.equals(val)) {
                    counts.put(n.getNilai(), counts.getOrDefault(n.getNilai(), 0) + 1);
                }
            }

            String[] grades = {"A", "A-", "B+", "B", "B-", "C+", "C", "D", "E"};
            for (String g : grades) {
                series.getData().add(new XYChart.Data<>(g, counts.getOrDefault(g, 0)));
            }

            barchart.getData().add(series);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update_linechart(String kode_mk) {
        // TODO: buatlah linechart yang menggambarkan nilai mean dari setiap angkatan
        // angkatan dapat di ambil dengan cara getAngkatan() pada entity Mahasiswa
        // tips: fetch dulu entity mahasiswa menggunakan fetch_mahasiswa_by_nim() di mhs_tabel menggunakan nim pada nilai_table
        linechart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Mean per Angkatan");

        java.util.Map<String, List<Double>> angkatanGrades = new java.util.TreeMap<>();

        try {
            for (Nilai n : nilai_table.fetch_all_nilai()) {
                if (n.getKode_mk().equals(kode_mk)) {
                    Mahasiswa m = mhs_table.fetch_mahasiswa_by_nim(n.getNIM());
                    if (m != null) {
                        String angkatan = String.valueOf(m.getAngkatan());
                        double poin = convertGradeToPoint(n.getNilai());

                        angkatanGrades.computeIfAbsent(angkatan, k -> new ArrayList<>()).add(poin);
                    }
                }
            }

            angkatanGrades.forEach((angkatan, points) -> {
                double average = points.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                series.getData().add(new XYChart.Data<>(angkatan, average));
            });

            linechart.getData().add(series);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double convertGradeToPoint(String grade) {
        switch (grade) {
            case "A": return 4.0;
            case "A-": return 3.75;
            case "B+": return 3.25;
            case "B": return 3.0;
            case "B-": return 2.75;
            case "C+": return 2.25;
            case "C": return 2.0;
            case "D": return 1.0;
            default: return 0.0;
        }
    }

    public void update_piechart(String target_col, String val) {
        // TODO: tampilkan banyaknya nilai A,A-,B+,... dalam bentuk piechart
        // method ini dapat di gunakan di 2 situasi yaitu nilai berdasarkan nim mahasiswa dan berdasarkan kode matakuliah
        // ambil data dari attribute nilai_table
        // tips: target_col merujuk pada nama kolom di datbase sedangkan val adalah value yang di cari dari kolom tersebut misal:
        // target_col -> nim, val -> 71200001, maka kita mencari 71200001 di kolom nim
        piechart.getData();

        List<Nilai> listNilai = nilai_table.fetch_all_nilai();
        java.util.Map<String, Integer> countMap = new java.util.HashMap<>();

        for (Nilai n : listNilai) {
            if ((target_col.equals("nim") && n.getNIM().equals(val)) ||
                    (target_col.equals("kode_mk") && n.getKode_mk().equals(val))) {

                String nilai = n.getNilai();
                countMap.put(nilai, countMap.getOrDefault(nilai, 0) + 1);
            }
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        for (String key : countMap.keySet()) {
            pieData.add(new PieChart.Data(key, countMap.get(key)));
        }

        piechart.setData(pieData);
    }
}
