package io.micronaut.api.excluded.subpackage;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
public class ExcludedPackageApi {

    @Get("/company")
    public void getCompany() {

    }
}
