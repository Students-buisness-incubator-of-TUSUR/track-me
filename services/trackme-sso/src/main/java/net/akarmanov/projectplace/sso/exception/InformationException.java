package net.akarmanov.projectplace.sso.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
public class InformationException extends RuntimeException {

  private String description;

  public InformationException(String description, Throwable cause) {
    super(cause != null ? cause.getMessage() : description, cause);
    this.description = description;
  }

  public static InformationExceptionBuilder builder(String description, Throwable throwable) {
    return new InformationExceptionBuilder()
        .description(description)
        .throwable(throwable);
  }

  public static InformationExceptionBuilder builder(String description) {
    return new InformationExceptionBuilder()
        .description(description);
  }

  @Setter
  @Accessors(chain = true, fluent = true)
  public static class InformationExceptionBuilder {
    private String description;

    private Throwable throwable;

    InformationExceptionBuilder() {
    }

    public InformationException build() {
      return new InformationException(description, throwable);
    }
  }
}
