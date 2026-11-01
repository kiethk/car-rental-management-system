package com.crms.mvc.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class HandoverSummaryDTO {
    private List<HandoverRecordDTO> records = new ArrayList<>();
    private boolean returned;
    private boolean settled;
    private BigDecimal rentalTotal = BigDecimal.ZERO;
    private BigDecimal feesTotal = BigDecimal.ZERO;
    private BigDecimal depositAmount = BigDecimal.ZERO;
    private BigDecimal finalAmount = BigDecimal.ZERO;

    public boolean isCustomerOwes() { return finalAmount.signum() > 0; }
    public boolean isRefundDue() { return finalAmount.signum() < 0; }
    public BigDecimal getRefundAmount() { return finalAmount.abs(); }

    public List<HandoverRecordDTO> getRecords() { return records; }
    public void setRecords(List<HandoverRecordDTO> records) { this.records = records; }

    public boolean isReturned() { return returned; }
    public void setReturned(boolean returned) { this.returned = returned; }

    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }

    public BigDecimal getRentalTotal() { return rentalTotal; }
    public void setRentalTotal(BigDecimal rentalTotal) { this.rentalTotal = rentalTotal; }

    public BigDecimal getFeesTotal() { return feesTotal; }
    public void setFeesTotal(BigDecimal feesTotal) { this.feesTotal = feesTotal; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
}
