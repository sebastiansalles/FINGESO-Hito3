<script setup>
import { ref, onMounted } from 'vue'

const usuario = ref(null)
const rut = ref('20200001-1')
const contrasena = ref('')
const errorLogin = ref('')

const tesis = ref(null)
const hitos = ref([])
const hitoSeleccionado = ref(null)
const archivo = ref(null)
const comentario = ref('')
const mensaje = ref('')
const error = ref('')
const enviando = ref(false)

function esEstudiante() {
  return usuario.value && usuario.value.rol === 'ESTUDIANTE'
}

async function cargarDatosDelEstudiante() {
  await cargarTesis()
  await cargarPanel()
}

async function iniciarSesion() {
  errorLogin.value = ''
  const respuesta = await fetch('/api/sesion', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ rut: rut.value, contrasena: contrasena.value })
  })

  if (respuesta.ok) {
    usuario.value = await respuesta.json()
    if (esEstudiante()) await cargarDatosDelEstudiante()
  } else {
    const cuerpo = await respuesta.json()
    errorLogin.value = cuerpo.mensaje || 'No se pudo iniciar sesión.'
  }
}

async function cerrarSesion() {
  await fetch('/api/sesion', { method: 'DELETE' })
  usuario.value = null
  tesis.value = null
  hitos.value = []
  contrasena.value = ''
  cerrarFormulario()
  mensaje.value = ''
  error.value = ''
}

async function cargarTesis() {
  const respuesta = await fetch('/api/tesis')
  if (respuesta.ok) {
    tesis.value = await respuesta.json()
  } else {
    tesis.value = null
  }
}

async function cargarPanel() {
  error.value = ''
  const respuesta = await fetch('/api/panel')
  if (respuesta.ok) {
    hitos.value = await respuesta.json()
  } else {
    const cuerpo = await respuesta.json()
    error.value = cuerpo.mensaje || 'No se pudieron cargar los hitos.'
  }
}

// Paso 1 del CU_009: el estudiante selecciona un hito
function seleccionarHito(hito) {
  hitoSeleccionado.value = hito
  archivo.value = null
  comentario.value = ''
  mensaje.value = ''
  error.value = ''
}

function cerrarFormulario() {
  hitoSeleccionado.value = null
  archivo.value = null
  comentario.value = ''
}

function elegirArchivo(evento) {
  archivo.value = evento.target.files[0]
}

// Paso 5 del CU_009: adjunta el documento y envía
async function enviarEntrega() {
  mensaje.value = ''
  error.value = ''

  if (!archivo.value) {
    error.value = 'Debe adjuntar un archivo.'
    return
  }

  const datos = new FormData()
  datos.append('hitoId', hitoSeleccionado.value.hitoId)
  datos.append('archivo', archivo.value)
  datos.append('comentario', comentario.value)

  enviando.value = true
  let respuesta
  try {
    respuesta = await fetch('/api/entregas', { method: 'POST', body: datos })
  } catch (e) {
    enviando.value = false
    error.value = 'No se pudo completar el envío. Intente nuevamente.'
    return
  }
  enviando.value = false

  const cuerpo = await respuesta.json()

  if (respuesta.ok) {
    mensaje.value = 'Entrega registrada: ' + cuerpo.nombreArchivo
    cerrarFormulario()
    await cargarPanel()
  } else {
    error.value = cuerpo.mensaje || 'No se pudo registrar la entrega.'
  }
}

