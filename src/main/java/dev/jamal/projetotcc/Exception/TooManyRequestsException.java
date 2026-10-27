package dev.jamal.projetotcc.Exception;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException() { super("Muitas solicitações. Aguarde alguns minutos e tente novamente."); }
}
