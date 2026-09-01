package br.com.tlf.infrastructure.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.tlf.core.port.out.transaction.TransactionRunner;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    @Override
    public void runInTransaction(Runnable work) {
        transactionTemplate.executeWithoutResult(status -> work.run());
    }
}
