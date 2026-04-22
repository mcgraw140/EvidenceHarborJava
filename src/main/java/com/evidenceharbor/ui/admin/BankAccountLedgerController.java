package com.evidenceharbor.ui.admin;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
import com.evidenceharbor.app.SessionManager;
import com.evidenceharbor.domain.BankAccount;
import com.evidenceharbor.domain.BankTransaction;
import com.evidenceharbor.persistence.BankAccountRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;


import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class BankAccountLedgerController implements Initializable {

    @FXML private Button navAdminTab;
    @FXML private Button navAuditTrailBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navInventoryBtn;
    @FXML private Button navReportsBtn;

    @FXML private ListView<BankAccount> accountList;
    @FXML private Label lblAccountName;
    @FXML private Label lblBalance;
    @FXML private Label lblBank;
    @FXML private Label lblAccountNumber;
    @FXML private Button btnEditAccount;
    @FXML private TableView<BankTransaction> txTable;
    @FXML private TableColumn<BankTransaction, String> colDate;
    @FXML private TableColumn<BankTransaction, String> colAction;
    @FXML private TableColumn<BankTransaction, String> colAmount;
    @FXML private TableColumn<BankTransaction, String> colSlip;
    @FXML private TableColumn<BankTransaction, String> colSource;
    @FXML private TableColumn<BankTransaction, String> colBy;
    @FXML private TableColumn<BankTransaction, String> colNotes;
    @FXML private TableColumn<BankTransaction, String> colTxAction;

    private final BankAccountRepository repo = new BankAccountRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Account list display
        accountList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BankAccount item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getAccountName() + "\n$" + String.format("%.2f", item.getBalance()));
                }
            }
        });
        accountList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> showAccount(sel));

        // Transaction table columns
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDate()));
        colAction.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isVoided() ? c.getValue().getAction() + " (VOIDED)" : c.getValue().getAction()));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%.2f", c.getValue().getAmount())));
        colSlip.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlipNumber()));
        colSource.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSourceRef() == null ? "" : c.getValue().getSourceRef()));
        colBy.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPerformedBy()));
        colNotes.setCellValueFactory(c -> {
            BankTransaction tx = c.getValue();
            String n = tx.getNotes() == null ? "" : tx.getNotes();
            if (tx.isVoided() && tx.getVoidedReason() != null && !tx.getVoidedReason().isBlank()) {
                String by = tx.getVoidedBy() == null || tx.getVoidedBy().isBlank() ? "" : " by " + tx.getVoidedBy();
                String voidNote = "[VOIDED" + by + "] " + tx.getVoidedReason();
                n = n.isBlank() ? voidNote : (n + "  —  " + voidNote);
            }
            return new SimpleStringProperty(n);
        });

        // Dim voided rows with strikethrough
        txTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(BankTransaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isVoided()) {
                    setStyle("-fx-text-fill:#94a3b8; -fx-opacity:0.75;");
                } else {
                    setStyle("");
                }
            }
        });

        colTxAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnVoid = new Button("Void");
            { btnVoid.setStyle("-fx-background-color:#e53e3e;-fx-text-fill:white;"); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                BankTransaction tx = getTableView().getItems().get(getIndex());
                if (tx.isVoided()) {
                    setGraphic(null);
                    return;
                }
                btnVoid.setOnAction(e -> voidTransaction(tx));
                setGraphic(btnVoid);
            }
        });

        loadAccounts();
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, null);
    }

    private void loadAccounts() {
        try {
            List<BankAccount> accounts = repo.findAllAccounts();
            accountList.setItems(FXCollections.observableArrayList(accounts));
            accountList.setPlaceholder(new Label(
                    "No bank accounts yet.\nClick \"+ New\" above to create one."));
            if (!accounts.isEmpty()) {
                accountList.getSelectionModel().selectFirst();
            } else {
                clearDetail();
            }
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void showAccount(BankAccount account) {
        if (account == null) { clearDetail(); return; }
        lblAccountName.setText(account.getAccountName());
        lblBalance.setText(String.format("$%.2f", account.getBalance()));
        lblBank.setText(account.getBankName() != null ? account.getBankName() : "");
        lblAccountNumber.setText(account.getAccountNumber() != null ? account.getAccountNumber() : "");
        btnEditAccount.setDisable(false);
        loadTransactions(account);
    }

    private void clearDetail() {
        lblAccountName.setText("No account selected");
        lblBalance.setText("$0.00");
        lblBank.setText("");
        lblAccountNumber.setText("");
        btnEditAccount.setDisable(true);
        txTable.getItems().clear();
    }

    private void loadTransactions(BankAccount account) {
        try {
            txTable.setItems(FXCollections.observableArrayList(
                    repo.findTransactionsByAccount(account.getId())));
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    // â”€â”€ Account CRUD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    private void onNewAccount() {
        showAccountDialog(null);
    }

    @FXML
    private void onEditAccount() {
        BankAccount sel = accountList.getSelectionModel().getSelectedItem();
        if (sel != null) showAccountDialog(sel);
    }

    private void showAccountDialog(BankAccount existing) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "New Account" : "Edit Account");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField tfName   = new TextField(existing != null ? existing.getAccountName() : "");
        TextField tfNumber = new TextField(existing != null ? nvl(existing.getAccountNumber()) : "");
        TextField tfBank   = new TextField(existing != null ? nvl(existing.getBankName()) : "");
        TextArea  taNotes  = new TextArea(existing != null ? nvl(existing.getNotes()) : "");
        taNotes.setPrefRowCount(3);

        grid.add(new Label("Account Name:*"), 0, 0); grid.add(tfName,   1, 0);
        grid.add(new Label("Account #:"),     0, 1); grid.add(tfNumber, 1, 1);
        grid.add(new Label("Bank:"),          0, 2); grid.add(tfBank,   1, 2);
        grid.add(new Label("Notes:"),         0, 3); grid.add(taNotes,  1, 3);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String name = tfName.getText().trim();
                if (name.isEmpty()) { showError("Account name is required."); return; }
                try {
                    if (existing == null) {
                        // Two-step verification: confirm creation with a summary
                        String summary = "You are about to create a new bank account:\n\n"
                                + "  • Name:    " + name + "\n"
                                + "  • Bank:    " + (tfBank.getText().trim().isEmpty() ? "(none)" : tfBank.getText().trim()) + "\n"
                                + "  • Acct #:  " + (tfNumber.getText().trim().isEmpty() ? "(none)" : tfNumber.getText().trim()) + "\n\n"
                                + "Bank accounts cannot be deleted. "
                                + "Are you sure you want to create this account?";
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, summary,
                                ButtonType.YES, ButtonType.NO);
                        confirm.setTitle("Confirm New Account");
                        confirm.setHeaderText("Verify new account details");
                        java.util.Optional<ButtonType> result = confirm.showAndWait();
                        if (result.isEmpty() || result.get() != ButtonType.YES) return;
                        repo.createAccount(name, tfNumber.getText().trim(),
                                tfBank.getText().trim(), taNotes.getText().trim());
                    } else {
                        repo.updateAccount(existing.getId(), name, tfNumber.getText().trim(),
                                tfBank.getText().trim(), taNotes.getText().trim());
                    }
                    loadAccounts();
                } catch (RuntimeException e) {
                    showError(e.getMessage());
                }
            }
        });
    }

    // â”€â”€ Transaction recording â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    private void onAddTransaction() {
        BankAccount sel = accountList.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("Please select an account first."); return; }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Record Transaction");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> cbAction = new ComboBox<>(
                FXCollections.observableArrayList("Deposit", "Withdrawal"));
        cbAction.setValue("Deposit");

        TextField tfAmount  = new TextField();
        TextField tfSlip    = new TextField();
        TextField tfSource  = new TextField();
        tfSource.setPromptText("Case # or source of funds (required for deposits)");
        TextField tfBy      = new TextField();
        // Default to current officer
        if (SessionManager.getCurrentOfficer() != null) {
            tfBy.setText(SessionManager.getCurrentOfficer().getName());
        }
        TextArea  taNotes   = new TextArea();
        taNotes.setPrefRowCount(2);
        DatePicker dpDate = new DatePicker(LocalDate.now());

        Label lblSource = new Label("Case / Source:*");
        // When type changes between Deposit/Withdrawal, the source asterisk hints required
        cbAction.valueProperty().addListener((obs, oldV, newV) -> {
            if ("Withdrawal".equals(newV)) {
                lblSource.setText("Case / Source:");
                tfSource.setPromptText("Optional for withdrawals");
            } else {
                lblSource.setText("Case / Source:*");
                tfSource.setPromptText("Case # or source of funds (required for deposits)");
            }
        });

        grid.add(new Label("Type:*"),    0, 0); grid.add(cbAction, 1, 0);
        grid.add(new Label("Amount:*"),  0, 1); grid.add(tfAmount, 1, 1);
        grid.add(new Label("Date:"),     0, 2); grid.add(dpDate,   1, 2);
        grid.add(lblSource,              0, 3); grid.add(tfSource, 1, 3);
        grid.add(new Label("Slip #:"),   0, 4); grid.add(tfSlip,   1, 4);
        grid.add(new Label("By:"),       0, 5); grid.add(tfBy,     1, 5);
        grid.add(new Label("Notes:"),    0, 6); grid.add(taNotes,  1, 6);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String amtStr = tfAmount.getText().trim();
                double amount;
                try {
                    amount = Double.parseDouble(amtStr);
                    if (amount <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    showError("Amount must be a positive number.");
                    return;
                }
                String dateStr = dpDate.getValue() != null
                        ? dpDate.getValue().toString()
                        : LocalDate.now().toString();
                String source = tfSource.getText().trim();
                if ("Deposit".equals(cbAction.getValue()) && source.isEmpty()) {
                    showError("Deposits require a Case # or source of funds.");
                    return;
                }
                try {
                    repo.addTransaction(sel.getId(), cbAction.getValue(), amount,
                            tfSlip.getText().trim(), dateStr,
                            tfBy.getText().trim(), taNotes.getText().trim(),
                            source.isEmpty() ? null : source);
                } catch (RuntimeException ex) {
                    showError(ex.getMessage());
                    return;
                }

                // Reload both lists
                loadAccounts();
                // Re-select the same account
                accountList.getItems().stream()
                        .filter(a -> a.getId() == sel.getId())
                        .findFirst()
                        .ifPresent(a -> accountList.getSelectionModel().select(a));
            }
        });
    }

    private void voidTransaction(BankTransaction tx) {
        if (tx.isVoided()) return;
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Void Transaction");
        dlg.setHeaderText("Voiding a transaction preserves the record for audit.\n"
                + "A reason is required and cannot be changed later.");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        TextArea taReason = new TextArea();
        taReason.setPrefRowCount(3);
        taReason.setPromptText("Explain the error (e.g. wrong amount, duplicate entry, wrong account)");
        grid.add(new Label("Reason:*"), 0, 0);
        grid.add(taReason, 1, 0);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String reason = taReason.getText().trim();
                if (reason.isEmpty()) {
                    showError("A reason is required to void a transaction.");
                    return;
                }
                String by = SessionManager.getCurrentOfficer() != null
                        ? SessionManager.getCurrentOfficer().getName() : "";
                try {
                    repo.voidTransaction(tx.getId(), tx.getAccountId(), tx.getAmount(), tx.getAction(), reason, by);
                } catch (RuntimeException e) {
                    showError(e.getMessage());
                    return;
                }
                BankAccount sel = accountList.getSelectionModel().getSelectedItem();
                loadAccounts();
                if (sel != null) {
                    accountList.getItems().stream()
                            .filter(a -> a.getId() == sel.getId())
                            .findFirst()
                            .ifPresent(a -> accountList.getSelectionModel().select(a));
                }
            }
        });
    }

    // â”€â”€ Nav bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML private void onCases()     { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople()    { Navigator.get().showPeople(); }
    @FXML private void onDropbox()   { Navigator.get().showDropbox(); }
    @FXML private void onReports()   { Navigator.get().showReports(); }
    @FXML private void onSettings()  { Navigator.get().showSettings(); }
    @FXML private void onAdmin()          { Navigator.get().showAdminDashboard(); }
    @FXML private void onBack()           { Navigator.get().showAdminDashboard(); }
    @FXML private void onDashboard()      { Navigator.get().showAdminDashboard(); }
    @FXML private void onAuditTrail()     { Navigator.get().showAuditTrail(); }
    @FXML private void onUserManagement()       { Navigator.get().showUserManagement(); }
    @FXML private void onLookupAdministration() { Navigator.get().showLookupAdmin(); }
    @FXML private void onEvidenceAudit()         { Navigator.get().showEvidenceAudit(); }
    @FXML private void onBankAccountLedger()     { }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Bank Ledger Error");
        a.showAndWait();
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
