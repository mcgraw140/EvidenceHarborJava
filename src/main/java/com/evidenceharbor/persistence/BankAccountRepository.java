package com.evidenceharbor.persistence;

import com.evidenceharbor.domain.BankAccount;
import com.evidenceharbor.domain.BankTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BankAccountRepository {

    private Connection conn() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    // ── Accounts ─────────────────────────────────────────────────────────────

    public List<BankAccount> findAllAccounts() {
        List<BankAccount> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM bank_accounts ORDER BY account_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapAccount(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    public BankAccount createAccount(String name, String number, String bank, String notes) {
        String sql = "INSERT INTO bank_accounts (account_name, account_number, bank_name, notes) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, number);
            ps.setString(3, bank);
            ps.setString(4, notes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return findAccountById(keys.getInt(1));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void updateAccount(int id, String name, String number, String bank, String notes) {
        String sql = "UPDATE bank_accounts SET account_name=?, account_number=?, bank_name=?, notes=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, number);
            ps.setString(3, bank);
            ps.setString(4, notes);
            ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteAccount(int id) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM bank_accounts WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public BankAccount findAccountById(int id) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM bank_accounts WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAccount(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    public List<BankTransaction> findTransactionsByAccount(int accountId) {
        List<BankTransaction> result = new ArrayList<>();
        String sql = "SELECT * FROM bank_account_transactions WHERE account_id=? ORDER BY date DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapTransaction(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    public void addTransaction(int accountId, String action, double amount, String slip, String date, String performedBy, String notes) {
        String sql = "INSERT INTO bank_account_transactions (account_id, action, amount, slip_number, date, performed_by, notes) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, action);
            ps.setDouble(3, amount);
            ps.setString(4, slip);
            ps.setString(5, date);
            ps.setString(6, performedBy);
            ps.setString(7, notes);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        // Update balance
        String op = "Deposit".equals(action) ? "+" : "-";
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE bank_accounts SET balance = balance " + op + " ? WHERE id=?")) {
            ps.setDouble(1, amount);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteTransaction(int transactionId, int accountId, double amount, String action) {
        // Reverse the balance change first
        String op = "Deposit".equals(action) ? "-" : "+";
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE bank_accounts SET balance = balance " + op + " ? WHERE id=?")) {
            ps.setDouble(1, amount);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM bank_account_transactions WHERE id=?")) {
            ps.setInt(1, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private BankAccount mapAccount(ResultSet rs) throws SQLException {
        BankAccount a = new BankAccount();
        a.setId(rs.getInt("id"));
        a.setAccountName(rs.getString("account_name"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setBankName(rs.getString("bank_name"));
        a.setBalance(rs.getDouble("balance"));
        a.setNotes(rs.getString("notes"));
        a.setCreatedAt(rs.getString("created_at"));
        return a;
    }

    private BankTransaction mapTransaction(ResultSet rs) throws SQLException {
        BankTransaction t = new BankTransaction();
        t.setId(rs.getInt("id"));
        t.setAccountId(rs.getInt("account_id"));
        t.setAction(rs.getString("action"));
        t.setAmount(rs.getDouble("amount"));
        t.setSlipNumber(rs.getString("slip_number"));
        t.setDate(rs.getString("date"));
        t.setPerformedBy(rs.getString("performed_by"));
        t.setNotes(rs.getString("notes"));
        return t;
    }
}
