package com.chipin.chipin;

import com.chipin.chipin.dto.CategoryResponseDTO;
import com.chipin.chipin.exception.BudgetNotFoundException;
import com.chipin.chipin.exception.CategoryNotFoundException;
import com.chipin.chipin.exception.DuplicateExternalExpenseException;
import com.chipin.chipin.model.BudgetCategory;
import com.chipin.chipin.model.BudgetExpense;
import com.chipin.chipin.model.BudgetStatus;
import com.chipin.chipin.model.ExpenseStatus;
import com.chipin.chipin.model.GroupBudget;
import com.chipin.chipin.repository.BudgetCategoryRepository;
import com.chipin.chipin.repository.BudgetExpenseRepository;
import com.chipin.chipin.repository.GroupBudgetRepository;
import com.chipin.chipin.service.BudgetExpenseService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Feature 11: BudgetExpenseService Spending & Audit Tests")
class BudgetExpenseServiceTest {

    @Mock
    private BudgetExpenseRepository expenseRepository;

    @Mock
    private BudgetCategoryRepository categoryRepository;

    @Mock
    private GroupBudgetRepository budgetRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BudgetExpenseService expenseService;

    private GroupBudget sampleBudget;
    private BudgetCategory sampleCategory;

    @BeforeEach
    void setUp() {
        sampleBudget = new GroupBudget(
                "group_01",
                "Mufliha",
                "November Household Budget",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                new BigDecimal("10000.00")
        );
        sampleBudget.setId(1L);
        sampleBudget.setStatus(BudgetStatus.ACTIVE);

        sampleCategory = new BudgetCategory(sampleBudget, "Groceries", new BigDecimal("4000.00"));
        sampleCategory.setId(10L);
    }

