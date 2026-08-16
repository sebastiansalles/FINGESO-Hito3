package cl.usach.pgt.service;

public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}