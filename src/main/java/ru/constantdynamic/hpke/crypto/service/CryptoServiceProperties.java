package ru.constantdynamic.hpke.crypto.service;

public interface CryptoServiceProperties {
    int bitsOfSecurity();

    String getServiceName();

    CryptoServicePurpose purpose();

    Object getParams();
}
