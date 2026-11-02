package app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class ListExportModelAdvice {

    private final AppListExportProperties exportProperties;

    @ModelAttribute("exportMaxRows")
    public int exportMaxRows() {
        return exportProperties.getMaxRows();
    }
}
