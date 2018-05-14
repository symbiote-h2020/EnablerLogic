package eu.h2020.symbiote.enablerlogic.messaging.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FullExchangeProperties {
    private String name;
    private String type;
    private boolean durable;
    private boolean autodelete;
    private boolean internal;
}
