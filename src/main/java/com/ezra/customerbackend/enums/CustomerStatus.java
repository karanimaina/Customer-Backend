package com.ezra.customerbackend.enums;

/**
 * Account lifecycle aligned with the customer-service flowchart.
 */
public enum CustomerStatus {
    /** Registered; KYC not complete. */
    PENDING,
    /** KYC documents under manual/automated review. */
    KYC_UNDER_REVIEW,
    /** KYC rejected; reason stored on KYC record. */
    KYC_REJECTED,
    /** KYC verified; credit profile active — eligible for loan disbursement. */
    LOAN_READY,
    SUSPENDED,
    CLOSED
}
