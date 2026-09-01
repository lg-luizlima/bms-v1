package br.com.tlf.core.port.in;

import java.util.List;

import br.com.tlf.core.domain.terms.PendingTermsResult;
import br.com.tlf.core.port.in.command.PendingTermsQuery;

public interface GetPendingTermsPort {

    List<PendingTermsResult> execute(PendingTermsQuery query);
}
