package hr.algebra.workforce.dto;

public record LeaveBalance(int year,
                           int annualEntitledDays,
                           int annualUsedDays,
                           int annualReservedDays,
                           int annualRemainingDays,
                           int paidEntitledDays,
                           int paidUsedDays,
                           int paidReservedDays,
                           int paidRemainingDays) {
}
