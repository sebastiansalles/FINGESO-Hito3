package cl.usach.pgt.entity;

import cl.usach.pgt.entity.enums.RolUsuario;
import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String rut;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "correo_institucional", nullable = false, unique = true, length = 120)
    private String correoInstitucional;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolUsuario rol;

    protected Usuario() {
    }

    public Usuario(String rut, String nombre, String correoInstitucional, RolUsuario rol) {
        this.rut = rut;
        this.nombre = nombre;
        this.correoInstitucional = correoInstitucional;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public String getRut() { return rut; }
    public String getNombre() { return nombre; }
    public String getCorreoInstitucional() { return correoInstitucional; }
    public RolUsuario getRol() { return rol; }
}