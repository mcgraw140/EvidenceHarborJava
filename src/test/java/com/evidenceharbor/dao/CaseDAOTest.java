package com.evidenceharbor.dao;

import com.evidenceharbor.model.Case;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CaseDAOTest {
    private CaseDAO caseDAO;

    @BeforeEach
    public void setUp() {
        caseDAO = new CaseDAO();
    }

    @Test
    public void testInsertAndGetCase() throws SQLException {
        Case testCase = new Case("TEST001", "Test Case", "Officer Smith", LocalDate.now(), "Station A", "Test notes");
        caseDAO.insertCase(testCase);

        Case retrieved = caseDAO.getCaseByNumber("TEST001");
        assertNotNull(retrieved);
        assertEquals("TEST001", retrieved.getCaseNumber());
        assertEquals("Test Case", retrieved.getDescription());
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // Clean up test data
        caseDAO.deleteCase("TEST001");
    }
}