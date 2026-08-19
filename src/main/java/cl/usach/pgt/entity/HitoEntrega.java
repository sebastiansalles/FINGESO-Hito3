package cl.usach.pgt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hito_entrega")
public class HitoEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDateTime fechaLimite;

    protected HitoEntrega() {
    }

    public HitoEntrega(String nombre, LocalDateTime fechaLimite) {
        this.nombre = nombre;
        this.fechaLimite = fechaLimite;
    }

    //OP-11 · verificarPlazoVigente()
    public boolean verificarPlazoVigente() {
        return LocalDateTime.now().isBefore(fechaLimite);
    }

    // getters

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDateTime getFechaLimite() {
        return fechaLimite;
    }
}