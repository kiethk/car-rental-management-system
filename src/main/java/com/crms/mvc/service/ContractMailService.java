package com.crms.mvc.service;

import com.crms.mvc.dto.ContractDocumentDTO;

public interface ContractMailService {
    /** Sends the issued contract to the customer as a PDF attachment. */
    void sendContract(ContractDocumentDTO document, byte[] pdf);
}
