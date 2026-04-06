package com.evidenceharbor.ui;

import com.evidenceharbor.dao.CaseDAO;
import com.evidenceharbor.model.Case;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MainFrame extends JFrame {
    private JTabbedPane tabbedPane;
    private CaseDAO caseDAO;

    public MainFrame() {
        caseDAO = new CaseDAO();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Evidence Management System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        add(tabbedPane);

        // Add Case Tab
        JPanel casePanel = createCasePanel();
        tabbedPane.addTab("Add Case", casePanel);

        // Placeholder for other tabs
        tabbedPane.addTab("Add Evidence", new JPanel());
        tabbedPane.addTab("Search Evidence", new JPanel());
        tabbedPane.addTab("Chain of Custody", new JPanel());
        tabbedPane.addTab("Check In/Out", new JPanel());
    }

    private JPanel createCasePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Case Number
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Case Number:"), gbc);
        gbc.gridx = 1;
        JTextField caseNumberField = new JTextField(20);
        panel.add(caseNumberField, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        JTextField descriptionField = new JTextField(20);
        panel.add(descriptionField, gbc);

        // Officer
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Officer:"), gbc);
        gbc.gridx = 1;
        JTextField officerField = new JTextField(20);
        panel.add(officerField, gbc);

        // Date
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        JTextField dateField = new JTextField(20);
        dateField.setText(LocalDate.now().toString());
        panel.add(dateField, gbc);

        // Location
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        JTextField locationField = new JTextField(20);
        panel.add(locationField, gbc);

        // Notes
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Notes:"), gbc);
        gbc.gridx = 1;
        JTextArea notesArea = new JTextArea(3, 20);
        JScrollPane scrollPane = new JScrollPane(notesArea);
        panel.add(scrollPane, gbc);

        // Save Button
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton saveButton = new JButton("Save Case");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String caseNumber = caseNumberField.getText().trim();
                    if (caseNumber.isEmpty()) {
                        JOptionPane.showMessageDialog(MainFrame.this, "Case Number is required.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String description = descriptionField.getText().trim();
                    String officer = officerField.getText().trim();
                    LocalDate date = LocalDate.parse(dateField.getText().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                    String location = locationField.getText().trim();
                    String notes = notesArea.getText().trim();

                    Case caseItem = new Case(caseNumber, description, officer, date, location, notes);
                    caseDAO.insertCase(caseItem);

                    JOptionPane.showMessageDialog(MainFrame.this, "Case added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                    // Clear fields
                    caseNumberField.setText("");
                    descriptionField.setText("");
                    officerField.setText("");
                    dateField.setText(LocalDate.now().toString());
                    locationField.setText("");
                    notesArea.setText("");

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Error saving case: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        panel.add(saveButton, gbc);

        return panel;
    }
}