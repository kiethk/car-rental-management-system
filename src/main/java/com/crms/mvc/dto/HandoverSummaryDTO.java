package com.crms.mvc.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class HandoverSummaryDTO {
    private List<HandoverRecordDTO> records = new ArrayList<>();
    private boolean returned;
    private boolean settled;
    /** Late fee, Return charges and Issue fees not paid yet: all that settlement collects (the rent was paid at handover). */
    private BigDecimal feesTotal = BigDecimal.ZERO;
    /** Issue fees the customer already paid before returning (VNPay or at the counter); shown, never charged again. */
    private BigDecimal prepaidTotal = BigDecimal.ZERO;
    /** A collision or damage Issue exists on this booking, so the vehicle should go to maintenance on return. */
    private boolean vehicleDamageReported;
    private BigDecimal finalAmount = BigDecimal.ZERO;

    public boolean isCustomerOwes() { return finalAmount.signum() > 0; }

    public List<HandoverRecordDTO> getRecords() { return records; }
    public void setRecords(List<HandoverRecordDTO> records) { this.records = records; }

    public boolean isReturned() { return returned; }
    public void setReturned(boolean returned) { this.returned = returned; }

    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }

    public BigDecimal getFeesTotal() { return feesTotal; }
    public void setFeesTotal(BigDecimal feesTotal) { this.feesTotal = feesTotal; }

    public BigDecimal getPrepaidTotal() { return prepaidTotal; }
    public void setPrepaidTotal(BigDecimal prepaidTotal) { this.prepaidTotal = prepaidTotal; }

    public boolean isVehicleDamageReported() { return vehicleDamageReported; }
    public void setVehicleDamageReported(boolean vehicleDamageReported) { this.vehicleDamageReported = vehicleDamageReported; }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
}
