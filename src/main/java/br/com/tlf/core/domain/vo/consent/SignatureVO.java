package br.com.tlf.core.domain.vo.consent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignatureVO {

    private String ip;
    private String userAgent;
    private String deviceId;
    private String channel;
    private GeolocationVO geolocation;
}
