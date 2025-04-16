package net.akarmanov.projectplace.sso.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

  @GetMapping({"/login", "/register"})
  public String index() {
    return "index";
  }
}