    @Test
    @DisplayName("Should record expense and persist with RECORDED status")
    void testRecordExpense_Success() {
        when(entityManager.find(eq(GroupBudget.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(sampleBudget);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(sampleCategory));
        when(expenseRepository.saveAndFlush(any(BudgetExpense.class))).thenAnswer(inv -> {
            BudgetExpense e = inv.getArgument(0);
            e.setId(500L);
            return e;
        });
        when(budgetRepository.existsById(1L)).thenReturn(true);
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(sampleBudget));
        when(expenseRepository.findByCategoryBudgetIdAndStatus(1L, ExpenseStatus.RECORDED))
                .thenReturn(Collections.emptyList());
        when(categoryRepository.findByBudgetId(1L)).thenReturn(List.of(sampleCategory));
        when(categoryRepository.existsById(10L)).thenReturn(true);

        BudgetExpense expense = expenseService.recordExpense(
                1L, 10L, "Afifa", "Afifa", new BigDecimal("1250.00"),
                "Weekly fresh produce", LocalDate.of(2026, 11, 15)
        );

        assertNotNull(expense);
        assertEquals(500L, expense.getId());
        assertEquals(ExpenseStatus.RECORDED, expense.getStatus());
        assertEquals("Afifa", expense.getPayerId());
        assertEquals(new BigDecimal("1250.00"), expense.getAmount());
        verify(expenseRepository).saveAndFlush(any(BudgetExpense.class));
    }

    @Test
    @DisplayName("Should detect overrun and transition budget to EXCEEDED when limit is surpassed")
    void testRecordExpense_OverrunTransitionsBudgetToExceeded() {
        when(entityManager.find(eq(GroupBudget.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(sampleBudget);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(sampleCategory));
        when(expenseRepository.saveAndFlush(any(BudgetExpense.class))).thenAnswer(inv -> inv.getArgument(0));

        // Total spending will be 12,000 against a 10,000 budget limit
        BudgetExpense bigExpense = new BudgetExpense(sampleCategory, "Musarrat", "Musarrat",
                new BigDecimal("12000.00"), "Big feast", LocalDate.of(2026, 11, 16), "Musarrat");
        when(budgetRepository.existsById(1L)).thenReturn(true);
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(sampleBudget));
        when(expenseRepository.findByCategoryBudgetIdAndStatus(1L, ExpenseStatus.RECORDED))
                .thenReturn(List.of(bigExpense));
        when(categoryRepository.findByBudgetId(1L)).thenReturn(List.of(sampleCategory));
        when(categoryRepository.existsById(10L)).thenReturn(true);

        expenseService.recordExpense(
                1L, 10L, "Musarrat", "Musarrat", new BigDecimal("12000.00"),
                "Big feast", LocalDate.of(2026, 11, 16)
        );

        assertEquals(BudgetStatus.EXCEEDED, sampleBudget.getStatus());
        verify(budgetRepository).save(sampleBudget);
    }

    @Test
    @DisplayName("Should void expense with reason and exclude from active spending")
    void testVoidExpense_Success() {
        BudgetExpense expense = new BudgetExpense(sampleCategory, "Afifa", "Afifa",
                new BigDecimal("500.00"), "Duplicate receipt", LocalDate.of(2026, 11, 15), "Afifa");
        expense.setId(501L);
        expense.setStatus(ExpenseStatus.RECORDED);

        when(expenseRepository.findById(501L)).thenReturn(Optional.of(expense));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(sampleCategory));
        when(entityManager.find(eq(GroupBudget.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(sampleBudget);
        when(expenseRepository.saveAndFlush(any(BudgetExpense.class))).thenAnswer(inv -> inv.getArgument(0));
        when(budgetRepository.existsById(1L)).thenReturn(true);
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(sampleBudget));
        when(expenseRepository.findByCategoryBudgetIdAndStatus(1L, ExpenseStatus.RECORDED))
                .thenReturn(Collections.emptyList());
        when(categoryRepository.findByBudgetId(1L)).thenReturn(List.of(sampleCategory));
        when(categoryRepository.existsById(10L)).thenReturn(true);

        BudgetExpense voided = expenseService.voidExpense(501L, "Accidental duplicate entry");

        assertEquals(ExpenseStatus.VOIDED, voided.getStatus());
        assertEquals("Accidental duplicate entry", voided.getVoidReason());
        assertNotNull(voided.getVoidedAt());
        assertFalse(voided.isActive());
    }

    @Test
    @DisplayName("Should reject voiding an already voided expense")
    void testVoidExpense_AlreadyVoided_Throws() {
        BudgetExpense expense = new BudgetExpense(sampleCategory, "Afifa", "Afifa",
                new BigDecimal("500.00"), "Item", LocalDate.of(2026, 11, 15), "Afifa");
        expense.setId(502L);
        expense.markVoided("First void");

        when(expenseRepository.findById(502L)).thenReturn(Optional.of(expense));
        when(entityManager.find(eq(GroupBudget.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(sampleBudget);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                expenseService.voidExpense(502L, "Second void attempt")
        );
        assertTrue(ex.getMessage().contains("already voided"));
    }

    @Test
    @DisplayName("Should reject duplicate external expense ID linking")
    void testRecordExpense_DuplicateExternalExpenseId() {
        when(entityManager.find(eq(GroupBudget.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(sampleBudget);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(sampleCategory));

        BudgetExpense existing = new BudgetExpense();
        existing.setId(99L);
        when(expenseRepository.findByExternalExpenseId(888L)).thenReturn(Optional.of(existing));

        assertThrows(DuplicateExternalExpenseException.class, () ->
                expenseService.recordExpense(
                        1L, 10L, "Afifa", "Afifa", new BigDecimal("500.00"),
                        "Linked expense", LocalDate.of(2026, 11, 15), 888L
                )
        );
    }

    @Test
    @DisplayName("Should correctly calculate remaining budget balance when within limit")
    void testCalculateRemainingBudgetAmount_Normal() {
        BudgetExpense exp = new BudgetExpense(sampleCategory, "Afifa", "Afifa",
                new BigDecimal("2500.00"), "Groceries", LocalDate.of(2026, 11, 10), "Afifa");
        exp.setStatus(ExpenseStatus.RECORDED);

        when(budgetRepository.findById(1L)).thenReturn(Optional.of(sampleBudget));
        when(budgetRepository.existsById(1L)).thenReturn(true);
        when(expenseRepository.findByCategoryBudgetIdAndStatus(1L, ExpenseStatus.RECORDED))
                .thenReturn(List.of(exp));

        BigDecimal remaining = expenseService.calculateRemainingBudgetAmount(1L);

        assertEquals(new BigDecimal("7500.00"), remaining);
    }

    @Test
    @DisplayName("Should correctly return negative remaining budget balance when budget is overrun")
    void testCalculateRemainingBudgetAmount_Overrun() {
        BudgetExpense exp = new BudgetExpense(sampleCategory, "Afifa", "Afifa",
                new BigDecimal("12500.00"), "Overrun purchase", LocalDate.of(2026, 11, 10), "Afifa");
        exp.setStatus(ExpenseStatus.RECORDED);

        when(budgetRepository.findById(1L)).thenReturn(Optional.of(sampleBudget));
        when(budgetRepository.existsById(1L)).thenReturn(true);
        when(expenseRepository.findByCategoryBudgetIdAndStatus(1L, ExpenseStatus.RECORDED))
                .thenReturn(List.of(exp));

        BigDecimal remaining = expenseService.calculateRemainingBudgetAmount(1L);

        assertEquals(new BigDecimal("-2500.00"), remaining);
    }

    @Test
    @DisplayName("Should aggregate category spending views with pre-calculated progress and warning metrics")
    void testGetCategorySpendingViews_AggregationAndMetrics() {
        BudgetCategory catWarning = new BudgetCategory(sampleBudget, "Utilities", new BigDecimal("1000.00"));
        catWarning.setId(20L);

        BudgetCategory catOverrun = new BudgetCategory(sampleBudget, "Entertainment", new BigDecimal("500.00"));
        catOverrun.setId(30L);

        when(budgetRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.findByBudgetId(1L)).thenReturn(List.of(sampleCategory, catWarning, catOverrun));

        // sampleCategory: 4000 allocated, 1000 spent -> 25%, fill-normal, not warning, not overrun
        BudgetExpense e1 = new BudgetExpense(sampleCategory, "Afifa", "Afifa", new BigDecimal("1000.00"), "Food", LocalDate.now(), "Afifa");
        when(categoryRepository.existsById(10L)).thenReturn(true);
        when(expenseRepository.findByCategoryIdAndStatus(10L, ExpenseStatus.RECORDED)).thenReturn(List.of(e1));

        // catWarning: 1000 allocated, 850 spent -> 85%, fill-warning, isWarning=true, not overrun
        BudgetExpense e2 = new BudgetExpense(catWarning, "Mufliha", "Mufliha", new BigDecimal("850.00"), "Bills", LocalDate.now(), "Mufliha");
        when(categoryRepository.existsById(20L)).thenReturn(true);
        when(expenseRepository.findByCategoryIdAndStatus(20L, ExpenseStatus.RECORDED)).thenReturn(List.of(e2));

        // catOverrun: 500 allocated, 600 spent -> 100% clamped, fill-danger, isOverrun=true
        BudgetExpense e3 = new BudgetExpense(catOverrun, "Musarrat", "Musarrat", new BigDecimal("600.00"), "Movies", LocalDate.now(), "Musarrat");
        when(categoryRepository.existsById(30L)).thenReturn(true);
        when(expenseRepository.findByCategoryIdAndStatus(30L, ExpenseStatus.RECORDED)).thenReturn(List.of(e3));

        List<CategoryResponseDTO> views = expenseService.getCategorySpendingViews(1L);

        assertEquals(3, views.size());

        // Category 1: normal
        CategoryResponseDTO v1 = views.get(0);
        assertEquals(10L, v1.getId());
        assertEquals(new BigDecimal("1000.00"), v1.getSpentAmount());
        assertEquals(new BigDecimal("3000.00"), v1.getRemainingAllocation());
        assertEquals(25, v1.getSpendingPercentage());
        assertFalse(v1.isWarning());
        assertFalse(v1.isOverrun());
        assertEquals("fill-normal", v1.getProgressFillClass());

        // Category 2: warning threshold (> 80%)
        CategoryResponseDTO v2 = views.get(1);
        assertEquals(20L, v2.getId());
        assertEquals(new BigDecimal("850.00"), v2.getSpentAmount());
        assertEquals(new BigDecimal("150.00"), v2.getRemainingAllocation());
        assertEquals(85, v2.getSpendingPercentage());
        assertTrue(v2.isWarning());
        assertFalse(v2.isOverrun());
        assertEquals("fill-warning", v2.getProgressFillClass());

        // Category 3: overrun
        CategoryResponseDTO v3 = views.get(2);
        assertEquals(30L, v3.getId());
        assertEquals(new BigDecimal("600.00"), v3.getSpentAmount());
        assertEquals(new BigDecimal("-100.00"), v3.getRemainingAllocation());
        assertEquals(new BigDecimal("100.00"), v3.getOverrunAmount());
        assertEquals(100, v3.getSpendingPercentage());
        assertTrue(v3.isOverrun());
        assertEquals("fill-danger", v3.getProgressFillClass());
    }
}
