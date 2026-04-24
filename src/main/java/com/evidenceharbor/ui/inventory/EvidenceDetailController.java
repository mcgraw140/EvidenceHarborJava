package com.evidenceharbor.ui.inventory;

import com.evidenceharbor.domain.ChainOfCustody;
import com.evidenceharbor.domain.Evidence;
import com.evidenceharbor.domain.Officer;
import com.evidenceharbor.domain.Person;
import com.evidenceharbor.persistence.ChainOfCustodyRepository;
import com.evidenceharbor.persistence.OfficerRepository;
import com.evidenceharbor.persistence.PersonRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.evidenceharbor.domain.AgencySettings;
import com.evidenceharbor.persistence.SettingsRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import javax.imageio.ImageIO;
import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class EvidenceDetailController implements Initializable {

    @FXML private Label labelBarcode;
    @FXML private Label labelStatusBadge;
    @FXML private Canvas barcodeCanvas;

    @FXML private Label infoCase;
    @FXML private Label infoType;
    @FXML private Label infoDate;
    @FXML private Label infoStatus;
    @FXML private Label infoLocation;
    @FXML private Label infoDescription;

    @FXML private Label infoCollectedBy;
    @FXML private Label infoCollectedFrom;
    @FXML private Label infoSpecificLocation;
    @FXML private Label infoAddress;
    @FXML private Label infoCity;
    @FXML private Label infoStateZip;

    @FXML private VBox typeDetailsCard;
    @FXML private Label typeDetailsTitle;
    @FXML private GridPane typeDetailsGrid;

    @FXML private Label cocCountLabel;
    @FXML private TableView<ChainOfCustody> cocTable;
    @FXML private TableColumn<ChainOfCustody, String> cocColTimestamp;
    @FXML private TableColumn<ChainOfCustody, String> cocColAction;
    @FXML private TableColumn<ChainOfCustody, String> cocColPerformedBy;
    @FXML private TableColumn<ChainOfCustody, String> cocColFrom;
    @FXML private TableColumn<ChainOfCustody, String> cocColTo;
    @FXML private TableColumn<ChainOfCustody, String> cocColToPerson;
    @FXML private TableColumn<ChainOfCustody, String> cocColNotes;

    private Evidence evidence;
    private String caseNumber;
    private final ChainOfCustodyRepository cocRepo = new ChainOfCustodyRepository();
    private final OfficerRepository officerRepo = new OfficerRepository();
    private final PersonRepository personRepo = new PersonRepository();
    private final SettingsRepository settingsRepo = new SettingsRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cocColTimestamp.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTimestamp()));
        cocColAction.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getAction()));
        cocColPerformedBy.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getPerformedByName())));
        cocColFrom.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getFromLocation())));
        cocColTo.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getToLocation())));
        cocColToPerson.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getToPerson())));
        cocColNotes.setCellValueFactory(cd -> new SimpleStringProperty(ns(cd.getValue().getNotes())));
    }

    public void setEvidence(Evidence e, String caseNumber) {
        this.evidence = e;
        this.caseNumber = caseNumber;
        populateFields();
        loadCoc();
    }

    private void populateFields() {
        String barcode = ns(evidence.getBarcode());
        labelBarcode.setText(barcode);
        drawBarcode(barcode);

        String status = ns(evidence.getStatus());
        labelStatusBadge.setText(status);
        String color = statusColor(status);
        labelStatusBadge.setStyle("-fx-background-color: " + color + "33; -fx-text-fill: " + color +
                "; -fx-font-weight: bold; -fx-padding: 6 14 6 14; -fx-background-radius: 6; -fx-font-size: 14;");

        infoCase.setText(caseNumber);
        infoType.setText(ns(evidence.getEvidenceType()));
        infoDate.setText(ns(evidence.getCollectionDate()));
        infoStatus.setText(status);
        infoLocation.setText(ns(evidence.getStorageLocation()));
        infoDescription.setText(ns(evidence.getDescription()));

        infoCollectedBy.setText(resolveOfficerName(evidence.getCollectedByOfficerId()));
        infoCollectedFrom.setText(resolvePersonName(evidence.getCollectedFromPersonId()));
        infoSpecificLocation.setText(ns(evidence.getSpecificLocation()));
        infoAddress.setText(ns(evidence.getAddress()));
        infoCity.setText(ns(evidence.getCity()));
        String st = ns(evidence.getState()), zip = ns(evidence.getZip());
        infoStateZip.setText((st + " " + zip).trim());

        buildTypeDetails();
    }

    private String resolveOfficerName(int id) {
        if (id <= 0) return "";
        try {
            Officer o = officerRepo.findById(id);
            return o == null ? "" : ns(o.getName());
        } catch (Exception ex) { return ""; }
    }

    private String resolvePersonName(int id) {
        if (id <= 0) return "";
        try {
            Person p = personRepo.findById(id);
            return p == null ? "" : ns(p.getFullName());
        } catch (Exception ex) { return ""; }
    }

    private void buildTypeDetails() {
        typeDetailsGrid.getChildren().clear();
        typeDetailsGrid.getRowConstraints().clear();

        String type = ns(evidence.getEvidenceType());
        typeDetailsTitle.setText(type.isBlank() ? "Item Details" : type + " Details");

        List<String[]> pairs = new ArrayList<>();
        switch (type) {
            case "Ammunition" -> {
                addPair(pairs, "Caliber", evidence.getAmmoCallber());
                addPair(pairs, "Rounds", evidence.getAmmoQuantity());
                addPair(pairs, "Grain Weight", evidence.getAmmoGrainWeight());
                addPair(pairs, "Bullet Type", evidence.getAmmoBulletType());
                addPair(pairs, "Brand", evidence.getAmmoBrand());
            }
            case "Biological / DNA" -> {
                addPair(pairs, "Source", evidence.getBioSampleType());
                addPair(pairs, "Collection Method", evidence.getBioCollectionMethod());
                addPair(pairs, "Storage Temp", evidence.getBioStorageTemp());
                addPair(pairs, "Suspect Name", evidence.getBioSuspectName());
                addPair(pairs, "DNA Analysis Requested", evidence.isBioDnaAnalysisRequested() ? "Yes" : "No");
            }
            case "Currency" -> {
                addPair(pairs, "Amount", evidence.getCurrencyAmount());
                addPair(pairs, "Denominations", evidence.getCurrencyDenominations());
                addPair(pairs, "Serial Numbers", evidence.getCurrencySerialNumbers());
                addPair(pairs, "Suspected Counterfeit", evidence.isCurrencySuspectedCounterfeit() ? "Yes" : "No");
            }
            case "Electronics" -> {
                addPair(pairs, "Device Type", evidence.getElecDeviceType());
                addPair(pairs, "Make", evidence.getElecMake());
                addPair(pairs, "Model", evidence.getElecModel());
                addPair(pairs, "Serial #", evidence.getElecSerialNumber());
                addPair(pairs, "Username", evidence.getElecDeviceUsername());
                addPair(pairs, "Password", evidence.getElecDevicePassword());
                addPair(pairs, "Password Protected", evidence.isElecPasswordProtected() ? "Yes" : "No");
                addPair(pairs, "Data Extraction Requested", evidence.isElecDataExtractionRequested() ? "Yes" : "No");
            }
            case "Firearm" -> {
                addPair(pairs, "Make", evidence.getFirearmMake());
                addPair(pairs, "Model", evidence.getFirearmModel());
                addPair(pairs, "Serial #", evidence.getFirearmSerialNumber());
                addPair(pairs, "Type", evidence.getFirearmType());
                addPair(pairs, "Caliber", evidence.getFirearmCaliber());
                addPair(pairs, "Reported Stolen", evidence.isFirearmReportedStolen() ? "Yes" : "No");
                addPair(pairs, "Loaded When Recovered", evidence.isFirearmLoadedWhenRecovered() ? "Yes" : "No");
            }
            case "Jewelry" -> {
                addPair(pairs, "Material", evidence.getJewelryMaterial());
                addPair(pairs, "Type", evidence.getJewelryType());
                addPair(pairs, "Estimated Value", evidence.getJewelryEstimatedValue());
                addPair(pairs, "Engraving / ID", evidence.getJewelryEngravingOrId());
            }
            case "Narcotic Equipment" -> {
                addPair(pairs, "Type", evidence.getNarcEquipType());
                addPair(pairs, "Description", evidence.getNarcEquipDescription());
                addPair(pairs, "Suspected Residue", evidence.getNarcEquipSuspectedResidue());
                addPair(pairs, "Field Test Kit Used", evidence.isNarcEquipFieldTestKitUsed() ? "Yes" : "No");
            }
            case "Narcotics" -> {
                addPair(pairs, "Drug Type", evidence.getNarcDrugType());
                addPair(pairs, "Net Weight", evidence.getNarcNetWeight());
                addPair(pairs, "Form / Unit", evidence.getNarcForm());
                addPair(pairs, "Packaging", evidence.getNarcPackaging());
                addPair(pairs, "Field Test Performed", evidence.isNarcFieldTestPerformed() ? "Yes" : "No");
                addPair(pairs, "Field Test Result", evidence.getNarcFieldTestResult());
            }
            case "Vehicle" -> {
                addPair(pairs, "Make", evidence.getVehicleMake());
                addPair(pairs, "Model", evidence.getVehicleModel());
                addPair(pairs, "Year", evidence.getVehicleYear());
                addPair(pairs, "Color", evidence.getVehicleColor());
                addPair(pairs, "Body Type", evidence.getVehicleBodyType());
                addPair(pairs, "VIN", evidence.getVehicleVin());
                addPair(pairs, "License Plate", evidence.getVehicleLicensePlate());
                addPair(pairs, "License State", evidence.getVehicleLicenseState());
                addPair(pairs, "Reported Stolen", evidence.isVehicleReportedStolen() ? "Yes" : "No");
                addPair(pairs, "Impounded", evidence.isVehicleImpounded() ? "Yes" : "No");
            }
            case "Weapon" -> {
                addPair(pairs, "Type", evidence.getWeaponType());
                addPair(pairs, "Make", evidence.getWeaponMake());
                addPair(pairs, "Model", evidence.getWeaponModel());
                addPair(pairs, "Serial #", evidence.getWeaponSerialNumber());
                addPair(pairs, "Length", evidence.getWeaponLength());
                addPair(pairs, "Reported Stolen", evidence.isWeaponReportedStolen() ? "Yes" : "No");
            }
        }

        if (pairs.isEmpty()) {
            typeDetailsCard.setVisible(false);
            typeDetailsCard.setManaged(false);
            return;
        }
        typeDetailsCard.setVisible(true);
        typeDetailsCard.setManaged(true);

        // Lay out pairs across 2 columns (4 grid columns: key, value, key, value)
        int row = 0, col = 0;
        for (String[] kv : pairs) {
            Label key = new Label(kv[0] + ":");
            key.getStyleClass().add("label-key");
            Label val = new Label(kv[1]);
            val.getStyleClass().add("label-value");
            val.setWrapText(true);

            GridPane.setColumnIndex(key, col);
            GridPane.setRowIndex(key, row);
            GridPane.setColumnIndex(val, col + 1);
            GridPane.setRowIndex(val, row);
            typeDetailsGrid.getChildren().addAll(key, val);

            if (col == 0) { col = 2; } else { col = 0; row++; }
        }
    }

    private void addPair(List<String[]> pairs, String key, String value) {
        if (value == null || value.isBlank()) return;
        pairs.add(new String[] { key, value });
    }

    private void drawBarcode(String text) {
        if (barcodeCanvas == null) return;
        GraphicsContext g = barcodeCanvas.getGraphicsContext2D();
        double w = barcodeCanvas.getWidth();
        double h = barcodeCanvas.getHeight();
        g.clearRect(0, 0, w, h);
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, w, h);
        if (text == null || text.isBlank()) return;
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.CODE_128, (int) w, 1);
            int matrixWidth = matrix.getWidth();
            double barH = h - 20;
            double scaleX = w / matrixWidth;
            g.setFill(Color.BLACK);
            for (int col = 0; col < matrixWidth; col++) {
                if (matrix.get(col, 0)) {
                    g.fillRect(col * scaleX, 4, scaleX, barH);
                }
            }
            g.setFill(Color.BLACK);
            g.setFont(javafx.scene.text.Font.font("Consolas", 12));
            g.fillText(text, 6, h - 4);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadCoc() {
        try {
            List<ChainOfCustody> entries = cocRepo.findByEvidence(evidence.getId());
            cocTable.setItems(FXCollections.observableArrayList(entries));
            cocCountLabel.setText(entries.size() + " entries");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onAddTransfer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CocTransfer.fxml"));
            Parent root = loader.load();
            CocTransferController ctrl = loader.getController();
            ctrl.setEvidence(evidence);

            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(labelBarcode.getScene().getWindow());
            dialog.setTitle("Add Chain of Custody Entry");
            Scene scene = new Scene(root, 560, 500);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
            // Reload evidence status and CoC after transfer
            loadCoc();
            populateFields();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onPrintLabel() {
        // Gather all installed print services, Zebra printers sorted first
        PrintService[] allServices = PrintServiceLookup.lookupPrintServices(null, null);
        if (allServices == null || allServices.length == 0) {
            com.evidenceharbor.util.Dialogs.warn("No Printers", "No print services found on this computer.");
            return;
        }
        List<PrintService> sorted = new ArrayList<>();
        for (PrintService ps : allServices)
            if (ps.getName().toLowerCase().contains("zebra") || ps.getName().toLowerCase().contains("zlp")
                    || ps.getName().toLowerCase().contains("zpl"))
                sorted.add(0, ps);
            else
                sorted.add(ps);

        // Printer picker dialog
        Dialog<PrintService> dlg = new Dialog<>();
        dlg.setTitle("Print Barcode Label");
        dlg.setHeaderText("Select Zebra printer (2\" × 1\" label)");
        com.evidenceharbor.util.Dialogs.style(dlg);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.control.ComboBox<PrintService> combo = new javafx.scene.control.ComboBox<>();
        combo.getItems().addAll(sorted);
        combo.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(PrintService item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item.getName());
            }
        });
        combo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(PrintService item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item.getName());
            }
        });
        combo.setPrefWidth(380);
        if (!sorted.isEmpty()) combo.getSelectionModel().selectFirst();

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10, combo);
        content.setPadding(new javafx.geometry.Insets(12));
        dlg.getDialogPane().setContent(content);

        dlg.setResultConverter(bt -> bt == ButtonType.OK ? combo.getValue() : null);
        dlg.showAndWait().ifPresent(service -> {
            try {
                byte[] zpl = buildZpl();
                DocPrintJob job = service.createPrintJob();
                Doc doc = new SimpleDoc(zpl, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
                job.print(doc, new HashPrintRequestAttributeSet());
                com.evidenceharbor.util.Dialogs.info("Label Sent", "Label sent to: " + service.getName());
            } catch (Exception ex) {
                ex.printStackTrace();
                com.evidenceharbor.util.Dialogs.error(ex);
            }
        });
    }

    /**
     * Builds ZPL II for a 2" × 1" label (203 dpi → 406 × 203 dots).
     * Layout:
     *   - Code 128 barcode spanning the full width
     *   - Case #, Collection Date, Storage Location printed below
     */
    private byte[] buildZpl() {
        return com.evidenceharbor.util.LabelPrintUtil.build(
                evidence.getScanCode(),
                evidence.getBarcode(),
                caseNumber,
                evidence.getCollectionDate(),
                evidence.getStorageLocation());
    }

    @FXML
    private void onClose() {
        labelBarcode.getScene().getWindow().hide();
    }

    @FXML
    private void onPrint() {
        try {
            AgencySettings agency = settingsRepo.load();
            List<ChainOfCustody> cocEntries = cocRepo.findByEvidence(evidence.getId());
            String barcodeValue = ns(evidence.getBarcode());
            String barcodeImg64 = generateBarcodeBase64(barcodeValue, 500, 80);

            String html = buildPrintHtml(agency, barcodeValue, barcodeImg64, cocEntries);

            File tmp = File.createTempFile("evidence_" + barcodeValue.replace("/", "_") + "_", ".html",
                    new File(System.getProperty("java.io.tmpdir")));
            try (FileWriter fw = new FileWriter(tmp, StandardCharsets.UTF_8)) {
                fw.write(html);
            }
            java.awt.Desktop.getDesktop().browse(tmp.toURI());
        } catch (Exception ex) {
            ex.printStackTrace();
            com.evidenceharbor.util.Dialogs.error(ex);
        }
    }

    private String generateBarcodeBase64(String text, int width, int height) {
        if (text == null || text.isBlank()) return "";
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.CODE_128, width, height);
            // Render to BufferedImage manually (no ZXing swing dependency needed)
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    img.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception ex) {
            return "";
        }
    }

    private String buildPrintHtml(AgencySettings agency, String barcodeValue,
                                   String barcodeImg64, List<ChainOfCustody> coc) {
        String agencyName    = agency.getAgencyName().isBlank()    ? "Evidence Harbor" : agency.getAgencyName();
        String agencyAddr    = agency.getAgencyAddress().isBlank() ? "" : agency.getAgencyAddress();
        String agencyCityLine = Stream.of(agency.getAgencyCity(), agency.getAgencyState(), agency.getAgencyZip())
                .filter(s -> s != null && !s.isBlank()).reduce((a, b) -> a + ", " + b).orElse("");
        String printed = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<title>Evidence Sheet — ").append(esc(barcodeValue)).append("</title>");
        sb.append("<style>");
        sb.append("body{font-family:Arial,sans-serif;font-size:12px;color:#111;margin:0;padding:0}");
        sb.append(".page{width:8.5in;min-height:11in;margin:0 auto;padding:.5in .6in .5in .6in;box-sizing:border-box}");
        sb.append(".header{display:flex;align-items:center;justify-content:space-between;border-bottom:3px solid #1a3a6e;padding-bottom:10px;margin-bottom:14px}");
        sb.append(".agency-name{font-size:20px;font-weight:bold;color:#1a3a6e}");
        sb.append(".agency-sub{font-size:11px;color:#444;margin-top:2px}");
        sb.append(".doc-title{font-size:15px;font-weight:bold;color:#1a3a6e;text-align:right}");
        sb.append(".barcode-row{display:flex;align-items:center;gap:24px;margin-bottom:14px;padding:10px 14px;border:1px solid #ccc;border-radius:4px;background:#fafafa}");
        sb.append(".barcode-label{font-size:18px;font-weight:bold;font-family:Consolas,monospace;letter-spacing:2px}");
        sb.append(".section-title{font-size:12px;font-weight:bold;color:#1a3a6e;border-bottom:1px solid #aac;margin:14px 0 6px 0;padding-bottom:2px;text-transform:uppercase;letter-spacing:.5px}");
        sb.append(".grid{display:grid;grid-template-columns:160px 1fr 160px 1fr;gap:4px 10px;margin-bottom:4px}");
        sb.append(".key{font-weight:bold;color:#333;font-size:11px}");
        sb.append(".val{color:#111;font-size:11px}");
        sb.append("table{width:100%;border-collapse:collapse;font-size:10.5px;margin-top:4px}");
        sb.append("th{background:#1a3a6e;color:#fff;padding:4px 6px;text-align:left}");
        sb.append("td{padding:3px 6px;border-bottom:1px solid #ddd}");
        sb.append("tr:nth-child(even) td{background:#f5f7fa}");
        sb.append(".footer{margin-top:20px;border-top:1px solid #ccc;padding-top:8px;font-size:10px;color:#666;display:flex;justify-content:space-between}");
        sb.append(".sig-block{display:grid;grid-template-columns:1fr 1fr 1fr;gap:20px;margin-top:24px}");
        sb.append(".sig-line{border-top:1px solid #333;padding-top:4px;font-size:10px;color:#555;text-align:center;margin-top:32px}");
        sb.append("@media print{body{-webkit-print-color-adjust:exact;print-color-adjust:exact}}");
        sb.append("</style></head><body><div class='page'>");

        // ── Header ──────────────────────────────────────────────
        sb.append("<div class='header'>");
        sb.append("<div><div class='agency-name'>").append(esc(agencyName)).append("</div>");
        if (!agencyAddr.isBlank())
            sb.append("<div class='agency-sub'>").append(esc(agencyAddr)).append("</div>");
        if (!agencyCityLine.isBlank())
            sb.append("<div class='agency-sub'>").append(esc(agencyCityLine)).append("</div>");
        sb.append("</div>");
        sb.append("<div><div class='doc-title'>EVIDENCE PROPERTY SHEET</div>");
        sb.append("<div style='font-size:10px;color:#555;text-align:right'>Printed: ").append(printed).append("</div></div>");
        sb.append("</div>");

        // ── Barcode ──────────────────────────────────────────────
        sb.append("<div class='barcode-row'>");
        if (!barcodeImg64.isBlank())
            sb.append("<img src='data:image/png;base64,").append(barcodeImg64).append("' style='height:60px'/>");
        sb.append("<div><div class='barcode-label'>").append(esc(barcodeValue)).append("</div>");
        sb.append("<div style='font-size:10px;color:#555'>Evidence Tracking Number</div></div>");
        sb.append("</div>");

        // ── Overview ─────────────────────────────────────────────
        sb.append("<div class='section-title'>Overview</div><div class='grid'>");
        gridRow(sb, "Case #", caseNumber, "Evidence Type", ns(evidence.getEvidenceType()));
        gridRow(sb, "Collection Date", ns(evidence.getCollectionDate()), "Status", ns(evidence.getStatus()));
        gridRow(sb, "Storage Location", ns(evidence.getStorageLocation()), "Description", ns(evidence.getDescription()));
        sb.append("</div>");

        // ── Collection ────────────────────────────────────────────
        sb.append("<div class='section-title'>Collection Information</div><div class='grid'>");
        gridRow(sb, "Collected By", resolveOfficerName(evidence.getCollectedByOfficerId()),
                "Collected From", resolvePersonName(evidence.getCollectedFromPersonId()));
        gridRow(sb, "Specific Location", ns(evidence.getSpecificLocation()), "Address", ns(evidence.getAddress()));
        String cityLine = Stream.of(evidence.getCity(), evidence.getState(), evidence.getZip())
                .filter(s -> s != null && !s.isBlank()).reduce((a, b) -> a + ", " + b).orElse("");
        gridRow(sb, "City / State / ZIP", cityLine, "", "");
        sb.append("</div>");

        // ── Type-specific ─────────────────────────────────────────
        List<String[]> pairs = collectTypePairs();
        if (!pairs.isEmpty()) {
            sb.append("<div class='section-title'>").append(esc(ns(evidence.getEvidenceType()))).append(" Details</div>");
            sb.append("<div class='grid'>");
            for (int i = 0; i < pairs.size(); i += 2) {
                String k1 = pairs.get(i)[0], v1 = pairs.get(i)[1];
                String k2 = i + 1 < pairs.size() ? pairs.get(i + 1)[0] : "";
                String v2 = i + 1 < pairs.size() ? pairs.get(i + 1)[1] : "";
                gridRow(sb, k1, v1, k2, v2);
            }
            sb.append("</div>");
        }

        // ── Chain of Custody ──────────────────────────────────────
        sb.append("<div class='section-title'>Chain of Custody Log (").append(coc.size()).append(" entries)</div>");
        sb.append("<table><tr><th>Timestamp</th><th>Action</th><th>Performed By</th><th>From</th><th>To Location</th><th>To Person</th><th>Notes</th></tr>");
        for (ChainOfCustody c : coc) {
            sb.append("<tr>");
            td(sb, ns(c.getTimestamp()));
            td(sb, ns(c.getAction()));
            td(sb, ns(c.getPerformedByName()));
            td(sb, ns(c.getFromLocation()));
            td(sb, ns(c.getToLocation()));
            td(sb, ns(c.getToPerson()));
            td(sb, ns(c.getNotes()));
            sb.append("</tr>");
        }
        sb.append("</table>");

        // ── Signature Block ───────────────────────────────────────
        sb.append("<div class='sig-block'>");
        sb.append("<div><div class='sig-line'>Submitting Officer Signature / Badge</div></div>");
        sb.append("<div><div class='sig-line'>Evidence Technician Signature</div></div>");
        sb.append("<div><div class='sig-line'>Date / Time Received</div></div>");
        sb.append("</div>");

        // ── Footer ────────────────────────────────────────────────
        sb.append("<div class='footer'>");
        sb.append("<span>").append(esc(agencyName)).append(" — Evidence Management System</span>");
        sb.append("<span>Evidence Harbor | Confidential Law Enforcement Record</span>");
        sb.append("</div>");

        sb.append("</div></body></html>");
        return sb.toString();
    }

    private List<String[]> collectTypePairs() {
        List<String[]> pairs = new ArrayList<>();
        String type = ns(evidence.getEvidenceType());
        switch (type) {
            case "Ammunition" -> {
                addPair(pairs, "Caliber", evidence.getAmmoCallber());
                addPair(pairs, "Rounds", evidence.getAmmoQuantity());
                addPair(pairs, "Grain Weight", evidence.getAmmoGrainWeight());
                addPair(pairs, "Bullet Type", evidence.getAmmoBulletType());
                addPair(pairs, "Brand", evidence.getAmmoBrand());
            }
            case "Biological / DNA" -> {
                addPair(pairs, "Source", evidence.getBioSampleType());
                addPair(pairs, "Collection Method", evidence.getBioCollectionMethod());
                addPair(pairs, "Storage Temp", evidence.getBioStorageTemp());
                addPair(pairs, "Suspect Name", evidence.getBioSuspectName());
                addPair(pairs, "DNA Analysis Requested", evidence.isBioDnaAnalysisRequested() ? "Yes" : "No");
            }
            case "Currency" -> {
                addPair(pairs, "Amount", evidence.getCurrencyAmount());
                addPair(pairs, "Denominations", evidence.getCurrencyDenominations());
                addPair(pairs, "Serial Numbers", evidence.getCurrencySerialNumbers());
                addPair(pairs, "Suspected Counterfeit", evidence.isCurrencySuspectedCounterfeit() ? "Yes" : "No");
            }
            case "Electronics" -> {
                addPair(pairs, "Device Type", evidence.getElecDeviceType());
                addPair(pairs, "Make", evidence.getElecMake());
                addPair(pairs, "Model", evidence.getElecModel());
                addPair(pairs, "Serial #", evidence.getElecSerialNumber());
                addPair(pairs, "Username", evidence.getElecDeviceUsername());
                addPair(pairs, "Password", evidence.getElecDevicePassword());
                addPair(pairs, "Password Protected", evidence.isElecPasswordProtected() ? "Yes" : "No");
                addPair(pairs, "Data Extraction Requested", evidence.isElecDataExtractionRequested() ? "Yes" : "No");
            }
            case "Firearm" -> {
                addPair(pairs, "Make", evidence.getFirearmMake());
                addPair(pairs, "Model", evidence.getFirearmModel());
                addPair(pairs, "Serial #", evidence.getFirearmSerialNumber());
                addPair(pairs, "Type", evidence.getFirearmType());
                addPair(pairs, "Caliber", evidence.getFirearmCaliber());
                addPair(pairs, "Reported Stolen", evidence.isFirearmReportedStolen() ? "Yes" : "No");
                addPair(pairs, "Loaded When Recovered", evidence.isFirearmLoadedWhenRecovered() ? "Yes" : "No");
            }
            case "Jewelry" -> {
                addPair(pairs, "Material", evidence.getJewelryMaterial());
                addPair(pairs, "Type", evidence.getJewelryType());
                addPair(pairs, "Estimated Value", evidence.getJewelryEstimatedValue());
                addPair(pairs, "Engraving / ID", evidence.getJewelryEngravingOrId());
            }
            case "Narcotic Equipment" -> {
                addPair(pairs, "Type", evidence.getNarcEquipType());
                addPair(pairs, "Description", evidence.getNarcEquipDescription());
                addPair(pairs, "Suspected Residue", evidence.getNarcEquipSuspectedResidue());
                addPair(pairs, "Field Test Kit Used", evidence.isNarcEquipFieldTestKitUsed() ? "Yes" : "No");
            }
            case "Narcotics" -> {
                addPair(pairs, "Drug Type", evidence.getNarcDrugType());
                addPair(pairs, "Net Weight", evidence.getNarcNetWeight());
                addPair(pairs, "Form / Unit", evidence.getNarcForm());
                addPair(pairs, "Packaging", evidence.getNarcPackaging());
                addPair(pairs, "Field Test Performed", evidence.isNarcFieldTestPerformed() ? "Yes" : "No");
                addPair(pairs, "Field Test Result", evidence.getNarcFieldTestResult());
            }
            case "Vehicle" -> {
                addPair(pairs, "Make", evidence.getVehicleMake());
                addPair(pairs, "Model", evidence.getVehicleModel());
                addPair(pairs, "Year", evidence.getVehicleYear());
                addPair(pairs, "Color", evidence.getVehicleColor());
                addPair(pairs, "Body Type", evidence.getVehicleBodyType());
                addPair(pairs, "VIN", evidence.getVehicleVin());
                addPair(pairs, "License Plate", evidence.getVehicleLicensePlate());
                addPair(pairs, "License State", evidence.getVehicleLicenseState());
                addPair(pairs, "Reported Stolen", evidence.isVehicleReportedStolen() ? "Yes" : "No");
                addPair(pairs, "Impounded", evidence.isVehicleImpounded() ? "Yes" : "No");
            }
            case "Weapon" -> {
                addPair(pairs, "Type", evidence.getWeaponType());
                addPair(pairs, "Make", evidence.getWeaponMake());
                addPair(pairs, "Model", evidence.getWeaponModel());
                addPair(pairs, "Serial #", evidence.getWeaponSerialNumber());
                addPair(pairs, "Length", evidence.getWeaponLength());
                addPair(pairs, "Reported Stolen", evidence.isWeaponReportedStolen() ? "Yes" : "No");
            }
        }
        return pairs;
    }

    private void gridRow(StringBuilder sb, String k1, String v1, String k2, String v2) {
        sb.append("<div class='key'>").append(esc(k1)).append(k1.isBlank() ? "" : ":").append("</div>")
          .append("<div class='val'>").append(esc(v1)).append("</div>")
          .append("<div class='key'>").append(esc(k2)).append(k2.isBlank() ? "" : ":").append("</div>")
          .append("<div class='val'>").append(esc(v2)).append("</div>");
    }

    private void td(StringBuilder sb, String val) {
        sb.append("<td>").append(esc(val)).append("</td>");
    }

    private String esc(String s) {
        if (s == null || s.isBlank()) return "&nbsp;";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String statusColor(String status) {
        if (status == null) return "#888888";
        return switch (status) {
            case "In Dropbox"        -> "#f59e0b";
            case "In Custody"        -> "#22c55e";
            case "In Storage"        -> "#14b8a6";
            case "Checked In"        -> "#22c55e";
            case "Checked Out"       -> "#3b82f6";
            case "Deposited"         -> "#6366f1";
            case "Destroyed"         -> "#ef4444";
            case "Disbursed"         -> "#6b7280";
            case "Returned to Owner" -> "#14b8a6";
            case "Missing"           -> "#dc2626";
            case "Pending"           -> "#94a3b8";
            default                  -> "#64748b";
        };
    }

    private String ns(String s) { return s == null ? "" : s; }
}
