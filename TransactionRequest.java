
package BankManagement.dto;

import java.math.BigDecimal;

public class TransactionRequest {

    private BigDecimal amount;

    public TransactionRequest() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}