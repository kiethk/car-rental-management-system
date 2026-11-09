package com.crms.mvc.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReturnReceiptDTO {
    private OffsetDateTime returnedAt;
    private Integer odometer;
    private Integer fuelPercent;
    private BigDecimal rentalTotal = BigDecimal.ZERO;
    private BigDecimal depositAmount = BigDecimal.ZERO;
    private List<ReturnReceiptLineDTO> lines = new ArrayList<>();
    private boolean settled;
    private BigDecimal amountDue = BigDecimal.ZERO;
    private String paymentMethod;
    private OffsetDateTime paidAt;

    public boolean isNothingToPay() { return amountDue.signum() == 0; }

    public String getPaymentMethodLabel() {
        if (paymentMethod == null) {
            return null;
        }
        return switch (paymentMethod) {
            case "CASH" -> "Tiền mặt";
            case "BANK_TRANSFER" -> "Chuyển khoản";
            default -> paymentMethod;
        };
    }

    public OffsetDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(OffsetDateTime returnedAt) { this.returnedAt = returnedAt; }

    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }

    public Integer getFuelPercent() { return fuelPercent; }
    public void setFuelPercent(Integer fuelPercent) { this.fuelPercent = fuelPercent; }


    public BigDecimal getRentalTotal() { return rentalTotal; }
    public void setRentalTotal(BigDecimal rentalTotal) { this.rentalTotal = rentalTotal; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public List<ReturnReceiptLineDTO> getLines() { return lines; }
    public void setLines(List<ReturnReceiptLineDTO> lines) { this.lines = lines; }

    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }

    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
}
