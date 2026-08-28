package br.com.tlf.core.port.out.transaction;

public interface TransactionRunner {

    void runInTransaction(Runnable work);
}
