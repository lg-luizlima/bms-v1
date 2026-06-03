package br.com.tlf.api.rest.dto.response.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private String id;

    @JsonProperty("proposta_id")
    private String propostaId;

    @JsonProperty("valor_contratado")
    private BigDecimal valorContratado;

    @JsonProperty("prazo")
    private Integer prazo;

    @JsonProperty("status")
    private String status;

    @JsonProperty("parcela")
    private BigDecimal parcela;

    @JsonProperty("assinatura_em")
    private String assinaturaEm;

    @JsonProperty("criacao_em")
    private String criacaoEm;

    @JsonProperty("atualizacao_em")
    private String atualizacaoEm;
}
