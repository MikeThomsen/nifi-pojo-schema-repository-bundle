package org.apache.nifi.pojo.complex;

import java.time.Instant;

public class FinancialTransaction {
    private Person sender;
    private Person receiver;
    private Double amount;
    private Instant sendDate;
    private Instant receiveDate;
    private PaymentService paymentService;

    public FinancialTransaction() {}

    public FinancialTransaction(Person sender, Person receiver, Double amount,
                                Instant sendDate, Instant receiveDate,
                                PaymentService paymentService) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.sendDate = sendDate;
        this.receiveDate = receiveDate;
        this.paymentService = paymentService;
    }

    public Person getSender() {
        return sender;
    }

    public void setSender(Person sender) {
        this.sender = sender;
    }

    public Person getReceiver() {
        return receiver;
    }

    public void setReceiver(Person receiver) {
        this.receiver = receiver;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Instant getSendDate() {
        return sendDate;
    }

    public void setSendDate(Instant sendDate) {
        this.sendDate = sendDate;
    }

    public Instant getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(Instant receiveDate) {
        this.receiveDate = receiveDate;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public String toString() {
        return "FinancialTransaction{" +
                "sender=" + sender +
                ", receiver=" + receiver +
                ", amount=" + amount +
                ", sendDate=" + sendDate +
                ", receiveDate=" + receiveDate +
                ", paymentService=" + paymentService +
                '}';
    }
}
