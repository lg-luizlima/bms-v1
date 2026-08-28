package br.com.tlf.core.port.in;

import br.com.tlf.core.domain.terms.PendingTermsResult;
import br.com.tlf.core.port.in.command.PendingTermsQuery;

public interface GetPendingTermsPort {

    PendingTermsResult execute(PendingTermsQuery query);
}
