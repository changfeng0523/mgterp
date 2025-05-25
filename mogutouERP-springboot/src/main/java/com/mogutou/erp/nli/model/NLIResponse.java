package com.mogutou.erp.nli.model;

public class NLIResponse {
    private String reply;
    private boolean needConfirm;

    public NLIResponse(String reply, boolean needConfirm) {
        this.reply = reply;
        this.needConfirm = needConfirm;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public boolean isNeedConfirm() {
        return needConfirm;
    }

    public void setNeedConfirm(boolean needConfirm) {
        this.needConfirm = needConfirm;
    }
}