function formatear(fecha) {
  if (!fecha) return ''
  return new Date(fecha).toLocaleString('es-CL', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

function puedeEnviar(hito) {
  return hito.plazoVigente && hito.estadoEntrega !== 'EVALUADO'
}

onMounted(async () => {
  const respuesta = await fetch('/api/sesion')
  if (respuesta.ok) {
    usuario.value = await respuesta.json()
    if (esEstudiante()) await cargarDatosDelEstudiante()
  }
})
</script>

<template>
  <div class="contenedor">

    <!-- Inicio de sesión -->
    <div v-if="!usuario">
      <h2>Plataforma de Gestión de Tesistas</h2>
      <div>
        <input v-model="rut" placeholder="RUT">
        <input v-model="contrasena" type="password" placeholder="Contraseña"
               @keyup.enter="iniciarSesion">
        <button @click="iniciarSesion">Ingresar</button>
      </div>
      <p v-if="errorLogin" style="color: red;">{{ errorLogin }}</p>
    </div>

    <!-- Perfil sin funcionalidad en esta iteración -->
    <div v-else-if="!esEstudiante()">
      <div class="encabezado">
        <h2>{{ usuario.nombre }}</h2>
        <button @click="cerrarSesion">Cerrar sesión</button>
      </div>
      <p>Sesión iniciada con perfil <strong>{{ usuario.rol }}</strong>.</p>
      <p>Esta iteración implementa el caso de uso CU_009 (Registrar Entrega Parcial),
        correspondiente al perfil Estudiante.</p>
    </div>

    <!-- Panel del estudiante -->
    <div v-else>
      <div class="encabezado">
        <div>
          <h2>Mis Entregas — {{ usuario.nombre }}</h2>
          <p v-if="tesis" class="tesis">
            <strong>{{ tesis.titulo }}</strong><br>
            Profesor guía: {{ tesis.profesorGuia }} · Estado: {{ tesis.estado }}
          </p>
        </div>
        <button @click="cerrarSesion">Cerrar sesión</button>
      </div>

      <p v-if="mensaje" style="color: green;">{{ mensaje }}</p>
      <p v-if="error" style="color: red;">{{ error }}</p>

      <!-- Paso 4: el sistema despliega el formulario de entrega -->
      <div v-if="hitoSeleccionado" class="formulario">
        <h3>Nueva entrega</h3>

        <p>
          <strong>Hito:</strong> {{ hitoSeleccionado.nombreHito }}<br>
          <strong>Plazo:</strong> {{ formatear(hitoSeleccionado.fechaLimite) }}
        </p>

        <p>
          <label>Documento (PDF o DOCX, máximo 20 MB)</label><br>
          <input type="file" @change="elegirArchivo">
        </p>

        <p>
          <label>Comentario para el profesor guía (opcional)</label><br>
          <textarea v-model="comentario" rows="3" maxlength="500"
                    placeholder="Ej: se incorporaron las correcciones del capítulo 2."></textarea>
          <br><small>{{ comentario.length }} / 500</small>
        </p>

        <button :disabled="enviando" @click="enviarEntrega">
          {{ enviando ? 'Enviando...' : 'Enviar entrega' }}
        </button>
        <button @click="cerrarFormulario">Cancelar</button>
      </div>

      <!-- Listado de hitos -->
      <table border="1" cellpadding="8" cellspacing="0">
        <colgroup>
          <col style="width: 26%">
          <col style="width: 18%">
          <col style="width: 12%">
          <col style="width: 30%">
          <col style="width: 14%">
        </colgroup>
        <thead style="background: #eee;">
        <tr>
          <th>Hito</th>
          <th>Plazo</th>
          <th>Estado</th>
          <th>Documento</th>
          <th>Acción</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="hito in hitos" :key="hito.hitoId">
          <td>{{ hito.nombreHito }}</td>
          <td>
            {{ formatear(hito.fechaLimite) }}
            <strong v-if="!hito.plazoVigente && !hito.nombreArchivo" style="color: red;">
              (Vencido)
            </strong>
            <span v-else-if="!hito.plazoVigente" style="color: #777;">
                (Cerrado)
              </span>
          </td>
          <td>{{ hito.estadoEntrega || 'Pendiente' }}</td>
          <td>
              <span v-if="hito.nombreArchivo">
                {{ hito.nombreArchivo }}
                <br><small>{{ formatear(hito.fechaHoraCarga) }}</small>
                <br><small v-if="hito.comentario" class="comentario">"{{ hito.comentario }}"</small>
              </span>
            <span v-else>—</span>
          </td>
          <td>
            <button v-if="puedeEnviar(hito)" @click="seleccionarHito(hito)">
              {{ hito.nombreArchivo ? 'Reemplazar' : 'Entregar' }}
            </button>
            <span v-else>No disponible</span>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style>
body {
  font-family: sans-serif;
  margin: 0;
  padding: 20px;
}
.contenedor {
  max-width: 900px;
  margin: 0 auto;
}
.encabezado {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.encabezado h2 {
  margin-bottom: 0;
}
.tesis {
  margin: 4px 0 0;
  font-size: 13px;
  color: #555;
}
.formulario {
  border: 1px solid #ccc;
  background: #fafafa;
  padding: 16px;
  margin-bottom: 20px;
}
.formulario h3 {
  margin-top: 0;
}
label {
  font-size: 13px;
  color: #444;
}
textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 6px;
  font-family: inherit;
}
table {
  width: 100%;
  text-align: left;
  table-layout: fixed;
}
td {
  vertical-align: top;
  overflow-wrap: anywhere;
}
.comentario {
  color: #666;
  font-style: italic;
}
input, button {
  margin-right: 5px;
  padding: 5px;
}
</style>