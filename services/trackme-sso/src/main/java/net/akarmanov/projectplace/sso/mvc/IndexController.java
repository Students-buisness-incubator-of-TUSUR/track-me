package net.akarmanov.projectplace.sso.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

  @GetMapping({"/login", "/register"})
  public String index() {
    return "index";
  }
}
