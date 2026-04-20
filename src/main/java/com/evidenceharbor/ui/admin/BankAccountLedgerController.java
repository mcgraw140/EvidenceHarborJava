package com.evidenceharbor.ui.admin;

import com.evidenceharbor.app.NavHelper;
import com.evidenceharbor.app.Navigator;
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
    @FXML private Button btnDeleteAccount;
    @FXML private TableView<BankTransaction> txTable;
    @FXML private TableColumn<BankTransaction, String> colDate;
    @FXML private TableColumn<BankTransaction, String> colAction;
    @FXML private TableColumn<BankTransaction, String> colAmount;
    @FXML private TableColumn<BankTransaction, String> colSlip;
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
        colAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("$%.2f", c.getValue().getAmount())));
        colSlip.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSlipNumber()));
        colBy.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPerformedBy()));
        colNotes.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNotes()));

        colTxAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnDel = new Button("Delete");
            { btnDel.setStyle("-fx-background-color:#e53e3e;-fx-text-fill:white;"); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                BankTransaction tx = getTableView().getItems().get(getIndex());
                btnDel.setOnAction(e -> deleteTransaction(tx));
                setGraphic(btnDel);
            }
        });

        loadAccounts();
        NavHelper.applyNavVisibility(navAdminTab, navAuditTrailBtn, navSettingsBtn, navInventoryBtn, navReportsBtn, null);
    }

    private void loadAccounts() {
        List<BankAccount> accounts = repo.findAllAccounts();
        accountList.setItems(FXCollections.observableArrayList(accounts));
        if (!accounts.isEmpty()) {
            accountList.getSelectionModel().selectFirst();
        } else {
            clearDetail();
        }
    }

    private void showAccount(BankAccount account) {
        if (account == null) { clearDetail(); return; }
        lblAccountName.setText(account.getAccountName());
        lblBalance.setText(String.format("$%.2f", account.getBalance()));
        lblBank.setText(account.getBankName() != null ? account.getBankName() : "");
        lblAccountNumber.setText(account.getAccountNumber() != null ? account.getAccountNumber() : "");
        btnEditAccount.setDisable(false);
        btnDeleteAccount.setDisable(false);
        loadTransactions(account);
    }

    private void clearDetail() {
        lblAccountName.setText("No account selected");
        lblBalance.setText("$0.00");
        lblBank.setText("");
        lblAccountNumber.setText("");
        btnEditAccount.setDisable(true);
        btnDeleteAccount.setDisable(true);
        txTable.getItems().clear();
    }

    private void loadTransactions(BankAccount account) {
        txTable.setItems(FXCollections.observableArrayList(
                repo.findTransactionsByAccount(account.getId())));
    }

    // ── Account CRUD ─────────────────────────────────────────────────────────

    @FXML
    private void onNewAccount() {
        showAccountDialog(null);
    }

    @FXML
    private void onEditAccount() {
        BankAccount sel = accountList.getSelectionModel().getSelectedItem();
        if (sel != null) showAccountDialog(sel);
    }

    @FXML
    private void onDeleteAccount() {
        BankAccount sel = accountList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete account \"" + sel.getAccountName() + "\" and all its transactions?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                repo.deleteAccount(sel.getId());
                loadAccounts();
            }
        });
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
                if (existing == null) {
                    repo.createAccount(name, tfNumber.getText().trim(),
                            tfBank.getText().trim(), taNotes.getText().trim());
                } else {
                    repo.updateAccount(existing.getId(), name, tfNumber.getText().trim(),
                            tfBank.getText().trim(), taNotes.getText().trim());
                }
                loadAccounts();
            }
        });
    }

    // ── Transaction recording ─────────────────────────────────────────────────

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
        TextField tfBy      = new TextField();
        TextArea  taNotes   = new TextArea();
        taNotes.setPrefRowCount(2);
        DatePicker dpDate = new DatePicker(LocalDate.now());

        grid.add(new Label("Type:*"),    0, 0); grid.add(cbAction, 1, 0);
        grid.add(new Label("Amount:*"),  0, 1); grid.add(tfAmount, 1, 1);
        grid.add(new Label("Date:"),     0, 2); grid.add(dpDate,   1, 2);
        grid.add(new Label("Slip #:"),   0, 3); grid.add(tfSlip,   1, 3);
        grid.add(new Label("By:"),       0, 4); grid.add(tfBy,     1, 4);
        grid.add(new Label("Notes:"),    0, 5); grid.add(taNotes,  1, 5);

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
                repo.addTransaction(sel.getId(), cbAction.getValue(), amount,
                        tfSlip.getText().trim(), dateStr,
                        tfBy.getText().trim(), taNotes.getText().trim());

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

    private void deleteTransaction(BankTransaction tx) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this transaction?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                repo.deleteTransaction(tx.getId(), tx.getAccountId(), tx.getAmount(), tx.getAction());
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

    // ── Nav bar ───────────────────────────────────────────────────────────────

    @FXML private void onCases()     { Navigator.get().showCaseList(); }
    @FXML private void onInventory() { Navigator.get().showInventory(); }
    @FXML private void onPeople()    { Navigator.get().showPeople(); }
    @FXML private void onDropbox()   { Navigator.get().showDropbox(); }
    @FXML private void onReports()   { Navigator.get().showReports(); }
    @FXML private void onSettings()  { Navigator.get().showSettings(); }
    @FXML private void onAdmin()          { Navigator.get().showAdminDashboard(); }
    @FXML private void onAuditTrail()     { Navigator.get().showAuditTrail(); }
    @FXML private void onQuartermaster()  { Navigator.get().showQmDashboard(); }
    @FXML private void onImpound()       { Navigator.get().showImpoundLot(); }
    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
