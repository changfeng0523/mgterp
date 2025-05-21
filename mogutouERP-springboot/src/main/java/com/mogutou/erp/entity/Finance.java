package com.mogutou.erp.entity;

import java.util.List;

public class Finance {
    private List<Float> profit;
    private List<Float> turnover;
    private List<Integer> orderQuantity;

    public Finance() {
    }

    public Finance(List<Float> profit, List<Float> turnover, List<Integer> orderQuantity) {
        this.profit = profit;
        this.turnover = turnover;
        this.orderQuantity = orderQuantity;
    }

    public List<Float> getProfit() {
        return profit;
    }

    public void setProfit(List<Float> profit) {
        this.profit = profit;
    }

    public List<Float> getTurnover() {
        return turnover;
    }

    public void setTurnover(List<Float> turnover) {
        this.turnover = turnover;
    }

    public List<Integer> getOrderQuantity() {
        return orderQuantity;
    }

    public void setOrderQuantity(List<Integer> orderQuantity) {
        this.orderQuantity = orderQuantity;
    }
}