package cl.usach.pgt.entity;

import cl.usach.pgt.entity.enums.EstadoEntrega;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "entrega",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_entrega_tesis_hito",
                columnNames = {"tesis_id", "hito_id"}
        )
)
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tesis_id", nullable = false)
    private Tesis tesis;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hito_id", nullable = false)
    private HitoEntrega hito;

    @Column(name = "fecha_hora_carga", nullable = false)
    private LocalDateTime fechaHoraCarga;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    /** Ruta en el repositorio de documentos. El archivo NO se guarda en la base. */
    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoEntrega estado;

    protected Entrega() {
    }

    public Entrega(Tesis tesis, HitoEntrega hito,
                   String nombreArchivo, String rutaArchivo, long tamanoBytes) {
        this.tesis = tesis;
        this.hito = hito;
        actualizarDocumento(nombreArchivo, rutaArchivo, tamanoBytes);
    }

    /** Decisión D1: el reenvío sobrescribe el documento anterior. */
    public void actualizarDocumento(String nombreArchivo, String rutaArchivo, long tamanoBytes) {
        this.nombreArchivo = nombreArchivo;
        this.rutaArchivo = rutaArchivo;
        this.tamanoBytes = tamanoBytes;
        this.fechaHoraCarga = LocalDateTime.now();
        this.estado = EstadoEntrega.ENVIADO_PARA_REVISION;
    }

    public void cambiarEstado(EstadoEntrega nuevo) {
        this.estado = nuevo;
    }

    public boolean estaEvaluada() {
        return this.estado == EstadoEntrega.EVALUADO;
    }

    // getters

    public Long getId() {
        return id;
    }

    public Tesis getTesis() {
        return tesis;
    }

    public HitoEntrega getHito() {
        return hito;
    }

    public LocalDateTime getFechaHoraCarga() {
        return fechaHoraCarga;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public EstadoEntrega getEstado() {
        return estado;
    }
}

