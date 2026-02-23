package blue.language.samples.paynote.sdk;

import blue.language.samples.paynote.dsl.ChannelKey;

public enum PayNoteRole {
    PAYER("payer", "payerChannel"),
    PAYEE("payee", "payeeChannel"),
    GUARANTOR("guarantor", "guarantorChannel"),
    SHIPPER("shipmentCompany", "shipmentCompanyChannel");

    private final String roleKey;
    private final ChannelKey channelKey;

    PayNoteRole(String roleKey, String channelKey) {
        this.roleKey = roleKey;
        this.channelKey = ChannelKey.of(channelKey);
    }

    public String roleKey() {
        return roleKey;
    }

    public ChannelKey channelKey() {
        return channelKey;
    }
}
