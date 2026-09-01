package br.com.tlf.core.port.in;

import br.com.tlf.core.domain.consent.ConsentReceipt;
import br.com.tlf.core.port.in.command.CreateConsentCommand;

public interface CreateConsentPort {

    ConsentReceipt execute(CreateConsentCommand command);
}
