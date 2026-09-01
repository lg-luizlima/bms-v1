package br.com.tlf.infrastructure.persistence.postgresql.jpa;

import java.util.UUID;

/** Interface projection over {@code tb_term_products}, used to group already-fetched vigent terms by product. */
public interface TermProductProjection {

    UUID getTermId();

    String getProduct();
}
