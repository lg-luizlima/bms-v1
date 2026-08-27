package br.com.tlf.core.domain.vo.terms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentSignaturePayloadVO {

    private String ip;
    private String userAgent;
    private String deviceId;
    private String channel;
    private ConsentGeolocationPayloadVO geolocation;
}
