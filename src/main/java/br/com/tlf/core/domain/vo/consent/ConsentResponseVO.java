package br.com.tlf.core.domain.vo.consent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsentResponseVO {

    private String id;
    private String expiresAt;
}
