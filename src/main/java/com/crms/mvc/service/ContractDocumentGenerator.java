package com.crms.mvc.service;

import com.crms.mvc.dto.ContractDocumentDTO;

/**
 * Turns contract data into a printable document. The PDF library stays hidden behind this interface.
 */
public interface ContractDocumentGenerator {
    byte[] generatePdf(ContractDocumentDTO document);
}
