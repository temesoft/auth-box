package com.authbox.web.controller;

import com.authbox.base.config.AppProperties;
import com.authbox.base.model.Organization;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Import(AppProperties.class)
public class DefaultController extends BaseController {

    @Autowired
    protected AppProperties appProperties;

    @GetMapping
    @PostMapping
    public ModelAndView indexPage() {
        return createModelAndView("index");
    }

    @GetMapping("/register.html")
    public ModelAndView registerPage() {
        return createModelAndView("register");
    }

    @GetMapping({"/login"})
    public ModelAndView loginPage() {
        return createModelAndView("index");
    }

    @GetMapping({"/secure/"})
    @PreAuthorize("isAuthenticated()")
    public ModelAndView secureIndexPage() {
        final Organization organization = getOrganization();
        return createModelAndView("secure/index");
    }

    @GetMapping("/secure/{securePageName}.html")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView secureOrganization(@PathVariable("securePageName") final String securePageName) {
        final Organization organization = getOrganization();
        return createModelAndView("secure/" + securePageName);
    }

    private ModelAndView createModelAndView(final String view) {
        return new ModelAndView(view, ImmutableMap.of("appProperties", appProperties));
    }
}
