package club.adsayaart.filestore.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties("storage")
public class StorageProperties {
    private String location;

    public void setLocation(String location) {
        this.location = location;
    }
}