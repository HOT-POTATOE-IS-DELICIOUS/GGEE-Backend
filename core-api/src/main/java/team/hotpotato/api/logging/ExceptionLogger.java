package team.hotpotato.api.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExceptionLogger {
    public void log(String exceptionMessage) {
        log.error("[ERROR] : { errorMessage : {} }", exceptionMessage);
    }
}
