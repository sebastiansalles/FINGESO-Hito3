package cl.usach.pgt.entity;

import cl.usach.pgt.entity.enums.EstadoTesis;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tesis")
public class Tesis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profesor_guia_id", nullable = false)
    private Usuario profesorGuia;

    @Column(name = "titulo_tema", nullable = false, length = 200)
    private String tituloTema;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTesis estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    protected Tesis() {
    }

    public Tesis(Usuario estudiante, Usuario profesorGuia, String tituloTema) {
        this.estudiante = estudiante;
        this.profesorGuia = profesorGuia;
        this.tituloTema = tituloTema;
        this.estado = EstadoTesis.ACTIVA;
        this.fechaInicio = LocalDate.now();
    }

    // getters aquí

    public Long getId() {
        return id;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public Usuario getProfesorGuia() {
        return profesorGuia;
    }

    public String getTituloTema() {
        return tituloTema;
    }

    public EstadoTesis getEstado() {
        return estado;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }
}